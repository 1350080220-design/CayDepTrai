package com.vietgo.domain;

/**
 * Doi tuong hanh khach tau hoa va muc giam gia (khong cong don).
 * Tuoi tinh theo ngay khoi hanh.
 */
public enum TrainPassengerType {
    ADULT("Người lớn", 0, 10, 150),
    CHILD("Trẻ em (6 – dưới 10 tuổi)", 25, 6, 9),
    STUDENT("Học sinh, sinh viên", 10, 10, 35),
    ELDERLY("Người cao tuổi (từ 60)", 15, 60, 150);

    private final String label;
    private final int discountPercent;
    private final int minAge;
    private final int maxAge;

    TrainPassengerType(String label, int discountPercent, int minAge, int maxAge) {
        this.label = label;
        this.discountPercent = discountPercent;
        this.minAge = minAge;
        this.maxAge = maxAge;
    }

    public String getLabel() { return label; }
    public int getDiscountPercent() { return discountPercent; }
    public int getMinAge() { return minAge; }
    public int getMaxAge() { return maxAge; }
}
