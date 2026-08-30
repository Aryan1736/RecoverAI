package com.recoverai.backend.service.notification.channel;

import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Notification;
import com.recoverai.backend.entity.NotificationDelivery;
import com.recoverai.backend.entity.enums.MerchantNotificationChannel;

/**
 * Strategy interface for delivering merchant notifications over a specific channel.
 */
public interface NotificationChannelSender {

    /**
     * The notification channel supported by this sender.
     */
    MerchantNotificationChannel getChannel();

    /**
     * Delivers the notification to the merchant through this channel.
     *
     * @param notification the persisted notification entity
     * @param merchant     the target merchant
     * @param delivery     the delivery tracking record
     * @return the updated delivery record
     */
    NotificationDelivery deliver(Notification notification, Merchant merchant, NotificationDelivery delivery);
}
