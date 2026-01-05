package com.saga.common.events;

import com.saga.common.enums.CommandType;
import com.saga.common.enums.ServiceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SagaCommand {
    private UUID eventId;
    private UUID sagaId;
    private String customerId;
    private ServiceType targetService;
    private CommandType commandType;
    
    // Details required for booking
    private String flightDetails;
    private String hotelDetails;
    private String carDetails;
}
