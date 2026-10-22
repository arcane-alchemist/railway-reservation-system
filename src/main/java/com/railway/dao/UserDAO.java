package com.railway.dao;

import com.railway.model.Admin;
import com.railway.model.Passenger;
import com.railway.model.User;
import com.railway.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/**
 * Data Access Object for User operations (Authentication, Registration, Profile).
 */
public class UserDAO {

    private final DatabaseConnection db = DatabaseConnection.getInstance();

    public Optional<User> authenticate(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("password");
                    if (com.railway.util.SecurityUtil.verifyPassword(password, storedPassword)) {
                        return Optional.of(mapResultSetToUser(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Authentication query error: " + e.getMessage());
        }
        return Optional.empty();
    }

    public boolean registerPassenger(Passenger passenger) {
        String sql = "INSERT INTO users (username, password, role, full_name, email, phone, age, gender) " +
                     "VALUES (?, ?, 'PASSENGER', ?, ?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, passenger.getUsername());
            pstmt.setString(2, com.railway.util.SecurityUtil.hashPassword(passenger.getPassword()));
            pstmt.setString(3, passenger.getFullName());
            pstmt.setString(4, passenger.getEmail());
            pstmt.setString(5, passenger.getPhone());
            pstmt.setInt(6, passenger.getAge());
            pstmt.setString(7, passenger.getGender());

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        passenger.setId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Registration error: " + e.getMessage());
        }
        return false;
    }

    public Optional<User> findById(int userId) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("User lookup error: " + e.getMessage());
        }
        return Optional.empty();
    }

    public boolean isUsernameTaken(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Username check error: " + e.getMessage());
        }
        return false;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String uname = rs.getString("username");
        String pass = rs.getString("password");
        String role = rs.getString("role");
        String fullName = rs.getString("full_name");
        String email = rs.getString("email");

        if ("ADMIN".equalsIgnoreCase(role)) {
            String department = rs.getString("department");
            return new Admin(id, uname, pass, fullName, email, department != null ? department : "Operations");
        } else {
            String phone = rs.getString("phone");
            int age = rs.getInt("age");
            String gender = rs.getString("gender");
            return new Passenger(id, uname, pass, fullName, email, phone, age, gender);
        }
    }
}
