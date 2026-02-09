package com.saga.hotel.service;

import com.saga.hotel.entity.Hotel;
import com.saga.hotel.repository.HotelRepository;
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
class HotelServiceTest {

    @Mock
    private HotelRepository hotelRepository;

    @InjectMocks
    private HotelService hotelService;

    @Test
    void bookHotel_Successfully() {
        UUID sagaId = UUID.randomUUID();
        when(hotelRepository.findBySagaId(sagaId)).thenReturn(Optional.empty());

        hotelService.bookHotel(sagaId, "CUST-1", "HOTEL-ABC");

        verify(hotelRepository).save(any(Hotel.class));
    }

    @Test
    void cancelHotel_Successfully() {
        UUID sagaId = UUID.randomUUID();
        Hotel hotel = Hotel.builder().status(Hotel.BookingStatus.BOOKED).build();
        when(hotelRepository.findBySagaId(sagaId)).thenReturn(Optional.of(hotel));

        hotelService.cancelHotel(sagaId);

        assertEquals(Hotel.BookingStatus.CANCELED, hotel.getStatus());
        verify(hotelRepository).save(hotel);
    }
}
