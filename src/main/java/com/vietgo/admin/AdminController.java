package com.vietgo.admin;

import com.vietgo.auth.CurrentUser;
import com.vietgo.auth.UserService;
import com.vietgo.booking.BookingService;
import com.vietgo.consultation.ConsultationService;
import com.vietgo.domain.Booking;
import com.vietgo.domain.BookingStatus;
import com.vietgo.domain.Consultation;
import com.vietgo.domain.ConsultationStatus;
import com.vietgo.domain.Role;
import com.vietgo.domain.TicketType;
import com.vietgo.repository.BookingRepository;
import com.vietgo.repository.BusTripRepository;
import com.vietgo.repository.ConsultationRepository;
import com.vietgo.repository.TrainTripRepository;
import com.vietgo.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Trang quan tri: tong quan, don dat ve, lich tu van, nguoi dung. */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final BookingRepository bookingRepo;
    private final ConsultationRepository consultRepo;
    private final UserRepository userRepo;
    private final TrainTripRepository trainRepo;
    private final BusTripRepository busRepo;
    private final BookingService bookingService;
    private final ConsultationService consultationService;
    private final UserService userService;
    private final CurrentUser currentUser;

    public AdminController(BookingRepository bookingRepo, ConsultationRepository consultRepo, UserRepository userRepo,
                           TrainTripRepository trainRepo, BusTripRepository busRepo, BookingService bookingService,
                           ConsultationService consultationService, UserService userService, CurrentUser currentUser) {
        this.bookingRepo = bookingRepo;
        this.consultRepo = consultRepo;
        this.userRepo = userRepo;
        this.trainRepo = trainRepo;
        this.busRepo = busRepo;
        this.bookingService = bookingService;
        this.consultationService = consultationService;
        this.userService = userService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String dashboard(Model model) {
        List<Booking> paid = bookingRepo.findAllPaid();
        long trainRevenue = paid.stream().filter(Booking::isTrain)
                .mapToLong(b -> b.getTotalAmount() - b.getRefundAmount()).sum();
        long busRevenue = paid.stream().filter(Booking::isBus)
                .mapToLong(b -> b.getTotalAmount() - b.getRefundAmount()).sum();

        LocalDate today = LocalDate.now();
        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd/MM", Locale.forLanguageTag("vi"));
        List<DayRevenue> days = new ArrayList<>();
        long maxDay = 1;
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            long tr = 0, bu = 0;
            for (Booking b : paid) {
                if (b.getPaidAt().toLocalDate().equals(d)) {
                    long net = b.getTotalAmount() - b.getRefundAmount();
                    if (b.isTrain()) tr += net; else bu += net;
                }
            }
            days.add(new DayRevenue(d.format(f), tr, bu));
            maxDay = Math.max(maxDay, tr + bu);
        }

        model.addAttribute("trainRevenue", trainRevenue);
        model.addAttribute("busRevenue", busRevenue);
        model.addAttribute("totalRevenue", trainRevenue + busRevenue);
        model.addAttribute("days", days);
        model.addAttribute("maxDay", maxDay);
        model.addAttribute("paidCount", bookingRepo.countByStatus(BookingStatus.PAID));
        model.addAttribute("heldCount", bookingRepo.countByStatus(BookingStatus.HELD));
        model.addAttribute("cancelledCount", bookingRepo.countByStatus(BookingStatus.CANCELLED));
        model.addAttribute("expiredCount", bookingRepo.countByStatus(BookingStatus.EXPIRED));
        model.addAttribute("trainPaid", bookingRepo.countByTicketTypeAndStatus(TicketType.TRAIN, BookingStatus.PAID));
        model.addAttribute("busPaid", bookingRepo.countByTicketTypeAndStatus(TicketType.BUS, BookingStatus.PAID));
        model.addAttribute("pendingConsult", consultRepo.countByStatus(ConsultationStatus.PENDING));
        model.addAttribute("userCount", userRepo.countByRole(Role.USER));
        model.addAttribute("trainTrips", trainRepo.findUpcoming(LocalDateTime.now()).size());
        model.addAttribute("busTrips", busRepo.findUpcoming(LocalDateTime.now()).size());
        model.addAttribute("recent", bookingRepo.findAllByOrderByCreatedAtDesc().stream().limit(8).toList());
        model.addAttribute("upcomingConsult", consultRepo.findAllByOrderByAppointmentTimeDesc().stream()
                .filter(c -> c.getStatus().isActive() && c.isUpcoming())
                .sorted((a, b) -> a.getAppointmentTime().compareTo(b.getAppointmentTime()))
                .limit(6).toList());
        model.addAttribute("section", "dashboard");
        return "admin/dashboard";
    }

    // ---------------- Don dat ve ----------------

    @GetMapping("/don-hang")
    public String bookings(@RequestParam(required = false) TicketType type,
                           @RequestParam(required = false) BookingStatus status,
                           @RequestParam(required = false) String q,
                           Model model) {
        String query = q == null ? "" : q.trim().toLowerCase();
        List<Booking> list = bookingRepo.findAllByOrderByCreatedAtDesc().stream()
                .filter(b -> type == null || b.getTicketType() == type)
                .filter(b -> status == null || b.getStatus() == status)
                .filter(b -> query.isEmpty()
                        || b.getCode().toLowerCase().contains(query)
                        || b.getContactName().toLowerCase().contains(query)
                        || b.getContactPhone().contains(query))
                .toList();
        model.addAttribute("bookings", list);
        model.addAttribute("type", type);
        model.addAttribute("status", status);
        model.addAttribute("q", q);
        model.addAttribute("statuses", BookingStatus.values());
        model.addAttribute("section", "bookings");
        return "admin/bookings";
    }

    @PostMapping("/don-hang/{code}/huy")
    public String cancelBooking(@PathVariable String code, @RequestParam(required = false) String reason,
                                RedirectAttributes ra) {
        Booking b = bookingService.cancelByAdmin(code, reason, LocalDateTime.now());
        ra.addFlashAttribute("success", "Đã hủy đơn " + code
                + (b.getRefundAmount() > 0 ? " và hoàn 100% cho khách." : "."));
        return "redirect:/admin/don-hang";
    }

    // ---------------- Lich tu van ----------------

    @GetMapping("/tu-van")
    public String consultations(@RequestParam(required = false) ConsultationStatus status, Model model) {
        List<Consultation> list = consultationService.listAll().stream()
                .filter(c -> status == null || c.getStatus() == status).toList();
        model.addAttribute("items", list);
        model.addAttribute("status", status);
        model.addAttribute("statuses", ConsultationStatus.values());
        model.addAttribute("section", "consultations");
        return "admin/consultations";
    }

    @PostMapping("/tu-van/{id}")
    public String updateConsultation(@PathVariable Long id, @RequestParam(required = false) ConsultationStatus status,
                                     @RequestParam(required = false) String adminNote, RedirectAttributes ra) {
        consultationService.updateByAdmin(id, status, adminNote);
        ra.addFlashAttribute("success", "Đã cập nhật lịch hẹn #" + id + ".");
        return "redirect:/admin/tu-van";
    }

    // ---------------- Nguoi dung ----------------

    @GetMapping("/nguoi-dung")
    public String users(Model model) {
        model.addAttribute("users", userRepo.findAllByOrderByCreatedAtDesc());
        model.addAttribute("section", "users");
        return "admin/users";
    }

    @PostMapping("/nguoi-dung/{id}/khoa")
    public String toggleUser(@PathVariable Long id, RedirectAttributes ra) {
        userService.toggleEnabled(id, currentUser.require());
        ra.addFlashAttribute("success", "Đã cập nhật trạng thái tài khoản.");
        return "redirect:/admin/nguoi-dung";
    }
}
