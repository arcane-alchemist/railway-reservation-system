package com.railway.service;

import com.railway.model.Reservation;
import com.railway.model.Train;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FareServiceTest {

    private FareService fareService;
    private Train sampleTrain;

    @BeforeEach
    public void setUp() {
        fareService = new FareService();
        sampleTrain = new Train(1, "12952", "Rajdhani Express", 1, 2, "16:55", "08:35", 60, 60, 1000.0, "RAJDHANI");
    }

    @Test
    public void testStandardFareCalculation() {
        Reservation adultPax = new Reservation("Adult Traveler", 30, "Male", "LOWER");
        // Sleeper = base 1000.0 * 1.0 = 1000.0
        double fare = fareService.calculateTotalFare(sampleTrain, "SLEEPER", List.of(adultPax));
        assertEquals(1000.0, fare, 0.01);
    }

    @Test
    public void testSeniorCitizenConcession40Percent() {
        Reservation seniorPax = new Reservation("Senior Traveler", 65, "Male", "LOWER");
        // Sleeper = 1000 * 0.60 = 600.0
        double fare = fareService.calculateTotalFare(sampleTrain, "SLEEPER", List.of(seniorPax));
        assertEquals(600.0, fare, 0.01);
    }

    @Test
    public void testChildDiscount50Percent() {
        Reservation childPax = new Reservation("Child Traveler", 8, "Female", "MIDDLE");
        // Sleeper = 1000 * 0.50 = 500.0
        double fare = fareService.calculateTotalFare(sampleTrain, "SLEEPER", List.of(childPax));
        assertEquals(500.0, fare, 0.01);
    }

    @Test
    public void testAcClassWithGst() {
        Reservation adultPax = new Reservation("Adult Traveler", 25, "Male", "LOWER");
        // AC_3_TIER multiplier = 1.8 -> 1000 * 1.8 = 1800. AC has 5% GST -> 1800 * 1.05 = 1890.0
        double fare = fareService.calculateTotalFare(sampleTrain, "AC_3_TIER", List.of(adultPax));
        assertEquals(1890.0, fare, 0.01);
    }
}
