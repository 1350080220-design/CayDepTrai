package com.vietgo.common;

import java.util.List;

/** Ket qua tinh gia cua mot don: chi tiet tung khach + bang phi tong hop + tong tien. */
public class FareQuote {

    private final List<PassengerFare> passengers;
    private final List<FeeItem> items;
    private final long total;

    public FareQuote(List<PassengerFare> passengers, List<FeeItem> items, long total) {
        this.passengers = passengers;
        this.items = items;
        this.total = total;
    }

    public List<PassengerFare> getPassengers() { return passengers; }
    public List<FeeItem> getItems() { return items; }
    public long getTotal() { return total; }

    public long sumOf(String kind) {
        return items.stream().filter(i -> i.getKind().equals(kind)).mapToLong(FeeItem::getAmount).sum();
    }
}
