package com.railway.service;

import com.railway.model.Booking;
import com.railway.model.Reservation;
import com.railway.model.Train;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CancellationAndTatkalTest {

    private final TatkalService tatkalService = new TatkalService();
    private final CancellationService cancellationService = new CancellationService();

    @Test
    public void testTatkalAdvanceBookingWindow() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDate nextMonth = LocalDate.now().plusDays(30);

        assertTrue(tatkalService.isEligibleForTatkalBooking(tomorrow), "Tatkal should be eligible 1 day prior");
        assertFalse(tatkalService.isEligibleForTatkalBooking(nextMonth), "Tatkal should not be open 30 days prior");
    }

    @Test
    public void testTatkalSurchargeCalculation() {
        Train train = new Train(1, "12952", "Rajdhani", 1, 2, "16:55", "08:35", 60, 60, 1000.0, "RAJDHANI");
        double baseFare = 1000.0;
        double tatkalFare = tatkalService.calculateTatkalFare(train, "AC_3_TIER", baseFare);

        // 30% surcharge on 1000 = 300
        assertEquals(1300.0, tatkalFare, 0.01, "Tatkal fare should add 30% premium surcharge");
    }

    @Test
    public void testTatkalConfirmedTicketHasZeroRefundOnCancellation() {
        Booking booking = new Booking("PNR12345678", 1, 1, "2026-11-01", 2000.0, "AC_3_TIER");
        booking.setQuota("TATKAL");
        booking.setStatus("CONFIRMED");
        booking.addReservation(new Reservation("User", 25, "Male", "LOWER"));

        CancellationService.CancellationQuote quote = cancellationService.calculateRefundQuote(booking);

        assertEquals(0.0, quote.getRefundAmount(), 0.01, "Confirmed Tatkal tickets must receive ZERO refund");
        assertEquals(2000.0, quote.getCancellationFee(), 0.01, "Confirmed Tatkal ticket fee should be 100%");
        assertTrue(quote.getRuleApplied().contains("Tatkal"));
    }

    @Test
    public void testWaitingListTicketHasFlatClerkage() {
        Booking booking = new Booking("PNR87654321", 1, 1, "2026-11-01", 1000.0, "AC_3_TIER");
        booking.setStatus("WAITING");
        booking.addReservation(new Reservation("User1", 25, "Male", "LOWER"));
        booking.addReservation(new Reservation("User2", 28, "Female", "UPPER"));

        CancellationService.CancellationQuote quote = cancellationService.calculateRefundQuote(booking);

        // 2 passengers * 60 = 120 clerkage fee, refund = 1000 - 120 = 880
        assertEquals(120.0, quote.getCancellationFee(), 0.01, "WL cancellation clerkage should be ₹60 per passenger");
        assertEquals(880.0, quote.getRefundAmount(), 0.01, "Net refund should be fare minus clerkage");
    }
}
