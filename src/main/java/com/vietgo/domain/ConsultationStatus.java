package com.vietgo.domain;

/** Trang thai lich hen tu van. */
public enum ConsultationStatus {
    PENDING("Chờ xác nhận", "held"),
    CONFIRMED("Đã xác nhận", "paid"),
    DONE("Đã tư vấn", "done"),
    CANCELLED("Đã hủy", "cancelled");

    private final String label;
    private final String css;

    ConsultationStatus(String label, String css) {
        this.label = label;
        this.css = css;
    }

    public String getLabel() { return label; }
    public String getCss() { return css; }

    public boolean isActive() { return this == PENDING || this == CONFIRMED; }
}
