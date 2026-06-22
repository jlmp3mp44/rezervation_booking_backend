package com.horovod.hub.service;

import com.horovod.hub.config.HorovodProperties;
import com.horovod.hub.domain.Booking;
import com.horovod.hub.exception.BookingValidationException;
import com.horovod.hub.repository.BookingRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingValidationServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    private BookingValidationService validationService;

    @BeforeEach
    void setUp() {
        HorovodProperties properties = new HorovodProperties();
        properties.setMaxConcurrentPerSlot(2);
        validationService = new BookingValidationService(properties, bookingRepository);
        when(bookingRepository.findAll()).thenReturn(List.of());
    }

    @Test
    void residentCannotBookToday() {
        Booking booking = sampleBooking(LocalDate.now());
        assertThrows(BookingValidationException.class, () -> validationService.validateBooking(booking, false));
    }

    @Test
    void residentCanBookTomorrow() {
        Booking booking = sampleBooking(LocalDate.now().plusDays(1));
        assertDoesNotThrow(() -> validationService.validateBooking(booking, false));
    }

    @Test
    void detectsExclusiveType() {
        org.junit.jupiter.api.Assertions.assertTrue(validationService.isExclusiveType("репетиція"));
        org.junit.jupiter.api.Assertions.assertFalse(validationService.isExclusiveType("чилл"));
    }

    private Booking sampleBooking(LocalDate date) {
        Booking booking = new Booking();
        booking.setId("book_test");
        booking.setUserName("Test User");
        booking.setEmail("test@example.com");
        booking.setDate(date.format(DateTimeFormatter.ISO_LOCAL_DATE));
        booking.setStartTime("10:00");
        booking.setEndTime("11:00");
        booking.setType("чилл");
        booking.setStatus("pending");
        return booking;
    }
}
