package com.saga.orchestrator.messaging;

import com.saga.common.events.SagaEvent;
import com.saga.orchestrator.service.SagaOrchestratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrchestratorEventHandler {

    private final SagaOrchestratorService sagaOrchestratorService;

    /**
     * Listens to saga reply events from worker services.
     *
     * <p>The {@code @DistributedLock} ensures only one orchestrator pod
     * processes a given sagaId at a time, preventing concurrent state machine
     * transitions for the same saga under horizontal scaling.
     *
     * <p><strong>Exception handling:</strong> We do NOT swallow exceptions here.
     * If {@code handleSagaEvent} throws, the exception propagates back to
     * Spring Kafka, which will NOT commit the offset. The message will be
     * retried according to the consumer's retry/DLQ configuration. Silently
     * catching and logging would cause the offset to commit, permanently
     * losing the event with no recovery path.
     */
    @RetryableTopic(
            attempts = "4",
            backoff = @org.springframework.retry.annotation.Backoff(delay = 1000, multiplier = 2.0),
            exclude = {IllegalArgumentException.class, NullPointerException.class}
    )
    @com.saga.common.annotations.DistributedLock(keyPrefix = "orchestrator", keyExpression = "#event.sagaId")
    @KafkaListener(topics = "saga-events", groupId = "orchestrator-group")
    public void listenSagaEvents(SagaEvent event) {
        log.info("Received Saga Event: sagaId={}, service={}, status={}",
                event.getSagaId(), event.getServiceType(), event.getEventStatus());
        sagaOrchestratorService.handleSagaEvent(event);
    }
}
