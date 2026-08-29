package com.recoverai.backend.service.link;

import com.recoverai.backend.config.RecoveryCommunicationProperties;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.RecoveryCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RecoveryLinkSecurityTest {

    private RecoveryCommunicationProperties properties;
    private DefaultRecoveryLinkService linkService;
    private Merchant merchant;
    private RecoveryCase recoveryCase;

    @BeforeEach
    void setUp() {
        properties = new RecoveryCommunicationProperties();
        properties.setBaseUrl("https://pay.recoverai.io/r/");
        properties.getLink().setSecret("secure-test-signing-secret-key-32-chars-long");
        properties.getLink().setExpirationSeconds(3600);

        linkService = new DefaultRecoveryLinkService(properties);

        merchant = Merchant.builder().id(UUID.randomUUID()).name("Merchant Alpha").build();
        recoveryCase = RecoveryCase.builder().id(UUID.randomUUID()).merchant(merchant).build();
    }

    @Test
    @DisplayName("Should generate secure signed recovery link with opaque token")
    void shouldGenerateSecureRecoveryLink() {
        String link = linkService.generateSecureRecoveryLink(recoveryCase);

        assertThat(link).startsWith("https://pay.recoverai.io/r/pay?token=");
        String token = link.substring(link.indexOf("token=") + 6);
        assertThat(token).contains(".");
    }

    @Test
    @DisplayName("Should generate unique tokens across subsequent calls")
    void shouldGenerateUniqueTokens() {
        String link1 = linkService.generateSecureRecoveryLink(recoveryCase);
        String link2 = linkService.generateSecureRecoveryLink(recoveryCase);

        assertThat(link1).isNotEqualTo(link2);
    }

    @Test
    @DisplayName("Should validate unexpired token matching expected tenant")
    void shouldValidateValidToken() {
        String link = linkService.generateSecureRecoveryLink(recoveryCase);
        String token = link.substring(link.indexOf("token=") + 6);

        RecoveryLinkToken validation = linkService.validateRecoveryToken(token, merchant.getId());

        assertThat(validation.isValid()).isTrue();
        assertThat(validation.getMerchantId()).isEqualTo(merchant.getId());
        assertThat(validation.getRecoveryCaseId()).isEqualTo(recoveryCase.getId());
        assertThat(validation.getExpiresAt()).isNotNull();
    }

    @Test
    @DisplayName("Should reject token when tenant mismatch is detected")
    void shouldRejectTenantMismatch() {
        String link = linkService.generateSecureRecoveryLink(recoveryCase);
        String token = link.substring(link.indexOf("token=") + 6);

        UUID differentMerchantId = UUID.randomUUID();
        RecoveryLinkToken validation = linkService.validateRecoveryToken(token, differentMerchantId);

        assertThat(validation.isValid()).isFalse();
        assertThat(validation.getErrorMessage()).contains("Tenant mismatch");
    }

    @Test
    @DisplayName("Should reject expired token")
    void shouldRejectExpiredToken() {
        // Expiration of -10 seconds (in the past)
        String link = linkService.generateSecureRecoveryLink(recoveryCase, -10);
        String token = link.substring(link.indexOf("token=") + 6);

        RecoveryLinkToken validation = linkService.validateRecoveryToken(token, merchant.getId());

        assertThat(validation.isValid()).isFalse();
        assertThat(validation.getErrorMessage()).contains("expired");
    }

    @Test
    @DisplayName("Should reject malformed or tampered token")
    void shouldRejectMalformedOrTamperedToken() {
        RecoveryLinkToken empty = linkService.validateRecoveryToken("", merchant.getId());
        assertThat(empty.isValid()).isFalse();

        RecoveryLinkToken noDot = linkService.validateRecoveryToken("invalidtokenwithoutdot", merchant.getId());
        assertThat(noDot.isValid()).isFalse();

        String link = linkService.generateSecureRecoveryLink(recoveryCase);
        String token = link.substring(link.indexOf("token=") + 6);
        String tamperedToken = token.substring(0, token.length() - 4) + "XXXX";

        RecoveryLinkToken tampered = linkService.validateRecoveryToken(tamperedToken, merchant.getId());
        assertThat(tampered.isValid()).isFalse();
        assertThat(tampered.getErrorMessage()).contains("Invalid token signature");
    }
}
