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
}
