package com.recoverai.backend.service.provider.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderIdempotencyAndMaskingTest {

    private final ProviderIdempotencyService idempotencyService = new ProviderIdempotencyService();

    @Test
    @DisplayName("Same merchant and attempt must produce strictly identical idempotency key across retries")
    void sameAttemptProducesSameKey() {
        UUID merchantId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();

        String key1 = idempotencyService.generateKey(merchantId, attemptId);
        String key2 = idempotencyService.generateKey(merchantId, attemptId);
        String key3 = idempotencyService.generateKey(merchantId, attemptId);

        assertThat(key1).isNotEmpty();
        assertThat(key1).isEqualTo(key2);
        assertThat(key2).isEqualTo(key3);
        assertThat(key1).startsWith("rec_idem_");
    }

    @Test
    @DisplayName("Different attempt must produce different idempotency key")
    void differentAttemptProducesDifferentKey() {
        UUID merchantId = UUID.randomUUID();
        UUID attemptId1 = UUID.randomUUID();
        UUID attemptId2 = UUID.randomUUID();

        String key1 = idempotencyService.generateKey(merchantId, attemptId1);
        String key2 = idempotencyService.generateKey(merchantId, attemptId2);

        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    @DisplayName("CredentialMasker should mask sensitive secrets, phone numbers, and emails")
    void credentialMaskerShouldMaskProperly() {
        assertThat(CredentialMasker.mask("SG.secret_key_long_value")).isEqualTo("SG....lue");
        assertThat(CredentialMasker.mask("1234")).isEqualTo("****");
        assertThat(CredentialMasker.mask("")).isEqualTo("[EMPTY]");

        assertThat(CredentialMasker.maskPhone("+919876543210")).isEqualTo("+91****10");
        assertThat(CredentialMasker.maskEmail("john.doe@company.com")).isEqualTo("j***@company.com");
    }

    @Test
    @DisplayName("CredentialMasker should sanitize Authorization headers and card numbers from messages")
    void credentialMaskerShouldSanitizeMessages() {
        String msg = "Request with Authorization: Bearer secret_jwt_token and card: 4111 2222 3333 4444 failed";
        String sanitized = CredentialMasker.sanitizeMessage(msg);

        assertThat(sanitized).doesNotContain("secret_jwt_token");
        assertThat(sanitized).doesNotContain("4111 2222 3333 4444");
        assertThat(sanitized).contains("[PROTECTED]");
        assertThat(sanitized).contains("[PROTECTED_CARD]");
    }
}
