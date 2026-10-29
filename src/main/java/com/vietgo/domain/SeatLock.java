package com.vietgo.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Khoa ghe: moi ghe cua moi chuyen chi co toi da 1 dong (rang buoc UNIQUE o database),
 * nho do hai nguoi dat cung luc khong the cung giu mot ghe.
 * Dong bi xoa khi don het han giu cho hoac bi huy.
 */
@Entity
@Table(name = "seat_locks",
        uniqueConstraints = @UniqueConstraint(name = "uk_seat_lock",
                columnNames = {"ticket_type", "trip_id", "seat_key"}))
public class SeatLock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_type", nullable = false, length = 10)
    private TicketType ticketType;

    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    @Column(name = "seat_key", nullable = false, length = 10)
    private String seatKey;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TicketType getTicketType() {
        return ticketType;
    }

    public void setTicketType(TicketType ticketType) {
        this.ticketType = ticketType;
    }

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public String getSeatKey() {
        return seatKey;
    }

    public void setSeatKey(String seatKey) {
        this.seatKey = seatKey;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
