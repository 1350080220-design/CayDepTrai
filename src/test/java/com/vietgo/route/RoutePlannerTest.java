package com.vietgo.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vietgo.bus.BusService;
import com.vietgo.domain.BusTrip;
import com.vietgo.domain.BusType;
import com.vietgo.domain.TrainCoachType;
import com.vietgo.domain.TrainTrip;
import com.vietgo.train.TrainService;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Kiem thu lap lo trinh: ga dung, tram nghi, gio den du kien. */
class RoutePlannerTest {

    private final RoutePlanner planner = new RoutePlanner(new ObjectMapper());

    static TrainTrip train(String from, String to, LocalDateTime dep, long minutes) {
        TrainTrip t = new TrainTrip();
        t.setId(1L);
        t.setTrainCode("SE1");
        t.setOriginStation(from);
        t.setDestinationStation(to);
        t.setDepartureTime(dep);
        t.setArrivalTime(dep.plusMinutes(minutes));
        t.setDistanceKm(Math.max(1, Railway.distanceKm(from, to)));
        t.addCoach(1, TrainCoachType.SOFT_SEAT);
        return t;
    }

    static BusTrip bus(String from, String to, LocalDateTime dep, long minutes) {
        BusTrip b = new BusTrip();
        b.setId(1L);
        b.setOperatorName("Nhà xe");
        b.setLicensePlate("51B-000.00");
        b.setOriginCity(from);
        b.setDestinationCity(to);
        b.setDepartureTime(dep);
        b.setArrivalTime(dep.plusMinutes(minutes));
        b.setBusType(BusType.SLEEPER_36);
        b.setBasePrice(300_000);
        b.setPickupPoints("Bến A;Bến B");
        b.setDropoffPoints("Bến C");
        return b;
    }

    private static void assertChronological(RoutePlan plan) {
        LocalDateTime prev = null;
        for (RouteStop s : plan.getStops()) {
            LocalDateTime t = s.getTime();
            if (prev != null) assertFalse(t.isBefore(prev), "Gio cua " + s.getName() + " truoc diem truoc do");
            prev = s.getDeparture() != null ? s.getDeparture() : t;
        }
    }

    @Test
    void everySearchableStationAndCityIsOnTheNetwork() {
        for (String s : TrainService.STATIONS) assertTrue(Railway.contains(s), s);
        for (String a : BusService.CITIES) {
            for (String b : BusService.CITIES) {
                if (!a.equals(b)) assertFalse(RoadNetwork.shortestPath(a, b).isEmpty(), a + " -> " + b);
            }
        }
    }

    @Test
    void trainStopsAtEveryStationInOrderAndKeepsTimetable() {
        LocalDateTime dep = LocalDateTime.of(2027, 3, 9, 20, 5);
        RoutePlan plan = planner.forTrain(train("Hà Nội", "Đà Nẵng", dep, 950));
        List<RouteStop> stops = plan.getStops();

        assertEquals("Hà Nội", stops.get(0).getName());
        assertEquals(StopKind.ORIGIN, stops.get(0).getKind());
        assertEquals("Đà Nẵng", stops.get(stops.size() - 1).getName());
        assertEquals(dep, stops.get(0).getDeparture());
        assertEquals(dep.plusMinutes(950), stops.get(stops.size() - 1).getArrival());
        assertEquals(791, plan.getTotalKm());
        assertTrue(stops.stream().anyMatch(s -> s.getName().equals("Vinh") && s.isLongStop()));
        assertTrue(plan.getTotalDwellMinutes() <= 950 * RoutePlanner.MAX_DWELL_SHARE);
        assertTrue(plan.isMapped());
        assertChronological(plan);
    }

    @Test
    void reverseDirectionTrainListsStationsSouthToNorth() {
        RoutePlan plan = planner.forTrain(train("Sài Gòn", "Nha Trang", LocalDateTime.of(2027, 3, 9, 20, 30), 480));
        List<String> names = plan.getStops().stream().map(RouteStop::getName).toList();
        assertEquals(List.of("Sài Gòn", "Biên Hòa", "Phan Thiết", "Tháp Chàm", "Nha Trang"), names);
        assertChronological(plan);
    }

    @Test
    void longBusTripGetsRestStopsWithMealBreak() {
        LocalDateTime dep = LocalDateTime.of(2027, 3, 9, 7, 0);
        RoutePlan plan = planner.forBus(bus("TP. Hồ Chí Minh", "Đà Lạt", dep, 420));
        assertEquals(2, plan.getRestCount());
        assertEquals(dep.plusMinutes(420), plan.getStops().get(plan.getStops().size() - 1).getArrival());
        assertEquals(List.of("Bến A", "Bến B"), plan.getStops().get(0).getDetails());
        assertEquals(List.of("Bến C"), plan.getStops().get(plan.getStops().size() - 1).getDetails());
        // Tram nghi thu hai roi vao gio an trua -> nghi 30 phut
        assertTrue(plan.getStops().stream().anyMatch(s -> s.getKind() == StopKind.REST
                && s.getDwellMinutes() == RoutePlanner.BUS_MEAL_MINUTES));
        assertChronological(plan);
    }

    @Test
    void shortBusTripHasNoRestStop() {
        RoutePlan plan = planner.forBus(bus("Đà Nẵng", "Hội An", LocalDateTime.of(2027, 3, 9, 8, 0), 50));
        assertEquals(0, plan.getRestCount());
        assertEquals(2, plan.getStops().size());
    }

    @Test
    void unknownPlacesFallBackToOriginAndDestinationOnly() {
        RoutePlan plan = planner.forBus(bus("Mộc Châu", "Hà Giang", LocalDateTime.of(2027, 3, 9, 8, 0), 300));
        assertFalse(plan.isMapped());
        assertEquals(2, plan.getStops().size());
        assertEquals(Duration.ofMinutes(300),
                Duration.between(plan.getStops().get(0).getDeparture(), plan.getStops().get(1).getArrival()));
    }

    @Test
    void jsonCarriesPathAndStopMinutes() {
        RoutePlan plan = planner.forTrain(train("Huế", "Đà Nẵng", LocalDateTime.of(2027, 3, 9, 8, 0), 150));
        String json = planner.toJson(plan);
        assertTrue(json.contains("\"stops\""));
        assertTrue(json.contains("\"path\""));
        assertTrue(json.contains("\"total\":150"));
    }
}
