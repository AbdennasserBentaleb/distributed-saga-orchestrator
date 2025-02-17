package com.saga.car.messaging;

import com.saga.common.enums.CommandType;
import com.saga.common.enums.EventStatus;
import com.saga.common.enums.ServiceType;
import com.saga.common.events.SagaCommand;
import com.saga.common.events.SagaEvent;
import com.saga.car.service.CarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CarCommandHandler {

    private final CarService carService;
    private final KafkaTemplate<String, SagaEvent> kafkaTemplate;

    private static final String SAGA_EVENTS_TOPIC = "saga-events";

    @KafkaListener(topics = "car-commands", groupId = "car-service-group")
    public void handleCarCommand(SagaCommand command) {
        log.info("Received Car Command: {}", command);

        SagaEvent event = SagaEvent.builder()
                .sagaId(command.getSagaId())
                .serviceType(ServiceType.CAR)
                .build();

        try {
            if (command.getCommandType() == CommandType.BOOK) {
                carService.bookCar(command.getSagaId(), command.getCustomerId(), command.getCarDetails());
                event.setEventStatus(EventStatus.SUCCESS);
                event.setMessage("Car booked successfully");
            } else if (command.getCommandType() == CommandType.CANCEL) {
                carService.cancelCar(command.getSagaId());
                event.setEventStatus(EventStatus.SUCCESS);
                event.setMessage("Car canceled successfully");
            }
        } catch (Exception e) {
            log.error("Error processing car command for sagaId: {}", command.getSagaId(), e);
            event.setEventStatus(EventStatus.FAILED);
            event.setMessage("Car operation failed: " + e.getMessage());
        }

        // Publish event back to orchestrator
        kafkaTemplate.send(SAGA_EVENTS_TOPIC, event.getSagaId().toString(), event);
        log.info("Published SagaEvent: {}", event);
    }
}
