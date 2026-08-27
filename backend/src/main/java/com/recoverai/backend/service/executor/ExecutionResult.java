package com.recoverai.backend.service.executor;

import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;

import java.util.Objects;

public class ExecutionResult {

    private final RecoveryAttemptStatus status;
    private final String resultCode;
    private final String resultMessage;
    private final String recoveryLink;
    private final String metadata;

    public ExecutionResult(RecoveryAttemptStatus status, String resultCode, String resultMessage, String recoveryLink, String metadata) {
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.resultCode = resultCode;
        this.resultMessage = resultMessage;
        this.recoveryLink = recoveryLink;
        this.metadata = metadata;
    }

    public static ExecutionResult sent(String resultCode, String resultMessage, String recoveryLink, String metadata) {
        return new ExecutionResult(RecoveryAttemptStatus.SENT, resultCode, resultMessage, recoveryLink, metadata);
    }

    public static ExecutionResult success(String resultCode, String resultMessage, String recoveryLink, String metadata) {
        return new ExecutionResult(RecoveryAttemptStatus.SUCCESS, resultCode, resultMessage, recoveryLink, metadata);
    }

    public static ExecutionResult failed(String resultCode, String resultMessage, String recoveryLink, String metadata) {
        return new ExecutionResult(RecoveryAttemptStatus.FAILED, resultCode, resultMessage, recoveryLink, metadata);
    }

    public static ExecutionResult skipped(String resultCode, String resultMessage, String metadata) {
        return new ExecutionResult(RecoveryAttemptStatus.SKIPPED, resultCode, resultMessage, null, metadata);
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
}
