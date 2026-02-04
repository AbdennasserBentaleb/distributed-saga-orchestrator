package com.saga.flight.repository;

import com.saga.flight.entity.Flight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FlightRepository extends JpaRepository<Flight, Long> {
    Optional<Flight> findBySagaId(UUID sagaId);
}
