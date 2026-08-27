package com.recoverai.backend.repository;

import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.enums.RecoveryCaseStatus;
import com.recoverai.backend.entity.enums.RecoveryPriority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecoveryCaseRepository extends JpaRepository<RecoveryCase, UUID> {

    List<RecoveryCase> findByMerchantId(UUID merchantId);

    Page<RecoveryCase> findByMerchantId(UUID merchantId, Pageable pageable);

    List<RecoveryCase> findByMerchantIdAndStatus(UUID merchantId, RecoveryCaseStatus status);

    Page<RecoveryCase> findByMerchantIdAndStatus(UUID merchantId, RecoveryCaseStatus status, Pageable pageable);

    List<RecoveryCase> findByMerchantIdAndPriority(UUID merchantId, RecoveryPriority priority);

    Optional<RecoveryCase> findByPaymentId(UUID paymentId);

    Optional<RecoveryCase> findByIdAndMerchantId(UUID id, UUID merchantId);

    List<RecoveryCase> findByMerchantIdAndCustomerId(UUID merchantId, UUID customerId);

    long countByMerchantIdAndStatus(UUID merchantId, RecoveryCaseStatus status);
}
