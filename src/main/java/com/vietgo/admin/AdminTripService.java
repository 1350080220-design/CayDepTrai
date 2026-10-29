package com.vietgo.admin;

import com.vietgo.common.NotFoundException;
import com.vietgo.domain.BusTrip;
import com.vietgo.domain.TrainCoachType;
import com.vietgo.domain.TrainTrip;
import com.vietgo.repository.BookingRepository;
import com.vietgo.repository.BusTripRepository;
import com.vietgo.repository.TrainTripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Quan ly chuyen tau / chuyen xe cho quan tri vien. */
@Service
public class AdminTripService {

    private final TrainTripRepository trainRepo;
    private final BusTripRepository busRepo;
    private final BookingRepository bookingRepo;

    public AdminTripService(TrainTripRepository trainRepo, BusTripRepository busRepo, BookingRepository bookingRepo) {
        this.trainRepo = trainRepo;
        this.busRepo = busRepo;
        this.bookingRepo = bookingRepo;
    }

    // ============================ TAU HOA ============================

    public TrainTripForm toForm(TrainTrip t) {
        TrainTripForm f = new TrainTripForm();
        f.setId(t.getId());
        f.setTrainCode(t.getTrainCode());
        f.setOriginStation(t.getOriginStation());
        f.setDestinationStation(t.getDestinationStation());
        f.setDepartureTime(t.getDepartureTime());
        f.setArrivalTime(t.getArrivalTime());
        f.setDistanceKm(t.getDistanceKm());
        f.setSoftSeatCoaches((int) t.countCoaches(TrainCoachType.SOFT_SEAT));
        f.setSleeper6Coaches((int) t.countCoaches(TrainCoachType.SLEEPER_6));
        f.setSleeper4Coaches((int) t.countCoaches(TrainCoachType.SLEEPER_4));
        f.setActive(t.isActive());
        return f;
    }

    public boolean trainHasBookings(Long id) {
        return id != null && bookingRepo.countByTrainTripId(id) > 0;
    }

    public void validateTrain(TrainTripForm f, Errors errors, LocalDateTime now) {
        if (f.getOriginStation() != null && f.getOriginStation().equals(f.getDestinationStation())) {
            errors.rejectValue("destinationStation", "same", "Ga đến phải khác ga đi");
        }
        if (f.getDepartureTime() != null && f.getArrivalTime() != null) {
            if (!f.getArrivalTime().isAfter(f.getDepartureTime())) {
                errors.rejectValue("arrivalTime", "order", "Giờ đến phải sau giờ khởi hành");
            } else if (f.getDistanceKm() != null) {
                double hours = Duration.between(f.getDepartureTime(), f.getArrivalTime()).toMinutes() / 60.0;
                double speed = f.getDistanceKm() / hours;
                if (speed > 90) {
                    errors.rejectValue("arrivalTime", "speed", String.format(
                            "Tốc độ trung bình %.0f km/h vượt quá giới hạn 90 km/h của tàu khách", speed));
                } else if (speed < 15) {
                    errors.rejectValue("arrivalTime", "speed", String.format(
                            "Tốc độ trung bình %.0f km/h quá thấp (tối thiểu 15 km/h)", speed));
                }
            }
            if (f.getId() == null && f.getDepartureTime().isBefore(now.plusHours(1))) {
                errors.rejectValue("departureTime", "past", "Chuyến mới phải khởi hành sau thời điểm hiện tại ít nhất 1 giờ");
            }
        }
        int total = nz(f.getSoftSeatCoaches()) + nz(f.getSleeper6Coaches()) + nz(f.getSleeper4Coaches());
        if (total < 1 || total > 15) {
            errors.rejectValue("softSeatCoaches", "total", "Đoàn tàu phải có từ 1 đến 15 toa khách (hiện " + total + ")");
        }
        if (f.getTrainCode() != null && f.getDepartureTime() != null) {
            String code = f.getTrainCode().trim().toUpperCase();
            boolean dup = f.getId() == null
                    ? trainRepo.existsByTrainCodeAndDepartureTime(code, f.getDepartureTime())
                    : trainRepo.existsByTrainCodeAndDepartureTimeAndIdNot(code, f.getDepartureTime(), f.getId());
            if (dup) errors.rejectValue("trainCode", "dup", "Tàu " + code + " đã có chuyến khởi hành vào thời điểm này");
        }
        if (f.getId() != null && trainHasBookings(f.getId())) {
            TrainTrip old = trainRepo.findById(f.getId()).orElseThrow();
            TrainTripForm o = toForm(old);
            boolean structural = !Objects.equals(o.getOriginStation(), f.getOriginStation())
                    || !Objects.equals(o.getDestinationStation(), f.getDestinationStation())
                    || !Objects.equals(o.getDistanceKm(), f.getDistanceKm())
                    || !Objects.equals(o.getSoftSeatCoaches(), f.getSoftSeatCoaches())
                    || !Objects.equals(o.getSleeper6Coaches(), f.getSleeper6Coaches())
                    || !Objects.equals(o.getSleeper4Coaches(), f.getSleeper4Coaches());
            if (structural) {
                errors.reject("locked", "Chuyến đã có khách đặt: không được đổi hành trình, cự ly hay cơ cấu toa. "
                        + "Chỉ có thể đổi giờ chạy hoặc tạm dừng bán.");
            }
        }
    }

