package com.recoverai.backend.observability;

import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.security.CorrelationIdFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ObservabilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RecoveryMetrics recoveryMetrics;

    @Test
    @DisplayName("Actuator /actuator/health should return 200 status and include queue and provider health components")
    void shouldReturnUpHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", anyOf(is("UP"), is("DEGRADED"))))
                .andExpect(jsonPath("$.components.recoveryExecutionQueue").exists())
                .andExpect(jsonPath("$.components.provider").exists());
    }

    @Test
    @DisplayName("Actuator liveness and readiness probe endpoints should be accessible and UP")
    void shouldExposeLivenessAndReadinessProbes() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Actuator /actuator/info should be accessible")
    void shouldExposeInfoEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Actuator /actuator/metrics should be exposed and contain RecoverAI custom metrics")
    void shouldExposeRecoverAIMetrics() throws Exception {
        recoveryMetrics.recordAttemptStarted(RecoveryChannel.EMAIL);

        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names", hasItem(RecoveryMetrics.METRIC_QUEUE_DEPTH)))
                .andExpect(jsonPath("$.names", hasItem(RecoveryMetrics.METRIC_ATTEMPTS_STARTED)));

        mockMvc.perform(get("/actuator/metrics/" + RecoveryMetrics.METRIC_QUEUE_DEPTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(RecoveryMetrics.METRIC_QUEUE_DEPTH))
                .andExpect(jsonPath("$.measurements[0].statistic").value("VALUE"));
    }

    @Test
    @DisplayName("Sensitive actuator endpoints like /actuator/env and /actuator/beans must NOT be exposed")
    void shouldNotExposeSensitiveActuatorEndpoints() throws Exception {
        // Since exposure.include is restricted to health,info,metrics, other endpoints return 404
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/actuator/beans"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/actuator/heapdump"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("X-Correlation-ID should round-trip on public /api/v1/health")
    void shouldPropagateSuppliedCorrelationId() throws Exception {
        String testCorrelationId = "test-corr-trace-8888";

        mockMvc.perform(get("/api/v1/health")
                        .header(CorrelationIdFilter.DEFAULT_HEADER_NAME, testCorrelationId))
                .andExpect(status().isOk())
                .andExpect(header().string(CorrelationIdFilter.DEFAULT_HEADER_NAME, testCorrelationId));
    }

    @Test
    @DisplayName("Missing X-Correlation-ID should result in generated UUID in response header")
    void shouldGenerateCorrelationIdWhenMissing() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String header = result.getResponse().getHeader(CorrelationIdFilter.DEFAULT_HEADER_NAME);
                    assertThat(header).isNotNull().isNotBlank();
                    assertThat(UUID.fromString(header)).isNotNull();
                });
    }

    @Test
    @DisplayName("Unsafe X-Correlation-ID should be replaced with safe UUID in response header")
    void shouldSanitizeUnsafeCorrelationId() throws Exception {
        mockMvc.perform(get("/api/v1/health")
                        .header(CorrelationIdFilter.DEFAULT_HEADER_NAME, "unsafe id with spaces;--"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String header = result.getResponse().getHeader(CorrelationIdFilter.DEFAULT_HEADER_NAME);
                    assertThat(header).isNotNull();
                    assertThat(header).isNotEqualTo("unsafe id with spaces;--");
                    assertThat(UUID.fromString(header)).isNotNull();
                });
    }
}
