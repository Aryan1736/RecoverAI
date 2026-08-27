package com.recoverai.backend.repository;

import com.recoverai.backend.entity.AgentDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgentDecisionRepository extends JpaRepository<AgentDecision, UUID> {

    List<AgentDecision> findByRecoveryCaseId(UUID recoveryCaseId);

    List<AgentDecision> findByRecoveryCaseIdOrderByCreatedAtDesc(UUID recoveryCaseId);

    List<AgentDecision> findByMerchantId(UUID merchantId);

    Optional<AgentDecision> findByIdAndMerchantId(UUID id, UUID merchantId);

    Optional<AgentDecision> findFirstByRecoveryCaseIdOrderByCreatedAtDesc(UUID recoveryCaseId);
}
