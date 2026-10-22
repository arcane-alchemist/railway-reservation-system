package com.railway.concurrency;

import com.railway.model.Booking;
import com.railway.model.Reservation;
import com.railway.model.Train;
import com.railway.service.BookingService;
import com.railway.service.TrainService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class ConcurrencyTest {

    private static BookingService bookingService;
    private static TrainService trainService;

    @BeforeAll
    public static void setup() {
        bookingService = new BookingService();
        trainService = new TrainService();
    }

    @Test
    public void testConcurrentBookingThreadSafetyAndZeroOverbooking() throws InterruptedException, ExecutionException {
        // Choose train 2 (New Delhi Rajdhani)
        int trainId = 2;
        Train trainBefore = trainService.getTrainDetails(trainId).orElseThrow();
        int initialAvailable = trainBefore.getAvailableSeats();

        int concurrentThreads = 15;
        ExecutorService testExecutor = Executors.newFixedThreadPool(concurrentThreads);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<BookingExecutor.BookingResult>> futures = new ArrayList<>();

        for (int i = 0; i < concurrentThreads; i++) {
            final int index = i;
            futures.add(testExecutor.submit(() -> {
                startGate.await();
                List<Reservation> pax = List.of(new Reservation("Concurrent Pax " + index, 30, "Male", "LOWER"));
                return bookingService.bookTicketAsync(2, trainId, "2026-12-01", "AC_3_TIER", pax).get();
            }));
        }

        // Release all threads simultaneously
        startGate.countDown();

        AtomicInteger confirmedCount = new AtomicInteger(0);
        AtomicInteger waitingCount = new AtomicInteger(0);

        for (Future<BookingExecutor.BookingResult> f : futures) {
            BookingExecutor.BookingResult res = f.get();
            assertTrue(res.isSuccess());
            if ("CONFIRMED".equalsIgnoreCase(res.getBooking().getStatus())) {
                confirmedCount.incrementAndGet();
            } else if ("WAITING".equalsIgnoreCase(res.getBooking().getStatus())) {
                waitingCount.incrementAndGet();
            }
        }

        testExecutor.shutdown();
        assertTrue(testExecutor.awaitTermination(5, TimeUnit.SECONDS));

        Train trainAfter = trainService.getTrainDetails(trainId).orElseThrow();
        int expectedRemaining = Math.max(0, initialAvailable - confirmedCount.get());
        assertEquals(expectedRemaining, trainAfter.getAvailableSeats(),
                "Available seats must accurately match confirmed seat deductions with zero overselling");
    }
}
