-- Railway Reservation System Database Schema (SQLite compatible)

PRAGMA foreign_keys = ON;

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL,
    role TEXT NOT NULL CHECK(role IN ('PASSENGER', 'ADMIN')),
    full_name TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL,
    phone TEXT,
    age INTEGER,
    gender TEXT,
    department TEXT, -- For Admin users
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Stations Table
CREATE TABLE IF NOT EXISTS stations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    station_code TEXT UNIQUE NOT NULL,
    station_name TEXT NOT NULL,
    city TEXT NOT NULL,
    state TEXT NOT NULL
);

-- 3. Trains Table
CREATE TABLE IF NOT EXISTS trains (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    train_number TEXT UNIQUE NOT NULL,
    train_name TEXT NOT NULL,
    source_station_id INTEGER NOT NULL,
    destination_station_id INTEGER NOT NULL,
    departure_time TEXT NOT NULL,
    arrival_time TEXT NOT NULL,
    duration TEXT NOT NULL DEFAULT '15h 40m',
    rating REAL NOT NULL DEFAULT 4.8,
    total_seats INTEGER NOT NULL,
    available_seats INTEGER NOT NULL,
    tatkal_total_seats INTEGER NOT NULL DEFAULT 15,
    tatkal_available_seats INTEGER NOT NULL DEFAULT 15,
    base_fare REAL NOT NULL,
    tatkal_fare REAL NOT NULL DEFAULT 2350.0,
    train_type TEXT NOT NULL, -- 'EXPRESS', 'SUPERFAST', 'VANDE_BHARAT', 'RAJDHANI'
    FOREIGN KEY (source_station_id) REFERENCES stations(id) ON DELETE RESTRICT,
    FOREIGN KEY (destination_station_id) REFERENCES stations(id) ON DELETE RESTRICT
);

-- 4. Bookings Table
CREATE TABLE IF NOT EXISTS bookings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    booking_reference TEXT UNIQUE NOT NULL, -- e.g. 'PNR10293847'
    user_id INTEGER NOT NULL,
    train_id INTEGER NOT NULL,
    booking_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    travel_date TEXT NOT NULL,
    total_fare REAL NOT NULL,
    quota TEXT NOT NULL DEFAULT 'GENERAL' CHECK(quota IN ('GENERAL', 'TATKAL', 'SENIOR_CITIZEN', 'LADIES')),
    status TEXT NOT NULL CHECK(status IN ('CONFIRMED', 'CANCELLED', 'WAITING')),
    travel_class TEXT NOT NULL DEFAULT 'AC_3_TIER',
    cancellation_fee REAL DEFAULT 0.0,
    refund_amount REAL DEFAULT 0.0,
    cancellation_reason TEXT,
    cancelled_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (train_id) REFERENCES trains(id) ON DELETE CASCADE
);

-- 5. Reservations (Passenger tickets inside a booking)
CREATE TABLE IF NOT EXISTS reservations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    booking_id INTEGER NOT NULL,
    seat_number INTEGER NOT NULL,
    coach_number TEXT NOT NULL DEFAULT 'B1',
    passenger_name TEXT NOT NULL,
    passenger_age INTEGER NOT NULL,
    passenger_gender TEXT NOT NULL,
    berth_preference TEXT NOT NULL, -- e.g., 'LOWER', 'MIDDLE', 'UPPER', 'SIDE_LOWER'
    allocated_berth TEXT NOT NULL DEFAULT 'LOWER',
    status TEXT NOT NULL CHECK(status IN ('CONFIRMED', 'CANCELLED', 'WAITING')),
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE
);

-- Seed Initial Data

-- Stations
INSERT OR IGNORE INTO stations (id, station_code, station_name, city, state) VALUES
(1, 'NDLS', 'New Delhi Central', 'New Delhi', 'Delhi'),
(2, 'CSMT', 'Chhatrapati Shivaji Maharaj Terminus', 'Mumbai', 'Maharashtra'),
(3, 'HWH', 'Howrah Junction', 'Kolkata', 'West Bengal'),
(4, 'SBC', 'KSR Bengaluru City', 'Bengaluru', 'Karnataka'),
(5, 'MAS', 'Chennai Central', 'Chennai', 'Tamil Nadu'),
(6, 'PNBE', 'Patna Junction', 'Patna', 'Bihar');

-- Trains
INSERT OR IGNORE INTO trains (id, train_number, train_name, source_station_id, destination_station_id, departure_time, arrival_time, duration, rating, total_seats, available_seats, tatkal_total_seats, tatkal_available_seats, base_fare, tatkal_fare, train_type) VALUES
(1, '12952', 'Mumbai Rajdhani Express', 1, 2, '16:55', '08:35', '15h 40m', 4.9, 60, 60, 15, 15, 1850.0, 2400.0, 'RAJDHANI'),
(2, '12951', 'New Delhi Rajdhani Express', 2, 1, '17:00', '08:32', '15h 32m', 4.8, 60, 60, 15, 15, 1850.0, 2400.0, 'RAJDHANI'),
(3, '22436', 'Vande Bharat Express', 1, 3, '06:00', '14:00', '08h 00m', 4.9, 50, 50, 10, 10, 1600.0, 2100.0, 'VANDE_BHARAT'),
(4, '12002', 'Bhopal Shatabdi Express', 1, 2, '06:00', '22:15', '16h 15m', 4.7, 75, 75, 15, 15, 1250.0, 1650.0, 'SUPERFAST'),
(5, '12622', 'Tamil Nadu Express', 1, 5, '21:05', '06:35', '33h 30m', 4.6, 80, 80, 20, 20, 1100.0, 1450.0, 'EXPRESS'),
(6, '12628', 'Karnataka Express', 1, 4, '20:20', '12:00', '39h 40m', 4.5, 80, 80, 20, 20, 1350.0, 1750.0, 'EXPRESS');

-- Default Users (Password: 'password123' or 'admin123')
INSERT OR IGNORE INTO users (id, username, password, role, full_name, email, phone, age, gender, department) VALUES
(1, 'admin', 'admin123', 'ADMIN', 'System Administrator', 'admin@railway.gov', '9876543210', 35, 'Other', 'Railway Operations'),
(2, 'passenger1', 'pass123', 'PASSENGER', 'Rahul Sharma', 'rahul@example.com', '9811122233', 28, 'Male', NULL),
(3, 'passenger2', 'pass123', 'PASSENGER', 'Priya Patel', 'priya@example.com', '9822233344', 62, 'Female', NULL);
