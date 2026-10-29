package com.vietgo.domain;

import jakarta.persistence.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/** Mot chuyen xe khach. */
@Entity
@Table(name = "bus_trips")
public class BusTrip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Ten nha xe. */
    @Column(name = "operator_name", nullable = false, length = 80)
    private String operatorName;

    @Column(name = "license_plate", nullable = false, length = 15)
    private String licensePlate;

    @Column(name = "origin_city", nullable = false, length = 60)
    private String originCity;

    @Column(name = "destination_city", nullable = false, length = 60)
    private String destinationCity;

    @Column(name = "departure_time", nullable = false)
    private LocalDateTime departureTime;

    @Column(name = "arrival_time", nullable = false)
    private LocalDateTime arrivalTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "bus_type", nullable = false, length = 20)
    private BusType busType;

    /** Gia ve co ban mot ghe (VND). */
    @Column(name = "base_price", nullable = false)
    private long basePrice;

    /** Cac diem don, ngan cach bang dau ';'. */
    @Column(name = "pickup_points", nullable = false, length = 500)
    private String pickupPoints;

    /** Cac diem tra, ngan cach bang dau ';'. */
    @Column(name = "dropoff_points", nullable = false, length = 500)
    private String dropoffPoints;

    @Column(nullable = false)
    private boolean active = true;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public String getOriginCity() {
        return originCity;
    }

    public void setOriginCity(String originCity) {
        this.originCity = originCity;
    }

    public String getDestinationCity() {
        return destinationCity;
    }

    public void setDestinationCity(String destinationCity) {
        this.destinationCity = destinationCity;
    }

    public LocalDateTime getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(LocalDateTime departureTime) {
        this.departureTime = departureTime;
    }

    public LocalDateTime getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(LocalDateTime arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public BusType getBusType() {
        return busType;
    }

    public void setBusType(BusType busType) {
        this.busType = busType;
    }

    public long getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(long basePrice) {
        this.basePrice = basePrice;
    }

    public String getPickupPoints() {
        return pickupPoints;
    }

    public void setPickupPoints(String pickupPoints) {
        this.pickupPoints = pickupPoints;
    }

    public String getDropoffPoints() {
        return dropoffPoints;
    }

    public void setDropoffPoints(String dropoffPoints) {
        this.dropoffPoints = dropoffPoints;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<String> getPickupPointList() {
        return split(pickupPoints);
    }

    public List<String> getDropoffPointList() {
        return split(dropoffPoints);
    }

    private static List<String> split(String s) {
        if (s == null || s.isBlank()) return List.of();
        return Arrays.stream(s.split(";")).map(String::trim).filter(x -> !x.isEmpty()).toList();
    }

    public String getDurationText() {
        if (departureTime == null || arrivalTime == null) return "";
        Duration d = Duration.between(departureTime, arrivalTime);
        long h = d.toHours();
        long m = d.toMinutesPart();
        return m == 0 ? h + " giờ" : h + " giờ " + m + " phút";
    }

    public int getCapacity() {
        return busType == null ? 0 : busType.getCapacity();
    }
}
