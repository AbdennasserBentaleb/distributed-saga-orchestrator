package com.saga.orchestrator.service;

import com.saga.common.dto.BookingRequestDTO;
import com.saga.common.enums.CommandType;
import com.saga.common.enums.EventStatus;
import com.saga.common.enums.SagaStatus;
import com.saga.common.enums.ServiceType;
import com.saga.common.events.SagaCommand;
import com.saga.common.events.SagaEvent;
import com.saga.orchestrator.entity.SagaState;
import com.saga.orchestrator.repository.SagaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SagaOrchestratorService {

    private final SagaRepository sagaRepository;
    private final KafkaTemplate<String, SagaCommand> kafkaTemplate;

    @Transactional
    public UUID startSaga(BookingRequestDTO request) {
        log.info("Starting saga for customer: {}", request.getCustomerId());

        SagaState sagaState = SagaState.builder()
                .customerId(request.getCustomerId())
                .flightDetails(request.getFlightDetails())
                .hotelDetails(request.getHotelDetails())
                .carDetails(request.getCarDetails())
                .status(SagaStatus.PENDING)
                .build();

        SagaState savedSaga = sagaRepository.save(sagaState);
        UUID sagaId = savedSaga.getId();

        sendCommand(sagaId, request.getCustomerId(), ServiceType.FLIGHT, CommandType.BOOK, request.getFlightDetails(),
                null, null);
        return sagaId;
    }

    @Transactional
    public void handleSagaEvent(SagaEvent event) {
        log.info("Received event for sagaId: {}, service: {}, status: {}", event.getSagaId(), event.getServiceType(),
                event.getEventStatus());

        SagaState sagaState = sagaRepository.findById(event.getSagaId())
                .orElseThrow(() -> new RuntimeException("Saga not found for ID: " + event.getSagaId()));

        if (event.getEventStatus() == EventStatus.SUCCESS) {
            handleSuccessEvent(sagaState, event.getServiceType());
        } else {
            handleFailureEvent(sagaState, event.getServiceType(), event.getMessage());
        }
    }

    private void handleSuccessEvent(SagaState sagaState, ServiceType serviceType) {
        switch (serviceType) {
            case FLIGHT:
                // A SUCCESS reply from FLIGHT during a CANCELLING_FLIGHT status means the compensation is fully done.
                if (sagaState.getStatus() == SagaStatus.CANCELLING_FLIGHT) {
                    sagaState.setStatus(SagaStatus.COMPENSATED);
                    sagaRepository.saveAndFlush(sagaState);
                    log.info("Saga COMPENSATED entirely for sagaId: {}", sagaState.getId());
                } else {
                    sagaState.setStatus(SagaStatus.FLIGHT_BOOKED);
                    sagaRepository.saveAndFlush(sagaState);
                    sendCommand(sagaState.getId(), sagaState.getCustomerId(), ServiceType.HOTEL, CommandType.BOOK, null,
                            sagaState.getHotelDetails(), null);
                }
                break;
            case HOTEL:
                if (sagaState.getStatus() == SagaStatus.CANCELLING_HOTEL) {
                    sagaState.setStatus(SagaStatus.CANCELLING_FLIGHT);
                    sagaRepository.saveAndFlush(sagaState);
                    sendCommand(sagaState.getId(), sagaState.getCustomerId(), ServiceType.FLIGHT, CommandType.CANCEL,
                            null, null, null);
                } else {
                    sagaState.setStatus(SagaStatus.HOTEL_BOOKED);
                    sagaRepository.saveAndFlush(sagaState);
                    sendCommand(sagaState.getId(), sagaState.getCustomerId(), ServiceType.CAR, CommandType.BOOK, null,
                            null, sagaState.getCarDetails());
                }
                break;
            case CAR:
                sagaState.setStatus(SagaStatus.COMPLETED);
                sagaRepository.saveAndFlush(sagaState);
                log.info("Saga COMPLETED successfully for sagaId: {}", sagaState.getId());
                break;
        }
    }

    private void handleFailureEvent(SagaState sagaState, ServiceType serviceType, String reason) {
        log.error("Saga Failed at service: {}, reason: {}", serviceType, reason);
        switch (serviceType) {
            case FLIGHT:
                sagaState.setStatus(SagaStatus.COMPENSATED);
                sagaRepository.saveAndFlush(sagaState);
                log.info("Failed on Flight. Nothing to compensate. Saga marked as COMPENSATED.");
                break;
            case HOTEL:
                sagaState.setStatus(SagaStatus.CANCELLING_FLIGHT);
                sagaRepository.saveAndFlush(sagaState);
                sendCommand(sagaState.getId(), sagaState.getCustomerId(), ServiceType.FLIGHT, CommandType.CANCEL, null,
                        null, null);
                break;
            case CAR:
                sagaState.setStatus(SagaStatus.CANCELLING_HOTEL);
                sagaRepository.saveAndFlush(sagaState);
                sendCommand(sagaState.getId(), sagaState.getCustomerId(), ServiceType.HOTEL, CommandType.CANCEL, null,
                        null, null);
                break;
        }
    }

    private void sendCommand(UUID sagaId, String customer, ServiceType targetService, CommandType commandType,
            String flight, String hotel, String car) {
        SagaCommand command = SagaCommand.builder()
                .sagaId(sagaId)
                .customerId(customer)
                .targetService(targetService)
                .commandType(commandType)
                .flightDetails(flight)
                .hotelDetails(hotel)
                .carDetails(car)
                .build();

        String topic = targetService.name().toLowerCase() + "-commands";
        kafkaTemplate.send(topic, sagaId.toString(), command);
        log.info("Dispatched command: {} to topic: {}", command, topic);
    }
}
