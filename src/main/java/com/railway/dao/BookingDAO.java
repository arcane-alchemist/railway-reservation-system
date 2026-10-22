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
     * Creates a complete booking with passenger reservations in an atomic transaction.
     * Decrements available seats safely using trainDAO.reserveSeatsAtomically.
     */
    public boolean createBookingTransaction(Connection conn, Booking booking, List<Reservation> passengers) throws SQLException {
        boolean originalAutoCommit = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);

            // Step 1: Attempt to atomically reserve seats on the train
            boolean seatsReserved = trainDAO.reserveSeatsAtomically(conn, booking.getTrainId(), passengers.size());
            String initialStatus = seatsReserved ? "CONFIRMED" : "WAITING";
            booking.setStatus(initialStatus);

            // Step 2: Insert into bookings table
            String insertBookingSql = "INSERT INTO bookings (booking_reference, user_id, train_id, travel_date, total_fare, status, travel_class) " +
                                      "VALUES (?, ?, ?, ?, ?, ?, ?)";
            int bookingId;
            try (PreparedStatement pstmt = conn.prepareStatement(insertBookingSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, booking.getBookingReference());
                pstmt.setInt(2, booking.getUserId());
                pstmt.setInt(3, booking.getTrainId());
                pstmt.setString(4, booking.getTravelDate());
                pstmt.setDouble(5, booking.getTotalFare());
                pstmt.setString(6, initialStatus);
                pstmt.setString(7, booking.getTravelClass());

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
            String insertResSql = "INSERT INTO reservations (booking_id, seat_number, passenger_name, passenger_age, passenger_gender, berth_preference, status) " +
                                  "VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertResSql)) {
                int seatCounter = 1;
                for (Reservation r : passengers) {
                    pstmt.setInt(1, bookingId);
                    int assignedSeat = seatsReserved ? ((booking.getTrainId() * 100) + seatCounter++) : 0;
                    r.setSeatNumber(assignedSeat);
                    r.setStatus(initialStatus);
                    pstmt.setInt(2, assignedSeat);
                    pstmt.setString(3, r.getPassengerName());
                    pstmt.setInt(4, r.getPassengerAge());
                    pstmt.setString(5, r.getPassengerGender());
                    pstmt.setString(6, r.getBerthPreference());
                    pstmt.setString(7, initialStatus);
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
        String queryBooking = "SELECT train_id, status FROM bookings WHERE id = ?";
        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);
            int trainId = -1;
            String currentStatus = null;

            try (PreparedStatement pstmt = conn.prepareStatement(queryBooking)) {
                pstmt.setInt(1, bookingId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        trainId = rs.getInt("train_id");
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

            // Update booking status
            try (PreparedStatement pstmt = conn.prepareStatement("UPDATE bookings SET status = 'CANCELLED' WHERE id = ?")) {
                pstmt.setInt(1, bookingId);
                pstmt.executeUpdate();
            }

            // Update reservations status
            try (PreparedStatement pstmt = conn.prepareStatement("UPDATE reservations SET status = 'CANCELLED' WHERE booking_id = ?")) {
                pstmt.setInt(1, bookingId);
                pstmt.executeUpdate();
            }

            conn.commit();

            // Dispatches to Producer-Consumer waiting list promotion queue
            if (passengerCount > 0 && "CONFIRMED".equalsIgnoreCase(currentStatus)) {
                com.railway.concurrency.WaitingListPromotionWorker.getInstance()
                        .submitCancellation(trainId, bookingId, passengerCount);
            }
            return true;
        } catch (SQLException e) {
            System.err.println("Error cancelling booking: " + e.getMessage());
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
        String updateResSql = "UPDATE reservations SET status = 'CONFIRMED', seat_number = ? WHERE id = ?";
        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(updateBookingSql)) {
                pstmt.setInt(1, bookingId);
                pstmt.executeUpdate();
            }

            try (PreparedStatement pstmt = conn.prepareStatement(updateResSql)) {
                int seatCounter = 1;
                for (Reservation r : passengers) {
                    int assignedSeat = (trainId * 100) + seatCounter++;
                    pstmt.setInt(1, assignedSeat);
                    pstmt.setInt(2, r.getId());
                    pstmt.addBatch();
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
                    list.add(new Reservation(
                            rs.getInt("id"),
                            rs.getInt("booking_id"),
                            rs.getInt("seat_number"),
                            rs.getString("passenger_name"),
                            rs.getInt("passenger_age"),
                            rs.getString("passenger_gender"),
                            rs.getString("berth_preference"),
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
        Booking b = new Booking(
                rs.getInt("id"),
                rs.getString("booking_reference"),
                rs.getInt("user_id"),
                rs.getInt("train_id"),
                rs.getString("booking_date"),
                rs.getString("travel_date"),
                rs.getDouble("total_fare"),
                rs.getString("status"),
                rs.getString("travel_class")
        );
        b.setTrainNumber(rs.getString("train_number"));
        b.setTrainName(rs.getString("train_name"));
        b.setSourceStation(rs.getString("src_name"));
        b.setDestinationStation(rs.getString("dest_name"));
        return b;
    }
}
