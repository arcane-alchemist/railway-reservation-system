package com.railway.model;

/**
 * Premium superfast train subclass demonstrating Polymorphism.
 * Includes complimentary gourmet meals, high operating speeds, and priority signalling.
 */
public class RajdhaniTrain extends Train {

    public RajdhaniTrain(int id, String trainNumber, String trainName, int sourceStationId, int destinationStationId,
                         String departureTime, String arrivalTime, int totalSeats, int availableSeats, double baseFare) {
        super(id, trainNumber, trainName, sourceStationId, destinationStationId, departureTime, arrivalTime,
                totalSeats, availableSeats, baseFare, "RAJDHANI");
    }

    @Override
    public String getAmenities() {
        return "Complimentary Gourmet Meals, Bedding & High-Speed Priority Track Clearance";
    }

    @Override
    public double getOperatingSpeedKmh() {
        return 140.0;
    }

    @Override
    public boolean hasComplimentaryMeals() {
        return true;
    }
}
