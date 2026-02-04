package com.saga.flight.messaging;

import com.saga.common.enums.CommandType;
import com.saga.common.enums.EventStatus;
import com.saga.common.enums.ServiceType;
import com.saga.common.events.SagaCommand;
import com.saga.common.events.SagaEvent;
import com.saga.flight.service.FlightService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FlightCommandHandler {

    private final FlightService flightService;
    private final KafkaTemplate<String, SagaEvent> kafkaTemplate;
    
    private static final String SAGA_EVENTS_TOPIC = "saga-events";

    @KafkaListener(topics = "flight-commands", groupId = "flight-service-group")
    public void handleFlightCommand(SagaCommand command) {
        log.info("Received Flight Command: {}", command);

        SagaEvent event = SagaEvent.builder()
                .sagaId(command.getSagaId())
                .serviceType(ServiceType.FLIGHT)
                .build();

        try {
            if (command.getCommandType() == CommandType.BOOK) {
                flightService.bookFlight(command.getSagaId(), command.getCustomerId(), command.getFlightDetails());
                event.setEventStatus(EventStatus.SUCCESS);
                event.setMessage("Flight booked successfully");
            } else if (command.getCommandType() == CommandType.CANCEL) {
                flightService.cancelFlight(command.getSagaId());
                event.setEventStatus(EventStatus.SUCCESS);
                event.setMessage("Flight canceled successfully");
            }
        } catch (Exception e) {
            log.error("Error processing flight command for sagaId: {}", command.getSagaId(), e);
            event.setEventStatus(EventStatus.FAILED);
            event.setMessage("Flight operation failed: " + e.getMessage());
        }

        // Publish event back to orchestrator
        kafkaTemplate.send(SAGA_EVENTS_TOPIC, event.getSagaId().toString(), event);
        log.info("Published SagaEvent: {}", event);
    }
}
