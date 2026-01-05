package com.saga.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequestDTO {
    private String customerId;
    private String flightDetails;
    private String hotelDetails;
    private String carDetails;
}
