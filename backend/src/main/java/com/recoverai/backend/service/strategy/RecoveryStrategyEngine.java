package com.recoverai.backend.service.strategy;

import com.recoverai.backend.config.RecoveryStrategyProperties;
import com.recoverai.backend.entity.AgentDecision;
import com.recoverai.backend.entity.Customer;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.RecoveryStrategy;
import com.recoverai.backend.entity.enums.PaymentStatus;
import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;
import com.recoverai.backend.entity.enums.RecoveryCaseStatus;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.entity.enums.RecoveryPriority;
import com.recoverai.backend.service.FailureReasonClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class RecoveryStrategyEngine {

    private static final Logger log = LoggerFactory.getLogger(RecoveryStrategyEngine.class);

    private static final Set<RecoveryCaseStatus> TERMINAL_CASE_STATUSES = Set.of(
            RecoveryCaseStatus.RECOVERED,
            RecoveryCaseStatus.CANCELLED,
            RecoveryCaseStatus.EXPIRED
    );

    private static final Set<PaymentStatus> TERMINAL_PAYMENT_STATUSES = Set.of(
            PaymentStatus.CAPTURED,
            PaymentStatus.REFUNDED
    );

    private static final Set<String> RETRY_CHARGE_ELIGIBLE_CATEGORIES = Set.of(
            FailureReasonClassifier.CATEGORY_INSUFFICIENT_FUNDS,
            FailureReasonClassifier.CATEGORY_NETWORK_ERROR,
            "system_error",
            "temporary_technical_issue",
            "technical_error",
            "payment_gateway_error",
            "timeout",
            "gateway_timeout"
    );

    private final RecoveryStrategyProperties defaultProperties;

    public RecoveryStrategyEngine(RecoveryStrategyProperties defaultProperties) {
        this.defaultProperties = defaultProperties;
    }

    /**
     * Deterministically evaluates the recovery context and produces a RecoveryStrategy.
     */
    public RecoveryStrategy evaluate(RecoveryCase recoveryCase,
                                     AgentDecision agentDecision,
                                     List<RecoveryAttempt> previousAttempts,
                                     RecoveryStrategyProperties overrideProperties) {
        RecoveryStrategyProperties config = overrideProperties != null ? overrideProperties : defaultProperties;

        Objects.requireNonNull(recoveryCase, "RecoveryCase cannot be null for strategy evaluation");

        RecoveryPriority priority = recoveryCase.getPriority() != null ? recoveryCase.getPriority() : RecoveryPriority.MEDIUM;
        int maxAttempts = config.getMaxAttempts() > 0 ? config.getMaxAttempts() : 3;

        // 1. TERMINAL CASE CHECK
        if (isTerminalState(recoveryCase)) {
            log.info("Recovery case id={} is terminal ({}), returning terminal strategy",
                    recoveryCase.getId(), recoveryCase.getStatus());
            return RecoveryStrategy.builder()
                    .merchant(recoveryCase.getMerchant())
                    .recoveryCase(recoveryCase)
                    .channel(RecoveryChannel.MANUAL)
                    .recommendedAction("NO_ACTION_TERMINAL")
                    .priority(priority)
                    .delaySeconds(0)
                    .maxAttempts(maxAttempts)
                    .confidenceScore(BigDecimal.ONE)
                    .reason("Recovery case is in terminal status: " + recoveryCase.getStatus())
                    .fallbackChannel(null)
                    .fallbackAction(null)
                    .isTerminal(true)
                    .build();
        }

        // 2. STRATEGY ENGINE DISABLED
        if (!config.isEnabled()) {
            log.info("Strategy engine is disabled by configuration, returning direct pass-through for case id={}", recoveryCase.getId());
            RecoveryChannel channel = agentDecision != null && agentDecision.getChannel() != null
                    ? agentDecision.getChannel() : RecoveryChannel.MANUAL;
            String action = agentDecision != null && agentDecision.getRecommendedAction() != null
                    ? agentDecision.getRecommendedAction() : "MANUAL_REVIEW";
            BigDecimal confidence = agentDecision != null ? agentDecision.getConfidenceScore() : BigDecimal.valueOf(0.50);

            return RecoveryStrategy.builder()
                    .merchant(recoveryCase.getMerchant())
                    .recoveryCase(recoveryCase)
                    .channel(channel)
                    .recommendedAction(action)
                    .priority(priority)
                    .delaySeconds(config.getDefaultDelaySeconds())
                    .maxAttempts(maxAttempts)
                    .confidenceScore(confidence)
                    .reason("Recovery Strategy engine is disabled by configuration; using direct pass-through strategy.")
                    .fallbackChannel(null)
                    .fallbackAction(null)
                    .isTerminal(false)
                    .build();
        }

        // 3. PREVIOUS ATTEMPTS & MAX ATTEMPTS CHECK
        int attemptCount = previousAttempts != null ? previousAttempts.size() : 0;
        if (attemptCount >= maxAttempts) {
            log.info("Recovery case id={} has reached maximum attempts ({}/{})",
                    recoveryCase.getId(), attemptCount, maxAttempts);
            return RecoveryStrategy.builder()
                    .merchant(recoveryCase.getMerchant())
                    .recoveryCase(recoveryCase)
                    .channel(RecoveryChannel.MANUAL)
                    .recommendedAction("MAX_ATTEMPTS_EXCEEDED")
                    .priority(priority)
                    .delaySeconds(0)
                    .maxAttempts(maxAttempts)
                    .confidenceScore(BigDecimal.ONE)
                    .reason(String.format("Maximum recovery attempts (%d) reached for recovery case", maxAttempts))
                    .fallbackChannel(null)
                    .fallbackAction(null)
                    .isTerminal(true)
                    .build();
        }

        // 4. CONTACT AVAILABILITY & CHANNEL FAILURE ANALYSIS
        Customer customer = recoveryCase.getCustomer();
        boolean hasPhone = customer != null && customer.getPhone() != null && !customer.getPhone().trim().isEmpty();
        boolean hasEmail = customer != null && customer.getEmail() != null && !customer.getEmail().trim().isEmpty();

        Map<RecoveryChannel, Integer> channelFailures = computeChannelFailures(previousAttempts);
        boolean retryChargeAlreadyAttempted = previousAttempts != null && previousAttempts.stream()
                .anyMatch(a -> a.getChannel() == RecoveryChannel.RETRY_CHARGE);

        int maxChannelFailures = config.getMaxChannelFailures() > 0 ? config.getMaxChannelFailures() : 1;

        BigDecimal minAiConfidence = config.getMinAiConfidence() != null ? config.getMinAiConfidence() : new BigDecimal("0.70");
        BigDecimal aiConfidence = agentDecision != null ? agentDecision.getConfidenceScore() : null;
        boolean isAiConfidenceSufficient = aiConfidence != null && aiConfidence.compareTo(minAiConfidence) >= 0;

        RecoveryChannel selectedChannel;
        String recommendedAction;
        String strategyReason;
        int delaySeconds = config.getDefaultDelaySeconds();

        // 5. LOW AI CONFIDENCE OR MISSING AI DECISION
        if (!isAiConfidenceSufficient) {
            log.info("AI confidence is low/missing for case id={} (score={}, minThreshold={}). Using conservative communication strategy.",
                    recoveryCase.getId(), aiConfidence, minAiConfidence);

            SelectedChannelResult conservativeResult = pickBestCommunicationChannel(
                    hasPhone, hasEmail, channelFailures, maxChannelFailures, null);

            selectedChannel = conservativeResult.channel;
            recommendedAction = conservativeResult.action;
            strategyReason = String.format("AI decision confidence (%s) is below threshold (%s); applied conservative %s communication strategy.",
                    aiConfidence != null ? aiConfidence.toPlainString() : "NONE",
                    minAiConfidence.toPlainString(),
                    selectedChannel);
        } else {
            // 6. HIGH CONFIDENCE AI DECISION EVALUATION
            RecoveryChannel aiChannel = agentDecision.getChannel() != null ? agentDecision.getChannel() : RecoveryChannel.MANUAL;
            String aiAction = agentDecision.getRecommendedAction() != null ? agentDecision.getRecommendedAction() : "EXECUTE_RECOVERY";

            if (aiChannel == RecoveryChannel.RETRY_CHARGE) {
                boolean retryEligible = isRetryChargeEligible(recoveryCase, config, retryChargeAlreadyAttempted);
                if (retryEligible) {
                    selectedChannel = RecoveryChannel.RETRY_CHARGE;
                    recommendedAction = aiAction;
                    delaySeconds = config.getRetryDelaySeconds();
                    strategyReason = String.format("AI recommended RETRY_CHARGE with high confidence (%s) and retry eligibility satisfied.",
                            aiConfidence.toPlainString());
                } else {
                    // Fallback from retry charge
                    SelectedChannelResult fallbackResult = pickBestCommunicationChannel(
                            hasPhone, hasEmail, channelFailures, maxChannelFailures, RecoveryChannel.RETRY_CHARGE);
                    selectedChannel = fallbackResult.channel;
                    recommendedAction = fallbackResult.action;
                    strategyReason = String.format("AI recommended RETRY_CHARGE but case is ineligible (category=%s, alreadyAttempted=%s, retryEnabled=%s); fell back to %s.",
                            recoveryCase.getFailureReasonCategory(),
                            retryChargeAlreadyAttempted,
                            config.isRetryChargeEnabled(),
                            selectedChannel);
                }
            } else {
                // Communication channel recommended by AI
                boolean isAiChannelViable = isChannelViable(aiChannel, hasPhone, hasEmail, channelFailures, maxChannelFailures);
                if (isAiChannelViable) {
                    selectedChannel = aiChannel;
                    recommendedAction = aiAction;
                    strategyReason = String.format("AI recommended channel %s selected with high confidence (%s).",
                            aiChannel, aiConfidence.toPlainString());
                } else {
                    // Fallback from AI channel due to missing contact or prior failures
                    SelectedChannelResult fallbackResult = pickBestCommunicationChannel(
                            hasPhone, hasEmail, channelFailures, maxChannelFailures, aiChannel);
                    selectedChannel = fallbackResult.channel;
                    recommendedAction = fallbackResult.action;
                    strategyReason = String.format("AI recommended channel %s is not viable (contact/failure constraint); fell back to %s.",
                            aiChannel, selectedChannel);
                }
            }
        }

        // 7. FALLBACK DETERMINATION
        RecoveryChannel fallbackChannel = null;
        String fallbackAction = null;
        if (config.isFallbackEnabled()) {
            SelectedChannelResult fallbackOption = pickBestCommunicationChannel(
                    hasPhone, hasEmail, channelFailures, maxChannelFailures, selectedChannel);
            if (fallbackOption.channel != selectedChannel) {
                fallbackChannel = fallbackOption.channel;
                fallbackAction = fallbackOption.action;
            }
        }

        return RecoveryStrategy.builder()
                .merchant(recoveryCase.getMerchant())
                .recoveryCase(recoveryCase)
                .channel(selectedChannel)
                .recommendedAction(recommendedAction)
                .priority(priority)
                .delaySeconds(delaySeconds)
                .maxAttempts(maxAttempts)
                .confidenceScore(aiConfidence != null ? aiConfidence : new BigDecimal("0.5000"))
                .reason(strategyReason)
                .fallbackChannel(fallbackChannel)
                .fallbackAction(fallbackAction)
                .isTerminal(false)
                .build();
    }

    private boolean isTerminalState(RecoveryCase recoveryCase) {
        if (recoveryCase.getStatus() != null && TERMINAL_CASE_STATUSES.contains(recoveryCase.getStatus())) {
            return true;
        }
        Payment payment = recoveryCase.getPayment();
        if (payment != null && payment.getStatus() != null && TERMINAL_PAYMENT_STATUSES.contains(payment.getStatus())) {
            return true;
        }
        return false;
    }

    private boolean isRetryChargeEligible(RecoveryCase recoveryCase,
                                         RecoveryStrategyProperties config,
                                         boolean retryChargeAlreadyAttempted) {
        if (!config.isRetryChargeEnabled()) {
            return false;
        }
        if (retryChargeAlreadyAttempted) {
            return false;
        }
        String category = recoveryCase.getFailureReasonCategory();
        if (category == null) {
            return false;
        }
        String normalizedCategory = category.trim().toLowerCase(Locale.ROOT);
        return RETRY_CHARGE_ELIGIBLE_CATEGORIES.contains(normalizedCategory);
    }

    private boolean isChannelViable(RecoveryChannel channel,
                                    boolean hasPhone,
                                    boolean hasEmail,
                                    Map<RecoveryChannel, Integer> channelFailures,
                                    int maxChannelFailures) {
        int failures = channelFailures.getOrDefault(channel, 0);
        if (failures >= maxChannelFailures) {
            return false;
        }
        return switch (channel) {
            case WHATSAPP, SMS -> hasPhone;
            case EMAIL -> hasEmail;
            case SMART_LINK -> hasPhone || hasEmail;
            case RETRY_CHARGE -> false; // Communication viability check
            case MANUAL -> true;
        };
    }

    private SelectedChannelResult pickBestCommunicationChannel(boolean hasPhone,
                                                                boolean hasEmail,
                                                                Map<RecoveryChannel, Integer> channelFailures,
                                                                int maxChannelFailures,
                                                                RecoveryChannel excludeChannel) {
        // Hierarchy of communication channels:
        // 1. WhatsApp (if phone available and not failed >= limit)
        // 2. Email (if email available and not failed >= limit)
        // 3. SMS (if phone available and not failed >= limit)
        // 4. Smart Link (if phone or email available and not failed >= limit)
        // 5. Manual Escalation

        if (hasPhone && excludeChannel != RecoveryChannel.WHATSAPP && channelFailures.getOrDefault(RecoveryChannel.WHATSAPP, 0) < maxChannelFailures) {
            return new SelectedChannelResult(RecoveryChannel.WHATSAPP, "SEND_WHATSAPP_REMINDER");
        }

        if (hasEmail && excludeChannel != RecoveryChannel.EMAIL && channelFailures.getOrDefault(RecoveryChannel.EMAIL, 0) < maxChannelFailures) {
            return new SelectedChannelResult(RecoveryChannel.EMAIL, "SEND_EMAIL_REMINDER");
        }

        if (hasPhone && excludeChannel != RecoveryChannel.SMS && channelFailures.getOrDefault(RecoveryChannel.SMS, 0) < maxChannelFailures) {
            return new SelectedChannelResult(RecoveryChannel.SMS, "SEND_SMS_REMINDER");
        }

        if ((hasPhone || hasEmail) && excludeChannel != RecoveryChannel.SMART_LINK && channelFailures.getOrDefault(RecoveryChannel.SMART_LINK, 0) < maxChannelFailures) {
            return new SelectedChannelResult(RecoveryChannel.SMART_LINK, "SEND_PAYMENT_LINK");
        }

        return new SelectedChannelResult(RecoveryChannel.MANUAL, "MANUAL_ESCALATION");
    }

    private Map<RecoveryChannel, Integer> computeChannelFailures(List<RecoveryAttempt> attempts) {
        Map<RecoveryChannel, Integer> failures = new EnumMap<>(RecoveryChannel.class);
        if (attempts == null) {
            return failures;
        }
        for (RecoveryAttempt attempt : attempts) {
            if (attempt.getStatus() == RecoveryAttemptStatus.FAILED && attempt.getChannel() != null) {
                failures.put(attempt.getChannel(), failures.getOrDefault(attempt.getChannel(), 0) + 1);
            }
        }
        return failures;
    }

    private record SelectedChannelResult(RecoveryChannel channel, String action) {
    }
}
