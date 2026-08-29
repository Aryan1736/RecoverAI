package com.recoverai.backend.service.link;

import com.recoverai.backend.entity.RecoveryCase;

import java.util.UUID;

public interface RecoveryLinkService {

    String generateRecoveryLink(RecoveryCase recoveryCase);

    String generateRecoveryLink(UUID recoveryCaseId);

    String generateSecureRecoveryLink(RecoveryCase recoveryCase);

    String generateSecureRecoveryLink(RecoveryCase recoveryCase, long expirationSeconds);

    RecoveryLinkToken validateRecoveryToken(String token, UUID expectedMerchantId);
}

