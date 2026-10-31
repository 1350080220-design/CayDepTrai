package com.vietgo.bus;

import com.vietgo.common.NotFoundException;
import com.vietgo.common.SeatView;
import com.vietgo.domain.BusTrip;
import com.vietgo.domain.BusType;
import com.vietgo.domain.TicketType;
import com.vietgo.repository.BusTripRepository;
import com.vietgo.repository.SeatLockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Nghiep vu tra cuu chuyen xe va so do ghe. */
@Service
@Transactional(readOnly = true)
public class BusService {

    public static final List<String> CITIES = List.of(
            "Hà Nội", "Hải Phòng", "Hạ Long", "Sa Pa", "Ninh Bình", "Vinh", "Huế", "Đà Nẵng", "Hội An",
            "Quy Nhơn", "Nha Trang", "Đà Lạt", "Buôn Ma Thuột", "Phan Thiết", "Vũng Tàu", "TP. Hồ Chí Minh",
            "Cần Thơ");

    private final BusTripRepository tripRepo;
    private final SeatLockRepository lockRepo;
    private final BusFareCalculator fare;

    public BusService(BusTripRepository tripRepo, SeatLockRepository lockRepo, BusFareCalculator fare) {
        this.tripRepo = tripRepo;
        this.lockRepo = lockRepo;
        this.fare = fare;
    }

    public List<BusTrip> search(String origin, String destination, LocalDate date, BusType busType) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        if (from.isBefore(now)) from = now;
        return tripRepo.search(origin, destination, from, date.plusDays(1).atStartOfDay(), busType);
    }

    public BusTrip getTrip(Long id) {
        return tripRepo.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy chuyến xe"));
    }

    public Set<String> takenSeats(Long tripId) {
        return new HashSet<>(lockRepo.findSeatKeys(TicketType.BUS, tripId));
    }

    public long occupied(BusTrip trip) {
        return lockRepo.countByTicketTypeAndTripId(TicketType.BUS, trip.getId());
    }

    public long availableSeats(BusTrip trip) {
        return trip.getCapacity() - occupied(trip);
    }

    /** Gia hien tai thap nhat cua chuyen (ghe thuong, nguoi lon). */
    public long currentMinPrice(BusTrip trip) {
        long occ = occupied(trip);
        return BusSeatLayout.allSeats(trip.getBusType()).stream()
                .mapToLong(code -> fare.displayPrice(trip, code, occ))
                .min().orElse(trip.getBasePrice());
    }

    /** Cac tuyen dang co chuyen sap chay: moi phan tu = [noi di, noi den]. */
    public List<List<String>> activeRoutes() {
        List<List<String>> res = new ArrayList<>();
        for (Object[] r : tripRepo.findActiveRoutePairs(LocalDateTime.now())) {
            res.add(List.of((String) r[0], (String) r[1]));
        }
        return res;
    }

    public List<BusDeckView> buildDeckViews(BusTrip trip) {
        Set<String> taken = takenSeats(trip.getId());
        long occ = taken.size();
        BusType type = trip.getBusType();
        List<BusDeckView> decks = new ArrayList<>();
        for (BusSeatLayout.Deck deck : BusSeatLayout.decks(type)) {
            List<List<SeatView>> rows = new ArrayList<>();
            for (List<String> row : deck.rows()) {
                List<SeatView> r = new ArrayList<>();
                for (String code : row) {
                    if (code == null) {
                        r.add(null);
                    } else {
                        r.add(new SeatView(code, 0, BusSeatLayout.label(type, code), BusSeatLayout.tierOf(type, code),
                                fare.displayPrice(trip, code, occ), taken.contains(code)));
                    }
                }
                rows.add(r);
            }
            decks.add(new BusDeckView(deck.name(), rows));
        }
        return decks;
    }
}
