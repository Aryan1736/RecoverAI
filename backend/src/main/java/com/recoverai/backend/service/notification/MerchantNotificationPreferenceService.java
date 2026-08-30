package com.recoverai.backend.service.notification;

import com.recoverai.backend.dto.notification.NotificationPreferenceResponseDto;
import com.recoverai.backend.dto.notification.NotificationPreferenceUpdateRequestDto;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.NotificationPreference;
import com.recoverai.backend.entity.enums.ActorType;
import com.recoverai.backend.entity.enums.MerchantNotificationChannel;
import com.recoverai.backend.entity.enums.MerchantNotificationEvent;
import com.recoverai.backend.exception.MerchantResolutionException;
import com.recoverai.backend.repository.MerchantRepository;
import com.recoverai.backend.repository.NotificationPreferenceRepository;
import com.recoverai.backend.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class MerchantNotificationPreferenceService {

    private static final Logger log = LoggerFactory.getLogger(MerchantNotificationPreferenceService.class);

    private final NotificationPreferenceRepository preferenceRepository;
    private final MerchantRepository merchantRepository;
    private final AuditService auditService;

    public MerchantNotificationPreferenceService(NotificationPreferenceRepository preferenceRepository,
                                                MerchantRepository merchantRepository,
                                                AuditService auditService) {
        this.preferenceRepository = preferenceRepository;
        this.merchantRepository = merchantRepository;
        this.auditService = auditService;
    }

    /**
     * Retrieves the notification preferences for a merchant, populated with default values
     * for any unconfigured event/channel combinations.
     */
    @Transactional(readOnly = true)
    public NotificationPreferenceResponseDto getPreferences(UUID merchantId) {
        Merchant merchant = getMerchantOrThrow(merchantId);
        Map<MerchantNotificationEvent, Map<MerchantNotificationChannel, Boolean>> prefs = buildDefaultPreferences();

        List<NotificationPreference> persistedPrefs = preferenceRepository.findByMerchantId(merchantId);
        for (NotificationPreference pref : persistedPrefs) {
            Map<MerchantNotificationChannel, Boolean> channelMap = prefs.computeIfAbsent(
                    pref.getEventType(), k -> new EnumMap<>(MerchantNotificationChannel.class));
            channelMap.put(pref.getChannel(), pref.isEnabled());
        }

        return new NotificationPreferenceResponseDto(merchantId, merchant.getWebhookUrl(), prefs);
    }

    /**
     * Updates notification preferences and optional webhook URL for a merchant.
     */
    @Transactional
    public NotificationPreferenceResponseDto updatePreferences(UUID merchantId,
                                                               NotificationPreferenceUpdateRequestDto request,
                                                               String actor) {
        Merchant merchant = getMerchantOrThrow(merchantId);

        // Update webhook URL if specified
        if (request.getWebhookUrl() != null) {
            String sanitizedUrl = request.getWebhookUrl().trim();
            merchant.setWebhookUrl(sanitizedUrl.isEmpty() ? null : sanitizedUrl);
            merchantRepository.save(merchant);
        }

        Map<MerchantNotificationEvent, Map<MerchantNotificationChannel, Boolean>> updatedPrefs = request.getPreferences();
        if (updatedPrefs != null) {
            for (Map.Entry<MerchantNotificationEvent, Map<MerchantNotificationChannel, Boolean>> eventEntry : updatedPrefs.entrySet()) {
                MerchantNotificationEvent event = eventEntry.getKey();
                if (event == null || eventEntry.getValue() == null) {
                    continue;
                }

                for (Map.Entry<MerchantNotificationChannel, Boolean> channelEntry : eventEntry.getValue().entrySet()) {
                    MerchantNotificationChannel channel = channelEntry.getKey();
                    Boolean enabled = channelEntry.getValue();
                    if (channel == null || enabled == null) {
                        continue;
                    }

                    Optional<NotificationPreference> existingOpt = preferenceRepository
                            .findByMerchantIdAndEventTypeAndChannel(merchantId, event, channel);

                    NotificationPreference preference;
                    if (existingOpt.isPresent()) {
                        preference = existingOpt.get();
                        preference.setEnabled(enabled);
                        preference.setUpdatedAt(Instant.now());
                    } else {
                        preference = NotificationPreference.builder()
                                .merchant(merchant)
                                .eventType(event)
                                .channel(channel)
                                .enabled(enabled)
                                .build();
                    }
                    preferenceRepository.save(preference);
                }
            }
        }

        auditService.recordEvent(
                merchant,
                "NOTIFICATION_PREFERENCE_UPDATED",
                ActorType.USER,
                actor != null ? actor : merchantId.toString(),
                "MerchantNotificationPreference",
                merchantId.toString(),
                "UPDATE_PREFERENCES",
                "Merchant notification preferences updated",
                null
        );

        log.info("Updated notification preferences for merchant {}", merchantId);
        return getPreferences(merchantId);
    }

    /**
     * Checks if a particular channel is enabled for an event and merchant.
     */
    @Transactional(readOnly = true)
    public boolean isChannelEnabled(UUID merchantId, MerchantNotificationEvent event, MerchantNotificationChannel channel) {
        return preferenceRepository.findByMerchantIdAndEventTypeAndChannel(merchantId, event, channel)
                .map(NotificationPreference::isEnabled)
                .orElseGet(() -> getDefaultPreference(event, channel));
    }

    private Merchant getMerchantOrThrow(UUID merchantId) {
        if (merchantId == null) {
            throw new MerchantResolutionException("Merchant ID cannot be null");
        }
        return merchantRepository.findById(merchantId)
                .orElseThrow(() -> new MerchantResolutionException("Merchant not found with ID: " + merchantId));
    }

    public static boolean getDefaultPreference(MerchantNotificationEvent event, MerchantNotificationChannel channel) {
        if (event == null || channel == null) {
            return false;
        }
        return switch (event) {
            case PAYMENT_RECOVERED -> true; // EMAIL=true, WEBHOOK=true, IN_APP=true
            case CASE_EXHAUSTED -> channel != MerchantNotificationChannel.WEBHOOK; // EMAIL=true, WEBHOOK=false, IN_APP=true
            case HIGH_PRIORITY_FAILURE -> true; // EMAIL=true, WEBHOOK=true, IN_APP=true
            case PROVIDER_DEGRADED -> true; // EMAIL=true, WEBHOOK=true, IN_APP=true
        };
    }

    public static Map<MerchantNotificationEvent, Map<MerchantNotificationChannel, Boolean>> buildDefaultPreferences() {
        Map<MerchantNotificationEvent, Map<MerchantNotificationChannel, Boolean>> defaults = new EnumMap<>(MerchantNotificationEvent.class);
        for (MerchantNotificationEvent event : MerchantNotificationEvent.values()) {
            Map<MerchantNotificationChannel, Boolean> channelMap = new EnumMap<>(MerchantNotificationChannel.class);
            for (MerchantNotificationChannel channel : MerchantNotificationChannel.values()) {
                channelMap.put(channel, getDefaultPreference(event, channel));
            }
            defaults.put(event, channelMap);
        }
        return defaults;
    }
}
