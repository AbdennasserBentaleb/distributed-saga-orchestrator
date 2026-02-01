package com.saga.orchestrator.statemachine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saga.common.enums.CommandType;
import com.saga.common.enums.SagaStatus;
import com.saga.common.enums.ServiceType;
import com.saga.common.events.SagaCommand;
import com.saga.orchestrator.entity.OutboxEvent;
import com.saga.orchestrator.entity.SagaState;
import com.saga.orchestrator.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SagaActions {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public Action<SagaStatus, SagaStateMachineEvent> dispatchFlightCommand() {
        return context -> {
            SagaState state = (SagaState) context.getMessageHeader("sagaState");
            sendCommand(state, ServiceType.FLIGHT, CommandType.BOOK, state.getFlightDetails(), null, null);
        };
    }

    public Action<SagaStatus, SagaStateMachineEvent> dispatchHotelCommand() {
        return context -> {
            SagaState state = (SagaState) context.getMessageHeader("sagaState");
            sendCommand(state, ServiceType.HOTEL, CommandType.BOOK, null, state.getHotelDetails(), null);
        };
    }

    public Action<SagaStatus, SagaStateMachineEvent> dispatchCarCommand() {
        return context -> {
            SagaState state = (SagaState) context.getMessageHeader("sagaState");
            sendCommand(state, ServiceType.CAR, CommandType.BOOK, null, null, state.getCarDetails());
        };
    }

    public Action<SagaStatus, SagaStateMachineEvent> dispatchCancelHotelCommand() {
        return context -> {
            SagaState state = (SagaState) context.getMessageHeader("sagaState");
            sendCommand(state, ServiceType.HOTEL, CommandType.CANCEL, null, null, null);
        };
    }

    public Action<SagaStatus, SagaStateMachineEvent> dispatchCancelFlightCommand() {
        return context -> {
            SagaState state = (SagaState) context.getMessageHeader("sagaState");
            sendCommand(state, ServiceType.FLIGHT, CommandType.CANCEL, null, null, null);
        };
    }

    public Action<SagaStatus, SagaStateMachineEvent> logCompleted() {
        return context -> {
            SagaState state = (SagaState) context.getMessageHeader("sagaState");
            log.info("Saga COMPLETED successfully for sagaId: {}", state.getId());
        };
    }

    public Action<SagaStatus, SagaStateMachineEvent> logCompensated() {
        return context -> {
            SagaState state = (SagaState) context.getMessageHeader("sagaState");
            log.info("Saga COMPENSATED entirely for sagaId: {}", state.getId());
        };
    }

    /**
     * Logs a CRITICAL alert when compensation itself has failed.
     *
     * <p>This is the last resort terminal state. The saga cannot be automatically
     * recovered — an operations engineer must manually inspect the DLQ and
     * reconcile the downstream services.
     */
    public Action<SagaStatus, SagaStateMachineEvent> logCompensationFailed() {
        return context -> {
            SagaState state = (SagaState) context.getMessageHeader("sagaState");
            log.error("CRITICAL: Saga compensation FAILED for sagaId: {}. " +
                      "Manual reconciliation required. Check DLQ for saga commands.", state.getId());
        };
    }

    public void sendCommand(SagaState sagaState, ServiceType targetService, CommandType commandType,
                             String flight, String hotel, String car) {
        SagaCommand command = SagaCommand.builder()
                .eventId(UUID.randomUUID())
                .sagaId(sagaState.getId())
                .customerId(sagaState.getCustomerId())
                .targetService(targetService)
                .commandType(commandType)
                .flightDetails(flight)
                .hotelDetails(hotel)
                .carDetails(car)
                .build();

        String topic = targetService.name().toLowerCase() + "-commands";

        try {
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .id(command.getEventId())
                    .sagaId(sagaState.getId())
                    .topic(topic)
                    .eventPayload(objectMapper.writeValueAsString(command))
                    .build();
            outboxEventRepository.save(outboxEvent);
            log.info("Saved command to Outbox: {} to topic: {}", command, topic);
        } catch (Exception e) {
            log.error("Failed to serialize SagaCommand for Outbox", e);
            throw new RuntimeException("Dual Write Prevention Failed: Could not save to Outbox", e);
        }
    }
}
