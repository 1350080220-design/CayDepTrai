package com.vietgo.train;

import com.vietgo.common.BusinessException;
import com.vietgo.domain.TrainPassengerType;
import com.vietgo.domain.TrainTrip;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainBookingValidatorTest {

    private final TrainBookingValidator validator = new TrainBookingValidator();
    private final LocalDate travel = LocalDate.of(2027, 3, 9);
    private final TrainTrip trip = TrainFareCalculatorTest.trip(791, travel.atTime(20, 0));

    private TrainPassengerItem pax(String seat, TrainPassengerType type, LocalDate birth, String id, String card) {
        TrainPassengerItem p = new TrainPassengerItem();
        p.setSeatKey(seat);
        p.setFullName("Nguyễn Văn An");
        p.setType(type);
        p.setBirthDate(birth);
        p.setIdNumber(id);
        p.setStudentCardNo(card);
        return p;
    }

    private BeanPropertyBindingResult validate(TrainPassengerItem... items) {
        TrainPassengerForm f = new TrainPassengerForm();
        f.setContactName("Nguyễn Văn An");
        f.setContactPhone("0912345678");
        f.setContactEmail("a@b.vn");
        f.setPassengers(List.of(items));
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(f, "form");
        validator.validatePassengers(trip, f, errors);
        return errors;
    }

    @Test
    void validAdultAndChildPass() {
        var e = validate(
                pax("1-1", TrainPassengerType.ADULT, travel.minusYears(30), "079203001234", null),
                pax("1-2", TrainPassengerType.CHILD, travel.minusYears(7), null, null));
        assertFalse(e.hasErrors());
    }

    @Test
    void adultNeedsIdAndStudentNeedsCard() {
        var e = validate(
                pax("1-1", TrainPassengerType.ADULT, travel.minusYears(30), null, null),
                pax("1-2", TrainPassengerType.STUDENT, travel.minusYears(20), "079203009999", null));
        assertTrue(e.hasFieldErrors("passengers[0].idNumber"));
        assertTrue(e.hasFieldErrors("passengers[1].studentCardNo"));
    }

    @Test
    void ageMustMatchPassengerType() {
        var e = validate(
                pax("1-1", TrainPassengerType.ELDERLY, travel.minusYears(45), "079203001234", null),
                pax("1-2", TrainPassengerType.ADULT, travel.minusYears(4), null, null));
        assertTrue(e.hasFieldErrors("passengers[0].type"));
        assertTrue(e.hasFieldErrors("passengers[1].birthDate")); // duoi 6 tuoi mien ve
    }

    @Test
    void childrenCannotTravelAlone() {
        var e = validate(pax("1-1", TrainPassengerType.CHILD, travel.minusYears(8), null, null));
        assertTrue(e.hasGlobalErrors());
    }

    @Test
    void duplicateIdIsRejected() {
        var e = validate(
                pax("1-1", TrainPassengerType.ADULT, travel.minusYears(30), "079203001234", null),
                pax("1-2", TrainPassengerType.ADULT, travel.minusYears(31), "079203001234", null));
        assertTrue(e.hasFieldErrors("passengers[1].idNumber"));
    }

    @Test
    void seatSelectionRules() {
        assertThrows(BusinessException.class, () -> validator.checkSeatSelection(trip, List.of(), Set.of()));
        assertThrows(BusinessException.class, () -> validator.checkSeatSelection(trip, List.of("1-1"), Set.of("1-1")));
        assertThrows(BusinessException.class, () -> validator.checkSeatSelection(trip, List.of("1-99"), Set.of()));
        assertThrows(BusinessException.class, () -> validator.checkSeatSelection(trip,
                List.of("1-1", "1-2", "1-3", "1-4", "1-5", "1-6", "1-7"), Set.of()));
        validator.checkSeatSelection(trip, List.of("1-1", "2-6"), Set.of("1-3"));
    }

    @Test
    void salesCloseOneHourBeforeDeparture() {
        LocalDateTime dep = trip.getDepartureTime();
        validator.checkTripOpen(trip, dep.minusMinutes(61));
        assertThrows(BusinessException.class, () -> validator.checkTripOpen(trip, dep.minusMinutes(59)));
    }
}
