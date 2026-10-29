package com.vietgo.common;

/** Tien te VND: lam tron, tinh phan tram. */
public final class Money {

    private Money() {
    }

    /** Lam tron den 1.000d gan nhat (gia ve thuong niem yet theo nghin dong). */
    public static long roundThousand(double value) {
        return Math.round(value / 1000.0) * 1000L;
    }

    /** Tinh percent% cua amount, lam tron den 1.000d. */
    public static long percentOf(long amount, int percent) {
        return roundThousand(amount * percent / 100.0);
    }
}
