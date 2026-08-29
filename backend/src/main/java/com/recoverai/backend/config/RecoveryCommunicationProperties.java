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
    private HttpProperties http = new HttpProperties();
    private ProviderRetryProperties retry = new ProviderRetryProperties();
    private LinkProperties link = new LinkProperties();

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

    public HttpProperties getHttp() {
        return http;
    }

    public void setHttp(HttpProperties http) {
        this.http = http;
    }

    public ProviderRetryProperties getRetry() {
        return retry;
    }

    public void setRetry(ProviderRetryProperties retry) {
        this.retry = retry;
    }

    public LinkProperties getLink() {
        return link;
    }

    public void setLink(LinkProperties link) {
        this.link = link;
    }

    public static class WhatsAppProperties {
        private String provider = "mock";
        private String senderNumber = "+14155238886";
        private String apiKey = "";
        private String accountSid = "";
        private String authToken = "";
        private String apiBaseUrl = "https://api.twilio.com";

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

        public String getAccountSid() {
            return accountSid;
        }

        public void setAccountSid(String accountSid) {
            this.accountSid = accountSid;
        }

        public String getAuthToken() {
            return authToken;
        }

        public void setAuthToken(String authToken) {
            this.authToken = authToken;
        }

        public String getApiBaseUrl() {
            return apiBaseUrl;
        }

        public void setApiBaseUrl(String apiBaseUrl) {
            this.apiBaseUrl = apiBaseUrl;
        }
    }

    public static class EmailProperties {
        private String provider = "mock";
        private String fromAddress = "recover@recoverai.io";
        private String fromName = "RecoverAI Payment Recovery";
        private String apiKey = "";
        private String apiBaseUrl = "https://api.sendgrid.com";
        private SmtpProperties smtp = new SmtpProperties();

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

        public String getApiBaseUrl() {
            return apiBaseUrl;
        }

        public void setApiBaseUrl(String apiBaseUrl) {
            this.apiBaseUrl = apiBaseUrl;
        }

        public SmtpProperties getSmtp() {
            return smtp;
        }

        public void setSmtp(SmtpProperties smtp) {
            this.smtp = smtp;
        }
    }

    public static class SmtpProperties {
        private String host = "localhost";
        private int port = 587;
        private String username = "";
        private String password = "";
        private boolean tlsEnabled = true;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public boolean isTlsEnabled() {
            return tlsEnabled;
        }

        public void setTlsEnabled(boolean tlsEnabled) {
            this.tlsEnabled = tlsEnabled;
        }
    }

    public static class SmsProperties {
        private String provider = "mock";
        private String senderId = "RECOVER";
        private String apiKey = "";
        private String accountSid = "";
        private String authToken = "";
        private String apiBaseUrl = "https://api.twilio.com";

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

        public String getAccountSid() {
            return accountSid;
        }

        public void setAccountSid(String accountSid) {
            this.accountSid = accountSid;
        }

        public String getAuthToken() {
            return authToken;
        }

        public void setAuthToken(String authToken) {
            this.authToken = authToken;
        }

        public String getApiBaseUrl() {
            return apiBaseUrl;
        }

        public void setApiBaseUrl(String apiBaseUrl) {
            this.apiBaseUrl = apiBaseUrl;
        }
    }

    public static class RetryChargeProperties {
        private String provider = "mock";
        private boolean autoRetryEnabled = true;
        private String keyId = "";
        private String keySecret = "";
        private String apiBaseUrl = "https://api.razorpay.com";

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

        public String getKeyId() {
            return keyId;
        }

        public void setKeyId(String keyId) {
            this.keyId = keyId;
        }

        public String getKeySecret() {
            return keySecret;
        }

        public void setKeySecret(String keySecret) {
            this.keySecret = keySecret;
        }

        public String getApiBaseUrl() {
            return apiBaseUrl;
        }

        public void setApiBaseUrl(String apiBaseUrl) {
            this.apiBaseUrl = apiBaseUrl;
        }
    }

    public static class HttpProperties {
        private int connectTimeoutMs = 5000;
        private int readTimeoutMs = 10000;

        public int getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        public void setConnectTimeoutMs(int connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }

        public int getReadTimeoutMs() {
            return readTimeoutMs;
        }

        public void setReadTimeoutMs(int readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
        }
    }

    public static class ProviderRetryProperties {
        private long baseDelaySeconds = 60L;
        private long maxDelaySeconds = 3600L;

        public long getBaseDelaySeconds() {
            return baseDelaySeconds;
        }

        public void setBaseDelaySeconds(long baseDelaySeconds) {
            this.baseDelaySeconds = baseDelaySeconds;
        }

        public long getMaxDelaySeconds() {
            return maxDelaySeconds;
        }

        public void setMaxDelaySeconds(long maxDelaySeconds) {
            this.maxDelaySeconds = maxDelaySeconds;
        }
    }

    public static class LinkProperties {
        private String secret = "recoverai-secure-recovery-link-secret-token-key";
        private long expirationSeconds = 86400L;
        private boolean useSignedTokens = false;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpirationSeconds() {
            return expirationSeconds;
        }

        public void setExpirationSeconds(long expirationSeconds) {
            this.expirationSeconds = expirationSeconds;
        }

        public boolean isUseSignedTokens() {
            return useSignedTokens;
        }

        public void setUseSignedTokens(boolean useSignedTokens) {
            this.useSignedTokens = useSignedTokens;
        }
    }
}
