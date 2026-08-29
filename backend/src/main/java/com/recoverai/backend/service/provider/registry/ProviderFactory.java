package com.recoverai.backend.service.provider.registry;

import com.recoverai.backend.config.RecoveryCommunicationProperties;
import com.recoverai.backend.service.provider.EmailProvider;
import com.recoverai.backend.service.provider.PaymentRetryProvider;
import com.recoverai.backend.service.provider.SmsProvider;
import com.recoverai.backend.service.provider.WhatsAppProvider;
import org.springframework.stereotype.Component;

@Component
public class ProviderFactory {

    private final ProviderRegistry registry;
    private final RecoveryCommunicationProperties properties;

    public ProviderFactory(ProviderRegistry registry, RecoveryCommunicationProperties properties) {
        this.registry = registry;
        this.properties = properties;
    }

    public WhatsAppProvider getActiveWhatsAppProvider() {
        String providerName = "mock";
        if (properties != null && properties.getWhatsapp() != null && properties.getWhatsapp().getProvider() != null) {
            providerName = properties.getWhatsapp().getProvider();
        }
        return registry.getWhatsAppProvider(providerName);
    }

    public EmailProvider getActiveEmailProvider() {
        String providerName = "mock";
        if (properties != null && properties.getEmail() != null && properties.getEmail().getProvider() != null) {
            providerName = properties.getEmail().getProvider();
        }
        return registry.getEmailProvider(providerName);
    }

    public SmsProvider getActiveSmsProvider() {
        String providerName = "mock";
        if (properties != null && properties.getSms() != null && properties.getSms().getProvider() != null) {
            providerName = properties.getSms().getProvider();
        }
        return registry.getSmsProvider(providerName);
    }

    public PaymentRetryProvider getActivePaymentRetryProvider() {
        String providerName = "mock";
        if (properties != null && properties.getRetryCharge() != null && properties.getRetryCharge().getProvider() != null) {
            providerName = properties.getRetryCharge().getProvider();
        }
        return registry.getPaymentRetryProvider(providerName);
    }

    public ProviderRegistry getRegistry() {
        return registry;
    }
}
