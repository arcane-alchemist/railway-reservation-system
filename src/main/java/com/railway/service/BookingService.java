package com.railway.service;

import com.railway.concurrency.BookingExecutor;
import com.railway.dao.BookingDAO;
import com.railway.dao.TrainDAO;
import com.railway.model.Booking;
import com.railway.model.Reservation;
import com.railway.model.Train;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service coordinating booking creation, fare calculation, ticket cancellations,
 * and concurrent execution.
 */
public class BookingService {

    private final BookingDAO bookingDAO;
    private final TrainDAO trainDAO;
    private final FareService fareService;
    private final BookingExecutor bookingExecutor;

    public BookingService() {
        this.bookingDAO = new BookingDAO();
        this.trainDAO = new TrainDAO();
        this.fareService = new FareService();
        this.bookingExecutor = BookingExecutor.getInstance();
    }

    public BookingService(BookingDAO bookingDAO, TrainDAO trainDAO, FareService fareService, BookingExecutor bookingExecutor) {
        this.bookingDAO = bookingDAO;
        this.trainDAO = trainDAO;
        this.fareService = fareService;
        this.bookingExecutor = bookingExecutor;
    }

    /**
     * Prepares and validates a booking, calculates fare polymorphically,
     * and submits it asynchronously to the multithreaded booking executor.
     */
    public CompletableFuture<BookingExecutor.BookingResult> bookTicketAsync(int userId, int trainId,
                                                                          String travelDate, String travelClass,
                                                                          List<Reservation> passengers) {
        if (passengers == null || passengers.isEmpty()) {
            return CompletableFuture.completedFuture(
                    new BookingExecutor.BookingResult(false, "At least one passenger is required for booking.", null, Thread.currentThread().getName())
            );
        }

        Optional<Train> trainOpt = trainDAO.findById(trainId);
        if (trainOpt.isEmpty()) {
            return CompletableFuture.completedFuture(
                    new BookingExecutor.BookingResult(false, "Train not found.", null, Thread.currentThread().getName())
            );
        }

        // Apply smart demographic berth allocation
        for (Reservation p : passengers) {
            p.setBerthPreference(BerthAllocator.allocateOptimalBerth(p));
        }

        Train train = trainOpt.get();
        double totalFare = fareService.calculateTotalFare(train, travelClass, passengers);
        String pnr = "PNR" + (10000000 + (int)(Math.random() * 90000000));

        Booking booking = new Booking(pnr, userId, trainId, travelDate, totalFare, travelClass);
        booking.setTrainNumber(train.getTrainNumber());
        booking.setTrainName(train.getTrainName());
        booking.setSourceStation(train.getSourceStationName());
        booking.setDestinationStation(train.getDestinationStationName());

        return bookingExecutor.processBookingAsync(booking, passengers);
    }

    public List<Booking> getUserBookings(int userId) {
        return bookingDAO.getBookingsByUserId(userId);
    }

    public Optional<Booking> getBookingByPNR(String pnr) {
        return bookingDAO.getBookingByPNR(pnr);
    }

    public boolean cancelTicket(int bookingId) {
        return bookingDAO.cancelBooking(bookingId);
    }

    public void runConcurrencySimulation(int trainId, int concurrentUsers) {
        bookingExecutor.simulateConcurrentBookings(trainId, concurrentUsers, 1);
    }

    public FareService getFareService() {
        return fareService;
    }
}
