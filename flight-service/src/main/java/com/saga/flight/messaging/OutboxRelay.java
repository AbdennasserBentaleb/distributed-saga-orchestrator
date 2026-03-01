package com.saga.flight.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saga.common.events.SagaEvent;
import com.saga.flight.entity.OutboxEvent;
import com.saga.flight.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.springframework.transaction.annotation.Transactional;
import com.saga.common.annotations.DistributedLock;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRelay {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, SagaEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 2000)
    @DistributedLock(keyPrefix = "flight-outbox-lock", waitTime = 0, leaseTime = 10)
    @Transactional
    public void relayOutboxEvents() {
        List<OutboxEvent> events = outboxEventRepository.findAll();
        for (OutboxEvent outboxEvent : events) {
            try {
                SagaEvent sagaEvent = objectMapper.readValue(outboxEvent.getEventPayload(), SagaEvent.class);
                kafkaTemplate.send(outboxEvent.getTopic(), sagaEvent.getSagaId().toString(), sagaEvent);
                outboxEventRepository.delete(outboxEvent);
                log.info("Successfully relayed OutboxEvent for sagaId: {}", outboxEvent.getSagaId());
            } catch (Exception e) {
                log.error("Failed to relay OutboxEvent id: {}", outboxEvent.getId(), e);
            }
        }
    }
}
