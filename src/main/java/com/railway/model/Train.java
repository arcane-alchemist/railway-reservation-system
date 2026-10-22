package com.railway.model;

/**
 * Train entity representing a scheduled train service.
 * Demonstrates Encapsulation and Domain Logic.
 */
public class Train {
    private int id;
    private String trainNumber;
    private String trainName;
    private int sourceStationId;
    private int destinationStationId;
    private String sourceStationName;
    private String destinationStationName;
    private String departureTime;
    private String arrivalTime;
    private String duration; // e.g. '15h 40m'
    private double rating;   // e.g. 4.9
    private int totalSeats;
    private int availableSeats;
    private int tatkalTotalSeats;
    private int tatkalAvailableSeats;
    private double baseFare;
    private double tatkalFare;
    private String trainType; // RAJDHANI, VANDE_BHARAT, SUPERFAST, EXPRESS

    public Train(int id, String trainNumber, String trainName, int sourceStationId, int destinationStationId,
                 String departureTime, String arrivalTime, String duration, double rating,
                 int totalSeats, int availableSeats, int tatkalTotalSeats, int tatkalAvailableSeats,
                 double baseFare, double tatkalFare, String trainType) {
        this.id = id;
        this.trainNumber = trainNumber;
        this.trainName = trainName;
        this.sourceStationId = sourceStationId;
        this.destinationStationId = destinationStationId;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.duration = duration != null ? duration : "14h 30m";
        this.rating = rating > 0 ? rating : 4.8;
        this.totalSeats = totalSeats;
        this.availableSeats = availableSeats;
        this.tatkalTotalSeats = tatkalTotalSeats > 0 ? tatkalTotalSeats : 15;
        this.tatkalAvailableSeats = tatkalAvailableSeats > 0 ? tatkalAvailableSeats : this.tatkalTotalSeats;
        this.baseFare = baseFare;
        this.tatkalFare = tatkalFare > 0 ? tatkalFare : (baseFare * 1.30);
        this.trainType = trainType;
    }

    public Train(int id, String trainNumber, String trainName, int sourceStationId, int destinationStationId,
                 String departureTime, String arrivalTime, int totalSeats, int availableSeats,
                 double baseFare, String trainType) {
        this(id, trainNumber, trainName, sourceStationId, destinationStationId, departureTime, arrivalTime,
                "15h 30m", 4.8, totalSeats, availableSeats, 15, 15, baseFare, baseFare * 1.30, trainType);
    }

    public Train(String trainNumber, String trainName, int sourceStationId, int destinationStationId,
                 String departureTime, String arrivalTime, int totalSeats, int availableSeats,
                 double baseFare, String trainType) {
        this(0, trainNumber, trainName, sourceStationId, destinationStationId, departureTime,
                arrivalTime, totalSeats, availableSeats, baseFare, trainType);
    }

    // Thread-safe in-memory seat decrement for rapid concurrency verification
    public synchronized boolean tryReserveSeats(int count) {
        if (this.availableSeats >= count) {
            this.availableSeats -= count;
            return true;
        }
        return false;
    }

    public synchronized void releaseSeats(int count) {
        this.availableSeats = Math.min(this.totalSeats, this.availableSeats + count);
    }

    // Polymorphic methods overridden by specialized train subclasses
    public String getAmenities() {
        return "Standard Express Seating & Water Facility";
    }

    public double getOperatingSpeedKmh() {
        return 110.0;
    }

    public boolean hasComplimentaryMeals() {
        return false;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTrainNumber() {
        return trainNumber;
    }

    public void setTrainNumber(String trainNumber) {
        this.trainNumber = trainNumber;
    }

    public String getTrainName() {
        return trainName;
    }

    public void setTrainName(String trainName) {
        this.trainName = trainName;
    }

    public int getSourceStationId() {
        return sourceStationId;
    }

    public void setSourceStationId(int sourceStationId) {
        this.sourceStationId = sourceStationId;
    }

    public int getDestinationStationId() {
        return destinationStationId;
    }

    public void setDestinationStationId(int destinationStationId) {
        this.destinationStationId = destinationStationId;
    }

    public String getSourceStationName() {
        return sourceStationName;
    }

    public void setSourceStationName(String sourceStationName) {
        this.sourceStationName = sourceStationName;
    }

    public String getDestinationStationName() {
        return destinationStationName;
    }

    public void setDestinationStationName(String destinationStationName) {
        this.destinationStationName = destinationStationName;
    }

    public String getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(String departureTime) {
        this.departureTime = departureTime;
    }

    public String getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(String arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(int totalSeats) {
        this.totalSeats = totalSeats;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(int availableSeats) {
        this.availableSeats = availableSeats;
    }

    public int getTatkalTotalSeats() {
        return tatkalTotalSeats;
    }

    public void setTatkalTotalSeats(int tatkalTotalSeats) {
        this.tatkalTotalSeats = tatkalTotalSeats;
    }

    public int getTatkalAvailableSeats() {
        return tatkalAvailableSeats;
    }

    public void setTatkalAvailableSeats(int tatkalAvailableSeats) {
        this.tatkalAvailableSeats = tatkalAvailableSeats;
    }

    public double getBaseFare() {
        return baseFare;
    }

    public void setBaseFare(double baseFare) {
        this.baseFare = baseFare;
    }

    public double getTatkalFare() {
        return tatkalFare;
    }

    public void setTatkalFare(double tatkalFare) {
        this.tatkalFare = tatkalFare;
    }

    public String getTrainType() {
        return trainType;
    }

    public void setTrainType(String trainType) {
        this.trainType = trainType;
    }

    @Override
    public String toString() {
        return trainNumber + " - " + trainName + " (" + trainType + ") [" +
                (sourceStationName != null ? sourceStationName : "Station " + sourceStationId) + " -> " +
                (destinationStationName != null ? destinationStationName : "Station " + destinationStationId) + "]";
    }
}
