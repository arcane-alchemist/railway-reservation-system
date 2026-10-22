package com.railway.model;

/**
 * Reservation entity representing an individual passenger's seat assignment on a train.
 */
public class Reservation {
    private int id;
    private int bookingId;
    private int seatNumber;
    private String coachNumber;
    private String passengerName;
    private int passengerAge;
    private String passengerGender;
    private String berthPreference;
    private String allocatedBerth;
    private String status; // 'CONFIRMED', 'CANCELLED', 'WAITING'
    private boolean isRandomlyAllocated;
    private String allocationNote;

    public Reservation(int id, int bookingId, int seatNumber, String coachNumber,
                       String passengerName, int passengerAge, String passengerGender,
                       String berthPreference, String allocatedBerth, String status) {
        this.id = id;
        this.bookingId = bookingId;
        this.seatNumber = seatNumber;
        this.coachNumber = coachNumber != null ? coachNumber : "B1";
        this.passengerName = passengerName;
        this.passengerAge = passengerAge;
        this.passengerGender = passengerGender;
        this.berthPreference = berthPreference != null ? berthPreference : "NO_PREFERENCE";
        this.allocatedBerth = allocatedBerth != null ? allocatedBerth : this.berthPreference;
        this.status = status;
        this.isRandomlyAllocated = false;
        this.allocationNote = "";
    }

    public Reservation(int id, int bookingId, int seatNumber, String passengerName,
                       int passengerAge, String passengerGender, String berthPreference, String status) {
        this(id, bookingId, seatNumber, "B1", passengerName, passengerAge, passengerGender, berthPreference, berthPreference, status);
    }

    public Reservation(String passengerName, int passengerAge, String passengerGender, String berthPreference) {
        this(0, 0, 0, "B1", passengerName, passengerAge, passengerGender, berthPreference, berthPreference, "CONFIRMED");
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(int seatNumber) {
        this.seatNumber = seatNumber;
    }

    public String getCoachNumber() {
        return coachNumber;
    }

    public void setCoachNumber(String coachNumber) {
        this.coachNumber = coachNumber;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }

    public int getPassengerAge() {
        return passengerAge;
    }

    public void setPassengerAge(int passengerAge) {
        this.passengerAge = passengerAge;
    }

    public String getPassengerGender() {
        return passengerGender;
    }

    public void setPassengerGender(String passengerGender) {
        this.passengerGender = passengerGender;
    }

    public String getBerthPreference() {
        return berthPreference;
    }

    public void setBerthPreference(String berthPreference) {
        this.berthPreference = berthPreference;
    }

    public String getAllocatedBerth() {
        return allocatedBerth;
    }

    public void setAllocatedBerth(String allocatedBerth) {
        this.allocatedBerth = allocatedBerth;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isRandomlyAllocated() {
        return isRandomlyAllocated;
    }

    public void setRandomlyAllocated(boolean randomlyAllocated) {
        isRandomlyAllocated = randomlyAllocated;
    }

    public String getAllocationNote() {
        return allocationNote;
    }

    public void setAllocationNote(String allocationNote) {
        this.allocationNote = allocationNote;
    }

    @Override
    public String toString() {
        String seatStr = "WAITING".equalsIgnoreCase(status) ? "WL" : (coachNumber + " - Seat #" + seatNumber + " [" + allocatedBerth + "]");
        String noteStr = isRandomlyAllocated ? " (" + allocationNote + ")" : "";
        return passengerName + " (" + passengerAge + "/" + passengerGender + ") | " + seatStr + noteStr + " | Status: " + status;
    }
}
