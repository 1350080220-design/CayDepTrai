package com.vietgo.bus;

import java.util.ArrayList;
import java.util.List;

/** Ban nhap dat ve xe luu trong session giua cac buoc. */
public class BusBookingDraft {

    public static final String SESSION_KEY = "BUS_DRAFT";

    private Long tripId;
    private List<String> seatCodes = new ArrayList<>();
    private BusPassengerForm form;

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public List<String> getSeatCodes() {
        return seatCodes;
    }

    public void setSeatCodes(List<String> seatCodes) {
        this.seatCodes = seatCodes;
    }

    public BusPassengerForm getForm() {
        return form;
    }

    public void setForm(BusPassengerForm form) {
        this.form = form;
    }
}
