package com.vietgo.consultation;

/** Mot khung gio tu van va so cho con lai. */
public class SlotView {

    private final String time;
    private final int remaining;
    private final boolean available;
    private final String reason;

    public SlotView(String time, int remaining, boolean available, String reason) {
        this.time = time;
        this.remaining = remaining;
        this.available = available;
        this.reason = reason;
    }

    public String getTime() { return time; }
    public int getRemaining() { return remaining; }
    public boolean isAvailable() { return available; }
    public String getReason() { return reason; }
}
