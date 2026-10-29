package com.vietgo.consultation;

import com.vietgo.common.BusinessException;
import com.vietgo.common.NotFoundException;
import com.vietgo.common.Patterns;
import com.vietgo.domain.Consultation;
import com.vietgo.domain.ConsultationStatus;
import com.vietgo.domain.User;
import com.vietgo.repository.ConsultationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Dang ky lich hen tu van.
 * - Gio lam viec 08:00 - 20:00, moi khung 30 phut, nghi trua 12:00 - 13:00, nghi Chu nhat.
 * - Moi khung toi da 3 khach (3 tu van vien).
 * - Hen truoc it nhat 2 gio, toi da 60 ngay.
 * - Moi khach toi da 3 lich hen dang hieu luc va khong trung khung gio.
 * - Khach huy lich truoc gio hen it nhat 1 gio.
 */
@Service
public class ConsultationService {

    public static final int SLOT_CAPACITY = 3;
    public static final int MIN_LEAD_HOURS = 2;
    public static final int MAX_DAYS_AHEAD = 60;
    public static final int MAX_ACTIVE_PER_USER = 3;
    public static final int CANCEL_BEFORE_HOURS = 1;
    public static final List<String> TOPICS = List.of(
            "Tư vấn lộ trình và chọn chuyến",
            "Đặt vé đoàn / vé tập thể",
            "Chính sách giá và ưu đãi",
            "Đổi, hủy và hoàn tiền vé",
            "Hỗ trợ khác");

    private static final Set<ConsultationStatus> ACTIVE =
            EnumSet.of(ConsultationStatus.PENDING, ConsultationStatus.CONFIRMED);

    private final ConsultationRepository repo;

    public ConsultationService(ConsultationRepository repo) {
        this.repo = repo;
    }

    /** Danh sach khung gio trong ngay lam viec. */
    public static List<LocalTime> allSlots() {
        List<LocalTime> list = new ArrayList<>();
        for (LocalTime t = LocalTime.of(8, 0); t.isBefore(LocalTime.of(20, 0)); t = t.plusMinutes(30)) {
            if (t.isBefore(LocalTime.of(12, 0)) || !t.isBefore(LocalTime.of(13, 0))) list.add(t);
        }
        return list;
    }

    /** Tinh trang tung khung gio cua mot ngay. */
    @Transactional(readOnly = true)
    public List<SlotView> availability(LocalDate date, LocalDateTime now) {
        List<SlotView> result = new ArrayList<>();
        if (date == null) return result;
        boolean sunday = date.getDayOfWeek() == DayOfWeek.SUNDAY;
        boolean tooFar = date.isAfter(now.toLocalDate().plusDays(MAX_DAYS_AHEAD));

        Map<LocalDateTime, Long> counts = repo.findBookedTimes(date.atStartOfDay(), date.plusDays(1).atStartOfDay(), ACTIVE)
                .stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        for (LocalTime t : allSlots()) {
            LocalDateTime at = date.atTime(t);
            int remaining = (int) Math.max(0, SLOT_CAPACITY - counts.getOrDefault(at, 0L));
            String reason = null;
            if (sunday) reason = "Nghỉ Chủ nhật";
            else if (tooFar) reason = "Quá 60 ngày";
            else if (at.isBefore(now.plusHours(MIN_LEAD_HOURS))) reason = "Đã qua";
            else if (remaining == 0) reason = "Đã kín";
            result.add(new SlotView(t.toString(), remaining, reason == null, reason));
        }
        return result;
    }

