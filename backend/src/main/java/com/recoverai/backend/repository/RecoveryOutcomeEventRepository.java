package com.recoverai.backend.repository;

import com.recoverai.backend.entity.RecoveryOutcomeEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecoveryOutcomeEventRepository extends JpaRepository<RecoveryOutcomeEvent, UUID> {

    Optional<RecoveryOutcomeEvent> findByMerchantIdAndProviderAndProviderEventId(
            UUID merchantId, String provider, String providerEventId);

    Optional<RecoveryOutcomeEvent> findByMerchantIdAndPayloadHash(UUID merchantId, String payloadHash);

    List<RecoveryOutcomeEvent> findByRecoveryAttemptId(UUID recoveryAttemptId);

    List<RecoveryOutcomeEvent> findByMerchantId(UUID merchantId);
}