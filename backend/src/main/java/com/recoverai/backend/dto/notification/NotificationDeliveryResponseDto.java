package com.recoverai.backend.dto.notification;

import com.recoverai.backend.entity.NotificationDelivery;
import com.recoverai.backend.entity.enums.MerchantNotificationChannel;
import com.recoverai.backend.entity.enums.NotificationDeliveryStatus;

import java.time.Instant;
import java.util.UUID;

public class NotificationDeliveryResponseDto {

    private UUID id;
    private MerchantNotificationChannel channel;
    private String provider;
    private NotificationDeliveryStatus status;
    private Instant attemptedAt;
    private Instant deliveredAt;
    private String errorCode;
    private String errorMessage;
    private int retryCount;

    public NotificationDeliveryResponseDto() {
    }

    public NotificationDeliveryResponseDto(UUID id, MerchantNotificationChannel channel, String provider,
                                           NotificationDeliveryStatus status, Instant attemptedAt,
                                           Instant deliveredAt, String errorCode, String errorMessage,
                                           int retryCount) {
        this.id = id;
        this.channel = channel;
        this.provider = provider;
        this.status = status;
        this.attemptedAt = attemptedAt;
        this.deliveredAt = deliveredAt;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.retryCount = retryCount;
    }

    public static NotificationDeliveryResponseDto fromEntity(NotificationDelivery entity) {
        if (entity == null) {
            return null;
        }
        return new NotificationDeliveryResponseDto(
                entity.getId(),
                entity.getChannel(),
                entity.getProvider(),
                entity.getStatus(),
                entity.getAttemptedAt(),
                entity.getDeliveredAt(),
                entity.getErrorCode(),
                entity.getErrorMessage(),
                entity.getRetryCount()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public MerchantNotificationChannel getChannel() {
        return channel;
    }

    public void setChannel(MerchantNotificationChannel channel) {
        this.channel = channel;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public NotificationDeliveryStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationDeliveryStatus status) {
        this.status = status;
    }

    public Instant getAttemptedAt() {
        return attemptedAt;
    }

    public void setAttemptedAt(Instant attemptedAt) {
        this.attemptedAt = attemptedAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Instant deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
}