    @Transactional
    public TrainTrip saveTrain(TrainTripForm f) {
        TrainTrip t = f.getId() == null ? new TrainTrip()
                : trainRepo.findById(f.getId()).orElseThrow(() -> new NotFoundException("Không tìm thấy chuyến tàu"));
        boolean rebuildCoaches = f.getId() == null || !trainHasBookings(f.getId());
        t.setTrainCode(f.getTrainCode().trim().toUpperCase());
        t.setOriginStation(f.getOriginStation());
        t.setDestinationStation(f.getDestinationStation());
        t.setDepartureTime(f.getDepartureTime());
        t.setArrivalTime(f.getArrivalTime());
        t.setDistanceKm(f.getDistanceKm());
        t.setActive(f.isActive());
        if (rebuildCoaches) {
            t.getCoaches().clear();
            if (t.getId() != null) {
                trainRepo.saveAndFlush(t); // xoa toa cu truoc khi them toa moi (tranh trung so toa)
            }
            int no = 1;
            for (int i = 0; i < nz(f.getSoftSeatCoaches()); i++) t.addCoach(no++, TrainCoachType.SOFT_SEAT);
            for (int i = 0; i < nz(f.getSleeper6Coaches()); i++) t.addCoach(no++, TrainCoachType.SLEEPER_6);
            for (int i = 0; i < nz(f.getSleeper4Coaches()); i++) t.addCoach(no++, TrainCoachType.SLEEPER_4);
        }
        return trainRepo.save(t);
    }

    @Transactional
    public TrainTrip toggleTrain(Long id) {
        TrainTrip t = trainRepo.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy chuyến tàu"));
        t.setActive(!t.isActive());
        return t;
    }

    // ============================ XE KHACH ============================

    public BusTripForm toForm(BusTrip b) {
        BusTripForm f = new BusTripForm();
        f.setId(b.getId());
        f.setOperatorName(b.getOperatorName());
        f.setLicensePlate(b.getLicensePlate());
        f.setOriginCity(b.getOriginCity());
        f.setDestinationCity(b.getDestinationCity());
        f.setDepartureTime(b.getDepartureTime());
        f.setArrivalTime(b.getArrivalTime());
        f.setBusType(b.getBusType());
        f.setBasePrice(b.getBasePrice());
        f.setPickupPoints(String.join("\n", b.getPickupPointList()));
        f.setDropoffPoints(String.join("\n", b.getDropoffPointList()));
        f.setActive(b.isActive());
        return f;
    }

    public boolean busHasBookings(Long id) {
        return id != null && bookingRepo.countByBusTripId(id) > 0;
    }

