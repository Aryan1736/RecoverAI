package com.recoverai.backend.dto.recoverycase;

import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.enums.RecoveryCaseStatus;
import com.recoverai.backend.entity.enums.RecoveryPriority;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class RecoveryCaseResponseDto {

    private UUID id;
    private UUID merchantId;
    private UUID paymentId;
    private UUID customerId;
    private String customerName;
    private String customerEmail;
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

    public RecoveryCaseResponseDto() {
    }

    public RecoveryCaseResponseDto(UUID id, UUID merchantId, UUID paymentId, UUID customerId,
                                   String customerName, String customerEmail, RecoveryCaseStatus status,
                                   RecoveryPriority priority, String failureReasonCategory,
                                   BigDecimal estimatedRecoverableAmount, BigDecimal recoveredAmount,
                                   String currency, Instant expiresAt, Instant recoveredAt,
                                   Instant closedAt, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.merchantId = merchantId;
        this.paymentId = paymentId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
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
    }

    public static RecoveryCaseResponseDto fromEntity(RecoveryCase entity) {
        if (entity == null) {
            return null;
        }
        return new RecoveryCaseResponseDto(
                entity.getId(),
                entity.getMerchant() != null ? entity.getMerchant().getId() : null,
                entity.getPayment() != null ? entity.getPayment().getId() : null,
                entity.getCustomer() != null ? entity.getCustomer().getId() : null,
                entity.getCustomer() != null ? entity.getCustomer().getName() : null,
                entity.getCustomer() != null ? entity.getCustomer().getEmail() : null,
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
                entity.getUpdatedAt()
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

    public UUID getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(UUID paymentId) {
        this.paymentId = paymentId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
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
}
