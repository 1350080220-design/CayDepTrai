package com.vietgo.admin;

import com.vietgo.bus.BusService;
import com.vietgo.domain.BusTrip;
import com.vietgo.domain.BusType;
import com.vietgo.domain.TicketType;
import com.vietgo.domain.TrainTrip;
import com.vietgo.repository.BusTripRepository;
import com.vietgo.repository.SeatLockRepository;
import com.vietgo.repository.TrainTripRepository;
import com.vietgo.train.TrainService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Quan ly chuyen tau va chuyen xe. */
@Controller
@RequestMapping("/admin")
public class AdminTripController {

    private final AdminTripService service;
    private final TrainTripRepository trainRepo;
    private final BusTripRepository busRepo;
    private final SeatLockRepository lockRepo;

    public AdminTripController(AdminTripService service, TrainTripRepository trainRepo, BusTripRepository busRepo,
                               SeatLockRepository lockRepo) {
        this.service = service;
        this.trainRepo = trainRepo;
        this.busRepo = busRepo;
        this.lockRepo = lockRepo;
    }

    // ============================ TAU HOA ============================

    @GetMapping("/tau-hoa")
    public String trainList(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                            Model model) {
        LocalDate day = date == null ? LocalDate.now() : date;
        List<TrainTrip> trips = trainRepo.findByDepartureTimeBetweenOrderByDepartureTimeAsc(
                day.atStartOfDay(), day.plusDays(1).atStartOfDay());
        Map<Long, Long> sold = new HashMap<>();
        for (TrainTrip t : trips) sold.put(t.getId(), lockRepo.countByTicketTypeAndTripId(TicketType.TRAIN, t.getId()));
        model.addAttribute("trips", trips);
        model.addAttribute("sold", sold);
        model.addAttribute("day", day);
        model.addAttribute("section", "trains");
        return "admin/train-list";
    }

    @GetMapping("/tau-hoa/moi")
    public String trainNew(Model model) {
        TrainTripForm f = new TrainTripForm();
        f.setDepartureTime(LocalDateTime.now().plusDays(1).withHour(19).withMinute(0).withSecond(0).withNano(0));
        f.setArrivalTime(f.getDepartureTime().plusHours(16));
        f.setDistanceKm(791);
        return trainForm(model, f, false);
    }

    @GetMapping("/tau-hoa/{id}/sua")
    public String trainEdit(@PathVariable Long id, Model model) {
        TrainTrip t = trainRepo.findById(id).orElseThrow(() -> new com.vietgo.common.NotFoundException("Không tìm thấy"));
        return trainForm(model, service.toForm(t), service.trainHasBookings(id));
    }

    @PostMapping("/tau-hoa/luu")
    public String trainSave(@Valid @ModelAttribute("form") TrainTripForm form, BindingResult result,
                            Model model, RedirectAttributes ra) {
        service.validateTrain(form, result, LocalDateTime.now());
        if (result.hasErrors()) {
            return trainForm(model, form, service.trainHasBookings(form.getId()));
        }
        TrainTrip t = service.saveTrain(form);
        ra.addFlashAttribute("success", "Đã lưu chuyến tàu " + t.getTrainCode() + ".");
        return "redirect:/admin/tau-hoa";
    }

    @PostMapping("/tau-hoa/{id}/trang-thai")
    public String trainToggle(@PathVariable Long id, RedirectAttributes ra) {
        TrainTrip t = service.toggleTrain(id);
        ra.addFlashAttribute("success", (t.isActive() ? "Đã mở bán" : "Đã tạm dừng bán") + " chuyến " + t.getTrainCode() + ".");
        return "redirect:/admin/tau-hoa";
    }

    private String trainForm(Model model, TrainTripForm form, boolean locked) {
        model.addAttribute("form", form);
        model.addAttribute("stations", TrainService.STATIONS);
        model.addAttribute("locked", locked);
        model.addAttribute("section", "trains");
        return "admin/train-form";
    }

    // ============================ XE KHACH ============================

    @GetMapping("/xe-khach")
    public String busList(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                          Model model) {
        LocalDate day = date == null ? LocalDate.now() : date;
        List<BusTrip> trips = busRepo.findByDepartureTimeBetweenOrderByDepartureTimeAsc(
                day.atStartOfDay(), day.plusDays(1).atStartOfDay());
        Map<Long, Long> sold = new HashMap<>();
        for (BusTrip t : trips) sold.put(t.getId(), lockRepo.countByTicketTypeAndTripId(TicketType.BUS, t.getId()));
        model.addAttribute("trips", trips);
        model.addAttribute("sold", sold);
        model.addAttribute("day", day);
        model.addAttribute("section", "buses");
        return "admin/bus-list";
    }

    @GetMapping("/xe-khach/moi")
    public String busNew(Model model) {
        BusTripForm f = new BusTripForm();
        f.setDepartureTime(LocalDateTime.now().plusDays(1).withHour(22).withMinute(0).withSecond(0).withNano(0));
        f.setArrivalTime(f.getDepartureTime().plusHours(7));
        f.setBasePrice(280_000L);
        return busForm(model, f, false);
    }

    @GetMapping("/xe-khach/{id}/sua")
    public String busEdit(@PathVariable Long id, Model model) {
        BusTrip b = busRepo.findById(id).orElseThrow(() -> new com.vietgo.common.NotFoundException("Không tìm thấy"));
        return busForm(model, service.toForm(b), service.busHasBookings(id));
    }

    @PostMapping("/xe-khach/luu")
    public String busSave(@Valid @ModelAttribute("form") BusTripForm form, BindingResult result,
                          Model model, RedirectAttributes ra) {
        service.validateBus(form, result, LocalDateTime.now());
        if (result.hasErrors()) {
            return busForm(model, form, service.busHasBookings(form.getId()));
        }
        BusTrip b = service.saveBus(form);
        ra.addFlashAttribute("success", "Đã lưu chuyến xe " + b.getOperatorName() + " ("
                + b.getOriginCity() + " → " + b.getDestinationCity() + ").");
        return "redirect:/admin/xe-khach";
    }

    @PostMapping("/xe-khach/{id}/trang-thai")
    public String busToggle(@PathVariable Long id, RedirectAttributes ra) {
        BusTrip b = service.toggleBus(id);
        ra.addFlashAttribute("success", (b.isActive() ? "Đã mở bán" : "Đã tạm dừng bán") + " chuyến xe #" + b.getId() + ".");
        return "redirect:/admin/xe-khach";
    }

    private String busForm(Model model, BusTripForm form, boolean locked) {
        model.addAttribute("form", form);
        model.addAttribute("cities", BusService.CITIES);
        model.addAttribute("busTypes", BusType.values());
        model.addAttribute("locked", locked);
        model.addAttribute("section", "buses");
        return "admin/bus-form";
    }
}
