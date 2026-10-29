package com.vietgo.common;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Map;

/**
 * Lich cao diem le, Tet tai Viet Nam dung cho chinh sach phu phi.
 * - Tet Nguyen Dan: tu 10 ngay truoc den 7 ngay sau mung 1 Tet (am lich, tra bang).
 * - 30/4 - 1/5: tu 28/4 den 3/5.
 * - Quoc khanh 2/9: tu 31/8 den 3/9.
 * - Tet Duong lich: 31/12 va 1/1.
 */
public final class VnHolidayCalendar {

    private VnHolidayCalendar() {
    }

    /** Ngay mung 1 Tet Nguyen Dan (duong lich). */
    private static final Map<Integer, LocalDate> LUNAR_NEW_YEAR = Map.of(
            2025, LocalDate.of(2025, 1, 29),
            2026, LocalDate.of(2026, 2, 17),
            2027, LocalDate.of(2027, 2, 6),
            2028, LocalDate.of(2028, 1, 26),
            2029, LocalDate.of(2029, 2, 13),
            2030, LocalDate.of(2030, 2, 3));

    /** Tra ve ten dip le neu ngay thuoc cao diem le Tet, nguoc lai null. */
    public static String holidayName(LocalDate date) {
        if (date == null) return null;
        for (LocalDate tet : LUNAR_NEW_YEAR.values()) {
            if (!date.isBefore(tet.minusDays(10)) && !date.isAfter(tet.plusDays(7))) {
                return "Tết Nguyên Đán";
            }
        }
        MonthDay md = MonthDay.from(date);
        if (between(md, MonthDay.of(4, 28), MonthDay.of(5, 3))) return "Lễ 30/4 – 1/5";
        if (between(md, MonthDay.of(8, 31), MonthDay.of(9, 3))) return "Lễ Quốc khánh 2/9";
        if (md.equals(MonthDay.of(12, 31)) || md.equals(MonthDay.of(1, 1))) return "Tết Dương lịch";
        return null;
    }

    public static boolean isHoliday(LocalDate date) {
        return holidayName(date) != null;
    }

    /** Cuoi tuan theo chinh sach van tai: thu Sau, thu Bay, Chu nhat. */
    public static boolean isWeekend(LocalDate date) {
        DayOfWeek d = date.getDayOfWeek();
        return d == DayOfWeek.FRIDAY || d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY;
    }

    private static boolean between(MonthDay md, MonthDay from, MonthDay to) {
        return !md.isBefore(from) && !md.isAfter(to);
    }
}
