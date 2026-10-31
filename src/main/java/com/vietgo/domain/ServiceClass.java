package com.vietgo.domain;

/** Hang dich vu cua cho ngoi / giuong, dung de gan nhan va to mau tren giao dien. */
public enum ServiceClass {
    ECONOMY("Phổ thông", "eco"),
    STANDARD("Tiêu chuẩn", "std"),
    PREMIUM("Cao cấp", "plus"),
    VIP("VIP", "vip");

    private final String label;
    private final String css;

    ServiceClass(String label, String css) {
        this.label = label;
        this.css = css;
    }

    public String getLabel() { return label; }
    public String getCss() { return css; }
}
