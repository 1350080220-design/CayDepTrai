package com.vietgo.domain;

/** Hinh thuc tu van. */
public enum ConsultationChannel {
    OFFICE("Tại văn phòng", "bi-building"),
    PHONE("Gọi điện thoại", "bi-telephone"),
    VIDEO("Video call", "bi-camera-video");

    private final String label;
    private final String icon;

    ConsultationChannel(String label, String icon) {
        this.label = label;
        this.icon = icon;
    }

    public String getLabel() { return label; }
    public String getIcon() { return icon; }
}
