package com.saga.hotel.service;

import com.saga.hotel.entity.Hotel;
import com.saga.hotel.repository.HotelRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class HotelService {

    private final HotelRepository hotelRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public void bookHotel(UUID sagaId, String customerId, String hotelDetails) {
        log.info("Attempting to book hotel for sagaId: {}", sagaId);

        Optional<Hotel> existingHotel = hotelRepository.findBySagaId(sagaId);
        if (existingHotel.isPresent()) {
            log.info("Hotel already booked for sagaId: {} (Idempotent request)", sagaId);
            return;
        }

        // Call external inventory system (Mocked)
        verifyHotelInventory(hotelDetails);

        Hotel hotel = Hotel.builder()
                .sagaId(sagaId)
                .customerId(customerId)
                .hotelDetails(hotelDetails)
                .status(Hotel.BookingStatus.BOOKED)
                .build();

        hotelRepository.save(hotel);
        log.info("Successfully booked hotel for sagaId: {}", sagaId);
    }

    @CircuitBreaker(name = "hotelInventory", fallbackMethod = "inventoryFallback")
    public void verifyHotelInventory(String hotelDetails) {
        log.info("Verifying hotel inventory with external provider...");
        // Mock external API call that might fail
        if (hotelDetails != null && hotelDetails.contains("FAIL_INVENTORY")) {
            throw new RuntimeException("External Inventory API 500 Internal Server Error");
        }
        // Success path
    }

    // Fallback must have the exact same signature plus a Throwable argument
    public void inventoryFallback(String hotelDetails, Throwable t) {
        log.error("Circuit breaker OPEN or fallback triggered for hotel inventory. Reason: {}", t.getMessage());
        throw new RuntimeException("Hotel Inventory System Unavailable. Failing fast.", t);
    }

    @Transactional
    public void cancelHotel(UUID sagaId) {
        log.info("Attempting to cancel hotel for sagaId: {}", sagaId);

        hotelRepository.findBySagaId(sagaId).ifPresentOrElse(hotel -> {
            if (hotel.getStatus() == Hotel.BookingStatus.CANCELED) {
                log.info("Hotel already canceled for sagaId: {} (Idempotent request)", sagaId);
                return;
            }
            hotel.setStatus(Hotel.BookingStatus.CANCELED);
            hotelRepository.save(hotel);
            log.info("Successfully canceled hotel for sagaId: {}", sagaId);
        }, () -> log.warn("Hotel not found for cancellation with sagaId: {}", sagaId));
    }
}
