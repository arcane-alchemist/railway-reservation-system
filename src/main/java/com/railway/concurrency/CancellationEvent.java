package com.railway.concurrency;

/**
 * Event object dispatched when a booking is cancelled.
 * Consumed by WaitingListPromotionWorker for automatic seat reallocation.
 */
public class CancellationEvent {
    private final int trainId;
    private final int cancelledBookingId;
    private final int releasedSeatCount;
    private final long timestamp;

    public CancellationEvent(int trainId, int cancelledBookingId, int releasedSeatCount) {
        this.trainId = trainId;
        this.cancelledBookingId = cancelledBookingId;
        this.releasedSeatCount = releasedSeatCount;
        this.timestamp = System.currentTimeMillis();
    }

    public int getTrainId() {
        return trainId;
    }

    public int getCancelledBookingId() {
        return cancelledBookingId;
    }

    public int getReleasedSeatCount() {
        return releasedSeatCount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "CancellationEvent{" +
                "trainId=" + trainId +
                ", cancelledBookingId=" + cancelledBookingId +
                ", releasedSeatCount=" + releasedSeatCount +
                '}';
    }
}
