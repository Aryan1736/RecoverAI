package com.recoverai.backend.repository;

import com.recoverai.backend.entity.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    List<AuditEvent> findByMerchantId(UUID merchantId);

    Page<AuditEvent> findByMerchantId(UUID merchantId, Pageable pageable);

    List<AuditEvent> findByEntityTypeAndEntityId(String entityType, String entityId);

    Page<AuditEvent> findByEntityTypeAndEntityId(String entityType, String entityId, Pageable pageable);

    List<AuditEvent> findByMerchantIdAndEventType(UUID merchantId, String eventType);

    Page<AuditEvent> findByMerchantIdAndEventType(UUID merchantId, String eventType, Pageable pageable);
}
