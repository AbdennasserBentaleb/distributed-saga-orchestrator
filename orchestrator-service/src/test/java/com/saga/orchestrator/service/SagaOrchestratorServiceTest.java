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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SagaOrchestratorServiceTest {

    @Mock
    private SagaRepository sagaRepository;

    @Mock
    private KafkaTemplate<String, SagaCommand> kafkaTemplate;

    @InjectMocks
    private SagaOrchestratorService orchestratorService;

    @Test
    void startSaga_Successfully() {
        BookingRequestDTO request = new BookingRequestDTO();
        request.setCustomerId("CUST-1");

        SagaState savedState = SagaState.builder().id(UUID.randomUUID()).status(SagaStatus.PENDING).build();
        when(sagaRepository.save(any(SagaState.class))).thenReturn(savedState);

        UUID resultId = orchestratorService.startSaga(request);

        assertEquals(savedState.getId(), resultId);
        verify(kafkaTemplate).send(eq("flight-commands"), eq(resultId.toString()), any(SagaCommand.class));
    }

    @Test
    void handleSagaEvent_FlightSuccess() {
        UUID sagaId = UUID.randomUUID();
        SagaState state = SagaState.builder().id(sagaId).status(SagaStatus.PENDING).build();
        when(sagaRepository.findById(sagaId)).thenReturn(Optional.of(state));

        SagaEvent event = SagaEvent.builder()
                .sagaId(sagaId)
                .serviceType(ServiceType.FLIGHT)
                .eventStatus(EventStatus.SUCCESS)
                .build();

        orchestratorService.handleSagaEvent(event);

        assertEquals(SagaStatus.FLIGHT_BOOKED, state.getStatus());
        verify(kafkaTemplate).send(eq("hotel-commands"), eq(sagaId.toString()), any(SagaCommand.class));
        verify(sagaRepository).saveAndFlush(state);
    }
}
