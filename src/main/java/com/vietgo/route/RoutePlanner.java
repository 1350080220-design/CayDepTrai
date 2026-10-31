package com.vietgo.route;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vietgo.domain.Booking;
import com.vietgo.domain.BusTrip;
import com.vietgo.domain.TrainTrip;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * LAP LO TRINH CHUYEN DI (ga dung, tram nghi, gio den du kien) DE HIEN THI BAN DO.
 *
 * 1) Tau hoa: lay doan tuyen duong sat giua ga di va ga den; tau dung o moi ga tren doan tuyen
 *    voi thoi gian dung rieng tung ga (ga lon 10-15 phut, ga nho 3-5 phut).
 * 2) Xe khach: tim duong ngan nhat tren mang duong bo; hanh trinh tu 150 phut tro len co tram nghi,
 *    cu khoang 210 phut lai nghi mot lan, dat tai thi tran gan moc chia deu cu ly nhat.
 *    Nghi 20 phut, trung gio an trua (10:30-13:30) hoac an toi (17:30-20:30) thi nghi 30 phut.
 * 3) Gio den: thoi gian chay = tong thoi gian - tong thoi gian dung, chia theo ty le cu ly;
 *    tong thoi gian dung toi da 25% hanh trinh. Gio xuat phat va gio den cuoi luon khop lich chuyen.
 */
@Component
public class RoutePlanner {

    static final double MAX_DWELL_SHARE = 0.25;
    static final int BUS_MIN_MINUTES_FOR_REST = 150;
    static final int BUS_REST_EVERY_MINUTES = 210;
    static final int BUS_REST_MINUTES = 20;
    static final int BUS_MEAL_MINUTES = 30;

    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    private final ObjectMapper mapper;

