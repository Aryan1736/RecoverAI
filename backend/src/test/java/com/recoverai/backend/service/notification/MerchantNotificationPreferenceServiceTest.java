package com.recoverai.backend.service.notification;

import com.recoverai.backend.dto.notification.NotificationPreferenceResponseDto;
import com.recoverai.backend.dto.notification.NotificationPreferenceUpdateRequestDto;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.NotificationPreference;
import com.recoverai.backend.entity.enums.MerchantNotificationChannel;
import com.recoverai.backend.entity.enums.MerchantNotificationEvent;
import com.recoverai.backend.entity.enums.MerchantStatus;
import com.recoverai.backend.exception.MerchantResolutionException;
import com.recoverai.backend.repository.MerchantRepository;
import com.recoverai.backend.repository.NotificationPreferenceRepository;
import com.recoverai.backend.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantNotificationPreferenceServiceTest {

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private AuditService auditService;

    private MerchantNotificationPreferenceService preferenceService;

    private Merchant merchant;
    private UUID merchantId;

    @BeforeEach
    void setUp() {
        preferenceService = new MerchantNotificationPreferenceService(preferenceRepository, merchantRepository, auditService);

        merchantId = UUID.randomUUID();
        merchant = Merchant.builder()
                .id(merchantId)
                .name("Acme Corp")
                .email("merchant@acme.com")
                .status(MerchantStatus.ACTIVE)
                .webhookUrl("https://example.com/webhook")
                .build();
    }

    @Test
    @DisplayName("Should return default preferences when no custom preferences are stored")
    void testGetDefaultPreferences() {
        when(merchantRepository.findById(merchantId)).thenReturn(Optional.of(merchant));
        when(preferenceRepository.findByMerchantId(merchantId)).thenReturn(Collections.emptyList());

        NotificationPreferenceResponseDto response = preferenceService.getPreferences(merchantId);

        assertNotNull(response);
        assertEquals(merchantId, response.getMerchantId());
        assertEquals("https://example.com/webhook", response.getWebhookUrl());

        Map<MerchantNotificationEvent, Map<MerchantNotificationChannel, Boolean>> prefs = response.getPreferences();
        assertNotNull(prefs);

        // Verify default preferences
        assertTrue(prefs.get(MerchantNotificationEvent.PAYMENT_RECOVERED).get(MerchantNotificationChannel.EMAIL));
        assertTrue(prefs.get(MerchantNotificationEvent.PAYMENT_RECOVERED).get(MerchantNotificationChannel.WEBHOOK));
        assertTrue(prefs.get(MerchantNotificationEvent.PAYMENT_RECOVERED).get(MerchantNotificationChannel.IN_APP));

        assertTrue(prefs.get(MerchantNotificationEvent.CASE_EXHAUSTED).get(MerchantNotificationChannel.EMAIL));
        assertFalse(prefs.get(MerchantNotificationEvent.CASE_EXHAUSTED).get(MerchantNotificationChannel.WEBHOOK));
        assertTrue(prefs.get(MerchantNotificationEvent.CASE_EXHAUSTED).get(MerchantNotificationChannel.IN_APP));
    }

    @Test
    @DisplayName("Should update notification preferences and webhook URL")
    void testUpdatePreferences() {
        when(merchantRepository.findById(merchantId)).thenReturn(Optional.of(merchant));

        Map<MerchantNotificationEvent, Map<MerchantNotificationChannel, Boolean>> updateMap = new EnumMap<>(MerchantNotificationEvent.class);
        Map<MerchantNotificationChannel, Boolean> paymentPrefs = new EnumMap<>(MerchantNotificationChannel.class);
        paymentPrefs.put(MerchantNotificationChannel.WEBHOOK, false);
        updateMap.put(MerchantNotificationEvent.PAYMENT_RECOVERED, paymentPrefs);

        NotificationPreferenceUpdateRequestDto request = new NotificationPreferenceUpdateRequestDto(
                "https://updated-webhook.com/alerts",
                updateMap
        );

        when(preferenceRepository.findByMerchantIdAndEventTypeAndChannel(
                merchantId, MerchantNotificationEvent.PAYMENT_RECOVERED, MerchantNotificationChannel.WEBHOOK))
                .thenReturn(Optional.empty());

        when(preferenceRepository.findByMerchantId(merchantId)).thenReturn(List.of(
                NotificationPreference.builder()
                        .merchant(merchant)
                        .eventType(MerchantNotificationEvent.PAYMENT_RECOVERED)
                        .channel(MerchantNotificationChannel.WEBHOOK)
                        .enabled(false)
                        .build()
        ));

        NotificationPreferenceResponseDto response = preferenceService.updatePreferences(merchantId, request, "admin");

        assertNotNull(response);
        assertEquals("https://updated-webhook.com/alerts", merchant.getWebhookUrl());
        verify(merchantRepository).save(merchant);
        verify(preferenceRepository).save(any(NotificationPreference.class));
    }

    @Test
    @DisplayName("Should throw MerchantResolutionException when merchant not found")
    void testMerchantNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(merchantRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(MerchantResolutionException.class, () -> preferenceService.getPreferences(unknownId));
    }
}
