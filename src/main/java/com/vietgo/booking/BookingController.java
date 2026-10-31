package com.vietgo.booking;

import com.vietgo.auth.CurrentUser;
import com.vietgo.common.BusinessException;
import com.vietgo.common.RefundQuote;
import com.vietgo.domain.Booking;
import com.vietgo.domain.BookingStatus;
import com.vietgo.domain.PaymentMethod;
import com.vietgo.domain.TicketType;
import com.vietgo.domain.User;
import com.vietgo.route.RoutePlan;
import com.vietgo.route.RoutePlanner;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

/** Don hang cua toi: xem, thanh toan, huy, in ve. */
@Controller
@RequestMapping("/don-hang")
public class BookingController {

    private final BookingService bookingService;
    private final CurrentUser currentUser;
    private final QrCodeService qr;
    private final RoutePlanner routePlanner;

    public BookingController(BookingService bookingService, CurrentUser currentUser, QrCodeService qr,
                             RoutePlanner routePlanner) {
        this.bookingService = bookingService;
        this.currentUser = currentUser;
        this.qr = qr;
        this.routePlanner = routePlanner;
    }

    @GetMapping
    public String list(@RequestParam(required = false) BookingStatus status,
                       @RequestParam(required = false) TicketType type,
                       Model model) {
        User u = currentUser.require();
        List<Booking> all = bookingService.listForUser(u);
        List<Booking> filtered = all.stream()
                .filter(b -> status == null || b.getStatus() == status)
                .filter(b -> type == null || b.getTicketType() == type)
                .toList();
        model.addAttribute("bookings", filtered);
        model.addAttribute("allCount", all.size());
        model.addAttribute("heldCount", all.stream().filter(Booking::isHeld).count());
        model.addAttribute("paidCount", all.stream().filter(Booking::isPaid).count());
        model.addAttribute("status", status);
        model.addAttribute("type", type);
        model.addAttribute("statuses", BookingStatus.values());
        return "booking/list";
    }

    @GetMapping("/{code}")
    public String detail(@PathVariable String code, Model model) {
        User u = currentUser.require();
        Booking b = bookingService.getForUser(code, u);
        model.addAttribute("b", b);
        model.addAttribute("methods", PaymentMethod.values());
        RoutePlan plan = routePlanner.forBooking(b);
        if (plan != null) {
            model.addAttribute("plan", plan);
            model.addAttribute("routeJson", routePlanner.toJson(plan));
        }
        if (b.isPaid()) {
            model.addAttribute("qr", qr.toDataUri("VIETGO|" + b.getCode() + "|" + b.getTicketType().name()
                    + "|" + b.getPassengerCount(), 220));
            model.addAttribute("refund", bookingService.refundQuote(b, LocalDateTime.now()));
        }
        return "booking/detail";
    }

    @PostMapping("/{code}/thanh-toan")
    public String pay(@PathVariable String code, @RequestParam(required = false) PaymentMethod method,
                      RedirectAttributes ra) {
        User u = currentUser.require();
        bookingService.pay(code, u, method, LocalDateTime.now());
        ra.addFlashAttribute("paid", true);
        ra.addFlashAttribute("success", "Thanh toán thành công! Vé điện tử đã được xuất.");
        return "redirect:/don-hang/" + code;
    }

    @GetMapping("/{code}/huy")
    public String cancelPreview(@PathVariable String code, Model model, RedirectAttributes ra) {
        User u = currentUser.require();
        Booking b = bookingService.getForUser(code, u);
        if (b.getStatus() == BookingStatus.CANCELLED || b.getStatus() == BookingStatus.EXPIRED) {
            ra.addFlashAttribute("error", "Đơn này đã hủy hoặc hết hạn.");
            return "redirect:/don-hang/" + code;
        }
        RefundQuote q = b.isPaid() ? bookingService.refundQuote(b, LocalDateTime.now()) : null;
        model.addAttribute("b", b);
        model.addAttribute("refund", q);
        return "booking/cancel";
    }

    @PostMapping("/{code}/huy")
    public String cancel(@PathVariable String code, @RequestParam(required = false) String reason,
                         RedirectAttributes ra) {
        User u = currentUser.require();
        Booking b = bookingService.cancelByCustomer(code, u, reason, LocalDateTime.now());
        ra.addFlashAttribute("success", b.getRefundAmount() > 0
                ? "Đã hủy vé. Số tiền hoàn sẽ được chuyển về tài khoản thanh toán trong 3–7 ngày làm việc."
                : "Đã hủy đơn giữ chỗ, ghế đã được trả lại.");
        return "redirect:/don-hang/" + code;
    }

    @GetMapping("/{code}/ve")
    public String ticket(@PathVariable String code, Model model) {
        User u = currentUser.require();
        Booking b = bookingService.getForUser(code, u);
        if (!b.isPaid()) {
            throw new BusinessException("Vé điện tử chỉ có sau khi thanh toán.");
        }
        model.addAttribute("b", b);
        model.addAttribute("qr", qr.toDataUri("VIETGO|" + b.getCode() + "|" + b.getTicketType().name()
                + "|" + b.getPassengerCount(), 260));
        return "booking/ticket";
    }
}
