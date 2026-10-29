package com.vietgo.bus;

import com.vietgo.domain.BusPassengerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Thong tin mot hanh khach xe khach (gan voi mot ghe). */
public class BusPassengerItem {

    private String seatCode;

    private String seatLabel;

    @NotBlank(message = "Vui lòng nhập họ tên hành khách")
    @Size(max = 60, message = "Họ tên tối đa 60 ký tự")
    private String fullName;

    @NotNull(message = "Vui lòng chọn đối tượng")
    private BusPassengerType type = BusPassengerType.ADULT;

    /** Nam sinh - bat buoc voi tre em de xac minh do tuoi. */
    private Integer birthYear;

    /** So dien thoai rieng cua hanh khach (khong bat buoc). */
    @Size(max = 15, message = "Số điện thoại quá dài")
    private String phone;

    public String getSeatCode() {
        return seatCode;
    }

    public void setSeatCode(String seatCode) {
        this.seatCode = seatCode;
    }

    public String getSeatLabel() {
        return seatLabel;
    }

    public void setSeatLabel(String seatLabel) {
        this.seatLabel = seatLabel;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public BusPassengerType getType() {
        return type;
    }

    public void setType(BusPassengerType type) {
        this.type = type;
    }

    public Integer getBirthYear() {
        return birthYear;
    }

    public void setBirthYear(Integer birthYear) {
        this.birthYear = birthYear;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
