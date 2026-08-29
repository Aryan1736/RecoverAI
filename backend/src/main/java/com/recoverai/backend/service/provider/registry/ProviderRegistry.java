package com.recoverai.backend.service.provider.registry;

import com.recoverai.backend.service.provider.EmailProvider;
import com.recoverai.backend.service.provider.PaymentRetryProvider;
import com.recoverai.backend.service.provider.SmsProvider;
import com.recoverai.backend.service.provider.WhatsAppProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ProviderRegistry {

    private final Map<String, WhatsAppProvider> whatsAppProviders = new ConcurrentHashMap<>();
    private final Map<String, EmailProvider> emailProviders = new ConcurrentHashMap<>();
    private final Map<String, SmsProvider> smsProviders = new ConcurrentHashMap<>();
    private final Map<String, PaymentRetryProvider> paymentRetryProviders = new ConcurrentHashMap<>();

    @Autowired
    public ProviderRegistry(Map<String, WhatsAppProvider> springWhatsAppProviders,
                            Map<String, EmailProvider> springEmailProviders,
                            Map<String, SmsProvider> springSmsProviders,
                            Map<String, PaymentRetryProvider> springPaymentRetryProviders) {
        if (springWhatsAppProviders != null) {
            springWhatsAppProviders.forEach((beanName, provider) -> {
                if (!beanName.toLowerCase(Locale.ROOT).startsWith("delegating")) {
                    registerWhatsAppProvider(normalizeName(beanName, "whatsappprovider"), provider);
                }
            });
        }
        if (springEmailProviders != null) {
            springEmailProviders.forEach((beanName, provider) -> {
                if (!beanName.toLowerCase(Locale.ROOT).startsWith("delegating")) {
                    registerEmailProvider(normalizeName(beanName, "emailprovider"), provider);
                }
            });
        }
        if (springSmsProviders != null) {
            springSmsProviders.forEach((beanName, provider) -> {
                if (!beanName.toLowerCase(Locale.ROOT).startsWith("delegating")) {
                    registerSmsProvider(normalizeName(beanName, "smsprovider"), provider);
                }
            });
        }
        if (springPaymentRetryProviders != null) {
            springPaymentRetryProviders.forEach((beanName, provider) -> {
                if (!beanName.toLowerCase(Locale.ROOT).startsWith("delegating")) {
                    registerPaymentRetryProvider(normalizeName(beanName, "paymentretryprovider"), provider);
                }
            });
        }
    }

    public ProviderRegistry() {
    }

    private String normalizeName(String beanName, String suffixToRemove) {
        String clean = beanName.toLowerCase(Locale.ROOT);
        if (clean.endsWith(suffixToRemove)) {
            clean = clean.substring(0, clean.length() - suffixToRemove.length());
        }
        return clean;
    }

    public void registerWhatsAppProvider(String name, WhatsAppProvider provider) {
        whatsAppProviders.put(name.trim().toLowerCase(Locale.ROOT), provider);
    }

    public void registerEmailProvider(String name, EmailProvider provider) {
        emailProviders.put(name.trim().toLowerCase(Locale.ROOT), provider);
    }

    public void registerSmsProvider(String name, SmsProvider provider) {
        smsProviders.put(name.trim().toLowerCase(Locale.ROOT), provider);
    }

    public void registerPaymentRetryProvider(String name, PaymentRetryProvider provider) {
        paymentRetryProviders.put(name.trim().toLowerCase(Locale.ROOT), provider);
    }

    public WhatsAppProvider getWhatsAppProvider(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("WhatsApp provider name cannot be blank");
        }
        String key = name.trim().toLowerCase(Locale.ROOT);
        WhatsAppProvider provider = whatsAppProviders.get(key);
        if (provider == null) {
            throw new IllegalArgumentException("Unsupported WhatsApp provider: '" + name + "'. Registered: " + whatsAppProviders.keySet());
        }
        return provider;
    }

    public EmailProvider getEmailProvider(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Email provider name cannot be blank");
        }
        String key = name.trim().toLowerCase(Locale.ROOT);
        EmailProvider provider = emailProviders.get(key);
        if (provider == null) {
            throw new IllegalArgumentException("Unsupported Email provider: '" + name + "'. Registered: " + emailProviders.keySet());
        }
        return provider;
    }

    public SmsProvider getSmsProvider(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("SMS provider name cannot be blank");
        }
        String key = name.trim().toLowerCase(Locale.ROOT);
        SmsProvider provider = smsProviders.get(key);
        if (provider == null) {
            throw new IllegalArgumentException("Unsupported SMS provider: '" + name + "'. Registered: " + smsProviders.keySet());
        }
        return provider;
    }

    public PaymentRetryProvider getPaymentRetryProvider(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Payment retry provider name cannot be blank");
        }
        String key = name.trim().toLowerCase(Locale.ROOT);
        PaymentRetryProvider provider = paymentRetryProviders.get(key);
        if (provider == null) {
            throw new IllegalArgumentException("Unsupported Payment Retry provider: '" + name + "'. Registered: " + paymentRetryProviders.keySet());
        }
        return provider;
    }

    public Set<String> getAvailableWhatsAppProviders() {
        return Collections.unmodifiableSet(whatsAppProviders.keySet());
    }

    public Set<String> getAvailableEmailProviders() {
        return Collections.unmodifiableSet(emailProviders.keySet());
    }

    public Set<String> getAvailableSmsProviders() {
        return Collections.unmodifiableSet(smsProviders.keySet());
    }

    public Set<String> getAvailablePaymentRetryProviders() {
        return Collections.unmodifiableSet(paymentRetryProviders.keySet());
    }
}
