package com.railway.service;

import com.railway.dao.BookingDAO;
import com.railway.model.Booking;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Service executing official Indian Railways (IRCTC) Ticket Cancellation & Refund Policies.
 * 
 * Rules:
 * 1. Confirmed Tatkal Tickets: NO REFUND (100% cancellation charge).
 * 2. Waiting List Tickets: Flat clerkage fee of ₹60 per passenger.
 * 3. General Quota Tickets:
 *    - > 48 hours prior to departure: Flat deduction (AC1: ₹240, AC2: ₹200, AC3: ₹180, SL: ₹120) per passenger.
 *    - 12 to 48 hours prior: 25% of fare or flat charge (whichever is higher).
 *    - 4 to 12 hours prior: 50% deduction.
 *    - < 4 hours prior: 100% deduction (0 refund).
 */
public class CancellationService {

    private final BookingDAO bookingDAO;

    public CancellationService() {
        this.bookingDAO = new BookingDAO();
    }

    public CancellationService(BookingDAO bookingDAO) {
        this.bookingDAO = bookingDAO;
    }

    /**
     * DTO containing cancellation charges and refund breakdown for user review.
     */
    public static class CancellationQuote {
        private final Booking booking;
        private final double totalFare;
        private final double cancellationFee;
        private final double refundAmount;
        private final String ruleApplied;
        private final long hoursToDeparture;

        public CancellationQuote(Booking booking, double totalFare, double cancellationFee,
                                 double refundAmount, String ruleApplied, long hoursToDeparture) {
            this.booking = booking;
            this.totalFare = totalFare;
            this.cancellationFee = cancellationFee;
            this.refundAmount = refundAmount;
            this.ruleApplied = ruleApplied;
            this.hoursToDeparture = hoursToDeparture;
        }

        public Booking getBooking() {
            return booking;
        }

        public double getTotalFare() {
            return totalFare;
        }

        public double getCancellationFee() {
            return cancellationFee;
        }

        public double getRefundAmount() {
            return refundAmount;
        }

        public String getRuleApplied() {
            return ruleApplied;
        }

        public long getHoursToDeparture() {
            return hoursToDeparture;
        }

        @Override
        public String toString() {
            return String.format("Total Fare: ₹%.2f | Fee: ₹%.2f | Refund: ₹%.2f [%s]",
                    totalFare, cancellationFee, refundAmount, ruleApplied);
        }
    }

    /**
     * Calculates the official cancellation charge and refund quote based on IRCTC policy.
     */
    public CancellationQuote calculateRefundQuote(Booking booking) {
        int paxCount = Math.max(1, booking.getPassengerCount());
        double totalFare = booking.getTotalFare();
        String quota = booking.getQuota() != null ? booking.getQuota().toUpperCase() : "GENERAL";
        String status = booking.getStatus() != null ? booking.getStatus().toUpperCase() : "CONFIRMED";

        // Calculate hours to departure
        long hoursToDeparture = calculateHoursToDeparture(booking.getTravelDate(), booking.getDepartureTime());

        // RULE 1: Confirmed Tatkal Tickets have ZERO refund
        if ("TATKAL".equals(quota) && "CONFIRMED".equals(status)) {
            return new CancellationQuote(
                    booking, totalFare, totalFare, 0.0,
                    "IRCTC Tatkal Rule: No refund is admissible on Confirmed Tatkal tickets.",
                    hoursToDeparture
            );
        }

        // RULE 2: Waiting List Tickets
        if ("WAITING".equals(status)) {
            double clerkage = 60.0 * paxCount;
            double refund = Math.max(0.0, totalFare - clerkage);
            return new CancellationQuote(
                    booking, totalFare, clerkage, refund,
                    "IRCTC WL Clerkage Fee: ₹60 per passenger deducted.",
                    hoursToDeparture
            );
        }

        // RULE 3: General Quota Confirmed Tickets
        double flatPerPax;
        String travelClass = booking.getTravelClass() != null ? booking.getTravelClass().toUpperCase() : "AC_3_TIER";
        if (travelClass.contains("AC_1")) {
            flatPerPax = 240.0;
        } else if (travelClass.contains("AC_2")) {
            flatPerPax = 200.0;
        } else if (travelClass.contains("AC_3") || travelClass.contains("CHAIR")) {
            flatPerPax = 180.0;
        } else {
            flatPerPax = 120.0; // Sleeper
        }
        double baseFlatCharge = flatPerPax * paxCount;

        double fee;
        String ruleDescription;

        if (hoursToDeparture > 48) {
            fee = Math.min(totalFare, baseFlatCharge);
            ruleDescription = "> 48 Hours before departure: Flat cancellation charge of ₹" + flatPerPax + " per passenger.";
        } else if (hoursToDeparture >= 12) {
            fee = Math.max(baseFlatCharge, totalFare * 0.25);
            ruleDescription = "12 - 48 Hours before departure: 25% of fare (min ₹" + flatPerPax + "/pax).";
        } else if (hoursToDeparture >= 4) {
            fee = Math.max(baseFlatCharge, totalFare * 0.50);
            ruleDescription = "4 - 12 Hours before departure: 50% cancellation deduction.";
        } else {
            fee = totalFare;
            ruleDescription = "< 4 Hours before departure: Chart prepared; No refund.";
        }

        double refund = Math.max(0.0, Math.round((totalFare - fee) * 100.0) / 100.0);
        return new CancellationQuote(booking, totalFare, fee, refund, ruleDescription, hoursToDeparture);
    }

