package com.vietgo.bus;

import com.vietgo.common.Patterns;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

/** Buoc 2 dat ve xe: lien he, diem don/tra, hanh ly, danh sach hanh khach. */
public class BusPassengerForm {

    @NotBlank(message = "Vui lòng nhập tên người liên hệ")
    @Size(max = 100, message = "Tên tối đa 100 ký tự")
    private String contactName;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(regexp = Patterns.VN_PHONE, message = "Số điện thoại không hợp lệ (VD: 0912345678)")
    private String contactPhone;

    @NotBlank(message = "Vui lòng nhập email")
    @Email(message = "Email không hợp lệ")
    @Size(max = 120, message = "Email tối đa 120 ký tự")
    private String contactEmail;

    @NotBlank(message = "Vui lòng chọn điểm đón")
    private String pickupPoint;

    @Size(max = 200, message = "Địa chỉ tối đa 200 ký tự")
    private String pickupAddress;

    @NotBlank(message = "Vui lòng chọn điểm trả")
    private String dropoffPoint;

    @NotNull(message = "Vui lòng nhập khối lượng hành lý (0 nếu không có)")
    @Min(value = 0, message = "Khối lượng hành lý không được âm")
    private Integer luggageKg = 0;

    @Size(max = 300, message = "Ghi chú tối đa 300 ký tự")
    private String note;

    @Valid
    private List<BusPassengerItem> passengers = new ArrayList<>();

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getPickupPoint() {
        return pickupPoint;
    }

    public void setPickupPoint(String pickupPoint) {
        this.pickupPoint = pickupPoint;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public String getDropoffPoint() {
        return dropoffPoint;
    }

    public void setDropoffPoint(String dropoffPoint) {
        this.dropoffPoint = dropoffPoint;
    }

    public Integer getLuggageKg() {
        return luggageKg;
    }

    public void setLuggageKg(Integer luggageKg) {
        this.luggageKg = luggageKg;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public List<BusPassengerItem> getPassengers() {
        return passengers;
    }

    public void setPassengers(List<BusPassengerItem> passengers) {
        this.passengers = passengers;
    }

    public boolean isDoorPickup() {
        return BusFareCalculator.DOOR_PICKUP.equals(pickupPoint);
    }
}
