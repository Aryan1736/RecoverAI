package com.recoverai.backend.security;

import com.recoverai.backend.exception.WebhookSignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class RecoveryOutcomeSignatureVerifier {

    private static final Logger log = LoggerFactory.getLogger(RecoveryOutcomeSignatureVerifier.class);
    private static final String HMAC_SHA256 = "HmacSHA256";

    /**
     * Verifies the authenticity of an incoming Recovery Outcome webhook payload using HMAC-SHA256
     * signature verification against the merchant's configured secret.
     *
     * @param payload         The raw request body payload
     * @param signatureHeader The signature header value (e.g. X-Recovery-Signature)
     * @param webhookSecret   The secret key configured for the merchant
     * @return true if valid, throws WebhookSignatureException if invalid
     */
    public boolean verifySignature(String payload, String signatureHeader, String webhookSecret) {
        if (payload == null || payload.isBlank()) {
            log.warn("Recovery outcome webhook signature verification failed: payload is null or blank");
            throw new WebhookSignatureException("Invalid webhook payload: payload is empty");
        }

        if (signatureHeader == null || signatureHeader.isBlank()) {
            log.warn("Recovery outcome webhook signature verification failed: signature header is missing or blank");
            throw new WebhookSignatureException("Missing webhook signature header");
        }

        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.warn("Recovery outcome webhook signature verification failed: merchant webhook secret is not configured");
            throw new WebhookSignatureException("Merchant webhook secret is not configured");
        }

        try {
            String calculatedSignature = calculateHmacSha256(payload, webhookSecret);
            byte[] expectedBytes = signatureHeader.trim().getBytes(StandardCharsets.UTF_8);
            byte[] calculatedBytes = calculatedSignature.getBytes(StandardCharsets.UTF_8);

            // Constant-time comparison to protect against timing attacks
            boolean matches = MessageDigest.isEqual(expectedBytes, calculatedBytes);

            if (!matches) {
                log.warn("Recovery outcome webhook signature verification failed: signature mismatch");
                throw new WebhookSignatureException("Invalid webhook signature");
            }

            return true;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Crypto failure during webhook signature computation: {}", e.getMessage());
            throw new WebhookSignatureException("Failed to verify webhook signature due to cryptographic error", e);
        }
    }

    /**
     * Calculates the HMAC-SHA256 hex string for a given payload and secret.
     */
    public String calculateHmacSha256(String payload, String secret)
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}
