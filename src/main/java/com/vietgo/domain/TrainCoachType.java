package com.vietgo.domain;

/**
 * Loai toa tau. Moi loai co he so gia, so cho va so tang giuong rieng.
 */
public enum TrainCoachType {
    SOFT_SEAT("Ngồi mềm điều hòa", 64, 1.00, 0, "bi-person-workspace"),
    SLEEPER_6("Giường nằm khoang 6", 42, 1.35, 3, "bi-layers"),
    SLEEPER_4("Giường nằm khoang 4", 28, 1.60, 2, "bi-door-closed");

    private final String label;
    private final int capacity;
    private final double classFactor;
    /** So tang giuong trong mot khoang (0 = toa ghe ngoi). */
    private final int tiers;
    private final String icon;

    TrainCoachType(String label, int capacity, double classFactor, int tiers, String icon) {
        this.label = label;
        this.capacity = capacity;
        this.classFactor = classFactor;
        this.tiers = tiers;
        this.icon = icon;
    }

    public String getLabel() { return label; }
    public int getCapacity() { return capacity; }
    public double getClassFactor() { return classFactor; }
    public int getTiers() { return tiers; }
    public String getIcon() { return icon; }

    public boolean isSleeper() { return tiers > 0; }

    /** So giuong moi khoang = 2 ben x so tang. */
    public int getBerthsPerCompartment() { return tiers * 2; }
}
