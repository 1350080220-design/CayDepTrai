package com.vietgo.train;

import com.vietgo.domain.TrainPassengerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** Thong tin mot hanh khach tau hoa (gan voi mot ghe da chon). */
public class TrainPassengerItem {

    private String seatKey;

    private String seatLabel;

    @NotBlank(message = "Vui lòng nhập họ tên hành khách")
    @Size(max = 60, message = "Họ tên tối đa 60 ký tự")
    private String fullName;

    @NotNull(message = "Vui lòng chọn đối tượng")
    private TrainPassengerType type = TrainPassengerType.ADULT;

    @NotNull(message = "Vui lòng nhập ngày sinh")
    @Past(message = "Ngày sinh phải trước hôm nay")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate birthDate;

    /** CCCD 12 so hoac ho chieu - bat buoc voi khach tu 14 tuoi. */
    @Size(max = 20, message = "Số giấy tờ quá dài")
    private String idNumber;

    /** The HS-SV - bat buoc voi doi tuong sinh vien. */
    @Size(max = 20, message = "Mã thẻ quá dài")
    private String studentCardNo;

    public String getSeatKey() {
        return seatKey;
    }

    public void setSeatKey(String seatKey) {
        this.seatKey = seatKey;
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

    public TrainPassengerType getType() {
        return type;
    }

    public void setType(TrainPassengerType type) {
        this.type = type;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    public String getStudentCardNo() {
        return studentCardNo;
    }

    public void setStudentCardNo(String studentCardNo) {
        this.studentCardNo = studentCardNo;
    }
}
