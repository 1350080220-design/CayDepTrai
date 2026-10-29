package com.vietgo.bus;

import com.vietgo.common.Money;
import com.vietgo.common.RefundQuote;
import com.vietgo.domain.Booking;
import com.vietgo.domain.BookingStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * CHINH SACH HOAN HUY VE XE KHACH.
 * - Huy truoc gio xuat ben tu 24 gio tro len: phi 10%.
 * - Huy truoc tu 2 gio den duoi 24 gio:      phi 25%.
 * - Duoi 2 gio truoc gio xuat ben:           khong nhan huy.
 * - Phi don tan noi khong duoc hoan (xe trung chuyen da dieu phoi).
 */
@Component
public class BusRefundPolicy {

    public static final int FULL_WINDOW_HOURS = 24;
    public static final int MIN_WINDOW_HOURS = 2;
    public static final int FEE_EARLY_PERCENT = 10;
    public static final int FEE_LATE_PERCENT = 25;

    public RefundQuote quote(Booking booking, LocalDateTime now) {
        long paid = booking.getTotalAmount();
        double hours = Duration.between(now, booking.getDepartureTime()).toMinutes() / 60.0;

        if (booking.getStatus() != BookingStatus.PAID) {
            return RefundQuote.denied("Chỉ vé đã thanh toán mới áp dụng chính sách hoàn tiền.", paid, hours);
        }
        if (hours < MIN_WINDOW_HOURS) {
            return RefundQuote.denied("Vé xe chỉ được hủy trước giờ xuất bến ít nhất "
                    + MIN_WINDOW_HOURS + " giờ.", paid, hours);
        }
        int feePct = hours >= FULL_WINDOW_HOURS ? FEE_EARLY_PERCENT : FEE_LATE_PERCENT;
        long nonRefundable = booking.getPickupAddress() != null ? BusFareCalculator.DOOR_PICKUP_FEE : 0;
        nonRefundable = Math.min(nonRefundable, paid);
        long refundBase = paid - nonRefundable;
        long fee = Math.min(Money.percentOf(refundBase, feePct), refundBase);
        long refund = refundBase - fee;

        String msg = hours >= FULL_WINDOW_HOURS
                ? "Hủy trước giờ xuất bến từ 24 giờ trở lên — phí hủy 10%."
                : "Hủy trước giờ xuất bến từ 2 đến dưới 24 giờ — phí hủy 25%.";
        return new RefundQuote(true, msg, paid, nonRefundable, feePct, fee, refund, hours);
    }
}
