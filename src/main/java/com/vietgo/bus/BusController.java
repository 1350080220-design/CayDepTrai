package com.vietgo.bus;

import com.vietgo.auth.CurrentUser;
import com.vietgo.booking.BookingService;
import com.vietgo.common.BusinessException;
import com.vietgo.common.FareQuote;
import com.vietgo.domain.Booking;
import com.vietgo.domain.BusPassengerType;
import com.vietgo.domain.BusTrip;
import com.vietgo.domain.BusType;
import com.vietgo.domain.User;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * QUY TRINH DAT VE XE KHACH (4 buoc):
 *  1. Tim chuyen theo noi di - noi den - ngay di, loc theo loai xe.
 *  2. Chon ghe tren so do xe (giuong 2 tang / ghe ngoi / limousine), gia dong theo ty le lap day.
 *  3. Chon diem don (hoac don tan noi), diem tra, khai bao hanh ly, nhap hanh khach.
 *  4. Xem bang tinh gia -> Giu cho 10 phut -> Thanh toan.
 */
@Controller
@RequestMapping("/xe-khach")
public class BusController {

    private final BusService busService;
    private final BusBookingValidator validator;
    private final BookingService bookingService;
    private final CurrentUser currentUser;

    public BusController(BusService busService, BusBookingValidator validator, BookingService bookingService,
                         CurrentUser currentUser) {
        this.busService = busService;
        this.validator = validator;
        this.bookingService = bookingService;
        this.currentUser = currentUser;
    }

    // ---------------- Buoc 1 ----------------

    @GetMapping
    public String search(@RequestParam(required = false) String from,
                         @RequestParam(required = false) String to,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                         @RequestParam(required = false) BusType busType,
                         Model model) {
        LocalDate today = LocalDate.now();
        model.addAttribute("cities", BusService.CITIES);
        model.addAttribute("busTypes", BusType.values());
        model.addAttribute("today", today);
        model.addAttribute("maxDate", today.plusDays(60));
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("busType", busType);
        model.addAttribute("date", date == null ? today.plusDays(1) : date);

        boolean searched = from != null && to != null && date != null;
        model.addAttribute("searched", searched);
        if (!searched) return "bus/search";
        if (from.equals(to)) {
            model.addAttribute("searchError", "Nơi đi và nơi đến phải khác nhau.");
            return "bus/search";
        }
        if (date.isBefore(today)) {
            model.addAttribute("searchError", "Ngày đi không được trước hôm nay.");
            return "bus/search";
        }

        List<BusTrip> trips = busService.search(from, to, date, busType);
        Map<Long, Long> available = new HashMap<>();
        Map<Long, Long> minPrice = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        Map<Long, Boolean> closed = new HashMap<>();
        for (BusTrip t : trips) {
            available.put(t.getId(), busService.availableSeats(t));
            minPrice.put(t.getId(), busService.currentMinPrice(t));
            closed.put(t.getId(), now.isAfter(t.getDepartureTime().minusMinutes(BusBookingValidator.SALES_CLOSE_MINUTES)));
        }
        model.addAttribute("trips", trips);
        model.addAttribute("available", available);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("closed", closed);

        List<LocalDate> days = new ArrayList<>();
        for (int i = -3; i <= 3; i++) {
            LocalDate d = date.plusDays(i);
            if (!d.isBefore(today) && !d.isAfter(today.plusDays(60))) days.add(d);
        }
        model.addAttribute("days", days);
        if (trips.isEmpty()) model.addAttribute("routes", busService.activeRoutes());
        return "bus/search";
    }

    // ---------------- Buoc 2 ----------------

    @GetMapping("/chuyen/{id}")
    public String trip(@PathVariable Long id, Model model) {
        BusTrip trip = busService.getTrip(id);
        boolean open = true;
        String closedReason = null;
        try {
            validator.checkTripOpen(trip, LocalDateTime.now());
        } catch (BusinessException e) {
            open = false;
            closedReason = e.getMessage();
        }
        long occupied = busService.occupied(trip);
        model.addAttribute("trip", trip);
        model.addAttribute("decks", busService.buildDeckViews(trip));
        model.addAttribute("occupied", occupied);
        model.addAttribute("occupancy", trip.getCapacity() == 0 ? 0 : occupied * 100 / trip.getCapacity());
        model.addAttribute("open", open);
        model.addAttribute("closedReason", closedReason);
        model.addAttribute("maxSeats", BusBookingValidator.MAX_SEATS);
        model.addAttribute("holdMinutes", BookingService.BUS_HOLD_MINUTES);
        return "bus/trip";
    }

    @PostMapping("/dat-ve/chon-ghe")
    public String chooseSeats(@RequestParam Long tripId,
                              @RequestParam(name = "seats", required = false) List<String> seats,
                              HttpSession session, RedirectAttributes ra) {
        BusTrip trip = busService.getTrip(tripId);
        try {
            validator.checkTripOpen(trip, LocalDateTime.now());
            validator.checkSeatSelection(trip, seats, busService.takenSeats(tripId));
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/xe-khach/chuyen/" + tripId;
        }
        BusBookingDraft draft = new BusBookingDraft();
        draft.setTripId(tripId);
        draft.setSeatCodes(new ArrayList<>(seats));
        session.setAttribute(BusBookingDraft.SESSION_KEY, draft);
        return "redirect:/xe-khach/dat-ve/hanh-khach";
    }

    // ---------------- Buoc 3 ----------------

