package com.railway.dao;

import com.railway.model.Station;
import com.railway.model.Train;
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
 * Data Access Object for Station and Train queries and seat management.
 */
public class TrainDAO {

    private final DatabaseConnection db = DatabaseConnection.getInstance();

    public List<Station> getAllStations() {
        List<Station> stations = new ArrayList<>();
        String sql = "SELECT * FROM stations ORDER BY station_name ASC";
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                stations.add(new Station(
                        rs.getInt("id"),
                        rs.getString("station_code"),
                        rs.getString("station_name"),
                        rs.getString("city"),
                        rs.getString("state")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching stations: " + e.getMessage());
        }
        return stations;
    }

    public List<Train> getAllTrains() {
        List<Train> trains = new ArrayList<>();
        String sql = "SELECT t.*, s1.station_name AS src_name, s2.station_name AS dest_name " +
                     "FROM trains t " +
                     "JOIN stations s1 ON t.source_station_id = s1.id " +
                     "JOIN stations s2 ON t.destination_station_id = s2.id " +
                     "ORDER BY t.train_number ASC";
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                trains.add(mapResultSetToTrain(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all trains: " + e.getMessage());
        }
        return trains;
    }

    public List<Train> searchTrains(int sourceStationId, int destinationStationId) {
        List<Train> trains = new ArrayList<>();
        String sql = "SELECT t.*, s1.station_name AS src_name, s2.station_name AS dest_name " +
                     "FROM trains t " +
                     "JOIN stations s1 ON t.source_station_id = s1.id " +
                     "JOIN stations s2 ON t.destination_station_id = s2.id " +
                     "WHERE t.source_station_id = ? AND t.destination_station_id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sourceStationId);
            pstmt.setInt(2, destinationStationId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    trains.add(mapResultSetToTrain(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error searching trains: " + e.getMessage());
        }
        return trains;
    }

    public Optional<Train> findById(int trainId) {
        String sql = "SELECT t.*, s1.station_name AS src_name, s2.station_name AS dest_name " +
                     "FROM trains t " +
                     "JOIN stations s1 ON t.source_station_id = s1.id " +
                     "JOIN stations s2 ON t.destination_station_id = s2.id " +
                     "WHERE t.id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, trainId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToTrain(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching train by id: " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Atomically decrements seats if available. Returns true if seats reserved, false otherwise.
     * Prevents race conditions during concurrent bookings.
     */
    public boolean reserveSeatsAtomically(Connection conn, int trainId, int seatsToReserve) throws SQLException {
        String sql = "UPDATE trains SET available_seats = available_seats - ? " +
                     "WHERE id = ? AND available_seats >= ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, seatsToReserve);
            pstmt.setInt(2, trainId);
            pstmt.setInt(3, seatsToReserve);
            int rowsUpdated = pstmt.executeUpdate();
            return rowsUpdated > 0;
        }
    }

    public boolean reserveTatkalSeatsAtomically(Connection conn, int trainId, int seatsToReserve) throws SQLException {
        String sql = "UPDATE trains SET tatkal_available_seats = tatkal_available_seats - ?, available_seats = MAX(0, available_seats - ?) " +
                     "WHERE id = ? AND tatkal_available_seats >= ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, seatsToReserve);
            pstmt.setInt(2, seatsToReserve);
            pstmt.setInt(3, trainId);
            pstmt.setInt(4, seatsToReserve);
            int rowsUpdated = pstmt.executeUpdate();
            return rowsUpdated > 0;
        }
    }

    public void releaseSeats(Connection conn, int trainId, int seatsToRelease) throws SQLException {
        String sql = "UPDATE trains SET available_seats = MIN(total_seats, available_seats + ?) " +
                     "WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, seatsToRelease);
            pstmt.setInt(2, trainId);
            pstmt.executeUpdate();
        }
    }

    public void releaseTatkalSeats(Connection conn, int trainId, int seatsToRelease) throws SQLException {
        String sql = "UPDATE trains SET tatkal_available_seats = MIN(tatkal_total_seats, tatkal_available_seats + ?), " +
                     "available_seats = MIN(total_seats, available_seats + ?) " +
                     "WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, seatsToRelease);
            pstmt.setInt(2, seatsToRelease);
            pstmt.setInt(3, trainId);
            pstmt.executeUpdate();
        }
    }

    /**
     * Admin functionality to update live train pricing, tatkal seats, and schedules.
     */
    public boolean updateTrainPricingAndSeats(int trainId, double baseFare, double tatkalFare, int totalSeats, int tatkalSeats, String depTime, String arrTime) {
        String sql = "UPDATE trains SET base_fare = ?, tatkal_fare = ?, total_seats = ?, tatkal_total_seats = ?, departure_time = ?, arrival_time = ? WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, baseFare);
            pstmt.setDouble(2, tatkalFare);
            pstmt.setInt(3, totalSeats);
            pstmt.setInt(4, tatkalSeats);
            pstmt.setString(5, depTime);
            pstmt.setString(6, arrTime);
            pstmt.setInt(7, trainId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating train details: " + e.getMessage());
            return false;
        }
    }

    private Train mapResultSetToTrain(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String trainNumber = rs.getString("train_number");
        String trainName = rs.getString("train_name");
        int sourceStationId = rs.getInt("source_station_id");
        int destinationStationId = rs.getInt("destination_station_id");
        String departureTime = rs.getString("departure_time");
        String arrivalTime = rs.getString("arrival_time");
        String duration = "15h 40m";
        try { duration = rs.getString("duration"); } catch (SQLException ignored) {}
        if (duration == null) duration = "15h 40m";

        double rating = 4.8;
        try { rating = rs.getDouble("rating"); } catch (SQLException ignored) {}
        if (rating <= 0) rating = 4.8;

        int totalSeats = rs.getInt("total_seats");
        int availableSeats = rs.getInt("available_seats");

        int tatkalTotalSeats = 15;
        try { tatkalTotalSeats = rs.getInt("tatkal_total_seats"); } catch (SQLException ignored) {}
        if (tatkalTotalSeats <= 0) tatkalTotalSeats = 15;

        int tatkalAvailableSeats = 15;
        try { tatkalAvailableSeats = rs.getInt("tatkal_available_seats"); } catch (SQLException ignored) {}

        double baseFare = rs.getDouble("base_fare");
        double tatkalFare = baseFare * 1.30;
        try {
            double tf = rs.getDouble("tatkal_fare");
            if (tf > 0) tatkalFare = tf;
        } catch (SQLException ignored) {}

        String trainType = rs.getString("train_type");

        Train t;
        if ("RAJDHANI".equalsIgnoreCase(trainType)) {
            t = new com.railway.model.RajdhaniTrain(id, trainNumber, trainName, sourceStationId, destinationStationId,
                    departureTime, arrivalTime, duration, rating, totalSeats, availableSeats,
                    tatkalTotalSeats, tatkalAvailableSeats, baseFare, tatkalFare);
        } else if ("VANDE_BHARAT".equalsIgnoreCase(trainType)) {
            t = new com.railway.model.VandeBharatTrain(id, trainNumber, trainName, sourceStationId, destinationStationId,
                    departureTime, arrivalTime, duration, rating, totalSeats, availableSeats,
                    tatkalTotalSeats, tatkalAvailableSeats, baseFare, tatkalFare);
        } else {
            t = new Train(id, trainNumber, trainName, sourceStationId, destinationStationId,
                    departureTime, arrivalTime, duration, rating, totalSeats, availableSeats,
                    tatkalTotalSeats, tatkalAvailableSeats, baseFare, tatkalFare, trainType);
        }

        try {
            t.setSourceStationName(rs.getString("src_name"));
            t.setDestinationStationName(rs.getString("dest_name"));
        } catch (SQLException ignored) {}
        return t;
    }
}
