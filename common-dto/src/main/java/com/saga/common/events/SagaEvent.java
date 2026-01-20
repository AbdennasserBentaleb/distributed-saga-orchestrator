package com.saga.common.events;

import com.saga.common.enums.EventStatus;
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
public class SagaEvent {
    private UUID sagaId;
    private ServiceType serviceType;
    private EventStatus eventStatus;
    private String message;
}
