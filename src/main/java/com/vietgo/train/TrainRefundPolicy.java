package com.vietgo.train;

import com.vietgo.common.Money;
import com.vietgo.common.RefundQuote;
import com.vietgo.domain.Booking;
import com.vietgo.domain.BookingStatus;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * CHINH SACH HOAN HUY VE TAU HOA.
 * - Huy truoc gio tau chay tu 24 gio tro len: phi 10%.
 * - Huy truoc tu 4 gio den duoi 24 gio:      phi 20%.
 * - Duoi 4 gio truoc gio chay:               khong nhan huy, tra ve.
 * - Phi bao hiem hanh khach khong duoc hoan.
 * - Phi huy toi thieu 10.000d/ve.
 */
@Component
public class TrainRefundPolicy {

    public static final int FULL_WINDOW_HOURS = 24;
    public static final int MIN_WINDOW_HOURS = 4;
    public static final int FEE_EARLY_PERCENT = 10;
    public static final int FEE_LATE_PERCENT = 20;
    public static final long MIN_FEE_PER_TICKET = 10_000;

    public RefundQuote quote(Booking booking, LocalDateTime now) {
        long paid = booking.getTotalAmount();
        double hours = Duration.between(now, booking.getDepartureTime()).toMinutes() / 60.0;

        if (booking.getStatus() != BookingStatus.PAID) {
            return RefundQuote.denied("Chỉ vé đã thanh toán mới áp dụng chính sách hoàn tiền.", paid, hours);
        }
        if (hours < MIN_WINDOW_HOURS) {
            return RefundQuote.denied("Vé tàu chỉ được hủy trước giờ tàu chạy ít nhất "
                    + MIN_WINDOW_HOURS + " giờ.", paid, hours);
        }

        int feePct = hours >= FULL_WINDOW_HOURS ? FEE_EARLY_PERCENT : FEE_LATE_PERCENT;
        long nonRefundable = TrainFareCalculator.INSURANCE_PER_PASSENGER * booking.getPassengerCount();
        long refundBase = Math.max(0, paid - nonRefundable);
        long fee = Math.max(Money.percentOf(refundBase, feePct), MIN_FEE_PER_TICKET * booking.getPassengerCount());
        fee = Math.min(fee, refundBase);
        long refund = refundBase - fee;

        String msg = hours >= FULL_WINDOW_HOURS
                ? "Hủy trước giờ chạy từ 24 giờ trở lên — phí hủy 10%."
                : "Hủy trước giờ chạy từ 4 đến dưới 24 giờ — phí hủy 20%.";
        return new RefundQuote(true, msg, paid, nonRefundable, feePct, fee, refund, hours);
    }
}
