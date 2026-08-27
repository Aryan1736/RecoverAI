package com.recoverai.backend.service.provider.dto;

import java.time.Instant;

public class PaymentRetryResult {

    private final boolean success;
    private final String transactionId;
    private final String providerName;
    private final String resultCode;
    private final String resultMessage;
    private final String metadata;
    private final Instant timestamp;

    public PaymentRetryResult(boolean success,
                              String transactionId,
                              String providerName,
                              String resultCode,
                              String resultMessage,
                              String metadata,
                              Instant timestamp) {
        this.success = success;
        this.transactionId = transactionId;
        this.providerName = providerName;
        this.resultCode = resultCode;
        this.resultMessage = resultMessage;
        this.metadata = metadata;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public static PaymentRetryResult success(String transactionId,
                                             String providerName,
                                             String resultCode,
                                             String resultMessage,
                                             String metadata) {
        return new PaymentRetryResult(true, transactionId, providerName, resultCode, resultMessage, metadata, Instant.now());
    }

    public static PaymentRetryResult failure(String transactionId,
                                             String providerName,
                                             String resultCode,
                                             String resultMessage,
                                             String metadata) {
        return new PaymentRetryResult(false, transactionId, providerName, resultCode, resultMessage, metadata, Instant.now());
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
}
