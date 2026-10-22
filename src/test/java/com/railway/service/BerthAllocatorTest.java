package com.railway.service;

import com.railway.model.Reservation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BerthAllocatorTest {

    @Test
    public void testSeniorCitizenReceivesLowerBerth() {
        Reservation senior = new Reservation("Grandpa", 68, "Male", "No Preference");
        String berth = BerthAllocator.allocateOptimalBerth(senior);
        assertEquals(BerthAllocator.LOWER, berth, "Senior citizen must receive LOWER berth by default");
    }

    @Test
    public void testSpecificPreferencePreserved() {
        Reservation pax = new Reservation("Young Traveler", 24, "Male", "SIDE LOWER");
        String berth = BerthAllocator.allocateOptimalBerth(pax);
        assertEquals("SIDE_LOWER", berth, "Explicit requested berth preference should be normalized and preserved");
    }

    @Test
    public void testFemalePassengerPriority() {
        Reservation femalePax = new Reservation("Solo Traveler", 32, "Female", "No Preference");
        String berth = BerthAllocator.allocateOptimalBerth(femalePax);
        assertEquals(BerthAllocator.LOWER, berth, "Female passenger without preference should receive LOWER berth");
    }

    @Test
    public void testRandomAllocationFallbackWhenRequestedBerthUnavailable() {
        // In standard coach mod 8 in {1, 4} are LOWER. Let's occupy seats 1 and 4 for 8 total seats
        java.util.Set<Integer> occupied = new java.util.HashSet<>(java.util.Arrays.asList(1, 4));
        Reservation pax = new Reservation("John Doe", 30, "Male", "LOWER");
        java.util.List<Reservation> list = new java.util.ArrayList<>(java.util.Collections.singletonList(pax));

        BerthAllocator.allocateSeatsRealtime(8, occupied, list);

        assertTrue(pax.isRandomlyAllocated(), "Should be marked as randomly allocated");
        assertNotEquals(1, pax.getSeatNumber());
        assertNotEquals(4, pax.getSeatNumber());
        assertTrue(pax.getSeatNumber() > 0 && pax.getSeatNumber() <= 8);
        assertTrue(pax.getAllocationNote().contains("Requested LOWER unavailable. Randomly assigned Seat #"));
    }
}
