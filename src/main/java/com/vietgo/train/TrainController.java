package com.vietgo.train;

import com.vietgo.auth.CurrentUser;
import com.vietgo.booking.BookingService;
import com.vietgo.common.BusinessException;
import com.vietgo.common.FareQuote;
import com.vietgo.domain.Booking;
import com.vietgo.domain.TrainCoachType;
import com.vietgo.domain.TrainPassengerType;
import com.vietgo.domain.TrainTrip;
import com.vietgo.domain.User;
import com.vietgo.repository.TrainTripRepository;
import com.vietgo.route.RoutePlan;
import com.vietgo.route.RoutePlanner;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * QUY TRINH DAT VE TAU HOA (4 buoc):
 *  1. Tim chuyen tau theo ga di - ga den - ngay di.
 *  2. Chon toa va cho tren so do (co nut goi y cho lien nhau), toi da 6 cho.
 *  3. Nhap thong tin tung hanh khach (doi tuong, ngay sinh, CCCD, the HS-SV) va nguoi lien he.
 *  4. Xem bang tinh gia chi tiet -> Giu cho 15 phut -> Thanh toan (tai trang don hang).
 */
@Controller
@RequestMapping("/tau-hoa")
public class TrainController {

    private final TrainService trainService;
    private final TrainTripRepository tripRepo;
    private final TrainBookingValidator validator;
    private final BookingService bookingService;
    private final CurrentUser currentUser;
    private final RoutePlanner routePlanner;

    public TrainController(TrainService trainService, TrainTripRepository tripRepo, TrainBookingValidator validator,
                           BookingService bookingService, CurrentUser currentUser, RoutePlanner routePlanner) {
        this.trainService = trainService;
        this.tripRepo = tripRepo;
        this.validator = validator;
        this.bookingService = bookingService;
        this.currentUser = currentUser;
        this.routePlanner = routePlanner;
    }

    // ---------------- Buoc 1: tim chuyen ----------------

    @GetMapping
    public String search(@RequestParam(required = false) String from,
                         @RequestParam(required = false) String to,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                         Model model) {
        LocalDate today = LocalDate.now();
        model.addAttribute("stations", TrainService.STATIONS);
        model.addAttribute("today", today);
        model.addAttribute("maxDate", today.plusDays(60));
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("date", date == null ? today.plusDays(1) : date);

        boolean searched = from != null && to != null && date != null;
        model.addAttribute("searched", searched);
        if (!searched) {
            return "train/search";
        }
        if (from.equals(to)) {
            model.addAttribute("searchError", "Ga đi và ga đến phải khác nhau.");
            return "train/search";
        }
        if (date.isBefore(today)) {
            model.addAttribute("searchError", "Ngày đi không được trước hôm nay.");
            return "train/search";
        }

        List<TrainTrip> trips = trainService.search(from, to, date);
        Map<Long, Long> available = new HashMap<>();
        Map<Long, Long> minPrice = new HashMap<>();
        Map<Long, Boolean> closed = new HashMap<>();
        Map<Long, List<ClassOption>> classes = new HashMap<>();
        Map<Long, RoutePlan> plans = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        for (TrainTrip t : trips) {
            Set<String> taken = trainService.takenSeats(t.getId());
            List<ClassOption> options = trainService.classOptions(t, taken);
            available.put(t.getId(), (long) t.getCapacity() - taken.size());
            minPrice.put(t.getId(), options.stream().filter(o -> !o.isSoldOut()).mapToLong(ClassOption::getMinPrice)
                    .min().orElse(trainService.minPrice(t)));
            closed.put(t.getId(), now.isAfter(t.getDepartureTime().minusMinutes(TrainBookingValidator.SALES_CLOSE_MINUTES)));
            classes.put(t.getId(), options);
            plans.put(t.getId(), routePlanner.forTrain(t));
        }
        model.addAttribute("trips", trips);
        model.addAttribute("available", available);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("closed", closed);
        model.addAttribute("classes", classes);
        model.addAttribute("plans", plans);
        model.addAttribute("filterTypes", trips.stream().flatMap(t -> t.getCoachTypes().stream())
                .distinct().sorted().toList());

        List<LocalDate> days = new ArrayList<>();
        for (int i = -3; i <= 3; i++) {
            LocalDate d = date.plusDays(i);
            if (!d.isBefore(today) && !d.isAfter(today.plusDays(60))) days.add(d);
        }
        model.addAttribute("days", days);
        if (trips.isEmpty()) {
            model.addAttribute("routes", trainService.activeRoutes());
        }
        return "train/search";
    }

