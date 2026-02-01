package com.saga.orchestrator.exception;

import java.util.UUID;

public class SagaNotFoundException extends RuntimeException {

    public SagaNotFoundException(UUID sagaId) {
        super("Saga not found for ID: " + sagaId);
    }

    public SagaNotFoundException(String message) {
        super(message);
    }
}
