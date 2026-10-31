package com.vietgo.route;

import java.time.LocalDateTime;
import java.util.List;

/** Lo trinh mot chuyen: cac diem dung theo thu tu va toa do duong di de ve ban do. */
public class RoutePlan {

    private final String mode;
    private final List<RouteStop> stops;
    /** Toa do [vi do, kinh do] theo thu tu duong di; rong neu khong ve duoc ban do. */
    private final List<double[]> path;
    private final int totalKm;
    private final LocalDateTime departure;
    private final LocalDateTime arrival;

    public RoutePlan(String mode, List<RouteStop> stops, List<double[]> path, int totalKm,
                     LocalDateTime departure, LocalDateTime arrival) {
        this.mode = mode;
        this.stops = stops;
        this.path = path;
        this.totalKm = totalKm;
        this.departure = departure;
        this.arrival = arrival;
    }

    public String getMode() { return mode; }
    public List<RouteStop> getStops() { return stops; }
    public List<double[]> getPath() { return path; }
    public int getTotalKm() { return totalKm; }
    public LocalDateTime getDeparture() { return departure; }
    public LocalDateTime getArrival() { return arrival; }

    public boolean isMapped() {
        return path.size() >= 2;
    }

    public long getStationCount() {
        return count(StopKind.STATION);
    }

    public long getRestCount() {
        return count(StopKind.REST);
    }

    public long getPassCount() {
        return count(StopKind.PASS);
    }

    /** Tong thoi gian dung do / nghi ngoi doc duong (phut). */
    public int getTotalDwellMinutes() {
        return stops.stream().filter(s -> !s.isEndpoint()).mapToInt(RouteStop::getDwellMinutes).sum();
    }

    /** Cac diem co dung lai (bo qua thi tran chi di qua) - dung cho ban xem nhanh o trang ket qua. */
    public List<RouteStop> getStoppingPoints() {
        return stops.stream().filter(s -> s.getKind() != StopKind.PASS).toList();
    }

    public long dayOffset(RouteStop stop) {
        return stop.dayOffset(departure);
    }

    private long count(StopKind kind) {
        return stops.stream().filter(s -> s.getKind() == kind).count();
    }
}
