package com.railway.service;

import com.railway.model.Reservation;

import java.security.SecureRandom;
import java.util.*;

/**
 * Intelligent Real-Time Berth Allocation engine embodying Indian Railways coach regulations.
 * 
 * Features:
 * 1. Standard Indian Railways 72-seat Sleeper / AC 3-Tier Coach Berth Formula:
 *    - Seat mod 8 in {1, 4} -> LOWER
 *    - Seat mod 8 in {2, 5} -> MIDDLE
 *    - Seat mod 8 in {3, 6} -> UPPER
 *    - Seat mod 8 == 7     -> SIDE_LOWER
 *    - Seat mod 8 == 0     -> SIDE_UPPER
 * 2. Demographic Accessibility Rules (Senior citizens receive Lower berths).
 * 3. Fallback Random Allocation: If a passenger's requested berth preference is NOT available,
 *    it automatically assigns a random unbooked seat and notifies the passenger.
 */
public class BerthAllocator {

    public static final String LOWER = "LOWER";
    public static final String MIDDLE = "MIDDLE";
    public static final String UPPER = "UPPER";
    public static final String SIDE_LOWER = "SIDE_LOWER";
    public static final String SIDE_UPPER = "SIDE_UPPER";
    public static final String NO_PREFERENCE = "NO_PREFERENCE";

    private static final Random RANDOM = new SecureRandom();

    /**
     * Maps an individual seat number (1-72) to its real coach berth type.
     */
    public static String getBerthForSeatNumber(int seatNumber) {
        int mod = seatNumber % 8;
        switch (mod) {
            case 1:
            case 4:
                return LOWER;
            case 2:
            case 5:
                return MIDDLE;
            case 3:
            case 6:
                return UPPER;
            case 7:
                return SIDE_LOWER;
            case 0:
                return SIDE_UPPER;
            default:
                return LOWER;
        }
    }

    /**
     * Determines optimal berth type based on demographics if no preference was expressed.
     */
    public static String allocateOptimalBerth(Reservation reservation) {
        return getDemographicPreference(reservation);
    }

    public static String getDemographicPreference(Reservation reservation) {
        String pref = reservation.getBerthPreference();
        if (pref != null && !pref.trim().isEmpty() &&
                !"No Preference".equalsIgnoreCase(pref.trim()) &&
                !NO_PREFERENCE.equalsIgnoreCase(pref.trim())) {
            return normalizeBerthName(pref);
        }

        // Senior Citizen Priority (Age 60+)
        if (reservation.getPassengerAge() >= 60) {
            return LOWER;
        }
        // Solo Female Traveler Priority
        if ("Female".equalsIgnoreCase(reservation.getPassengerGender())) {
            return LOWER;
        }
        // Children under 12
        if (reservation.getPassengerAge() < 12) {
            return MIDDLE;
        }

        return NO_PREFERENCE;
    }

    /**
     * Real-time seat allocation for a list of passengers against currently occupied seats.
     * If requested berth preference is unavailable, picks a RANDOM unbooked seat.
     *
     * @param totalSeats total capacity of the train
     * @param occupiedSeats set of seat numbers already booked for this train & date
     * @param passengers list of passengers to seat
     */
    public static void allocateSeatsRealtime(int totalSeats, Set<Integer> occupiedSeats, List<Reservation> passengers) {
        // Build list of unbooked seats
        List<Integer> freeSeats = new ArrayList<>();
        for (int s = 1; s <= totalSeats; s++) {
            if (!occupiedSeats.contains(s)) {
                freeSeats.add(s);
            }
        }

        for (Reservation pax : passengers) {
            if (freeSeats.isEmpty()) {
                // Coach is full -> Queued to Waiting List
                pax.setSeatNumber(0);
                pax.setStatus("WAITING");
                pax.setAllocatedBerth("WL");
                pax.setAllocationNote("Coach full. Placed on Waiting List.");
                continue;
            }

            String requestedPref = pax.getBerthPreference();
            if (requestedPref == null || requestedPref.isEmpty() || "No Preference".equalsIgnoreCase(requestedPref)) {
                requestedPref = getDemographicPreference(pax);
            } else {
                requestedPref = normalizeBerthName(requestedPref);
            }

            // Find free seats matching requested preference
            List<Integer> matchingSeats = new ArrayList<>();
            if (!NO_PREFERENCE.equalsIgnoreCase(requestedPref)) {
                for (int seat : freeSeats) {
                    if (getBerthForSeatNumber(seat).equalsIgnoreCase(requestedPref)) {
                        matchingSeats.add(seat);
                    }
                }
            }

            int chosenSeat;
            String allocatedBerth;

            if (!matchingSeats.isEmpty()) {
                // Requested preference is AVAILABLE!
                chosenSeat = matchingSeats.get(0);
                allocatedBerth = getBerthForSeatNumber(chosenSeat);
                pax.setRandomlyAllocated(false);
                pax.setAllocationNote("Allocated requested " + allocatedBerth + " berth.");
            } else {
                // Requested preference is UNAVAILABLE -> Pick a RANDOM unbooked seat!
                int randomIndex = RANDOM.nextInt(freeSeats.size());
                chosenSeat = freeSeats.get(randomIndex);
                allocatedBerth = getBerthForSeatNumber(chosenSeat);
                pax.setRandomlyAllocated(true);
                if (NO_PREFERENCE.equalsIgnoreCase(requestedPref)) {
                    pax.setAllocationNote("Assigned Seat #" + chosenSeat + " (" + allocatedBerth + ").");
                } else {
                    pax.setAllocationNote("Requested " + requestedPref + " unavailable. Randomly assigned Seat #" +
                            chosenSeat + " (" + allocatedBerth + ").");
                }
            }

            // Finalize passenger assignment
            pax.setSeatNumber(chosenSeat);
            pax.setCoachNumber("B1");
            pax.setAllocatedBerth(allocatedBerth);
            pax.setStatus("CONFIRMED");

            // Mark this seat as occupied for subsequent passengers in this group
            freeSeats.remove(Integer.valueOf(chosenSeat));
            occupiedSeats.add(chosenSeat);
        }
    }

    public static String normalizeBerthName(String pref) {
        if (pref == null) return NO_PREFERENCE;
        String cleaned = pref.trim().toUpperCase().replace(" ", "_");
        switch (cleaned) {
            case "LOWER":
            case "MIDDLE":
            case "UPPER":
            case "SIDE_LOWER":
            case "SIDE_UPPER":
                return cleaned;
            default:
                return NO_PREFERENCE;
        }
    }
}
