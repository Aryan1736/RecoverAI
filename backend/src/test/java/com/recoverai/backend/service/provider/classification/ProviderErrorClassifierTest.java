package com.recoverai.backend.service.provider.classification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderErrorClassifierTest {

    @Test
    @DisplayName("Should classify HTTP status codes deterministically")
    void shouldClassifyHttpStatus() {
        assertThat(ProviderErrorClassifier.classifyHttpStatus(429)).isEqualTo(ProviderFailureType.RATE_LIMITED);
        assertThat(ProviderErrorClassifier.classifyHttpStatus(401)).isEqualTo(ProviderFailureType.AUTHENTICATION);
        assertThat(ProviderErrorClassifier.classifyHttpStatus(403)).isEqualTo(ProviderFailureType.AUTHENTICATION);
        assertThat(ProviderErrorClassifier.classifyHttpStatus(404)).isEqualTo(ProviderFailureType.NOT_FOUND);
        assertThat(ProviderErrorClassifier.classifyHttpStatus(400)).isEqualTo(ProviderFailureType.VALIDATION);
        assertThat(ProviderErrorClassifier.classifyHttpStatus(422)).isEqualTo(ProviderFailureType.VALIDATION);
        assertThat(ProviderErrorClassifier.classifyHttpStatus(500)).isEqualTo(ProviderFailureType.TRANSIENT);
        assertThat(ProviderErrorClassifier.classifyHttpStatus(502)).isEqualTo(ProviderFailureType.TRANSIENT);
        assertThat(ProviderErrorClassifier.classifyHttpStatus(503)).isEqualTo(ProviderFailureType.TRANSIENT);
        assertThat(ProviderErrorClassifier.classifyHttpStatus(504)).isEqualTo(ProviderFailureType.TRANSIENT);
        assertThat(ProviderErrorClassifier.classifyHttpStatus(200)).isEqualTo(ProviderFailureType.UNKNOWN);
    }

    @Test
    @DisplayName("Should classify exception types deterministically")
    void shouldClassifyExceptions() {
        assertThat(ProviderErrorClassifier.classifyException(new SocketTimeoutException("Read timed out")))
                .isEqualTo(ProviderFailureType.TIMEOUT);

        assertThat(ProviderErrorClassifier.classifyException(new ConnectException("Connection refused")))
                .isEqualTo(ProviderFailureType.TRANSIENT);

        assertThat(ProviderErrorClassifier.classifyException(new ResourceAccessException("I/O error on POST request: timeout")))
                .isEqualTo(ProviderFailureType.TIMEOUT);

        assertThat(ProviderErrorClassifier.classifyException(HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", null, null, null)))
                .isEqualTo(ProviderFailureType.RATE_LIMITED);

        assertThat(ProviderErrorClassifier.classifyException(HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "Unauthorized", null, null, null)))
                .isEqualTo(ProviderFailureType.AUTHENTICATION);

        assertThat(ProviderErrorClassifier.classifyException(HttpServerErrorException.create(HttpStatus.BAD_GATEWAY, "Bad Gateway", null, null, null)))
                .isEqualTo(ProviderFailureType.TRANSIENT);
    }

    @Test
    @DisplayName("Should classify string result codes deterministically")
    void shouldClassifyResultCodes() {
        assertThat(ProviderErrorClassifier.classifyResultCode("WHATSAPP_TIMEOUT")).isEqualTo(ProviderFailureType.TIMEOUT);
        assertThat(ProviderErrorClassifier.classifyResultCode("PROVIDER_RATE_LIMITED")).isEqualTo(ProviderFailureType.RATE_LIMITED);
        assertThat(ProviderErrorClassifier.classifyResultCode("AUTH_FAILED")).isEqualTo(ProviderFailureType.AUTHENTICATION);
        assertThat(ProviderErrorClassifier.classifyResultCode("TRANSIENT_NETWORK_ERROR")).isEqualTo(ProviderFailureType.TRANSIENT);
        assertThat(ProviderErrorClassifier.classifyResultCode("INVALID_RECIPIENT")).isEqualTo(ProviderFailureType.PERMANENT);
        assertThat(ProviderErrorClassifier.classifyResultCode("RETRY_DISABLED")).isEqualTo(ProviderFailureType.PERMANENT);
    }

    @Test
    @DisplayName("isRetryable should return true only for TRANSIENT, RATE_LIMITED, and TIMEOUT")
    void shouldIdentifyRetryableFailures() {
        assertThat(ProviderErrorClassifier.isRetryable(ProviderFailureType.TRANSIENT)).isTrue();
        assertThat(ProviderErrorClassifier.isRetryable(ProviderFailureType.RATE_LIMITED)).isTrue();
        assertThat(ProviderErrorClassifier.isRetryable(ProviderFailureType.TIMEOUT)).isTrue();

        assertThat(ProviderErrorClassifier.isRetryable(ProviderFailureType.AUTHENTICATION)).isFalse();
        assertThat(ProviderErrorClassifier.isRetryable(ProviderFailureType.VALIDATION)).isFalse();
        assertThat(ProviderErrorClassifier.isRetryable(ProviderFailureType.NOT_FOUND)).isFalse();
        assertThat(ProviderErrorClassifier.isRetryable(ProviderFailureType.PERMANENT)).isFalse();
        assertThat(ProviderErrorClassifier.isRetryable(ProviderFailureType.UNKNOWN)).isFalse();
        assertThat(ProviderErrorClassifier.isRetryable(null)).isFalse();
    }
}
