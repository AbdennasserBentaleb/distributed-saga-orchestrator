package com.saga.car.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "cars")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Car {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private UUID sagaId;

    private String customerId;
    private String carDetails;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    public enum BookingStatus {
        BOOKED, CANCELED
    }
}
