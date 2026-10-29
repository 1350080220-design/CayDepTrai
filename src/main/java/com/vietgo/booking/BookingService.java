package com.vietgo.booking;

import com.vietgo.bus.BusBookingValidator;
import com.vietgo.bus.BusFareCalculator;
import com.vietgo.bus.BusPassengerForm;
import com.vietgo.bus.BusPassengerItem;
import com.vietgo.bus.BusRefundPolicy;
import com.vietgo.common.BusinessException;
import com.vietgo.common.CodeGenerator;
import com.vietgo.common.FareQuote;
import com.vietgo.common.FeeItem;
import com.vietgo.common.NotFoundException;
import com.vietgo.common.PassengerFare;
import com.vietgo.common.Patterns;
import com.vietgo.common.RefundQuote;
import com.vietgo.domain.Booking;
import com.vietgo.domain.BookingStatus;
import com.vietgo.domain.BusTrip;
import com.vietgo.domain.PaymentMethod;
import com.vietgo.domain.SeatLock;
import com.vietgo.domain.Ticket;
import com.vietgo.domain.TicketType;
import com.vietgo.domain.TrainTrip;
import com.vietgo.domain.User;
import com.vietgo.repository.BookingRepository;
import com.vietgo.repository.SeatLockRepository;
import com.vietgo.train.TrainBookingValidator;
import com.vietgo.train.TrainFareCalculator;
import com.vietgo.train.TrainPassengerForm;
import com.vietgo.train.TrainPassengerItem;
import com.vietgo.train.TrainRefundPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Nghiep vu chung cua don dat ve: giu cho, thanh toan, huy, het han.
 * Phan tinh gia / validation / hoan huy duoc uy quyen cho tung loai ve.
 */
