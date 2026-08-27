package com.recoverai.backend.service.link;

import com.recoverai.backend.entity.RecoveryCase;

import java.util.UUID;

public interface RecoveryLinkService {

    String generateRecoveryLink(RecoveryCase recoveryCase);

    String generateRecoveryLink(UUID recoveryCaseId);
}
