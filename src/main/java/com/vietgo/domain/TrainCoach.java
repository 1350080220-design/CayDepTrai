package com.vietgo.domain;

import jakarta.persistence.*;

/** Mot toa trong doan tau. */
@Entity
@Table(name = "train_coaches",
        uniqueConstraints = @UniqueConstraint(name = "uk_coach_trip_no", columnNames = {"trip_id", "coach_no"}))
public class TrainCoach {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private TrainTrip trip;

    @Column(name = "coach_no", nullable = false)
    private int coachNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "coach_type", nullable = false, length = 20)
    private TrainCoachType coachType;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TrainTrip getTrip() {
        return trip;
    }

    public void setTrip(TrainTrip trip) {
        this.trip = trip;
    }

    public int getCoachNo() {
        return coachNo;
    }

    public void setCoachNo(int coachNo) {
        this.coachNo = coachNo;
    }

    public TrainCoachType getCoachType() {
        return coachType;
    }

    public void setCoachType(TrainCoachType coachType) {
        this.coachType = coachType;
    }
}
