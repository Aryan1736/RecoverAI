package com.recoverai.backend.service.link;

import com.recoverai.backend.config.RecoveryCommunicationProperties;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.RecoveryCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

@Service
public class DefaultRecoveryLinkService implements RecoveryLinkService {

    private static final Logger log = LoggerFactory.getLogger(DefaultRecoveryLinkService.class);
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final RecoveryCommunicationProperties properties;

    public DefaultRecoveryLinkService(RecoveryCommunicationProperties properties) {
        this.properties = properties;
    }

    @Override
    public String generateRecoveryLink(RecoveryCase recoveryCase) {
        Objects.requireNonNull(recoveryCase, "RecoveryCase cannot be null");
        if (properties != null && properties.getLink() != null && properties.getLink().isUseSignedTokens()) {
            return generateSecureRecoveryLink(recoveryCase);
        }
        return generateRecoveryLink(recoveryCase.getId());
    }

    @Override
    public String generateRecoveryLink(UUID recoveryCaseId) {
        Objects.requireNonNull(recoveryCaseId, "RecoveryCaseId cannot be null");
        String baseUrl = getBaseUrl();
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        String link = normalizedBase + recoveryCaseId;
        log.debug("Generated recovery link for caseId={}", recoveryCaseId);
        return link;
    }

    @Override
    public String generateSecureRecoveryLink(RecoveryCase recoveryCase) {
        long expirationSec = properties != null && properties.getLink() != null
                ? properties.getLink().getExpirationSeconds() : 86400L;
        return generateSecureRecoveryLink(recoveryCase, expirationSec);
    }

    @Override
    public String generateSecureRecoveryLink(RecoveryCase recoveryCase, long expirationSeconds) {
        Objects.requireNonNull(recoveryCase, "RecoveryCase cannot be null");
        Merchant merchant = recoveryCase.getMerchant();
        UUID merchantId = merchant != null ? merchant.getId() : UUID.randomUUID();
        UUID caseId = recoveryCase.getId();

        Instant expiresAt = Instant.now().plusSeconds(expirationSeconds);
        String nonce = UUID.randomUUID().toString().substring(0, 8);
        String payload = merchantId + ":" + caseId + ":" + expiresAt.getEpochSecond() + ":" + nonce;

        String secret = resolveSecret();
        String signature = computeHmac(payload, secret);

        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8))
                + "." + signature;

        String baseUrl = getBaseUrl();
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        return normalizedBase + "pay?token=" + token;
    }

    @Override
    public RecoveryLinkToken validateRecoveryToken(String token, UUID expectedMerchantId) {
        if (token == null || token.isBlank()) {
            return RecoveryLinkToken.invalid("Token cannot be blank");
        }

        int dotIndex = token.indexOf('.');
        if (dotIndex <= 0 || dotIndex >= token.length() - 1) {
            return RecoveryLinkToken.invalid("Malformed recovery token format");
        }

        String encodedPayload = token.substring(0, dotIndex);
        String providedSignature = token.substring(dotIndex + 1);

        String payload;
        try {
            payload = new String(Base64.getUrlDecoder().decode(encodedPayload), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return RecoveryLinkToken.invalid("Malformed base64 token payload");
        }

        String secret = resolveSecret();
        String expectedSignature = computeHmac(payload, secret);
        if (!MessageDigest.isEqual(providedSignature.getBytes(StandardCharsets.UTF_8), expectedSignature.getBytes(StandardCharsets.UTF_8))) {
            return RecoveryLinkToken.invalid("Invalid token signature");
        }

        String[] parts = payload.split(":");
        if (parts.length < 4) {
            return RecoveryLinkToken.invalid("Malformed token contents");
        }

        UUID tokenMerchantId;
        UUID tokenCaseId;
        long expiresEpoch;
        try {
            tokenMerchantId = UUID.fromString(parts[0]);
            tokenCaseId = UUID.fromString(parts[1]);
            expiresEpoch = Long.parseLong(parts[2]);
        } catch (Exception e) {
            return RecoveryLinkToken.invalid("Invalid token field encoding: " + e.getMessage());
        }

        Instant expiresAt = Instant.ofEpochSecond(expiresEpoch);
        if (Instant.now().isAfter(expiresAt)) {
            return RecoveryLinkToken.invalid("Recovery token has expired at " + expiresAt);
        }

        if (expectedMerchantId != null && !expectedMerchantId.equals(tokenMerchantId)) {
            return RecoveryLinkToken.invalid("Tenant mismatch: token merchant does not match expected merchant");
        }

        return RecoveryLinkToken.valid(tokenMerchantId, tokenCaseId, expiresAt);
    }

    private String getBaseUrl() {
        String baseUrl = properties != null ? properties.getBaseUrl() : null;
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://pay.recoverai.io/r/";
        }
        return baseUrl;
    }

    private String resolveSecret() {
        if (properties != null && properties.getLink() != null && properties.getLink().getSecret() != null
                && !properties.getLink().getSecret().isBlank()) {
            return properties.getLink().getSecret();
        }
        return "recoverai-secure-recovery-link-secret-token-key";
    }

    private String computeHmac(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKey);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute HMAC-SHA256 signature", e);
        }
    }
}
