package com.vietgo.bus;

import com.vietgo.common.BusinessException;
import com.vietgo.common.Patterns;
import com.vietgo.domain.BusPassengerType;
import com.vietgo.domain.BusTrip;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * VALIDATION RIENG CHO VE XE KHACH.
 * - Dong ban ve truoc gio xe chay 30 phut.
 * - Moi don toi da 8 ghe; ghe phai ton tai tren so do loai xe va con trong.
 * - Diem don phai thuoc danh sach diem don cua chuyen, hoac chon "Don tan noi" va nhap dia chi (10-200 ky tu).
 * - Diem tra phai thuoc danh sach diem tra cua chuyen.
 * - Hanh ly toi da 40kg/khach.
 * - Tre em bat buoc nhap nam sinh, tuoi (tinh theo nam xe chay) tu 5 den 11.
 * - So dien thoai rieng cua hanh khach (neu nhap) phai dung dinh dang.
 * - Phai co it nhat mot nguoi lon trong don.
 */
@Component
public class BusBookingValidator {

    public static final int SALES_CLOSE_MINUTES = 30;
    public static final int MAX_SEATS = 8;
    public static final int CHILD_MIN_AGE = 5;
    public static final int CHILD_MAX_AGE = 11;

    public void checkTripOpen(BusTrip trip, LocalDateTime now) {
        if (trip == null || !trip.isActive()) {
            throw new BusinessException("Chuyến xe không tồn tại hoặc đã ngừng bán vé.");
        }
        if (now.isAfter(trip.getDepartureTime().minusMinutes(SALES_CLOSE_MINUTES))) {
            throw new BusinessException("Chuyến xe đã đóng bán vé (đóng trước giờ xuất bến "
                    + SALES_CLOSE_MINUTES + " phút).");
        }
    }

    public void checkSeatSelection(BusTrip trip, List<String> seatCodes, Collection<String> taken) {
        if (seatCodes == null || seatCodes.isEmpty()) {
            throw new BusinessException("Vui lòng chọn ít nhất 1 ghế.");
        }
        if (seatCodes.size() > MAX_SEATS) {
            throw new BusinessException("Mỗi lần đặt tối đa " + MAX_SEATS + " ghế xe khách.");
        }
        if (new HashSet<>(seatCodes).size() != seatCodes.size()) {
            throw new BusinessException("Danh sách ghế bị trùng lặp.");
        }
        for (String code : seatCodes) {
            if (!BusSeatLayout.exists(trip.getBusType(), code)) {
                throw new BusinessException("Ghế " + code + " không có trên loại xe "
                        + trip.getBusType().getLabel() + ".");
            }
            if (taken.contains(code)) {
                throw new BusinessException("Ghế " + code + " vừa được người khác đặt. Vui lòng chọn ghế khác.");
            }
        }
    }

    public void validateDetails(BusTrip trip, BusPassengerForm form, Errors errors) {
        int departureYear = trip.getDepartureTime().getYear();
        List<BusPassengerItem> list = form.getPassengers();

        if (form.getContactName() != null
                && !Patterns.normalizeName(form.getContactName()).matches(Patterns.PERSON_NAME)) {
            errors.rejectValue("contactName", "name", "Họ tên chỉ gồm chữ cái và khoảng trắng");
        }

        // Diem don / tra
        if (form.getPickupPoint() != null && !form.getPickupPoint().isBlank()) {
            if (form.isDoorPickup()) {
                String addr = form.getPickupAddress() == null ? "" : form.getPickupAddress().trim();
                if (addr.length() < 10) {
                    errors.rejectValue("pickupAddress", "required",
                            "Vui lòng nhập địa chỉ đón cụ thể (ít nhất 10 ký tự)");
                }
            } else {
                form.setPickupAddress(null);
                if (!trip.getPickupPointList().contains(form.getPickupPoint())) {
                    errors.rejectValue("pickupPoint", "invalid", "Điểm đón không thuộc lộ trình chuyến xe");
                }
            }
        }
        if (form.getDropoffPoint() != null && !form.getDropoffPoint().isBlank()
                && !trip.getDropoffPointList().contains(form.getDropoffPoint())) {
            errors.rejectValue("dropoffPoint", "invalid", "Điểm trả không thuộc lộ trình chuyến xe");
        }

        // Hanh ly
        if (form.getLuggageKg() != null && form.getLuggageKg() > BusFareCalculator.MAX_LUGGAGE_KG * list.size()) {
            errors.rejectValue("luggageKg", "max", "Hành lý tối đa " + BusFareCalculator.MAX_LUGGAGE_KG
                    + " kg/khách (đơn này tối đa " + BusFareCalculator.MAX_LUGGAGE_KG * list.size() + " kg)");
        }

        boolean hasAdult = false;
        for (int i = 0; i < list.size(); i++) {
            BusPassengerItem p = list.get(i);
            String path = "passengers[" + i + "].";

            if (p.getFullName() != null && !p.getFullName().isBlank()
                    && !Patterns.normalizeName(p.getFullName()).matches(Patterns.PERSON_NAME)) {
                errors.rejectValue(path + "fullName", "name", "Họ tên chỉ gồm chữ cái và khoảng trắng");
            }

            String phone = p.getPhone() == null ? "" : Patterns.normalizePhone(p.getPhone().trim());
            if (!phone.isEmpty() && !phone.matches(Patterns.VN_PHONE)) {
                errors.rejectValue(path + "phone", "format", "Số điện thoại không hợp lệ");
            }
            p.setPhone(phone.isEmpty() ? null : phone);

            if (p.getType() == BusPassengerType.ADULT) {
                hasAdult = true;
                p.setBirthYear(null);
            } else if (p.getType() == BusPassengerType.CHILD) {
                if (p.getBirthYear() == null) {
                    errors.rejectValue(path + "birthYear", "required", "Trẻ em cần nhập năm sinh");
                } else {
                    int age = departureYear - p.getBirthYear();
                    if (age < CHILD_MIN_AGE) {
                        errors.rejectValue(path + "birthYear", "age",
                                "Trẻ dưới 5 tuổi đi kèm người lớn, không cần mua vé");
                    } else if (age > CHILD_MAX_AGE) {
                        errors.rejectValue(path + "birthYear", "age",
                                "Từ 12 tuổi trở lên vui lòng chọn vé Người lớn");
                    }
                }
            }
        }
        if (!list.isEmpty() && !hasAdult && list.stream().allMatch(p -> p.getType() != null)) {
            errors.reject("child.alone", "Trẻ em phải đi cùng ít nhất một hành khách người lớn.");
        }
    }
}
