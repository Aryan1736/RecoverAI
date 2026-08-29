package com.recoverai.backend.service.provider.delegating;

import com.recoverai.backend.service.provider.WhatsAppProvider;
import com.recoverai.backend.service.provider.dto.CommunicationDeliveryResult;
import com.recoverai.backend.service.provider.dto.WhatsAppMessageRequest;
import com.recoverai.backend.service.provider.registry.ProviderFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component("delegatingWhatsAppProvider")
public class DelegatingWhatsAppProvider implements WhatsAppProvider {

    private final ProviderFactory providerFactory;

    public DelegatingWhatsAppProvider(@org.springframework.context.annotation.Lazy ProviderFactory providerFactory) {
        this.providerFactory = providerFactory;
    }

    @Override
    public CommunicationDeliveryResult sendWhatsApp(WhatsAppMessageRequest request) {
        return providerFactory.getActiveWhatsAppProvider().sendWhatsApp(request);
    }
}
