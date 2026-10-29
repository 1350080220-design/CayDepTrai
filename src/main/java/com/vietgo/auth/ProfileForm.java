package com.vietgo.auth;

import com.vietgo.common.Patterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Form cap nhat thong tin ca nhan. */
public class ProfileForm {

    @NotBlank(message = "Vui lòng nhập họ tên")
    @Size(min = 2, max = 100, message = "Họ tên từ 2 đến 100 ký tự")
    @Pattern(regexp = "^[\\p{L} .'-]+$", message = "Họ tên chỉ gồm chữ cái và khoảng trắng")
    private String fullName;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(regexp = Patterns.VN_PHONE, message = "Số điện thoại không hợp lệ (VD: 0912345678)")
    private String phone;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
