package com.vietgo.admin;

/** Doanh thu mot ngay de ve bieu do. */
public class DayRevenue {

    private final String label;
    private final long train;
    private final long bus;

    public DayRevenue(String label, long train, long bus) {
        this.label = label;
        this.train = train;
        this.bus = bus;
    }

    public String getLabel() { return label; }
    public long getTrain() { return train; }
    public long getBus() { return bus; }
    public long getTotal() { return train + bus; }
}