    public RoutePlanner(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public RoutePlan forBooking(Booking b) {
        if (b.isTrain() && b.getTrainTrip() != null) return forTrain(b.getTrainTrip());
        if (b.isBus() && b.getBusTrip() != null) return forBus(b.getBusTrip());
        return null;
    }

    // ------------------------------------------------------------------ tau hoa

    public RoutePlan forTrain(TrainTrip trip) {
        LocalDateTime dep = trip.getDepartureTime();
        LocalDateTime arr = trip.getArrivalTime();
        List<Railway.Station> seg = Railway.segment(trip.getOriginStation(), trip.getDestinationStation());
        if (seg.isEmpty()) {
            return direct("train", trip.getOriginStation(), trip.getDestinationStation(), dep, arr,
                    trip.getDistanceKm());
        }
        int startKm = seg.get(0).km();
        List<RouteStop> stops = new ArrayList<>();
        List<double[]> path = new ArrayList<>();
        for (int i = 0; i < seg.size(); i++) {
            Railway.Station s = seg.get(i);
            path.add(new double[]{s.lat(), s.lng()});
            if (!s.stop()) continue;
            StopKind kind = i == 0 ? StopKind.ORIGIN : i == seg.size() - 1 ? StopKind.DESTINATION : StopKind.STATION;
            stops.add(new RouteStop(s.name(), kind, s.lat(), s.lng(), Math.abs(s.km() - startKm), i,
                    kind == StopKind.STATION ? s.dwellMinutes() : 0));
        }
        schedule(stops, dep, arr);
        for (RouteStop s : stops) {
            if (s.getKind() == StopKind.STATION && s.isLongStop()) {
                s.setNote("Dừng " + s.getDwellMinutes() + " phút, có thể xuống sân ga");
            }
        }
        int totalKm = stops.get(stops.size() - 1).getKm();
        return new RoutePlan("train", stops, path, totalKm, dep, arr);
    }

    // ------------------------------------------------------------------ xe khach

    public RoutePlan forBus(BusTrip trip) {
        LocalDateTime dep = trip.getDepartureTime();
        LocalDateTime arr = trip.getArrivalTime();
        List<RoadNetwork.Node> nodes = RoadNetwork.shortestPath(trip.getOriginCity(), trip.getDestinationCity());
        RoutePlan plan;
        if (nodes.isEmpty()) {
            plan = direct("bus", trip.getOriginCity(), trip.getDestinationCity(), dep, arr, 0);
        } else {
            int[] cum = new int[nodes.size()];
            for (int i = 1; i < nodes.size(); i++) {
                cum[i] = cum[i - 1] + RoadNetwork.edgeKm(nodes.get(i - 1), nodes.get(i));
            }
            long minutes = Duration.between(dep, arr).toMinutes();
            int restCount = minutes < BUS_MIN_MINUTES_FOR_REST ? 0
                    : (int) Math.max(1, minutes / BUS_REST_EVERY_MINUTES);
            Set<Integer> restAt = pickRestNodes(cum, restCount);

            List<RouteStop> stops = new ArrayList<>();
            List<double[]> path = new ArrayList<>();
            int last = nodes.size() - 1;
            for (int i = 0; i <= last; i++) {
                RoadNetwork.Node n = nodes.get(i);
                path.add(new double[]{n.lat(), n.lng()});
                if (i == 0) {
                    stops.add(new RouteStop(n.name(), StopKind.ORIGIN, n.lat(), n.lng(), 0, i, 0));
                } else if (i == last) {
                    stops.add(new RouteStop(n.name(), StopKind.DESTINATION, n.lat(), n.lng(), cum[i], i, 0));
                } else if (restAt.contains(i)) {
                    stops.add(new RouteStop("Trạm dừng nghỉ " + n.name(), StopKind.REST, n.lat(), n.lng(), cum[i], i,
                            BUS_REST_MINUTES));
                } else {
                    stops.add(new RouteStop(n.name(), StopKind.PASS, n.lat(), n.lng(), cum[i], i, 0));
                }
            }
            schedule(stops, dep, arr);
            // Trung gio an thi nghi lau hon, roi tinh lai gio
            boolean changed = false;
            for (RouteStop s : stops) {
                if (s.getKind() != StopKind.REST) continue;
                int want = isMealTime(s.getArrival()) ? BUS_MEAL_MINUTES : BUS_REST_MINUTES;
                if (want != s.getDwellMinutes()) {
                    s.setDwellMinutes(want);
                    changed = true;
                }
            }
            if (changed) schedule(stops, dep, arr);
            for (RouteStop s : stops) {
                if (s.getKind() == StopKind.REST) s.setNote(restNote(s.getArrival()) + " · " + s.getDwellMinutes() + " phút");
            }
            plan = new RoutePlan("bus", stops, path, cum[last], dep, arr);
        }
        plan.getStops().get(0).setDetails(trip.getPickupPointList());
        plan.getStops().get(plan.getStops().size() - 1).setDetails(trip.getDropoffPointList());
        return plan;
    }

    /** Chon cac nut trung gian gan nhat voi moc chia deu cu ly (k / (n + 1)). */
    static Set<Integer> pickRestNodes(int[] cum, int count) {
        Set<Integer> chosen = new HashSet<>();
        int last = cum.length - 1;
        if (count <= 0 || last < 2) return chosen;
        for (int k = 1; k <= count; k++) {
            double target = cum[last] * (double) k / (count + 1);
            int best = -1;
            double bestGap = Double.MAX_VALUE;
            for (int i = 1; i < last; i++) {
                if (chosen.contains(i)) continue;
                double gap = Math.abs(cum[i] - target);
                if (gap < bestGap) {
                    bestGap = gap;
                    best = i;
                }
            }
            if (best > 0) chosen.add(best);
        }
        return chosen;
    }

    private static boolean isMealTime(LocalDateTime t) {
        if (t == null) return false;
        int m = t.getHour() * 60 + t.getMinute();
        return (m >= 630 && m <= 810) || (m >= 1050 && m <= 1230);
    }

    private static String restNote(LocalDateTime t) {
        if (t == null) return "Nghỉ giải lao";
        int m = t.getHour() * 60 + t.getMinute();
        if (m >= 630 && m <= 810) return "Nghỉ ăn trưa";
        if (m >= 1050 && m <= 1230) return "Nghỉ ăn tối";
        if (m >= 1320 || m < 300) return "Nghỉ đêm, vệ sinh cá nhân";
        if (m < 510) return "Ăn sáng, vệ sinh cá nhân";
        return "Nghỉ giải lao, vệ sinh";
    }

    // ------------------------------------------------------------------ chung

    /** Lo trinh khong co toa do: chi co diem dau va diem cuoi. */
    private RoutePlan direct(String mode, String from, String to, LocalDateTime dep, LocalDateTime arr, int km) {
        List<RouteStop> stops = new ArrayList<>();
        stops.add(new RouteStop(from, StopKind.ORIGIN, 0, 0, 0, -1, 0));
        stops.add(new RouteStop(to, StopKind.DESTINATION, 0, 0, km, -1, 0));
        schedule(stops, dep, arr);
        return new RoutePlan(mode, stops, List.of(), km, dep, arr);
    }

    /** Gan gio den / gio di cho tung diem (xem muc 3 o dau lop). */
    static void schedule(List<RouteStop> stops, LocalDateTime dep, LocalDateTime arr) {
        int last = stops.size() - 1;
        long total = Math.max(0, Duration.between(dep, arr).toMinutes());
        int totalKm = Math.max(1, stops.get(last).getKm());

        long dwell = 0;
        for (int i = 1; i < last; i++) dwell += stops.get(i).getDwellMinutes();
        long cap = (long) (total * MAX_DWELL_SHARE);
        if (dwell > cap) {
            double f = (double) cap / dwell;
            dwell = 0;
            for (int i = 1; i < last; i++) {
                RouteStop s = stops.get(i);
                if (s.getDwellMinutes() > 0) s.setDwellMinutes((int) Math.max(1, Math.floor(s.getDwellMinutes() * f)));
                dwell += s.getDwellMinutes();
            }
            if (dwell > cap) {
                for (int i = 1; i < last; i++) stops.get(i).setDwellMinutes(0);
                dwell = 0;
            }
        }

        long run = total - dwell;
        long dwellBefore = 0;
        for (int i = 0; i <= last; i++) {
            RouteStop s = stops.get(i);
            if (i == 0) {
                s.setArrival(null);
                s.setDeparture(dep);
            } else if (i == last) {
                s.setArrival(arr);
                s.setDeparture(null);
            } else {
                long runMin = Math.round(run * (double) s.getKm() / totalKm);
                LocalDateTime a = dep.plusMinutes(runMin + dwellBefore);
                s.setArrival(a);
                s.setDeparture(a.plusMinutes(s.getDwellMinutes()));
                dwellBefore += s.getDwellMinutes();
            }
        }
    }

    /** Du lieu cho ban do (doc bang journey.js). Thoi gian tinh bang phut ke tu luc khoi hanh. */
    public String toJson(RoutePlan plan) {
        if (plan == null) return "{}";
        LocalDateTime dep = plan.getDeparture();
        long total = Duration.between(dep, plan.getArrival()).toMinutes();
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("mode", plan.getMode());
        root.put("dep", dep.withSecond(0).withNano(0).toString());
        root.put("total", total);
        root.put("km", plan.getTotalKm());
        List<double[]> path = new ArrayList<>();
        for (double[] p : plan.getPath()) path.add(new double[]{round4(p[0]), round4(p[1])});
        root.put("path", path);
        List<Map<String, Object>> stops = new ArrayList<>();
        for (RouteStop s : plan.getStops()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("n", s.getName());
            m.put("k", s.getKind().name());
            m.put("i", s.getPathIndex());
            m.put("a", s.getArrival() == null ? 0 : Duration.between(dep, s.getArrival()).toMinutes());
            m.put("d", s.getDeparture() == null ? total : Duration.between(dep, s.getDeparture()).toMinutes());
            m.put("w", s.getDwellMinutes());
            m.put("t", s.getTime() == null ? "" : s.getTime().format(HM));
            m.put("km", s.getKm());
            if (s.getNote() != null) m.put("note", s.getNote());
            stops.add(m);
        }
        root.put("stops", stops);
        try {
            return mapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private static double round4(double v) {
        return Math.round(v * 10_000) / 10_000.0;
    }
}
