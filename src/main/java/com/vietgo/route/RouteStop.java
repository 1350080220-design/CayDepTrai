package com.vietgo.route;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/** Mot diem tren lo trinh: ga dung, tram nghi, thi tran di qua, diem dau / diem cuoi. */
public class RouteStop {

    private final String name;
    private final StopKind kind;
    private final double lat;
    private final double lng;
    private final int km;
    /** Vi tri cua diem trong danh sach toa do lo trinh (-1 neu khong co toa do). */
    private final int pathIndex;
    private int dwellMinutes;
    private LocalDateTime arrival;
    private LocalDateTime departure;
    private String note;
    private List<String> details = List.of();

    public RouteStop(String name, StopKind kind, double lat, double lng, int km, int pathIndex, int dwellMinutes) {
        this.name = name;
        this.kind = kind;
        this.lat = lat;
        this.lng = lng;
        this.km = km;
        this.pathIndex = pathIndex;
        this.dwellMinutes = dwellMinutes;
    }

    public String getName() { return name; }
    public StopKind getKind() { return kind; }
    public double getLat() { return lat; }
    public double getLng() { return lng; }
    public int getKm() { return km; }
    public int getPathIndex() { return pathIndex; }
    public int getDwellMinutes() { return dwellMinutes; }
    public LocalDateTime getArrival() { return arrival; }
    public LocalDateTime getDeparture() { return departure; }
    public String getNote() { return note; }
    public List<String> getDetails() { return details; }

    void setDwellMinutes(int dwellMinutes) { this.dwellMinutes = dwellMinutes; }
    void setArrival(LocalDateTime arrival) { this.arrival = arrival; }
    void setDeparture(LocalDateTime departure) { this.departure = departure; }
    void setNote(String note) { this.note = note; }
    void setDetails(List<String> details) { this.details = details == null ? List.of() : details; }

    /** Gio hien thi: gio di voi diem dau, gio den voi cac diem con lai. */
    public LocalDateTime getTime() {
        return arrival != null ? arrival : departure;
    }

    /** Dung tu 10 phut tro len: hanh khach co the xuong san ga / tram nghi. */
    public boolean isLongStop() {
        return dwellMinutes >= 10;
    }

    public boolean isEndpoint() {
        return kind == StopKind.ORIGIN || kind == StopKind.DESTINATION;
    }

    /** So ngay chenh lech so voi ngay khoi hanh (de hien "+1 ngày"). */
    public long dayOffset(LocalDateTime start) {
        LocalDateTime t = getTime();
        if (t == null || start == null) return 0;
        return Duration.between(start.toLocalDate().atStartOfDay(), t.toLocalDate().atStartOfDay()).toDays();
    }
}
