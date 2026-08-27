package com.recoverai.backend.service.link;

import com.recoverai.backend.config.RecoveryCommunicationProperties;
import com.recoverai.backend.entity.RecoveryCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecoveryLinkServiceTest {

    private RecoveryCommunicationProperties properties;
    private RecoveryLinkService linkService;

    @BeforeEach
    void setUp() {
        properties = new RecoveryCommunicationProperties();
        properties.setBaseUrl("https://pay.recoverai.io/r/");
        linkService = new DefaultRecoveryLinkService(properties);
    }

    @Test
    @DisplayName("Should generate valid recovery link from RecoveryCase entity")
    void shouldGenerateLinkFromRecoveryCase() {
        UUID caseId = UUID.randomUUID();
        RecoveryCase recoveryCase = RecoveryCase.builder()
                .id(caseId)
                .build();

        String link = linkService.generateRecoveryLink(recoveryCase);

        assertThat(link).isEqualTo("https://pay.recoverai.io/r/" + caseId);
    }

    @Test
    @DisplayName("Should normalize base URL without trailing slash")
    void shouldNormalizeBaseUrlWithoutTrailingSlash() {
        properties.setBaseUrl("https://pay.recoverai.io/r");
        linkService = new DefaultRecoveryLinkService(properties);

        UUID caseId = UUID.randomUUID();
        String link = linkService.generateRecoveryLink(caseId);

        assertThat(link).isEqualTo("https://pay.recoverai.io/r/" + caseId);
    }

    @Test
    @DisplayName("Should use default fallback URL if baseUrl is empty or null")
    void shouldUseDefaultIfEmpty() {
        properties.setBaseUrl("");
        linkService = new DefaultRecoveryLinkService(properties);

        UUID caseId = UUID.randomUUID();
        String link = linkService.generateRecoveryLink(caseId);

        assertThat(link).isEqualTo("https://pay.recoverai.io/r/" + caseId);
    }

    @Test
    @DisplayName("Should throw NullPointerException when argument is null")
    void shouldThrowWhenNull() {
        assertThatThrownBy(() -> linkService.generateRecoveryLink((RecoveryCase) null))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> linkService.generateRecoveryLink((UUID) null))
                .isInstanceOf(NullPointerException.class);
    }
}
