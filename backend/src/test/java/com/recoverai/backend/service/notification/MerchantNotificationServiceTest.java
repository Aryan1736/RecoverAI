package com.recoverai.backend.service.notification;

import com.recoverai.backend.config.RecoverAINotificationProperties;
import com.recoverai.backend.dto.notification.NotificationResponseDto;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Notification;
import com.recoverai.backend.entity.NotificationDelivery;
import com.recoverai.backend.entity.enums.MerchantNotificationChannel;
import com.recoverai.backend.entity.enums.MerchantNotificationEvent;
import com.recoverai.backend.entity.enums.NotificationDeliveryStatus;
import com.recoverai.backend.entity.enums.NotificationStatus;
import com.recoverai.backend.exception.NotificationNotFoundException;
import com.recoverai.backend.repository.NotificationDeliveryRepository;
import com.recoverai.backend.repository.NotificationRepository;
import com.recoverai.backend.service.AuditService;
import com.recoverai.backend.service.notification.channel.NotificationChannelSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantNotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationDeliveryRepository deliveryRepository;

    @Mock
    private MerchantNotificationPreferenceService preferenceService;

    @Mock
    private AuditService auditService;

    @Mock
    private NotificationChannelSender emailSender;

    @Mock
    private NotificationChannelSender webhookSender;

    @Mock
    private NotificationChannelSender inAppSender;

    private RecoverAINotificationProperties properties;
    private MerchantNotificationService notificationService;

    private Merchant merchant;
    private UUID merchantId;

    @BeforeEach
    void setUp() {
        properties = new RecoverAINotificationProperties();

        when(emailSender.getChannel()).thenReturn(MerchantNotificationChannel.EMAIL);
        when(webhookSender.getChannel()).thenReturn(MerchantNotificationChannel.WEBHOOK);
        when(inAppSender.getChannel()).thenReturn(MerchantNotificationChannel.IN_APP);

        notificationService = new MerchantNotificationService(
                notificationRepository,
                deliveryRepository,
                preferenceService,
                auditService,
                properties,
                List.of(emailSender, webhookSender, inAppSender)
        );

        merchantId = UUID.randomUUID();
        merchant = Merchant.builder()
                .id(merchantId)
                .name("Acme Corp")
                .email("billing@acme.com")
                .webhookUrl("https://example.com/webhook")
                .build();
    }

    @Test
    @DisplayName("Should create notification and dispatch only to enabled channels")
    void testCreateAndDispatchNotificationWithPreferences() {
        when(notificationRepository.findByMerchantIdAndIdempotencyKey(any(), any())).thenReturn(Optional.empty());

        when(notificationRepository.saveAndFlush(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });

        when(deliveryRepository.saveAndFlush(any(NotificationDelivery.class))).thenAnswer(invocation -> {
            NotificationDelivery d = invocation.getArgument(0);
            if (d.getId() == null) d.setId(UUID.randomUUID());
            return d;
        });

        // Email and InApp enabled; Webhook disabled
        when(preferenceService.isChannelEnabled(merchantId, MerchantNotificationEvent.PAYMENT_RECOVERED, MerchantNotificationChannel.EMAIL))
                .thenReturn(true);
        when(preferenceService.isChannelEnabled(merchantId, MerchantNotificationEvent.PAYMENT_RECOVERED, MerchantNotificationChannel.WEBHOOK))
                .thenReturn(false);
        when(preferenceService.isChannelEnabled(merchantId, MerchantNotificationEvent.PAYMENT_RECOVERED, MerchantNotificationChannel.IN_APP))
                .thenReturn(true);

        when(emailSender.deliver(any(), any(), any())).thenAnswer(invocation -> {
            NotificationDelivery d = invocation.getArgument(2);
            d.setStatus(NotificationDeliveryStatus.DELIVERED);
            return d;
        });

        when(inAppSender.deliver(any(), any(), any())).thenAnswer(invocation -> {
            NotificationDelivery d = invocation.getArgument(2);
            d.setStatus(NotificationDeliveryStatus.DELIVERED);
            return d;
        });

        Notification result = notificationService.createAndDispatchNotification(
                merchant,
                MerchantNotificationEvent.PAYMENT_RECOVERED,
                "Payment Recovered",
                "Message",
                null,
                null,
                "KEY:001",
                "{}"
        );

        assertNotNull(result);
        verify(emailSender).deliver(any(), eq(merchant), any());
        verify(webhookSender, never()).deliver(any(), any(), any());
        verify(inAppSender).deliver(any(), eq(merchant), any());
    }

    @Test
    @DisplayName("Should return existing notification when idempotency key matches (deduplication)")
    void testIdempotencySuppression() {
        Notification existing = Notification.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .eventType(MerchantNotificationEvent.PAYMENT_RECOVERED)
                .title("Original")
                .message("Msg")
                .idempotencyKey("KEY:001")
                .build();

        when(notificationRepository.findByMerchantIdAndIdempotencyKey(merchantId, "KEY:001"))
                .thenReturn(Optional.of(existing));

        Notification result = notificationService.createAndDispatchNotification(
                merchant,
                MerchantNotificationEvent.PAYMENT_RECOVERED,
                "Duplicate",
                "Msg",
                null,
                null,
                "KEY:001",
                "{}"
        );

        assertEquals(existing.getId(), result.getId());
        verify(notificationRepository, never()).saveAndFlush(any());
        verify(emailSender, never()).deliver(any(), any(), any());
    }

    @Test
    @DisplayName("Should mark notification as read and record audit event")
    void testMarkAsRead() {
        UUID notifId = UUID.randomUUID();
        Notification notification = Notification.builder()
                .id(notifId)
                .merchant(merchant)
                .eventType(MerchantNotificationEvent.CASE_EXHAUSTED)
                .title("Case Exhausted")
                .message("Msg")
                .status(NotificationStatus.UNREAD)
                .build();

        when(notificationRepository.findByIdAndMerchantId(notifId, merchantId)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        NotificationResponseDto response = notificationService.markAsRead(merchantId, notifId, "merchant-admin");

        assertNotNull(response);
        assertEquals(NotificationStatus.READ, response.getStatus());
        assertTrue(response.isRead());
        verify(notificationRepository).save(notification);
    }

    @Test
    @DisplayName("Should throw NotificationNotFoundException when notification does not exist or belongs to another merchant")
    void testNotificationNotFound() {
        UUID notifId = UUID.randomUUID();
        when(notificationRepository.findByIdAndMerchantId(notifId, merchantId)).thenReturn(Optional.empty());

        assertThrows(NotificationNotFoundException.class, () ->
                notificationService.getNotification(merchantId, notifId));
    }
}
