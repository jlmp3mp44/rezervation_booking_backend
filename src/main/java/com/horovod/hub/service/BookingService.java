package com.horovod.hub.service;

import com.horovod.hub.config.HorovodProperties;
import com.horovod.hub.domain.Booking;
import com.horovod.hub.dto.RealtimeEvent;
import com.horovod.hub.repository.BookingRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingValidationService validationService;
    private final HorovodProperties properties;
    private final EventBroadcastService eventBroadcastService;

    public BookingService(
            BookingRepository bookingRepository,
            BookingValidationService validationService,
            HorovodProperties properties,
            EventBroadcastService eventBroadcastService
    ) {
        this.bookingRepository = bookingRepository;
        this.validationService = validationService;
        this.properties = properties;
        this.eventBroadcastService = eventBroadcastService;
    }

    public List<Booking> findAll() {
        autoExpirePendingBookings();
        return bookingRepository.findAll();
    }

    public Optional<Booking> findById(String id) {
        return bookingRepository.findById(id);
    }

    @Transactional
    public Booking save(Booking booking, boolean isAdminSubmit) {
        validationService.validateStatusTransition(booking);

        boolean isNew = bookingRepository.findById(booking.getId()).isEmpty();
        if (isNew || "pending".equals(booking.getStatus())) {
            validationService.validateBooking(booking, isAdminSubmit);
        }

        if (booking.getNotes() == null) {
            booking.setNotes("");
        }

        Booking saved = bookingRepository.save(booking);
        eventBroadcastService.broadcast(new RealtimeEvent(
                "bookings",
                isNew ? "INSERT" : "UPDATE",
                null,
                saved
        ));
        return saved;
    }

    @Transactional
    public List<Booking> upsertAll(List<Booking> bookings) {
        autoExpirePendingBookings();
        List<Booking> saved = bookingRepository.saveAll(bookings);
        eventBroadcastService.broadcast(new RealtimeEvent("bookings", "UPDATE", null, saved));
        return saved;
    }

    @Transactional
    public Booking upsert(Booking booking) {
        boolean isAdmin = properties.isAdminEmail(booking.getEmail());
        return save(booking, isAdmin);
    }

    @Transactional
    public void autoExpirePendingBookings() {
        List<Booking> expired = bookingRepository.findAll().stream()
                .filter(b -> "pending".equals(b.getStatus()))
                .filter(validationService::isBookingInPast)
                .toList();

        for (Booking booking : expired) {
            booking.setStatus("rejected");
            booking.setCancelReason("Термін дії запиту минув — слот вже в минулому.");
            bookingRepository.save(booking);
            eventBroadcastService.broadcast(new RealtimeEvent("bookings", "UPDATE", null, booking));
        }
    }
}
