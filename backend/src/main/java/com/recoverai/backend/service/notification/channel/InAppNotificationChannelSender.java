package com.recoverai.backend.service.notification.channel;

import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Notification;
import com.recoverai.backend.entity.NotificationDelivery;
import com.recoverai.backend.entity.enums.MerchantNotificationChannel;
import com.recoverai.backend.entity.enums.NotificationDeliveryStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class InAppNotificationChannelSender implements NotificationChannelSender {

    @Override
    public MerchantNotificationChannel getChannel() {
        return MerchantNotificationChannel.IN_APP;
    }

    @Override
    public NotificationDelivery deliver(Notification notification, Merchant merchant, NotificationDelivery delivery) {
        Instant now = Instant.now();
        delivery.setAttemptedAt(now);
        delivery.setDeliveredAt(now);
        delivery.setProvider("IN_APP");
        delivery.setStatus(NotificationDeliveryStatus.DELIVERED);
        delivery.setRetryCount(delivery.getRetryCount() + 1);
        delivery.setErrorCode(null);
        delivery.setErrorMessage(null);
        return delivery;
    }
}
