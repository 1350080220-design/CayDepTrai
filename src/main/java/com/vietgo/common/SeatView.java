package com.vietgo.common;

/** Thong tin mot ghe/giuong de ve so do cho. */
public class SeatView {

    private final String key;
    private final int number;
    private final String label;
    /** Tang giuong (1 = tang duoi). 0 voi ghe ngoi. */
    private final int tier;
    private final long price;
    private final boolean taken;

    public SeatView(String key, int number, String label, int tier, long price, boolean taken) {
        this.key = key;
        this.number = number;
        this.label = label;
        this.tier = tier;
        this.price = price;
        this.taken = taken;
    }

    public String getKey() { return key; }
    public int getNumber() { return number; }
    public String getLabel() { return label; }
    public int getTier() { return tier; }
    public long getPrice() { return price; }
    public boolean isTaken() { return taken; }
}
