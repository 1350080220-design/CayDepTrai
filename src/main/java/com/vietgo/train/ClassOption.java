package com.vietgo.train;

import com.vietgo.domain.TrainCoachType;

import java.util.List;

/** Tom tat mot hang toa tren chuyen tau: gia thap nhat con trong, so cho trong, cac toa thuoc hang. */
public class ClassOption {

    private final TrainCoachType type;
    private final long minPrice;
    private final long available;
    private final List<Integer> coachNos;

    public ClassOption(TrainCoachType type, long minPrice, long available, List<Integer> coachNos) {
        this.type = type;
        this.minPrice = minPrice;
        this.available = available;
        this.coachNos = coachNos;
    }

    public TrainCoachType getType() { return type; }
    public long getMinPrice() { return minPrice; }
    public long getAvailable() { return available; }
    public List<Integer> getCoachNos() { return coachNos; }

    public boolean isSoldOut() { return available == 0; }

    /** "HARD_SEAT:491000:80" - du lieu cho bo loc / sap xep phia trinh duyet. */
    public String getToken() {
        return type.name() + ":" + minPrice + ":" + available;
    }

    public String getCoachList() {
        return coachNos.stream().map(String::valueOf).reduce((a, b) -> a + ", " + b).orElse("");
    }
}
