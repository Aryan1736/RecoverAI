package com.recoverai.backend.service.provider.dto;

import java.time.Instant;
import java.util.Objects;

public class CommunicationDeliveryResult {

    private final boolean success;
    private final String deliveryId;
    private final String providerName;
    private final String resultCode;
    private final String resultMessage;
    private final String metadata;
    private final Instant timestamp;

    public CommunicationDeliveryResult(boolean success,
                                       String deliveryId,
                                       String providerName,
                                       String resultCode,
                                       String resultMessage,
                                       String metadata,
                                       Instant timestamp) {
        this.success = success;
        this.deliveryId = deliveryId;
        this.providerName = providerName;
        this.resultCode = resultCode;
        this.resultMessage = resultMessage;
        this.metadata = metadata;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public static CommunicationDeliveryResult success(String deliveryId,
                                                      String providerName,
                                                      String resultCode,
                                                      String resultMessage,
                                                      String metadata) {
        return new CommunicationDeliveryResult(true, deliveryId, providerName, resultCode, resultMessage, metadata, Instant.now());
    }

    public static CommunicationDeliveryResult failure(String deliveryId,
                                                      String providerName,
                                                      String resultCode,
                                                      String resultMessage,
                                                      String metadata) {
        return new CommunicationDeliveryResult(false, deliveryId, providerName, resultCode, resultMessage, metadata, Instant.now());
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
}
