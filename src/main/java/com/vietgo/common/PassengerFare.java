package com.vietgo.common;

/** Gia ve cua tung hanh khach sau khi ap dung chinh sach. */
public class PassengerFare {

    private final String seatKey;
    private final String seatLabel;
    private final String passengerName;
    private final String passengerTypeLabel;
    private final long basePrice;
    private final long surcharge;
    private final long discount;
    private final long finalPrice;

    public PassengerFare(String seatKey, String seatLabel, String passengerName, String passengerTypeLabel,
                         long basePrice, long surcharge, long discount, long finalPrice) {
        this.seatKey = seatKey;
        this.seatLabel = seatLabel;
        this.passengerName = passengerName;
        this.passengerTypeLabel = passengerTypeLabel;
        this.basePrice = basePrice;
        this.surcharge = surcharge;
        this.discount = discount;
        this.finalPrice = finalPrice;
    }

    public String getSeatKey() { return seatKey; }
    public String getSeatLabel() { return seatLabel; }
    public String getPassengerName() { return passengerName; }
    public String getPassengerTypeLabel() { return passengerTypeLabel; }
    public long getBasePrice() { return basePrice; }
    public long getSurcharge() { return surcharge; }
    public long getDiscount() { return discount; }
    public long getFinalPrice() { return finalPrice; }
}
