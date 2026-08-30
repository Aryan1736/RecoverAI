package com.recoverai.backend.observability;

import com.recoverai.backend.config.ObservabilityProperties;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.entity.enums.RecoveryQueueStatus;
import com.recoverai.backend.repository.RecoveryExecutionQueueRepository;
import com.recoverai.backend.service.provider.classification.ProviderFailureType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecoveryMetricsTest {

    private MeterRegistry meterRegistry;
    private ObservabilityProperties properties;

    @Mock
    private RecoveryExecutionQueueRepository queueRepository;

    private RecoveryMetrics recoveryMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        properties = new ObservabilityProperties();
        lenient().when(queueRepository.countByStatus(RecoveryQueueStatus.READY)).thenReturn(5L);

        recoveryMetrics = new RecoveryMetrics(meterRegistry, properties, queueRepository);
    }

    @Test
    @DisplayName("Should increment recovery attempt counters with channel and failure tags")
    void shouldIncrementLifecycleCounters() {
        recoveryMetrics.recordAttemptStarted(RecoveryChannel.WHATSAPP);
        recoveryMetrics.recordAttemptSucceeded(RecoveryChannel.WHATSAPP);
        recoveryMetrics.recordAttemptFailed(RecoveryChannel.EMAIL, ProviderFailureType.RATE_LIMITED);
        recoveryMetrics.recordAttemptSkipped(RecoveryChannel.SMS);
        recoveryMetrics.recordCaseRecovered(RecoveryChannel.RETRY_CHARGE);

        Counter started = meterRegistry.find(RecoveryMetrics.METRIC_ATTEMPTS_STARTED)
                .tag(RecoveryMetrics.TAG_CHANNEL, "WHATSAPP")
                .counter();
        assertThat(started).isNotNull();
        assertThat(started.count()).isEqualTo(1.0);

        Counter succeeded = meterRegistry.find(RecoveryMetrics.METRIC_ATTEMPTS_SUCCEEDED)
                .tag(RecoveryMetrics.TAG_CHANNEL, "WHATSAPP")
                .counter();
        assertThat(succeeded).isNotNull();
        assertThat(succeeded.count()).isEqualTo(1.0);

        Counter failed = meterRegistry.find(RecoveryMetrics.METRIC_ATTEMPTS_FAILED)
                .tag(RecoveryMetrics.TAG_CHANNEL, "EMAIL")
                .tag(RecoveryMetrics.TAG_FAILURE_TYPE, "RATE_LIMITED")
                .counter();
        assertThat(failed).isNotNull();
        assertThat(failed.count()).isEqualTo(1.0);

        Counter skipped = meterRegistry.find(RecoveryMetrics.METRIC_ATTEMPTS_SKIPPED)
                .tag(RecoveryMetrics.TAG_CHANNEL, "SMS")
                .counter();
        assertThat(skipped).isNotNull();
        assertThat(skipped.count()).isEqualTo(1.0);

        Counter recovered = meterRegistry.find(RecoveryMetrics.METRIC_CASES_RECOVERED)
                .tag(RecoveryMetrics.TAG_CHANNEL, "RETRY_CHARGE")
                .counter();
        assertThat(recovered).isNotNull();
        assertThat(recovered.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should increment queue operation counters")
    void shouldIncrementQueueCounters() {
        recoveryMetrics.recordQueueClaim();
        recoveryMetrics.recordQueueRetry();
        recoveryMetrics.recordQueueDeadLetter(ProviderFailureType.AUTHENTICATION);
        recoveryMetrics.recordQueueProcessingFailure();

        Counter claims = meterRegistry.find(RecoveryMetrics.METRIC_QUEUE_CLAIMS).counter();
        assertThat(claims).isNotNull();
        assertThat(claims.count()).isEqualTo(1.0);

        Counter retries = meterRegistry.find(RecoveryMetrics.METRIC_QUEUE_RETRIES).counter();
        assertThat(retries).isNotNull();
        assertThat(retries.count()).isEqualTo(1.0);

        Counter dlq = meterRegistry.find(RecoveryMetrics.METRIC_QUEUE_DEAD_LETTERS)
                .tag(RecoveryMetrics.TAG_FAILURE_TYPE, "AUTHENTICATION")
                .counter();
        assertThat(dlq).isNotNull();
        assertThat(dlq.count()).isEqualTo(1.0);

        Counter failures = meterRegistry.find(RecoveryMetrics.METRIC_QUEUE_PROCESSING_FAILURES).counter();
        assertThat(failures).isNotNull();
        assertThat(failures.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should increment provider dispatch metrics and record latency timer")
    void shouldRecordProviderMetricsAndLatency() {
        recoveryMetrics.recordProviderDispatchSuccess("TWILIO", RecoveryChannel.WHATSAPP);
        recoveryMetrics.recordProviderDispatchFailure("SENDGRID", RecoveryChannel.EMAIL, ProviderFailureType.TRANSIENT);
        recoveryMetrics.recordProviderDispatchRetryableFailure("SENDGRID", RecoveryChannel.EMAIL, ProviderFailureType.TRANSIENT);
        recoveryMetrics.recordProviderDispatchPermanentFailure("RAZORPAY", RecoveryChannel.RETRY_CHARGE, ProviderFailureType.PERMANENT);
        recoveryMetrics.recordProviderDispatchDuration("TWILIO", RecoveryChannel.WHATSAPP, "SUCCESS", Duration.ofMillis(150));

        Counter pSuccess = meterRegistry.find(RecoveryMetrics.METRIC_PROVIDER_DISPATCH_SUCCESS)
                .tag(RecoveryMetrics.TAG_PROVIDER, "TWILIO")
                .tag(RecoveryMetrics.TAG_CHANNEL, "WHATSAPP")
                .counter();
        assertThat(pSuccess).isNotNull();
        assertThat(pSuccess.count()).isEqualTo(1.0);

        Counter pFail = meterRegistry.find(RecoveryMetrics.METRIC_PROVIDER_DISPATCH_FAILURE)
                .tag(RecoveryMetrics.TAG_PROVIDER, "SENDGRID")
                .tag(RecoveryMetrics.TAG_CHANNEL, "EMAIL")
                .tag(RecoveryMetrics.TAG_FAILURE_TYPE, "TRANSIENT")
                .counter();
        assertThat(pFail).isNotNull();
        assertThat(pFail.count()).isEqualTo(1.0);

        Timer timer = meterRegistry.find(RecoveryMetrics.METRIC_PROVIDER_DISPATCH_DURATION)
                .tag(RecoveryMetrics.TAG_PROVIDER, "TWILIO")
                .tag(RecoveryMetrics.TAG_CHANNEL, "WHATSAPP")
                .tag(RecoveryMetrics.TAG_STATUS, "SUCCESS")
                .timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1L);
        assertThat(timer.totalTime(TimeUnit.MILLISECONDS)).isGreaterThanOrEqualTo(150.0);
    }

    @Test
    @DisplayName("Should report executable queue depth gauge from repository count")
    void shouldReportQueueDepthGauge() {
        Gauge gauge = meterRegistry.find(RecoveryMetrics.METRIC_QUEUE_DEPTH)
                .tag(RecoveryMetrics.TAG_STATUS, "READY")
                .gauge();

        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(5.0);
        assertThat(recoveryMetrics.getQueueDepth()).isEqualTo(5.0);
    }

    @Test
    @DisplayName("Should enforce low-cardinality: no high-cardinality tags present in any meter")
    void shouldNeverContainHighCardinalityTags() {
        recoveryMetrics.recordAttemptStarted(RecoveryChannel.EMAIL);
        recoveryMetrics.recordAttemptFailed(RecoveryChannel.EMAIL, ProviderFailureType.TRANSIENT);
        recoveryMetrics.recordProviderDispatchDuration("MOCK", RecoveryChannel.EMAIL, "SUCCESS", Duration.ofMillis(50));

        Set<String> forbiddenTagKeys = Set.of(
                "merchantId", "merchant_id",
                "customerId", "customer_id",
                "paymentId", "payment_id",
                "caseId", "recoveryCaseId",
                "attemptId", "recoveryAttemptId",
                "email", "phone", "correlationId"
        );

        for (Meter meter : meterRegistry.getMeters()) {
            for (Tag tag : meter.getId().getTags()) {
                assertThat(forbiddenTagKeys).doesNotContain(tag.getKey());
            }
        }
    }

    @Test
    @DisplayName("Should sanitize tags and handle nulls safely")
    void shouldSanitizeTags() {
        assertThat(RecoveryMetrics.sanitizeTag(null)).isEqualTo("UNKNOWN");
        assertThat(RecoveryMetrics.sanitizeTag("   ")).isEqualTo("UNKNOWN");
        assertThat(RecoveryMetrics.sanitizeTag("twilio")).isEqualTo("TWILIO");
        assertThat(RecoveryMetrics.sanitizeTag("a".repeat(50))).hasSize(32);
    }
}
