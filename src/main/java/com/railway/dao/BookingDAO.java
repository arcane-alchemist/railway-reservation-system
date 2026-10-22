package com.railway.dao;

import com.railway.model.Booking;
import com.railway.model.Reservation;
import com.railway.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Booking transactions and Reservation persistence.
 */
public class BookingDAO {

    private final DatabaseConnection db = DatabaseConnection.getInstance();
    private final TrainDAO trainDAO = new TrainDAO();

    /**
    /**
     * Retrieves currently booked seat numbers for a train on a given travel date.
     */
    public java.util.Set<Integer> getBookedSeatNumbers(int trainId, String travelDate) {
        java.util.Set<Integer> booked = new java.util.HashSet<>();
        String sql = "SELECT r.seat_number FROM reservations r " +
                     "JOIN bookings b ON r.booking_id = b.id " +
                     "WHERE b.train_id = ? AND b.travel_date = ? AND r.status = 'CONFIRMED' AND r.seat_number > 0";
        try (Connection conn = db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, trainId);
            pstmt.setString(2, travelDate);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    booked.add(rs.getInt("seat_number"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching booked seat numbers: " + e.getMessage());
        }
        return booked;
    }

    /**
     * Creates a complete booking with passenger reservations in an atomic transaction.
     * Decrements available seats safely using trainDAO.reserveSeatsAtomically or reserveTatkalSeatsAtomically.
     */
    public boolean createBookingTransaction(Connection conn, Booking booking, List<Reservation> passengers) throws SQLException {
        boolean originalAutoCommit = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);

            boolean isTatkal = "TATKAL".equalsIgnoreCase(booking.getQuota());

            // Step 1: Attempt to atomically reserve seats on the train
            boolean seatsReserved;
            if (isTatkal) {
                seatsReserved = trainDAO.reserveTatkalSeatsAtomically(conn, booking.getTrainId(), passengers.size());
            } else {
                seatsReserved = trainDAO.reserveSeatsAtomically(conn, booking.getTrainId(), passengers.size());
            }

            String initialStatus = seatsReserved ? "CONFIRMED" : "WAITING";
            booking.setStatus(initialStatus);

            // Step 2: Insert into bookings table
            String insertBookingSql = "INSERT INTO bookings (booking_reference, user_id, train_id, travel_date, total_fare, quota, status, travel_class) " +
                                      "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            int bookingId;
            try (PreparedStatement pstmt = conn.prepareStatement(insertBookingSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, booking.getBookingReference());
                pstmt.setInt(2, booking.getUserId());
                pstmt.setInt(3, booking.getTrainId());
                pstmt.setString(4, booking.getTravelDate());
                pstmt.setDouble(5, booking.getTotalFare());
                pstmt.setString(6, booking.getQuota() != null ? booking.getQuota() : "GENERAL");
                pstmt.setString(7, initialStatus);
                pstmt.setString(8, booking.getTravelClass());

                pstmt.executeUpdate();
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        bookingId = generatedKeys.getInt(1);
                        booking.setId(bookingId);
                    } else {
                        conn.rollback();
                        return false;
                    }
                }
            }

            // Step 3: Insert passenger reservations
            String insertResSql = "INSERT INTO reservations (booking_id, seat_number, coach_number, passenger_name, passenger_age, passenger_gender, berth_preference, allocated_berth, status) " +
                                  "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertResSql)) {
                for (Reservation r : passengers) {
                    pstmt.setInt(1, bookingId);
                    int assignedSeat = seatsReserved ? (r.getSeatNumber() > 0 ? r.getSeatNumber() : 1) : 0;
                    String coach = seatsReserved ? (r.getCoachNumber() != null ? r.getCoachNumber() : "B1") : "WL";
                    String berth = seatsReserved ? (r.getAllocatedBerth() != null ? r.getAllocatedBerth() : "LOWER") : "WL";

                    r.setSeatNumber(assignedSeat);
                    r.setCoachNumber(coach);
                    r.setAllocatedBerth(berth);
                    r.setStatus(initialStatus);

                    pstmt.setInt(2, assignedSeat);
                    pstmt.setString(3, coach);
                    pstmt.setString(4, r.getPassengerName());
                    pstmt.setInt(5, r.getPassengerAge());
                    pstmt.setString(6, r.getPassengerGender());
                    pstmt.setString(7, r.getBerthPreference());
                    pstmt.setString(8, berth);
                    pstmt.setString(9, initialStatus);
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(originalAutoCommit);
        }
    }

    public List<Booking> getBookingsByUserId(int userId) {
        List<Booking> list = new ArrayList<>();
        String sql = "SELECT b.*, t.train_number, t.train_name, s1.station_name AS src_name, s2.station_name AS dest_name " +
                     "FROM bookings b " +
                     "JOIN trains t ON b.train_id = t.id " +
                     "JOIN stations s1 ON t.source_station_id = s1.id " +
                     "JOIN stations s2 ON t.destination_station_id = s2.id " +
                     "WHERE b.user_id = ? ORDER BY b.id DESC";
        try (Connection conn = db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Booking b = mapResultSetToBooking(rs);
                    b.setReservations(getReservationsForBooking(b.getId()));
                    list.add(b);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching user bookings: " + e.getMessage());
        }
        return list;
    }

    /**
     * Admin functionality: Retrieves all bookings system-wide across all passengers.
     */
    public List<Booking> getAllBookingsSystemWide() {
        List<Booking> list = new ArrayList<>();
        String sql = "SELECT b.*, t.train_number, t.train_name, s1.station_name AS src_name, s2.station_name AS dest_name " +
                     "FROM bookings b " +
                     "JOIN trains t ON b.train_id = t.id " +
                     "JOIN stations s1 ON t.source_station_id = s1.id " +
                     "JOIN stations s2 ON t.destination_station_id = s2.id " +
                     "ORDER BY b.id DESC";
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Booking b = mapResultSetToBooking(rs);
                b.setReservations(getReservationsForBooking(b.getId()));
                list.add(b);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching system-wide bookings: " + e.getMessage());
        }
        return list;
    }

    public Optional<Booking> getBookingByPNR(String pnr) {
        String sql = "SELECT b.*, t.train_number, t.train_name, s1.station_name AS src_name, s2.station_name AS dest_name " +
                     "FROM bookings b " +
                     "JOIN trains t ON b.train_id = t.id " +
                     "JOIN stations s1 ON t.source_station_id = s1.id " +
                     "JOIN stations s2 ON t.destination_station_id = s2.id " +
                     "WHERE b.booking_reference = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, pnr);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Booking b = mapResultSetToBooking(rs);
                    b.setReservations(getReservationsForBooking(b.getId()));
                    return Optional.of(b);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching booking by PNR: " + e.getMessage());
        }
        return Optional.empty();
    }

    public boolean cancelBooking(int bookingId) {
        return cancelBookingWithRefund(bookingId, 0.0, 0.0, "Standard cancellation");
    }

    public boolean cancelBookingWithRefund(int bookingId, double fee, double refund, String reason) {
        String queryBooking = "SELECT train_id, quota, status FROM bookings WHERE id = ?";
        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);
            int trainId = -1;
            String quota = "GENERAL";
            String currentStatus = null;

            try (PreparedStatement pstmt = conn.prepareStatement(queryBooking)) {
                pstmt.setInt(1, bookingId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        trainId = rs.getInt("train_id");
                        quota = rs.getString("quota");
                        currentStatus = rs.getString("status");
                    }
                }
            }

            if (trainId == -1 || "CANCELLED".equalsIgnoreCase(currentStatus)) {
                conn.rollback();
                return false;
            }

            // Count passenger seats to release
            int passengerCount = 0;
            try (PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) FROM reservations WHERE booking_id = ?")) {
                pstmt.setInt(1, bookingId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        passengerCount = rs.getInt(1);
                    }
                }
            }

            // Update booking status and refund details
            String updateBookingSql = "UPDATE bookings SET status = 'CANCELLED', cancellation_fee = ?, " +
                                      "refund_amount = ?, cancellation_reason = ?, cancelled_at = CURRENT_TIMESTAMP WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateBookingSql)) {
                pstmt.setDouble(1, fee);
                pstmt.setDouble(2, refund);
                pstmt.setString(3, reason != null ? reason : "Passenger requested cancellation");
                pstmt.setInt(4, bookingId);
                pstmt.executeUpdate();
            }

            // Update reservations status
            try (PreparedStatement pstmt = conn.prepareStatement("UPDATE reservations SET status = 'CANCELLED' WHERE booking_id = ?")) {
                pstmt.setInt(1, bookingId);
                pstmt.executeUpdate();
            }

            // Release seats in trains table if confirmed
            if (passengerCount > 0 && "CONFIRMED".equalsIgnoreCase(currentStatus)) {
                if ("TATKAL".equalsIgnoreCase(quota)) {
                    trainDAO.releaseTatkalSeats(conn, trainId, passengerCount);
                } else {
                    trainDAO.releaseSeats(conn, trainId, passengerCount);
                }
            }

            conn.commit();

            // Dispatches to Producer-Consumer waiting list promotion queue
            if (passengerCount > 0 && "CONFIRMED".equalsIgnoreCase(currentStatus)) {
                com.railway.concurrency.WaitingListPromotionWorker.getInstance()
                        .submitCancellation(trainId, bookingId, passengerCount);
            }
            return true;
        } catch (SQLException e) {
            System.err.println("Error cancelling booking with refund: " + e.getMessage());
            return false;
        }
    }

    public Optional<Booking> getOldestWaitingBookingForTrain(int trainId) {
        String sql = "SELECT b.*, t.train_number, t.train_name, s1.station_name AS src_name, s2.station_name AS dest_name " +
                     "FROM bookings b " +
                     "JOIN trains t ON b.train_id = t.id " +
                     "JOIN stations s1 ON t.source_station_id = s1.id " +
                     "JOIN stations s2 ON t.destination_station_id = s2.id " +
                     "WHERE b.train_id = ? AND b.status = 'WAITING' ORDER BY b.id ASC LIMIT 1";
        try (Connection conn = db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, trainId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Booking b = mapResultSetToBooking(rs);
                    b.setReservations(getReservationsForBooking(b.getId()));
                    return Optional.of(b);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching oldest waiting booking: " + e.getMessage());
        }
        return Optional.empty();
    }

    public boolean promoteWaitingBooking(int bookingId, int trainId, List<Reservation> passengers) {
        String updateBookingSql = "UPDATE bookings SET status = 'CONFIRMED' WHERE id = ?";
        String updateResSql = "UPDATE reservations SET status = 'CONFIRMED', seat_number = ?, coach_number = ?, allocated_berth = ? WHERE id = ?";
        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(updateBookingSql)) {
                pstmt.setInt(1, bookingId);
                pstmt.executeUpdate();
            }

            try (PreparedStatement pstmt = conn.prepareStatement(updateResSql)) {
                int seatCounter = 1;
                for (Reservation r : passengers) {
                    int assignedSeat = (trainId * 10) + (seatCounter % 60) + 1;
                    String berth = com.railway.service.BerthAllocator.getBerthForSeatNumber(assignedSeat);
                    pstmt.setInt(1, assignedSeat);
                    pstmt.setString(2, "B1");
                    pstmt.setString(3, berth);
                    pstmt.setInt(4, r.getId());
                    pstmt.addBatch();
                    seatCounter++;
                }
                pstmt.executeBatch();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Error promoting waiting booking: " + e.getMessage());
            return false;
        }
    }

    public List<Reservation> getReservationsForBooking(int bookingId) {
        List<Reservation> list = new ArrayList<>();
        String sql = "SELECT * FROM reservations WHERE booking_id = ? ORDER BY id ASC";
        try (Connection conn = db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, bookingId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String coach = "B1";
                    try { coach = rs.getString("coach_number"); } catch (SQLException ignored) {}
                    if (coach == null) coach = "B1";

                    String allocatedBerth = "LOWER";
                    try { allocatedBerth = rs.getString("allocated_berth"); } catch (SQLException ignored) {}
                    if (allocatedBerth == null) allocatedBerth = "LOWER";

                    list.add(new Reservation(
                            rs.getInt("id"),
                            rs.getInt("booking_id"),
                            rs.getInt("seat_number"),
                            coach,
                            rs.getString("passenger_name"),
                            rs.getInt("passenger_age"),
                            rs.getString("passenger_gender"),
                            rs.getString("berth_preference"),
                            allocatedBerth,
                            rs.getString("status")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching reservations: " + e.getMessage());
        }
        return list;
    }

    private Booking mapResultSetToBooking(ResultSet rs) throws SQLException {
        String quota = "GENERAL";
        try { quota = rs.getString("quota"); } catch (SQLException ignored) {}
        if (quota == null) quota = "GENERAL";

        Booking b = new Booking(
                rs.getInt("id"),
                rs.getString("booking_reference"),
                rs.getInt("user_id"),
                rs.getInt("train_id"),
                rs.getString("booking_date"),
                rs.getString("travel_date"),
                rs.getDouble("total_fare"),
                quota,
                rs.getString("status"),
                rs.getString("travel_class")
        );
        try { b.setCancellationFee(rs.getDouble("cancellation_fee")); } catch (SQLException ignored) {}
        try { b.setRefundAmount(rs.getDouble("refund_amount")); } catch (SQLException ignored) {}
        try { b.setCancellationReason(rs.getString("cancellation_reason")); } catch (SQLException ignored) {}
        try { b.setCancelledAt(rs.getString("cancelled_at")); } catch (SQLException ignored) {}

        try { b.setTrainNumber(rs.getString("train_number")); } catch (SQLException ignored) {}
        try { b.setTrainName(rs.getString("train_name")); } catch (SQLException ignored) {}
        try { b.setSourceStation(rs.getString("src_name")); } catch (SQLException ignored) {}
        try { b.setDestinationStation(rs.getString("dest_name")); } catch (SQLException ignored) {}
        return b;
    }
}
