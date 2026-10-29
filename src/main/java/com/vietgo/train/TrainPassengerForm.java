package com.vietgo.train;

import com.vietgo.common.Patterns;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

/** Buoc 2 dat ve tau: thong tin lien he + danh sach hanh khach. */
public class TrainPassengerForm {

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

    @Valid
    private List<TrainPassengerItem> passengers = new ArrayList<>();

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

    public List<TrainPassengerItem> getPassengers() {
        return passengers;
    }

    public void setPassengers(List<TrainPassengerItem> passengers) {
        this.passengers = passengers;
    }
}
