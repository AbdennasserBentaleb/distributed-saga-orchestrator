package com.saga.flight.service;

import com.saga.flight.entity.Flight;
import com.saga.flight.repository.FlightRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlightServiceTest {

    @Mock
    private FlightRepository flightRepository;

    @InjectMocks
    private FlightService flightService;

    @Test
    void bookFlight_Successfully() {
        UUID sagaId = UUID.randomUUID();
        when(flightRepository.findBySagaId(sagaId)).thenReturn(Optional.empty());

        flightService.bookFlight(sagaId, "CUST-1", "FLIGHT-123");

        verify(flightRepository).save(any(Flight.class));
    }

    @Test
    void bookFlight_Idempotent() {
        UUID sagaId = UUID.randomUUID();
        when(flightRepository.findBySagaId(sagaId)).thenReturn(Optional.of(Flight.builder().build()));

        flightService.bookFlight(sagaId, "CUST-1", "FLIGHT-123");

        verify(flightRepository, never()).save(any(Flight.class));
    }

    @Test
    void cancelFlight_Successfully() {
        UUID sagaId = UUID.randomUUID();
        Flight flight = Flight.builder().status(Flight.BookingStatus.BOOKED).build();
        when(flightRepository.findBySagaId(sagaId)).thenReturn(Optional.of(flight));

        flightService.cancelFlight(sagaId);

        assertEquals(Flight.BookingStatus.CANCELED, flight.getStatus());
        verify(flightRepository).save(flight);
    }
}
