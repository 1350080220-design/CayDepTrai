package com.vietgo.domain;

import jakarta.persistence.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Mot chuyen tau (mot doan tau chay mot hanh trinh vao mot thoi diem). */
@Entity
@Table(name = "train_trips")
public class TrainTrip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Mac tau, vi du SE1, SE3, TN2. */
    @Column(name = "train_code", nullable = false, length = 10)
    private String trainCode;

    @Column(name = "origin_station", nullable = false, length = 60)
    private String originStation;

    @Column(name = "destination_station", nullable = false, length = 60)
    private String destinationStation;

    @Column(name = "departure_time", nullable = false)
    private LocalDateTime departureTime;

    @Column(name = "arrival_time", nullable = false)
    private LocalDateTime arrivalTime;

    /** Cu ly hanh trinh (km) - dau vao cua thuat toan tinh gia theo bac cu ly. */
    @Column(name = "distance_km", nullable = false)
    private int distanceKm;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("coachNo ASC")
    private List<TrainCoach> coaches = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTrainCode() {
        return trainCode;
    }

    public void setTrainCode(String trainCode) {
        this.trainCode = trainCode;
    }

    public String getOriginStation() {
        return originStation;
    }

    public void setOriginStation(String originStation) {
        this.originStation = originStation;
    }

    public String getDestinationStation() {
        return destinationStation;
    }

    public void setDestinationStation(String destinationStation) {
        this.destinationStation = destinationStation;
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

    public int getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(int distanceKm) {
        this.distanceKm = distanceKm;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<TrainCoach> getCoaches() {
        return coaches;
    }

    public void setCoaches(List<TrainCoach> coaches) {
        this.coaches = coaches;
    }

    public void addCoach(int coachNo, TrainCoachType type) {
        TrainCoach c = new TrainCoach();
        c.setTrip(this);
        c.setCoachNo(coachNo);
        c.setCoachType(type);
        coaches.add(c);
    }

    public int getCapacity() {
        return coaches.stream().mapToInt(c -> c.getCoachType().getCapacity()).sum();
    }

    public long getDurationMinutes() {
        if (departureTime == null || arrivalTime == null) return 0;
        return Duration.between(departureTime, arrivalTime).toMinutes();
    }

    /** Gio khoi hanh tinh bang phut trong ngay (dung de sap xep tren giao dien). */
    public int getDepartureMinuteOfDay() {
        return departureTime == null ? 0 : departureTime.getHour() * 60 + departureTime.getMinute();
    }

    public String getDurationText() {
        if (departureTime == null || arrivalTime == null) return "";
        Duration d = Duration.between(departureTime, arrivalTime);
        long h = d.toHours();
        long m = d.toMinutesPart();
        return m == 0 ? h + " giờ" : h + " giờ " + m + " phút";
    }

    /** Mo ta co cau toa, vd "2 ngồi mềm · 1 khoang 6 · 1 khoang 4". */
    public String getCoachSummary() {
        List<String> parts = new ArrayList<>();
        for (TrainCoachType type : TrainCoachType.values()) {
            long n = countCoaches(type);
            if (n > 0) parts.add(n + " " + type.getShortLabel());
        }
        return String.join(" · ", parts);
    }

    /** Cac hang toa co tren doan tau, theo thu tu tu re den dat. */
    public List<TrainCoachType> getCoachTypes() {
        List<TrainCoachType> types = new ArrayList<>();
        for (TrainCoachType type : TrainCoachType.values()) {
            if (countCoaches(type) > 0) types.add(type);
        }
        return types;
    }

    public boolean hasCoachType(String typeName) {
        return coaches.stream().anyMatch(c -> c.getCoachType().name().equals(typeName));
    }

    public long countCoaches(TrainCoachType type) {
        return coaches.stream().filter(c -> c.getCoachType() == type).count();
    }
}
