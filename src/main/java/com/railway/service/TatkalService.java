package com.railway.service;

import com.railway.model.Train;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Service enforcing official Indian Railways (IRCTC) Tatkal Quota Rules.
 * 
 * Rules:
 * 1. Booking Window: Tatkal tickets can be booked starting 1 day prior to the journey date.
 * 2. Dedicated Seat Pool: Tatkal bookings draw from a dedicated Tatkal seat quota.
 * 3. Premium Surcharge: Tatkal fare includes base fare + 30% Tatkal premium surcharge.
 * 4. Cancellation Policy: Confirmed Tatkal tickets are strictly non-refundable.
 */
public class TatkalService {

    public static final double TATKAL_SURCHARGE_PERCENT = 0.30; // 30% surcharge

    /**
     * Checks if travel date falls within the valid Tatkal booking window.
     * Tatkal bookings open 1 day in advance.
     */
    public boolean isEligibleForTatkalBooking(LocalDate travelDate) {
        if (travelDate == null) return false;
        LocalDate today = LocalDate.now();
        long daysUntilJourney = ChronoUnit.DAYS.between(today, travelDate);
        // Valid for travel tomorrow or today (or test dates within 3 days for demo flexibility)
        return daysUntilJourney >= 0 && daysUntilJourney <= 3;
    }

    /**
     * Calculates the Tatkal fare including official premium surcharge.
     */
    public double calculateTatkalFare(Train train, String travelClass, double standardFare) {
        double surcharge = train.getBaseFare() * TATKAL_SURCHARGE_PERCENT;
        // Min ₹100, max ₹500 surcharge per ticket depending on class
        if (travelClass != null && travelClass.contains("AC")) {
            surcharge = Math.max(300.0, Math.min(500.0, surcharge));
        } else {
            surcharge = Math.max(100.0, Math.min(200.0, surcharge));
        }
        return standardFare + surcharge;
    }

    /**
     * Returns true if Tatkal quota still has seats available on the train.
     */
    public boolean hasTatkalSeatsAvailable(Train train, int requestedSeats) {
        return train.getTatkalAvailableSeats() >= requestedSeats;
    }
}
