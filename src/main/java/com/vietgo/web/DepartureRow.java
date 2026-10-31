package com.vietgo.web;

import com.vietgo.domain.BusTrip;
import com.vietgo.domain.TrainTrip;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/** Mot dong tren bang gio khoi hanh o trang chu. */
public class DepartureRow {

    private final String kind;
    private final String code;
    private final String origin;
    private final String destination;
    private final LocalDateTime departure;
    private final String classes;
    private final String url;

    private DepartureRow(String kind, String code, String origin, String destination, LocalDateTime departure,
                         String classes, String url) {
        this.kind = kind;
        this.code = code;
        this.origin = origin;
        this.destination = destination;
        this.departure = departure;
        this.classes = classes;
        this.url = url;
    }

    public static DepartureRow of(TrainTrip t) {
        String classes = t.getCoachTypes().stream().map(c -> c.getServiceClass().getLabel()).distinct()
                .collect(Collectors.joining(", "));
        return new DepartureRow("train", t.getTrainCode(), t.getOriginStation(), t.getDestinationStation(),
                t.getDepartureTime(), classes, "/tau-hoa/chuyen/" + t.getId());
    }

    public static DepartureRow of(BusTrip b) {
        return new DepartureRow("bus", b.getOperatorName(), b.getOriginCity(), b.getDestinationCity(),
                b.getDepartureTime(), b.getBusType().getLabel(), "/xe-khach/chuyen/" + b.getId());
    }

    public String getKind() { return kind; }
    public String getCode() { return code; }
    public String getOrigin() { return origin; }
    public String getDestination() { return destination; }
    public LocalDateTime getDeparture() { return departure; }
    public String getClasses() { return classes; }
    public String getUrl() { return url; }

    public boolean isTrain() {
        return "train".equals(kind);
    }

    /** Thoi diem khoi hanh dang ISO de JavaScript dem nguoc. */
    public String getDepartureIso() {
        return departure.withSecond(0).withNano(0).toString();
    }
}
