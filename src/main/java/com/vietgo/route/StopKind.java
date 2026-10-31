package com.vietgo.route;

/** Loai diem tren lo trinh. */
public enum StopKind {
    ORIGIN("Khởi hành", "bi-geo-alt-fill"),
    STATION("Ga dừng", "bi-signpost-2"),
    PASS("Đi qua", "bi-dot"),
    REST("Trạm dừng nghỉ", "bi-cup-hot"),
    DESTINATION("Điểm đến", "bi-flag-fill");

    private final String label;
    private final String icon;

    StopKind(String label, String icon) {
        this.label = label;
        this.icon = icon;
    }

    public String getLabel() { return label; }
    public String getIcon() { return icon; }
    public String getCss() { return name().toLowerCase(); }
}
