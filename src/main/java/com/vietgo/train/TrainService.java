package com.vietgo.train;

import com.vietgo.common.Money;
import com.vietgo.common.NotFoundException;
import com.vietgo.common.SeatView;
import com.vietgo.domain.TicketType;
import com.vietgo.domain.TrainCoach;
import com.vietgo.domain.TrainCoachType;
import com.vietgo.domain.TrainTrip;
import com.vietgo.repository.SeatLockRepository;
import com.vietgo.repository.TrainTripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** Nghiep vu tra cuu chuyen tau, so do cho va goi y cho. */
@Service
@Transactional(readOnly = true)
public class TrainService {

    /** Danh sach ga theo thu tu Bac - Nam. */
    public static final List<String> STATIONS = List.of(
            "Hà Nội", "Ninh Bình", "Thanh Hóa", "Vinh", "Đồng Hới", "Huế", "Đà Nẵng",
            "Quảng Ngãi", "Quy Nhơn (Diêu Trì)", "Nha Trang", "Phan Thiết", "Sài Gòn");

    private final TrainTripRepository tripRepo;
    private final SeatLockRepository lockRepo;
    private final TrainFareCalculator fare;

    public TrainService(TrainTripRepository tripRepo, SeatLockRepository lockRepo, TrainFareCalculator fare) {
        this.tripRepo = tripRepo;
        this.lockRepo = lockRepo;
        this.fare = fare;
    }

