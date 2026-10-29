package com.vietgo.bus;

import com.vietgo.common.FareQuote;
import com.vietgo.common.FeeItem;
import com.vietgo.common.Money;
import com.vietgo.common.PassengerFare;
import com.vietgo.common.VnHolidayCalendar;
import com.vietgo.domain.BusPassengerType;
import com.vietgo.domain.BusTrip;
import com.vietgo.domain.FeeLine;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * CHINH SACH PHI VE XE KHACH (thuat toan gia dong theo ty le lap day).
 *
 * 1) Gia goc ghe = gia niem yet cua chuyen x he so vi tri
 *    (giuong tang duoi +10%, ghe VIP hang dau Limousine +15%), lam tron 1.000d.
 * 2) Gia dong theo ty le lap day TRUOC khi dat: duoi 50% +0%, tu 50% den duoi 80% +5%, tu 80% +10%.
 * 3) Phu phi ngay xe chay: Le/Tet +25%, cuoi tuan (T6-CN) +10% (khong cong don).
 * 4) Tre em 5-11 tuoi giam 30%. Tre duoi 5 tuoi di kem nguoi lon, khong can mua ve.
 * 5) Uu dai nhom: tu 4 khach tro len giam them 5%.
 * 6) Hanh ly: mien phi 20kg/khach, vuot muc 10.000d/kg (toi da 40kg/khach).
 * 7) Don tan noi (trung chuyen): 50.000d/don.
 */
@Component
public class BusFareCalculator {

    public static final int OCCUPANCY_MID = 50;
    public static final int OCCUPANCY_HIGH = 80;
    public static final int OCCUPANCY_MID_PERCENT = 5;
    public static final int OCCUPANCY_HIGH_PERCENT = 10;
    public static final int HOLIDAY_SURCHARGE_PERCENT = 25;
    public static final int WEEKEND_SURCHARGE_PERCENT = 10;
    public static final int GROUP_MIN_SIZE = 4;
    public static final int GROUP_DISCOUNT_PERCENT = 5;
    public static final int FREE_LUGGAGE_KG = 20;
    public static final int MAX_LUGGAGE_KG = 40;
    public static final long LUGGAGE_FEE_PER_KG = 10_000;
    public static final long DOOR_PICKUP_FEE = 50_000;
    public static final String DOOR_PICKUP = "DON_TAN_NOI";

    /** Gia goc cua ghe theo vi tri. */
    public long seatBasePrice(BusTrip trip, String seatCode) {
        return Money.roundThousand(trip.getBasePrice() * BusSeatLayout.positionFactor(trip.getBusType(), seatCode));
    }

    /** Ty le lap day (%) truoc khi dat. */
    public int occupancyPercent(BusTrip trip, long occupiedSeats) {
        if (trip.getCapacity() == 0) return 0;
        return (int) Math.floor(occupiedSeats * 100.0 / trip.getCapacity());
    }

    /** Muc phu phi theo ty le lap day. */
    public int occupancySurchargePercent(int occupancyPercent) {
        if (occupancyPercent >= OCCUPANCY_HIGH) return OCCUPANCY_HIGH_PERCENT;
        if (occupancyPercent >= OCCUPANCY_MID) return OCCUPANCY_MID_PERCENT;
        return 0;
    }

    public int dateSurchargePercent(LocalDate date) {
        if (VnHolidayCalendar.isHoliday(date)) return HOLIDAY_SURCHARGE_PERCENT;
        if (VnHolidayCalendar.isWeekend(date)) return WEEKEND_SURCHARGE_PERCENT;
        return 0;
    }

    public String dateSurchargeLabel(LocalDate date) {
        String holiday = VnHolidayCalendar.holidayName(date);
        if (holiday != null) return "Phụ phí " + holiday + " (+" + HOLIDAY_SURCHARGE_PERCENT + "%)";
        if (VnHolidayCalendar.isWeekend(date)) return "Phụ phí cuối tuần (+" + WEEKEND_SURCHARGE_PERCENT + "%)";
        return null;
    }

