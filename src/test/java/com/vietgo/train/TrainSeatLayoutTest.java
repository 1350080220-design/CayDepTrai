package com.vietgo.train;

import com.vietgo.domain.TrainCoachType;
import com.vietgo.domain.TrainTrip;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainSeatLayoutTest {

    @Test
    void berthTiersAndCompartments() {
        int[] expected = {1, 1, 2, 2, 3, 3};
        for (int i = 1; i <= 6; i++) {
            assertEquals(expected[i - 1], TrainSeatLayout.tierOf(TrainCoachType.SLEEPER_6, i));
        }
        assertEquals(2, TrainSeatLayout.compartmentOf(TrainCoachType.SLEEPER_6, 7));
        assertEquals(1, TrainSeatLayout.tierOf(TrainCoachType.SLEEPER_4, 5));
        assertEquals(0, TrainSeatLayout.tierOf(TrainCoachType.SOFT_SEAT, 5));
    }

    @Test
    void seatExistence() {
        TrainTrip t = TrainFareCalculatorTest.trip(300, LocalDateTime.of(2027, 3, 9, 8, 0));
        assertTrue(TrainSeatLayout.exists(t, "1-64"));
        assertFalse(TrainSeatLayout.exists(t, "1-65"));
        assertTrue(TrainSeatLayout.exists(t, "3-28"));
        assertFalse(TrainSeatLayout.exists(t, "3-29"));
        assertFalse(TrainSeatLayout.exists(t, "9-1"));
        assertFalse(TrainSeatLayout.exists(t, "abc"));
    }
}
