package com.recoverai.backend.security;

import com.recoverai.backend.config.ObservabilityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CorrelationIdFilterTest {

    private ObservabilityProperties properties;
    private CorrelationIdFilter filter;

    @BeforeEach
    void setUp() {
        MDC.clear();
        properties = new ObservabilityProperties();
        filter = new CorrelationIdFilter(properties);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("Should generate UUID when X-Correlation-ID header is missing")
    void shouldGenerateUuidWhenHeaderIsMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        String correlationIdHeader = response.getHeader(CorrelationIdFilter.DEFAULT_HEADER_NAME);
        assertThat(correlationIdHeader).isNotNull().isNotBlank();
        // Assert valid UUID format
        assertThat(UUID.fromString(correlationIdHeader)).isNotNull();
        // MDC must be cleared after filter execution
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("Should preserve and propagate valid client-supplied X-Correlation-ID")
    void shouldPropagateValidCorrelationId() throws ServletException, IOException {
        String customId = "req-custom-trace-12345_abc";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.DEFAULT_HEADER_NAME, customId);
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> capturedMdc = new AtomicReference<>();
        FilterChain filterChain = (req, res) -> capturedMdc.set(MDC.get(CorrelationIdFilter.MDC_KEY));

        filter.doFilter(request, response, filterChain);

        assertThat(response.getHeader(CorrelationIdFilter.DEFAULT_HEADER_NAME)).isEqualTo(customId);
        assertThat(capturedMdc.get()).isEqualTo(customId);
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "bad\nid",
            "bad\rid",
            "bad\r\nid",
            "admin\nHTTP/1.1 200 OK",
            "attacker; drop table",
            "<script>alert(1)</script>",
            "spaces not allowed",
            "tab\tseparated"
    })
    @DisplayName("Should reject and replace log injection characters with server UUID")
    void shouldSanitizeUnsafeLogInjectionPayloads(String unsafeInput) throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.DEFAULT_HEADER_NAME, unsafeInput);
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> capturedMdc = new AtomicReference<>();
        FilterChain filterChain = (req, res) -> capturedMdc.set(MDC.get(CorrelationIdFilter.MDC_KEY));

        filter.doFilter(request, response, filterChain);

        String resultHeader = response.getHeader(CorrelationIdFilter.DEFAULT_HEADER_NAME);
        assertThat(resultHeader).isNotNull().isNotEqualTo(unsafeInput);
        assertThat(UUID.fromString(resultHeader)).isNotNull();
        assertThat(capturedMdc.get()).isEqualTo(resultHeader);
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("Should reject oversized correlation IDs exceeding configured max length")
    void shouldRejectOversizedCorrelationId() throws ServletException, IOException {
        String oversized = "a".repeat(128);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.DEFAULT_HEADER_NAME, oversized);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        String resultHeader = response.getHeader(CorrelationIdFilter.DEFAULT_HEADER_NAME);
        assertThat(resultHeader).isNotNull().isNotEqualTo(oversized);
        assertThat(UUID.fromString(resultHeader)).isNotNull();
    }

    @Test
    @DisplayName("Should clear MDC even if filter chain throws exception")
    void shouldClearMdcOnFilterFailure() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain throwingChain = (req, res) -> {
            assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNotNull();
            throw new RuntimeException("Simulated downstream filter crash");
        };

        try {
            filter.doFilter(request, response, throwingChain);
        } catch (Exception expected) {
            // Expected
        }

        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("Should isolate MDC values across repeated sequential requests on same thread")
    void shouldIsolateSequentialRequestsOnSameThread() throws ServletException, IOException {
        MockHttpServletRequest req1 = new MockHttpServletRequest();
        MockHttpServletResponse res1 = new MockHttpServletResponse();
        MockHttpServletRequest req2 = new MockHttpServletRequest();
        MockHttpServletResponse res2 = new MockHttpServletResponse();

        filter.doFilter(req1, res1, new MockFilterChain());
        String id1 = res1.getHeader(CorrelationIdFilter.DEFAULT_HEADER_NAME);

        filter.doFilter(req2, res2, new MockFilterChain());
        String id2 = res2.getHeader(CorrelationIdFilter.DEFAULT_HEADER_NAME);

        assertThat(id1).isNotNull();
        assertThat(id2).isNotNull();
        assertThat(id1).isNotEqualTo(id2);
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("Should respect disabled property")
    void shouldBypassWhenDisabled() throws ServletException, IOException {
        properties.getCorrelationId().setEnabled(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(CorrelationIdFilter.DEFAULT_HEADER_NAME)).isNull();
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }
}
