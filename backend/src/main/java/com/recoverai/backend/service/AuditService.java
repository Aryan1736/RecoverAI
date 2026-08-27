package com.recoverai.backend.service;

import com.recoverai.backend.entity.AuditEvent;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.enums.ActorType;
import com.recoverai.backend.repository.AuditEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional
    public AuditEvent recordEvent(Merchant merchant, String eventType, ActorType actorType,
                                  String actorId, String entityType, String entityId,
                                  String action, String details, String ipAddress) {
        try {
            AuditEvent event = AuditEvent.builder()
                    .merchant(merchant)
                    .eventType(eventType)
                    .actorType(actorType != null ? actorType : ActorType.WEBHOOK)
                    .actorId(actorId != null ? actorId : "RazorpayWebhook")
                    .entityType(entityType)
                    .entityId(entityId != null ? entityId : "UNKNOWN")
                    .action(action)
                    .details(details)
                    .ipAddress(ipAddress)
                    .build();

            AuditEvent saved = auditEventRepository.save(event);
            log.debug("Recorded audit event: type={}, entityType={}, entityId={}", eventType, entityType, entityId);
            return saved;
        } catch (Exception e) {
            log.error("Failed to record audit event: type={}, error={}", eventType, e.getMessage());
            return null;
        }
    }
}
