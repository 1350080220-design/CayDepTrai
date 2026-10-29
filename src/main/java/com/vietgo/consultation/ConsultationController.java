package com.vietgo.consultation;

import com.vietgo.auth.CurrentUser;
import com.vietgo.domain.ConsultationChannel;
import com.vietgo.domain.TicketType;
import com.vietgo.domain.User;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Dang ky lich hen tu van dat ve. */
@Controller
@RequestMapping("/tu-van")
public class ConsultationController {

    private final ConsultationService service;
    private final CurrentUser currentUser;

    public ConsultationController(ConsultationService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String page(@RequestParam(required = false) TicketType type, Model model) {
        User u = currentUser.require();
        ConsultationForm form = new ConsultationForm();
        form.setFullName(u.getFullName());
        form.setPhone(u.getPhone());
        form.setEmail(u.getEmail());
        if (type != null) form.setTicketType(type);
        LocalDate d = LocalDate.now().plusDays(1);
        if (d.getDayOfWeek() == DayOfWeek.SUNDAY) d = d.plusDays(1);
        form.setDate(d);
        fill(model, form, u);
        return "consultation/index";
    }

    @GetMapping("/khung-gio")
    @ResponseBody
    public List<SlotView> slots(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.availability(date, LocalDateTime.now());
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") ConsultationForm form, BindingResult result,
                         Model model, RedirectAttributes ra) {
        User u = currentUser.require();
        service.validate(form, u, LocalDateTime.now(), result);
        if (result.hasErrors()) {
            fill(model, form, u);
            return "consultation/index";
        }
        service.create(form, u);
        ra.addFlashAttribute("success", "Đã đăng ký lịch tư vấn lúc " + form.getSlot() + " ngày "
                + form.getDate().getDayOfMonth() + "/" + form.getDate().getMonthValue()
                + ". Tư vấn viên sẽ xác nhận sớm.");
        return "redirect:/tu-van";
    }

    @PostMapping("/{id}/huy")
    public String cancel(@PathVariable Long id, RedirectAttributes ra) {
        service.cancelByUser(id, currentUser.require(), LocalDateTime.now());
        ra.addFlashAttribute("success", "Đã hủy lịch hẹn.");
        return "redirect:/tu-van";
    }

    private void fill(Model model, ConsultationForm form, User u) {
        model.addAttribute("form", form);
        model.addAttribute("topics", ConsultationService.TOPICS);
        model.addAttribute("channels", ConsultationChannel.values());
        model.addAttribute("ticketTypes", TicketType.values());
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("maxDate", LocalDate.now().plusDays(ConsultationService.MAX_DAYS_AHEAD));
        model.addAttribute("appointments", service.listForUser(u));
        model.addAttribute("slots", service.availability(form.getDate(), LocalDateTime.now()));
    }
}
