package com.vietgo.domain;

/** Doi tuong hanh khach xe khach. */
public enum BusPassengerType {
    ADULT("Người lớn", 0),
    CHILD("Trẻ em (5 – 11 tuổi)", 30);

    private final String label;
    private final int discountPercent;

    BusPassengerType(String label, int discountPercent) {
        this.label = label;
        this.discountPercent = discountPercent;
    }

    public String getLabel() { return label; }
    public int getDiscountPercent() { return discountPercent; }
}
