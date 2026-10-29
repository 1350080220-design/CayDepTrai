package com.vietgo.train;

import com.vietgo.common.FareQuote;
import com.vietgo.common.FeeItem;
import com.vietgo.common.Money;
import com.vietgo.common.PassengerFare;
import com.vietgo.common.VnHolidayCalendar;
import com.vietgo.domain.FeeLine;
import com.vietgo.domain.TrainCoachType;
import com.vietgo.domain.TrainPassengerType;
import com.vietgo.domain.TrainTrip;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * CHINH SACH PHI VE TAU HOA (thuat toan tinh gia theo bac cu ly).
 *
 * 1) Cuoc cu ly luy tien giam dan (cang di xa don gia/km cang re):
 *      - 300 km dau tien:        900 d/km
 *      - tu km 301 den km 1000:  700 d/km
 *      - tu km 1001 tro di:      500 d/km
 * 2) Nhan he so hang toa: Ngoi mem x1.00, Giuong khoang 6 x1.35, Giuong khoang 4 x1.60.
 * 3) Nhan he so tang giuong: K6 tang 1 x1.10, tang 2 x1.00, tang 3 x0.90; K4 tang 1 x1.05, tang 2 x1.00.
 *    => Gia goc cua ghe, lam tron den 1.000d.
 * 4) Phu phi thoi diem (tinh theo ngay tau chay, khong cong don): cao diem Le/Tet +20%, cuoi tuan (T6-CN) +5%.
 * 5) Giam gia doi tuong (khong cong don): tre em -25%, sinh vien -10%, nguoi cao tuoi -15%.
 * 6) Uu dai dat som: dat truoc ngay di tu 30 ngay tro len -5% (tinh sau giam doi tuong).
 * 7) Bao hiem hanh khach bat buoc: 1.000d/khach (khong hoan lai khi huy ve).
 */
@Component
public class TrainFareCalculator {

    public static final int TIER1_KM = 300;
    public static final int TIER2_KM = 1000;
    public static final int TIER1_RATE = 900;
    public static final int TIER2_RATE = 700;
    public static final int TIER3_RATE = 500;

    public static final int HOLIDAY_SURCHARGE_PERCENT = 20;
    public static final int WEEKEND_SURCHARGE_PERCENT = 5;
    public static final int EARLY_BOOKING_DAYS = 30;
    public static final int EARLY_BOOKING_PERCENT = 5;
    public static final long INSURANCE_PER_PASSENGER = 1_000;

    /** Cuoc cu ly luy tien (chua nhan he so). */
    public long distanceFare(int km) {
        if (km <= 0) return 0;
        long tier1 = (long) Math.min(km, TIER1_KM) * TIER1_RATE;
        long tier2 = (long) Math.max(0, Math.min(km, TIER2_KM) - TIER1_KM) * TIER2_RATE;
        long tier3 = (long) Math.max(0, km - TIER2_KM) * TIER3_RATE;
        return tier1 + tier2 + tier3;
    }

    /** He so tang giuong. */
    public double berthFactor(TrainCoachType type, int tier) {
        if (type == TrainCoachType.SLEEPER_6) {
            return switch (tier) {
                case 1 -> 1.10;
                case 3 -> 0.90;
                default -> 1.00;
            };
        }
        if (type == TrainCoachType.SLEEPER_4) {
            return tier == 1 ? 1.05 : 1.00;
        }
        return 1.00;
    }

    /** Gia goc cua mot ghe/giuong cu the. */
    public long seatBasePrice(TrainTrip trip, TrainCoachType type, int seatNo) {
        int tier = TrainSeatLayout.tierOf(type, seatNo);
        double price = distanceFare(trip.getDistanceKm()) * type.getClassFactor() * berthFactor(type, tier);
        return Money.roundThousand(price);
    }

    /** Phan tram phu phi theo ngay tau chay. */
    public int surchargePercent(LocalDate departureDate) {
        if (VnHolidayCalendar.isHoliday(departureDate)) return HOLIDAY_SURCHARGE_PERCENT;
        if (VnHolidayCalendar.isWeekend(departureDate)) return WEEKEND_SURCHARGE_PERCENT;
        return 0;
    }

