package com.recoverai.backend.dto.notification;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MerchantWebhookPayloadDto {

    private String event;
    private UUID notificationId;
    private UUID merchantId;
    private UUID recoveryCaseId;
    private UUID recoveryAttemptId;
    private BigDecimal amount;
    private String currency;
    private String title;
    private String message;
    private Instant timestamp;

    public MerchantWebhookPayloadDto() {
    }

    public MerchantWebhookPayloadDto(String event, UUID notificationId, UUID merchantId,
                                     UUID recoveryCaseId, UUID recoveryAttemptId,
                                     BigDecimal amount, String currency, String title,
                                     String message, Instant timestamp) {
        this.event = event;
        this.notificationId = notificationId;
        this.merchantId = merchantId;
        this.recoveryCaseId = recoveryCaseId;
        this.recoveryAttemptId = recoveryAttemptId;
        this.amount = amount;
        this.currency = currency;
        this.title = title;
        this.message = message;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(UUID notificationId) {
        this.notificationId = notificationId;
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(UUID merchantId) {
        this.merchantId = merchantId;
    }

    public UUID getRecoveryCaseId() {
        return recoveryCaseId;
    }

    public void setRecoveryCaseId(UUID recoveryCaseId) {
        this.recoveryCaseId = recoveryCaseId;
    }

    public UUID getRecoveryAttemptId() {
        return recoveryAttemptId;
    }

    public void setRecoveryAttemptId(UUID recoveryAttemptId) {
        this.recoveryAttemptId = recoveryAttemptId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
