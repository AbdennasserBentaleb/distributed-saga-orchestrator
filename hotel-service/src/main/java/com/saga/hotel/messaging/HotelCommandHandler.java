package com.saga.hotel.messaging;

import com.saga.common.enums.CommandType;
import com.saga.common.enums.EventStatus;
import com.saga.common.enums.ServiceType;
import com.saga.common.events.SagaCommand;
import com.saga.common.events.SagaEvent;
import com.saga.hotel.entity.OutboxEvent;
import com.saga.hotel.repository.OutboxEventRepository;
import com.saga.hotel.service.HotelService;
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
public class HotelCommandHandler {

    private final HotelService hotelService;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    private static final String SAGA_EVENTS_TOPIC = "saga-events";

    @RetryableTopic(
            attempts = "4",
            backoff = @org.springframework.retry.annotation.Backoff(delay = 1000, multiplier = 2.0),
            exclude = {IllegalArgumentException.class, NullPointerException.class}
    )
    @com.saga.common.annotations.DistributedLock(keyPrefix = "hotel", keyExpression = "#command.sagaId")
    @Transactional
    @KafkaListener(topics = "hotel-commands", groupId = "hotel-service-group")
    public void handleHotelCommand(SagaCommand command) throws Exception {
        log.info("Received Hotel Command: {}", command);

        SagaEvent event = SagaEvent.builder()
                .eventId(UUID.randomUUID())
                .sagaId(command.getSagaId())
                .serviceType(ServiceType.HOTEL)
                .build();

        if (command.getCommandType() == CommandType.BOOK) {
            hotelService.bookHotel(command.getSagaId(), command.getCustomerId(), command.getHotelDetails());
            event.setEventStatus(EventStatus.SUCCESS);
            event.setMessage("Hotel booked successfully");
        } else if (command.getCommandType() == CommandType.CANCEL) {
            hotelService.cancelHotel(command.getSagaId());
            event.setEventStatus(EventStatus.SUCCESS);
            event.setMessage("Hotel canceled successfully");
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
        log.error("Hotel Command permanently failed after retries for sagaId: {}. Reason: {}", command.getSagaId(), exceptionMessage);

        SagaEvent event = SagaEvent.builder()
                .eventId(UUID.randomUUID())
                .sagaId(command.getSagaId())
                .serviceType(ServiceType.HOTEL)
                .eventStatus(EventStatus.FAILED)
                .message("Hotel operation failed after retries: " + exceptionMessage)
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
