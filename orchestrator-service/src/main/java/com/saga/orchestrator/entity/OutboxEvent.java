package com.saga.orchestrator.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "outbox_event")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {

    @Id
    @Column(name = "event_id")
    private UUID id;

    @Column(nullable = false, name = "saga_id")
    private UUID sagaId;

    @Column(nullable = false, columnDefinition = "TEXT", name = "event_payload")
    private String eventPayload;

    @Column(nullable = false)
    private String topic;
}