    /** Gia hien thi tren so do ghe cho nguoi lon (da gom phu phi hien hanh). */
    public long displayPrice(BusTrip trip, String seatCode, long occupiedSeats) {
        long base = seatBasePrice(trip, seatCode);
        int occPct = occupancySurchargePercent(occupancyPercent(trip, occupiedSeats));
        int datePct = dateSurchargePercent(trip.getDepartureTime().toLocalDate());
        return base + Money.percentOf(base, occPct) + Money.percentOf(base, datePct);
    }

    public long luggageFee(int totalKg, int passengers) {
        int extra = Math.max(0, totalKg - FREE_LUGGAGE_KG * passengers);
        return extra * LUGGAGE_FEE_PER_KG;
    }

    /** Dau vao tinh gia cua tung hanh khach. */
    public record PassengerInput(String seatCode, String passengerName, BusPassengerType type) {
    }

    public FareQuote quote(BusTrip trip, List<PassengerInput> inputs, long occupiedSeats,
                           int luggageKg, boolean doorPickup) {
        LocalDate date = trip.getDepartureTime().toLocalDate();
        int occPercent = occupancyPercent(trip, occupiedSeats);
        int occPct = occupancySurchargePercent(occPercent);
        int datePct = dateSurchargePercent(date);
        int n = inputs.size();
        boolean group = n >= GROUP_MIN_SIZE;

        List<PassengerFare> fares = new ArrayList<>();
        long totalBase = 0, totalOcc = 0, totalDate = 0, totalChild = 0, totalGroup = 0;

        for (PassengerInput in : inputs) {
            long base = seatBasePrice(trip, in.seatCode());
            long occ = Money.percentOf(base, occPct);
            long dateS = Money.percentOf(base, datePct);
            long child = Money.percentOf(base + occ + dateS, in.type().getDiscountPercent());
            long grp = group ? Money.percentOf(base + occ + dateS - child, GROUP_DISCOUNT_PERCENT) : 0;
            long fin = base + occ + dateS - child - grp;

            totalBase += base;
            totalOcc += occ;
            totalDate += dateS;
            totalChild += child;
            totalGroup += grp;
            fares.add(new PassengerFare(in.seatCode(), BusSeatLayout.label(trip.getBusType(), in.seatCode()),
                    in.passengerName(), in.type().getLabel(), base, occ + dateS, child + grp, fin));
        }

        List<FeeItem> items = new ArrayList<>();
        items.add(new FeeItem("Giá vé gốc (" + n + " chỗ)", totalBase, FeeLine.BASE,
                "Giá niêm yết × hệ số vị trí ghế"));
        if (totalOcc > 0) {
            items.add(new FeeItem("Giá động: xe đã lấp đầy " + occPercent + "% (+" + occPct + "%)",
                    totalOcc, FeeLine.SURCHARGE, null));
        }
        if (totalDate > 0) {
            items.add(new FeeItem(dateSurchargeLabel(date), totalDate, FeeLine.SURCHARGE, null));
        }
        if (totalChild > 0) {
            items.add(new FeeItem("Giảm giá trẻ em (−" + BusPassengerType.CHILD.getDiscountPercent() + "%)",
                    -totalChild, FeeLine.DISCOUNT, null));
        }
        if (totalGroup > 0) {
            items.add(new FeeItem("Ưu đãi nhóm từ " + GROUP_MIN_SIZE + " khách (−" + GROUP_DISCOUNT_PERCENT + "%)",
                    -totalGroup, FeeLine.DISCOUNT, null));
        }
        long luggage = luggageFee(luggageKg, n);
        if (luggage > 0) {
            int extraKg = luggageKg - FREE_LUGGAGE_KG * n;
            items.add(new FeeItem("Hành lý vượt mức " + extraKg + " kg × 10.000đ", luggage, FeeLine.SERVICE,
                    "Miễn phí " + FREE_LUGGAGE_KG + " kg/khách"));
        }
        long door = doorPickup ? DOOR_PICKUP_FEE : 0;
        if (door > 0) {
            items.add(new FeeItem("Đón tận nơi (xe trung chuyển)", door, FeeLine.SERVICE, null));
        }

        long total = fares.stream().mapToLong(PassengerFare::getFinalPrice).sum() + luggage + door;
        return new FareQuote(fares, items, total);
    }
}
