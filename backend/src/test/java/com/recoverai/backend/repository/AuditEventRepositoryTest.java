package com.recoverai.backend.repository;

import com.recoverai.backend.entity.AuditEvent;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.enums.ActorType;
import com.recoverai.backend.entity.enums.MerchantStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class AuditEventRepositoryTest {

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    private Merchant merchant;

    @BeforeEach
    void setUp() {
        merchant = merchantRepository.saveAndFlush(Merchant.builder()
                .name("Audit Merchant")
                .email("audit_" + UUID.randomUUID() + "@test.com")
                .status(MerchantStatus.ACTIVE)
                .build());
    }

    @Test
    @DisplayName("Should persist and retrieve immutable audit event")
    void testCreateAndFindAuditEvent() {
        UUID entityId = UUID.randomUUID();
        AuditEvent event = AuditEvent.builder()
                .merchant(merchant)
                .eventType("PAYMENT_RECOVERY_TRIGGERED")
                .actorType(ActorType.AGENT)
                .actorId("gemini-3.7-flash")
                .entityType("RECOVERY_CASE")
                .entityId(entityId.toString())
                .action("EXECUTE_WORKFLOW")
                .details("Automated recovery action dispatched via WhatsApp")
                .ipAddress("192.168.1.100")
                .build();

        AuditEvent saved = auditEventRepository.saveAndFlush(event);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());

        Optional<AuditEvent> found = auditEventRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("PAYMENT_RECOVERY_TRIGGERED", found.get().getEventType());
        assertEquals(ActorType.AGENT, found.get().getActorType());
        assertEquals("gemini-3.7-flash", found.get().getActorId());
        assertEquals(entityId.toString(), found.get().getEntityId());
        assertEquals("192.168.1.100", found.get().getIpAddress());
    }

    @Test
    @DisplayName("Should find audit events by entity type and ID")
    void testFindByEntityTypeAndEntityId() {
        String entityId = UUID.randomUUID().toString();
        AuditEvent e1 = AuditEvent.builder()
                .merchant(merchant)
                .eventType("CASE_CREATED")
                .actorType(ActorType.SYSTEM)
                .entityType("RECOVERY_CASE")
                .entityId(entityId)
                .action("CREATE")
                .build();
        AuditEvent e2 = AuditEvent.builder()
                .merchant(merchant)
                .eventType("CASE_STATUS_CHANGED")
                .actorType(ActorType.USER)
                .entityType("RECOVERY_CASE")
                .entityId(entityId)
                .action("UPDATE")
                .build();

        auditEventRepository.saveAndFlush(e1);
        auditEventRepository.saveAndFlush(e2);

        List<AuditEvent> events = auditEventRepository.findByEntityTypeAndEntityId("RECOVERY_CASE", entityId);
        assertEquals(2, events.size());
    }
}
