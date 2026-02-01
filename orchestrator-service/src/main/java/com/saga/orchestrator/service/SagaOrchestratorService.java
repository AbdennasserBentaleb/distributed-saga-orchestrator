package com.saga.orchestrator.service;

import com.saga.common.dto.BookingRequestDTO;
import com.saga.common.enums.CommandType;
import com.saga.common.enums.EventStatus;
import com.saga.common.enums.SagaStatus;
import com.saga.common.enums.ServiceType;
import com.saga.common.events.SagaEvent;
import com.saga.orchestrator.entity.SagaState;
import com.saga.orchestrator.repository.SagaRepository;
import com.saga.orchestrator.statemachine.SagaStateMachineEvent;
import com.saga.orchestrator.statemachine.SagaActions;
import com.saga.orchestrator.exception.SagaNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SagaOrchestratorService {

    private final SagaRepository sagaRepository;
    private final SagaActions sagaActions;
    private final StateMachineFactory<SagaStatus, SagaStateMachineEvent> stateMachineFactory;

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

        sagaActions.sendCommand(sagaState, ServiceType.FLIGHT, CommandType.BOOK, request.getFlightDetails(), null, null);
        return sagaId;
    }

    @Transactional
    public void handleSagaEvent(SagaEvent event) {
        log.info("Received event for sagaId: {}, service: {}, status: {}", event.getSagaId(), event.getServiceType(),
                event.getEventStatus());

        SagaState sagaState = sagaRepository.findById(event.getSagaId())
                .orElseThrow(() -> new SagaNotFoundException(event.getSagaId()));

        if (event.getEventStatus() == EventStatus.SUCCESS) {
            handleSuccessEvent(sagaState, event.getServiceType());
        } else {
            handleFailureEvent(sagaState, event.getServiceType(), event.getMessage());
        }
    }

    private StateMachine<SagaStatus, SagaStateMachineEvent> build(SagaState sagaState) {
        StateMachine<SagaStatus, SagaStateMachineEvent> sm = stateMachineFactory.getStateMachine(sagaState.getId().toString());
        sm.stop();
        sm.getStateMachineAccessor()
                .doWithAllRegions(sma -> sma.resetStateMachine(new DefaultStateMachineContext<>(sagaState.getStatus(), null, null, null)));
        sm.start();
        return sm;
    }

    private void handleSuccessEvent(SagaState sagaState, ServiceType serviceType) {
        StateMachine<SagaStatus, SagaStateMachineEvent> sm = build(sagaState);
        SagaStateMachineEvent event = null;
        switch (serviceType) {
            case FLIGHT: event = SagaStateMachineEvent.FLIGHT_SUCCESS; break;
            case HOTEL: event = SagaStateMachineEvent.HOTEL_SUCCESS; break;
            case CAR: event = SagaStateMachineEvent.CAR_SUCCESS; break;
        }

        if (event != null) {
            Message<SagaStateMachineEvent> msg = MessageBuilder.withPayload(event)
                    .setHeader("sagaState", sagaState)
                    .build();
            sm.sendEvent(msg);
            sagaState.setStatus(sm.getState().getId());
            sagaRepository.saveAndFlush(sagaState);
        }
    }

    private void handleFailureEvent(SagaState sagaState, ServiceType serviceType, String reason) {
        log.error("Saga Failed at service: {}, reason: {}", serviceType, reason);
        StateMachine<SagaStatus, SagaStateMachineEvent> sm = build(sagaState);
        SagaStateMachineEvent event = null;
        switch (serviceType) {
            case FLIGHT: event = SagaStateMachineEvent.FLIGHT_FAILED; break;
            case HOTEL: event = SagaStateMachineEvent.HOTEL_FAILED; break;
            case CAR: event = SagaStateMachineEvent.CAR_FAILED; break;
        }

        if (event != null) {
            Message<SagaStateMachineEvent> msg = MessageBuilder.withPayload(event)
                    .setHeader("sagaState", sagaState)
                    .build();
            sm.sendEvent(msg);
            sagaState.setStatus(sm.getState().getId());
            sagaRepository.saveAndFlush(sagaState);
        }
    }
}
