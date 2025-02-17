package com.saga.orchestrator.messaging;

import com.saga.common.events.SagaEvent;
import com.saga.orchestrator.service.SagaOrchestratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrchestratorEventHandler {

    private final SagaOrchestratorService sagaOrchestratorService;

    @KafkaListener(topics = "saga-events", groupId = "orchestrator-group")
    public void listenSagaEvents(SagaEvent event) {
        log.info("Received Saga Event from Data Topic: {}", event);
        try {
            sagaOrchestratorService.handleSagaEvent(event);
        } catch (Exception e) {
            log.error("Error processing saga event: {}", event, e);
        }
    }
}
