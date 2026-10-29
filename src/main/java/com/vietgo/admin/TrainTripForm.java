package com.vietgo.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/** Form them / sua chuyen tau (quan tri). */
public class TrainTripForm {

    private Long id;

    @NotBlank(message = "Vui lòng nhập mác tàu")
    @Pattern(regexp = "^[A-Z]{1,4}[0-9]{1,4}$", message = "Mác tàu gồm chữ in hoa và số, VD: SE1, SNT2")
    private String trainCode;

    @NotBlank(message = "Vui lòng chọn ga đi")
    private String originStation;

    @NotBlank(message = "Vui lòng chọn ga đến")
    private String destinationStation;

    @NotNull(message = "Vui lòng nhập giờ khởi hành")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime departureTime;

    @NotNull(message = "Vui lòng nhập giờ đến")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime arrivalTime;

    @NotNull(message = "Vui lòng nhập cự ly")
    @Min(value = 10, message = "Cự ly tối thiểu 10 km")
    @Max(value = 2000, message = "Cự ly tối đa 2.000 km")
    private Integer distanceKm;

    @NotNull(message = "Nhập số toa (0 nếu không có)")
    @Min(value = 0, message = "Số toa không được âm")
    @Max(value = 8, message = "Tối đa 8 toa mỗi loại")
    private Integer softSeatCoaches = 2;

    @NotNull(message = "Nhập số toa (0 nếu không có)")
    @Min(value = 0, message = "Số toa không được âm")
    @Max(value = 8, message = "Tối đa 8 toa mỗi loại")
    private Integer sleeper6Coaches = 1;

    @NotNull(message = "Nhập số toa (0 nếu không có)")
    @Min(value = 0, message = "Số toa không được âm")
    @Max(value = 8, message = "Tối đa 8 toa mỗi loại")
    private Integer sleeper4Coaches = 1;

    private boolean active = true;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTrainCode() {
        return trainCode;
    }

    public void setTrainCode(String trainCode) {
        this.trainCode = trainCode;
    }

    public String getOriginStation() {
        return originStation;
    }

    public void setOriginStation(String originStation) {
        this.originStation = originStation;
    }

    public String getDestinationStation() {
        return destinationStation;
    }

    public void setDestinationStation(String destinationStation) {
        this.destinationStation = destinationStation;
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

    public Integer getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Integer distanceKm) {
        this.distanceKm = distanceKm;
    }

    public Integer getSoftSeatCoaches() {
        return softSeatCoaches;
    }

    public void setSoftSeatCoaches(Integer softSeatCoaches) {
        this.softSeatCoaches = softSeatCoaches;
    }

    public Integer getSleeper6Coaches() {
        return sleeper6Coaches;
    }

    public void setSleeper6Coaches(Integer sleeper6Coaches) {
        this.sleeper6Coaches = sleeper6Coaches;
    }

    public Integer getSleeper4Coaches() {
        return sleeper4Coaches;
    }

    public void setSleeper4Coaches(Integer sleeper4Coaches) {
        this.sleeper4Coaches = sleeper4Coaches;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
