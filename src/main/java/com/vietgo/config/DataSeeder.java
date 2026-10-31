package com.vietgo.config;

import com.vietgo.booking.BookingService;
import com.vietgo.bus.BusPassengerForm;
import com.vietgo.bus.BusPassengerItem;
import com.vietgo.bus.BusSeatLayout;
import com.vietgo.domain.Booking;
import com.vietgo.domain.BusPassengerType;
import com.vietgo.domain.BusTrip;
import com.vietgo.domain.BusType;
import com.vietgo.domain.Consultation;
import com.vietgo.domain.ConsultationChannel;
import com.vietgo.domain.ConsultationStatus;
import com.vietgo.domain.PaymentMethod;
import com.vietgo.domain.Role;
import com.vietgo.domain.TicketType;
import com.vietgo.domain.TrainCoachType;
import com.vietgo.domain.TrainPassengerType;
import com.vietgo.domain.TrainTrip;
import com.vietgo.domain.User;
import com.vietgo.repository.BookingRepository;
import com.vietgo.repository.BusTripRepository;
import com.vietgo.repository.ConsultationRepository;
import com.vietgo.repository.TrainTripRepository;
import com.vietgo.repository.UserRepository;
import com.vietgo.route.Railway;
import com.vietgo.train.TrainPassengerForm;
import com.vietgo.train.TrainPassengerItem;
import com.vietgo.train.TrainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tao du lieu mau khi database con trong:
 *  - Tai khoan quan tri va khach hang mau.
 *  - Lich chay tau, xe trong N ngay toi (tinh tu ngay khoi dong).
 *  - Mot so don da thanh toan va lich tu van de trang quan tri co so lieu.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    /** Mot mac tau chay hang ngay; profile quyet dinh co cau toa va toc do trung binh. */
    private record TrainLine(String code, String from, String to, LocalTime dep, String profile) {
    }

    /** Co cau doan tau theo loai tau: toa duoc danh so theo thu tu trong danh sach. */
    private static final Map<String, List<TrainCoachType>> TRAIN_PROFILES = Map.of(
            "premium", List.of(TrainCoachType.HARD_SEAT, TrainCoachType.SOFT_SEAT, TrainCoachType.SOFT_SEAT,
                    TrainCoachType.SLEEPER_6, TrainCoachType.SLEEPER_6, TrainCoachType.SLEEPER_4,
                    TrainCoachType.SLEEPER_4, TrainCoachType.VIP_2),
            "express", List.of(TrainCoachType.HARD_SEAT, TrainCoachType.SOFT_SEAT, TrainCoachType.SOFT_SEAT,
                    TrainCoachType.SLEEPER_6, TrainCoachType.SLEEPER_6, TrainCoachType.SLEEPER_4),
            "night", List.of(TrainCoachType.SOFT_SEAT, TrainCoachType.SLEEPER_6, TrainCoachType.SLEEPER_6,
                    TrainCoachType.SLEEPER_4, TrainCoachType.SLEEPER_4, TrainCoachType.VIP_2),
            "local", List.of(TrainCoachType.HARD_SEAT, TrainCoachType.HARD_SEAT, TrainCoachType.SOFT_SEAT,
                    TrainCoachType.SLEEPER_6),
            "day", List.of(TrainCoachType.HARD_SEAT, TrainCoachType.SOFT_SEAT, TrainCoachType.SOFT_SEAT,
                    TrainCoachType.SOFT_SEAT));

    /** Toc do thuong mai (km/h) theo loai tau, da gom thoi gian dung o cac ga. */
    private static final Map<String, Double> TRAIN_SPEED = Map.of(
            "premium", 54.0, "express", 52.0, "night", 50.0, "local", 42.0, "day", 50.0);

    private record BusLine(String operator, String plate, String from, String to, BusType type, long price,
                           int minutes, List<LocalTime> deps, String pickups, String dropoffs) {
    }

    private final UserRepository userRepo;
    private final TrainTripRepository trainRepo;
    private final BusTripRepository busRepo;
    private final BookingRepository bookingRepo;
    private final ConsultationRepository consultRepo;
    private final PasswordEncoder encoder;
    private final BookingService bookingService;
    private final TrainService trainService;
    private final TransactionTemplate tx;

    @Value("${app.seed.enabled:true}")
    private boolean enabled;

    @Value("${app.seed.days:30}")
    private int days;

    public DataSeeder(UserRepository userRepo, TrainTripRepository trainRepo, BusTripRepository busRepo,
                      BookingRepository bookingRepo, ConsultationRepository consultRepo, PasswordEncoder encoder,
                      BookingService bookingService, TrainService trainService, TransactionTemplate tx) {
        this.userRepo = userRepo;
        this.trainRepo = trainRepo;
        this.busRepo = busRepo;
        this.bookingRepo = bookingRepo;
        this.consultRepo = consultRepo;
        this.encoder = encoder;
        this.bookingService = bookingService;
        this.trainService = trainService;
        this.tx = tx;
    }

    @Override
    public void run(String... args) {
        if (!enabled) return;
        seedUsers();
        if (trainRepo.count() == 0) seedTrains();
        if (busRepo.count() == 0) seedBuses();
        if (bookingRepo.count() == 0) {
            try {
                seedDemoBookings();
            } catch (Exception e) {
                log.warn("Bo qua tao don mau: {}", e.getMessage());
            }
        }
        if (consultRepo.count() == 0) seedConsultations();
    }

    // ------------------------------------------------------------------ users

    private void seedUsers() {
        if (userRepo.count() > 0) return;
        userRepo.save(user("Quản Trị Viên", "admin@vietgo.vn", "0901000001", "Admin@123", Role.ADMIN));
        userRepo.save(user("Nguyễn Minh Anh", "khachhang@vietgo.vn", "0912345678", "Khach@123", Role.USER));
        userRepo.save(user("Trần Thu Hà", "thuha@vietgo.vn", "0987654321", "Khach@123", Role.USER));
        log.info("Da tao tai khoan mau: admin@vietgo.vn / Admin@123 - khachhang@vietgo.vn / Khach@123");
    }

    private User user(String name, String email, String phone, String pass, Role role) {
        User u = new User();
        u.setFullName(name);
        u.setEmail(email);
        u.setPhone(phone);
        u.setPasswordHash(encoder.encode(pass));
        u.setRole(role);
        return u;
    }

    // ------------------------------------------------------------------ trains

    private void seedTrains() {
        String hn = "Hà Nội", sg = "Sài Gòn", dn = "Đà Nẵng", hue = "Huế", vinh = "Vinh";
        String nt = "Nha Trang", qn = "Quy Nhơn (Diêu Trì)", pt = "Phan Thiết";
        List<TrainLine> lines = List.of(
                // Thong Nhat Ha Noi - Sai Gon
                new TrainLine("SE1", hn, sg, LocalTime.of(19, 25), "premium"),
                new TrainLine("SE2", sg, hn, LocalTime.of(19, 30), "premium"),
                new TrainLine("SE3", hn, sg, LocalTime.of(22, 15), "premium"),
                new TrainLine("SE4", sg, hn, LocalTime.of(21, 55), "premium"),
                new TrainLine("SE5", hn, sg, LocalTime.of(8, 50), "express"),
                new TrainLine("SE6", sg, hn, LocalTime.of(8, 45), "express"),
                new TrainLine("SE7", hn, sg, LocalTime.of(6, 0), "express"),
                new TrainLine("SE8", sg, hn, LocalTime.of(6, 0), "express"),
                new TrainLine("TN1", hn, sg, LocalTime.of(10, 0), "local"),
                new TrainLine("TN2", sg, hn, LocalTime.of(11, 0), "local"),
                // Mien Bac - mien Trung
                new TrainLine("SE11", hn, hue, LocalTime.of(21, 0), "night"),
                new TrainLine("SE12", hue, hn, LocalTime.of(19, 0), "night"),
                new TrainLine("SE19", hn, dn, LocalTime.of(20, 5), "night"),
                new TrainLine("SE20", dn, hn, LocalTime.of(20, 20), "night"),
                new TrainLine("NA1", hn, vinh, LocalTime.of(21, 10), "night"),
                new TrainLine("NA2", vinh, hn, LocalTime.of(21, 15), "night"),
                new TrainLine("NA3", hn, vinh, LocalTime.of(6, 15), "day"),
                new TrainLine("NA4", vinh, hn, LocalTime.of(13, 20), "day"),
                new TrainLine("QB1", hn, "Đồng Hới", LocalTime.of(13, 30), "local"),
                new TrainLine("QB2", "Đồng Hới", hn, LocalTime.of(19, 0), "local"),
                new TrainLine("TH1", hn, "Thanh Hóa", LocalTime.of(6, 10), "day"),
                new TrainLine("TH2", "Thanh Hóa", hn, LocalTime.of(14, 45), "day"),
                new TrainLine("NB1", hn, "Ninh Bình", LocalTime.of(7, 30), "day"),
                new TrainLine("NB2", "Ninh Bình", hn, LocalTime.of(16, 30), "day"),
                // Mien Trung
                new TrainLine("HD1", hue, dn, LocalTime.of(8, 0), "day"),
                new TrainLine("HD2", dn, hue, LocalTime.of(15, 0), "day"),
                new TrainLine("HD3", hue, dn, LocalTime.of(13, 30), "day"),
                new TrainLine("HD4", dn, hue, LocalTime.of(8, 30), "day"),
                new TrainLine("DQ1", dn, "Quảng Ngãi", LocalTime.of(6, 30), "day"),
                new TrainLine("DQ2", "Quảng Ngãi", dn, LocalTime.of(15, 30), "day"),
                new TrainLine("SE21", sg, dn, LocalTime.of(10, 0), "express"),
                new TrainLine("SE22", dn, sg, LocalTime.of(9, 0), "express"),
                // Mien Nam
                new TrainLine("SQN1", sg, qn, LocalTime.of(19, 40), "night"),
                new TrainLine("SQN2", qn, sg, LocalTime.of(18, 30), "night"),
                new TrainLine("SNT2", sg, nt, LocalTime.of(20, 30), "night"),
                new TrainLine("SNT1", nt, sg, LocalTime.of(20, 0), "night"),
                new TrainLine("SNT4", sg, nt, LocalTime.of(7, 15), "express"),
                new TrainLine("SNT3", nt, sg, LocalTime.of(13, 0), "express"),
                new TrainLine("SPT2", sg, pt, LocalTime.of(6, 40), "day"),
                new TrainLine("SPT1", pt, sg, LocalTime.of(13, 10), "day"),
                new TrainLine("SPT4", sg, pt, LocalTime.of(15, 30), "day"),
                new TrainLine("SPT3", pt, sg, LocalTime.of(7, 0), "day"));

        LocalDate start = LocalDate.now();
        List<TrainTrip> batch = new ArrayList<>();
        for (int d = 0; d <= days; d++) {
            LocalDate day = start.plusDays(d);
            for (TrainLine l : lines) {
                int km = Railway.distanceKm(l.from(), l.to());
                long minutes = Math.round(km / TRAIN_SPEED.get(l.profile()) * 60 / 5) * 5;
                TrainTrip t = new TrainTrip();
                t.setTrainCode(l.code());
                t.setOriginStation(l.from());
                t.setDestinationStation(l.to());
                t.setDepartureTime(day.atTime(l.dep()));
                t.setArrivalTime(t.getDepartureTime().plusMinutes(minutes));
                t.setDistanceKm(km);
                int no = 1;
                for (TrainCoachType type : TRAIN_PROFILES.get(l.profile())) {
                    t.addCoach(no++, type);
                }
                batch.add(t);
            }
        }
        trainRepo.saveAll(batch);
        log.info("Da tao {} chuyen tau mau", batch.size());
    }

    // ------------------------------------------------------------------ buses

    private void seedBuses() {
        String hcm = "TP. Hồ Chí Minh", hn = "Hà Nội", dn = "Đà Nẵng";
        List<BusLine> lines = List.of(
                // Tu TP. Ho Chi Minh
                new BusLine("Sen Vàng Express", "51B-123.45", hcm, "Đà Lạt", BusType.SLEEPER_36, 290_000, 420,
                        times(7, 0, 13, 30, 22, 0),
                        "Bến xe Miền Đông mới;Văn phòng Quận 1 – 272 Đề Thám;Ngã tư Hàng Xanh",
                        "Bến xe liên tỉnh Đà Lạt;Chợ Đà Lạt;Hồ Xuân Hương"),
                new BusLine("Mây Trắng Limousine", "51F-678.90", hcm, "Đà Lạt", BusType.LIMOUSINE_9, 380_000, 390,
                        times(8, 30, 23, 0),
                        "Văn phòng Quận 3 – 15 Nguyễn Thị Minh Khai;Sân bay Tân Sơn Nhất",
                        "Văn phòng Đà Lạt – 12 Bùi Thị Xuân;Quảng trường Lâm Viên"),
                new BusLine("Đồi Thông Cabin", "49B-688.99", hcm, "Đà Lạt", BusType.CABIN_24, 420_000, 410,
                        times(9, 30, 22, 30),
                        "Văn phòng Quận 5 – 120 Trần Hưng Đạo;Bến xe Miền Đông mới",
                        "Văn phòng Đà Lạt – 7 Hồ Tùng Mậu;Chợ Đà Lạt"),
                new BusLine("Hải Âu Express", "51B-246.80", hcm, "Vũng Tàu", BusType.LIMOUSINE_9, 190_000, 120,
                        times(6, 0, 9, 0, 14, 0, 17, 30),
                        "Văn phòng Quận 1 – 98 Nguyễn Cư Trinh;Ngã tư Bình Thái",
                        "Bãi Sau – Thùy Vân;Văn phòng Vũng Tàu – 3 Lê Lợi"),
                new BusLine("Đồng Nội Bus", "51B-332.10", hcm, "Vũng Tàu", BusType.SEAT_45, 120_000, 140,
                        times(5, 30, 11, 0, 16, 0),
                        "Bến xe Miền Đông mới;Ngã tư Bình Thái",
                        "Bến xe Vũng Tàu;Bãi Trước – Quang Trung"),
                new BusLine("Đồng Nội Bus", "65B-135.79", hcm, "Cần Thơ", BusType.SEAT_45, 165_000, 210,
                        times(5, 30, 10, 0, 15, 0, 19, 0),
                        "Bến xe Miền Tây;Vòng xoay An Lạc",
                        "Bến xe Trung tâm Cần Thơ;Bến Ninh Kiều"),
                new BusLine("Phương Nam Limousine", "65F-246.88", hcm, "Cần Thơ", BusType.LIMOUSINE_9, 230_000, 180,
                        times(7, 0, 12, 0, 17, 0),
                        "Văn phòng Quận 10 – 45 Ba Tháng Hai;Bến xe Miền Tây",
                        "Văn phòng Cần Thơ – 22 Hòa Bình;Bến Ninh Kiều"),
                new BusLine("Sen Vàng Express", "51B-555.21", hcm, "Nha Trang", BusType.SLEEPER_36, 320_000, 510,
                        times(8, 0, 21, 0),
                        "Bến xe Miền Đông mới;Văn phòng Quận 1 – 272 Đề Thám",
                        "Bến xe phía Nam Nha Trang;Trần Phú – Bãi biển Nha Trang"),
                new BusLine("Đồi Thông Cabin", "79B-456.78", hcm, "Nha Trang", BusType.CABIN_24, 450_000, 500,
                        times(21, 30),
                        "Văn phòng Quận 5 – 120 Trần Hưng Đạo;Bến xe Miền Đông mới",
                        "Văn phòng Nha Trang – 58 Yersin;Trần Phú – Bãi biển Nha Trang"),
                new BusLine("Hải Âu Express", "86B-135.24", hcm, "Phan Thiết", BusType.LIMOUSINE_9, 250_000, 200,
                        times(7, 0, 10, 30, 14, 30),
                        "Văn phòng Quận 1 – 98 Nguyễn Cư Trinh;Sân bay Tân Sơn Nhất",
                        "Trung tâm TP. Phan Thiết;Mũi Né – Nguyễn Đình Chiểu"),
                new BusLine("Sen Vàng Express", "86B-777.12", hcm, "Phan Thiết", BusType.SLEEPER_36, 180_000, 230,
                        times(8, 0, 23, 0),
                        "Bến xe Miền Đông mới;Ngã tư Hàng Xanh",
                        "Bến xe Bắc Bình Thuận;Mũi Né – Nguyễn Đình Chiểu"),
                new BusLine("Cao Nguyên Xanh", "47B-121.56", hcm, "Buôn Ma Thuột", BusType.SLEEPER_36, 280_000, 480,
                        times(8, 0, 20, 30),
                        "Bến xe Miền Đông mới;Ngã tư Ga",
                        "Bến xe phía Bắc Buôn Ma Thuột;Ngã sáu Buôn Ma Thuột"),
                new BusLine("Biển Xanh Bus", "77B-345.67", hcm, "Quy Nhơn", BusType.SLEEPER_36, 400_000, 660,
                        times(17, 0),
                        "Bến xe Miền Đông mới;Văn phòng Quận 1 – 272 Đề Thám",
                        "Bến xe Quy Nhơn;Bãi biển Quy Nhơn – An Dương Vương"),
                // Nam Trung Bo, Tay Nguyen
                new BusLine("Mây Trắng Limousine", "79F-202.20", "Nha Trang", "Đà Lạt", BusType.LIMOUSINE_9, 220_000, 220,
                        times(7, 0, 13, 0),
                        "Văn phòng Nha Trang – 58 Yersin;Trần Phú – Bãi biển Nha Trang",
                        "Văn phòng Đà Lạt – 12 Bùi Thị Xuân;Chợ Đà Lạt"),
                new BusLine("Đồng Nội Bus", "79B-505.05", "Nha Trang", "Đà Lạt", BusType.SEAT_45, 150_000, 240,
                        times(8, 0, 14, 30),
                        "Bến xe phía Nam Nha Trang;Chợ Đầm",
                        "Bến xe liên tỉnh Đà Lạt;Chợ Đà Lạt"),
                new BusLine("Biển Xanh Bus", "43B-818.18", dn, "Quy Nhơn", BusType.SLEEPER_36, 260_000, 330,
                        times(8, 0, 21, 0),
                        "Bến xe Trung tâm Đà Nẵng;Cầu Rồng",
                        "Bến xe Quy Nhơn;Bãi biển Quy Nhơn – An Dương Vương"),
                // Mien Trung
                new BusLine("Bình Minh Bus", "43B-202.33", dn, "Huế", BusType.SEAT_45, 120_000, 150,
                        times(7, 0, 12, 0, 16, 30),
                        "Bến xe Trung tâm Đà Nẵng;Cầu Rồng",
                        "Bến xe phía Nam Huế;Đại Nội Huế"),
                new BusLine("Phố Núi Express", "43B-909.10", dn, "Hội An", BusType.LIMOUSINE_9, 120_000, 50,
                        times(8, 0, 11, 0, 14, 0, 17, 0),
                        "Sân bay Đà Nẵng;Cầu Rồng",
                        "Phố cổ Hội An;Biển An Bàng"),
                new BusLine("Bình Minh Bus", "43B-111.22", dn, "Hội An", BusType.SEAT_45, 60_000, 60,
                        times(7, 30, 10, 30, 13, 30, 16, 30, 19, 30),
                        "Bến xe Trung tâm Đà Nẵng;Chợ Hàn",
                        "Bến xe Hội An;Phố cổ Hội An"),
                // Tu Ha Noi
                new BusLine("Phố Núi Express", "24B-111.68", hn, "Sa Pa", BusType.SLEEPER_36, 300_000, 330,
                        times(7, 0, 13, 0, 22, 0),
                        "Bến xe Mỹ Đình;Văn phòng Phố Cổ – 18 Hàng Bạc;Sân bay Nội Bài",
                        "Trung tâm thị xã Sa Pa;Nhà thờ Đá Sa Pa"),
                new BusLine("Mây Trắng Limousine", "29F-246.13", hn, "Sa Pa", BusType.LIMOUSINE_9, 390_000, 320,
                        times(6, 30, 21, 30),
                        "Văn phòng Phố Cổ – 18 Hàng Bạc;Lotte Center Liễu Giai",
                        "Văn phòng Sa Pa – 4 Thạch Sơn"),
                new BusLine("Đồi Thông Cabin", "24B-999.68", hn, "Sa Pa", BusType.CABIN_24, 450_000, 330,
                        times(7, 30, 22, 30),
                        "Văn phòng Hoàn Kiếm – 30 Trần Nhật Duật;Bến xe Mỹ Đình",
                        "Văn phòng Sa Pa – 9 Cầu Mây;Nhà thờ Đá Sa Pa"),
                new BusLine("Bình Minh Bus", "15B-456.12", hn, "Hải Phòng", BusType.SEAT_45, 130_000, 120,
                        times(6, 0, 9, 30, 13, 0, 17, 0),
                        "Bến xe Gia Lâm;Bến xe Giáp Bát",
                        "Bến xe Niệm Nghĩa;Bến xe Cầu Rào"),
                new BusLine("Hải Âu Express", "15F-888.66", hn, "Hải Phòng", BusType.LIMOUSINE_9, 200_000, 110,
                        times(7, 15, 11, 15, 15, 15, 19, 15),
                        "Văn phòng Hoàn Kiếm – 30 Trần Nhật Duật;Times City",
                        "Văn phòng Hải Phòng – 5 Lạch Tray;Sân bay Cát Bi"),
                new BusLine("Hải Âu Express", "29B-777.88", hn, "Hạ Long", BusType.SEAT_45, 160_000, 150,
                        times(7, 0, 11, 0, 15, 30),
                        "Bến xe Mỹ Đình;Bến xe Gia Lâm",
                        "Bến xe Bãi Cháy;Bãi Cháy – khu du lịch"),
                new BusLine("Mây Trắng Limousine", "14F-123.88", hn, "Hạ Long", BusType.LIMOUSINE_9, 250_000, 150,
                        times(6, 30, 9, 30, 14, 30),
                        "Văn phòng Phố Cổ – 18 Hàng Bạc;Lotte Center Liễu Giai",
                        "Cảng tàu khách Tuần Châu;Bãi Cháy – khu du lịch"),
                new BusLine("Mây Trắng Limousine", "35F-321.09", hn, "Ninh Bình", BusType.LIMOUSINE_9, 170_000, 120,
                        times(7, 30, 14, 0, 18, 0),
                        "Văn phòng Phố Cổ – 18 Hàng Bạc;Bến xe Giáp Bát",
                        "Tràng An;Trung tâm TP. Ninh Bình"),
                new BusLine("Bình Minh Bus", "35B-101.01", hn, "Ninh Bình", BusType.SEAT_45, 100_000, 130,
                        times(6, 0, 9, 0, 13, 0, 16, 0),
                        "Bến xe Giáp Bát;Bến xe Nước Ngầm",
                        "Bến xe Ninh Bình;Tam Cốc – Bích Động"),
                new BusLine("Bình Minh Bus", "37B-246.35", hn, "Vinh", BusType.SLEEPER_36, 230_000, 330,
                        times(9, 0, 21, 0),
                        "Bến xe Giáp Bát;Bến xe Nước Ngầm",
                        "Bến xe Bắc Vinh;Bến xe Vinh"),
                new BusLine("Bình Minh Bus", "75B-123.98", hn, "Huế", BusType.SLEEPER_36, 420_000, 780,
                        times(17, 0),
                        "Bến xe Giáp Bát;Bến xe Nước Ngầm",
                        "Bến xe phía Bắc Huế;Phố đi bộ Phạm Ngũ Lão"),
                new BusLine("Hoàng Thành Cabin", "75B-686.86", hn, "Huế", BusType.CABIN_24, 550_000, 760,
                        times(18, 30),
                        "Văn phòng Hoàn Kiếm – 30 Trần Nhật Duật;Bến xe Nước Ngầm",
                        "Văn phòng Huế – 15 Lê Lợi;Đại Nội Huế"));

        LocalDate start = LocalDate.now();
        List<BusTrip> batch = new ArrayList<>();
        for (int d = 0; d <= days; d++) {
            LocalDate day = start.plusDays(d);
            for (BusLine l : lines) {
                for (LocalTime dep : l.deps()) {
                    batch.add(bus(l, day.atTime(dep), false));
                    batch.add(bus(l, day.atTime(dep.plusMinutes(30)), true));
                }
            }
        }
        busRepo.saveAll(batch);
        log.info("Da tao {} chuyen xe mau", batch.size());
    }

    private BusTrip bus(BusLine l, LocalDateTime dep, boolean reverse) {
        BusTrip b = new BusTrip();
        b.setOperatorName(l.operator());
        b.setLicensePlate(l.plate());
        b.setOriginCity(reverse ? l.to() : l.from());
        b.setDestinationCity(reverse ? l.from() : l.to());
        b.setDepartureTime(dep);
        b.setArrivalTime(dep.plusMinutes(l.minutes()));
        b.setBusType(l.type());
        b.setBasePrice(l.price());
        b.setPickupPoints(reverse ? reverseList(l.dropoffs()) : l.pickups());
        b.setDropoffPoints(reverse ? reverseList(l.pickups()) : l.dropoffs());
        return b;
    }

    private static String reverseList(String s) {
        List<String> parts = new ArrayList<>(List.of(s.split(";")));
        java.util.Collections.reverse(parts);
        return String.join(";", parts);
    }

    private static List<LocalTime> times(int... hm) {
        List<LocalTime> list = new ArrayList<>();
        for (int i = 0; i + 1 < hm.length; i += 2) list.add(LocalTime.of(hm[i], hm[i + 1]));
        return list;
    }

    // ------------------------------------------------------------------ demo bookings

    private void seedDemoBookings() {
        User demo = userRepo.findByEmailIgnoreCase("khachhang@vietgo.vn").orElse(null);
        User ha = userRepo.findByEmailIgnoreCase("thuha@vietgo.vn").orElse(null);
        if (demo == null || ha == null) return;
        LocalDate base = LocalDate.now().plusDays(5);

        demoTrain(demo, "Hà Nội", "Đà Nẵng", base, TrainCoachType.SLEEPER_4, 2, PaymentMethod.E_WALLET, 6);
        demoTrain(ha, "Sài Gòn", "Nha Trang", base.plusDays(1), TrainCoachType.SLEEPER_6, 3, PaymentMethod.BANK_CARD, 4);
        demoTrain(demo, "Hà Nội", "Sài Gòn", base.plusDays(3), TrainCoachType.SOFT_SEAT, 1, PaymentMethod.BANK_TRANSFER, 1);
        demoBus(demo, "TP. Hồ Chí Minh", "Đà Lạt", base.plusDays(2), BusType.SLEEPER_36, 2, PaymentMethod.E_WALLET, 5);
        demoBus(ha, "Hà Nội", "Sa Pa", base.plusDays(4), BusType.SLEEPER_36, 4, PaymentMethod.INTL_CARD, 3);
        demoBus(demo, "Đà Nẵng", "Hội An", base.plusDays(1), BusType.LIMOUSINE_9, 2, PaymentMethod.BANK_CARD, 2);
        demoBus(ha, "TP. Hồ Chí Minh", "Vũng Tàu", base, BusType.LIMOUSINE_9, 3, PaymentMethod.E_WALLET, 0);
        demoTrain(ha, "Hà Nội", "Sài Gòn", base.plusDays(6), TrainCoachType.VIP_2, 2, PaymentMethod.INTL_CARD, 2);
        demoTrain(demo, "Sài Gòn", "Phan Thiết", base.plusDays(2), TrainCoachType.HARD_SEAT, 3, PaymentMethod.E_WALLET, 1);
        demoBus(demo, "Hà Nội", "Sa Pa", base.plusDays(7), BusType.CABIN_24, 2, PaymentMethod.BANK_CARD, 1);
        log.info("Da tao don dat ve mau");
    }

    private void demoTrain(User user, String from, String to, LocalDate date, TrainCoachType type, int n,
                           PaymentMethod method, int paidDaysAgo) {
        tx.executeWithoutResult(status -> {
            TrainTrip trip = trainService.search(from, to, date).stream()
                    .filter(t -> t.countCoaches(type) > 0)
                    .findFirst().orElse(null);
            if (trip == null) return;
            List<String> seats = trainService.suggestSeats(trip, type, n);
            if (seats.size() != n) return;
            TrainPassengerForm f = new TrainPassengerForm();
            f.setContactName(user.getFullName());
            f.setContactPhone(user.getPhone());
            f.setContactEmail(user.getEmail());
            String[] names = {user.getFullName(), "Lê Văn Bình", "Phạm Gia Huy"};
            for (int i = 0; i < n; i++) {
                TrainPassengerItem p = new TrainPassengerItem();
                p.setSeatKey(seats.get(i));
                p.setFullName(names[i % names.length]);
                if (i == 2) {
                    p.setType(TrainPassengerType.CHILD);
                    p.setBirthDate(date.minusYears(8));
                } else if (i == 1) {
                    p.setType(TrainPassengerType.STUDENT);
                    p.setBirthDate(date.minusYears(20));
                    p.setIdNumber("07920300" + (1000 + i));
                    p.setStudentCardNo("SV2024" + (100 + i));
                } else {
                    p.setType(TrainPassengerType.ADULT);
                    p.setBirthDate(date.minusYears(30));
                    p.setIdNumber("07919000" + (1000 + i + user.getId()));
                }
                f.getPassengers().add(p);
            }
            LocalDateTime when = LocalDateTime.now().minusDays(paidDaysAgo).withHour(9 + paidDaysAgo).withMinute(15);
            Booking b = bookingService.holdTrain(user, trip, f, when);
            payAndBackdate(b, user, method, when);
        });
    }

    private void demoBus(User user, String from, String to, LocalDate date, BusType type, int n,
                         PaymentMethod method, int paidDaysAgo) {
        tx.executeWithoutResult(status -> {
            BusTrip trip = busRepo.search(from, to, date.atStartOfDay(), date.plusDays(1).atStartOfDay(), type)
                    .stream().findFirst().orElse(null);
            if (trip == null) return;
            List<String> seats = BusSeatLayout.allSeats(type).subList(0, n);
            BusPassengerForm f = new BusPassengerForm();
            f.setContactName(user.getFullName());
            f.setContactPhone(user.getPhone());
            f.setContactEmail(user.getEmail());
            f.setPickupPoint(trip.getPickupPointList().get(0));
            f.setDropoffPoint(trip.getDropoffPointList().get(0));
            f.setLuggageKg(n * 20 + 5);
            String[] names = {user.getFullName(), "Đỗ Quang Khải", "Võ Thị Lan", "Bùi Minh Châu"};
            for (int i = 0; i < n; i++) {
                BusPassengerItem p = new BusPassengerItem();
                p.setSeatCode(seats.get(i));
                p.setFullName(names[i % names.length]);
                if (i == 3) {
                    p.setType(BusPassengerType.CHILD);
                    p.setBirthYear(date.getYear() - 8);
                } else {
                    p.setType(BusPassengerType.ADULT);
                }
                f.getPassengers().add(p);
            }
            LocalDateTime when = LocalDateTime.now().minusDays(paidDaysAgo).withHour(10 + paidDaysAgo).withMinute(40);
            Booking b = bookingService.holdBus(user, trip, f, when);
            payAndBackdate(b, user, method, when);
        });
    }

    private void payAndBackdate(Booking b, User user, PaymentMethod method, LocalDateTime when) {
        b.setHoldExpiresAt(LocalDateTime.now().plusMinutes(10));
        bookingService.pay(b.getCode(), user, method, LocalDateTime.now());
        b.setCreatedAt(when);
        b.setPaidAt(when.plusMinutes(4));
        bookingRepo.save(b);
    }

    // ------------------------------------------------------------------ consultations

    private void seedConsultations() {
        User demo = userRepo.findByEmailIgnoreCase("khachhang@vietgo.vn").orElse(null);
        User ha = userRepo.findByEmailIgnoreCase("thuha@vietgo.vn").orElse(null);
        if (demo == null || ha == null) return;
        LocalDate d = LocalDate.now().plusDays(2);
        if (d.getDayOfWeek() == DayOfWeek.SUNDAY) d = d.plusDays(1);
        consultRepo.save(consult(demo, TicketType.TRAIN, ConsultationChannel.PHONE,
                "Đặt vé đoàn / vé tập thể", d.atTime(9, 30), ConsultationStatus.CONFIRMED,
                "Đoàn 12 người đi Hà Nội – Đà Nẵng dịp cuối tháng, cần tư vấn toa giường nằm."));
        consultRepo.save(consult(ha, TicketType.BUS, ConsultationChannel.OFFICE,
                "Tư vấn lộ trình và chọn chuyến", d.atTime(14, 0), ConsultationStatus.PENDING,
                "Muốn đi Sa Pa 3 ngày, so sánh xe giường nằm và limousine."));
        consultRepo.save(consult(demo, TicketType.BUS, ConsultationChannel.VIDEO,
                "Đổi, hủy và hoàn tiền vé", LocalDate.now().minusDays(3).atTime(10, 0), ConsultationStatus.DONE,
                null));
    }

    private Consultation consult(User u, TicketType type, ConsultationChannel ch, String topic, LocalDateTime at,
                                 ConsultationStatus st, String note) {
        Consultation c = new Consultation();
        c.setUser(u);
        c.setFullName(u.getFullName());
        c.setPhone(u.getPhone());
        c.setEmail(u.getEmail());
        c.setTicketType(type);
        c.setChannel(ch);
        c.setTopic(topic);
        c.setAppointmentTime(at);
        c.setStatus(st);
        c.setNote(note);
        if (st == ConsultationStatus.DONE) c.setAdminNote("Đã hướng dẫn khách quy trình hủy vé trực tuyến.");
        return c;
    }
}
