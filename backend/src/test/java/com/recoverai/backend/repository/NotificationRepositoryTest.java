package com.recoverai.backend.repository;

import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Notification;
import com.recoverai.backend.entity.NotificationDelivery;
import com.recoverai.backend.entity.NotificationPreference;
import com.recoverai.backend.entity.enums.MerchantNotificationChannel;
import com.recoverai.backend.entity.enums.MerchantNotificationEvent;
import com.recoverai.backend.entity.enums.MerchantStatus;
import com.recoverai.backend.entity.enums.NotificationDeliveryStatus;
import com.recoverai.backend.entity.enums.NotificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class NotificationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationPreferenceRepository preferenceRepository;

    @Autowired
    private NotificationDeliveryRepository deliveryRepository;

    private Merchant merchant;
    private Merchant otherMerchant;

    @BeforeEach
    void setUp() {
        merchant = Merchant.builder()
                .name("Acme Corp")
                .email("test-alerts@acme.com")
                .status(MerchantStatus.ACTIVE)
                .webhookSecret("whsec_test123")
                .webhookUrl("https://acme.com/webhook")
                .build();
        merchant = entityManager.persistAndFlush(merchant);

        otherMerchant = Merchant.builder()
                .name("Beta Corp")
                .email("test-alerts@beta.com")
                .status(MerchantStatus.ACTIVE)
                .webhookSecret("whsec_beta123")
                .build();
        otherMerchant = entityManager.persistAndFlush(otherMerchant);
    }

    @Test
    @DisplayName("Should persist and retrieve notification with deliveries")
    void testPersistAndRetrieveNotification() {
        Notification notification = Notification.builder()
                .merchant(merchant)
                .eventType(MerchantNotificationEvent.PAYMENT_RECOVERED)
                .title("Payment Recovered")
                .message("Successfully recovered payment of 5000 INR")
                .status(NotificationStatus.UNREAD)
                .idempotencyKey("PAYMENT_RECOVERED:test:001")
                .build();

        NotificationDelivery delivery = NotificationDelivery.builder()
                .merchant(merchant)
                .channel(MerchantNotificationChannel.EMAIL)
                .provider("SENDGRID")
                .status(NotificationDeliveryStatus.DELIVERED)
                .build();

        notification.addDelivery(delivery);
        Notification saved = notificationRepository.saveAndFlush(notification);

        assertNotNull(saved.getId());
        assertEquals(1, saved.getDeliveries().size());

        Optional<Notification> found = notificationRepository.findByIdAndMerchantId(saved.getId(), merchant.getId());
        assertTrue(found.isPresent());
        assertEquals("PAYMENT_RECOVERED:test:001", found.get().getIdempotencyKey());
    }

    @Test
    @DisplayName("Should enforce merchant isolation when finding notification")
    void testMerchantIsolation() {
        Notification notification = Notification.builder()
                .merchant(merchant)
                .eventType(MerchantNotificationEvent.CASE_EXHAUSTED)
                .title("Case Exhausted")
                .message("Max attempts reached")
                .status(NotificationStatus.UNREAD)
                .build();
        notification = notificationRepository.saveAndFlush(notification);

        Optional<Notification> crossTenantLookup = notificationRepository
                .findByIdAndMerchantId(notification.getId(), otherMerchant.getId());
        assertFalse(crossTenantLookup.isPresent());
    }

    @Test
    @DisplayName("Should enforce uniqueness constraint on merchant notification preferences")
    void testPreferenceUniquenessConstraint() {
        NotificationPreference pref1 = NotificationPreference.builder()
                .merchant(merchant)
                .eventType(MerchantNotificationEvent.PAYMENT_RECOVERED)
                .channel(MerchantNotificationChannel.EMAIL)
                .enabled(true)
                .build();
        preferenceRepository.saveAndFlush(pref1);

        NotificationPreference pref2 = NotificationPreference.builder()
                .merchant(merchant)
                .eventType(MerchantNotificationEvent.PAYMENT_RECOVERED)
                .channel(MerchantNotificationChannel.EMAIL)
                .enabled(false)
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> {
            preferenceRepository.saveAndFlush(pref2);
        });
    }

    @Test
    @DisplayName("Should mark all unread notifications as read for a merchant")
    void testMarkAllUnreadAsRead() {
        for (int i = 0; i < 3; i++) {
            Notification n = Notification.builder()
                    .merchant(merchant)
                    .eventType(MerchantNotificationEvent.PAYMENT_RECOVERED)
                    .title("Alert " + i)
                    .message("Msg " + i)
                    .status(NotificationStatus.UNREAD)
                    .build();
            notificationRepository.save(n);
        }

        Notification otherN = Notification.builder()
                .merchant(otherMerchant)
                .eventType(MerchantNotificationEvent.PAYMENT_RECOVERED)
                .title("Other Alert")
                .message("Other Msg")
                .status(NotificationStatus.UNREAD)
                .build();
        notificationRepository.save(otherN);
        notificationRepository.flush();

        int updated = notificationRepository.markAllUnreadAsReadForMerchant(
                merchant.getId(), NotificationStatus.READ, Instant.now());
        assertEquals(3, updated);

        assertEquals(0, notificationRepository.countByMerchantIdAndStatus(merchant.getId(), NotificationStatus.UNREAD));
        assertEquals(1, notificationRepository.countByMerchantIdAndStatus(otherMerchant.getId(), NotificationStatus.UNREAD));
    }

    @Test
    @DisplayName("Should filter and paginate notifications correctly")
    void testPaginationAndFiltering() {
        Notification n1 = Notification.builder()
                .merchant(merchant)
                .eventType(MerchantNotificationEvent.PAYMENT_RECOVERED)
                .title("Alert 1")
                .message("Msg 1")
                .status(NotificationStatus.UNREAD)
                .build();
        Notification n2 = Notification.builder()
                .merchant(merchant)
                .eventType(MerchantNotificationEvent.HIGH_PRIORITY_FAILURE)
                .title("Alert 2")
                .message("Msg 2")
                .status(NotificationStatus.READ)
                .build();
        notificationRepository.saveAllAndFlush(List.of(n1, n2));

        Page<Notification> unreadPage = notificationRepository.findByMerchantIdAndStatus(
                merchant.getId(),
                NotificationStatus.UNREAD,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        assertEquals(1, unreadPage.getTotalElements());
        assertEquals(MerchantNotificationEvent.PAYMENT_RECOVERED, unreadPage.getContent().get(0).getEventType());
    }
}
