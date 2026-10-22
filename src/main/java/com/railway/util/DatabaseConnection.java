package com.railway.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Singleton database connection manager for SQLite.
 * Handles automatic schema initialization and provides thread-safe connections.
 */
public class DatabaseConnection {

    private static final String DB_FILE_NAME = "railway_system.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_FILE_NAME;
    private static volatile DatabaseConnection instance;
    private Connection connection;

    private DatabaseConnection() {
        try {
            // Load SQLite JDBC Driver
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection(DB_URL);
            
            // Configure SQLite pragmas for concurrency and integrity
            try (Statement stmt = this.connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
                stmt.execute("PRAGMA journal_mode = WAL;"); // Write-Ahead Logging for better concurrent read/write
                stmt.execute("PRAGMA busy_timeout = 5000;"); // 5s timeout waiting for lock
            }
            
            initializeSchemaIfRequired();
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC Driver not found: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("Error connecting to database: " + e.getMessage());
        }
    }

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return createNewConnection();
    }

    /**
     * Creates a new independent connection configured with WAL and busy timeout.
     */
    public static Connection createNewConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
            stmt.execute("PRAGMA journal_mode = WAL;");
            stmt.execute("PRAGMA busy_timeout = 10000;");
        }
        return conn;
    }

    /**
     * Executes schema.sql if tables do not exist yet.
     */
    private void initializeSchemaIfRequired() {
        try (Statement checkStmt = this.connection.createStatement()) {
            var rs = checkStmt.executeQuery("SELECT count(*) FROM sqlite_master WHERE type='table' AND name='trains';");
            if (rs.next() && rs.getInt(1) == 0) {
                System.out.println("Initializing database schema from schema.sql...");
                executeSchemaScript();
            } else {
                applySchemaMigrations(checkStmt);
            }
        } catch (SQLException e) {
            System.err.println("Schema verification error: " + e.getMessage());
        }
    }

    private void applySchemaMigrations(Statement stmt) {
        String[] migrations = {
            "ALTER TABLE trains ADD COLUMN duration TEXT NOT NULL DEFAULT '15h 40m'",
            "ALTER TABLE trains ADD COLUMN rating REAL NOT NULL DEFAULT 4.8",
            "ALTER TABLE trains ADD COLUMN tatkal_total_seats INTEGER NOT NULL DEFAULT 15",
            "ALTER TABLE trains ADD COLUMN tatkal_available_seats INTEGER NOT NULL DEFAULT 15",
            "ALTER TABLE trains ADD COLUMN tatkal_fare REAL NOT NULL DEFAULT 2350.0",
            "ALTER TABLE bookings ADD COLUMN quota TEXT NOT NULL DEFAULT 'GENERAL'",
            "ALTER TABLE bookings ADD COLUMN cancellation_fee REAL DEFAULT 0.0",
            "ALTER TABLE bookings ADD COLUMN refund_amount REAL DEFAULT 0.0",
            "ALTER TABLE bookings ADD COLUMN cancellation_reason TEXT",
            "ALTER TABLE bookings ADD COLUMN cancelled_at TIMESTAMP",
            "ALTER TABLE reservations ADD COLUMN coach_number TEXT NOT NULL DEFAULT 'B1'",
            "ALTER TABLE reservations ADD COLUMN allocated_berth TEXT NOT NULL DEFAULT 'LOWER'"
        };

        for (String m : migrations) {
            try {
                stmt.execute(m);
            } catch (SQLException ignored) {
                // Column already exists
            }
        }
    }

    private void executeSchemaScript() {
        File schemaFile = new File("database/schema.sql");
        StringBuilder sql = new StringBuilder();

        try {
            InputStream in = null;
            if (schemaFile.exists()) {
                in = new java.io.FileInputStream(schemaFile);
            } else {
                in = getClass().getResourceAsStream("/database/schema.sql");
                if (in == null) {
                    in = getClass().getClassLoader().getResourceAsStream("database/schema.sql");
                }
            }

            if (in != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String trimmedLine = line.trim();
                        if (!trimmedLine.startsWith("--")) {
                            sql.append(line).append("\n");
                        }
                    }
                }
            }

            if (sql.length() > 0) {
                String[] statements = sql.toString().split(";");
                try (Statement stmt = this.connection.createStatement()) {
                    for (String singleSql : statements) {
                        String trimmed = singleSql.trim();
                        if (!trimmed.isEmpty()) {
                            stmt.execute(trimmed);
                        }
                    }
                }
                System.out.println("Database schema and seed data loaded successfully.");
            }
        } catch (Exception e) {
            System.err.println("Failed to execute schema script: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public synchronized void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
        }
    }
}
