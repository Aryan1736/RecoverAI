package com.recoverai.backend.security;

import com.recoverai.backend.config.ObservabilityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Servlet filter that manages request correlation tracing via SLF4J MDC and HTTP headers.
 * <p>
 * Ensures every incoming HTTP request is assigned a safe, bounded correlation ID.
 * Protects against log injection by strictly validating client-supplied headers
 * and replacing invalid values with server-generated UUIDs.
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String DEFAULT_HEADER_NAME = "X-Correlation-ID";
    public static final String MDC_KEY = "correlationId";
    private static final int DEFAULT_MAX_LENGTH = 64;

    /**
     * Allowed characters: alphanumeric, hyphens, underscores.
     * Rejects control characters, newlines (\r, \n), spaces, or log injection payloads.
     */
    private static final Pattern SAFE_CORRELATION_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");

    private final ObservabilityProperties observabilityProperties;

    public CorrelationIdFilter() {
        this.observabilityProperties = new ObservabilityProperties();
    }

    public CorrelationIdFilter(ObservabilityProperties observabilityProperties) {
        this.observabilityProperties = observabilityProperties != null ? observabilityProperties : new ObservabilityProperties();
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        ObservabilityProperties.CorrelationIdProperties config = observabilityProperties.getCorrelationId();
        if (!observabilityProperties.isEnabled() || (config != null && !config.isEnabled())) {
            filterChain.doFilter(request, response);
            return;
        }

        String headerName = (config != null && StringUtils.hasText(config.getHeaderName()))
                ? config.getHeaderName()
                : DEFAULT_HEADER_NAME;
        int maxLength = (config != null && config.getMaxLength() > 0)
                ? config.getMaxLength()
                : DEFAULT_MAX_LENGTH;

        String rawCorrelationId = request.getHeader(headerName);
        String safeCorrelationId = sanitizeOrCreateCorrelationId(rawCorrelationId, maxLength);

        MDC.put(MDC_KEY, safeCorrelationId);
        response.setHeader(headerName, safeCorrelationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    /**
     * Validates and sanitizes client-provided correlation ID.
     * If absent, empty, exceeding max length, or containing unsafe characters,
     * generates a fresh UUID.
     */
    public String sanitizeOrCreateCorrelationId(String rawId, int maxLength) {
        if (!StringUtils.hasText(rawId)) {
            return UUID.randomUUID().toString();
        }

        String trimmed = rawId.trim();
        if (trimmed.length() > maxLength) {
            return UUID.randomUUID().toString();
        }

        if (!SAFE_CORRELATION_ID_PATTERN.matcher(trimmed).matches()) {
            return UUID.randomUUID().toString();
        }

        return trimmed;
    }
}
