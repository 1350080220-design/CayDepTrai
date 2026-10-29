package com.vietgo.train;

import com.vietgo.common.SeatView;
import com.vietgo.domain.TrainCoachType;

import java.util.List;

/**
 * So do mot toa tau.
 * - Toa ghe ngoi: rows = 4 hang (A, B | loi di | C, D), moi hang 16 ghe.
 * - Toa giuong nam: compartments = danh sach khoang, moi khoang la danh sach tang (tren -> duoi),
 *   moi tang 2 giuong (trai, phai).
 */
public class CoachView {

    private final int coachNo;
    private final TrainCoachType type;
    private final List<SeatView> seats;
    private final List<List<SeatView>> rows;
    private final List<List<List<SeatView>>> compartments;
    private final long minPrice;
    private final long maxPrice;

    public CoachView(int coachNo, TrainCoachType type, List<SeatView> seats,
                     List<List<SeatView>> rows, List<List<List<SeatView>>> compartments) {
        this.coachNo = coachNo;
        this.type = type;
        this.seats = seats;
        this.rows = rows;
        this.compartments = compartments;
        this.minPrice = seats.stream().mapToLong(SeatView::getPrice).min().orElse(0);
        this.maxPrice = seats.stream().mapToLong(SeatView::getPrice).max().orElse(0);
    }

    public int getCoachNo() { return coachNo; }
    public TrainCoachType getType() { return type; }
    public List<SeatView> getSeats() { return seats; }
    public List<List<SeatView>> getRows() { return rows; }
    public List<List<List<SeatView>>> getCompartments() { return compartments; }
    public long getMinPrice() { return minPrice; }
    public long getMaxPrice() { return maxPrice; }

    public long getAvailable() {
        return seats.stream().filter(s -> !s.isTaken()).count();
    }

    public boolean isSleeper() {
        return type.isSleeper();
    }
}
