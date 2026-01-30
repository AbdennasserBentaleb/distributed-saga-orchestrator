package com.saga.orchestrator.service;

import com.saga.common.dto.BookingRequestDTO;
import com.saga.common.enums.SagaStatus;
import com.saga.orchestrator.entity.SagaState;
import com.saga.orchestrator.repository.SagaRepository;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates that {@code SagaOrchestratorService.startSaga()} is safe under concurrent load.
 *
 * <p>Each test fires N simultaneous booking requests from separate threads. We assert two
 * properties:
 * <ul>
 *   <li>No thread throws an unhandled exception (i.e., no deadlock or data corruption).</li>
 *   <li>The number of persisted {@link SagaState} records equals the number of requests sent.</li>
 * </ul>
 *
 * <p>This is important because {@code startSaga} is {@code @Transactional} and Spring's
 * transaction proxy must handle concurrent entrants without sharing state between threads.
 */
@SpringBootTest
@Testcontainers
class SagaConcurrencyIntegrationTest {

    private static final int THREAD_COUNT = 50;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private SagaOrchestratorService sagaOrchestratorService;

    @Autowired
    private SagaRepository sagaRepository;

    /**
     * Fires {@code THREAD_COUNT} concurrent booking requests from a cached thread pool.
     * The test asserts that every call completes without error and a corresponding
     * SagaState record exists in the database.
     */
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
                    startGate.await(); // All threads release at the same moment
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

        startGate.countDown(); // Release all threads simultaneously
        boolean completed = doneLatch.await(60, TimeUnit.SECONDS);

        pool.shutdown();

        assertTrue(completed, "Test timed out — possible deadlock under concurrent load");
        assertEquals(0, errorCount.get(),
                "Expected zero errors under concurrent startSaga() calls, but got: " + errorCount.get());
        assertEquals(THREAD_COUNT, results.size(),
                "Expected " + THREAD_COUNT + " persisted saga IDs, got: " + results.size());

        // Cross-verify against the database
        List<UUID> dbIds = results.stream()
                .map(id -> sagaRepository.findById(id))
                .filter(java.util.Optional::isPresent)
                .map(opt -> opt.get().getId())
                .collect(Collectors.toList());

        assertEquals(THREAD_COUNT, dbIds.size(),
                "Some saga records were missing from the DB after concurrent inserts");

        // Verify all persisted sagas start in PENDING status (no cross-thread state leakage)
        List<SagaState> pendingStates = dbIds.stream()
                .map(id -> sagaRepository.findById(id).orElseThrow())
                .filter(s -> s.getStatus() != SagaStatus.PENDING)
                .collect(Collectors.toList());

        assertTrue(pendingStates.isEmpty(),
                "Found saga states with unexpected non-PENDING status after concurrent creation: " + pendingStates);
    }

    /**
     * Repeated 3 times to detect non-deterministic race conditions that only surface
     * intermittently under certain scheduling orders.
     */
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
                    // intentionally silenced; counted via successCount
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