    public List<TrainTrip> search(String origin, String destination, LocalDate date) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        if (from.isBefore(now)) from = now;
        return tripRepo.search(origin, destination, from, date.plusDays(1).atStartOfDay());
    }

    public TrainTrip getTrip(Long id) {
        return tripRepo.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy chuyến tàu"));
    }

    public Set<String> takenSeats(Long tripId) {
        return new HashSet<>(lockRepo.findSeatKeys(TicketType.TRAIN, tripId));
    }

    public long availableSeats(TrainTrip trip) {
        return trip.getCapacity() - lockRepo.countByTicketTypeAndTripId(TicketType.TRAIN, trip.getId());
    }

    /** Gia thap nhat cua chuyen (ghe re nhat) - hien thi o trang ket qua. */
    public long minPrice(TrainTrip trip) {
        return trip.getCoaches().stream()
                .mapToLong(c -> {
                    TrainCoachType t = c.getCoachType();
                    int cheapestSeat = t.isSleeper() ? t.getTiers() * 2 : 1; // tang tren cung re nhat
                    return displayPrice(trip, t, cheapestSeat);
                })
                .min().orElse(0);
    }

    /** Gia hien thi cho nguoi lon tren so do (gia goc + phu phi ngay chay). */
    public long displayPrice(TrainTrip trip, TrainCoachType type, int seatNo) {
        long base = fare.seatBasePrice(trip, type, seatNo);
        int pct = fare.surchargePercent(trip.getDepartureTime().toLocalDate());
        return base + Money.percentOf(base, pct);
    }

    /** Lap so do tung toa. */
    public List<CoachView> buildCoachViews(TrainTrip trip) {
        Set<String> taken = takenSeats(trip.getId());
        List<CoachView> views = new ArrayList<>();
        for (TrainCoach coach : trip.getCoaches()) {
            TrainCoachType type = coach.getCoachType();
            List<SeatView> seats = new ArrayList<>();
            for (int no = 1; no <= type.getCapacity(); no++) {
                String key = TrainSeatLayout.key(coach.getCoachNo(), no);
                seats.add(new SeatView(key, no, TrainSeatLayout.label(type, coach.getCoachNo(), no),
                        TrainSeatLayout.tierOf(type, no), displayPrice(trip, type, no), taken.contains(key)));
            }

            List<List<SeatView>> rows = new ArrayList<>();
            List<List<List<SeatView>>> compartments = new ArrayList<>();
            if (type.isSleeper()) {
                int per = type.getBerthsPerCompartment();
                for (int c = 0; c < type.getCapacity() / per; c++) {
                    List<List<SeatView>> tiers = new ArrayList<>();
                    for (int tier = type.getTiers(); tier >= 1; tier--) { // ve tang tren truoc
                        int left = c * per + (tier - 1) * 2; // index 0-based
                        tiers.add(List.of(seats.get(left), seats.get(left + 1)));
                    }
                    compartments.add(tiers);
                }
            } else {
                // 4 hang ngang: ghe so 4k+1 (hang A), 4k+2 (B), 4k+3 (C), 4k+4 (D)
                for (int r = 0; r < 4; r++) {
                    List<SeatView> row = new ArrayList<>();
                    for (int col = 0; col < type.getCapacity() / 4; col++) {
                        row.add(seats.get(col * 4 + r));
                    }
                    rows.add(row);
                }
            }
            views.add(new CoachView(coach.getCoachNo(), type, seats, rows, compartments));
        }
        return views;
    }

    /**
     * THUAT TOAN GOI Y CHO NGOI LIEN NHAU.
     * Muc tieu: dat n cho gan nhau nhat trong cung mot toa thuoc hang toa yeu cau.
     *  1. Toa giuong: uu tien khoang con du n giuong trong (ca nhom o chung khoang),
     *     trong khoang uu tien tang thap.
     *  2. Neu khong co khoang du cho (hoac toa ghe ngoi): truot cua so kich thuoc n tren danh sach
     *     so cho trong da sap xep, chon cua so co khoang cach (so cuoi - so dau) nho nhat.
     *  3. Neu khong toa nao du n cho: tra ve danh sach rong.
     */
    public List<String> suggestSeats(TrainTrip trip, TrainCoachType type, int n) {
        if (n < 1) return List.of();
        Set<String> taken = takenSeats(trip.getId());
        List<String> best = List.of();
        int bestSpan = Integer.MAX_VALUE;

        for (TrainCoach coach : trip.getCoaches()) {
            if (type != null && coach.getCoachType() != type) continue;
            TrainCoachType t = coach.getCoachType();
            List<Integer> free = new ArrayList<>();
            for (int no = 1; no <= t.getCapacity(); no++) {
                if (!taken.contains(TrainSeatLayout.key(coach.getCoachNo(), no))) free.add(no);
            }
            if (free.size() < n) continue;

            if (t.isSleeper() && n <= t.getBerthsPerCompartment()) {
                Map<Integer, TreeSet<Integer>> byCompartment = new TreeMap<>();
                for (int no : free) {
                    byCompartment.computeIfAbsent(TrainSeatLayout.compartmentOf(t, no), k -> new TreeSet<>()).add(no);
                }
                for (TreeSet<Integer> berths : byCompartment.values()) {
                    if (berths.size() >= n) {
                        List<Integer> chosen = berths.stream()
                                .sorted(Comparator.comparingInt((Integer no) -> TrainSeatLayout.tierOf(t, no))
                                        .thenComparingInt(no -> no))
                                .limit(n).toList();
                        // cung khoang => coi nhu span = 0, uu tien tuyet doi
                        return toKeys(coach.getCoachNo(), chosen);
                    }
                }
            }

            for (int i = 0; i + n - 1 < free.size(); i++) {
                int span = free.get(i + n - 1) - free.get(i);
                if (span < bestSpan) {
                    bestSpan = span;
                    best = toKeys(coach.getCoachNo(), free.subList(i, i + n));
                }
            }
        }
        return best;
    }

    private List<String> toKeys(int coachNo, List<Integer> seatNos) {
        return seatNos.stream().sorted().map(no -> TrainSeatLayout.key(coachNo, no)).collect(Collectors.toList());
    }

    /** Cac tuyen dang co chuyen sap chay: moi phan tu = [ga di, ga den]. */
    public List<List<String>> activeRoutes() {
        return tripRepo.findActiveRoutePairs(LocalDateTime.now()).stream()
                .map(r -> List.of((String) r[0], (String) r[1]))
                .collect(Collectors.toList());
    }
}
