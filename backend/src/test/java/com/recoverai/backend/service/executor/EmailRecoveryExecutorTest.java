package com.recoverai.backend.service.executor;

import com.recoverai.backend.entity.Customer;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.service.link.RecoveryLinkService;
import com.recoverai.backend.service.provider.EmailProvider;
import com.recoverai.backend.service.provider.dto.CommunicationDeliveryResult;
import com.recoverai.backend.service.provider.dto.EmailMessageRequest;
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
class EmailRecoveryExecutorTest {

    @Mock
    private EmailProvider emailProvider;

    @Mock
    private RecoveryLinkService recoveryLinkService;

    private EmailRecoveryExecutor executor;

    private RecoveryAttempt attempt;
    private RecoveryCase recoveryCase;
    private Customer customer;
    private Merchant merchant;
    private Payment payment;

    @BeforeEach
    void setUp() {
        executor = new EmailRecoveryExecutor(emailProvider, recoveryLinkService);

        merchant = Merchant.builder().id(UUID.randomUUID()).name("Acme Corp").build();
        customer = Customer.builder().id(UUID.randomUUID()).name("Jane Doe").email("jane@example.com").build();
        payment = Payment.builder().id(UUID.randomUUID()).amount(new BigDecimal("2999.00")).currency("INR").build();
        recoveryCase = RecoveryCase.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .customer(customer)
                .payment(payment)
                .failureReasonCategory("AUTHENTICATION_FAILURE")
                .build();
        attempt = RecoveryAttempt.builder()
                .id(UUID.randomUUID())
                .channel(RecoveryChannel.EMAIL)
                .build();
    }

    @Test
    @DisplayName("Should support EMAIL channel only")
    void shouldSupportEmailChannel() {
        assertThat(executor.supports(RecoveryChannel.EMAIL)).isTrue();
        assertThat(executor.supports(RecoveryChannel.WHATSAPP)).isFalse();
        assertThat(executor.supports(RecoveryChannel.SMS)).isFalse();
    }

    @Test
    @DisplayName("Should execute successfully and return SENT status when provider delivers email")
    void shouldExecuteSuccessfully() {
        String mockLink = "https://pay.recoverai.io/r/" + recoveryCase.getId();
        when(recoveryLinkService.generateRecoveryLink(recoveryCase)).thenReturn(mockLink);
        when(emailProvider.sendEmail(any(EmailMessageRequest.class)))
                .thenReturn(CommunicationDeliveryResult.success("email_123", "MOCK_EMAIL", "EMAIL_DISPATCHED", "Delivered", "{}"));

        ExecutionResult result = executor.execute(attempt, recoveryCase);

        assertThat(result.getStatus()).isEqualTo(RecoveryAttemptStatus.SENT);
        assertThat(result.getResultCode()).isEqualTo("EMAIL_DISPATCHED");
        assertThat(result.getRecoveryLink()).isEqualTo(mockLink);

        ArgumentCaptor<EmailMessageRequest> captor = ArgumentCaptor.forClass(EmailMessageRequest.class);
        verify(emailProvider).sendEmail(captor.capture());
        EmailMessageRequest captured = captor.getValue();
        assertThat(captured.getRecipientEmail()).isEqualTo("jane@example.com");
        assertThat(captured.getCustomerName()).isEqualTo("Jane Doe");
        assertThat(captured.getMerchantName()).isEqualTo("Acme Corp");
        assertThat(captured.getAmount()).isEqualTo(new BigDecimal("2999.00"));
        assertThat(captured.getRecoveryLink()).isEqualTo(mockLink);
    }

    @Test
    @DisplayName("Should return FAILED status when provider delivery fails")
    void shouldReturnFailedWhenProviderFails() {
        when(recoveryLinkService.generateRecoveryLink(recoveryCase)).thenReturn("https://pay.recoverai.io/r/123");
        when(emailProvider.sendEmail(any(EmailMessageRequest.class)))
                .thenReturn(CommunicationDeliveryResult.failure("email_123", "MOCK_EMAIL", "EMAIL_FAILED", "Domain invalid", "{}"));

        ExecutionResult result = executor.execute(attempt, recoveryCase);

        assertThat(result.getStatus()).isEqualTo(RecoveryAttemptStatus.FAILED);
        assertThat(result.getResultCode()).isEqualTo("EMAIL_FAILED");
    }
}
