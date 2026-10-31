package com.vietgo.domain;

/** Tien ich tren phuong tien, hien thi o ket qua tim kiem va trang chon cho. */
public enum Amenity {
    AIRCON("Điều hòa", "bi-snow"),
    WIFI("Wi-Fi miễn phí", "bi-wifi"),
    USB("Sạc USB", "bi-usb-plug"),
    WATER("Nước uống", "bi-cup-straw"),
    BLANKET("Chăn gối", "bi-moon-stars"),
    WC("Nhà vệ sinh", "bi-badge-wc"),
    TV("Màn hình giải trí", "bi-tv"),
    MASSAGE("Ghế massage", "bi-stars"),
    CURTAIN("Rèm riêng tư", "bi-door-closed"),
    LAMP("Đèn đọc sách", "bi-lamp"),
    MEAL("Suất ăn nhẹ", "bi-egg-fried");

    private final String label;
    private final String icon;

    Amenity(String label, String icon) {
        this.label = label;
        this.icon = icon;
    }

    public String getLabel() { return label; }
    public String getIcon() { return icon; }
}
