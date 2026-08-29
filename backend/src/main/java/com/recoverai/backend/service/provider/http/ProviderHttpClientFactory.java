package com.recoverai.backend.service.provider.http;

import com.recoverai.backend.config.RecoveryCommunicationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

@Component
public class ProviderHttpClientFactory {

    private final RecoveryCommunicationProperties properties;

    public ProviderHttpClientFactory(RecoveryCommunicationProperties properties) {
        this.properties = properties;
    }

    public RestClient createClient() {
        return createClient(null, null);
    }

    public RestClient createClient(String baseUrl) {
        return createClient(baseUrl, null);
    }

    public RestClient createClient(String baseUrl, Map<String, String> defaultHeaders) {
        int connectTimeout = 5000;
        int readTimeout = 10000;

        if (properties != null && properties.getHttp() != null) {
            connectTimeout = Math.max(500, properties.getHttp().getConnectTimeoutMs());
            readTimeout = Math.max(1000, properties.getHttp().getReadTimeoutMs());
        }

        return createClient(baseUrl, connectTimeout, readTimeout, defaultHeaders);
    }

    public RestClient createClient(String baseUrl, int connectTimeoutMs, int readTimeoutMs, Map<String, String> defaultHeaders) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        RestClient.Builder builder = RestClient.builder()
                .requestFactory(requestFactory);

        if (baseUrl != null && !baseUrl.isBlank()) {
            builder.baseUrl(baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl);
        }

        if (defaultHeaders != null && !defaultHeaders.isEmpty()) {
            builder.defaultHeaders(httpHeaders -> defaultHeaders.forEach(httpHeaders::add));
        }

        return builder.build();
    }
}
