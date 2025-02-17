package com.saga.orchestrator.controller;

import com.saga.common.dto.BookingRequestDTO;
import com.saga.orchestrator.entity.SagaState;
import com.saga.orchestrator.repository.SagaRepository;
import com.saga.orchestrator.service.SagaOrchestratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class OrchestratorController {

    private final SagaOrchestratorService sagaOrchestratorService;
    private final SagaRepository sagaRepository;

    @PostMapping("/book")
    public ResponseEntity<String> bookTrip(@RequestBody BookingRequestDTO request) {
        UUID sagaId = sagaOrchestratorService.startSaga(request);
        return ResponseEntity.accepted().body("Trip booking saga initiated with ID: " + sagaId);
    }

    @GetMapping("/audit")
    public ResponseEntity<Map<String, Long>> getAuditSummary() {
        var allSagas = sagaRepository.findAll();
        Map<String, Long> summary = allSagas.stream()
                .collect(Collectors.groupingBy(saga -> saga.getStatus().name(), Collectors.counting()));
        summary.put("TOTAL_REQUESTS", (long) allSagas.size());
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<SagaState>> getRecentTrips() {
        List<SagaState> recent = sagaRepository.findTop20ByOrderByCreatedAtDesc();
        return ResponseEntity.ok(recent);
    }
}
