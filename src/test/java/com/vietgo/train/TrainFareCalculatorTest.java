package com.vietgo.train;

import com.vietgo.common.FareQuote;
import com.vietgo.domain.TrainCoachType;
import com.vietgo.domain.TrainPassengerType;
import com.vietgo.domain.TrainTrip;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Kiem thu thuat toan tinh gia ve tau theo bac cu ly. */
class TrainFareCalculatorTest {

    private final TrainFareCalculator calc = new TrainFareCalculator();

    static TrainTrip trip(int km, LocalDateTime departure) {
        TrainTrip t = new TrainTrip();
        t.setId(1L);
        t.setTrainCode("SE19");
        t.setOriginStation("Hà Nội");
        t.setDestinationStation("Đà Nẵng");
        t.setDistanceKm(km);
        t.setDepartureTime(departure);
        t.setArrivalTime(departure.plusHours(15));
        t.addCoach(1, TrainCoachType.SOFT_SEAT);
        t.addCoach(2, TrainCoachType.SLEEPER_6);
        t.addCoach(3, TrainCoachType.SLEEPER_4);
        return t;
    }

    @Test
    void distanceFareIsProgressive() {
        assertEquals(90_000, calc.distanceFare(100));
        assertEquals(270_000, calc.distanceFare(300));
        assertEquals(270_000 + 491 * 700, calc.distanceFare(791));
        assertEquals(270_000 + 490_000 + 726 * 500, calc.distanceFare(1726));
        assertEquals(0, calc.distanceFare(0));
    }

    @Test
    void seatPriceUsesCoachAndBerthFactors() {
        TrainTrip t = trip(791, LocalDateTime.of(2027, 3, 9, 20, 0));
        assertEquals(614_000, calc.seatBasePrice(t, TrainCoachType.SOFT_SEAT, 10));
        assertEquals(911_000, calc.seatBasePrice(t, TrainCoachType.SLEEPER_6, 1)); // tang 1: x1.35 x1.10
        assertEquals(746_000, calc.seatBasePrice(t, TrainCoachType.SLEEPER_6, 5)); // tang 3: x1.35 x0.90
    }

    @Test
    void weekdayQuoteWithChildDiscountAndInsurance() {
        LocalDate dep = LocalDate.of(2027, 3, 9);
        assertEquals(DayOfWeek.TUESDAY, dep.getDayOfWeek());
        TrainTrip t = trip(791, dep.atTime(20, 0));
        FareQuote q = calc.quote(t, List.of(
                new TrainFareCalculator.PassengerInput("1-1", "Nguyễn Văn A", TrainPassengerType.ADULT),
                new TrainFareCalculator.PassengerInput("1-2", "Nguyễn Văn B", TrainPassengerType.CHILD)),
                dep.minusDays(10).atTime(9, 0));
        assertEquals(614_000, q.getPassengers().get(0).getFinalPrice());
        assertEquals(614_000 - 154_000, q.getPassengers().get(1).getFinalPrice());
        assertEquals(614_000 + 460_000 + 2_000, q.getTotal());
    }

    @Test
    void weekendSurchargeAndEarlyBookingDiscount() {
        LocalDate dep = LocalDate.of(2027, 3, 12);
        assertEquals(DayOfWeek.FRIDAY, dep.getDayOfWeek());
        TrainTrip t = trip(791, dep.atTime(20, 0));
        FareQuote q = calc.quote(t, List.of(
                new TrainFareCalculator.PassengerInput("1-1", "Nguyễn Văn A", TrainPassengerType.ADULT)),
                dep.minusDays(40).atTime(9, 0));
        // 614.000 + 5% (31.000) = 645.000; dat som -5% (32.000) = 613.000
        assertEquals(31_000, q.getPassengers().get(0).getSurcharge());
        assertEquals(613_000, q.getPassengers().get(0).getFinalPrice());
        assertEquals(614_000, q.getTotal());
    }

    @Test
    void holidaySurcharge() {
        assertEquals(20, calc.surchargePercent(LocalDate.of(2027, 2, 6)));
        assertEquals(20, calc.surchargePercent(LocalDate.of(2026, 4, 30)));
        assertEquals(5, calc.surchargePercent(LocalDate.of(2027, 3, 13)));
        assertEquals(0, calc.surchargePercent(LocalDate.of(2027, 3, 9)));
    }
}
