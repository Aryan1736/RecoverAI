package com.recoverai.backend.service.provider.dto;

import com.recoverai.backend.service.provider.classification.ProviderErrorClassifier;
import com.recoverai.backend.service.provider.classification.ProviderFailureType;

import java.time.Instant;

public class PaymentRetryResult {

    private final boolean success;
    private final String transactionId;
    private final String providerName;
    private final String resultCode;
    private final String resultMessage;
    private final String metadata;
    private final Instant timestamp;
    private final ProviderFailureType failureType;

    public PaymentRetryResult(boolean success,
                              String transactionId,
                              String providerName,
                              String resultCode,
                              String resultMessage,
                              String metadata,
                              Instant timestamp) {
        this(success, transactionId, providerName, resultCode, resultMessage, metadata, timestamp,
                success ? null : ProviderErrorClassifier.classifyResultCode(resultCode));
    }

    public PaymentRetryResult(boolean success,
                              String transactionId,
                              String providerName,
                              String resultCode,
                              String resultMessage,
                              String metadata,
                              Instant timestamp,
                              ProviderFailureType failureType) {
        this.success = success;
        this.transactionId = transactionId;
        this.providerName = providerName;
        this.resultCode = resultCode;
        this.resultMessage = resultMessage;
        this.metadata = metadata;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.failureType = failureType != null ? failureType
                : (success ? null : ProviderErrorClassifier.classifyResultCode(resultCode));
    }

    public static PaymentRetryResult success(String transactionId,
                                             String providerName,
                                             String resultCode,
                                             String resultMessage,
                                             String metadata) {
        return new PaymentRetryResult(true, transactionId, providerName, resultCode, resultMessage, metadata, Instant.now(), null);
    }

    public static PaymentRetryResult failure(String transactionId,
                                             String providerName,
                                             String resultCode,
                                             String resultMessage,
                                             String metadata) {
        return failure(transactionId, providerName, resultCode, resultMessage, metadata,
                ProviderErrorClassifier.classifyResultCode(resultCode));
    }

    public static PaymentRetryResult failure(String transactionId,
                                             String providerName,
                                             String resultCode,
                                             String resultMessage,
                                             String metadata,
                                             ProviderFailureType failureType) {
        return new PaymentRetryResult(false, transactionId, providerName, resultCode, resultMessage, metadata, Instant.now(), failureType);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getProviderName() {
        return providerName;
    }

    public String getResultCode() {
        return resultCode;
    }

    public String getResultMessage() {
        return resultMessage;
    }

    public String getMetadata() {
        return metadata;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public ProviderFailureType getFailureType() {
        return failureType;
    }

    public boolean isRetryable() {
        return !success && failureType != null && failureType.isRetryable();
    }
}
