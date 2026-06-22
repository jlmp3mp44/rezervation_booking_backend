package com.horovod.hub.dto;

import com.horovod.hub.domain.Booking;

public record BookingConflictResponse(
        String message,
        Booking conflictingBooking
) {
}
