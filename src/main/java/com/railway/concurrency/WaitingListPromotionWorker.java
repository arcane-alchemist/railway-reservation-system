package com.railway.concurrency;

import com.railway.dao.BookingDAO;
import com.railway.dao.TrainDAO;
import com.railway.model.Booking;
import com.railway.model.Reservation;
import com.railway.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Background consumer thread demonstrating the Producer-Consumer Multithreading pattern.
 * Continuously polls a thread-safe BlockingQueue for ticket cancellation events,
 * and automatically promotes the highest-priority waiting list passenger to CONFIRMED status.
 */
public class WaitingListPromotionWorker implements Runnable {

    private static final BlockingQueue<CancellationEvent> cancellationQueue = new LinkedBlockingQueue<>(1000);
    private static volatile WaitingListPromotionWorker instance;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final BookingDAO bookingDAO = new BookingDAO();
    private final TrainDAO trainDAO = new TrainDAO();
    private Thread workerThread;

    private WaitingListPromotionWorker() {
        startWorker();
    }

    public static WaitingListPromotionWorker getInstance() {
        if (instance == null) {
            synchronized (WaitingListPromotionWorker.class) {
                if (instance == null) {
                    instance = new WaitingListPromotionWorker();
                }
            }
        }
        return instance;
    }

    private void startWorker() {
        workerThread = new Thread(this, "Railway-WaitListPromoter");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    /**
     * Producer method: submits a cancellation event into the thread-safe queue.
     */
    public void submitCancellation(int trainId, int cancelledBookingId, int releasedSeats) {
        cancellationQueue.offer(new CancellationEvent(trainId, cancelledBookingId, releasedSeats));
    }

    @Override
    public void run() {
        System.out.println(">>> [WaitingListPromotionWorker] Consumer thread started and listening for cancellations.");
        while (running.get() || !cancellationQueue.isEmpty()) {
            try {
                CancellationEvent event = cancellationQueue.poll(2, TimeUnit.SECONDS);
                if (event != null) {
                    handleCancellationEvent(event);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Error in WaitingListPromotionWorker: " + e.getMessage());
            }
        }
        System.out.println(">>> [WaitingListPromotionWorker] Consumer thread shutting down.");
    }

    /**
     * Consumes cancellation event, finds eligible waiting bookings on the train,
     * and atomically promotes them to CONFIRMED with assigned seats.
     */
    private void handleCancellationEvent(CancellationEvent event) {
        int trainId = event.getTrainId();
        int availableToReassign = event.getReleasedSeatCount();

        // Lock on the specific train to ensure atomic seat re-assignment without race conditions
        BookingExecutor.getInstance().getTrainLock(trainId).lock();
        try {
            while (availableToReassign > 0) {
                Optional<Booking> waitingOpt = bookingDAO.getOldestWaitingBookingForTrain(trainId);
                if (waitingOpt.isEmpty()) {
                    // No more waiting passengers; release remaining seats to train inventory
                    try (Connection conn = DatabaseConnection.createNewConnection()) {
                        trainDAO.releaseSeats(conn, trainId, availableToReassign);
                    } catch (SQLException e) {
                        System.err.println("Failed to release residual seats: " + e.getMessage());
                    }
                    break;
                }

                Booking waitingBooking = waitingOpt.get();
                List<Reservation> waitingPax = bookingDAO.getReservationsForBooking(waitingBooking.getId());
                int paxCount = waitingPax.size();

                if (availableToReassign >= paxCount) {
                    // Promote this waiting booking
                    boolean promoted = bookingDAO.promoteWaitingBooking(waitingBooking.getId(), trainId, waitingPax);
                    if (promoted) {
                        availableToReassign -= paxCount;
                        System.out.printf("⚡ [AUTO-PROMOTION WORKER] Successfully PROMOTED waiting ticket PNR %s on Train #%d (%d seat(s) assigned)!\n",
                                waitingBooking.getBookingReference(), trainId, paxCount);
                    } else {
                        break;
                    }
                } else {
                    // Not enough seats to accommodate entire family/group in this waiting booking; release to inventory
                    try (Connection conn = DatabaseConnection.createNewConnection()) {
                        trainDAO.releaseSeats(conn, trainId, availableToReassign);
                    } catch (SQLException e) {
                        System.err.println("Failed to release seats: " + e.getMessage());
                    }
                    break;
                }
            }
        } finally {
            BookingExecutor.getInstance().getTrainLock(trainId).unlock();
        }
    }

    public void stop() {
        running.set(false);
        if (workerThread != null) {
            workerThread.interrupt();
        }
    }
}
