package com.saga.orchestrator;

import com.saga.common.dto.BookingRequestDTO;
import com.saga.orchestrator.repository.OutboxEventRepository;
import com.saga.orchestrator.repository.SagaRepository;
import com.saga.orchestrator.service.SagaOrchestratorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class IntegrationTest extends BaseIntegrationTest {

    @Autowired
    private SagaOrchestratorService sagaOrchestratorService;

    @Autowired
    private SagaRepository sagaRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Test
    void testConcurrentSagaCreationWithOutbox() throws InterruptedException {
        int threadCount = 20;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final String customerId = "CUST-" + i;
            executorService.submit(() -> {
                try {
                    BookingRequestDTO request = new BookingRequestDTO();
                    request.setCustomerId(customerId);
                    request.setFlightDetails("FLIGHT-" + UUID.randomUUID());
                    request.setHotelDetails("HOTEL-" + UUID.randomUUID());
                    request.setCarDetails("CAR-" + UUID.randomUUID());
                    sagaOrchestratorService.startSaga(request);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);

        assertThat(sagaRepository.count()).isEqualTo(threadCount);
        assertThat(outboxEventRepository.count()).isEqualTo(threadCount);
    }
}
