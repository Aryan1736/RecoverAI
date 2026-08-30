package com.recoverai.backend.service.notification;

import com.recoverai.backend.config.RecoverAINotificationProperties;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.enums.ActorType;
import com.recoverai.backend.entity.enums.MerchantStatus;
import com.recoverai.backend.repository.MerchantRepository;
import com.recoverai.backend.service.AuditService;
import com.recoverai.backend.service.provider.health.ProviderHealthResult;
import com.recoverai.backend.service.provider.health.ProviderHealthService;
import com.recoverai.backend.service.provider.health.ProviderHealthStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProviderHealthAlertService {

    private static final Logger log = LoggerFactory.getLogger(ProviderHealthAlertService.class);

    private final ProviderHealthService providerHealthService;
    private final MerchantNotificationService notificationService;
    private final MerchantRepository merchantRepository;
    private final AuditService auditService;
    private final RecoverAINotificationProperties properties;

    private final Map<String, Instant> lastAlertTime = new ConcurrentHashMap<>();

    public ProviderHealthAlertService(ProviderHealthService providerHealthService,
                                      MerchantNotificationService notificationService,
                                      MerchantRepository merchantRepository,
                                      AuditService auditService,
                                      RecoverAINotificationProperties properties) {
        this.providerHealthService = providerHealthService;
        this.notificationService = notificationService;
        this.merchantRepository = merchantRepository;
        this.auditService = auditService;
        this.properties = properties != null ? properties : new RecoverAINotificationProperties();
    }

    /**
     * Inspects provider health across all registered providers and triggers
     * PROVIDER_DEGRADED notifications to active merchants if health is degraded,
     * enforcing a cooldown period to prevent alert storms.
     *
     * @return count of degraded provider alerts triggered
     */
    public int checkAndAlertDegradedProviders() {
        List<ProviderHealthResult> healthResults = providerHealthService.checkAll();
        if (healthResults == null || healthResults.isEmpty()) {
            return 0;
        }

        int alertsTriggered = 0;
        Instant now = Instant.now();
        int cooldownMinutes = properties != null ? Math.max(1, properties.getProviderHealthCooldownMinutes()) : 30;

        for (ProviderHealthResult result : healthResults) {
            if (result.getStatus() == ProviderHealthStatus.DEGRADED) {
                String providerKey = result.getProviderName().toUpperCase();
                Instant lastAlert = lastAlertTime.get(providerKey);

                if (lastAlert != null && now.isBefore(lastAlert.plus(Duration.ofMinutes(cooldownMinutes)))) {
                    log.debug("Cooldown active for degraded provider {}; skipping duplicate alert", providerKey);
                    continue;
                }

                log.warn("Detected degraded provider {}: {}", providerKey, result.getMessage());
                String cooldownBucket = String.valueOf(now.getEpochSecond() / (cooldownMinutes * 60L));

                List<Merchant> activeMerchants = merchantRepository.findByStatus(MerchantStatus.ACTIVE);
                for (Merchant merchant : activeMerchants) {
                    notificationService.notifyProviderDegraded(
                            merchant,
                            result.getProviderName(),
                            result.getCategory(),
                            result.getMessage(),
                            cooldownBucket
                    );
                }

                auditService.recordEvent(
                        null,
                        "PROVIDER_DEGRADED_ALERTED",
                        ActorType.SYSTEM,
                        "ProviderHealthAlertService",
                        "Provider",
                        providerKey,
                        "ALERT_DEGRADED",
                        String.format("Alerted %d merchants of degraded provider %s: %s",
                                activeMerchants.size(), providerKey, result.getMessage()),
                        null
                );

                lastAlertTime.put(providerKey, now);
                alertsTriggered++;
            }
        }

        return alertsTriggered;
    }

    public void clearCooldown(String providerName) {
        if (providerName != null) {
            lastAlertTime.remove(providerName.toUpperCase());
        }
    }

    public void clearAllCooldowns() {
        lastAlertTime.clear();
    }
}
