package com.saga.car.service;

import com.saga.car.entity.Car;
import com.saga.car.repository.CarRepository;
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
class CarServiceTest {

    @Mock
    private CarRepository carRepository;

    @InjectMocks
    private CarService carService;

    @Test
    void bookCar_Idempotent() {
        UUID sagaId = UUID.randomUUID();
        when(carRepository.findBySagaId(sagaId)).thenReturn(Optional.of(Car.builder().build()));

        carService.bookCar(sagaId, "CUST-1", "CAR-XYZ");

        verify(carRepository, never()).save(any(Car.class));
    }

    @Test
    void cancelCar_Successfully() {
        UUID sagaId = UUID.randomUUID();
        Car car = Car.builder().status(Car.BookingStatus.BOOKED).build();
        when(carRepository.findBySagaId(sagaId)).thenReturn(Optional.of(car));

        carService.cancelCar(sagaId);

        assertEquals(Car.BookingStatus.CANCELED, car.getStatus());
        verify(carRepository).save(car);
    }
}