@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    /** Thoi gian giu cho cua tung loai ve (phut). */
    public static final int TRAIN_HOLD_MINUTES = 15;
    public static final int BUS_HOLD_MINUTES = 10;

    private final BookingRepository bookingRepo;
    private final SeatLockRepository lockRepo;
    private final TrainFareCalculator trainFare;
    private final TrainBookingValidator trainValidator;
    private final TrainRefundPolicy trainRefund;
    private final BusFareCalculator busFare;
    private final BusBookingValidator busValidator;
    private final BusRefundPolicy busRefund;

    public BookingService(BookingRepository bookingRepo, SeatLockRepository lockRepo,
                          TrainFareCalculator trainFare, TrainBookingValidator trainValidator,
                          TrainRefundPolicy trainRefund, BusFareCalculator busFare,
                          BusBookingValidator busValidator, BusRefundPolicy busRefund) {
        this.bookingRepo = bookingRepo;
        this.lockRepo = lockRepo;
        this.trainFare = trainFare;
        this.trainValidator = trainValidator;
        this.trainRefund = trainRefund;
        this.busFare = busFare;
        this.busValidator = busValidator;
        this.busRefund = busRefund;
    }

    // ===================================================================
    //  TINH GIA (khong ghi database)
    // ===================================================================

    public FareQuote quoteTrain(TrainTrip trip, TrainPassengerForm form, LocalDateTime now) {
        List<TrainFareCalculator.PassengerInput> inputs = new ArrayList<>();
        for (TrainPassengerItem p : form.getPassengers()) {
            inputs.add(new TrainFareCalculator.PassengerInput(p.getSeatKey(),
                    Patterns.normalizeName(p.getFullName()), p.getType()));
        }
        return trainFare.quote(trip, inputs, now);
    }

    public FareQuote quoteBus(BusTrip trip, BusPassengerForm form, long occupied) {
        List<BusFareCalculator.PassengerInput> inputs = new ArrayList<>();
        for (BusPassengerItem p : form.getPassengers()) {
            inputs.add(new BusFareCalculator.PassengerInput(p.getSeatCode(),
                    Patterns.normalizeName(p.getFullName()), p.getType()));
        }
        int luggage = form.getLuggageKg() == null ? 0 : form.getLuggageKg();
        return busFare.quote(trip, inputs, occupied, luggage, form.isDoorPickup());
    }

    // ===================================================================
    //  GIU CHO
    // ===================================================================

    @Transactional
    public Booking holdTrain(User user, TrainTrip trip, TrainPassengerForm form, LocalDateTime now) {
        trainValidator.checkTripOpen(trip, now);
        List<String> seats = form.getPassengers().stream().map(TrainPassengerItem::getSeatKey).toList();
        trainValidator.checkSeatSelection(trip, seats,
                new HashSet<>(lockRepo.findTaken(TicketType.TRAIN, trip.getId(), seats)));

        FareQuote quote = quoteTrain(trip, form, now);

        Booking b = newBooking(user, TicketType.TRAIN, form.getContactName(), form.getContactPhone(),
                form.getContactEmail(), now.plusMinutes(TRAIN_HOLD_MINUTES));
        b.setTrainTrip(trip);
        for (int i = 0; i < form.getPassengers().size(); i++) {
            TrainPassengerItem p = form.getPassengers().get(i);
            PassengerFare f = quote.getPassengers().get(i);
            Ticket t = new Ticket();
            t.setSeatKey(p.getSeatKey());
            t.setSeatLabel(f.getSeatLabel());
            t.setPassengerName(Patterns.normalizeName(p.getFullName()));
            t.setPassengerType(p.getType().name());
            t.setPassengerTypeLabel(p.getType().getLabel());
            t.setIdNumber(p.getIdNumber());
            t.setBirthDate(p.getBirthDate());
            t.setStudentCardNo(p.getStudentCardNo());
            t.setBasePrice(f.getBasePrice());
            t.setFinalPrice(f.getFinalPrice());
            b.addTicket(t);
        }
        return persistWithLocks(b, quote, seats, trip.getId());
    }

    @Transactional
    public Booking holdBus(User user, BusTrip trip, BusPassengerForm form, LocalDateTime now) {
        busValidator.checkTripOpen(trip, now);
        List<String> seats = form.getPassengers().stream().map(BusPassengerItem::getSeatCode).toList();
        busValidator.checkSeatSelection(trip, seats,
                new HashSet<>(lockRepo.findTaken(TicketType.BUS, trip.getId(), seats)));

        long occupied = lockRepo.countByTicketTypeAndTripId(TicketType.BUS, trip.getId());
        FareQuote quote = quoteBus(trip, form, occupied);

        Booking b = newBooking(user, TicketType.BUS, form.getContactName(), form.getContactPhone(),
                form.getContactEmail(), now.plusMinutes(BUS_HOLD_MINUTES));
        b.setBusTrip(trip);
        if (form.isDoorPickup()) {
            b.setPickupPoint("Đón tận nơi");
            b.setPickupAddress(form.getPickupAddress().trim());
        } else {
            b.setPickupPoint(form.getPickupPoint());
        }
        b.setDropoffPoint(form.getDropoffPoint());
        b.setLuggageKg(form.getLuggageKg() == null ? 0 : form.getLuggageKg());
        b.setNote(form.getNote() == null || form.getNote().isBlank() ? null : form.getNote().trim());

        for (int i = 0; i < form.getPassengers().size(); i++) {
            BusPassengerItem p = form.getPassengers().get(i);
            PassengerFare f = quote.getPassengers().get(i);
            Ticket t = new Ticket();
            t.setSeatKey(p.getSeatCode());
            t.setSeatLabel(f.getSeatLabel());
            t.setPassengerName(Patterns.normalizeName(p.getFullName()));
            t.setPassengerType(p.getType().name());
            t.setPassengerTypeLabel(p.getType().getLabel());
            t.setPhone(p.getPhone());
            t.setBasePrice(f.getBasePrice());
            t.setFinalPrice(f.getFinalPrice());
            b.addTicket(t);
        }
        return persistWithLocks(b, quote, seats, trip.getId());
    }

    private Booking newBooking(User user, TicketType type, String contactName, String phone, String email,
                               LocalDateTime holdUntil) {
        Booking b = new Booking();
        String code;
        do {
            code = CodeGenerator.next(type.getCodePrefix());
        } while (bookingRepo.existsByCode(code));
        b.setCode(code);
        b.setUser(user);
        b.setTicketType(type);
        b.setStatus(BookingStatus.HELD);
        b.setContactName(Patterns.normalizeName(contactName));
        b.setContactPhone(Patterns.normalizePhone(phone));
        b.setContactEmail(email.trim().toLowerCase());
        b.setHoldExpiresAt(holdUntil);
        return b;
    }

    /** Luu don + khoa ghe. Rang buoc UNIQUE cua bang seat_locks chan dat trung ghe dong thoi. */
    private Booking persistWithLocks(Booking b, FareQuote quote, List<String> seats, Long tripId) {
        for (FeeItem item : quote.getItems()) {
            b.addFeeLine(item.getLabel(), item.getAmount(), item.getKind());
        }
        b.setPassengerCount(seats.size());
        b.setTotalAmount(quote.getTotal());
        Booking saved = bookingRepo.saveAndFlush(b);

        List<SeatLock> locks = new ArrayList<>();
        for (String seat : seats) {
            SeatLock l = new SeatLock();
            l.setTicketType(saved.getTicketType());
            l.setTripId(tripId);
            l.setSeatKey(seat);
            l.setBookingId(saved.getId());
            locks.add(l);
        }
        lockRepo.saveAllAndFlush(locks);
        log.info("Giu cho don {} ({} cho, {}d)", saved.getCode(), seats.size(), saved.getTotalAmount());
        return saved;
    }

    // ===================================================================
    //  TRA CUU
    // ===================================================================

    @Transactional
    public Booking getForUser(String code, User user) {
        Booking b = bookingRepo.findByCode(code).orElseThrow(() -> new NotFoundException("Không tìm thấy đơn đặt vé"));
        if (!user.isAdmin() && !b.getUser().getId().equals(user.getId())) {
            throw new NotFoundException("Không tìm thấy đơn đặt vé");
        }
        expireIfNeeded(b, LocalDateTime.now());
        return b;
    }

    @Transactional
    public Booking getByCode(String code) {
        Booking b = bookingRepo.findByCode(code).orElseThrow(() -> new NotFoundException("Không tìm thấy đơn đặt vé"));
        expireIfNeeded(b, LocalDateTime.now());
        return b;
    }

    @Transactional(readOnly = true)
    public List<Booking> listForUser(User user) {
        return bookingRepo.findByUserOrderByCreatedAtDesc(user);
    }

    // ===================================================================
    //  THANH TOAN
    // ===================================================================

    @Transactional
    public Booking pay(String code, User user, PaymentMethod method, LocalDateTime now) {
        Booking b = getForUser(code, user);
        if (method == null) {
            throw new BusinessException("Vui lòng chọn phương thức thanh toán.");
        }
        if (b.getStatus() == BookingStatus.PAID) {
            throw new BusinessException("Đơn " + code + " đã được thanh toán trước đó.");
        }
        if (b.getStatus() == BookingStatus.EXPIRED) {
            throw new BusinessException("Đơn " + code + " đã hết thời gian giữ chỗ. Vui lòng đặt lại.");
        }
        if (b.getStatus() != BookingStatus.HELD) {
            throw new BusinessException("Đơn " + code + " không ở trạng thái chờ thanh toán.");
        }
        if (!now.isBefore(b.getDepartureTime())) {
            throw new BusinessException("Phương tiện đã khởi hành, không thể thanh toán.");
        }
        b.setStatus(BookingStatus.PAID);
        b.setPaymentMethod(method);
        b.setPaidAt(now);
        b.setHoldExpiresAt(null);
        log.info("Thanh toan don {} bang {}", code, method);
        return b;
    }

    // ===================================================================
    //  HUY / HOAN TIEN
    // ===================================================================

    public RefundQuote refundQuote(Booking b, LocalDateTime now) {
        return b.isTrain() ? trainRefund.quote(b, now) : busRefund.quote(b, now);
    }

    /** Khach huy: don dang giu cho -> huy mien phi; don da thanh toan -> theo chinh sach tung loai ve. */
    @Transactional
    public Booking cancelByCustomer(String code, User user, String reason, LocalDateTime now) {
        Booking b = getForUser(code, user);
        String cleanReason = reason == null || reason.isBlank() ? null : reason.trim();
        if (cleanReason != null && cleanReason.length() > 255) cleanReason = cleanReason.substring(0, 255);

        if (b.getStatus() == BookingStatus.CANCELLED || b.getStatus() == BookingStatus.EXPIRED) {
            throw new BusinessException("Đơn " + code + " đã hủy hoặc đã hết hạn giữ chỗ.");
        }
        if (b.getStatus() == BookingStatus.HELD) {
            markCancelled(b, now, 0, 0, cleanReason == null ? "Khách hủy giữ chỗ" : cleanReason);
            return b;
        }
        RefundQuote q = refundQuote(b, now);
        if (!q.isAllowed()) {
            throw new BusinessException(q.getMessage());
        }
        markCancelled(b, now, q.getCancelFee(), q.getRefundAmount(),
                cleanReason == null ? "Khách yêu cầu hủy vé" : cleanReason);
        return b;
    }

    /** Nha van hanh huy (vd: chuyen bi huy) -> hoan 100%. */
    @Transactional
    public Booking cancelByAdmin(String code, String reason, LocalDateTime now) {
        Booking b = bookingRepo.findByCode(code).orElseThrow(() -> new NotFoundException("Không tìm thấy đơn"));
        if (b.getStatus() == BookingStatus.CANCELLED || b.getStatus() == BookingStatus.EXPIRED) {
            throw new BusinessException("Đơn " + code + " đã hủy hoặc hết hạn.");
        }
        long refund = b.getStatus() == BookingStatus.PAID ? b.getTotalAmount() : 0;
        markCancelled(b, now, 0, refund, reason == null || reason.isBlank()
                ? "Nhà vận hành hủy đơn – hoàn 100%" : reason.trim());
        return b;
    }

    private void markCancelled(Booking b, LocalDateTime now, long fee, long refund, String reason) {
        b.setStatus(BookingStatus.CANCELLED);
        b.setCancelledAt(now);
        b.setCancelFee(fee);
        b.setRefundAmount(refund);
        b.setCancelReason(reason);
        b.setHoldExpiresAt(null);
        lockRepo.deleteByBookingId(b.getId());
        log.info("Huy don {} - phi {} - hoan {}", b.getCode(), fee, refund);
    }

    // ===================================================================
    //  HET HAN GIU CHO
    // ===================================================================

    private void expireIfNeeded(Booking b, LocalDateTime now) {
        if (b.getStatus() == BookingStatus.HELD && b.getHoldExpiresAt() != null && !now.isBefore(b.getHoldExpiresAt())) {
            b.setStatus(BookingStatus.EXPIRED);
            lockRepo.deleteByBookingId(b.getId());
            log.info("Don {} het han giu cho", b.getCode());
        }
    }

    @Transactional
    public int expireHolds(LocalDateTime now) {
        List<Booking> list = bookingRepo.findByStatusAndHoldExpiresAtBefore(BookingStatus.HELD, now);
        list.forEach(b -> expireIfNeeded(b, now));
        return list.size();
    }
}
