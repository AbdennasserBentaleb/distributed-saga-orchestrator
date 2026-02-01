package com.saga.orchestrator.service;

import com.saga.common.enums.EventStatus;
import com.saga.common.enums.SagaStatus;
import com.saga.common.enums.ServiceType;
import com.saga.common.events.SagaEvent;
import com.saga.orchestrator.entity.SagaState;
import com.saga.orchestrator.repository.SagaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StuckSagaSweeper {

    private final SagaRepository sagaRepository;
    private final SagaOrchestratorService sagaOrchestratorService;

    // Sagas stuck in these states might need compensation
    private static final List<SagaStatus> NON_TERMINAL_STATES = List.of(
            SagaStatus.PENDING,
            SagaStatus.FLIGHT_BOOKED,
            SagaStatus.HOTEL_BOOKED,
            SagaStatus.CANCELLING_HOTEL,
            SagaStatus.CANCELLING_FLIGHT
    );

    @Scheduled(cron = "0 * * * * *") // Run every minute
    @SchedulerLock(name = "stuck_saga_sweeper_lock", lockAtLeastFor = "30s", lockAtMostFor = "2m")
    public void sweepStuckSagas() {
        log.info("Starting sweep for stuck Sagas...");
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);

        int pageNumber = 0;
        int pageSize = 100;
        org.springframework.data.domain.Page<SagaState> page;

        do {
            page = sagaRepository.findStuckSagas(NON_TERMINAL_STATES, fiveMinutesAgo, org.springframework.data.domain.PageRequest.of(pageNumber, pageSize));
            if (page.isEmpty() && pageNumber == 0) {
                log.info("No stuck Sagas found.");
                return;
            }

            for (SagaState saga : page.getContent()) {
                log.warn("Rollback triggered for stuck Saga ID: {}, Status: {}", saga.getId(), saga.getStatus());
                try {
                    // Simulate a failure event to trigger compensation through the normal flow
                    sagaOrchestratorService.handleSagaEvent(
                            SagaEvent.builder()
                                    .eventId(java.util.UUID.randomUUID())
                                    .sagaId(saga.getId())
                                    .serviceType(getCurrentServiceForStatus(saga.getStatus()))
                                    .eventStatus(EventStatus.FAILED)
                                    .message("Saga TTL Exceeded. Sweeper triggered rollback.")
                                    .build()
                    );
                } catch (Exception e) {
                    log.error("Failed to rollback stuck Saga ID: {}", saga.getId(), e);
                }
            }
            pageNumber++;
        } while (page.hasNext());
    }

    /**
     * Maps the current saga status to the service that should receive a synthetic
     * failure event to trigger or resume compensation.
     *
     * <p>The mapping follows the compensation chain:
     * <ul>
     *   <li>{@code PENDING} → FLIGHT never responded; inject a FLIGHT failure.</li>
     *   <li>{@code FLIGHT_BOOKED} → Stuck after flight booked, hotel never responded;
     *       inject a HOTEL failure to start cancelling the flight.</li>
     *   <li>{@code HOTEL_BOOKED} → Stuck after hotel booked, car never responded;
     *       inject a CAR failure to start cancelling hotel + flight.</li>
     *   <li>{@code CANCELLING_HOTEL} → Hotel cancel is stuck; re-inject a HOTEL failure
     *       to retry hotel cancellation.</li>
     *   <li>{@code CANCELLING_FLIGHT} → Flight cancel is stuck; re-inject a FLIGHT failure
     *       to retry flight cancellation.</li>
     * </ul>
     */
    private ServiceType getCurrentServiceForStatus(SagaStatus status) {
        return switch (status) {
            case PENDING            -> ServiceType.FLIGHT;
            case FLIGHT_BOOKED      -> ServiceType.HOTEL;
            case HOTEL_BOOKED       -> ServiceType.CAR;
            case CANCELLING_HOTEL   -> ServiceType.HOTEL;   // retry hotel cancellation
            case CANCELLING_FLIGHT  -> ServiceType.FLIGHT;  // retry flight cancellation
            default                 -> ServiceType.FLIGHT;
        };
    }
}
