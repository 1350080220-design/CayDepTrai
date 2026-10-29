package com.vietgo.train;

import com.vietgo.common.BusinessException;
import com.vietgo.common.Patterns;
import com.vietgo.domain.TrainPassengerType;
import com.vietgo.domain.TrainTrip;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * VALIDATION RIENG CHO VE TAU HOA.
 * - Dong ban ve truoc gio tau chay 60 phut.
 * - Moi don toi da 6 hanh khach, ghe phai ton tai va con trong.
 * - Moi hanh khach phai co ngay sinh; tuoi tinh theo ngay tau chay phai khop doi tuong da chon.
 * - Tre duoi 6 tuoi duoc mien ve (khong dat cho).
 * - Khach tu 14 tuoi bat buoc co CCCD 12 so hoac ho chieu (1 chu + 7 so); khong trung trong cung don.
 * - Doi tuong sinh vien bat buoc nhap ma the HS-SV.
 * - Tre em phai di cung it nhat 1 hanh khach khong phai tre em.
 */
@Component
public class TrainBookingValidator {

    public static final int SALES_CLOSE_MINUTES = 60;
    public static final int MAX_PASSENGERS = 6;
    public static final int FREE_CHILD_AGE = 6;
    public static final int ID_REQUIRED_AGE = 14;

    /** Kiem tra chuyen tau con mo ban. */
    public void checkTripOpen(TrainTrip trip, LocalDateTime now) {
        if (trip == null || !trip.isActive()) {
            throw new BusinessException("Chuyến tàu không tồn tại hoặc đã ngừng bán vé.");
        }
        if (now.isAfter(trip.getDepartureTime().minusMinutes(SALES_CLOSE_MINUTES))) {
            throw new BusinessException("Chuyến tàu " + trip.getTrainCode()
                    + " đã đóng bán vé (đóng trước giờ chạy " + SALES_CLOSE_MINUTES + " phút).");
        }
    }

    /** Buoc 1: kiem tra danh sach ghe da chon. */
    public void checkSeatSelection(TrainTrip trip, List<String> seatKeys, Collection<String> takenSeats) {
        if (seatKeys == null || seatKeys.isEmpty()) {
            throw new BusinessException("Vui lòng chọn ít nhất 1 chỗ.");
        }
        if (seatKeys.size() > MAX_PASSENGERS) {
            throw new BusinessException("Mỗi lần đặt tối đa " + MAX_PASSENGERS + " vé tàu.");
        }
        if (new HashSet<>(seatKeys).size() != seatKeys.size()) {
            throw new BusinessException("Danh sách chỗ bị trùng lặp.");
        }
        for (String key : seatKeys) {
            if (!TrainSeatLayout.exists(trip, key)) {
                throw new BusinessException("Chỗ " + key + " không tồn tại trên tàu " + trip.getTrainCode() + ".");
            }
            if (takenSeats.contains(key)) {
                throw new BusinessException(TrainSeatLayout.label(trip, key)
                        + " vừa được người khác đặt. Vui lòng chọn chỗ khác.");
            }
        }
    }

    /** Buoc 2: kiem tra thong tin hanh khach (ghi loi vao tung o nhap). */
    public void validatePassengers(TrainTrip trip, TrainPassengerForm form, Errors errors) {
        LocalDate travelDate = trip.getDepartureTime().toLocalDate();
        List<TrainPassengerItem> list = form.getPassengers();

        if (form.getContactName() != null
                && !Patterns.normalizeName(form.getContactName()).matches(Patterns.PERSON_NAME)) {
            errors.rejectValue("contactName", "name", "Họ tên chỉ gồm chữ cái và khoảng trắng");
        }

        Set<String> ids = new HashSet<>();
        boolean hasAccompanyingAdult = false;

        for (int i = 0; i < list.size(); i++) {
            TrainPassengerItem p = list.get(i);
            String path = "passengers[" + i + "].";

            if (p.getFullName() != null && !p.getFullName().isBlank()
                    && !Patterns.normalizeName(p.getFullName()).matches(Patterns.PERSON_NAME)) {
                errors.rejectValue(path + "fullName", "name", "Họ tên chỉ gồm chữ cái và khoảng trắng");
            }
            if (p.getType() == null || p.getBirthDate() == null) {
                continue; // da co loi @NotNull
            }

            int age = Period.between(p.getBirthDate(), travelDate).getYears();
            TrainPassengerType type = p.getType();

            if (age < FREE_CHILD_AGE) {
                errors.rejectValue(path + "birthDate", "age",
                        "Trẻ dưới 6 tuổi được miễn vé, đi cùng người lớn và không cần đặt chỗ");
                continue;
            }
            if (age < type.getMinAge() || age > type.getMaxAge()) {
                errors.rejectValue(path + "type", "age", ageMessage(type, age));
            }

            if (type != TrainPassengerType.CHILD) {
                hasAccompanyingAdult = true;
            }

            String id = p.getIdNumber() == null ? "" : p.getIdNumber().trim().toUpperCase();
            p.setIdNumber(id.isEmpty() ? null : id);
            if (age >= ID_REQUIRED_AGE) {
                if (id.isEmpty()) {
                    errors.rejectValue(path + "idNumber", "required",
                            "Hành khách từ 14 tuổi phải có CCCD hoặc hộ chiếu");
                } else if (!id.matches(Patterns.CCCD) && !id.matches(Patterns.PASSPORT)) {
                    errors.rejectValue(path + "idNumber", "format",
                            "CCCD gồm 12 chữ số, hộ chiếu gồm 1 chữ cái + 7 chữ số");
                } else if (!ids.add(id)) {
                    errors.rejectValue(path + "idNumber", "duplicate",
                            "Số giấy tờ bị trùng với hành khách khác trong đơn");
                }
            } else if (!id.isEmpty() && !id.matches(Patterns.CCCD) && !id.matches(Patterns.PASSPORT)) {
                errors.rejectValue(path + "idNumber", "format", "Số giấy tờ không hợp lệ");
            }

            if (type == TrainPassengerType.STUDENT) {
                String card = p.getStudentCardNo() == null ? "" : p.getStudentCardNo().trim();
                if (card.isEmpty()) {
                    errors.rejectValue(path + "studentCardNo", "required",
                            "Đối tượng sinh viên phải nhập mã thẻ HS-SV");
                } else if (!card.matches(Patterns.STUDENT_CARD)) {
                    errors.rejectValue(path + "studentCardNo", "format",
                            "Mã thẻ gồm 6–15 chữ cái hoặc chữ số");
                }
            } else {
                p.setStudentCardNo(null);
            }
        }

        boolean allTyped = list.stream().allMatch(p -> p.getType() != null && p.getBirthDate() != null);
        if (!list.isEmpty() && allTyped && !hasAccompanyingAdult) {
            errors.reject("child.alone",
                    "Trẻ em phải đi cùng ít nhất một hành khách người lớn, sinh viên hoặc người cao tuổi.");
        }
    }

    private String ageMessage(TrainPassengerType type, int age) {
        return switch (type) {
            case CHILD -> "Trẻ em được giảm giá khi từ 6 đến dưới 10 tuổi (hiện " + age + " tuổi)";
            case STUDENT -> "Ưu đãi HS-SV áp dụng từ 10 đến 35 tuổi (hiện " + age + " tuổi)";
            case ELDERLY -> "Người cao tuổi phải từ 60 tuổi (hiện " + age + " tuổi)";
            case ADULT -> "Khách dưới 10 tuổi vui lòng chọn đối tượng Trẻ em (hiện " + age + " tuổi)";
        };
    }
}
