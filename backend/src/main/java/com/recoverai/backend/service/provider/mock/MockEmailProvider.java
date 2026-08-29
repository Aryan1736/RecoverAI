package com.recoverai.backend.service.provider.mock;

import com.recoverai.backend.service.provider.EmailProvider;
import com.recoverai.backend.service.provider.dto.CommunicationDeliveryResult;
import com.recoverai.backend.service.provider.dto.EmailMessageRequest;
import com.recoverai.backend.service.provider.health.ProviderHealthCheck;
import com.recoverai.backend.service.provider.health.ProviderHealthResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("mockEmailProvider")
public class MockEmailProvider implements EmailProvider, ProviderHealthCheck {

    private static final Logger log = LoggerFactory.getLogger(MockEmailProvider.class);

    @Override
    public CommunicationDeliveryResult sendEmail(EmailMessageRequest request) {
        String deliveryId = "mock_email_" + UUID.randomUUID().toString().substring(0, 8);
        String maskedEmail = maskEmail(request != null ? request.getRecipientEmail() : null);

        log.info("[MOCK_EMAIL] Dispatching email: deliveryId={}, recipient={}, amount={} {}",
                deliveryId, maskedEmail, request != null ? request.getAmount() : "N/A",
                request != null ? request.getCurrency() : "INR");

        String metadata = String.format("{\"provider\":\"MOCK_EMAIL\",\"deliveryId\":\"%s\",\"simulated\":true}",
                deliveryId);

        return CommunicationDeliveryResult.success(
                deliveryId,
                "MOCK_EMAIL",
                "EMAIL_DISPATCHED",
                "Simulated recovery email dispatched to customer",
                metadata
        );
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "ANONYMOUS";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "*@" + email.substring(atIndex + 1);
        }
        return email.charAt(0) + "***@" + email.substring(atIndex + 1);
    }

    @Override
    public ProviderHealthResult checkHealth() {
        return ProviderHealthResult.available("MOCK_EMAIL", "EMAIL", "Mock Email provider active");
    }

    @Override
    public String getProviderIdentifier() {
        return "mock";
    }

    @Override
    public String getProviderCategory() {
        return "EMAIL";
    }
}

