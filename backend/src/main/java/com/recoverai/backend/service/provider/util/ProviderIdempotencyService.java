package com.recoverai.backend.service.provider.util;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

@Service
public class ProviderIdempotencyService {

    private static final String PREFIX = "rec_idem_";

    public String generateKey(UUID merchantId, UUID recoveryAttemptId) {
        Objects.requireNonNull(merchantId, "merchantId cannot be null");
        Objects.requireNonNull(recoveryAttemptId, "recoveryAttemptId cannot be null");

        String raw = merchantId + ":" + recoveryAttemptId;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return PREFIX + HexFormat.of().formatHex(hash).substring(0, 32);
        } catch (NoSuchAlgorithmException e) {
            // Fallback to direct string representation if SHA-256 is unavailable
            return PREFIX + merchantId + "_" + recoveryAttemptId;
        }
    }

    public String generateKey(UUID merchantId, UUID recoveryAttemptId, String channelOrProvider) {
        Objects.requireNonNull(merchantId, "merchantId cannot be null");
        Objects.requireNonNull(recoveryAttemptId, "recoveryAttemptId cannot be null");

        String scope = channelOrProvider != null ? channelOrProvider.trim().toLowerCase() : "default";
        String raw = merchantId + ":" + recoveryAttemptId + ":" + scope;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return PREFIX + HexFormat.of().formatHex(hash).substring(0, 32);
        } catch (NoSuchAlgorithmException e) {
            return PREFIX + merchantId + "_" + recoveryAttemptId + "_" + scope;
        }
    }
}
