package com.recoverai.backend.security;

import com.recoverai.backend.exception.WebhookSignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RazorpaySignatureVerifierTest {

    private RazorpaySignatureVerifier verifier;
    private static final String SAMPLE_PAYLOAD = "{\"entity\":\"event\",\"account_id\":\"acc_test123\",\"event\":\"payment.failed\"}";
    private static final String SECRET = "top_secret_webhook_key";

    @BeforeEach
    void setUp() {
        verifier = new RazorpaySignatureVerifier();
    }

    @Test
    @DisplayName("Valid signature should be accepted")
    void testValidSignatureAccepted() throws Exception {
        String expectedSignature = verifier.calculateHmacSha256(SAMPLE_PAYLOAD, SECRET);
        assertNotNull(expectedSignature);

        assertDoesNotThrow(() -> {
            boolean result = verifier.verifySignature(SAMPLE_PAYLOAD, expectedSignature, SECRET);
            assertTrue(result);
        });
    }

    @Test
    @DisplayName("Invalid signature should be rejected with WebhookSignatureException")
    void testInvalidSignatureRejected() {
        String invalidSignature = "deadbeef1234567890abcdef1234567890abcdef1234567890abcdef1234567890ab";

        assertThrows(WebhookSignatureException.class, () ->
                verifier.verifySignature(SAMPLE_PAYLOAD, invalidSignature, SECRET)
        );
    }

    @Test
    @DisplayName("Tampered/modified payload should be rejected with WebhookSignatureException")
    void testModifiedPayloadRejected() throws Exception {
        String validSignature = verifier.calculateHmacSha256(SAMPLE_PAYLOAD, SECRET);
        String tamperedPayload = "{\"entity\":\"event\",\"account_id\":\"acc_test123\",\"event\":\"payment.captured\"}";

        assertThrows(WebhookSignatureException.class, () ->
                verifier.verifySignature(tamperedPayload, validSignature, SECRET)
        );
    }

    @Test
    @DisplayName("Missing payload should be rejected with WebhookSignatureException")
    void testMissingPayloadRejected() {
        assertThrows(WebhookSignatureException.class, () ->
                verifier.verifySignature("", "any_sig", SECRET)
        );
        assertThrows(WebhookSignatureException.class, () ->
                verifier.verifySignature(null, "any_sig", SECRET)
        );
    }

    @Test
    @DisplayName("Missing signature header should be rejected with WebhookSignatureException")
    void testMissingSignatureHeaderRejected() {
        assertThrows(WebhookSignatureException.class, () ->
                verifier.verifySignature(SAMPLE_PAYLOAD, "", SECRET)
        );
        assertThrows(WebhookSignatureException.class, () ->
                verifier.verifySignature(SAMPLE_PAYLOAD, null, SECRET)
        );
    }

    @Test
    @DisplayName("Missing webhook secret should be rejected with WebhookSignatureException")
    void testMissingSecretRejected() {
        assertThrows(WebhookSignatureException.class, () ->
                verifier.verifySignature(SAMPLE_PAYLOAD, "any_sig", "")
        );
        assertThrows(WebhookSignatureException.class, () ->
                verifier.verifySignature(SAMPLE_PAYLOAD, "any_sig", null)
        );
    }
}
