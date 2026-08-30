package com.recoverai.backend.dto.queue;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.entity.RecoveryExecutionQueueItem;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.entity.enums.RecoveryQueueStatus;

import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeadLetterQueueItemResponseDto {

    private static final Pattern SENSITIVE_TOKEN_PATTERN = Pattern.compile(
            "(?i)(bearer\\s+[a-zA-Z0-9_\\-\\.]+)|(key\\s*[:=]\\s*['\"]?[a-zA-Z0-9_\\-\\.]+['\"]?)|(secret\\s*[:=]\\s*['\"]?[a-zA-Z0-9_\\-\\.]+['\"]?)|(rzp_[a-zA-Z0-9_]+)|(sk_[a-zA-Z0-9_]+)|(\\b(?:\\d[ -]*?){13,16}\\b)"
    );

    private UUID id;
    private UUID recoveryAttemptId;
    private UUID recoveryCaseId;
    private RecoveryQueueStatus status;
    private int retryCount;
    private int maxRetries;
    private Instant availableAt;
    private Instant claimedAt;
    private Instant completedAt;
    private String lastErrorCode;
    private String lastErrorMessage;
    private Instant createdAt;
    private Instant updatedAt;
    private RecoveryChannel channel;
    private String strategyAction;
    private Integer attemptNumber;

    public DeadLetterQueueItemResponseDto() {
    }

    public DeadLetterQueueItemResponseDto(UUID id, UUID recoveryAttemptId, UUID recoveryCaseId,
                                         RecoveryQueueStatus status, int retryCount, int maxRetries,
                                         Instant availableAt, Instant claimedAt, Instant completedAt,
                                         String lastErrorCode, String lastErrorMessage,
                                         Instant createdAt, Instant updatedAt,
                                         RecoveryChannel channel, String strategyAction, Integer attemptNumber) {
        this.id = id;
        this.recoveryAttemptId = recoveryAttemptId;
        this.recoveryCaseId = recoveryCaseId;
        this.status = status;
        this.retryCount = retryCount;
        this.maxRetries = maxRetries;
        this.availableAt = availableAt;
        this.claimedAt = claimedAt;
        this.completedAt = completedAt;
        this.lastErrorCode = lastErrorCode;
        this.lastErrorMessage = sanitizeErrorMessage(lastErrorMessage);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.channel = channel;
        this.strategyAction = strategyAction;
        this.attemptNumber = attemptNumber;
    }

    public static DeadLetterQueueItemResponseDto fromEntity(RecoveryExecutionQueueItem item) {
        if (item == null) {
            return null;
        }

        RecoveryAttempt attempt = item.getRecoveryAttempt();
        UUID attemptId = attempt != null ? attempt.getId() : null;
        RecoveryChannel channel = attempt != null ? attempt.getChannel() : null;
        String action = attempt != null && attempt.getStrategy() != null ? attempt.getStrategy().getRecommendedAction() : null;
        Integer attemptNumber = attempt != null ? attempt.getAttemptNumber() : null;

        UUID caseId = item.getRecoveryCase() != null ? item.getRecoveryCase().getId() : null;

        return new DeadLetterQueueItemResponseDto(
                item.getId(),
                attemptId,
                caseId,
                item.getStatus(),
                item.getRetryCount(),
                item.getMaxRetries(),
                item.getAvailableAt(),
                item.getClaimedAt(),
                item.getCompletedAt(),
                item.getLastErrorCode(),
                item.getLastErrorMessage(),
                item.getCreatedAt(),
                item.getUpdatedAt(),
                channel,
                action,
                attemptNumber
        );
    }

    public static String sanitizeErrorMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            return rawMessage;
        }
        return SENSITIVE_TOKEN_PATTERN.matcher(rawMessage).replaceAll("[REDACTED]");
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRecoveryAttemptId() {
        return recoveryAttemptId;
    }

    public void setRecoveryAttemptId(UUID recoveryAttemptId) {
        this.recoveryAttemptId = recoveryAttemptId;
    }

    public UUID getRecoveryCaseId() {
        return recoveryCaseId;
    }

    public void setRecoveryCaseId(UUID recoveryCaseId) {
        this.recoveryCaseId = recoveryCaseId;
    }

    public RecoveryQueueStatus getStatus() {
        return status;
    }

    public void setStatus(RecoveryQueueStatus status) {
        this.status = status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public Instant getAvailableAt() {
        return availableAt;
    }

    public void setAvailableAt(Instant availableAt) {
        this.availableAt = availableAt;
    }

    public Instant getClaimedAt() {
        return claimedAt;
    }

    public void setClaimedAt(Instant claimedAt) {
        this.claimedAt = claimedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getLastErrorCode() {
        return lastErrorCode;
    }

    public void setLastErrorCode(String lastErrorCode) {
        this.lastErrorCode = lastErrorCode;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public void setLastErrorMessage(String lastErrorMessage) {
        this.lastErrorMessage = sanitizeErrorMessage(lastErrorMessage);
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

    public RecoveryChannel getChannel() {
        return channel;
    }

    public void setChannel(RecoveryChannel channel) {
        this.channel = channel;
    }

    public String getStrategyAction() {
        return strategyAction;
    }

    public void setStrategyAction(String strategyAction) {
        this.strategyAction = strategyAction;
    }

    public Integer getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(Integer attemptNumber) {
        this.attemptNumber = attemptNumber;
    }
}
