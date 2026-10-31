package com.vietgo.bus;

import com.vietgo.domain.BusType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BusSeatLayoutTest {

    @Test
    void everyBusTypeLayoutMatchesCapacity() {
        for (BusType type : BusType.values()) {
            assertEquals(type.getCapacity(), BusSeatLayout.allSeats(type).size(), type.name());
        }
    }

    @Test
    void cabinBusHasTwoDecksWithLowerDeckSurcharge() {
        assertTrue(BusSeatLayout.exists(BusType.CABIN_24, "A12"));
        assertTrue(BusSeatLayout.exists(BusType.CABIN_24, "B01"));
        assertFalse(BusSeatLayout.exists(BusType.CABIN_24, "A13"));
        assertEquals(1.10, BusSeatLayout.positionFactor(BusType.CABIN_24, "A03"));
        assertEquals(1.0, BusSeatLayout.positionFactor(BusType.CABIN_24, "B03"));
        assertEquals("Phòng A01 (tầng dưới)", BusSeatLayout.label(BusType.CABIN_24, "A01"));
        assertEquals(2, BusSeatLayout.tierOf(BusType.CABIN_24, "B07"));
        assertEquals(0, BusSeatLayout.tierOf(BusType.SEAT_45, "A07"));
    }
}