    // ---------------- Buoc 2: so do cho ----------------

    @GetMapping("/chuyen/{id}")
    public String trip(@PathVariable Long id, Model model) {
        TrainTrip trip = trainService.getTrip(id);
        boolean open = true;
        String closedReason = null;
        try {
            validator.checkTripOpen(trip, LocalDateTime.now());
        } catch (BusinessException e) {
            open = false;
            closedReason = e.getMessage();
        }
        RoutePlan plan = routePlanner.forTrain(trip);
        model.addAttribute("trip", trip);
        model.addAttribute("coaches", trainService.buildCoachViews(trip));
        model.addAttribute("classOptions", trainService.classOptions(trip, trainService.takenSeats(trip.getId())));
        model.addAttribute("plan", plan);
        model.addAttribute("routeJson", routePlanner.toJson(plan));
        model.addAttribute("open", open);
        model.addAttribute("closedReason", closedReason);
        model.addAttribute("maxSeats", TrainBookingValidator.MAX_PASSENGERS);
        model.addAttribute("holdMinutes", BookingService.TRAIN_HOLD_MINUTES);
        model.addAttribute("coachTypes", TrainCoachType.values());
        return "train/trip";
    }

    /** API goi y cho ngoi lien nhau (thuat toan cua so truot). */
    @GetMapping("/chuyen/{id}/goi-y")
    @ResponseBody
    public Map<String, Object> suggest(@PathVariable Long id,
                                       @RequestParam(required = false) TrainCoachType type,
                                       @RequestParam(defaultValue = "1") int n) {
        TrainTrip trip = trainService.getTrip(id);
        int count = Math.max(1, Math.min(n, TrainBookingValidator.MAX_PASSENGERS));
        List<String> seats = trainService.suggestSeats(trip, type, count);
        Map<String, Object> res = new HashMap<>();
        res.put("seats", seats);
        res.put("found", !seats.isEmpty());
        return res;
    }

    @PostMapping("/dat-ve/chon-ghe")
    public String chooseSeats(@RequestParam Long tripId,
                              @RequestParam(name = "seats", required = false) List<String> seats,
                              HttpSession session, RedirectAttributes ra) {
        TrainTrip trip = trainService.getTrip(tripId);
        try {
            validator.checkTripOpen(trip, LocalDateTime.now());
            validator.checkSeatSelection(trip, seats, trainService.takenSeats(tripId));
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/tau-hoa/chuyen/" + tripId;
        }
        TrainBookingDraft draft = new TrainBookingDraft();
        draft.setTripId(tripId);
        draft.setSeatKeys(new ArrayList<>(seats));
        session.setAttribute(TrainBookingDraft.SESSION_KEY, draft);
        return "redirect:/tau-hoa/dat-ve/hanh-khach";
    }

    // ---------------- Buoc 3: thong tin hanh khach ----------------

    @GetMapping("/dat-ve/hanh-khach")
    public String passengers(HttpSession session, Model model, RedirectAttributes ra) {
        TrainBookingDraft draft = draft(session);
        if (draft == null) return noDraft(ra);
        TrainTrip trip = trainService.getTrip(draft.getTripId());

        TrainPassengerForm form = draft.getForm();
        if (form == null) {
            User u = currentUser.require();
            form = new TrainPassengerForm();
            form.setContactName(u.getFullName());
            form.setContactPhone(u.getPhone());
            form.setContactEmail(u.getEmail());
            for (int i = 0; i < draft.getSeatKeys().size(); i++) {
                TrainPassengerItem p = new TrainPassengerItem();
                if (i == 0) p.setFullName(u.getFullName());
                form.getPassengers().add(p);
            }
        }
        applySeats(trip, draft, form);
        fillPassengerModel(model, trip, form);
        return "train/passengers";
    }

