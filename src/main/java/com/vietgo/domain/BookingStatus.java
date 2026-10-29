package com.vietgo.domain;

/** Trang thai don dat ve. HELD = dang giu cho (chua thanh toan). */
public enum BookingStatus {
    HELD("Đang giữ chỗ", "held"),
    PAID("Đã thanh toán", "paid"),
    CANCELLED("Đã hủy", "cancelled"),
    EXPIRED("Hết hạn giữ chỗ", "expired");

    private final String label;
    private final String css;

    BookingStatus(String label, String css) {
        this.label = label;
        this.css = css;
    }

    public String getLabel() { return label; }
    public String getCss() { return css; }
}
