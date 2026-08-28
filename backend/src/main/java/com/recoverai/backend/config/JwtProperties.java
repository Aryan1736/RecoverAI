package com.recoverai.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "recoverai.security.jwt")
public class JwtProperties {

    /**
     * Secret key for signing HMAC-SHA256 JWT tokens (minimum 256 bits / 32 bytes).
     */
    private String secret = "recoverai-secure-jwt-signing-secret-key-must-be-at-least-256-bits-long";

    /**
     * Token expiration duration in milliseconds (default 24 hours).
     */
    private long expirationMs = 86400000L;

    /**
     * JWT Issuer.
     */
    private String issuer = "RecoverAI";

    public JwtProperties() {
    }

    public JwtProperties(String secret, long expirationMs, String issuer) {
        this.secret = secret;
        this.expirationMs = expirationMs;
        this.issuer = issuer;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public void setExpirationMs(long expirationMs) {
        this.expirationMs = expirationMs;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
}
