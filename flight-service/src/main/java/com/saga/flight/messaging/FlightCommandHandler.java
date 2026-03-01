package com.saga.flight.messaging;

import com.saga.common.enums.CommandType;
import com.saga.common.enums.EventStatus;
import com.saga.common.enums.ServiceType;
import com.saga.common.events.SagaCommand;
import com.saga.common.events.SagaEvent;
import com.saga.flight.entity.OutboxEvent;
import com.saga.flight.repository.OutboxEventRepository;
import com.saga.flight.service.FlightService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class FlightCommandHandler {

    private final FlightService flightService;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    
    private static final String SAGA_EVENTS_TOPIC = "saga-events";

    @RetryableTopic(
            attempts = "4",
            backoff = @org.springframework.retry.annotation.Backoff(delay = 1000, multiplier = 2.0),
            exclude = {IllegalArgumentException.class, NullPointerException.class}
    )
    @com.saga.common.annotations.DistributedLock(keyPrefix = "flight", keyExpression = "#command.sagaId")
    @Transactional
    @KafkaListener(topics = "flight-commands", groupId = "flight-service-group")
    public void handleFlightCommand(SagaCommand command) throws Exception {
        log.info("Received Flight Command: {}", command);

        SagaEvent event = SagaEvent.builder()
                .eventId(UUID.randomUUID())
                .sagaId(command.getSagaId())
                .serviceType(ServiceType.FLIGHT)
                .build();

        if (command.getCommandType() == CommandType.BOOK) {
            flightService.bookFlight(command.getSagaId(), command.getCustomerId(), command.getFlightDetails());
            event.setEventStatus(EventStatus.SUCCESS);
            event.setMessage("Flight booked successfully");
        } else if (command.getCommandType() == CommandType.CANCEL) {
            flightService.cancelFlight(command.getSagaId());
            event.setEventStatus(EventStatus.SUCCESS);
            event.setMessage("Flight canceled successfully");
        }

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .id(event.getEventId())
                .sagaId(event.getSagaId())
                .topic(SAGA_EVENTS_TOPIC)
                .eventPayload(objectMapper.writeValueAsString(event))
                .build();
        outboxEventRepository.save(outboxEvent);
        log.info("Saved SagaEvent to Outbox: {}", event);
    }

    @DltHandler
    @Transactional
    public void handleDlt(SagaCommand command, @Header(KafkaHeaders.EXCEPTION_MESSAGE) String exceptionMessage) throws Exception {
        log.error("Flight Command permanently failed after retries for sagaId: {}. Reason: {}", command.getSagaId(), exceptionMessage);

        SagaEvent event = SagaEvent.builder()
                .eventId(UUID.randomUUID())
                .sagaId(command.getSagaId())
                .serviceType(ServiceType.FLIGHT)
                .eventStatus(EventStatus.FAILED)
                .message("Flight operation failed after retries: " + exceptionMessage)
                .build();

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .id(event.getEventId())
                .sagaId(event.getSagaId())
                .topic(SAGA_EVENTS_TOPIC)
                .eventPayload(objectMapper.writeValueAsString(event))
                .build();
        outboxEventRepository.save(outboxEvent);
        log.info("Saved FAILED SagaEvent to Outbox from DLQ handler: {}", event);
    }
}
