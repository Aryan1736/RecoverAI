package com.recoverai.backend.dto.recoverycase;

import com.recoverai.backend.dto.diagnosis.AgentDecisionResponseDto;
import com.recoverai.backend.dto.orchestration.RecoveryAttemptResponseDto;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.enums.RecoveryCaseStatus;
import com.recoverai.backend.entity.enums.RecoveryPriority;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RecoveryCaseDetailResponseDto {

    private UUID id;
    private UUID merchantId;
    private RecoveryCaseStatus status;
    private RecoveryPriority priority;
    private String failureReasonCategory;
    private BigDecimal estimatedRecoverableAmount;
    private BigDecimal recoveredAmount;
    private String currency;
    private Instant expiresAt;
    private Instant recoveredAt;
    private Instant closedAt;
    private Instant createdAt;
    private Instant updatedAt;

    private PaymentResponseDto payment;
    private CustomerResponseDto customer;
    private List<RecoveryAttemptResponseDto> attempts = new ArrayList<>();
    private AgentDecisionResponseDto latestDiagnosis;

    public RecoveryCaseDetailResponseDto() {
    }

    public RecoveryCaseDetailResponseDto(UUID id, UUID merchantId, RecoveryCaseStatus status,
                                         RecoveryPriority priority, String failureReasonCategory,
                                         BigDecimal estimatedRecoverableAmount, BigDecimal recoveredAmount,
                                         String currency, Instant expiresAt, Instant recoveredAt,
                                         Instant closedAt, Instant createdAt, Instant updatedAt,
                                         PaymentResponseDto payment, CustomerResponseDto customer,
                                         List<RecoveryAttemptResponseDto> attempts,
                                         AgentDecisionResponseDto latestDiagnosis) {
        this.id = id;
        this.merchantId = merchantId;
        this.status = status;
        this.priority = priority;
        this.failureReasonCategory = failureReasonCategory;
        this.estimatedRecoverableAmount = estimatedRecoverableAmount;
        this.recoveredAmount = recoveredAmount;
        this.currency = currency;
        this.expiresAt = expiresAt;
        this.recoveredAt = recoveredAt;
        this.closedAt = closedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.payment = payment;
        this.customer = customer;
        this.attempts = attempts != null ? attempts : new ArrayList<>();
        this.latestDiagnosis = latestDiagnosis;
    }

    public static RecoveryCaseDetailResponseDto fromEntity(RecoveryCase entity,
                                                           List<RecoveryAttemptResponseDto> attempts,
                                                           AgentDecisionResponseDto latestDiagnosis) {
        if (entity == null) {
            return null;
        }
        return new RecoveryCaseDetailResponseDto(
                entity.getId(),
                entity.getMerchant() != null ? entity.getMerchant().getId() : null,
                entity.getStatus(),
                entity.getPriority(),
                entity.getFailureReasonCategory(),
                entity.getEstimatedRecoverableAmount(),
                entity.getRecoveredAmount(),
                entity.getCurrency(),
                entity.getExpiresAt(),
                entity.getRecoveredAt(),
                entity.getClosedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                PaymentResponseDto.fromEntity(entity.getPayment()),
                CustomerResponseDto.fromEntity(entity.getCustomer()),
                attempts != null ? attempts : new ArrayList<>(),
                latestDiagnosis
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(UUID merchantId) {
        this.merchantId = merchantId;
    }

    public RecoveryCaseStatus getStatus() {
        return status;
    }

    public void setStatus(RecoveryCaseStatus status) {
        this.status = status;
    }

    public RecoveryPriority getPriority() {
        return priority;
    }

    public void setPriority(RecoveryPriority priority) {
        this.priority = priority;
    }

    public String getFailureReasonCategory() {
        return failureReasonCategory;
    }

    public void setFailureReasonCategory(String failureReasonCategory) {
        this.failureReasonCategory = failureReasonCategory;
    }

    public BigDecimal getEstimatedRecoverableAmount() {
        return estimatedRecoverableAmount;
    }

    public void setEstimatedRecoverableAmount(BigDecimal estimatedRecoverableAmount) {
        this.estimatedRecoverableAmount = estimatedRecoverableAmount;
    }

    public BigDecimal getRecoveredAmount() {
        return recoveredAmount;
    }

    public void setRecoveredAmount(BigDecimal recoveredAmount) {
        this.recoveredAmount = recoveredAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getRecoveredAt() {
        return recoveredAt;
    }

    public void setRecoveredAt(Instant recoveredAt) {
        this.recoveredAt = recoveredAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public PaymentResponseDto getPayment() {
        return payment;
    }

    public void setPayment(PaymentResponseDto payment) {
        this.payment = payment;
    }

    public CustomerResponseDto getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerResponseDto customer) {
        this.customer = customer;
    }

    public List<RecoveryAttemptResponseDto> getAttempts() {
        return attempts;
    }

    public void setAttempts(List<RecoveryAttemptResponseDto> attempts) {
        this.attempts = attempts;
    }

    public AgentDecisionResponseDto getLatestDiagnosis() {
        return latestDiagnosis;
    }

    public void setLatestDiagnosis(AgentDecisionResponseDto latestDiagnosis) {
        this.latestDiagnosis = latestDiagnosis;
    }
}
