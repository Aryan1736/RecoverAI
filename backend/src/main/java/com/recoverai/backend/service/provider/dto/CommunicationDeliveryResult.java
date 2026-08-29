package com.recoverai.backend.service.provider.dto;

import com.recoverai.backend.service.provider.classification.ProviderErrorClassifier;
import com.recoverai.backend.service.provider.classification.ProviderFailureType;

import java.time.Instant;

public class CommunicationDeliveryResult {

    private final boolean success;
    private final String deliveryId;
    private final String providerName;
    private final String resultCode;
    private final String resultMessage;
    private final String metadata;
    private final Instant timestamp;
    private final ProviderFailureType failureType;

    public CommunicationDeliveryResult(boolean success,
                                       String deliveryId,
                                       String providerName,
                                       String resultCode,
                                       String resultMessage,
                                       String metadata,
                                       Instant timestamp) {
        this(success, deliveryId, providerName, resultCode, resultMessage, metadata, timestamp,
                success ? null : ProviderErrorClassifier.classifyResultCode(resultCode));
    }

    public CommunicationDeliveryResult(boolean success,
                                       String deliveryId,
                                       String providerName,
                                       String resultCode,
                                       String resultMessage,
                                       String metadata,
                                       Instant timestamp,
                                       ProviderFailureType failureType) {
        this.success = success;
        this.deliveryId = deliveryId;
        this.providerName = providerName;
        this.resultCode = resultCode;
        this.resultMessage = resultMessage;
        this.metadata = metadata;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.failureType = failureType != null ? failureType
                : (success ? null : ProviderErrorClassifier.classifyResultCode(resultCode));
    }

    public static CommunicationDeliveryResult success(String deliveryId,
                                                      String providerName,
                                                      String resultCode,
                                                      String resultMessage,
                                                      String metadata) {
        return new CommunicationDeliveryResult(true, deliveryId, providerName, resultCode, resultMessage, metadata, Instant.now(), null);
    }

    public static CommunicationDeliveryResult failure(String deliveryId,
                                                      String providerName,
                                                      String resultCode,
                                                      String resultMessage,
                                                      String metadata) {
        return failure(deliveryId, providerName, resultCode, resultMessage, metadata,
                ProviderErrorClassifier.classifyResultCode(resultCode));
    }

    public static CommunicationDeliveryResult failure(String deliveryId,
                                                      String providerName,
                                                      String resultCode,
                                                      String resultMessage,
                                                      String metadata,
                                                      ProviderFailureType failureType) {
        return new CommunicationDeliveryResult(false, deliveryId, providerName, resultCode, resultMessage, metadata, Instant.now(), failureType);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getDeliveryId() {
        return deliveryId;
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
