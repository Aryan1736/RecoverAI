package com.recoverai.backend.service.link;

import com.recoverai.backend.config.RecoveryCommunicationProperties;
import com.recoverai.backend.entity.RecoveryCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Service
public class DefaultRecoveryLinkService implements RecoveryLinkService {

    private static final Logger log = LoggerFactory.getLogger(DefaultRecoveryLinkService.class);

    private final RecoveryCommunicationProperties properties;

    public DefaultRecoveryLinkService(RecoveryCommunicationProperties properties) {
        this.properties = properties;
    }

    @Override
    public String generateRecoveryLink(RecoveryCase recoveryCase) {
        Objects.requireNonNull(recoveryCase, "RecoveryCase cannot be null");
        return generateRecoveryLink(recoveryCase.getId());
    }

    @Override
    public String generateRecoveryLink(UUID recoveryCaseId) {
        Objects.requireNonNull(recoveryCaseId, "RecoveryCaseId cannot be null");
        String baseUrl = properties != null ? properties.getBaseUrl() : null;
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://pay.recoverai.io/r/";
        }
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        String link = normalizedBase + recoveryCaseId;
        log.debug("Generated recovery link for caseId={}", recoveryCaseId);
        return link;
    }
}
