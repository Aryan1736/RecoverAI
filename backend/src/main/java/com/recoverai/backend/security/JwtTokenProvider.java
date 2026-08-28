package com.recoverai.backend.security;

import com.recoverai.backend.config.JwtProperties;
import com.recoverai.backend.entity.Merchant;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Merchant merchant) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(jwtProperties.getExpirationMs());

        return Jwts.builder()
                .issuer(jwtProperties.getIssuer())
                .subject(merchant.getId().toString())
                .claim("merchantId", merchant.getId().toString())
                .claim("email", merchant.getEmail())
                .claim("name", merchant.getName())
                .claim("role", "ROLE_MERCHANT")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("Invalid JWT signature or format: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty or invalid: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
        }
        return false;
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID extractMerchantId(String token) {
        Claims claims = extractAllClaims(token);
        String merchantIdStr = claims.get("merchantId", String.class);
        if (merchantIdStr != null && !merchantIdStr.isBlank()) {
            return UUID.fromString(merchantIdStr);
        }
        return UUID.fromString(claims.getSubject());
    }

    public String extractEmail(String token) {
        Claims claims = extractAllClaims(token);
        String email = claims.get("email", String.class);
        if (email != null && !email.isBlank()) {
            return email;
        }
        return claims.getSubject();
    }

    public String extractName(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("name", String.class);
    }

    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            return true;
        }
    }
}
