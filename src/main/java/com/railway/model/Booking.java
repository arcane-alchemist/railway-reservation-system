package com.railway.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Booking entity representing a confirmed or pending reservation with a unique PNR.
 */
public class Booking {
    private int id;
    private String bookingReference; // PNR Number
    private int userId;
    private int trainId;
    private String trainNumber;
    private String trainName;
    private String sourceStation;
    private String destinationStation;
    private String departureTime;
    private String bookingDate;
    private String travelDate;
    private double totalFare;
    private String quota; // 'GENERAL', 'TATKAL', 'SENIOR_CITIZEN', 'LADIES'
    private String status; // 'CONFIRMED', 'CANCELLED', 'WAITING'
    private String travelClass;
    private double cancellationFee;
    private double refundAmount;
    private String cancellationReason;
    private String cancelledAt;
    private List<Reservation> reservations = new ArrayList<>();

    public Booking(int id, String bookingReference, int userId, int trainId,
                   String bookingDate, String travelDate, double totalFare,
                   String quota, String status, String travelClass) {
        this.id = id;
        this.bookingReference = bookingReference;
        this.userId = userId;
        this.trainId = trainId;
        this.bookingDate = bookingDate;
        this.travelDate = travelDate;
        this.totalFare = totalFare;
        this.quota = quota != null ? quota : "GENERAL";
        this.status = status != null ? status : "CONFIRMED";
        this.travelClass = travelClass != null ? travelClass : "AC_3_TIER";
        this.cancellationFee = 0.0;
        this.refundAmount = 0.0;
    }

    public Booking(int id, String bookingReference, int userId, int trainId,
                   String bookingDate, String travelDate, double totalFare,
                   String status, String travelClass) {
        this(id, bookingReference, userId, trainId, bookingDate, travelDate, totalFare, "GENERAL", status, travelClass);
    }

    public Booking(String bookingReference, int userId, int trainId,
                   String travelDate, double totalFare, String travelClass) {
        this(0, bookingReference, userId, trainId, null, travelDate, totalFare, "GENERAL", "CONFIRMED", travelClass);
    }

    public void addReservation(Reservation reservation) {
        this.reservations.add(reservation);
    }

    public int getPassengerCount() {
        return reservations != null ? reservations.size() : 0;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getBookingReference() {
        return bookingReference;
    }

    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getTrainId() {
        return trainId;
    }

    public void setTrainId(int trainId) {
        this.trainId = trainId;
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

    public String getSourceStation() {
        return sourceStation;
    }

    public void setSourceStation(String sourceStation) {
        this.sourceStation = sourceStation;
    }

    public String getDestinationStation() {
        return destinationStation;
    }

    public void setDestinationStation(String destinationStation) {
        this.destinationStation = destinationStation;
    }

    public String getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(String departureTime) {
        this.departureTime = departureTime;
    }

    public String getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(String bookingDate) {
        this.bookingDate = bookingDate;
    }

    public String getTravelDate() {
        return travelDate;
    }

    public void setTravelDate(String travelDate) {
        this.travelDate = travelDate;
    }

    public double getTotalFare() {
        return totalFare;
    }

    public void setTotalFare(double totalFare) {
        this.totalFare = totalFare;
    }

    public String getQuota() {
        return quota;
    }

    public void setQuota(String quota) {
        this.quota = quota;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTravelClass() {
        return travelClass;
    }

    public void setTravelClass(String travelClass) {
        this.travelClass = travelClass;
    }

    public double getCancellationFee() {
        return cancellationFee;
    }

    public void setCancellationFee(double cancellationFee) {
        this.cancellationFee = cancellationFee;
    }

    public double getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(double refundAmount) {
        this.refundAmount = refundAmount;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public String getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(String cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public List<Reservation> getReservations() {
        return reservations;
    }

    public void setReservations(List<Reservation> reservations) {
        this.reservations = reservations;
    }

    @Override
    public String toString() {
        return "PNR: " + bookingReference + " [" + quota + "] | Train: " + trainName + " (" + trainNumber +
                ") | Travel Date: " + travelDate + " | Status: " + status + " | Fare: ₹" + totalFare;
    }
}
