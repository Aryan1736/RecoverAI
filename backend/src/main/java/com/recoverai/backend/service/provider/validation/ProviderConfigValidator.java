package com.recoverai.backend.service.provider.validation;

import com.recoverai.backend.config.RecoveryCommunicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class ProviderConfigValidator {

    private static final Logger log = LoggerFactory.getLogger(ProviderConfigValidator.class);

    private final RecoveryCommunicationProperties properties;

    public ProviderConfigValidator(RecoveryCommunicationProperties properties) {
        this.properties = properties;
    }

    public void validateActiveProviders() {
        validateWhatsAppConfig();
        validateEmailConfig();
        validateSmsConfig();
        validateRetryChargeConfig();
    }

    public void validateWhatsAppConfig() {
        if (properties == null || properties.getWhatsapp() == null) {
            return;
        }
        String provider = properties.getWhatsapp().getProvider();
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("WhatsApp provider cannot be blank");
        }

        String normalized = provider.trim().toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "mock" -> log.debug("WhatsApp provider configured as MOCK");
            case "twilio" -> {
                if (isBlank(properties.getWhatsapp().getAccountSid())) {
                    throw new IllegalArgumentException("Twilio WhatsApp requires 'accountSid' (WHATSAPP_ACCOUNT_SID) to be configured");
                }
                if (isBlank(properties.getWhatsapp().getAuthToken()) && isBlank(properties.getWhatsapp().getApiKey())) {
                    throw new IllegalArgumentException("Twilio WhatsApp requires 'authToken' (WHATSAPP_AUTH_TOKEN) or 'apiKey' to be configured");
                }
            }
            case "meta" -> {
                if (isBlank(properties.getWhatsapp().getApiKey())) {
                    throw new IllegalArgumentException("Meta WhatsApp requires 'apiKey' (WHATSAPP_API_KEY) to be configured");
                }
            }
            default -> throw new IllegalArgumentException("Unsupported WhatsApp provider: '" + normalized + "'. Supported: mock, twilio, meta");
        }
    }

    public void validateEmailConfig() {
        if (properties == null || properties.getEmail() == null) {
            return;
        }
        String provider = properties.getEmail().getProvider();
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("Email provider cannot be blank");
        }

        String normalized = provider.trim().toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "mock" -> log.debug("Email provider configured as MOCK");
            case "sendgrid" -> {
                if (isBlank(properties.getEmail().getApiKey())) {
                    throw new IllegalArgumentException("SendGrid email provider requires 'apiKey' (EMAIL_API_KEY) to be configured");
                }
                if (isBlank(properties.getEmail().getFromAddress())) {
                    throw new IllegalArgumentException("SendGrid email provider requires 'fromAddress' (EMAIL_FROM_ADDRESS) to be configured");
                }
            }
            case "smtp" -> {
                if (properties.getEmail().getSmtp() == null || isBlank(properties.getEmail().getSmtp().getHost())) {
                    throw new IllegalArgumentException("SMTP email provider requires 'smtp.host' (SMTP_HOST) to be configured");
                }
                if (properties.getEmail().getSmtp().getPort() <= 0) {
                    throw new IllegalArgumentException("SMTP email provider requires a valid 'smtp.port' (SMTP_PORT > 0)");
                }
            }
            default -> throw new IllegalArgumentException("Unsupported Email provider: '" + normalized + "'. Supported: mock, sendgrid, smtp");
        }
    }

    public void validateSmsConfig() {
        if (properties == null || properties.getSms() == null) {
            return;
        }
        String provider = properties.getSms().getProvider();
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("SMS provider cannot be blank");
        }

        String normalized = provider.trim().toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "mock" -> log.debug("SMS provider configured as MOCK");
            case "twilio" -> {
                if (isBlank(properties.getSms().getAccountSid())) {
                    throw new IllegalArgumentException("Twilio SMS requires 'accountSid' (SMS_ACCOUNT_SID) to be configured");
                }
                if (isBlank(properties.getSms().getAuthToken()) && isBlank(properties.getSms().getApiKey())) {
                    throw new IllegalArgumentException("Twilio SMS requires 'authToken' (SMS_AUTH_TOKEN) or 'apiKey' to be configured");
                }
            }
            default -> throw new IllegalArgumentException("Unsupported SMS provider: '" + normalized + "'. Supported: mock, twilio");
        }
    }

    public void validateRetryChargeConfig() {
        if (properties == null || properties.getRetryCharge() == null) {
            return;
        }
        String provider = properties.getRetryCharge().getProvider();
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("Retry charge provider cannot be blank");
        }

        String normalized = provider.trim().toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "mock" -> log.debug("Payment retry provider configured as MOCK");
            case "razorpay" -> {
                if (isBlank(properties.getRetryCharge().getKeyId()) || isBlank(properties.getRetryCharge().getKeySecret())) {
                    throw new IllegalArgumentException("Razorpay payment retry requires 'keyId' and 'keySecret' (RAZORPAY_KEY_ID / RAZORPAY_KEY_SECRET) to be configured");
                }
            }
            default -> throw new IllegalArgumentException("Unsupported Payment Retry provider: '" + normalized + "'. Supported: mock, razorpay");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
