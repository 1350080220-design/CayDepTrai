package com.vietgo.common;

import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Ham dinh dang dung trong Thymeleaf: ${@fmt.money(x)}, ${@fmt.dateTime(t)}...
 */
@Component("fmt")
public class Fmt {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("dd/MM");

    private static DecimalFormat moneyFormat() {
        DecimalFormatSymbols s = new DecimalFormatSymbols(Locale.US);
        s.setGroupingSeparator('.');
        return new DecimalFormat("#,##0", s);
    }

    /** 1250000 -> "1.250.000đ" */
    public String money(Long amount) {
        if (amount == null) return "0đ";
        String sign = amount < 0 ? "−" : "";
        return sign + moneyFormat().format(Math.abs(amount)) + "đ";
    }

    /** 1250000 -> "1.250.000" (khong kem don vi). */
    public String number(Long amount) {
        if (amount == null) return "0";
        return moneyFormat().format(amount);
    }

    public String date(LocalDate d) {
        return d == null ? "" : d.format(DATE);
    }

    public String date(LocalDateTime d) {
        return d == null ? "" : d.format(DATE);
    }

    public String time(LocalDateTime d) {
        return d == null ? "" : d.format(TIME);
    }

    public String dateTime(LocalDateTime d) {
        return d == null ? "" : d.format(DATE_TIME);
    }

    public String shortDate(LocalDateTime d) {
        return d == null ? "" : d.format(SHORT_DATE);
    }

    public String isoDate(LocalDate d) {
        return d == null ? "" : d.toString();
    }

    public String weekday(LocalDateTime d) {
        return d == null ? "" : weekday(d.toLocalDate());
    }

    public String weekday(LocalDate d) {
        if (d == null) return "";
        DayOfWeek w = d.getDayOfWeek();
        return switch (w) {
            case MONDAY -> "Thứ Hai";
            case TUESDAY -> "Thứ Ba";
            case WEDNESDAY -> "Thứ Tư";
            case THURSDAY -> "Thứ Năm";
            case FRIDAY -> "Thứ Sáu";
            case SATURDAY -> "Thứ Bảy";
            case SUNDAY -> "Chủ nhật";
        };
    }

    /** Che bot so giay to: 079203001234 -> 0792****1234 */
    public String maskId(String id) {
        if (id == null || id.length() < 6) return id == null ? "" : id;
        return id.substring(0, 4) + "*".repeat(id.length() - 8 > 0 ? id.length() - 8 : 2) + id.substring(id.length() - 4);
    }
}
