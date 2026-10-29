package com.vietgo.common;

/** Mot dong trong bang tinh gia. amount am = giam gia. */
public class FeeItem {

    private final String label;
    private final long amount;
    private final String kind;
    private final String note;

    public FeeItem(String label, long amount, String kind, String note) {
        this.label = label;
        this.amount = amount;
        this.kind = kind;
        this.note = note;
    }

    public String getLabel() { return label; }
    public long getAmount() { return amount; }
    public String getKind() { return kind; }
    public String getNote() { return note; }

    public boolean isDiscount() { return amount < 0; }
}
