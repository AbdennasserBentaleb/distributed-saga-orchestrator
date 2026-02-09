package com.saga.hotel.service;

import com.saga.hotel.entity.Hotel;
import com.saga.hotel.repository.HotelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class HotelService {

    private final HotelRepository hotelRepository;

    @Transactional
    public void bookHotel(UUID sagaId, String customerId, String hotelDetails) {
        log.info("Attempting to book hotel for sagaId: {}", sagaId);

        Optional<Hotel> existingHotel = hotelRepository.findBySagaId(sagaId);
        if (existingHotel.isPresent()) {
            log.info("Hotel already booked for sagaId: {} (Idempotent request)", sagaId);
            return;
        }

        Hotel hotel = Hotel.builder()
                .sagaId(sagaId)
                .customerId(customerId)
                .hotelDetails(hotelDetails)
                .status(Hotel.BookingStatus.BOOKED)
                .build();

        hotelRepository.save(hotel);
        log.info("Successfully booked hotel for sagaId: {}", sagaId);
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
