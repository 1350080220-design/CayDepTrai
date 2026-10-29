package com.vietgo.bus;

import com.vietgo.domain.BusPassengerType;
import com.vietgo.domain.BusTrip;
import com.vietgo.domain.BusType;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BusBookingValidatorTest {

    private final BusBookingValidator validator = new BusBookingValidator();
    private final BusTrip trip = BusFareCalculatorTest.trip(BusType.SLEEPER_36, 290_000, LocalDateTime.of(2027, 3, 9, 22, 0));

    private BusPassengerForm form() {
        BusPassengerForm f = new BusPassengerForm();
        f.setContactName("Trần Thu Hà");
        f.setContactPhone("0987654321");
        f.setContactEmail("ha@vietgo.vn");
        f.setPickupPoint("Bến xe Miền Đông mới");
        f.setDropoffPoint("Chợ Đà Lạt");
        f.setLuggageKg(20);
        f.setPassengers(new ArrayList<>());
        BusPassengerItem a = new BusPassengerItem();
        a.setSeatCode("A01");
        a.setFullName("Trần Thu Hà");
        a.setType(BusPassengerType.ADULT);
        f.getPassengers().add(a);
        return f;
    }

    private BeanPropertyBindingResult run(BusPassengerForm f) {
        BeanPropertyBindingResult e = new BeanPropertyBindingResult(f, "form");
        validator.validateDetails(trip, f, e);
        return e;
    }

    @Test
    void validFormPasses() {
        assertFalse(run(form()).hasErrors());
    }

    @Test
    void doorPickupNeedsAddress() {
        BusPassengerForm f = form();
        f.setPickupPoint(BusFareCalculator.DOOR_PICKUP);
        f.setPickupAddress("Q1");
        assertTrue(run(f).hasFieldErrors("pickupAddress"));
    }

    @Test
    void pointsMustBelongToRoute() {
        BusPassengerForm f = form();
        f.setDropoffPoint("Hồ Gươm");
        assertTrue(run(f).hasFieldErrors("dropoffPoint"));
    }

    @Test
    void luggageLimitAndChildAge() {
        BusPassengerForm f = form();
        f.setLuggageKg(90);
        BusPassengerItem c = new BusPassengerItem();
        c.setSeatCode("A02");
        c.setFullName("Bé Na");
        c.setType(BusPassengerType.CHILD);
        c.setBirthYear(2025); // 2 tuoi -> khong can mua ve
        f.getPassengers().add(c);
        var e = run(f);
        assertTrue(e.hasFieldErrors("luggageKg"));
        assertTrue(e.hasFieldErrors("passengers[1].birthYear"));
    }

    @Test
    void invalidPassengerPhone() {
        BusPassengerForm f = form();
        f.getPassengers().get(0).setPhone("12345");
        assertTrue(run(f).hasFieldErrors("passengers[0].phone"));
    }
}
