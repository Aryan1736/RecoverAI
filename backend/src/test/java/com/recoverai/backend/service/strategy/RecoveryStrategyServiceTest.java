package com.recoverai.backend.service.strategy;

import com.recoverai.backend.config.RecoveryStrategyProperties;
import com.recoverai.backend.dto.strategy.RecoveryStrategyResponseDto;
import com.recoverai.backend.entity.AgentDecision;
import com.recoverai.backend.entity.AuditEvent;
import com.recoverai.backend.entity.Customer;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.RecoveryStrategy;
import com.recoverai.backend.entity.enums.PaymentMethod;
import com.recoverai.backend.entity.enums.PaymentStatus;
import com.recoverai.backend.entity.enums.RecoveryCaseStatus;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.entity.enums.RecoveryPriority;
import com.recoverai.backend.exception.RecoveryCaseNotFoundException;
import com.recoverai.backend.exception.RecoveryStrategyNotFoundException;
import com.recoverai.backend.repository.AgentDecisionRepository;
import com.recoverai.backend.repository.AuditEventRepository;
import com.recoverai.backend.repository.RecoveryAttemptRepository;
import com.recoverai.backend.repository.RecoveryCaseRepository;
import com.recoverai.backend.repository.RecoveryStrategyRepository;
import com.recoverai.backend.service.AuditService;
import com.recoverai.backend.service.FailureReasonClassifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecoveryStrategyServiceTest {

    @Mock
    private RecoveryCaseRepository recoveryCaseRepository;

    @Mock
    private AgentDecisionRepository agentDecisionRepository;

    @Mock
    private RecoveryAttemptRepository recoveryAttemptRepository;

    @Mock
    private RecoveryStrategyRepository recoveryStrategyRepository;

    @Mock
    private AuditEventRepository auditEventRepository;

    private RecoveryStrategyEngine strategyEngine;
    private RecoveryStrategyProperties properties;
    private AuditService auditService;
    private RecoveryStrategyService strategyService;

    private Merchant merchant;
    private Customer customer;
    private Payment payment;
    private RecoveryCase recoveryCase;
    private UUID merchantId;
    private UUID recoveryCaseId;

    @BeforeEach
    void setUp() {
        properties = new RecoveryStrategyProperties();
        properties.setEnabled(true);
        properties.setMinAiConfidence(new BigDecimal("0.70"));
        properties.setMaxAttempts(3);
        properties.setRetryChargeEnabled(true);
        properties.setFallbackEnabled(true);
        properties.setMaxChannelFailures(1);
        properties.setDefaultDelaySeconds(0);
        properties.setRetryDelaySeconds(300);

        strategyEngine = new RecoveryStrategyEngine(properties);
        auditService = new AuditService(auditEventRepository);

        strategyService = new RecoveryStrategyService(
                recoveryCaseRepository,
                agentDecisionRepository,
                recoveryAttemptRepository,
                recoveryStrategyRepository,
                strategyEngine,
                properties,
                auditService
        );

        merchantId = UUID.randomUUID();
        recoveryCaseId = UUID.randomUUID();

        merchant = Merchant.builder()
                .id(merchantId)
                .name("Test Merchant")
                .email("merchant@test.com")
                .build();

        customer = Customer.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .name("John Smith")
                .email("john@test.com")
                .phone("+919123456789")
                .build();

        payment = Payment.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .customer(customer)
                .amount(new BigDecimal("5000.00"))
                .currency("INR")
                .status(PaymentStatus.FAILED)
                .method(PaymentMethod.CARD)
                .build();

        recoveryCase = RecoveryCase.builder()
                .id(recoveryCaseId)
                .merchant(merchant)
                .customer(customer)
                .payment(payment)
                .status(RecoveryCaseStatus.OPEN)
                .priority(RecoveryPriority.HIGH)
                .failureReasonCategory(FailureReasonClassifier.CATEGORY_INSUFFICIENT_FUNDS)
                .estimatedRecoverableAmount(new BigDecimal("5000.00"))
                .currency("INR")
                .build();
    }

    @Test
    @DisplayName("generateStrategy successfully computes and persists recovery strategy")
    void testGenerateStrategySuccess() {
        when(recoveryCaseRepository.findByIdAndMerchantId(recoveryCaseId, merchantId))
                .thenReturn(Optional.of(recoveryCase));

        AgentDecision decision = AgentDecision.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .recoveryCase(recoveryCase)
                .channel(RecoveryChannel.RETRY_CHARGE)
                .recommendedAction("RETRY_CHARGE")
                .confidenceScore(new BigDecimal("0.85"))
                .reasoning("High probability of funds restored")
                .build();

        when(agentDecisionRepository.findFirstByRecoveryCaseIdOrderByCreatedAtDesc(recoveryCaseId))
                .thenReturn(Optional.of(decision));

        when(recoveryAttemptRepository.findByRecoveryCaseIdOrderByAttemptNumberAsc(recoveryCaseId))
                .thenReturn(List.of());

        when(recoveryStrategyRepository.save(any(RecoveryStrategy.class)))
                .thenAnswer(invocation -> {
                    RecoveryStrategy s = invocation.getArgument(0);
                    s.setId(UUID.randomUUID());
                    s.setCreatedAt(Instant.now());
                    return s;
                });

        RecoveryStrategyResponseDto response = strategyService.generateStrategy(merchantId, recoveryCaseId);

        assertThat(response).isNotNull();
        assertThat(response.getChannel()).isEqualTo(RecoveryChannel.RETRY_CHARGE);
        assertThat(response.getRecommendedAction()).isEqualTo("RETRY_CHARGE");
        assertThat(response.getDelaySeconds()).isEqualTo(300);
        assertThat(response.isTerminal()).isFalse();

        verify(recoveryStrategyRepository).save(any(RecoveryStrategy.class));
        verify(auditEventRepository).save(any(AuditEvent.class));
    }

    @Test
    @DisplayName("generateStrategy for non-existent case throws RecoveryCaseNotFoundException")
    void testGenerateStrategyCaseNotFound() {
        when(recoveryCaseRepository.findByIdAndMerchantId(recoveryCaseId, merchantId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> strategyService.generateStrategy(merchantId, recoveryCaseId))
                .isInstanceOf(RecoveryCaseNotFoundException.class)
                .hasMessageContaining("Recovery case not found");
    }

    @Test
    @DisplayName("getLatestStrategy returns persisted strategy")
    void testGetLatestStrategySuccess() {
        when(recoveryCaseRepository.existsByIdAndMerchantId(recoveryCaseId, merchantId))
                .thenReturn(true);

        RecoveryStrategy strategy = RecoveryStrategy.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .recoveryCase(recoveryCase)
                .channel(RecoveryChannel.WHATSAPP)
                .recommendedAction("SEND_WHATSAPP_REMINDER")
                .priority(RecoveryPriority.HIGH)
                .confidenceScore(new BigDecimal("0.80"))
                .reason("AI recommended WhatsApp")
                .createdAt(Instant.now())
                .build();

        when(recoveryStrategyRepository.findFirstByRecoveryCaseIdAndMerchantIdOrderByCreatedAtDesc(recoveryCaseId, merchantId))
                .thenReturn(Optional.of(strategy));

        RecoveryStrategyResponseDto response = strategyService.getLatestStrategy(merchantId, recoveryCaseId);

        assertThat(response).isNotNull();
        assertThat(response.getChannel()).isEqualTo(RecoveryChannel.WHATSAPP);
        assertThat(response.getRecommendedAction()).isEqualTo("SEND_WHATSAPP_REMINDER");
    }

    @Test
    @DisplayName("getLatestStrategy throws RecoveryStrategyNotFoundException when no strategy exists")
    void testGetLatestStrategyNotFound() {
        when(recoveryCaseRepository.existsByIdAndMerchantId(recoveryCaseId, merchantId))
                .thenReturn(true);

        when(recoveryStrategyRepository.findFirstByRecoveryCaseIdAndMerchantIdOrderByCreatedAtDesc(recoveryCaseId, merchantId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> strategyService.getLatestStrategy(merchantId, recoveryCaseId))
                .isInstanceOf(RecoveryStrategyNotFoundException.class)
                .hasMessageContaining("No recovery strategy found");
    }

    @Test
    @DisplayName("getLatestStrategy throws RecoveryCaseNotFoundException when cross-tenant access attempted")
    void testGetLatestStrategyCrossTenant() {
        when(recoveryCaseRepository.existsByIdAndMerchantId(recoveryCaseId, merchantId))
                .thenReturn(false);

        assertThatThrownBy(() -> strategyService.getLatestStrategy(merchantId, recoveryCaseId))
                .isInstanceOf(RecoveryCaseNotFoundException.class);
    }
}
