package com.saga.orchestrator.service;

import com.saga.orchestrator.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class OrchestratorIT extends BaseIntegrationTest {

    @Autowired
    private SagaOrchestratorService sagaOrchestratorService;

    @Test
    void contextLoadsAndServiceIsInjected() {
        assertNotNull(sagaOrchestratorService, "Orchestrator service should be instantiated by Spring Context");
    }
}
