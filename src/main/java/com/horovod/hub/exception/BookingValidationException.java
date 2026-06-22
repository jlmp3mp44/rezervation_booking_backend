package com.horovod.hub.exception;

public class BookingValidationException extends RuntimeException {

    private final Object conflictBooking;

    public BookingValidationException(String message) {
        super(message);
        this.conflictBooking = null;
    }

    public BookingValidationException(String message, Object conflictBooking) {
        super(message);
        this.conflictBooking = conflictBooking;
    }

    public Object getConflictBooking() {
        return conflictBooking;
    }
}
