package com.recoverai.backend.service.provider;

import com.recoverai.backend.service.provider.dto.CommunicationDeliveryResult;
import com.recoverai.backend.service.provider.dto.WhatsAppMessageRequest;

public interface WhatsAppProvider {

    CommunicationDeliveryResult sendWhatsApp(WhatsAppMessageRequest request);
}
