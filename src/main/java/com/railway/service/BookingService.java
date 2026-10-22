package com.railway.service;

import com.railway.concurrency.BookingExecutor;
import com.railway.dao.BookingDAO;
import com.railway.dao.TrainDAO;
import com.railway.model.Booking;
import com.railway.model.Reservation;
import com.railway.model.Train;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Service coordinating real-time booking creation, intelligent berth allocation,
 * official IRCTC Tatkal quota enforcement, and tiered cancellations.
 */
public class BookingService {

    private final BookingDAO bookingDAO;
    private final TrainDAO trainDAO;
    private final FareService fareService;
    private final TatkalService tatkalService;
    private final CancellationService cancellationService;
    private final BookingExecutor bookingExecutor;

    public BookingService() {
        this.bookingDAO = new BookingDAO();
        this.trainDAO = new TrainDAO();
        this.fareService = new FareService();
        this.tatkalService = new TatkalService();
        this.cancellationService = new CancellationService(this.bookingDAO);
        this.bookingExecutor = BookingExecutor.getInstance();
    }

    public BookingService(BookingDAO bookingDAO, TrainDAO trainDAO, FareService fareService,
                          TatkalService tatkalService, CancellationService cancellationService,
                          BookingExecutor bookingExecutor) {
        this.bookingDAO = bookingDAO;
        this.trainDAO = trainDAO;
        this.fareService = fareService;
        this.tatkalService = tatkalService;
        this.cancellationService = cancellationService;
        this.bookingExecutor = bookingExecutor;
    }

    /**
     * Overloaded method defaulting to GENERAL quota for backward compatibility.
     */
    public CompletableFuture<BookingExecutor.BookingResult> bookTicketAsync(int userId, int trainId,
                                                                          String travelDate, String travelClass,
                                                                          List<Reservation> passengers) {
        return bookTicketAsync(userId, trainId, travelDate, travelClass, "GENERAL", passengers);
    }

    /**
     * Prepares and validates a real-time booking with IRCTC quota rules,
     * allocates real coach seats (falling back to a random unbooked seat if preferred berth is unavailable),
     * and submits it asynchronously to the multithreaded booking executor.
     */
    public CompletableFuture<BookingExecutor.BookingResult> bookTicketAsync(int userId, int trainId,
                                                                          String travelDate, String travelClass,
                                                                          String quota,
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

        Train train = trainOpt.get();
        String normalizedQuota = (quota != null && !quota.trim().isEmpty()) ? quota.trim().toUpperCase() : "GENERAL";

        // Step 1: IRCTC Tatkal Quota Validation
        if ("TATKAL".equalsIgnoreCase(normalizedQuota)) {
            LocalDate journeyDate = null;
            try {
                journeyDate = LocalDate.parse(travelDate);
            } catch (Exception ignored) {}

            if (journeyDate != null && !tatkalService.isEligibleForTatkalBooking(journeyDate)) {
                return CompletableFuture.completedFuture(
                        new BookingExecutor.BookingResult(false, "Tatkal bookings can only be made 1 day in advance of journey date.", null, Thread.currentThread().getName())
                );
            }

            if (!tatkalService.hasTatkalSeatsAvailable(train, passengers.size())) {
                return CompletableFuture.completedFuture(
                        new BookingExecutor.BookingResult(false, "Requested seats exceed available Tatkal quota (" + train.getTatkalAvailableSeats() + " left).", null, Thread.currentThread().getName())
                );
            }
        }

        // Step 2: Real-Time Seat & Berth Allocation with Random Fallback
        Set<Integer> occupiedSeats = bookingDAO.getBookedSeatNumbers(trainId, travelDate);
        BerthAllocator.allocateSeatsRealtime(train.getTotalSeats(), occupiedSeats, passengers);

        // Check if any passenger got randomly allocated
        boolean anyRandomlyAssigned = false;
        StringBuilder notes = new StringBuilder();
        for (Reservation p : passengers) {
            if (p.isRandomlyAllocated()) {
                anyRandomlyAssigned = true;
                if (notes.length() > 0) notes.append("; ");
                notes.append(p.getPassengerName()).append(": ").append(p.getAllocationNote());
            }
        }

        // Step 3: Fare Calculation
        double totalFare;
        if ("TATKAL".equalsIgnoreCase(normalizedQuota)) {
            double standardFare = fareService.calculateTotalFare(train, travelClass, passengers);
            totalFare = tatkalService.calculateTatkalFare(train, travelClass, standardFare);
        } else {
            totalFare = fareService.calculateTotalFare(train, travelClass, passengers);
        }

        String pnr = "PNR" + (10000000 + (int)(Math.random() * 90000000));
        Booking booking = new Booking(pnr, userId, trainId, travelDate, totalFare, travelClass);
        booking.setQuota(normalizedQuota);
        booking.setTrainNumber(train.getTrainNumber());
        booking.setTrainName(train.getTrainName());
        booking.setSourceStation(train.getSourceStationName());
        booking.setDestinationStation(train.getDestinationStationName());
        booking.setDepartureTime(train.getDepartureTime());

        final boolean hasRandomAllocation = anyRandomlyAssigned;
        final String randomNotes = notes.toString();

        return bookingExecutor.processBookingAsync(booking, passengers)
                .thenApply(result -> {
                    if (result.isSuccess() && hasRandomAllocation && !"WAITING".equalsIgnoreCase(result.getBooking().getStatus())) {
                        String enhancedMsg = result.message() + " [Berth Notice: " + randomNotes + "]";
                        return new BookingExecutor.BookingResult(true, enhancedMsg, result.getBooking(), result.getThreadName());
                    }
                    return result;
                });
    }

    public List<Booking> getUserBookings(int userId) {
        return bookingDAO.getBookingsByUserId(userId);
    }

    public List<Booking> getAllBookingsSystemWide() {
        return bookingDAO.getAllBookingsSystemWide();
    }

    public Optional<Booking> getBookingByPNR(String pnr) {
        return bookingDAO.getBookingByPNR(pnr);
    }

    public CancellationService.CancellationQuote calculateCancellationQuote(Booking booking) {
        return cancellationService.calculateRefundQuote(booking);
    }

    public boolean cancelTicketWithRefund(Booking booking, String reason) {
        return cancellationService.processCancellation(booking, reason);
    }

    public boolean cancelTicket(int bookingId) {
        Optional<Booking> opt = bookingDAO.getBookingsByUserId(AuthService.getCurrentUser() != null ? AuthService.getCurrentUser().getId() : 2)
                .stream().filter(b -> b.getId() == bookingId).findFirst();
        if (opt.isPresent()) {
            return cancelTicketWithRefund(opt.get(), "Passenger requested cancellation");
        }
        return bookingDAO.cancelBooking(bookingId);
    }

    public void runConcurrencySimulation(int trainId, int concurrentUsers) {
        bookingExecutor.simulateConcurrentBookings(trainId, concurrentUsers, 1);
    }

    public FareService getFareService() {
        return fareService;
    }

    public TatkalService getTatkalService() {
        return tatkalService;
    }

    public CancellationService getCancellationService() {
        return cancellationService;
    }
}
