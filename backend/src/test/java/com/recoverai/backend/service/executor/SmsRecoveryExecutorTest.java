package com.recoverai.backend.service.executor;

import com.recoverai.backend.entity.Customer;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.service.link.RecoveryLinkService;
import com.recoverai.backend.service.provider.SmsProvider;
import com.recoverai.backend.service.provider.dto.CommunicationDeliveryResult;
import com.recoverai.backend.service.provider.dto.SmsMessageRequest;
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
class SmsRecoveryExecutorTest {

    @Mock
    private SmsProvider smsProvider;

    @Mock
    private RecoveryLinkService recoveryLinkService;

    private SmsRecoveryExecutor executor;

    private RecoveryAttempt attempt;
    private RecoveryCase recoveryCase;
    private Customer customer;
    private Merchant merchant;
    private Payment payment;

    @BeforeEach
    void setUp() {
        executor = new SmsRecoveryExecutor(smsProvider, recoveryLinkService);

        merchant = Merchant.builder().id(UUID.randomUUID()).name("Acme Corp").build();
        customer = Customer.builder().id(UUID.randomUUID()).name("Bob Smith").phone("+919876543211").build();
        payment = Payment.builder().id(UUID.randomUUID()).amount(new BigDecimal("999.00")).currency("INR").build();
        recoveryCase = RecoveryCase.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .customer(customer)
                .payment(payment)
                .build();
        attempt = RecoveryAttempt.builder()
                .id(UUID.randomUUID())
                .channel(RecoveryChannel.SMS)
                .build();
    }

    @Test
    @DisplayName("Should support SMS channel only")
    void shouldSupportSmsChannel() {
        assertThat(executor.supports(RecoveryChannel.SMS)).isTrue();
        assertThat(executor.supports(RecoveryChannel.EMAIL)).isFalse();
    }

    @Test
    @DisplayName("Should execute successfully and return SENT status when SMS delivered")
    void shouldExecuteSuccessfully() {
        String mockLink = "https://pay.recoverai.io/r/" + recoveryCase.getId();
        when(recoveryLinkService.generateRecoveryLink(recoveryCase)).thenReturn(mockLink);
        when(smsProvider.sendSms(any(SmsMessageRequest.class)))
                .thenReturn(CommunicationDeliveryResult.success("sms_123", "MOCK_SMS", "SMS_DISPATCHED", "Delivered", "{}"));

        ExecutionResult result = executor.execute(attempt, recoveryCase);

        assertThat(result.getStatus()).isEqualTo(RecoveryAttemptStatus.SENT);
        assertThat(result.getResultCode()).isEqualTo("SMS_DISPATCHED");
        assertThat(result.getRecoveryLink()).isEqualTo(mockLink);

        ArgumentCaptor<SmsMessageRequest> captor = ArgumentCaptor.forClass(SmsMessageRequest.class);
        verify(smsProvider).sendSms(captor.capture());
        SmsMessageRequest captured = captor.getValue();
        assertThat(captured.getRecipientPhone()).isEqualTo("+919876543211");
        assertThat(captured.getAmount()).isEqualTo(new BigDecimal("999.00"));
        assertThat(captured.getRecoveryLink()).isEqualTo(mockLink);
    }

    @Test
    @DisplayName("Should return FAILED status when SMS provider delivery fails")
    void shouldReturnFailedWhenProviderFails() {
        when(recoveryLinkService.generateRecoveryLink(recoveryCase)).thenReturn("https://pay.recoverai.io/r/123");
        when(smsProvider.sendSms(any(SmsMessageRequest.class)))
                .thenReturn(CommunicationDeliveryResult.failure("sms_123", "MOCK_SMS", "SMS_FAILED", "Gateway timeout", "{}"));

        ExecutionResult result = executor.execute(attempt, recoveryCase);

        assertThat(result.getStatus()).isEqualTo(RecoveryAttemptStatus.FAILED);
        assertThat(result.getResultCode()).isEqualTo("SMS_FAILED");
    }
}
