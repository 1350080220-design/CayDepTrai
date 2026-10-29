package com.vietgo.bus;

import com.vietgo.common.FareQuote;
import com.vietgo.domain.BusPassengerType;
import com.vietgo.domain.BusTrip;
import com.vietgo.domain.BusType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Kiem thu thuat toan gia dong theo ty le lap day cua ve xe khach. */
class BusFareCalculatorTest {

    private final BusFareCalculator calc = new BusFareCalculator();

    static BusTrip trip(BusType type, long price, LocalDateTime dep) {
        BusTrip b = new BusTrip();
        b.setId(1L);
        b.setOperatorName("Sen Vàng Express");
        b.setLicensePlate("51B-123.45");
        b.setOriginCity("TP. Hồ Chí Minh");
        b.setDestinationCity("Đà Lạt");
        b.setDepartureTime(dep);
        b.setArrivalTime(dep.plusHours(7));
        b.setBusType(type);
        b.setBasePrice(price);
        b.setPickupPoints("Bến xe Miền Đông mới;Văn phòng Quận 1");
        b.setDropoffPoints("Bến xe liên tỉnh Đà Lạt;Chợ Đà Lạt");
        return b;
    }

    @Test
    void occupancyTiers() {
        assertEquals(0, calc.occupancySurchargePercent(49));
        assertEquals(5, calc.occupancySurchargePercent(50));
        assertEquals(5, calc.occupancySurchargePercent(79));
        assertEquals(10, calc.occupancySurchargePercent(80));
    }

    @Test
    void positionFactors() {
        BusTrip s = trip(BusType.SLEEPER_36, 290_000, LocalDateTime.of(2027, 3, 9, 22, 0));
        assertEquals(319_000, calc.seatBasePrice(s, "A01"));
        assertEquals(290_000, calc.seatBasePrice(s, "B01"));
        BusTrip l = trip(BusType.LIMOUSINE_9, 380_000, LocalDateTime.of(2027, 3, 9, 22, 0));
        assertEquals(437_000, calc.seatBasePrice(l, "L1"));
        assertEquals(380_000, calc.seatBasePrice(l, "L5"));
    }

    @Test
    void groupLuggageAndDoorPickup() {
        BusTrip s = trip(BusType.SLEEPER_36, 290_000, LocalDateTime.of(2027, 3, 9, 22, 0)); // thu Ba
        List<BusFareCalculator.PassengerInput> in = List.of(
                new BusFareCalculator.PassengerInput("B01", "A", BusPassengerType.ADULT),
                new BusFareCalculator.PassengerInput("B02", "B", BusPassengerType.ADULT),
                new BusFareCalculator.PassengerInput("B03", "C", BusPassengerType.ADULT),
                new BusFareCalculator.PassengerInput("B04", "D", BusPassengerType.ADULT));
        FareQuote q = calc.quote(s, in, 0, 85, true);
        assertEquals(275_000, q.getPassengers().get(0).getFinalPrice()); // -5% nhom
        assertEquals(4 * 275_000 + 50_000 + 50_000, q.getTotal());
    }

    @Test
    void dynamicPricingAndChildDiscount() {
        BusTrip s = trip(BusType.SLEEPER_36, 290_000, LocalDateTime.of(2027, 3, 9, 22, 0));
        FareQuote q = calc.quote(s, List.of(
                new BusFareCalculator.PassengerInput("B10", "A", BusPassengerType.ADULT),
                new BusFareCalculator.PassengerInput("B11", "B", BusPassengerType.CHILD)), 30, 0, false);
        // 30/36 = 83% -> +10% = 29.000
        assertEquals(319_000, q.getPassengers().get(0).getFinalPrice());
        assertEquals(319_000 - 96_000, q.getPassengers().get(1).getFinalPrice()); // tre em -30% (95.700 -> 96.000)
    }

    @Test
    void weekendSurcharge() {
        BusTrip s = trip(BusType.SEAT_45, 160_000, LocalDateTime.of(2027, 3, 13, 7, 0)); // thu Bay
        assertEquals(176_000, calc.displayPrice(s, "A01", 0));
    }
}