    @PostMapping("/dat-ve/hanh-khach")
    public String submitPassengers(@Valid @ModelAttribute("form") TrainPassengerForm form, BindingResult result,
                                   HttpSession session, Model model, RedirectAttributes ra) {
        TrainBookingDraft draft = draft(session);
        if (draft == null) return noDraft(ra);
        TrainTrip trip = trainService.getTrip(draft.getTripId());

        if (form.getPassengers().size() != draft.getSeatKeys().size()) {
            ra.addFlashAttribute("error", "Dữ liệu hành khách không khớp với số chỗ đã chọn. Vui lòng nhập lại.");
            return "redirect:/tau-hoa/dat-ve/hanh-khach";
        }
        applySeats(trip, draft, form);
        validator.validatePassengers(trip, form, result);
        if (result.hasErrors()) {
            fillPassengerModel(model, trip, form);
            return "train/passengers";
        }
        draft.setForm(form);
        return "redirect:/tau-hoa/dat-ve/xac-nhan";
    }

    // ---------------- Buoc 4: xac nhan & giu cho ----------------

    @GetMapping("/dat-ve/xac-nhan")
    public String confirm(HttpSession session, Model model, RedirectAttributes ra) {
        TrainBookingDraft draft = draft(session);
        if (draft == null) return noDraft(ra);
        if (draft.getForm() == null) return "redirect:/tau-hoa/dat-ve/hanh-khach";
        TrainTrip trip = trainService.getTrip(draft.getTripId());
        FareQuote quote = bookingService.quoteTrain(trip, draft.getForm(), LocalDateTime.now());
        model.addAttribute("trip", trip);
        model.addAttribute("form", draft.getForm());
        model.addAttribute("quote", quote);
        model.addAttribute("holdMinutes", BookingService.TRAIN_HOLD_MINUTES);
        model.addAttribute("step", 4);
        return "train/confirm";
    }

    @PostMapping("/dat-ve/giu-cho")
    public String hold(HttpSession session, RedirectAttributes ra) {
        TrainBookingDraft draft = draft(session);
        if (draft == null || draft.getForm() == null) return noDraft(ra);
        TrainTrip trip = trainService.getTrip(draft.getTripId());
        Booking b = bookingService.holdTrain(currentUser.require(), trip, draft.getForm(), LocalDateTime.now());
        session.removeAttribute(TrainBookingDraft.SESSION_KEY);
        ra.addFlashAttribute("success", "Đã giữ chỗ thành công! Vui lòng thanh toán trong "
                + BookingService.TRAIN_HOLD_MINUTES + " phút để xuất vé.");
        return "redirect:/don-hang/" + b.getCode();
    }

    @GetMapping("/dat-ve/huy-nhap")
    public String discardDraft(HttpSession session) {
        TrainBookingDraft draft = draft(session);
        session.removeAttribute(TrainBookingDraft.SESSION_KEY);
        return draft == null ? "redirect:/tau-hoa" : "redirect:/tau-hoa/chuyen/" + draft.getTripId();
    }

    // ---------------- Helpers ----------------

    private TrainBookingDraft draft(HttpSession session) {
        Object o = session.getAttribute(TrainBookingDraft.SESSION_KEY);
        return o instanceof TrainBookingDraft d ? d : null;
    }

    private String noDraft(RedirectAttributes ra) {
        ra.addFlashAttribute("error", "Phiên đặt vé đã hết hạn. Vui lòng chọn lại chuyến tàu.");
        return "redirect:/tau-hoa";
    }

    /** Gan ma ghe tu session (khong tin du lieu an tu trinh duyet). */
    private void applySeats(TrainTrip trip, TrainBookingDraft draft, TrainPassengerForm form) {
        for (int i = 0; i < form.getPassengers().size() && i < draft.getSeatKeys().size(); i++) {
            String key = draft.getSeatKeys().get(i);
            form.getPassengers().get(i).setSeatKey(key);
            form.getPassengers().get(i).setSeatLabel(TrainSeatLayout.label(trip, key));
        }
    }

    private void fillPassengerModel(Model model, TrainTrip trip, TrainPassengerForm form) {
        model.addAttribute("trip", trip);
        model.addAttribute("form", form);
        model.addAttribute("passengerTypes", TrainPassengerType.values());
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("step", 3);
    }
}
