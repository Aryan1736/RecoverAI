package com.recoverai.backend.service.link;

import java.time.Instant;
import java.util.UUID;

public class RecoveryLinkToken {

    private final UUID merchantId;
    private final UUID recoveryCaseId;
    private final Instant expiresAt;
    private final boolean valid;
    private final String errorMessage;

    public RecoveryLinkToken(UUID merchantId, UUID recoveryCaseId, Instant expiresAt, boolean valid, String errorMessage) {
        this.merchantId = merchantId;
        this.recoveryCaseId = recoveryCaseId;
        this.expiresAt = expiresAt;
        this.valid = valid;
        this.errorMessage = errorMessage;
    }

    public static RecoveryLinkToken valid(UUID merchantId, UUID recoveryCaseId, Instant expiresAt) {
        return new RecoveryLinkToken(merchantId, recoveryCaseId, expiresAt, true, null);
    }

    public static RecoveryLinkToken invalid(String errorMessage) {
        return new RecoveryLinkToken(null, null, null, false, errorMessage);
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public UUID getRecoveryCaseId() {
        return recoveryCaseId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isValid() {
        return valid;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
