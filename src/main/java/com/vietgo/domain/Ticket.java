package com.vietgo.domain;

import jakarta.persistence.*;

import java.time.LocalDate;

/** Ve cua mot hanh khach trong don dat ve. */
@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    /** Ma ghe ky thuat: tau "toa-so" (vd 3-12), xe "A05". */
    @Column(name = "seat_key", nullable = false, length = 10)
    private String seatKey;

    /** Ten ghe hien thi: "Toa 3 · Giường 12 (tầng 2)". */
    @Column(name = "seat_label", nullable = false, length = 80)
    private String seatLabel;

    @Column(name = "passenger_name", nullable = false, length = 100)
    private String passengerName;

    @Column(name = "passenger_type", nullable = false, length = 20)
    private String passengerType;

    @Column(name = "passenger_type_label", nullable = false, length = 60)
    private String passengerTypeLabel;

    @Column(name = "id_number", length = 20)
    private String idNumber;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "student_card_no", length = 20)
    private String studentCardNo;

    @Column(length = 15)
    private String phone;

    /** Gia goc cua ghe (truoc phu phi, giam gia). */
    @Column(name = "base_price", nullable = false)
    private long basePrice;

    /** Gia cuoi cung cua ve (sau phu phi, giam gia). */
    @Column(name = "final_price", nullable = false)
    private long finalPrice;

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

    public String getSeatKey() {
        return seatKey;
    }

    public void setSeatKey(String seatKey) {
        this.seatKey = seatKey;
    }

    public String getSeatLabel() {
        return seatLabel;
    }

    public void setSeatLabel(String seatLabel) {
        this.seatLabel = seatLabel;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }

    public String getPassengerType() {
        return passengerType;
    }

    public void setPassengerType(String passengerType) {
        this.passengerType = passengerType;
    }

    public String getPassengerTypeLabel() {
        return passengerTypeLabel;
    }

    public void setPassengerTypeLabel(String passengerTypeLabel) {
        this.passengerTypeLabel = passengerTypeLabel;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getStudentCardNo() {
        return studentCardNo;
    }

    public void setStudentCardNo(String studentCardNo) {
        this.studentCardNo = studentCardNo;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public long getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(long basePrice) {
        this.basePrice = basePrice;
    }

    public long getFinalPrice() {
        return finalPrice;
    }

    public void setFinalPrice(long finalPrice) {
        this.finalPrice = finalPrice;
    }
}
