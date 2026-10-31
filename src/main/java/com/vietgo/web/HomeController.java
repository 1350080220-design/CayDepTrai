package com.vietgo.web;

import com.vietgo.bus.BusBookingValidator;
import com.vietgo.bus.BusService;
import com.vietgo.common.Money;
import com.vietgo.domain.BusType;
import com.vietgo.domain.TrainCoachType;
import com.vietgo.repository.BookingRepository;
import com.vietgo.repository.BusTripRepository;
import com.vietgo.repository.TrainTripRepository;
import com.vietgo.repository.UserRepository;
import com.vietgo.train.TrainBookingValidator;
import com.vietgo.train.TrainFareCalculator;
import com.vietgo.train.TrainService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Trang chu, trang chinh sach, trang loi 403. */
@Controller
public class HomeController {

    /** Tuyen pho bien hien thi o trang chu. */
    public static class PopularRoute {
        private final String type;
        private final String from;
        private final String to;
        private final String note;
        private final long fromPrice;
        private final String image;

        public PopularRoute(String type, String from, String to, String note, long fromPrice, String image) {
            this.type = type;
            this.from = from;
            this.to = to;
            this.note = note;
            this.fromPrice = fromPrice;
            this.image = image;
        }

        public String getType() { return type; }
        public String getFrom() { return from; }
        public String getTo() { return to; }
        public String getNote() { return note; }
        public long getFromPrice() { return fromPrice; }
        public String getImage() { return image; }
    }

    private final TrainFareCalculator trainFare;
    private final TrainTripRepository trainRepo;
    private final BusTripRepository busRepo;
    private final BookingRepository bookingRepo;
    private final UserRepository userRepo;

    public HomeController(TrainFareCalculator trainFare, TrainTripRepository trainRepo, BusTripRepository busRepo,
                          BookingRepository bookingRepo, UserRepository userRepo) {
        this.trainFare = trainFare;
        this.trainRepo = trainRepo;
        this.busRepo = busRepo;
        this.bookingRepo = bookingRepo;
        this.userRepo = userRepo;
    }

    @GetMapping("/")
    public String index(Model model) {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        model.addAttribute("stations", TrainService.STATIONS);
        model.addAttribute("cities", BusService.CITIES);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("tomorrow", tomorrow);
        model.addAttribute("maxDate", LocalDate.now().plusDays(60));

        List<PopularRoute> routes = new ArrayList<>();
        routes.add(new PopularRoute("train", "Hà Nội", "Đà Nẵng", "Cung đèo Hải Vân", economyTrainFare(791), "hue"));
        routes.add(new PopularRoute("train", "Sài Gòn", "Nha Trang", "Tàu đêm ven biển", economyTrainFare(411), "beach"));
        routes.add(new PopularRoute("train", "Hà Nội", "Sài Gòn", "Thống Nhất Bắc – Nam", economyTrainFare(1726), "long"));
        routes.add(new PopularRoute("bus", "TP. Hồ Chí Minh", "Đà Lạt", "Giường nằm, cabin, Limousine", 290_000, "pine"));
        routes.add(new PopularRoute("bus", "Hà Nội", "Sa Pa", "Cao tốc Nội Bài – Lào Cai", 300_000, "mountain"));
        routes.add(new PopularRoute("bus", "Đà Nẵng", "Hội An", "Xe buýt và Limousine 50 phút", 60_000, "lantern"));
        model.addAttribute("routes", routes);

        // Bang gio khoi hanh: 4 chuyen tau + 4 chuyen xe con mo ban gan nhat
        LocalDateTime now = LocalDateTime.now();
        List<DepartureRow> board = new ArrayList<>();
        trainRepo.findTop4ByActiveTrueAndDepartureTimeAfterOrderByDepartureTimeAsc(
                now.plusMinutes(TrainBookingValidator.SALES_CLOSE_MINUTES)).forEach(t -> board.add(DepartureRow.of(t)));
        busRepo.findTop4ByActiveTrueAndDepartureTimeAfterOrderByDepartureTimeAsc(
                now.plusMinutes(BusBookingValidator.SALES_CLOSE_MINUTES)).forEach(b -> board.add(DepartureRow.of(b)));
        board.sort(Comparator.comparing(DepartureRow::getDeparture));
        model.addAttribute("board", board);
        model.addAttribute("coachTypes", TrainCoachType.values());
        model.addAttribute("busTypes", BusType.values());

        model.addAttribute("trainTripCount", trainRepo.count());
        model.addAttribute("busTripCount", busRepo.count());
        model.addAttribute("bookingCount", bookingRepo.count());
        model.addAttribute("userCount", userRepo.count());
        return "index";
    }

    /** Gia ve hang ngoi cung (re nhat) cho cu ly cho truoc. */
    private long economyTrainFare(int km) {
        return Money.roundThousand(trainFare.distanceFare(km) * TrainCoachType.HARD_SEAT.getClassFactor());
    }

    @GetMapping("/chinh-sach")
    public String policy(Model model) {
        Map<Integer, Long> distanceTable = new LinkedHashMap<>();
        for (int km : new int[]{100, 300, 500, 791, 1000, 1315, 1726}) {
            distanceTable.put(km, trainFare.distanceFare(km));
        }
        model.addAttribute("distanceTable", distanceTable);
        model.addAttribute("coachTypes", TrainCoachType.values());
        model.addAttribute("busTypes", BusType.values());
        return "policy";
    }

    @GetMapping("/403")
    public String forbidden() {
        return "error/403";
    }
}