    @GetMapping("/dat-ve/hanh-khach")
    public String passengers(HttpSession session, Model model, RedirectAttributes ra) {
        BusBookingDraft draft = draft(session);
        if (draft == null) return noDraft(ra);
        BusTrip trip = busService.getTrip(draft.getTripId());

        BusPassengerForm form = draft.getForm();
        if (form == null) {
            User u = currentUser.require();
            form = new BusPassengerForm();
            form.setContactName(u.getFullName());
            form.setContactPhone(u.getPhone());
            form.setContactEmail(u.getEmail());
            List<String> pickups = trip.getPickupPointList();
            List<String> drops = trip.getDropoffPointList();
            if (!pickups.isEmpty()) form.setPickupPoint(pickups.get(0));
            if (!drops.isEmpty()) form.setDropoffPoint(drops.get(drops.size() - 1));
            for (int i = 0; i < draft.getSeatCodes().size(); i++) {
                BusPassengerItem p = new BusPassengerItem();
                if (i == 0) {
                    p.setFullName(u.getFullName());
                }
                form.getPassengers().add(p);
            }
        }
        applySeats(trip, draft, form);
        fillModel(model, trip, form);
        return "bus/passengers";
    }

    @PostMapping("/dat-ve/hanh-khach")
    public String submitPassengers(@Valid @ModelAttribute("form") BusPassengerForm form, BindingResult result,
                                   HttpSession session, Model model, RedirectAttributes ra) {
        BusBookingDraft draft = draft(session);
        if (draft == null) return noDraft(ra);
        BusTrip trip = busService.getTrip(draft.getTripId());
        if (form.getPassengers().size() != draft.getSeatCodes().size()) {
            ra.addFlashAttribute("error", "Dữ liệu hành khách không khớp với số ghế đã chọn. Vui lòng nhập lại.");
            return "redirect:/xe-khach/dat-ve/hanh-khach";
        }
        applySeats(trip, draft, form);
        validator.validateDetails(trip, form, result);
        if (result.hasErrors()) {
            fillModel(model, trip, form);
            return "bus/passengers";
        }
        draft.setForm(form);
        return "redirect:/xe-khach/dat-ve/xac-nhan";
    }

    // ---------------- Buoc 4 ----------------

    @GetMapping("/dat-ve/xac-nhan")
    public String confirm(HttpSession session, Model model, RedirectAttributes ra) {
        BusBookingDraft draft = draft(session);
        if (draft == null) return noDraft(ra);
        if (draft.getForm() == null) return "redirect:/xe-khach/dat-ve/hanh-khach";
        BusTrip trip = busService.getTrip(draft.getTripId());
        long occupied = busService.occupied(trip);
        FareQuote quote = bookingService.quoteBus(trip, draft.getForm(), occupied);
        model.addAttribute("trip", trip);
        model.addAttribute("form", draft.getForm());
        model.addAttribute("quote", quote);
        model.addAttribute("holdMinutes", BookingService.BUS_HOLD_MINUTES);
        model.addAttribute("step", 4);
        return "bus/confirm";
    }

    @PostMapping("/dat-ve/giu-cho")
    public String hold(HttpSession session, RedirectAttributes ra) {
        BusBookingDraft draft = draft(session);
        if (draft == null || draft.getForm() == null) return noDraft(ra);
        BusTrip trip = busService.getTrip(draft.getTripId());
        Booking b = bookingService.holdBus(currentUser.require(), trip, draft.getForm(), LocalDateTime.now());
        session.removeAttribute(BusBookingDraft.SESSION_KEY);
        ra.addFlashAttribute("success", "Đã giữ chỗ thành công! Vui lòng thanh toán trong "
                + BookingService.BUS_HOLD_MINUTES + " phút để xuất vé.");
        return "redirect:/don-hang/" + b.getCode();
    }

    @GetMapping("/dat-ve/huy-nhap")
    public String discardDraft(HttpSession session) {
        BusBookingDraft draft = draft(session);
        session.removeAttribute(BusBookingDraft.SESSION_KEY);
        return draft == null ? "redirect:/xe-khach" : "redirect:/xe-khach/chuyen/" + draft.getTripId();
    }

    // ---------------- Helpers ----------------

    private BusBookingDraft draft(HttpSession session) {
        Object o = session.getAttribute(BusBookingDraft.SESSION_KEY);
        return o instanceof BusBookingDraft d ? d : null;
    }

    private String noDraft(RedirectAttributes ra) {
        ra.addFlashAttribute("error", "Phiên đặt vé đã hết hạn. Vui lòng chọn lại chuyến xe.");
        return "redirect:/xe-khach";
    }

    private void applySeats(BusTrip trip, BusBookingDraft draft, BusPassengerForm form) {
        for (int i = 0; i < form.getPassengers().size() && i < draft.getSeatCodes().size(); i++) {
            String code = draft.getSeatCodes().get(i);
            form.getPassengers().get(i).setSeatCode(code);
            form.getPassengers().get(i).setSeatLabel(BusSeatLayout.label(trip.getBusType(), code));
        }
    }

    private void fillModel(Model model, BusTrip trip, BusPassengerForm form) {
        model.addAttribute("trip", trip);
        model.addAttribute("form", form);
        model.addAttribute("passengerTypes", BusPassengerType.values());
        model.addAttribute("currentYear", LocalDate.now().getYear());
        model.addAttribute("doorPickup", BusFareCalculator.DOOR_PICKUP);
        model.addAttribute("step", 3);
    }
}
