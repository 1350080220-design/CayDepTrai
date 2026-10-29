package com.vietgo.domain;

import jakarta.persistence.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Don dat ve. Mot don gom nhieu ve (moi hanh khach mot ve) cung mot chuyen.
 * Vong doi: HELD (giu cho) -> PAID (thanh toan) -> CANCELLED (huy, hoan tien)
 *           HELD -> EXPIRED (qua han giu cho) | CANCELLED (huy giu cho)
 */
@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 12)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_type", nullable = false, length = 10)
    private TicketType ticketType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private BookingStatus status = BookingStatus.HELD;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_trip_id")
    private TrainTrip trainTrip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bus_trip_id")
    private BusTrip busTrip;

    @Column(name = "contact_name", nullable = false, length = 100)
    private String contactName;

    @Column(name = "contact_phone", nullable = false, length = 15)
    private String contactPhone;

    @Column(name = "contact_email", nullable = false, length = 120)
    private String contactEmail;

    @Column(name = "passenger_count", nullable = false)
    private int passengerCount;

    @Column(name = "total_amount", nullable = false)
    private long totalAmount;

    @Column(name = "hold_expires_at")
    private LocalDateTime holdExpiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancel_fee", nullable = false)
    private long cancelFee;

    @Column(name = "refund_amount", nullable = false)
    private long refundAmount;

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    // ---- Rieng cho xe khach ----
    @Column(name = "pickup_point", length = 150)
    private String pickupPoint;

    @Column(name = "pickup_address", length = 200)
    private String pickupAddress;

    @Column(name = "dropoff_point", length = 150)
    private String dropoffPoint;

    @Column(name = "luggage_kg", nullable = false)
    private int luggageKg;

    @Column(length = 500)
    private String note;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<Ticket> tickets = new ArrayList<>();

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineOrder ASC")
    private List<FeeLine> feeLines = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public TicketType getTicketType() {
        return ticketType;
    }

    public void setTicketType(TicketType ticketType) {
        this.ticketType = ticketType;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public TrainTrip getTrainTrip() {
        return trainTrip;
    }

    public void setTrainTrip(TrainTrip trainTrip) {
        this.trainTrip = trainTrip;
    }

    public BusTrip getBusTrip() {
        return busTrip;
    }

    public void setBusTrip(BusTrip busTrip) {
        this.busTrip = busTrip;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public int getPassengerCount() {
        return passengerCount;
    }

    public void setPassengerCount(int passengerCount) {
        this.passengerCount = passengerCount;
    }

    public long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDateTime getHoldExpiresAt() {
        return holdExpiresAt;
    }

    public void setHoldExpiresAt(LocalDateTime holdExpiresAt) {
        this.holdExpiresAt = holdExpiresAt;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public long getCancelFee() {
        return cancelFee;
    }

    public void setCancelFee(long cancelFee) {
        this.cancelFee = cancelFee;
    }

    public long getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(long refundAmount) {
        this.refundAmount = refundAmount;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public String getPickupPoint() {
        return pickupPoint;
    }

    public void setPickupPoint(String pickupPoint) {
        this.pickupPoint = pickupPoint;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public String getDropoffPoint() {
        return dropoffPoint;
    }

    public void setDropoffPoint(String dropoffPoint) {
        this.dropoffPoint = dropoffPoint;
    }

    public int getLuggageKg() {
        return luggageKg;
    }

    public void setLuggageKg(int luggageKg) {
        this.luggageKg = luggageKg;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<Ticket> getTickets() {
        return tickets;
    }

    public void setTickets(List<Ticket> tickets) {
        this.tickets = tickets;
    }

    public List<FeeLine> getFeeLines() {
        return feeLines;
    }

    public void setFeeLines(List<FeeLine> feeLines) {
        this.feeLines = feeLines;
    }

    public void addTicket(Ticket t) {
        t.setBooking(this);
        tickets.add(t);
    }

    public void addFeeLine(String label, long amount, String kind) {
        FeeLine f = new FeeLine();
        f.setBooking(this);
        f.setLineOrder(feeLines.size() + 1);
        f.setLabel(label);
        f.setAmount(amount);
        f.setKind(kind);
        feeLines.add(f);
    }

    public boolean isTrain() {
        return ticketType == TicketType.TRAIN;
    }

    public boolean isBus() {
        return ticketType == TicketType.BUS;
    }

    public LocalDateTime getDepartureTime() {
        if (isTrain() && trainTrip != null) return trainTrip.getDepartureTime();
        if (isBus() && busTrip != null) return busTrip.getDepartureTime();
        return null;
    }

    public LocalDateTime getArrivalTime() {
        if (isTrain() && trainTrip != null) return trainTrip.getArrivalTime();
        if (isBus() && busTrip != null) return busTrip.getArrivalTime();
        return null;
    }

    public String getOrigin() {
        if (isTrain() && trainTrip != null) return trainTrip.getOriginStation();
        if (isBus() && busTrip != null) return busTrip.getOriginCity();
        return "";
    }

    public String getDestination() {
        if (isTrain() && trainTrip != null) return trainTrip.getDestinationStation();
        if (isBus() && busTrip != null) return busTrip.getDestinationCity();
        return "";
    }

    public String getVehicleName() {
        if (isTrain() && trainTrip != null) return "Tàu " + trainTrip.getTrainCode();
        if (isBus() && busTrip != null) return busTrip.getOperatorName();
        return "";
    }

    public Long getTripId() {
        if (isTrain() && trainTrip != null) return trainTrip.getId();
        if (isBus() && busTrip != null) return busTrip.getId();
        return null;
    }

    /** So giay con lai cua thoi gian giu cho (0 neu da het). */
    public long getHoldSecondsLeft() {
        if (status != BookingStatus.HELD || holdExpiresAt == null) return 0;
        long s = Duration.between(LocalDateTime.now(), holdExpiresAt).getSeconds();
        return Math.max(0, s);
    }

    public boolean isHeld() {
        return status == BookingStatus.HELD;
    }

    public boolean isPaid() {
        return status == BookingStatus.PAID;
    }
}
