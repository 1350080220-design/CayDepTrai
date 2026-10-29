package com.vietgo.common;

import com.vietgo.bus.BusRefundPolicy;
import com.vietgo.domain.Booking;
import com.vietgo.domain.BookingStatus;
import com.vietgo.domain.BusTrip;
import com.vietgo.domain.TicketType;
import com.vietgo.domain.TrainTrip;
import com.vietgo.train.TrainRefundPolicy;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Kiem thu chinh sach hoan huy rieng cua tung loai ve. */
class RefundPolicyTest {

    private final LocalDateTime dep = LocalDateTime.of(2027, 3, 9, 20, 0);

    private Booking train(long total, int pax) {
        TrainTrip t = new TrainTrip();
        t.setDepartureTime(dep);
        Booking b = new Booking();
        b.setTicketType(TicketType.TRAIN);
        b.setTrainTrip(t);
        b.setStatus(BookingStatus.PAID);
        b.setTotalAmount(total);
        b.setPassengerCount(pax);
        return b;
    }

    private Booking bus(long total, boolean door) {
        BusTrip t = new BusTrip();
        t.setDepartureTime(dep);
        Booking b = new Booking();
        b.setTicketType(TicketType.BUS);
        b.setBusTrip(t);
        b.setStatus(BookingStatus.PAID);
        b.setTotalAmount(total);
        b.setPassengerCount(2);
        if (door) b.setPickupAddress("12 Nguyễn Văn Bảo, Gò Vấp");
        return b;
    }

    @Test
    void trainRefundWindows() {
        TrainRefundPolicy p = new TrainRefundPolicy();
        RefundQuote early = p.quote(train(1_000_000, 2), dep.minusHours(30));
        assertTrue(early.isAllowed());
        assertEquals(2_000, early.getNonRefundable());
        assertEquals(100_000, early.getCancelFee());   // 10% cua 998.000
        assertEquals(898_000, early.getRefundAmount());

        RefundQuote late = p.quote(train(1_000_000, 2), dep.minusHours(10));
        assertEquals(200_000, late.getCancelFee());    // 20%

        assertFalse(p.quote(train(1_000_000, 2), dep.minusHours(3)).isAllowed());
    }

    @Test
    void busRefundWindows() {
        BusRefundPolicy p = new BusRefundPolicy();
        RefundQuote early = p.quote(bus(600_000, true), dep.minusHours(48));
        assertEquals(50_000, early.getNonRefundable());
        assertEquals(55_000, early.getCancelFee());    // 10% cua 550.000
        assertEquals(495_000, early.getRefundAmount());

        RefundQuote late = p.quote(bus(600_000, false), dep.minusHours(5));
        assertEquals(150_000, late.getCancelFee());    // 25%

        assertFalse(p.quote(bus(600_000, false), dep.minusMinutes(90)).isAllowed());
    }

    @Test
    void holidayCalendar() {
        assertEquals("Tết Nguyên Đán", VnHolidayCalendar.holidayName(java.time.LocalDate.of(2026, 2, 17)));
        assertEquals("Lễ Quốc khánh 2/9", VnHolidayCalendar.holidayName(java.time.LocalDate.of(2026, 9, 2)));
        assertEquals(null, VnHolidayCalendar.holidayName(java.time.LocalDate.of(2026, 10, 20)));
    }
}
