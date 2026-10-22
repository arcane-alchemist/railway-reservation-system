package com.railway.model;

/**
 * Semi-high-speed express train subclass demonstrating Polymorphism.
 * Features 160 km/h cruising speed, 180-degree rotating seats, and automated doors.
 */
public class VandeBharatTrain extends Train {

    public VandeBharatTrain(int id, String trainNumber, String trainName, int sourceStationId, int destinationStationId,
                            String departureTime, String arrivalTime, int totalSeats, int availableSeats, double baseFare) {
        super(id, trainNumber, trainName, sourceStationId, destinationStationId, departureTime, arrivalTime,
                totalSeats, availableSeats, baseFare, "VANDE_BHARAT");
    }

    public VandeBharatTrain(int id, String trainNumber, String trainName, int sourceStationId, int destinationStationId,
                            String departureTime, String arrivalTime, String duration, double rating,
                            int totalSeats, int availableSeats, int tatkalTotalSeats, int tatkalAvailableSeats,
                            double baseFare, double tatkalFare) {
        super(id, trainNumber, trainName, sourceStationId, destinationStationId, departureTime, arrivalTime,
                duration, rating, totalSeats, availableSeats, tatkalTotalSeats, tatkalAvailableSeats,
                baseFare, tatkalFare, "VANDE_BHARAT");
    }

    @Override
    public String getAmenities() {
        return "160 km/h Cruising, 180° Rotating Ergonomic Seats, Bio-Vacuum Toilets & Onboard Wi-Fi Infotainment";
    }

    @Override
    public double getOperatingSpeedKmh() {
        return 160.0;
    }

    @Override
    public boolean hasComplimentaryMeals() {
        return true;
    }
}
