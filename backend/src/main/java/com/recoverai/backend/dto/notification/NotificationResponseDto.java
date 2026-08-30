package com.recoverai.backend.dto.notification;

import com.recoverai.backend.entity.Notification;
import com.recoverai.backend.entity.enums.MerchantNotificationEvent;
import com.recoverai.backend.entity.enums.NotificationStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class NotificationResponseDto {

    private UUID id;
    private UUID merchantId;
    private MerchantNotificationEvent eventType;
    private String title;
    private String message;
    private NotificationStatus status;
    private boolean read;
    private UUID recoveryCaseId;
    private UUID recoveryAttemptId;
    private String metadata;
    private Instant createdAt;
    private Instant updatedAt;
    private List<NotificationDeliveryResponseDto> deliveries = new ArrayList<>();

    public NotificationResponseDto() {
    }

    public NotificationResponseDto(UUID id, UUID merchantId, MerchantNotificationEvent eventType,
                                   String title, String message, NotificationStatus status,
                                   boolean read, UUID recoveryCaseId, UUID recoveryAttemptId,
                                   String metadata, Instant createdAt, Instant updatedAt,
                                   List<NotificationDeliveryResponseDto> deliveries) {
        this.id = id;
        this.merchantId = merchantId;
        this.eventType = eventType;
        this.title = title;
        this.message = message;
        this.status = status;
        this.read = read;
        this.recoveryCaseId = recoveryCaseId;
        this.recoveryAttemptId = recoveryAttemptId;
        this.metadata = metadata;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deliveries = deliveries != null ? deliveries : new ArrayList<>();
    }

    public static NotificationResponseDto fromEntity(Notification entity) {
        if (entity == null) {
            return null;
        }
        boolean isRead = entity.getStatus() == NotificationStatus.READ;
        UUID caseId = entity.getRecoveryCase() != null ? entity.getRecoveryCase().getId() : null;
        UUID attemptId = entity.getRecoveryAttempt() != null ? entity.getRecoveryAttempt().getId() : null;
        UUID merchantId = entity.getMerchant() != null ? entity.getMerchant().getId() : null;

        List<NotificationDeliveryResponseDto> deliveryDtos = entity.getDeliveries() != null
                ? entity.getDeliveries().stream()
                .map(NotificationDeliveryResponseDto::fromEntity)
                .collect(Collectors.toList())
                : new ArrayList<>();

        return new NotificationResponseDto(
                entity.getId(),
                merchantId,
                entity.getEventType(),
                entity.getTitle(),
                entity.getMessage(),
                entity.getStatus(),
                isRead,
                caseId,
                attemptId,
                entity.getMetadata(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                deliveryDtos
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(UUID merchantId) {
        this.merchantId = merchantId;
    }

    public MerchantNotificationEvent getEventType() {
        return eventType;
    }

    public void setEventType(MerchantNotificationEvent eventType) {
        this.eventType = eventType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
        this.read = status == NotificationStatus.READ;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public UUID getRecoveryCaseId() {
        return recoveryCaseId;
    }

    public void setRecoveryCaseId(UUID recoveryCaseId) {
        this.recoveryCaseId = recoveryCaseId;
    }

    public UUID getRecoveryAttemptId() {
        return recoveryAttemptId;
    }

    public void setRecoveryAttemptId(UUID recoveryAttemptId) {
        this.recoveryAttemptId = recoveryAttemptId;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<NotificationDeliveryResponseDto> getDeliveries() {
        return deliveries;
    }

    public void setDeliveries(List<NotificationDeliveryResponseDto> deliveries) {
        this.deliveries = deliveries != null ? deliveries : new ArrayList<>();
    }
}
