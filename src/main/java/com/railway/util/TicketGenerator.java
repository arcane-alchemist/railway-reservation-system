package com.railway.util;

import com.railway.model.Booking;
import com.railway.model.Reservation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Utility for rendering and exporting official Indian Railways digital E-Tickets / Boarding Passes.
 */
public class TicketGenerator {

    public static String generateETicketText(Booking booking) {
        StringBuilder sb = new StringBuilder();
        sb.append("==================================================================================\n");
        sb.append("                      🚆  INDIAN RAILWAYS - ELECTRONIC RESERVATION SLIP (ERS)     \n");
        sb.append("==================================================================================\n");
        sb.append(String.format(" PNR NUMBER: %-25s BOOKING DATE: %s\n", booking.getBookingReference(), booking.getBookingDate() != null ? booking.getBookingDate() : "Instant"));
        sb.append(String.format(" STATUS:     %-25s TRAVEL CLASS:  %s\n", booking.getStatus(), booking.getTravelClass()));
        sb.append("----------------------------------------------------------------------------------\n");
        sb.append(String.format(" TRAIN:      %s (%s)\n", booking.getTrainName(), booking.getTrainNumber()));
        sb.append(String.format(" ROUTE:      %s  ➔  %s\n", booking.getSourceStation(), booking.getDestinationStation()));
        sb.append(String.format(" DEPARTURE DATE: %s\n", booking.getTravelDate()));
        sb.append("==================================================================================\n");
        sb.append(String.format(" %-4s %-25s %-6s %-8s %-15s %-12s\n", "#", "PASSENGER NAME", "AGE", "GENDER", "BERTH / SEAT", "STATUS"));
        sb.append("----------------------------------------------------------------------------------\n");

        int index = 1;
        if (booking.getReservations() != null) {
            for (Reservation r : booking.getReservations()) {
                String seatStr = "WAITING".equalsIgnoreCase(r.getStatus()) ? "WL" : "Seat #" + r.getSeatNumber();
                sb.append(String.format(" %-4d %-25s %-6d %-8s %-15s %-12s\n",
                        index++,
                        r.getPassengerName(),
                        r.getPassengerAge(),
                        r.getPassengerGender(),
                        seatStr + " (" + r.getBerthPreference() + ")",
                        r.getStatus()
                ));
            }
        }

        sb.append("==================================================================================\n");
        sb.append(String.format(" TOTAL FARE PAID (INCL. GST):  ₹ %.2f\n", booking.getTotalFare()));
        sb.append(" DIGITAL VERIFICATION CODE:    " + Math.abs(booking.getBookingReference().hashCode()) + "-SECURE\n");
        sb.append("==================================================================================\n");
        sb.append("  * Please carry valid Government ID during travel.\n");
        sb.append("  * Have a pleasant and safe journey with Indian Railways.\n");
        sb.append("==================================================================================\n");
        return sb.toString();
    }

    public static File exportTicketToFile(Booking booking, String outputDirPath) throws IOException {
        File dir = new File(outputDirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File ticketFile = new File(dir, "Ticket_" + booking.getBookingReference() + ".txt");
        try (FileWriter writer = new FileWriter(ticketFile, StandardCharsets.UTF_8)) {
            writer.write(generateETicketText(booking));
        }
        return ticketFile;
    }
}
