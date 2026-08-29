package com.recoverai.backend.service.provider.validation;

import com.recoverai.backend.config.RecoveryCommunicationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProviderConfigValidatorTest {

    private RecoveryCommunicationProperties properties;
    private ProviderConfigValidator validator;

    @BeforeEach
    void setUp() {
        properties = new RecoveryCommunicationProperties();
        validator = new ProviderConfigValidator(properties);
    }

    @Test
    @DisplayName("Default mock configuration should pass validation with no extra credentials")
    void defaultMockConfigShouldPass() {
        assertThatCode(() -> validator.validateActiveProviders())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Twilio WhatsApp requires accountSid and authToken")
    void twilioWhatsAppValidation() {
        properties.getWhatsapp().setProvider("twilio");
        properties.getWhatsapp().setAccountSid("");
        properties.getWhatsapp().setAuthToken("");

        assertThatThrownBy(() -> validator.validateWhatsAppConfig())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accountSid");

        properties.getWhatsapp().setAccountSid("AC_test");
        assertThatThrownBy(() -> validator.validateWhatsAppConfig())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("authToken");

        properties.getWhatsapp().setAuthToken("auth_token_123");
        assertThatCode(() -> validator.validateWhatsAppConfig())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SendGrid email requires apiKey and fromAddress")
    void sendGridValidation() {
        properties.getEmail().setProvider("sendgrid");
        properties.getEmail().setApiKey("");
        properties.getEmail().setFromAddress("");

        assertThatThrownBy(() -> validator.validateEmailConfig())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("apiKey");

        properties.getEmail().setApiKey("SG.123");
        assertThatThrownBy(() -> validator.validateEmailConfig())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fromAddress");

        properties.getEmail().setFromAddress("recover@recoverai.io");
        assertThatCode(() -> validator.validateEmailConfig())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Razorpay retry requires keyId and keySecret")
    void razorpayValidation() {
        properties.getRetryCharge().setProvider("razorpay");
        properties.getRetryCharge().setKeyId("");
        properties.getRetryCharge().setKeySecret("");

        assertThatThrownBy(() -> validator.validateRetryChargeConfig())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("keyId");

        properties.getRetryCharge().setKeyId("rzp_key");
        properties.getRetryCharge().setKeySecret("rzp_sec");
        assertThatCode(() -> validator.validateRetryChargeConfig())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Unsupported provider name should be rejected with clear message")
    void unsupportedProviderShouldBeRejected() {
        properties.getWhatsapp().setProvider("invalid_provider");
        assertThatThrownBy(() -> validator.validateWhatsAppConfig())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported WhatsApp provider");
    }
}
