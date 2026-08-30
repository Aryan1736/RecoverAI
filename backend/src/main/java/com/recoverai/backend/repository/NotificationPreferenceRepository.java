package com.recoverai.backend.repository;

import com.recoverai.backend.entity.NotificationPreference;
import com.recoverai.backend.entity.enums.MerchantNotificationChannel;
import com.recoverai.backend.entity.enums.MerchantNotificationEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {

    List<NotificationPreference> findByMerchantId(UUID merchantId);

    List<NotificationPreference> findByMerchantIdAndEventType(UUID merchantId, MerchantNotificationEvent eventType);

    Optional<NotificationPreference> findByMerchantIdAndEventTypeAndChannel(
            UUID merchantId,
            MerchantNotificationEvent eventType,
            MerchantNotificationChannel channel
    );
}
