package com.vietgo.train;

import com.vietgo.domain.TrainCoach;
import com.vietgo.domain.TrainCoachType;
import com.vietgo.domain.TrainTrip;

import java.util.Optional;

/**
 * Quy uoc danh so cho tren tau.
 * Ma ghe (seat key) = "{soToa}-{soCho}", vi du "3-12".
 * Toa giuong: moi khoang co (2 x so tang) giuong; giuong le ben trai, chan ben phai;
 * giuong 1-2 tang 1 (duoi), 3-4 tang 2, 5-6 tang 3.
 */
public final class TrainSeatLayout {

    private TrainSeatLayout() {
    }

    public static String key(int coachNo, int seatNo) {
        return coachNo + "-" + seatNo;
    }

    /** Tach ma ghe thanh [soToa, soCho]; tra ve null neu sai dinh dang. */
    public static int[] parse(String key) {
        if (key == null || !key.matches("^\\d{1,2}-\\d{1,2}$")) return null;
        String[] p = key.split("-");
        return new int[]{Integer.parseInt(p[0]), Integer.parseInt(p[1])};
    }

    public static Optional<TrainCoach> findCoach(TrainTrip trip, int coachNo) {
        return trip.getCoaches().stream().filter(c -> c.getCoachNo() == coachNo).findFirst();
    }

    /** Kiem tra ghe co ton tai tren chuyen tau hay khong. */
    public static boolean exists(TrainTrip trip, String key) {
        int[] p = parse(key);
        if (p == null) return false;
        Optional<TrainCoach> coach = findCoach(trip, p[0]);
        return coach.isPresent() && p[1] >= 1 && p[1] <= coach.get().getCoachType().getCapacity();
    }

    /** Tang cua giuong (1 = duoi). Ghe ngoi tra ve 0. */
    public static int tierOf(TrainCoachType type, int seatNo) {
        if (!type.isSleeper()) return 0;
        int idx = (seatNo - 1) % type.getBerthsPerCompartment();
        return idx / 2 + 1;
    }

    public static int compartmentOf(TrainCoachType type, int seatNo) {
        if (!type.isSleeper()) return 0;
        return (seatNo - 1) / type.getBerthsPerCompartment() + 1;
    }

    public static String label(TrainCoachType type, int coachNo, int seatNo) {
        if (!type.isSleeper()) {
            return "Toa " + coachNo + " · Ghế " + seatNo;
        }
        return "Toa " + coachNo + " · Giường " + seatNo
                + " (khoang " + compartmentOf(type, seatNo) + ", tầng " + tierOf(type, seatNo) + ")";
    }

    public static String label(TrainTrip trip, String key) {
        int[] p = parse(key);
        if (p == null) return key;
        return findCoach(trip, p[0])
                .map(c -> label(c.getCoachType(), p[0], p[1]))
                .orElse(key);
    }
}
