package com.vietgo.train;

import java.util.ArrayList;
import java.util.List;

/** Ban nhap dat ve tau luu trong session giua cac buoc. */
public class TrainBookingDraft {

    public static final String SESSION_KEY = "TRAIN_DRAFT";

    private Long tripId;
    private List<String> seatKeys = new ArrayList<>();
    private TrainPassengerForm form;

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public List<String> getSeatKeys() {
        return seatKeys;
    }

    public void setSeatKeys(List<String> seatKeys) {
        this.seatKeys = seatKeys;
    }

    public TrainPassengerForm getForm() {
        return form;
    }

    public void setForm(TrainPassengerForm form) {
        this.form = form;
    }
}
