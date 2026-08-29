package com.recoverai.backend.service.provider.mock;

import com.recoverai.backend.service.provider.SmsProvider;
import com.recoverai.backend.service.provider.dto.CommunicationDeliveryResult;
import com.recoverai.backend.service.provider.dto.SmsMessageRequest;
import com.recoverai.backend.service.provider.health.ProviderHealthCheck;
import com.recoverai.backend.service.provider.health.ProviderHealthResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("mockSmsProvider")
public class MockSmsProvider implements SmsProvider, ProviderHealthCheck {

    private static final Logger log = LoggerFactory.getLogger(MockSmsProvider.class);

    @Override
    public CommunicationDeliveryResult sendSms(SmsMessageRequest request) {
        String deliveryId = "mock_sms_" + UUID.randomUUID().toString().substring(0, 8);
        String maskedPhone = maskPhone(request != null ? request.getRecipientPhone() : null);

        log.info("[MOCK_SMS] Dispatching SMS: deliveryId={}, recipient={}, amount={} {}",
                deliveryId, maskedPhone, request != null ? request.getAmount() : "N/A",
                request != null ? request.getCurrency() : "INR");

        String metadata = String.format("{\"provider\":\"MOCK_SMS\",\"deliveryId\":\"%s\",\"simulated\":true}",
                deliveryId);

        return CommunicationDeliveryResult.success(
                deliveryId,
                "MOCK_SMS",
                "SMS_DISPATCHED",
                "Simulated SMS recovery message dispatched to customer",
                metadata
        );
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "ANONYMOUS";
        }
        return phone.substring(0, Math.min(3, phone.length())) + "****" + phone.substring(phone.length() - 2);
    }

    @Override
    public ProviderHealthResult checkHealth() {
        return ProviderHealthResult.available("MOCK_SMS", "SMS", "Mock SMS provider active");
    }

    @Override
    public String getProviderIdentifier() {
        return "mock";
    }

    @Override
    public String getProviderCategory() {
        return "SMS";
    }
}

