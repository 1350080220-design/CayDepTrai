package com.vietgo.admin;

import com.vietgo.domain.BusType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/** Form them / sua chuyen xe (quan tri). */
public class BusTripForm {

    private Long id;

    @NotBlank(message = "Vui lòng nhập tên nhà xe")
    @Size(max = 80, message = "Tên nhà xe tối đa 80 ký tự")
    private String operatorName;

    @NotBlank(message = "Vui lòng nhập biển số xe")
    @Pattern(regexp = "^[0-9]{2}[A-Z]{1,2}[0-9]?-[0-9]{3}\\.?[0-9]{2}$",
            message = "Biển số không đúng định dạng, VD: 51B-123.45")
    private String licensePlate;

    @NotBlank(message = "Vui lòng chọn nơi đi")
    private String originCity;

    @NotBlank(message = "Vui lòng chọn nơi đến")
    private String destinationCity;

    @NotNull(message = "Vui lòng nhập giờ xuất bến")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime departureTime;

    @NotNull(message = "Vui lòng nhập giờ đến dự kiến")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime arrivalTime;

    @NotNull(message = "Vui lòng chọn loại xe")
    private BusType busType = BusType.SLEEPER_36;

    @NotNull(message = "Vui lòng nhập giá vé")
    @Min(value = 50_000, message = "Giá vé tối thiểu 50.000đ")
    @Max(value = 3_000_000, message = "Giá vé tối đa 3.000.000đ")
    private Long basePrice;

    @NotBlank(message = "Nhập ít nhất một điểm đón")
    @Size(max = 500, message = "Tối đa 500 ký tự")
    private String pickupPoints;

    @NotBlank(message = "Nhập ít nhất một điểm trả")
    @Size(max = 500, message = "Tối đa 500 ký tự")
    private String dropoffPoints;

    private boolean active = true;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public String getOriginCity() {
        return originCity;
    }

    public void setOriginCity(String originCity) {
        this.originCity = originCity;
    }

    public String getDestinationCity() {
        return destinationCity;
    }

    public void setDestinationCity(String destinationCity) {
        this.destinationCity = destinationCity;
    }

    public LocalDateTime getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(LocalDateTime departureTime) {
        this.departureTime = departureTime;
    }

    public LocalDateTime getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(LocalDateTime arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public BusType getBusType() {
        return busType;
    }

    public void setBusType(BusType busType) {
        this.busType = busType;
    }

    public Long getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(Long basePrice) {
        this.basePrice = basePrice;
    }

    public String getPickupPoints() {
        return pickupPoints;
    }

    public void setPickupPoints(String pickupPoints) {
        this.pickupPoints = pickupPoints;
    }

    public String getDropoffPoints() {
        return dropoffPoints;
    }

    public void setDropoffPoints(String dropoffPoints) {
        this.dropoffPoints = dropoffPoints;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
