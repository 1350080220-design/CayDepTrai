package com.vietgo.domain;

import java.util.List;

import static com.vietgo.domain.Amenity.*;

/**
 * Loai toa tau. Moi loai co he so gia, so cho, so tang giuong va hang dich vu rieng.
 * Toa ghe ngoi: seatsAcross = so ghe tren mot hang ngang, loi di nam sau ghe thu aisleAfter.
 */
public enum TrainCoachType {
    HARD_SEAT("Ngồi cứng điều hòa", "ngồi cứng", ServiceClass.ECONOMY, 80, 0.80, 0, 5, "bi-person",
            List.of(AIRCON, WATER)),
    SOFT_SEAT("Ngồi mềm điều hòa", "ngồi mềm", ServiceClass.STANDARD, 64, 1.00, 0, 4, "bi-person-workspace",
            List.of(AIRCON, USB, WATER)),
    SLEEPER_6("Giường nằm khoang 6", "khoang 6", ServiceClass.STANDARD, 42, 1.35, 3, 0, "bi-layers",
            List.of(AIRCON, BLANKET, USB, LAMP)),
    SLEEPER_4("Giường nằm khoang 4", "khoang 4", ServiceClass.PREMIUM, 28, 1.60, 2, 0, "bi-door-closed",
            List.of(AIRCON, BLANKET, USB, LAMP, WATER, WIFI)),
    VIP_2("Khoang VIP 2 giường", "VIP 2 giường", ServiceClass.VIP, 14, 2.40, 1, 0, "bi-gem",
            List.of(AIRCON, BLANKET, TV, WIFI, MEAL, CURTAIN, USB));

    public static final int SEAT_AISLE_AFTER = 2;

    private final String label;
    private final String shortLabel;
    private final ServiceClass serviceClass;
    private final int capacity;
    private final double classFactor;
    /** So tang giuong trong mot khoang (0 = toa ghe ngoi). */
    private final int tiers;
    /** So ghe tren mot hang ngang cua toa ghe ngoi (0 voi toa giuong). */
    private final int seatsAcross;
    private final String icon;
    private final List<Amenity> amenities;

    TrainCoachType(String label, String shortLabel, ServiceClass serviceClass, int capacity, double classFactor,
                   int tiers, int seatsAcross, String icon, List<Amenity> amenities) {
        this.label = label;
        this.shortLabel = shortLabel;
        this.serviceClass = serviceClass;
        this.capacity = capacity;
        this.classFactor = classFactor;
        this.tiers = tiers;
        this.seatsAcross = seatsAcross;
        this.icon = icon;
        this.amenities = amenities;
    }

    public String getLabel() { return label; }
    public String getShortLabel() { return shortLabel; }
    public ServiceClass getServiceClass() { return serviceClass; }
    public int getCapacity() { return capacity; }
    public double getClassFactor() { return classFactor; }
    public int getTiers() { return tiers; }
    public int getSeatsAcross() { return seatsAcross; }
    public int getAisleAfter() { return SEAT_AISLE_AFTER; }
    public String getIcon() { return icon; }
    public List<Amenity> getAmenities() { return amenities; }

    public boolean isSleeper() { return tiers > 0; }

    /** So giuong moi khoang = 2 ben x so tang. */
    public int getBerthsPerCompartment() { return tiers * 2; }
}