    public String surchargeLabel(LocalDate departureDate) {
        String holiday = VnHolidayCalendar.holidayName(departureDate);
        if (holiday != null) return "Phụ phí cao điểm " + holiday + " (+" + HOLIDAY_SURCHARGE_PERCENT + "%)";
        if (VnHolidayCalendar.isWeekend(departureDate)) {
            return "Phụ phí cuối tuần (+" + WEEKEND_SURCHARGE_PERCENT + "%)";
        }
        return null;
    }

    public boolean isEarlyBooking(LocalDateTime bookingTime, LocalDateTime departure) {
        return ChronoUnit.DAYS.between(bookingTime.toLocalDate(), departure.toLocalDate()) >= EARLY_BOOKING_DAYS;
    }

    /** Dau vao tinh gia cho tung hanh khach. */
    public record PassengerInput(String seatKey, String passengerName, TrainPassengerType type) {
    }

    /** Tinh gia cho ca don. */
    public FareQuote quote(TrainTrip trip, List<PassengerInput> inputs, LocalDateTime bookingTime) {
        LocalDate depDate = trip.getDepartureTime().toLocalDate();
        int surchargePct = surchargePercent(depDate);
        boolean early = isEarlyBooking(bookingTime, trip.getDepartureTime());

        List<PassengerFare> fares = new ArrayList<>();
        long totalBase = 0;
        long totalSurcharge = 0;
        long totalEarly = 0;
        Map<TrainPassengerType, Long> discountByType = new EnumMap<>(TrainPassengerType.class);

        for (PassengerInput in : inputs) {
            int[] p = TrainSeatLayout.parse(in.seatKey());
            TrainCoachType coachType = TrainSeatLayout.findCoach(trip, p[0])
                    .orElseThrow(() -> new IllegalArgumentException("Toa không tồn tại: " + in.seatKey()))
                    .getCoachType();

            long base = seatBasePrice(trip, coachType, p[1]);
            long surcharge = Money.percentOf(base, surchargePct);
            long typeDiscount = Money.percentOf(base + surcharge, in.type().getDiscountPercent());
            long earlyDiscount = early
                    ? Money.percentOf(base + surcharge - typeDiscount, EARLY_BOOKING_PERCENT) : 0;
            long finalPrice = base + surcharge - typeDiscount - earlyDiscount;

            totalBase += base;
            totalSurcharge += surcharge;
            totalEarly += earlyDiscount;
            if (typeDiscount > 0) discountByType.merge(in.type(), typeDiscount, Long::sum);

            fares.add(new PassengerFare(in.seatKey(), TrainSeatLayout.label(trip, in.seatKey()),
                    in.passengerName(), in.type().getLabel(),
                    base, surcharge, typeDiscount + earlyDiscount, finalPrice));
        }

        int n = inputs.size();
        List<FeeItem> items = new ArrayList<>();
        items.add(new FeeItem("Giá vé gốc theo cự ly " + trip.getDistanceKm() + " km (" + n + " vé)",
                totalBase, FeeLine.BASE, "Cước lũy tiến theo bậc cự ly × hệ số hạng toa × hệ số tầng"));
        if (totalSurcharge > 0) {
            items.add(new FeeItem(surchargeLabel(depDate), totalSurcharge, FeeLine.SURCHARGE, null));
        }
        for (Map.Entry<TrainPassengerType, Long> e : discountByType.entrySet()) {
            items.add(new FeeItem("Giảm giá " + e.getKey().getLabel().toLowerCase()
                    + " (−" + e.getKey().getDiscountPercent() + "%)", -e.getValue(), FeeLine.DISCOUNT, null));
        }
        if (totalEarly > 0) {
            items.add(new FeeItem("Ưu đãi đặt sớm từ " + EARLY_BOOKING_DAYS + " ngày (−" + EARLY_BOOKING_PERCENT + "%)",
                    -totalEarly, FeeLine.DISCOUNT, null));
        }
        long insurance = INSURANCE_PER_PASSENGER * n;
        items.add(new FeeItem("Bảo hiểm hành khách (" + n + " × 1.000đ)", insurance, FeeLine.SERVICE,
                "Không hoàn lại khi hủy vé"));

        long total = fares.stream().mapToLong(PassengerFare::getFinalPrice).sum() + insurance;
        return new FareQuote(fares, items, total);
    }
}
