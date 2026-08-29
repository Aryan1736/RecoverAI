package com.recoverai.backend.service.provider.registry;

import com.recoverai.backend.config.RecoveryCommunicationProperties;
import com.recoverai.backend.service.provider.EmailProvider;
import com.recoverai.backend.service.provider.PaymentRetryProvider;
import com.recoverai.backend.service.provider.SmsProvider;
import com.recoverai.backend.service.provider.WhatsAppProvider;
import com.recoverai.backend.service.provider.delegating.DelegatingEmailProvider;
import com.recoverai.backend.service.provider.delegating.DelegatingPaymentRetryProvider;
import com.recoverai.backend.service.provider.delegating.DelegatingSmsProvider;
import com.recoverai.backend.service.provider.delegating.DelegatingWhatsAppProvider;
import com.recoverai.backend.service.provider.mock.MockEmailProvider;
import com.recoverai.backend.service.provider.mock.MockPaymentRetryProvider;
import com.recoverai.backend.service.provider.mock.MockSmsProvider;
import com.recoverai.backend.service.provider.mock.MockWhatsAppProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProviderRegistryAndFactoryTest {

    private ProviderRegistry registry;
    private RecoveryCommunicationProperties properties;
    private ProviderFactory factory;

    private WhatsAppProvider mockWhatsApp;
    private WhatsAppProvider twilioWhatsApp;
    private EmailProvider mockEmail;
    private EmailProvider sendGridEmail;
    private SmsProvider mockSms;
    private SmsProvider twilioSms;
    private PaymentRetryProvider mockRetry;
    private PaymentRetryProvider razorpayRetry;

    @BeforeEach
    void setUp() {
        mockWhatsApp = new MockWhatsAppProvider();
        twilioWhatsApp = Mockito.mock(WhatsAppProvider.class);
        mockEmail = new MockEmailProvider();
        sendGridEmail = Mockito.mock(EmailProvider.class);
        mockSms = new MockSmsProvider();
        twilioSms = Mockito.mock(SmsProvider.class);
        properties = new RecoveryCommunicationProperties();
        mockRetry = new MockPaymentRetryProvider(properties);
        razorpayRetry = Mockito.mock(PaymentRetryProvider.class);

        registry = new ProviderRegistry(
                Map.of("mockWhatsAppProvider", mockWhatsApp, "twilioWhatsAppProvider", twilioWhatsApp),
                Map.of("mockEmailProvider", mockEmail, "sendGridEmailProvider", sendGridEmail),
                Map.of("mockSmsProvider", mockSms, "twilioSmsProvider", twilioSms),
                Map.of("mockPaymentRetryProvider", mockRetry, "razorpayPaymentRetryProvider", razorpayRetry)
        );

        factory = new ProviderFactory(registry, properties);
    }

    @Test
    @DisplayName("Should retrieve registered providers by normalized name")
    void shouldRetrieveProviders() {
        assertThat(registry.getWhatsAppProvider("mock")).isSameAs(mockWhatsApp);
        assertThat(registry.getWhatsAppProvider("twilio")).isSameAs(twilioWhatsApp);
        assertThat(registry.getEmailProvider("sendgrid")).isSameAs(sendGridEmail);
        assertThat(registry.getSmsProvider("twilio")).isSameAs(twilioSms);
        assertThat(registry.getPaymentRetryProvider("razorpay")).isSameAs(razorpayRetry);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when unsupported provider requested")
    void shouldThrowForUnknownProvider() {
        assertThatThrownBy(() -> registry.getWhatsAppProvider("unknown_wa"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported WhatsApp provider");

        assertThatThrownBy(() -> registry.getEmailProvider("unknown_email"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported Email provider");
    }

    @Test
    @DisplayName("Should resolve active provider dynamically via ProviderFactory based on properties")
    void shouldResolveActiveProvidersViaFactory() {
        // By default, properties configure 'mock'
        assertThat(factory.getActiveWhatsAppProvider()).isSameAs(mockWhatsApp);
        assertThat(factory.getActiveEmailProvider()).isSameAs(mockEmail);
        assertThat(factory.getActiveSmsProvider()).isSameAs(mockSms);
        assertThat(factory.getActivePaymentRetryProvider()).isSameAs(mockRetry);

        // Change config dynamically
        properties.getWhatsapp().setProvider("twilio");
        properties.getEmail().setProvider("sendgrid");
        properties.getSms().setProvider("twilio");
        properties.getRetryCharge().setProvider("razorpay");

        assertThat(factory.getActiveWhatsAppProvider()).isSameAs(twilioWhatsApp);
        assertThat(factory.getActiveEmailProvider()).isSameAs(sendGridEmail);
        assertThat(factory.getActiveSmsProvider()).isSameAs(twilioSms);
        assertThat(factory.getActivePaymentRetryProvider()).isSameAs(razorpayRetry);
    }

    @Test
    @DisplayName("Delegating providers should forward to active factory provider")
    void shouldDelegateCallsProperly() {
        DelegatingWhatsAppProvider delegatingWa = new DelegatingWhatsAppProvider(factory);
        DelegatingEmailProvider delegatingEmail = new DelegatingEmailProvider(factory);
        DelegatingSmsProvider delegatingSms = new DelegatingSmsProvider(factory);
        DelegatingPaymentRetryProvider delegatingRetry = new DelegatingPaymentRetryProvider(factory);

        properties.getWhatsapp().setProvider("mock");
        properties.getEmail().setProvider("mock");
        properties.getSms().setProvider("mock");
        properties.getRetryCharge().setProvider("mock");

        assertThat(delegatingWa.sendWhatsApp(null).isSuccess()).isTrue();
        assertThat(delegatingEmail.sendEmail(null).isSuccess()).isTrue();
        assertThat(delegatingSms.sendSms(null).isSuccess()).isTrue();
        assertThat(delegatingRetry.retryCharge(null).isSuccess()).isTrue();
    }
}
