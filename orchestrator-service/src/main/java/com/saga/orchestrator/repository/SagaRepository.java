package com.saga.orchestrator.repository;

import com.saga.common.enums.SagaStatus;
import com.saga.orchestrator.entity.SagaState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SagaRepository extends JpaRepository<SagaState, UUID> {
    List<SagaState> findByStatus(SagaStatus status);
    List<SagaState> findTop20ByOrderByCreatedAtDesc();
}
