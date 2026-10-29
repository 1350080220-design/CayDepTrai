package com.vietgo.domain;

import jakarta.persistence.*;

/** Mot dong trong bang chi tiet gia cua don (gia goc, phu phi, giam gia, phi dich vu). */
@Entity
@Table(name = "booking_fee_lines")
public class FeeLine {

    public static final String BASE = "BASE";
    public static final String SURCHARGE = "SURCHARGE";
    public static final String DISCOUNT = "DISCOUNT";
    public static final String SERVICE = "SERVICE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "line_order", nullable = false)
    private int lineOrder;

    @Column(nullable = false, length = 150)
    private String label;

    /** So tien (am neu la giam gia). */
    @Column(nullable = false)
    private long amount;

    @Column(nullable = false, length = 15)
    private String kind;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public int getLineOrder() {
        return lineOrder;
    }

    public void setLineOrder(int lineOrder) {
        this.lineOrder = lineOrder;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }
}
