package com.railway.service;

import com.railway.model.Reservation;
import com.railway.model.Train;

/**
 * Service demonstrating Polymorphism in business logic.
 * Calculates dynamic fares, class multipliers, and passenger category concessions.
 */
public class FareService {

    /**
     * Interface defining the contract for polymorphic fare calculations.
     */
    public interface FareStrategy {
        double calculatePassengerFare(double baseFare, int age, String travelClass);
        String getCategoryName();
    }

    /**
     * Standard Adult Passenger Fare Strategy
     */
    public static class StandardFareStrategy implements FareStrategy {
        @Override
        public double calculatePassengerFare(double baseFare, int age, String travelClass) {
            double multiplier = getClassMultiplier(travelClass);
            return baseFare * multiplier;
        }

        @Override
        public String getCategoryName() {
            return "Standard Adult";
        }
    }

    /**
     * Senior Citizen Concession Strategy (Age 60+, 40% discount) - Polymorphic dispatch
     */
    public static class SeniorCitizenFareStrategy implements FareStrategy {
        @Override
        public double calculatePassengerFare(double baseFare, int age, String travelClass) {
            double multiplier = getClassMultiplier(travelClass);
            double fullFare = baseFare * multiplier;
            return fullFare * 0.60; // 40% Senior Concession
        }

        @Override
        public String getCategoryName() {
            return "Senior Citizen (40% Concession)";
        }
    }

    /**
     * Child Fare Strategy (Age < 12, 50% discount) - Polymorphic dispatch
     */
    public static class ChildFareStrategy implements FareStrategy {
        @Override
        public double calculatePassengerFare(double baseFare, int age, String travelClass) {
            double multiplier = getClassMultiplier(travelClass);
            double fullFare = baseFare * multiplier;
            return fullFare * 0.50; // 50% Child Discount
        }

        @Override
        public String getCategoryName() {
            return "Child (50% Concession)";
        }
    }

    /**
     * Resolves the polymorphic strategy based on passenger characteristics.
     */
    public FareStrategy resolveStrategy(int age) {
        if (age >= 60) {
            return new SeniorCitizenFareStrategy();
        } else if (age < 12) {
            return new ChildFareStrategy();
        } else {
            return new StandardFareStrategy();
        }
    }

    /**
     * Computes the total fare for a train journey and list of passenger reservations.
     */
    public double calculateTotalFare(Train train, String travelClass, Iterable<Reservation> passengers) {
        double total = 0.0;
        double baseFare = train.getBaseFare();

        for (Reservation passenger : passengers) {
            FareStrategy strategy = resolveStrategy(passenger.getPassengerAge());
            total += strategy.calculatePassengerFare(baseFare, passenger.getPassengerAge(), travelClass);
        }

        // Add 5% GST on AC classes
        if (travelClass.contains("AC")) {
            total = total * 1.05;
        }

        return Math.round(total * 100.0) / 100.0;
    }

    public static double getClassMultiplier(String travelClass) {
        if (travelClass == null) return 1.0;
        switch (travelClass.toUpperCase()) {
            case "AC_1_TIER":
                return 3.5;
            case "AC_2_TIER":
                return 2.5;
            case "AC_3_TIER":
                return 1.8;
            case "CHAIR_CAR":
                return 1.3;
            case "SLEEPER":
            default:
                return 1.0;
        }
    }
}
