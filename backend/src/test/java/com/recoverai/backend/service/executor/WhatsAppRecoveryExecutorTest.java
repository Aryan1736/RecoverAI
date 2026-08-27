package com.recoverai.backend.service.executor;

import com.recoverai.backend.entity.Customer;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.service.link.RecoveryLinkService;
import com.recoverai.backend.service.provider.WhatsAppProvider;
import com.recoverai.backend.service.provider.dto.CommunicationDeliveryResult;
import com.recoverai.backend.service.provider.dto.WhatsAppMessageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WhatsAppRecoveryExecutorTest {

    @Mock
    private WhatsAppProvider whatsappProvider;

    @Mock
    private RecoveryLinkService recoveryLinkService;

    private WhatsAppRecoveryExecutor executor;

    private RecoveryAttempt attempt;
    private RecoveryCase recoveryCase;
    private Customer customer;
    private Merchant merchant;
    private Payment payment;

    @BeforeEach
    void setUp() {
        executor = new WhatsAppRecoveryExecutor(whatsappProvider, recoveryLinkService);

        merchant = Merchant.builder().id(UUID.randomUUID()).name("Acme Corp").build();
        customer = Customer.builder().id(UUID.randomUUID()).name("John Doe").phone("+919876543210").build();
        payment = Payment.builder().id(UUID.randomUUID()).amount(new BigDecimal("1999.00")).currency("INR").build();
        recoveryCase = RecoveryCase.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .customer(customer)
                .payment(payment)
                .failureReasonCategory("INSUFFICIENT_FUNDS")
                .build();
        attempt = RecoveryAttempt.builder()
                .id(UUID.randomUUID())
                .channel(RecoveryChannel.WHATSAPP)
                .build();
    }

    @Test
    @DisplayName("Should support WHATSAPP channel only")
    void shouldSupportWhatsAppChannel() {
        assertThat(executor.supports(RecoveryChannel.WHATSAPP)).isTrue();
        assertThat(executor.supports(RecoveryChannel.EMAIL)).isFalse();
        assertThat(executor.supports(RecoveryChannel.SMS)).isFalse();
        assertThat(executor.supports(RecoveryChannel.RETRY_CHARGE)).isFalse();
        assertThat(executor.supports(RecoveryChannel.SMART_LINK)).isFalse();
        assertThat(executor.supports(RecoveryChannel.MANUAL)).isFalse();
    }

    @Test
    @DisplayName("Should execute successfully and return SENT status when provider delivers message")
    void shouldExecuteSuccessfully() {
        String mockLink = "https://pay.recoverai.io/r/" + recoveryCase.getId();
        when(recoveryLinkService.generateRecoveryLink(recoveryCase)).thenReturn(mockLink);
        when(whatsappProvider.sendWhatsApp(any(WhatsAppMessageRequest.class)))
                .thenReturn(CommunicationDeliveryResult.success("wa_123", "MOCK_WHATSAPP", "WHATSAPP_DISPATCHED", "Delivered", "{}"));

        ExecutionResult result = executor.execute(attempt, recoveryCase);

        assertThat(result.getStatus()).isEqualTo(RecoveryAttemptStatus.SENT);
        assertThat(result.getResultCode()).isEqualTo("WHATSAPP_DISPATCHED");
        assertThat(result.getRecoveryLink()).isEqualTo(mockLink);

        ArgumentCaptor<WhatsAppMessageRequest> captor = ArgumentCaptor.forClass(WhatsAppMessageRequest.class);
        verify(whatsappProvider).sendWhatsApp(captor.capture());
        WhatsAppMessageRequest captured = captor.getValue();
        assertThat(captured.getRecipientPhone()).isEqualTo("+919876543210");
        assertThat(captured.getCustomerName()).isEqualTo("John Doe");
        assertThat(captured.getMerchantName()).isEqualTo("Acme Corp");
        assertThat(captured.getAmount()).isEqualTo(new BigDecimal("1999.00"));
        assertThat(captured.getRecoveryLink()).isEqualTo(mockLink);
    }

    @Test
    @DisplayName("Should return FAILED status when provider delivery fails")
    void shouldReturnFailedWhenProviderFails() {
        when(recoveryLinkService.generateRecoveryLink(recoveryCase)).thenReturn("https://pay.recoverai.io/r/123");
        when(whatsappProvider.sendWhatsApp(any(WhatsAppMessageRequest.class)))
                .thenReturn(CommunicationDeliveryResult.failure("wa_123", "MOCK_WHATSAPP", "WHATSAPP_FAILED", "Phone invalid", "{}"));

        ExecutionResult result = executor.execute(attempt, recoveryCase);

        assertThat(result.getStatus()).isEqualTo(RecoveryAttemptStatus.FAILED);
        assertThat(result.getResultCode()).isEqualTo("WHATSAPP_FAILED");
        assertThat(result.getResultMessage()).isEqualTo("Phone invalid");
    }
}
