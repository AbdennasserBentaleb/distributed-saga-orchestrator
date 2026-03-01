package com.saga.car.service;

import com.saga.car.entity.Car;
import com.saga.car.repository.CarRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CarService {

    private final CarRepository carRepository;
    private final Random random = new Random();

    @Transactional
    public void bookCar(UUID sagaId, String customerId, String carDetails) {
        log.info("Attempting to book car for sagaId: {}", sagaId);

        Optional<Car> existingCar = carRepository.findBySagaId(sagaId);
        if (existingCar.isPresent()) {
            log.info("Car already booked for sagaId: {} (Idempotent request)", sagaId);
            return;
        }

        // --- THE 20% RANDOM FAILURE INJECTION ---
        if (random.nextInt(100) < 20) {
            log.error("Simulated random car booking failure for sagaId: {}", sagaId);
            throw new RuntimeException("Simulated car booking failure for testing compensating transactions");
        }

        Car car = Car.builder()
                .sagaId(sagaId)
                .customerId(customerId)
                .carDetails(carDetails)
                .status(Car.BookingStatus.BOOKED)
                .build();

        carRepository.save(car);
        log.info("Successfully booked car for sagaId: {}", sagaId);
    }

    @Transactional
    public void cancelCar(UUID sagaId) {
        log.info("Attempting to cancel car for sagaId: {}", sagaId);

        carRepository.findBySagaId(sagaId).ifPresentOrElse(car -> {
            if (car.getStatus() == Car.BookingStatus.CANCELED) {
                log.info("Car already canceled for sagaId: {} (Idempotent request)", sagaId);
                return;
            }
            car.setStatus(Car.BookingStatus.CANCELED);
            carRepository.save(car);
            log.info("Successfully canceled car for sagaId: {}", sagaId);
        }, () -> log.warn("Car not found for cancellation with sagaId: {}", sagaId));
    }
}
