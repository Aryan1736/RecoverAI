package com.recoverai.backend.service.provider.delegating;

import com.recoverai.backend.service.provider.PaymentRetryProvider;
import com.recoverai.backend.service.provider.dto.PaymentRetryRequest;
import com.recoverai.backend.service.provider.dto.PaymentRetryResult;
import com.recoverai.backend.service.provider.registry.ProviderFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component("delegatingPaymentRetryProvider")
public class DelegatingPaymentRetryProvider implements PaymentRetryProvider {

    private final ProviderFactory providerFactory;

    public DelegatingPaymentRetryProvider(@org.springframework.context.annotation.Lazy ProviderFactory providerFactory) {
        this.providerFactory = providerFactory;
    }

    @Override
    public PaymentRetryResult retryCharge(PaymentRetryRequest request) {
        return providerFactory.getActivePaymentRetryProvider().retryCharge(request);
    }
}
