package com.recoverai.backend.security;

import com.recoverai.backend.config.JwtProperties;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.enums.MerchantStatus;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderTest {

    private static final String TEST_SECRET = "recoverai-super-secret-test-key-must-be-at-least-256-bits-long-for-hmac-sha256";
    private JwtProperties jwtProperties;
    private JwtTokenProvider jwtTokenProvider;
    private Merchant testMerchant;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties(TEST_SECRET, 3600000L, "RecoverAITest");
        jwtTokenProvider = new JwtTokenProvider(jwtProperties);

        testMerchant = Merchant.builder()
                .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .name("Acme Corp")
                .email("merchant@acme.com")
                .status(MerchantStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Should generate valid signed JWT token for merchant")
    void testGenerateAndValidateToken() {
        String token = jwtTokenProvider.generateToken(testMerchant);

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals(testMerchant.getId(), jwtTokenProvider.extractMerchantId(token));
        assertEquals("merchant@acme.com", jwtTokenProvider.extractEmail(token));
        assertEquals("Acme Corp", jwtTokenProvider.extractName(token));
        assertFalse(jwtTokenProvider.isTokenExpired(token));
    }

    @Test
    @DisplayName("Should correctly extract claims from token")
    void testExtractClaims() {
        String token = jwtTokenProvider.generateToken(testMerchant);
        Claims claims = jwtTokenProvider.extractAllClaims(token);

        assertEquals("RecoverAITest", claims.getIssuer());
        assertEquals(testMerchant.getId().toString(), claims.getSubject());
        assertEquals("merchant@acme.com", claims.get("email"));
        assertEquals("Acme Corp", claims.get("name"));
        assertEquals("ROLE_MERCHANT", claims.get("role"));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }

    @Test
    @DisplayName("Should reject expired JWT token")
    void testRejectExpiredToken() {
        Instant now = Instant.now().minus(2, ChronoUnit.HOURS);
        Instant expiredAt = now.plus(1, ChronoUnit.HOURS);

        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .issuer("RecoverAITest")
                .subject(testMerchant.getId().toString())
                .claim("merchantId", testMerchant.getId().toString())
                .claim("email", testMerchant.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiredAt))
                .signWith(key)
                .compact();

        assertFalse(jwtTokenProvider.validateToken(expiredToken));
        assertTrue(jwtTokenProvider.isTokenExpired(expiredToken));
    }

    @Test
    @DisplayName("Should reject tampered token with invalid signature")
    void testRejectTamperedToken() {
        String token = jwtTokenProvider.generateToken(testMerchant);
        // Tamper with payload
        String[] parts = token.split("\\.");
        String tamperedToken = parts[0] + "." + parts[1] + "tampered." + parts[2];

        assertFalse(jwtTokenProvider.validateToken(tamperedToken));
    }

    @Test
    @DisplayName("Should reject token signed with different key")
    void testRejectTokenSignedWithDifferentSecret() {
        String otherSecret = "another-secret-key-that-is-at-least-256-bits-long-and-different-from-test";
        SecretKey otherKey = Keys.hmacShaKeyFor(otherSecret.getBytes(StandardCharsets.UTF_8));

        String token = Jwts.builder()
                .issuer("RecoverAITest")
                .subject(testMerchant.getId().toString())
                .claim("merchantId", testMerchant.getId().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(otherKey)
                .compact();

        assertFalse(jwtTokenProvider.validateToken(token));
    }

    @Test
    @DisplayName("Should reject malformed or blank tokens")
    void testRejectMalformedOrBlankTokens() {
        assertFalse(jwtTokenProvider.validateToken(null));
        assertFalse(jwtTokenProvider.validateToken(""));
        assertFalse(jwtTokenProvider.validateToken("   "));
        assertFalse(jwtTokenProvider.validateToken("invalid.token.structure"));
        assertFalse(jwtTokenProvider.validateToken("not-a-jwt"));
    }
}
