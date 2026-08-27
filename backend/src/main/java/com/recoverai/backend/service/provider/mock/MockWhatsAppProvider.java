package com.recoverai.backend.service.provider.mock;

import com.recoverai.backend.service.provider.WhatsAppProvider;
import com.recoverai.backend.service.provider.dto.CommunicationDeliveryResult;
import com.recoverai.backend.service.provider.dto.WhatsAppMessageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MockWhatsAppProvider implements WhatsAppProvider {

    private static final Logger log = LoggerFactory.getLogger(MockWhatsAppProvider.class);

    @Override
    public CommunicationDeliveryResult sendWhatsApp(WhatsAppMessageRequest request) {
        String deliveryId = "mock_wa_" + UUID.randomUUID().toString().substring(0, 8);
        String maskedPhone = maskPhone(request != null ? request.getRecipientPhone() : null);

        log.info("[MOCK_WHATSAPP] Dispatching message: deliveryId={}, recipient={}, amount={} {}",
                deliveryId, maskedPhone, request != null ? request.getAmount() : "N/A",
                request != null ? request.getCurrency() : "INR");

        String metadata = String.format("{\"provider\":\"MOCK_WHATSAPP\",\"deliveryId\":\"%s\",\"simulated\":true}",
                deliveryId);

        return CommunicationDeliveryResult.success(
                deliveryId,
                "MOCK_WHATSAPP",
                "WHATSAPP_DISPATCHED",
                "Simulated WhatsApp message dispatched to customer",
                metadata
        );
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "ANONYMOUS";
        }
        return phone.substring(0, Math.min(3, phone.length())) + "****" + phone.substring(phone.length() - 2);
    }
}
