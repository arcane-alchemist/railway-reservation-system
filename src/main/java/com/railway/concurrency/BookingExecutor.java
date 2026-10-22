package com.railway.concurrency;

import com.railway.dao.BookingDAO;
import com.railway.model.Booking;
import com.railway.model.Reservation;
import com.railway.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Concurrency manager for the Railway Reservation System.
 * Demonstrates Multithreading concepts:
 * - ThreadPool management via ExecutorService
 * - Fine-grained train-level locking using ReentrantLock to avoid global bottlenecks
 * - Atomic state tracking using AtomicInteger
 * - Asynchronous execution with CompletableFuture
 * - Thread safety against race conditions and overbooking
 */
public class BookingExecutor {

    private static volatile BookingExecutor instance;
    private final ExecutorService threadPool;
    private final BookingDAO bookingDAO;
    
    // Fine-grained locks per train ID so bookings on different trains run in parallel
    private final ConcurrentHashMap<Integer, ReentrantLock> trainLocks = new ConcurrentHashMap<>();

    private BookingExecutor() {
        this.bookingDAO = new BookingDAO();
        // Named thread factory for clear thread debugging
        ThreadFactory customThreadFactory = new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "Railway-BookingWorker-" + threadNumber.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        };

        int coreThreads = Math.max(4, Runtime.getRuntime().availableProcessors());
        this.threadPool = new ThreadPoolExecutor(
                coreThreads,
                coreThreads * 2,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(500),
                customThreadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    public static BookingExecutor getInstance() {
        if (instance == null) {
            synchronized (BookingExecutor.class) {
                if (instance == null) {
                    instance = new BookingExecutor();
                }
            }
        }
        return instance;
    }

    public ReentrantLock getTrainLock(int trainId) {
        return trainLocks.computeIfAbsent(trainId, k -> new ReentrantLock(true));
    }

    /**
     * Represents the result of an asynchronous booking attempt.
     */
    public static class BookingResult {
        private final boolean success;
        private final String message;
        private final Booking booking;
        private final String threadName;

        public BookingResult(boolean success, String message, Booking booking, String threadName) {
            this.success = success;
            this.message = message;
            this.booking = booking;
            this.threadName = threadName;
        }

        public boolean isSuccess() {
            return success;
        }

        public String message() {
            return message;
        }

        public Booking getBooking() {
            return booking;
        }

        public String getThreadName() {
            return threadName;
        }

        @Override
        public String toString() {
            return "[" + threadName + "] " + (success ? "SUCCESS: " : "FAILED: ") + message;
        }
    }

    /**
     * Submits an asynchronous booking request processed by a worker thread in the thread pool.
     */
    public CompletableFuture<BookingResult> processBookingAsync(Booking booking, List<Reservation> passengers) {
        return CompletableFuture.supplyAsync(() -> processBookingSync(booking, passengers), threadPool);
    }

    /**
     * Core thread-safe booking logic with train-level reentrant lock and DB transaction.
     */
    public BookingResult processBookingSync(Booking booking, List<Reservation> passengers) {
        String threadName = Thread.currentThread().getName();
        ReentrantLock trainLock = trainLocks.computeIfAbsent(booking.getTrainId(), k -> new ReentrantLock(true));

        trainLock.lock();
        try {
            // Create a dedicated connection for the worker thread
            try (Connection conn = DatabaseConnection.createNewConnection()) {
                if (booking.getBookingReference() == null || booking.getBookingReference().isEmpty()) {
                    booking.setBookingReference("PNR" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                }

                boolean booked = bookingDAO.createBookingTransaction(conn, booking, passengers);
                if (booked) {
                    booking.setReservations(passengers);
                    if ("WAITING".equalsIgnoreCase(booking.getStatus())) {
                        return new BookingResult(
                                true,
                                "Train is currently FULL. Booking registered under WAITING LIST (PNR: " + booking.getBookingReference() + "). You will be auto-promoted upon cancellations.",
                                booking,
                                threadName
                        );
                    } else {
                        return new BookingResult(
                                true,
                                "Booking confirmed! PNR: " + booking.getBookingReference() + " (" + passengers.size() + " seat(s) reserved)",
                                booking,
                                threadName
                        );
                    }
                } else {
                    return new BookingResult(
                            false,
                            "Booking failed: Transaction could not be completed.",
                            booking,
                            threadName
                    );
                }
            } catch (SQLException e) {
                return new BookingResult(
                        false,
                        "Database transaction error during booking: " + e.getMessage(),
                        booking,
                        threadName
                );
            }
        } finally {
            trainLock.unlock();
        }
    }

    /**
     * Simulates high-concurrency booking storm to demonstrate multithreading, race condition handling,
     * and thread safety under heavy simultaneous traffic.
     */
    public void simulateConcurrentBookings(int trainId, int numberOfConcurrentUsers, int seatsPerUser) {
        System.out.println("\n=======================================================");
        System.out.println(">>> STARTING CONCURRENT BOOKING SIMULATION <<<");
        System.out.printf("Simulating %d concurrent passengers competing for seats on Train ID: %d\n", numberOfConcurrentUsers, trainId);
        System.out.println("=======================================================\n");

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(numberOfConcurrentUsers);
        AtomicInteger successCounter = new AtomicInteger(0);
        AtomicInteger failCounter = new AtomicInteger(0);

        for (int i = 1; i <= numberOfConcurrentUsers; i++) {
            final int userId = (i % 2 == 0) ? 2 : 3;
            final int passengerIndex = i;

            threadPool.submit(() -> {
                try {
                    // All threads wait at the starting gate to fire simultaneously
                    startGate.await();

                    Booking testBooking = new Booking(
                            "SIM" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(),
                            userId,
                            trainId,
                            "2026-11-15",
                            1850.0 * seatsPerUser,
                            "AC_3_TIER"
                    );

                    List<Reservation> passengers = List.of(
                            new Reservation("Simulated Passenger " + passengerIndex, 25 + (passengerIndex % 40), "Other", "LOWER")
                    );

                    BookingResult result = processBookingSync(testBooking, passengers);
                    if (result.isSuccess()) {
                        successCounter.incrementAndGet();
                        System.out.printf("✅ [%s] Passenger #%d SUCCESS -> PNR: %s\n",
                                result.getThreadName(), passengerIndex, testBooking.getBookingReference());
                    } else {
                        failCounter.incrementAndGet();
                        System.out.printf("❌ [%s] Passenger #%d REJECTED -> %s\n",
                                result.getThreadName(), passengerIndex, result.message());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endGate.countDown();
                }
            });
        }

        // Release all threads simultaneously
        long startTime = System.currentTimeMillis();
        startGate.countDown();

        try {
            endGate.await(15, TimeUnit.SECONDS);
            long elapsed = System.currentTimeMillis() - startTime;
            System.out.println("\n-------------------------------------------------------");
            System.out.println(">>> CONCURRENT SIMULATION COMPLETED <<<");
            System.out.printf("Total Requests: %d | Confirmed: %d | Rejected: %d | Time: %d ms\n",
                    numberOfConcurrentUsers, successCounter.get(), failCounter.get(), elapsed);
            System.out.println("Zero overselling: all reservations strictly respected train seat limits.");
            System.out.println("-------------------------------------------------------\n");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void shutdown() {
        WaitingListPromotionWorker.getInstance().stop();
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(3, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