    /**
     * Executes the cancellation in the database, records refund details,
     * and dispatches seats to the WaitingListPromotionWorker.
     */
    public boolean processCancellation(Booking booking, String reason) {
        CancellationQuote quote = calculateRefundQuote(booking);
        boolean success = bookingDAO.cancelBookingWithRefund(
                booking.getId(),
                quote.getCancellationFee(),
                quote.getRefundAmount(),
                reason != null ? reason : "Passenger requested cancellation"
        );

        if (success) {
            booking.setStatus("CANCELLED");
            booking.setCancellationFee(quote.getCancellationFee());
            booking.setRefundAmount(quote.getRefundAmount());
            booking.setCancellationReason(reason);
            exportCancellationReceipt(booking, quote);
        }
        return success;
    }

    private long calculateHoursToDeparture(String travelDateStr, String departureTimeStr) {
        try {
            LocalDate travelDate = LocalDate.parse(travelDateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            LocalTime depTime = (departureTimeStr != null && departureTimeStr.contains(":"))
                    ? LocalTime.parse(departureTimeStr.trim()) : LocalTime.of(12, 0);
            LocalDateTime departureDateTime = LocalDateTime.of(travelDate, depTime);
            LocalDateTime now = LocalDateTime.now();
            return ChronoUnit.HOURS.between(now, departureDateTime);
        } catch (Exception e) {
            return 72; // Default to > 48 hours for demo flexibility
        }
    }

    private void exportCancellationReceipt(Booking booking, CancellationQuote quote) {
        File dir = new File("tickets");
        if (!dir.exists()) dir.mkdirs();
        File receiptFile = new File(dir, "Cancellation_Receipt_" + booking.getBookingReference() + ".txt");
        try (FileWriter writer = new FileWriter(receiptFile, StandardCharsets.UTF_8)) {
            writer.write("====================================================================\n");
            writer.write("           🚆  INDIAN RAILWAYS - CANCELLATION & REFUND SLIP         \n");
            writer.write("====================================================================\n");
            writer.write(" PNR NUMBER:        " + booking.getBookingReference() + "\n");
            writer.write(" TRAIN:             " + booking.getTrainName() + " (" + booking.getTrainNumber() + ")\n");
            writer.write(" TRAVEL DATE:       " + booking.getTravelDate() + " | QUOTA: " + booking.getQuota() + "\n");
            writer.write(" CANCELLATION DATE: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n");
            writer.write("--------------------------------------------------------------------\n");
            writer.write(String.format(" TOTAL FARE PAID:      ₹ %.2f\n", quote.getTotalFare()));
            writer.write(String.format(" CANCELLATION CHARGE:  ₹ %.2f\n", quote.getCancellationFee()));
            writer.write(String.format(" NET REFUND AMOUNT:    ₹ %.2f\n", quote.getRefundAmount()));
            writer.write(" REFUND POLICY RULE:   " + quote.getRuleApplied() + "\n");
            writer.write("--------------------------------------------------------------------\n");
            writer.write(" Status: CANCELLED. Refund initiated to original payment source.\n");
            writer.write("====================================================================\n");
        } catch (IOException ignored) {}
    }
}