    public void validate(ConsultationForm form, User user, LocalDateTime now, Errors errors) {
        if (form.getTopic() != null && !form.getTopic().isBlank() && !TOPICS.contains(form.getTopic())) {
            errors.rejectValue("topic", "invalid", "Nội dung tư vấn không hợp lệ");
        }
        if (form.getDate() == null || form.getSlot() == null || form.getSlot().isBlank()) return;

        LocalTime time;
        try {
            time = LocalTime.parse(form.getSlot());
        } catch (DateTimeParseException e) {
            errors.rejectValue("slot", "invalid", "Khung giờ không hợp lệ");
            return;
        }
        if (!allSlots().contains(time)) {
            errors.rejectValue("slot", "invalid", "Khung giờ ngoài giờ làm việc (08:00–20:00, nghỉ trưa 12:00–13:00)");
            return;
        }
        LocalDateTime at = form.getDate().atTime(time);
        if (form.getDate().getDayOfWeek() == DayOfWeek.SUNDAY) {
            errors.rejectValue("date", "sunday", "Trung tâm tư vấn nghỉ Chủ nhật");
        } else if (at.isBefore(now.plusHours(MIN_LEAD_HOURS))) {
            errors.rejectValue("slot", "lead", "Vui lòng đặt lịch trước ít nhất " + MIN_LEAD_HOURS + " giờ");
        } else if (form.getDate().isAfter(now.toLocalDate().plusDays(MAX_DAYS_AHEAD))) {
            errors.rejectValue("date", "far", "Chỉ nhận lịch hẹn trong vòng " + MAX_DAYS_AHEAD + " ngày tới");
        } else if (repo.countByAppointmentTimeAndStatusIn(at, ACTIVE) >= SLOT_CAPACITY) {
            errors.rejectValue("slot", "full", "Khung giờ này đã kín lịch, vui lòng chọn giờ khác");
        } else if (repo.existsByUserAndAppointmentTimeAndStatusIn(user, at, ACTIVE)) {
            errors.rejectValue("slot", "duplicate", "Bạn đã có lịch hẹn vào khung giờ này");
        } else if (repo.countByUserAndAppointmentTimeAfterAndStatusIn(user, now, ACTIVE) >= MAX_ACTIVE_PER_USER) {
            errors.reject("limit", "Bạn đang có " + MAX_ACTIVE_PER_USER
                    + " lịch hẹn chưa diễn ra. Vui lòng hoàn tất hoặc hủy bớt trước khi đặt thêm.");
        }
    }

    @Transactional
    public Consultation create(ConsultationForm form, User user) {
        Consultation c = new Consultation();
        c.setUser(user);
        c.setFullName(Patterns.normalizeName(form.getFullName()));
        c.setPhone(Patterns.normalizePhone(form.getPhone()));
        c.setEmail(form.getEmail().trim().toLowerCase());
        c.setTicketType(form.getTicketType());
        c.setChannel(form.getChannel());
        c.setTopic(form.getTopic());
        c.setAppointmentTime(form.getDate().atTime(LocalTime.parse(form.getSlot())));
        c.setNote(form.getNote() == null || form.getNote().isBlank() ? null : form.getNote().trim());
        c.setStatus(ConsultationStatus.PENDING);
        return repo.save(c);
    }

    @Transactional(readOnly = true)
    public List<Consultation> listForUser(User user) {
        return repo.findByUserOrderByAppointmentTimeDesc(user);
    }

    @Transactional(readOnly = true)
    public List<Consultation> listAll() {
        return repo.findAllByOrderByAppointmentTimeDesc();
    }

    @Transactional
    public void cancelByUser(Long id, User user, LocalDateTime now) {
        Consultation c = repo.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy lịch hẹn"));
        if (!c.getUser().getId().equals(user.getId())) {
            throw new NotFoundException("Không tìm thấy lịch hẹn");
        }
        if (!c.getStatus().isActive()) {
            throw new BusinessException("Lịch hẹn này không thể hủy (" + c.getStatus().getLabel().toLowerCase() + ").");
        }
        if (now.isAfter(c.getAppointmentTime().minusHours(CANCEL_BEFORE_HOURS))) {
            throw new BusinessException("Chỉ hủy được lịch hẹn trước giờ hẹn ít nhất " + CANCEL_BEFORE_HOURS + " giờ.");
        }
        c.setStatus(ConsultationStatus.CANCELLED);
    }

    @Transactional
    public void updateByAdmin(Long id, ConsultationStatus status, String adminNote) {
        Consultation c = repo.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy lịch hẹn"));
        if (status == null) throw new BusinessException("Vui lòng chọn trạng thái.");
        if (c.getStatus() == ConsultationStatus.CANCELLED || c.getStatus() == ConsultationStatus.DONE) {
            throw new BusinessException("Lịch hẹn đã " + c.getStatus().getLabel().toLowerCase() + ", không thể cập nhật.");
        }
        c.setStatus(status);
        if (adminNote != null && !adminNote.isBlank()) {
            c.setAdminNote(adminNote.trim().length() > 500 ? adminNote.trim().substring(0, 500) : adminNote.trim());
        }
    }
}
