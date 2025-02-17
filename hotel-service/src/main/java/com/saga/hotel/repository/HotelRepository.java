package com.saga.hotel.repository;

import com.saga.hotel.entity.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, Long> {
    Optional<Hotel> findBySagaId(UUID sagaId);
}
