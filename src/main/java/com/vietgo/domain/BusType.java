package com.vietgo.domain;

import java.util.List;

import static com.vietgo.domain.Amenity.*;

/** Loai xe khach. Moi loai co so do ghe va hang dich vu khac nhau. */
public enum BusType {
    SEAT_45("Ghế ngồi 45 chỗ", "Ghế", ServiceClass.ECONOMY, 45, false, "bi-person-standing",
            List.of(AIRCON, WATER)),
    SLEEPER_36("Giường nằm 36 chỗ (2 tầng)", "Giường", ServiceClass.STANDARD, 36, true, "bi-moon-stars",
            List.of(AIRCON, BLANKET, USB, WATER, WC)),
    CABIN_24("Phòng nằm cabin 24 phòng", "Phòng", ServiceClass.PREMIUM, 24, true, "bi-door-closed",
            List.of(AIRCON, CURTAIN, TV, USB, BLANKET, WIFI, WC)),
    LIMOUSINE_9("Limousine 9 chỗ VIP", "Ghế", ServiceClass.VIP, 9, false, "bi-gem",
            List.of(AIRCON, MASSAGE, WIFI, USB, WATER));

    private final String label;
    /** Ten goi mot cho tren loai xe nay: "Ghế", "Giường", "Phòng". */
    private final String unit;
    private final ServiceClass serviceClass;
    private final int capacity;
    /** Xe 2 tang: tang duoi ma A, tang tren ma B. */
    private final boolean doubleDeck;
    private final String icon;
    private final List<Amenity> amenities;

    BusType(String label, String unit, ServiceClass serviceClass, int capacity, boolean doubleDeck, String icon,
            List<Amenity> amenities) {
        this.label = label;
        this.unit = unit;
        this.serviceClass = serviceClass;
        this.capacity = capacity;
        this.doubleDeck = doubleDeck;
        this.icon = icon;
        this.amenities = amenities;
    }

    public String getLabel() { return label; }
    public String getUnit() { return unit; }
    public ServiceClass getServiceClass() { return serviceClass; }
    public int getCapacity() { return capacity; }
    public boolean isDoubleDeck() { return doubleDeck; }
    public String getIcon() { return icon; }
    public List<Amenity> getAmenities() { return amenities; }
}
