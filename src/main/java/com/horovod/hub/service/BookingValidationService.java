package com.horovod.hub.service;

import com.horovod.hub.config.HorovodProperties;
import com.horovod.hub.domain.Booking;
import com.horovod.hub.exception.BookingValidationException;
import com.horovod.hub.repository.BookingRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class BookingValidationService {

    private static final Set<String> ACTIVE_STATUSES = Set.of("pending", "approved");
    private static final Set<String> EXCLUSIVE_TYPES = Set.of("репетиція", "заниматися самому");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final HorovodProperties properties;
    private final BookingRepository bookingRepository;

    public BookingValidationService(HorovodProperties properties, BookingRepository bookingRepository) {
        this.properties = properties;
        this.bookingRepository = bookingRepository;
    }

    public boolean isExclusiveType(String type) {
        return type != null && EXCLUSIVE_TYPES.contains(type.toLowerCase());
    }

    public void validateBooking(Booking booking, boolean isAdminSubmit) {
        validateRequiredFields(booking);
        validateTimeRange(booking);
        validateDateRules(booking, isAdminSubmit);

        List<Booking> all = bookingRepository.findAll();
        if (isExclusiveType(booking.getType())) {
            validateExclusiveBooking(booking, all, isAdminSubmit);
        }
    }

    public void validateStatusTransition(Booking booking) {
        if (booking.getStatus() == null) {
            throw new BookingValidationException("Статус бронювання обов'язковий.");
        }
        Set<String> allowed = Set.of("pending", "approved", "rejected", "cancelled");
        if (!allowed.contains(booking.getStatus())) {
            throw new BookingValidationException("Недопустимий статус бронювання.");
        }
    }

    public boolean isBookingInPast(Booking booking) {
        LocalDate today = LocalDate.now();
        LocalDate bookingDate = LocalDate.parse(booking.getDate());
        if (bookingDate.isBefore(today)) {
            return true;
        }
        if (bookingDate.isAfter(today)) {
            return false;
        }
        double endDecimal = timeToDecimal(booking.getEndTime());
        double nowDecimal = LocalTime.now().getHour() + LocalTime.now().getMinute() / 60.0;
        return endDecimal <= nowDecimal;
    }

    private void validateRequiredFields(Booking booking) {
        if (isBlank(booking.getUserName()) || isBlank(booking.getEmail()) || isBlank(booking.getDate())
                || isBlank(booking.getStartTime()) || isBlank(booking.getEndTime()) || isBlank(booking.getType())) {
            throw new BookingValidationException("Заповніть усі обов'язкові поля бронювання.");
        }
    }

    private void validateTimeRange(Booking booking) {
        double start = timeToDecimal(booking.getStartTime());
        double end = timeToDecimal(booking.getEndTime());
        if (start < properties.getCalendarStartHour()) {
            throw new BookingValidationException("Бронювання раніше 08:00 не допускається.");
        }
        if (end <= start) {
            throw new BookingValidationException("Час завершення має бути пізніше часу початку.");
        }
        if (end > properties.getCalendarEndHour()) {
            throw new BookingValidationException("Бронювання після 24:00 не допускається.");
        }
    }

    private void validateDateRules(Booking booking, boolean isAdminSubmit) {
        LocalDate today = LocalDate.now();
        LocalDate bookingDate = LocalDate.parse(booking.getDate());

        if (bookingDate.isBefore(today)) {
            throw new BookingValidationException("Бронювання в минулому неможливе.");
        }

        if (isAdminSubmit) {
            if (bookingDate.isEqual(today)) {
                double start = timeToDecimal(booking.getStartTime());
                double now = LocalTime.now().getHour() + LocalTime.now().getMinute() / 60.0;
                if (start < now) {
                    throw new BookingValidationException("Адмін: не можна забронювати час, який уже розпочався або минув.");
                }
            }
        } else if (!bookingDate.isAfter(today)) {
            throw new BookingValidationException("Бронювання на сьогодні неможливе. Тільки з завтрашнього дня.");
        }
    }

    private void validateExclusiveBooking(Booking booking, List<Booking> all, boolean isAdminSubmit) {
        double startDec = timeToDecimal(booking.getStartTime());
        double endDec = timeToDecimal(booking.getEndTime());

        for (double slot = startDec; slot < endDec; slot += 0.5) {
            double finalSlot = slot;
            List<Booking> slotBookings = all.stream()
                    .filter(b -> ACTIVE_STATUSES.contains(b.getStatus()))
                    .filter(b -> booking.getDate().equals(b.getDate()))
                    .filter(b -> !b.getId().equals(booking.getId()))
                    .filter(b -> isExclusiveType(b.getType()))
                    .filter(b -> finalSlot >= timeToDecimal(b.getStartTime()) && finalSlot < timeToDecimal(b.getEndTime()))
                    .toList();

            if (slotBookings.size() >= properties.getMaxConcurrentPerSlot()) {
                throw new BookingValidationException(
                        "Час уже зайнятий (максимум " + properties.getMaxConcurrentPerSlot() + " одночасних бронювань).",
                        slotBookings.getFirst()
                );
            }
        }

        if (!isAdminSubmit) {
            Booking conflict = findExclusiveOverlap(booking.getDate(), booking.getStartTime(), booking.getEndTime(), booking.getId(), all);
            if (conflict != null) {
                throw new BookingValidationException("У цей час уже запланована ексклюзивна активність.", conflict);
            }
        }
    }

    public Booking findExclusiveOverlap(String date, String start, String end, String ignoreId, List<Booking> all) {
        double startDec = timeToDecimal(start);
        double endDec = timeToDecimal(end);

        return all.stream()
                .filter(b -> "approved".equals(b.getStatus()))
                .filter(b -> date.equals(b.getDate()))
                .filter(b -> ignoreId == null || !ignoreId.equals(b.getId()))
                .filter(b -> isExclusiveType(b.getType()))
                .filter(b -> startDec < timeToDecimal(b.getEndTime()) && endDec > timeToDecimal(b.getStartTime()))
                .findFirst()
                .orElse(null);
    }

    public int getConcurrentBookingCount(String dayStr, double halfHour, String ignoreId, List<Booking> all) {
        return (int) all.stream()
                .filter(b -> ACTIVE_STATUSES.contains(b.getStatus()))
                .filter(b -> dayStr.equals(b.getDate()))
                .filter(b -> ignoreId == null || !ignoreId.equals(b.getId()))
                .filter(b -> halfHour >= timeToDecimal(b.getStartTime()) && halfHour < timeToDecimal(b.getEndTime()))
                .count();
    }

    private double timeToDecimal(String timeStr) {
        LocalTime time = LocalTime.parse(timeStr, TIME_FORMAT);
        return time.getHour() + time.getMinute() / 60.0;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
