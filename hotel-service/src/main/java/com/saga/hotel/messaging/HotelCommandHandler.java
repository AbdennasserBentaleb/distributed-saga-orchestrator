package com.saga.hotel.messaging;

import com.saga.common.enums.CommandType;
import com.saga.common.enums.EventStatus;
import com.saga.common.enums.ServiceType;
import com.saga.common.events.SagaCommand;
import com.saga.common.events.SagaEvent;
import com.saga.hotel.service.HotelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class HotelCommandHandler {

    private final HotelService hotelService;
    private final KafkaTemplate<String, SagaEvent> kafkaTemplate;

    private static final String SAGA_EVENTS_TOPIC = "saga-events";

    @KafkaListener(topics = "hotel-commands", groupId = "hotel-service-group")
    public void handleHotelCommand(SagaCommand command) {
        log.info("Received Hotel Command: {}", command);

        SagaEvent event = SagaEvent.builder()
                .sagaId(command.getSagaId())
                .serviceType(ServiceType.HOTEL)
                .build();

        try {
            if (command.getCommandType() == CommandType.BOOK) {
                hotelService.bookHotel(command.getSagaId(), command.getCustomerId(), command.getHotelDetails());
                event.setEventStatus(EventStatus.SUCCESS);
                event.setMessage("Hotel booked successfully");
            } else if (command.getCommandType() == CommandType.CANCEL) {
                hotelService.cancelHotel(command.getSagaId());
                event.setEventStatus(EventStatus.SUCCESS);
                event.setMessage("Hotel canceled successfully");
            }
        } catch (Exception e) {
            log.error("Error processing hotel command for sagaId: {}", command.getSagaId(), e);
            event.setEventStatus(EventStatus.FAILED);
            event.setMessage("Hotel operation failed: " + e.getMessage());
        }

        // Publish event back to orchestrator
        kafkaTemplate.send(SAGA_EVENTS_TOPIC, event.getSagaId().toString(), event);
        log.info("Published SagaEvent: {}", event);
    }
}
