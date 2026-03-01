package com.saga.flight.service;

import com.saga.flight.entity.Flight;
import com.saga.flight.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlightService {

    private final FlightRepository flightRepository;

    @Transactional
    public void bookFlight(UUID sagaId, String customerId, String flightDetails) {
        log.info("Attempting to book flight for sagaId: {}", sagaId);
        
        Optional<Flight> existingFlight = flightRepository.findBySagaId(sagaId);
        if (existingFlight.isPresent()) {
            log.info("Flight already booked for sagaId: {} (Idempotent request)", sagaId);
            return;
        }

        Flight flight = Flight.builder()
                .sagaId(sagaId)
                .customerId(customerId)
                .flightDetails(flightDetails)
                .status(Flight.BookingStatus.BOOKED)
                .build();
                
        flightRepository.save(flight);
        log.info("Successfully booked flight for sagaId: {}", sagaId);
    }

    @Transactional
    public void cancelFlight(UUID sagaId) {
        log.info("Attempting to cancel flight for sagaId: {}", sagaId);
        
        flightRepository.findBySagaId(sagaId).ifPresentOrElse(flight -> {
            if (flight.getStatus() == Flight.BookingStatus.CANCELED) {
                log.info("Flight already canceled for sagaId: {} (Idempotent request)", sagaId);
                return;
            }
            flight.setStatus(Flight.BookingStatus.CANCELED);
            flightRepository.save(flight);
            log.info("Successfully canceled flight for sagaId: {}", sagaId);
        }, () -> log.warn("Flight not found for cancellation with sagaId: {}", sagaId));
    }
}
