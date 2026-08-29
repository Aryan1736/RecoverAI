package com.recoverai.backend.service.provider.delegating;

import com.recoverai.backend.service.provider.EmailProvider;
import com.recoverai.backend.service.provider.dto.CommunicationDeliveryResult;
import com.recoverai.backend.service.provider.dto.EmailMessageRequest;
import com.recoverai.backend.service.provider.registry.ProviderFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component("delegatingEmailProvider")
public class DelegatingEmailProvider implements EmailProvider {

    private final ProviderFactory providerFactory;

    public DelegatingEmailProvider(@org.springframework.context.annotation.Lazy ProviderFactory providerFactory) {
        this.providerFactory = providerFactory;
    }

    @Override
    public CommunicationDeliveryResult sendEmail(EmailMessageRequest request) {
        return providerFactory.getActiveEmailProvider().sendEmail(request);
    }
}
