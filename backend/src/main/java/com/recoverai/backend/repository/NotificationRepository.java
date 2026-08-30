package com.recoverai.backend.repository;

import com.recoverai.backend.entity.Notification;
import com.recoverai.backend.entity.enums.MerchantNotificationEvent;
import com.recoverai.backend.entity.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByMerchantId(UUID merchantId, Pageable pageable);

    Page<Notification> findByMerchantIdAndStatus(UUID merchantId, NotificationStatus status, Pageable pageable);

    Page<Notification> findByMerchantIdAndEventType(UUID merchantId, MerchantNotificationEvent eventType, Pageable pageable);

    Page<Notification> findByMerchantIdAndStatusAndEventType(
            UUID merchantId,
            NotificationStatus status,
            MerchantNotificationEvent eventType,
            Pageable pageable
    );

    Optional<Notification> findByIdAndMerchantId(UUID id, UUID merchantId);

    Optional<Notification> findByMerchantIdAndIdempotencyKey(UUID merchantId, String idempotencyKey);

    long countByMerchantIdAndStatus(UUID merchantId, NotificationStatus status);

    @Modifying
    @Query("UPDATE Notification n SET n.status = :status, n.updatedAt = :now WHERE n.merchant.id = :merchantId AND n.status = 'UNREAD'")
    int markAllUnreadAsReadForMerchant(
            @Param("merchantId") UUID merchantId,
            @Param("status") NotificationStatus status,
            @Param("now") Instant now
    );
}
