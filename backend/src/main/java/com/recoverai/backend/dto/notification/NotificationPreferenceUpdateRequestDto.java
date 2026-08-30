package com.recoverai.backend.dto.notification;

import com.recoverai.backend.entity.enums.MerchantNotificationChannel;
import com.recoverai.backend.entity.enums.MerchantNotificationEvent;

import java.util.EnumMap;
import java.util.Map;

public class NotificationPreferenceUpdateRequestDto {

    private String webhookUrl;
    private Map<MerchantNotificationEvent, Map<MerchantNotificationChannel, Boolean>> preferences = new EnumMap<>(MerchantNotificationEvent.class);

    public NotificationPreferenceUpdateRequestDto() {
    }

    public NotificationPreferenceUpdateRequestDto(String webhookUrl,
                                                  Map<MerchantNotificationEvent, Map<MerchantNotificationChannel, Boolean>> preferences) {
        this.webhookUrl = webhookUrl;
        this.preferences = preferences != null ? preferences : new EnumMap<>(MerchantNotificationEvent.class);
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public Map<MerchantNotificationEvent, Map<MerchantNotificationChannel, Boolean>> getPreferences() {
        return preferences;
    }

    public void setPreferences(Map<MerchantNotificationEvent, Map<MerchantNotificationChannel, Boolean>> preferences) {
        this.preferences = preferences != null ? preferences : new EnumMap<>(MerchantNotificationEvent.class);
    }
}
