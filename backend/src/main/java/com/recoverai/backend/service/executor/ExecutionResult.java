package com.recoverai.backend.service.executor;

import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;
import com.recoverai.backend.service.provider.classification.ProviderErrorClassifier;
import com.recoverai.backend.service.provider.classification.ProviderFailureType;

import java.util.Objects;

public class ExecutionResult {

    private final RecoveryAttemptStatus status;
    private final String resultCode;
    private final String resultMessage;
    private final String recoveryLink;
    private final String metadata;
    private final ProviderFailureType failureType;

    public ExecutionResult(RecoveryAttemptStatus status, String resultCode, String resultMessage, String recoveryLink, String metadata) {
        this(status, resultCode, resultMessage, recoveryLink, metadata,
                status == RecoveryAttemptStatus.FAILED ? ProviderErrorClassifier.classifyResultCode(resultCode) : null);
    }

    public ExecutionResult(RecoveryAttemptStatus status, String resultCode, String resultMessage, String recoveryLink, String metadata, ProviderFailureType failureType) {
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.resultCode = resultCode;
        this.resultMessage = resultMessage;
        this.recoveryLink = recoveryLink;
        this.metadata = metadata;
        this.failureType = failureType != null ? failureType
                : (status == RecoveryAttemptStatus.FAILED ? ProviderErrorClassifier.classifyResultCode(resultCode) : null);
    }

    public static ExecutionResult sent(String resultCode, String resultMessage, String recoveryLink, String metadata) {
        return new ExecutionResult(RecoveryAttemptStatus.SENT, resultCode, resultMessage, recoveryLink, metadata, null);
    }

    public static ExecutionResult success(String resultCode, String resultMessage, String recoveryLink, String metadata) {
        return new ExecutionResult(RecoveryAttemptStatus.SUCCESS, resultCode, resultMessage, recoveryLink, metadata, null);
    }

    public static ExecutionResult failed(String resultCode, String resultMessage, String recoveryLink, String metadata) {
        return failed(resultCode, resultMessage, recoveryLink, metadata, ProviderErrorClassifier.classifyResultCode(resultCode));
    }

    public static ExecutionResult failed(String resultCode, String resultMessage, String recoveryLink, String metadata, ProviderFailureType failureType) {
        return new ExecutionResult(RecoveryAttemptStatus.FAILED, resultCode, resultMessage, recoveryLink, metadata, failureType);
    }

    public static ExecutionResult skipped(String resultCode, String resultMessage, String metadata) {
        return new ExecutionResult(RecoveryAttemptStatus.SKIPPED, resultCode, resultMessage, null, metadata, null);
    }

    public RecoveryAttemptStatus getStatus() {
        return status;
    }

    public String getResultCode() {
        return resultCode;
    }

    public String getResultMessage() {
        return resultMessage;
    }

    public String getRecoveryLink() {
        return recoveryLink;
    }

    public String getMetadata() {
        return metadata;
    }

    public ProviderFailureType getFailureType() {
        return failureType;
    }

    public boolean isRetryable() {
        return status == RecoveryAttemptStatus.FAILED && failureType != null && failureType.isRetryable();
    }
}
