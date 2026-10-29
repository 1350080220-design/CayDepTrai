package com.vietgo.common;

/**
 * Ket qua tinh hoan tien khi huy ve.
 * allowed = false -> khong duoc huy (ly do trong message).
 */
public class RefundQuote {

    private final boolean allowed;
    private final String message;
    private final long paidAmount;
    private final long nonRefundable;
    private final int feePercent;
    private final long cancelFee;
    private final long refundAmount;
    private final double hoursBeforeDeparture;

    public RefundQuote(boolean allowed, String message, long paidAmount, long nonRefundable,
                       int feePercent, long cancelFee, long refundAmount, double hoursBeforeDeparture) {
        this.allowed = allowed;
        this.message = message;
        this.paidAmount = paidAmount;
        this.nonRefundable = nonRefundable;
        this.feePercent = feePercent;
        this.cancelFee = cancelFee;
        this.refundAmount = refundAmount;
        this.hoursBeforeDeparture = hoursBeforeDeparture;
    }

    public static RefundQuote denied(String message, long paid, double hours) {
        return new RefundQuote(false, message, paid, 0, 100, 0, 0, hours);
    }

    public boolean isAllowed() { return allowed; }
    public String getMessage() { return message; }
    public long getPaidAmount() { return paidAmount; }
    public long getNonRefundable() { return nonRefundable; }
    public int getFeePercent() { return feePercent; }
    public long getCancelFee() { return cancelFee; }
    public long getRefundAmount() { return refundAmount; }
    public double getHoursBeforeDeparture() { return hoursBeforeDeparture; }

    public String getHoursText() {
        long totalMin = Math.max(0, Math.round(hoursBeforeDeparture * 60));
        long d = totalMin / (60 * 24);
        long h = (totalMin % (60 * 24)) / 60;
        long m = totalMin % 60;
        if (d > 0) return d + " ngày " + h + " giờ";
        if (h > 0) return h + " giờ " + m + " phút";
        return m + " phút";
    }
}
