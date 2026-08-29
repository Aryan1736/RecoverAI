package com.recoverai.backend.service.provider.delegating;

import com.recoverai.backend.service.provider.SmsProvider;
import com.recoverai.backend.service.provider.dto.CommunicationDeliveryResult;
import com.recoverai.backend.service.provider.dto.SmsMessageRequest;
import com.recoverai.backend.service.provider.registry.ProviderFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component("delegatingSmsProvider")
public class DelegatingSmsProvider implements SmsProvider {

    private final ProviderFactory providerFactory;

    public DelegatingSmsProvider(@org.springframework.context.annotation.Lazy ProviderFactory providerFactory) {
        this.providerFactory = providerFactory;
    }

    @Override
    public CommunicationDeliveryResult sendSms(SmsMessageRequest request) {
        return providerFactory.getActiveSmsProvider().sendSms(request);
    }
}
