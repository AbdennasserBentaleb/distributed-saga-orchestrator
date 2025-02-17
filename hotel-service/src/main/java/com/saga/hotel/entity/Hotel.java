package com.saga.hotel.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "hotels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hotel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private UUID sagaId;

    private String customerId;
    private String hotelDetails;
    
    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    public enum BookingStatus {
        BOOKED, CANCELED
    }
}
