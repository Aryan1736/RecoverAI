package com.recoverai.backend.repository;

import com.recoverai.backend.entity.WebhookEvent;
import com.recoverai.backend.entity.enums.WebhookProcessingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {

    List<WebhookEvent> findByMerchantId(UUID merchantId);

    Page<WebhookEvent> findByMerchantId(UUID merchantId, Pageable pageable);

    Optional<WebhookEvent> findByMerchantIdAndRazorpayEventId(UUID merchantId, String razorpayEventId);

    Optional<WebhookEvent> findByMerchantIdAndPayloadHash(UUID merchantId, String payloadHash);

    boolean existsByMerchantIdAndRazorpayEventId(UUID merchantId, String razorpayEventId);

    boolean existsByMerchantIdAndPayloadHash(UUID merchantId, String payloadHash);

    List<WebhookEvent> findByMerchantIdAndProcessingStatus(UUID merchantId, WebhookProcessingStatus status);
}
