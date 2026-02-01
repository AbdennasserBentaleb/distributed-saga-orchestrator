package com.saga.orchestrator.service;

import com.saga.common.dto.BookingRequestDTO;
import com.saga.common.enums.CommandType;
import com.saga.common.enums.EventStatus;
import com.saga.common.enums.SagaStatus;
import com.saga.common.enums.ServiceType;
import com.saga.common.events.SagaEvent;
import com.saga.orchestrator.entity.OutboxEvent;
import com.saga.orchestrator.entity.SagaState;
import com.saga.orchestrator.repository.OutboxEventRepository;
import com.saga.orchestrator.repository.SagaRepository;
import com.saga.orchestrator.statemachine.SagaActions;
import com.saga.orchestrator.statemachine.SagaStateMachineEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.access.StateMachineAccessor;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.state.State;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SagaOrchestratorService}.
 *
 * <p>These tests verify the service's interaction with its actual dependencies:
 * {@link SagaRepository}, {@link SagaActions}, and {@link StateMachineFactory}.
 * The service uses the Transactional Outbox pattern — it never calls Kafka
 * directly. Tests must NOT mock {@code KafkaTemplate}; they must verify
 * {@link SagaActions#sendCommand} is called, which writes to the outbox.
 */
@ExtendWith(MockitoExtension.class)
class SagaOrchestratorServiceTest {

    @Mock
    private SagaRepository sagaRepository;

    /**
     * The real dependency that writes to the Outbox.
     * This is the correct mock target — NOT KafkaTemplate.
     */
    @Mock
    private SagaActions sagaActions;

    @Mock
    private StateMachineFactory<SagaStatus, SagaStateMachineEvent> stateMachineFactory;

    @Mock
    private StateMachine<SagaStatus, SagaStateMachineEvent> stateMachine;

    @Mock
    private StateMachineAccessor<SagaStatus, SagaStateMachineEvent> stateMachineAccessor;

    @Mock
    private State<SagaStatus, SagaStateMachineEvent> state;

    @InjectMocks
    private SagaOrchestratorService orchestratorService;

    @Test
    void startSaga_persistsSagaAndDispatchesFlightCommand() {
        // Arrange
        BookingRequestDTO request = new BookingRequestDTO();
        request.setCustomerId("CUST-1");
        request.setFlightDetails("FLT-001");
        request.setHotelDetails("HTL-001");
        request.setCarDetails("CAR-001");

        UUID sagaId = UUID.randomUUID();
        SagaState savedState = SagaState.builder()
                .id(sagaId)
                .customerId("CUST-1")
                .status(SagaStatus.PENDING)
                .build();

        when(sagaRepository.save(any(SagaState.class))).thenReturn(savedState);

        // Act
        UUID resultId = orchestratorService.startSaga(request);

        // Assert: correct saga ID returned
        assertEquals(sagaId, resultId);

        // Assert: saga was persisted once
        verify(sagaRepository, times(1)).save(any(SagaState.class));

        // Assert: FLIGHT booking command dispatched via Outbox (NOT KafkaTemplate.send)
        verify(sagaActions, times(1)).sendCommand(
                any(SagaState.class),
                eq(ServiceType.FLIGHT),
                eq(CommandType.BOOK),
                eq("FLT-001"),
                isNull(),
                isNull()
        );
    }

    @Test
    void handleSagaEvent_flightSuccess_transitionsToFlightBooked() {
        // Arrange
        UUID sagaId = UUID.randomUUID();
        SagaState sagaState = SagaState.builder()
                .id(sagaId)
                .status(SagaStatus.PENDING)
                .build();

        when(sagaRepository.findById(sagaId)).thenReturn(Optional.of(sagaState));
        when(stateMachineFactory.getStateMachine(sagaId.toString())).thenReturn(stateMachine);
        when(stateMachine.getStateMachineAccessor()).thenReturn(stateMachineAccessor);
        doNothing().when(stateMachineAccessor).doWithAllRegions(any());
        when(stateMachine.getState()).thenReturn(state);
        when(state.getId()).thenReturn(SagaStatus.FLIGHT_BOOKED);

        SagaEvent event = SagaEvent.builder()
                .sagaId(sagaId)
                .serviceType(ServiceType.FLIGHT)
                .eventStatus(EventStatus.SUCCESS)
                .build();

        // Act
        orchestratorService.handleSagaEvent(event);

        // Assert: state machine queried and saga updated to FLIGHT_BOOKED
        assertEquals(SagaStatus.FLIGHT_BOOKED, sagaState.getStatus());
        verify(sagaRepository).saveAndFlush(sagaState);
    }

    @Test
    void handleSagaEvent_sagaNotFound_throwsException() {
        UUID unknownId = UUID.randomUUID();
        when(sagaRepository.findById(unknownId)).thenReturn(Optional.empty());

        SagaEvent event = SagaEvent.builder()
                .sagaId(unknownId)
                .serviceType(ServiceType.FLIGHT)
                .eventStatus(EventStatus.SUCCESS)
                .build();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> orchestratorService.handleSagaEvent(event));
        assertTrue(ex.getMessage().contains(unknownId.toString()));
    }

    @Test
    void handleSagaEvent_flightFailure_triggersCompensation() {
        // Arrange
        UUID sagaId = UUID.randomUUID();
        SagaState sagaState = SagaState.builder()
                .id(sagaId)
                .status(SagaStatus.PENDING)
                .build();

        when(sagaRepository.findById(sagaId)).thenReturn(Optional.of(sagaState));
        when(stateMachineFactory.getStateMachine(sagaId.toString())).thenReturn(stateMachine);
        when(stateMachine.getStateMachineAccessor()).thenReturn(stateMachineAccessor);
        doNothing().when(stateMachineAccessor).doWithAllRegions(any());
        when(stateMachine.getState()).thenReturn(state);
        when(state.getId()).thenReturn(SagaStatus.COMPENSATED);

        SagaEvent event = SagaEvent.builder()
                .sagaId(sagaId)
                .serviceType(ServiceType.FLIGHT)
                .eventStatus(EventStatus.FAILED)
                .message("Seat unavailable")
                .build();

        // Act
        orchestratorService.handleSagaEvent(event);

        // Assert: saga moved to COMPENSATED (no flight to roll back at this point)
        assertEquals(SagaStatus.COMPENSATED, sagaState.getStatus());
        verify(sagaRepository).saveAndFlush(sagaState);
    }
}
