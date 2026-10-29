package com.vietgo.domain;

/** Loai xe khach. Moi loai co so do ghe khac nhau. */
public enum BusType {
    SEAT_45("Ghế ngồi 45 chỗ", 45, "bi-person-standing"),
    SLEEPER_36("Giường nằm 36 chỗ (2 tầng)", 36, "bi-moon-stars"),
    LIMOUSINE_9("Limousine 9 chỗ VIP", 9, "bi-gem");

    private final String label;
    private final int capacity;
    private final String icon;

    BusType(String label, int capacity, String icon) {
        this.label = label;
        this.capacity = capacity;
        this.icon = icon;
    }

    public String getLabel() { return label; }
    public int getCapacity() { return capacity; }
    public String getIcon() { return icon; }
}
