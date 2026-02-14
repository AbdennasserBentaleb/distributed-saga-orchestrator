package com.saga.car.repository;

import com.saga.car.entity.Car;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CarRepository extends JpaRepository<Car, Long> {
    Optional<Car> findBySagaId(UUID sagaId);
}
