package com.recoverai.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "recoverai.recovery")
public class RecoveryCommunicationProperties {

    private String baseUrl = "https://pay.recoverai.io/r/";
    private WhatsAppProperties whatsapp = new WhatsAppProperties();
    private EmailProperties email = new EmailProperties();
    private SmsProperties sms = new SmsProperties();
    private RetryChargeProperties retryCharge = new RetryChargeProperties();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public WhatsAppProperties getWhatsapp() {
        return whatsapp;
    }

    public void setWhatsapp(WhatsAppProperties whatsapp) {
        this.whatsapp = whatsapp;
    }

    public EmailProperties getEmail() {
        return email;
    }

    public void setEmail(EmailProperties email) {
        this.email = email;
    }

    public SmsProperties getSms() {
        return sms;
    }

    public void setSms(SmsProperties sms) {
        this.sms = sms;
    }

    public RetryChargeProperties getRetryCharge() {
        return retryCharge;
    }

    public void setRetryCharge(RetryChargeProperties retryCharge) {
        this.retryCharge = retryCharge;
    }

    public static class WhatsAppProperties {
        private String provider = "mock";
        private String senderNumber = "+14155238886";
        private String apiKey = "";

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getSenderNumber() {
            return senderNumber;
        }

        public void setSenderNumber(String senderNumber) {
            this.senderNumber = senderNumber;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }

    public static class EmailProperties {
        private String provider = "mock";
        private String fromAddress = "recover@recoverai.io";
        private String fromName = "RecoverAI Payment Recovery";
        private String apiKey = "";

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getFromAddress() {
            return fromAddress;
        }

        public void setFromAddress(String fromAddress) {
            this.fromAddress = fromAddress;
        }

        public String getFromName() {
            return fromName;
        }

        public void setFromName(String fromName) {
            this.fromName = fromName;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }

    public static class SmsProperties {
        private String provider = "mock";
        private String senderId = "RECOVER";
        private String apiKey = "";

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getSenderId() {
            return senderId;
        }

        public void setSenderId(String senderId) {
            this.senderId = senderId;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }

    public static class RetryChargeProperties {
        private String provider = "mock";
        private boolean autoRetryEnabled = true;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public boolean isAutoRetryEnabled() {
            return autoRetryEnabled;
        }

        public void setAutoRetryEnabled(boolean autoRetryEnabled) {
            this.autoRetryEnabled = autoRetryEnabled;
        }
    }
}
