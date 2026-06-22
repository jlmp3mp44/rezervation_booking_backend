package com.horovod.hub.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.horovod.hub.config.HorovodProperties;
import com.horovod.hub.domain.Booking;
import com.horovod.hub.service.BookingService;

import java.io.IOException;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final HorovodProperties properties;
    private final ObjectMapper objectMapper;

    public BookingController(BookingService bookingService, HorovodProperties properties, ObjectMapper objectMapper) {
        this.bookingService = bookingService;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public List<Booking> findAll() {
        return bookingService.findAll();
    }

    @PostMapping
    public Booking create(@RequestBody Booking booking) {
        boolean isAdmin = properties.isAdminEmail(booking.getEmail());
        return bookingService.save(booking, isAdmin);
    }

    @PutMapping("/{id}")
    public ResponseEntity<List<Booking>> update(@PathVariable String id, @RequestBody Booking booking) {
        booking.setId(id);
        boolean isAdmin = properties.isAdminEmail(booking.getEmail());
        Booking saved = bookingService.save(booking, isAdmin);
        return ResponseEntity.ok(List.of(saved));
    }

    @PostMapping("/upsert")
    public ResponseEntity<?> upsert(@RequestBody JsonNode body) throws com.fasterxml.jackson.core.JsonProcessingException {
        if (body.isArray()) {
            List<Booking> bookings = null;
            try {
                bookings = objectMapper.readerForListOf(Booking.class).readValue(body);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return ResponseEntity.ok(bookingService.upsertAll(bookings));
        }
        Booking booking = objectMapper.treeToValue(body, Booking.class);
        return ResponseEntity.ok(bookingService.upsert(booking));
    }
}
