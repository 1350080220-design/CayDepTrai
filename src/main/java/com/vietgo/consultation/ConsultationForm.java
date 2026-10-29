package com.vietgo.consultation;

import com.vietgo.common.Patterns;
import com.vietgo.domain.ConsultationChannel;
import com.vietgo.domain.TicketType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** Form dang ky lich hen tu van. */
public class ConsultationForm {

    @NotBlank(message = "Vui lòng nhập họ tên")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    @Pattern(regexp = "^[\\p{L} .'-]+$", message = "Họ tên chỉ gồm chữ cái và khoảng trắng")
    private String fullName;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(regexp = Patterns.VN_PHONE, message = "Số điện thoại không hợp lệ (VD: 0912345678)")
    private String phone;

    @NotBlank(message = "Vui lòng nhập email")
    @Email(message = "Email không hợp lệ")
    @Size(max = 120, message = "Email tối đa 120 ký tự")
    private String email;

    @NotNull(message = "Vui lòng chọn loại vé cần tư vấn")
    private TicketType ticketType = TicketType.TRAIN;

    @NotNull(message = "Vui lòng chọn hình thức tư vấn")
    private ConsultationChannel channel = ConsultationChannel.PHONE;

    @NotBlank(message = "Vui lòng chọn nội dung cần tư vấn")
    private String topic;

    @NotNull(message = "Vui lòng chọn ngày hẹn")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    @NotBlank(message = "Vui lòng chọn khung giờ")
    private String slot;

    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    private String note;

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public TicketType getTicketType() {
        return ticketType;
    }

    public void setTicketType(TicketType ticketType) {
        this.ticketType = ticketType;
    }

    public ConsultationChannel getChannel() {
        return channel;
    }

    public void setChannel(ConsultationChannel channel) {
        this.channel = channel;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getSlot() {
        return slot;
    }

    public void setSlot(String slot) {
        this.slot = slot;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
