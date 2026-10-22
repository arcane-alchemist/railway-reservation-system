package com.railway.service;

import com.railway.dao.TrainDAO;
import com.railway.model.Station;
import com.railway.model.Train;

import java.util.List;
import java.util.Optional;

/**
 * Service managing train searches, station schedules, and seat availability.
 */
public class TrainService {

    private final TrainDAO trainDAO;

    public TrainService() {
        this.trainDAO = new TrainDAO();
    }

    public TrainService(TrainDAO trainDAO) {
        this.trainDAO = trainDAO;
    }

    public List<Station> getAvailableStations() {
        return trainDAO.getAllStations();
    }

    public List<Train> getAllTrains() {
        return trainDAO.getAllTrains();
    }

    public List<Train> searchTrains(int sourceStationId, int destinationStationId) {
        return trainDAO.searchTrains(sourceStationId, destinationStationId);
    }

    public Optional<Train> getTrainDetails(int trainId) {
        return trainDAO.findById(trainId);
    }

    public boolean hasAvailableSeats(int trainId, int requiredSeats) {
        return trainDAO.findById(trainId)
                .map(train -> train.getAvailableSeats() >= requiredSeats)
                .orElse(false);
    }

    public boolean updateTrainPricingAndSeats(int trainId, double baseFare, double tatkalFare, int totalSeats, int tatkalSeats, String depTime, String arrTime) {
        return trainDAO.updateTrainPricingAndSeats(trainId, baseFare, tatkalFare, totalSeats, tatkalSeats, depTime, arrTime);
    }
}

