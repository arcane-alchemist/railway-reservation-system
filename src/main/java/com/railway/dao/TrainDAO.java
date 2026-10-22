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

    public void releaseSeats(Connection conn, int trainId, int seatsToRelease) throws SQLException {
        String sql = "UPDATE trains SET available_seats = MIN(total_seats, available_seats + ?) " +
                     "WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, seatsToRelease);
            pstmt.setInt(2, trainId);
            pstmt.executeUpdate();
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
        int totalSeats = rs.getInt("total_seats");
        int availableSeats = rs.getInt("available_seats");
        double baseFare = rs.getDouble("base_fare");
        String trainType = rs.getString("train_type");

        Train t;
        if ("RAJDHANI".equalsIgnoreCase(trainType)) {
            t = new com.railway.model.RajdhaniTrain(id, trainNumber, trainName, sourceStationId, destinationStationId,
                    departureTime, arrivalTime, totalSeats, availableSeats, baseFare);
        } else if ("VANDE_BHARAT".equalsIgnoreCase(trainType)) {
            t = new com.railway.model.VandeBharatTrain(id, trainNumber, trainName, sourceStationId, destinationStationId,
                    departureTime, arrivalTime, totalSeats, availableSeats, baseFare);
        } else {
            t = new Train(id, trainNumber, trainName, sourceStationId, destinationStationId,
                    departureTime, arrivalTime, totalSeats, availableSeats, baseFare, trainType);
        }

        t.setSourceStationName(rs.getString("src_name"));
        t.setDestinationStationName(rs.getString("dest_name"));
        return t;
    }
}