    public void validateBus(BusTripForm f, Errors errors, LocalDateTime now) {
        if (f.getOriginCity() != null && f.getOriginCity().equals(f.getDestinationCity())) {
            errors.rejectValue("destinationCity", "same", "Nơi đến phải khác nơi đi");
        }
        if (f.getDepartureTime() != null && f.getArrivalTime() != null) {
            if (!f.getArrivalTime().isAfter(f.getDepartureTime())) {
                errors.rejectValue("arrivalTime", "order", "Giờ đến phải sau giờ xuất bến");
            } else if (Duration.between(f.getDepartureTime(), f.getArrivalTime()).toHours() > 48) {
                errors.rejectValue("arrivalTime", "long", "Hành trình xe khách tối đa 48 giờ");
            }
            if (f.getId() == null && f.getDepartureTime().isBefore(now.plusHours(1))) {
                errors.rejectValue("departureTime", "past", "Chuyến mới phải xuất bến sau thời điểm hiện tại ít nhất 1 giờ");
            }
        }
        if (f.getBasePrice() != null && f.getBasePrice() % 1000 != 0) {
            errors.rejectValue("basePrice", "round", "Giá vé phải là bội số của 1.000đ");
        }
        checkPoints(f.getPickupPoints(), "pickupPoints", errors);
        checkPoints(f.getDropoffPoints(), "dropoffPoints", errors);
        if (f.getId() != null && busHasBookings(f.getId())) {
            BusTrip old = busRepo.findById(f.getId()).orElseThrow();
            if (old.getBusType() != f.getBusType()) {
                errors.rejectValue("busType", "locked", "Chuyến đã có khách đặt, không thể đổi loại xe (sơ đồ ghế)");
            }
            if (!Objects.equals(old.getOriginCity(), f.getOriginCity())
                    || !Objects.equals(old.getDestinationCity(), f.getDestinationCity())) {
                errors.reject("locked", "Chuyến đã có khách đặt, không thể đổi nơi đi / nơi đến.");
            }
        }
    }

    private void checkPoints(String raw, String field, Errors errors) {
        if (raw == null || raw.isBlank()) return;
        List<String> pts = splitLines(raw);
        if (pts.isEmpty()) {
            errors.rejectValue(field, "empty", "Nhập ít nhất một điểm");
        } else if (pts.size() > 8) {
            errors.rejectValue(field, "many", "Tối đa 8 điểm");
        } else if (pts.stream().anyMatch(p -> p.length() < 3 || p.length() > 150)) {
            errors.rejectValue(field, "len", "Mỗi điểm dài từ 3 đến 150 ký tự");
        } else if (pts.stream().distinct().count() != pts.size()) {
            errors.rejectValue(field, "dup", "Có điểm bị trùng lặp");
        }
    }

    private static List<String> splitLines(String raw) {
        return Arrays.stream(raw.split("[\\r\\n;]+")).map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    @Transactional
    public BusTrip saveBus(BusTripForm f) {
        BusTrip b = f.getId() == null ? new BusTrip()
                : busRepo.findById(f.getId()).orElseThrow(() -> new NotFoundException("Không tìm thấy chuyến xe"));
        b.setOperatorName(f.getOperatorName().trim());
        b.setLicensePlate(f.getLicensePlate().trim().toUpperCase());
        b.setOriginCity(f.getOriginCity());
        b.setDestinationCity(f.getDestinationCity());
        b.setDepartureTime(f.getDepartureTime());
        b.setArrivalTime(f.getArrivalTime());
        b.setBusType(f.getBusType());
        b.setBasePrice(f.getBasePrice());
        b.setPickupPoints(String.join(";", splitLines(f.getPickupPoints())));
        b.setDropoffPoints(String.join(";", splitLines(f.getDropoffPoints())));
        b.setActive(f.isActive());
        return busRepo.save(b);
    }

    @Transactional
    public BusTrip toggleBus(Long id) {
        BusTrip b = busRepo.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy chuyến xe"));
        b.setActive(!b.isActive());
        return b;
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }
}
