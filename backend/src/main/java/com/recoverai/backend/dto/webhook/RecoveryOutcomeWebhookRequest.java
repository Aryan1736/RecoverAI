package com.recoverai.backend.dto.webhook;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RecoveryOutcomeWebhookRequest {

    @NotBlank(message = "Provider event ID is required")
    @JsonProperty("providerEventId")
    @JsonAlias({"event_id", "eventId", "provider_event_id"})
    private String providerEventId;

    @NotNull(message = "Merchant ID is required")
    @JsonProperty("merchantId")
    @JsonAlias({"merchant_id", "merchant"})
    private UUID merchantId;

    @NotNull(message = "Recovery attempt ID is required")
    @JsonProperty("recoveryAttemptId")
    @JsonAlias({"recovery_attempt_id", "attemptId", "attempt_id"})
    private UUID recoveryAttemptId;

    @NotNull(message = "Outcome status is required")
    @JsonProperty("outcomeStatus")
    @JsonAlias({"outcome_status", "status", "outcome"})
    private RecoveryAttemptStatus outcomeStatus;

    @NotBlank(message = "Provider is required")
    @JsonProperty("provider")
    private String provider;

    @JsonProperty("providerReference")
    @JsonAlias({"provider_reference", "externalReference", "external_reference", "reference"})
    private String providerReference;

    @JsonProperty("occurredAt")
    @JsonAlias({"occurred_at", "timestamp", "event_time"})
    private Instant occurredAt;

    @JsonProperty("resultCode")
    @JsonAlias({"result_code", "code", "error_code"})
    private String resultCode;

    @JsonProperty("resultMessage")
    @JsonAlias({"result_message", "message", "description", "error_description"})
    private String resultMessage;

    @JsonProperty("metadata")
    private String metadata;

    public RecoveryOutcomeWebhookRequest() {
    }

    public RecoveryOutcomeWebhookRequest(String providerEventId, UUID merchantId, UUID recoveryAttemptId,
                                         RecoveryAttemptStatus outcomeStatus, String provider,
                                         String providerReference, Instant occurredAt,
                                         String resultCode, String resultMessage, String metadata) {
        this.providerEventId = providerEventId;
        this.merchantId = merchantId;
        this.recoveryAttemptId = recoveryAttemptId;
        this.outcomeStatus = outcomeStatus;
        this.provider = provider;
        this.providerReference = providerReference;
        this.occurredAt = occurredAt;
        this.resultCode = resultCode;
        this.resultMessage = resultMessage;
        this.metadata = metadata;
    }

    public static RecoveryOutcomeWebhookRequestBuilder builder() {
        return new RecoveryOutcomeWebhookRequestBuilder();
    }

    public String getProviderEventId() {
        return providerEventId;
    }

    public void setProviderEventId(String providerEventId) {
        this.providerEventId = providerEventId;
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(UUID merchantId) {
        this.merchantId = merchantId;
    }

    public UUID getRecoveryAttemptId() {
        return recoveryAttemptId;
    }

    public void setRecoveryAttemptId(UUID recoveryAttemptId) {
        this.recoveryAttemptId = recoveryAttemptId;
    }

    public RecoveryAttemptStatus getOutcomeStatus() {
        return outcomeStatus;
    }

    public void setOutcomeStatus(RecoveryAttemptStatus outcomeStatus) {
        this.outcomeStatus = outcomeStatus;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProviderReference() {
        return providerReference;
    }

    public void setProviderReference(String providerReference) {
        this.providerReference = providerReference;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getResultCode() {
        return resultCode;
    }

    public void setResultCode(String resultCode) {
        this.resultCode = resultCode;
    }

    public String getResultMessage() {
        return resultMessage;
    }

    public void setResultMessage(String resultMessage) {
        this.resultMessage = resultMessage;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public static class RecoveryOutcomeWebhookRequestBuilder {
        private String providerEventId;
        private UUID merchantId;
        private UUID recoveryAttemptId;
        private RecoveryAttemptStatus outcomeStatus;
        private String provider;
        private String providerReference;
        private Instant occurredAt;
        private String resultCode;
        private String resultMessage;
        private String metadata;

        RecoveryOutcomeWebhookRequestBuilder() {
        }

        public RecoveryOutcomeWebhookRequestBuilder providerEventId(String providerEventId) {
            this.providerEventId = providerEventId;
            return this;
        }

        public RecoveryOutcomeWebhookRequestBuilder merchantId(UUID merchantId) {
            this.merchantId = merchantId;
            return this;
        }

        public RecoveryOutcomeWebhookRequestBuilder recoveryAttemptId(UUID recoveryAttemptId) {
            this.recoveryAttemptId = recoveryAttemptId;
            return this;
        }

        public RecoveryOutcomeWebhookRequestBuilder outcomeStatus(RecoveryAttemptStatus outcomeStatus) {
            this.outcomeStatus = outcomeStatus;
            return this;
        }

        public RecoveryOutcomeWebhookRequestBuilder provider(String provider) {
            this.provider = provider;
            return this;
        }

        public RecoveryOutcomeWebhookRequestBuilder providerReference(String providerReference) {
            this.providerReference = providerReference;
            return this;
        }

        public RecoveryOutcomeWebhookRequestBuilder occurredAt(Instant occurredAt) {
            this.occurredAt = occurredAt;
            return this;
        }

        public RecoveryOutcomeWebhookRequestBuilder resultCode(String resultCode) {
            this.resultCode = resultCode;
            return this;
        }

        public RecoveryOutcomeWebhookRequestBuilder resultMessage(String resultMessage) {
            this.resultMessage = resultMessage;
            return this;
        }

        public RecoveryOutcomeWebhookRequestBuilder metadata(String metadata) {
            this.metadata = metadata;
            return this;
        }

        public RecoveryOutcomeWebhookRequest build() {
            return new RecoveryOutcomeWebhookRequest(providerEventId, merchantId, recoveryAttemptId,
                    outcomeStatus, provider, providerReference, occurredAt, resultCode, resultMessage, metadata);
        }
    }
}
