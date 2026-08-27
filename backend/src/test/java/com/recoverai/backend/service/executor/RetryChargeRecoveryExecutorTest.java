package com.recoverai.backend.service.executor;

import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.enums.PaymentMethod;
import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.service.provider.PaymentRetryProvider;
import com.recoverai.backend.service.provider.dto.PaymentRetryRequest;
import com.recoverai.backend.service.provider.dto.PaymentRetryResult;
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
class RetryChargeRecoveryExecutorTest {

    @Mock
    private PaymentRetryProvider paymentRetryProvider;

    private RetryChargeRecoveryExecutor executor;

    private RecoveryAttempt attempt;
    private RecoveryCase recoveryCase;
    private Merchant merchant;
    private Payment payment;

    @BeforeEach
    void setUp() {
        executor = new RetryChargeRecoveryExecutor(paymentRetryProvider);

        merchant = Merchant.builder().id(UUID.randomUUID()).build();
        payment = Payment.builder()
                .id(UUID.randomUUID())
                .razorpayPaymentId("pay_abc123")
                .amount(new BigDecimal("5000.00"))
                .currency("INR")
                .method(PaymentMethod.CARD)
                .build();
        recoveryCase = RecoveryCase.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .payment(payment)
                .build();
        attempt = RecoveryAttempt.builder()
                .id(UUID.randomUUID())
                .channel(RecoveryChannel.RETRY_CHARGE)
                .build();
    }

    @Test
    @DisplayName("Should support RETRY_CHARGE channel only")
    void shouldSupportRetryCharge() {
        assertThat(executor.supports(RecoveryChannel.RETRY_CHARGE)).isTrue();
        assertThat(executor.supports(RecoveryChannel.EMAIL)).isFalse();
    }

    @Test
    @DisplayName("Should return SUCCESS status when payment retry captures successfully")
    void shouldReturnSuccessWhenRetryCaptured() {
        when(paymentRetryProvider.retryCharge(any(PaymentRetryRequest.class)))
                .thenReturn(PaymentRetryResult.success("txn_123", "MOCK_RAZORPAY", "PAYMENT_RETRY_CAPTURED", "Captured", "{}"));

        ExecutionResult result = executor.execute(attempt, recoveryCase);

        assertThat(result.getStatus()).isEqualTo(RecoveryAttemptStatus.SUCCESS);
        assertThat(result.getResultCode()).isEqualTo("PAYMENT_RETRY_CAPTURED");
        assertThat(result.getRecoveryLink()).isNull();

        ArgumentCaptor<PaymentRetryRequest> captor = ArgumentCaptor.forClass(PaymentRetryRequest.class);
        verify(paymentRetryProvider).retryCharge(captor.capture());
        PaymentRetryRequest captured = captor.getValue();
        assertThat(captured.getRazorpayPaymentId()).isEqualTo("pay_abc123");
        assertThat(captured.getAmount()).isEqualTo(new BigDecimal("5000.00"));
        assertThat(captured.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
    }

    @Test
    @DisplayName("Should return FAILED status when payment retry fails")
    void shouldReturnFailedWhenRetryFails() {
        when(paymentRetryProvider.retryCharge(any(PaymentRetryRequest.class)))
                .thenReturn(PaymentRetryResult.failure("txn_123", "MOCK_RAZORPAY", "RETRY_DECLINED", "Card expired", "{}"));

        ExecutionResult result = executor.execute(attempt, recoveryCase);

        assertThat(result.getStatus()).isEqualTo(RecoveryAttemptStatus.FAILED);
        assertThat(result.getResultCode()).isEqualTo("RETRY_DECLINED");
        assertThat(result.getResultMessage()).isEqualTo("Card expired");
    }
}
