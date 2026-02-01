package com.saga.orchestrator.service;

import com.saga.common.dto.BookingRequestDTO;
import com.saga.common.enums.SagaStatus;
import com.saga.orchestrator.BaseIntegrationTest;
import com.saga.orchestrator.entity.SagaState;
import com.saga.orchestrator.repository.SagaRepository;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SagaConcurrencyIT extends BaseIntegrationTest {

    private static final int THREAD_COUNT = 50;

    @Autowired
    private SagaOrchestratorService sagaOrchestratorService;

    @Autowired
    private SagaRepository sagaRepository;

    @Test
    void concurrentSagaStarts_produceNoPersistenceErrors() throws InterruptedException {
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger errorCount = new AtomicInteger(0);
        List<UUID> results = new java.util.concurrent.CopyOnWriteArrayList<>();

        ExecutorService pool = Executors.newFixedThreadPool(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int requestId = i;
            pool.submit(() -> {
                try {
                    startGate.await(); 
                    BookingRequestDTO dto = new BookingRequestDTO();
                    dto.setCustomerId("stress-test-user-" + requestId);
                    dto.setFlightDetails("FLT-" + requestId);
                    dto.setHotelDetails("HTL-" + requestId);
                    dto.setCarDetails("CAR-" + requestId);
                    UUID id = sagaOrchestratorService.startSaga(dto);
                    results.add(id);
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startGate.countDown(); 
        boolean completed = doneLatch.await(60, TimeUnit.SECONDS);

        pool.shutdown();

        assertTrue(completed, "Test timed out — possible deadlock under concurrent load");
        assertEquals(0, errorCount.get(),
                "Expected zero errors under concurrent startSaga() calls, but got: " + errorCount.get());
        assertEquals(THREAD_COUNT, results.size(),
                "Expected " + THREAD_COUNT + " persisted saga IDs, got: " + results.size());

        List<UUID> dbIds = results.stream()
                .map(id -> sagaRepository.findById(id))
                .filter(java.util.Optional::isPresent)
                .map(opt -> opt.get().getId())
                .collect(Collectors.toList());

        assertEquals(THREAD_COUNT, dbIds.size(),
                "Some saga records were missing from the DB after concurrent inserts");

        List<SagaState> pendingStates = dbIds.stream()
                .map(id -> sagaRepository.findById(id).orElseThrow())
                .filter(s -> s.getStatus() != SagaStatus.PENDING)
                .collect(Collectors.toList());

        assertTrue(pendingStates.isEmpty(),
                "Found saga states with unexpected non-PENDING status after concurrent creation: " + pendingStates);
    }

    @RepeatedTest(3)
    void repeatedConcurrentStarts_alwaysSucceed() throws InterruptedException {
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);

        ExecutorService pool = Executors.newFixedThreadPool(THREAD_COUNT);
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int requestId = i;
            pool.submit(() -> {
                try {
                    startGate.await();
                    BookingRequestDTO dto = new BookingRequestDTO();
                    dto.setCustomerId("repeat-user-" + requestId + "-" + UUID.randomUUID());
                    dto.setFlightDetails("FLT-R" + requestId);
                    dto.setHotelDetails("HTL-R" + requestId);
                    dto.setCarDetails("CAR-R" + requestId);
                    sagaOrchestratorService.startSaga(dto);
                    successCount.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startGate.countDown();
        boolean completed = doneLatch.await(60, TimeUnit.SECONDS);
        pool.shutdown();

        assertTrue(completed, "Repeated concurrent test timed out — suspect deadlock");
        assertEquals(THREAD_COUNT, successCount.get(),
                "Not all concurrent invocations succeeded in repeated run");
    }
}
