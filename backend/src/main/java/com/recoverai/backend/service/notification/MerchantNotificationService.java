package com.recoverai.backend.service.notification;

import com.recoverai.backend.config.RecoverAINotificationProperties;
import com.recoverai.backend.dto.notification.NotificationResponseDto;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Notification;
import com.recoverai.backend.entity.NotificationDelivery;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.enums.ActorType;
import com.recoverai.backend.entity.enums.MerchantNotificationChannel;
import com.recoverai.backend.entity.enums.MerchantNotificationEvent;
import com.recoverai.backend.entity.enums.NotificationDeliveryStatus;
import com.recoverai.backend.entity.enums.NotificationStatus;
import com.recoverai.backend.exception.NotificationNotFoundException;
import com.recoverai.backend.repository.NotificationDeliveryRepository;
import com.recoverai.backend.repository.NotificationRepository;
import com.recoverai.backend.service.AuditService;
import com.recoverai.backend.service.notification.channel.NotificationChannelSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class MerchantNotificationService {

    private static final Logger log = LoggerFactory.getLogger(MerchantNotificationService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final MerchantNotificationPreferenceService preferenceService;
    private final AuditService auditService;
    private final RecoverAINotificationProperties properties;
    private final Map<MerchantNotificationChannel, NotificationChannelSender> channelSenders = new EnumMap<>(MerchantNotificationChannel.class);

    public MerchantNotificationService(NotificationRepository notificationRepository,
                                       NotificationDeliveryRepository deliveryRepository,
                                       MerchantNotificationPreferenceService preferenceService,
                                       AuditService auditService,
                                       RecoverAINotificationProperties properties,
                                       List<NotificationChannelSender> senders) {
        this.notificationRepository = notificationRepository;
        this.deliveryRepository = deliveryRepository;
        this.preferenceService = preferenceService;
        this.auditService = auditService;
        this.properties = properties != null ? properties : new RecoverAINotificationProperties();

        if (senders != null) {
            for (NotificationChannelSender sender : senders) {
                if (sender != null && sender.getChannel() != null) {
                    this.channelSenders.put(sender.getChannel(), sender);
                }
            }
        }
    }

    /**
     * Creates and dispatches a merchant notification safely.
     * Uses independent transaction propagation so notification delivery failures never
     * roll back payment reconciliation or queue execution.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification createAndDispatchNotification(Merchant merchant,
                                                      MerchantNotificationEvent eventType,
                                                      String title,
                                                      String message,
                                                      RecoveryCase recoveryCase,
                                                      RecoveryAttempt recoveryAttempt,
                                                      String idempotencyKey,
                                                      String metadata) {
        if (properties != null && !properties.isEnabled()) {
            log.info("Notifications are disabled via configuration; skipping notification creation");
            return null;
        }

        if (merchant == null || merchant.getId() == null) {
            log.warn("Cannot create notification: merchant is null or has no ID");
            return null;
        }

        try {
            // Deduplication via idempotency key
            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                Optional<Notification> existingOpt = notificationRepository
                        .findByMerchantIdAndIdempotencyKey(merchant.getId(), idempotencyKey);
                if (existingOpt.isPresent()) {
                    log.info("Duplicate notification suppressed via idempotency key: {} for merchant {}",
                            idempotencyKey, merchant.getId());
                    return existingOpt.get();
                }
            }

            Instant now = Instant.now();
            Notification notification = Notification.builder()
                    .merchant(merchant)
                    .eventType(eventType)
                    .title(title != null ? title : eventType.getDefaultTitle())
                    .message(message != null ? message : eventType.getDefaultDescription())
                    .recoveryCase(recoveryCase)
                    .recoveryAttempt(recoveryAttempt)
                    .status(NotificationStatus.UNREAD)
                    .idempotencyKey(idempotencyKey)
                    .metadata(metadata)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            notification = notificationRepository.saveAndFlush(notification);

            auditService.recordEvent(
                    merchant,
                    "NOTIFICATION_CREATED",
                    ActorType.SYSTEM,
                    "NotificationEngine",
                    "Notification",
                    notification.getId().toString(),
                    "CREATE_NOTIFICATION",
                    String.format("Created notification %s for event %s", notification.getId(), eventType),
                    null
            );

            // Dispatch across all enabled channels
            int maxRetries = properties != null ? properties.getMaxRetries() : 3;

            for (MerchantNotificationChannel channel : MerchantNotificationChannel.values()) {
                boolean enabled = preferenceService.isChannelEnabled(merchant.getId(), eventType, channel);
                if (!enabled) {
                    continue;
                }

                NotificationChannelSender sender = channelSenders.get(channel);
                if (sender == null) {
                    log.warn("No sender registered for channel {}", channel);
                    continue;
                }

                NotificationDelivery delivery = NotificationDelivery.builder()
                        .notification(notification)
                        .merchant(merchant)
                        .channel(channel)
                        .status(NotificationDeliveryStatus.PENDING)
                        .maxRetries(maxRetries)
                        .retryCount(0)
                        .createdAt(now)
                        .updatedAt(now)
                        .build();

                delivery = deliveryRepository.saveAndFlush(delivery);
                notification.addDelivery(delivery);

                auditService.recordEvent(
                        merchant,
                        "NOTIFICATION_DISPATCH_STARTED",
                        ActorType.SYSTEM,
                        "NotificationEngine",
                        "NotificationDelivery",
                        delivery.getId().toString(),
                        "DISPATCH_START",
                        String.format("Starting dispatch on channel %s for notification %s", channel, notification.getId()),
                        null
                );

                try {
                    delivery = sender.deliver(notification, merchant, delivery);
                    delivery = deliveryRepository.saveAndFlush(delivery);

                    if (delivery.getStatus() == NotificationDeliveryStatus.DELIVERED) {
                        auditService.recordEvent(
                                merchant,
                                "NOTIFICATION_DISPATCH_SUCCEEDED",
                                ActorType.SYSTEM,
                                "NotificationEngine",
                                "NotificationDelivery",
                                delivery.getId().toString(),
                                "DISPATCH_SUCCESS",
                                String.format("Dispatch succeeded on channel %s for notification %s", channel, notification.getId()),
                                null
                        );
                    } else if (delivery.getStatus() == NotificationDeliveryStatus.RETRYING) {
                        auditService.recordEvent(
                                merchant,
                                "NOTIFICATION_RETRY_SCHEDULED",
                                ActorType.SYSTEM,
                                "NotificationEngine",
                                "NotificationDelivery",
                                delivery.getId().toString(),
                                "RETRY_SCHEDULED",
                                String.format("Dispatch retry scheduled on channel %s: %s", channel, delivery.getErrorMessage()),
                                null
                        );
                    } else {
                        auditService.recordEvent(
                                merchant,
                                "NOTIFICATION_DISPATCH_FAILED",
                                ActorType.SYSTEM,
                                "NotificationEngine",
                                "NotificationDelivery",
                                delivery.getId().toString(),
                                "DISPATCH_FAILED",
                                String.format("Dispatch failed permanently on channel %s: %s", channel, delivery.getErrorMessage()),
                                null
                        );
                    }
                } catch (Exception dispatchEx) {
                    log.error("Unhandled error dispatching notification id={} via {}: {}",
                            notification.getId(), channel, dispatchEx.getMessage(), dispatchEx);
                    delivery.setStatus(NotificationDeliveryStatus.FAILED);
                    delivery.setErrorMessage("Dispatch error: " + dispatchEx.getMessage());
                    deliveryRepository.saveAndFlush(delivery);
                }
            }

            return notification;
        } catch (Exception ex) {
            log.error("Error creating or dispatching notification for merchant {}: {}",
                    merchant.getId(), ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * Triggers PAYMENT_RECOVERED notification.
     */
    public Notification notifyPaymentRecovered(Merchant merchant, RecoveryCase recoveryCase, BigDecimal amount) {
        if (merchant == null || recoveryCase == null) {
            return null;
        }
        String currency = recoveryCase.getCurrency() != null ? recoveryCase.getCurrency() : "INR";
        BigDecimal effectiveAmount = amount != null ? amount
                : (recoveryCase.getRecoveredAmount() != null ? recoveryCase.getRecoveredAmount() : BigDecimal.ZERO);

        String idempotencyKey = "PAYMENT_RECOVERED:case:" + recoveryCase.getId();
        String title = String.format("Payment Recovered: %s %s", currency, effectiveAmount);
        String message = String.format("Payment for case %s was successfully recovered in the amount of %s %s.",
                recoveryCase.getId(), currency, effectiveAmount);

        String metadata = String.format("{\"caseId\":\"%s\",\"amount\":%s,\"currency\":\"%s\"}",
                recoveryCase.getId(), effectiveAmount, currency);

        return createAndDispatchNotification(
                merchant,
                MerchantNotificationEvent.PAYMENT_RECOVERED,
                title,
                message,
                recoveryCase,
                null,
                idempotencyKey,
                metadata
        );
    }

    /**
     * Triggers CASE_EXHAUSTED notification.
     */
    public Notification notifyCaseExhausted(Merchant merchant, RecoveryCase recoveryCase, String reason) {
        if (merchant == null || recoveryCase == null) {
            return null;
        }
        String idempotencyKey = "CASE_EXHAUSTED:case:" + recoveryCase.getId();
        String title = "Recovery Case Exhausted";
        String message = String.format("Recovery case %s exhausted all retry attempts and fallback channels. Reason: %s",
                recoveryCase.getId(), reason != null ? reason : "Maximum attempts reached");

        String metadata = String.format("{\"caseId\":\"%s\",\"reason\":\"%s\"}",
                recoveryCase.getId(), reason != null ? reason : "EXHAUSTED");

        return createAndDispatchNotification(
                merchant,
                MerchantNotificationEvent.CASE_EXHAUSTED,
                title,
                message,
                recoveryCase,
                null,
                idempotencyKey,
                metadata
        );
    }

    /**
     * Triggers HIGH_PRIORITY_FAILURE notification.
     */
    public Notification notifyHighPriorityFailure(Merchant merchant, RecoveryCase recoveryCase,
                                                  RecoveryAttempt attempt, String error) {
        if (merchant == null || recoveryCase == null) {
            return null;
        }
        UUID attemptId = attempt != null ? attempt.getId() : null;
        String idempotencyKey = "HIGH_PRIORITY_FAILURE:case:" + recoveryCase.getId()
                + (attemptId != null ? ":attempt:" + attemptId : "");

        String title = "High Priority Case Failed";
        String channelName = attempt != null && attempt.getChannel() != null ? attempt.getChannel().name() : "DISPATCH";
        String message = String.format("High priority recovery case %s failed on channel %s: %s",
                recoveryCase.getId(), channelName, error != null ? error : "Unknown error");

        String metadata = String.format("{\"caseId\":\"%s\",\"priority\":\"%s\",\"channel\":\"%s\",\"error\":\"%s\"}",
                recoveryCase.getId(), recoveryCase.getPriority(), channelName, error != null ? error : "FAILED");

        return createAndDispatchNotification(
                merchant,
                MerchantNotificationEvent.HIGH_PRIORITY_FAILURE,
                title,
                message,
                recoveryCase,
                attempt,
                idempotencyKey,
                metadata
        );
    }

    /**
     * Triggers PROVIDER_DEGRADED notification.
     */
    public Notification notifyProviderDegraded(Merchant merchant, String providerName,
                                               String category, String reason, String cooldownBucket) {
        if (merchant == null || providerName == null) {
            return null;
        }
        String idempotencyKey = "PROVIDER_DEGRADED:" + providerName.toUpperCase() + ":" + cooldownBucket;
        String title = "Provider Degraded: " + providerName;
        String message = String.format("Upstream provider %s (%s) is experiencing degraded health: %s",
                providerName, category != null ? category : "COMMUNICATION", reason != null ? reason : "Health check failed");

        String metadata = String.format("{\"provider\":\"%s\",\"category\":\"%s\",\"reason\":\"%s\"}",
                providerName, category != null ? category : "UNKNOWN", reason != null ? reason : "DEGRADED");

        return createAndDispatchNotification(
                merchant,
                MerchantNotificationEvent.PROVIDER_DEGRADED,
                title,
                message,
                null,
                null,
                idempotencyKey,
                metadata
        );
    }

    /**
     * Lists notifications for a merchant with optional filtering and pagination.
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponseDto> getNotifications(UUID merchantId,
                                                          Boolean unreadOnly,
                                                          MerchantNotificationEvent event,
                                                          Pageable pageable) {
        Page<Notification> page;
        boolean filterUnread = Boolean.TRUE.equals(unreadOnly);

        if (filterUnread && event != null) {
            page = notificationRepository.findByMerchantIdAndStatusAndEventType(merchantId, NotificationStatus.UNREAD, event, pageable);
        } else if (filterUnread) {
            page = notificationRepository.findByMerchantIdAndStatus(merchantId, NotificationStatus.UNREAD, pageable);
        } else if (event != null) {
            page = notificationRepository.findByMerchantIdAndEventType(merchantId, event, pageable);
        } else {
            page = notificationRepository.findByMerchantId(merchantId, pageable);
        }

        return page.map(NotificationResponseDto::fromEntity);
    }

    /**
     * Retrieves a single notification belonging to the merchant.
     */
    @Transactional(readOnly = true)
    public NotificationResponseDto getNotification(UUID merchantId, UUID notificationId) {
        Notification notification = notificationRepository.findByIdAndMerchantId(notificationId, merchantId)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found: " + notificationId));
        return NotificationResponseDto.fromEntity(notification);
    }

    /**
     * Marks a notification as READ.
     */
    @Transactional
    public NotificationResponseDto markAsRead(UUID merchantId, UUID notificationId, String actor) {
        Notification notification = notificationRepository.findByIdAndMerchantId(notificationId, merchantId)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found: " + notificationId));

        if (notification.getStatus() != NotificationStatus.READ) {
            notification.setStatus(NotificationStatus.READ);
            notification.setUpdatedAt(Instant.now());
            notification = notificationRepository.save(notification);

            auditService.recordEvent(
                    notification.getMerchant(),
                    "NOTIFICATION_READ",
                    ActorType.USER,
                    actor != null ? actor : merchantId.toString(),
                    "Notification",
                    notification.getId().toString(),
                    "MARK_READ",
                    "Notification marked as read",
                    null
            );
        }

        return NotificationResponseDto.fromEntity(notification);
    }

    /**
     * Marks all unread notifications for a merchant as READ.
     */
    @Transactional
    public int markAllAsRead(UUID merchantId, String actor) {
        Instant now = Instant.now();
        int updated = notificationRepository.markAllUnreadAsReadForMerchant(merchantId, NotificationStatus.READ, now);

        if (updated > 0) {
            auditService.recordEvent(
                    null,
                    "NOTIFICATION_READ_ALL",
                    ActorType.USER,
                    actor != null ? actor : merchantId.toString(),
                    "Merchant",
                    merchantId.toString(),
                    "MARK_ALL_READ",
                    String.format("Marked %d notifications as read", updated),
                    null
            );
        }
        return updated;
    }

    /**
     * Retries pending/retrying deliveries with exponential backoff.
     */
    @Transactional
    public int retryPendingDeliveries() {
        int maxRetries = properties != null ? properties.getMaxRetries() : 3;
        List<NotificationDelivery> retryingDeliveries = deliveryRepository.findByStatusInAndRetryCountLessThan(
                List.of(NotificationDeliveryStatus.RETRYING),
                maxRetries,
                Pageable.ofSize(50)
        );

        int processed = 0;
        Instant now = Instant.now();

        for (NotificationDelivery delivery : retryingDeliveries) {
            // Exponential backoff check: base delay * 2^(retryCount - 1)
            long baseDelay = properties != null ? properties.getRetryDelaySeconds() : 300L;
            long delay = baseDelay * (1L << Math.max(0, delivery.getRetryCount() - 1));
            Instant eligibleAt = delivery.getAttemptedAt() != null ? delivery.getAttemptedAt().plusSeconds(delay) : now;

            if (now.isBefore(eligibleAt)) {
                continue;
            }

            NotificationChannelSender sender = channelSenders.get(delivery.getChannel());
            if (sender == null) {
                continue;
            }

            Notification notification = delivery.getNotification();
            Merchant merchant = delivery.getMerchant();
            if (notification == null || merchant == null) {
                continue;
            }

            log.info("Retrying delivery id={} for notification id={} on channel {}",
                    delivery.getId(), notification.getId(), delivery.getChannel());

            delivery = sender.deliver(notification, merchant, delivery);
            deliveryRepository.saveAndFlush(delivery);
            processed++;
        }

        return processed;
    }
}
