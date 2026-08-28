package com.recoverai.backend.repository;

import com.recoverai.backend.entity.RecoveryStrategy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecoveryStrategyRepository extends JpaRepository<RecoveryStrategy, UUID> {

    @Query("SELECT s FROM RecoveryStrategy s WHERE s.recoveryCase.id = :recoveryCaseId AND s.merchant.id = :merchantId ORDER BY s.createdAt DESC")
    List<RecoveryStrategy> findByRecoveryCaseIdAndMerchantIdOrderByCreatedAtDesc(@Param("recoveryCaseId") UUID recoveryCaseId, @Param("merchantId") UUID merchantId);

    @Query("SELECT s FROM RecoveryStrategy s WHERE s.recoveryCase.id = :recoveryCaseId AND s.merchant.id = :merchantId ORDER BY s.createdAt DESC LIMIT 1")
    Optional<RecoveryStrategy> findFirstByRecoveryCaseIdAndMerchantIdOrderByCreatedAtDesc(@Param("recoveryCaseId") UUID recoveryCaseId, @Param("merchantId") UUID merchantId);

    @Query("SELECT s FROM RecoveryStrategy s WHERE s.recoveryCase.id = :recoveryCaseId ORDER BY s.createdAt DESC LIMIT 1")
    Optional<RecoveryStrategy> findFirstByRecoveryCaseIdOrderByCreatedAtDesc(@Param("recoveryCaseId") UUID recoveryCaseId);

    @Query("SELECT s FROM RecoveryStrategy s WHERE s.id = :id AND s.merchant.id = :merchantId")
    Optional<RecoveryStrategy> findByIdAndMerchantId(@Param("id") UUID id, @Param("merchantId") UUID merchantId);
}
