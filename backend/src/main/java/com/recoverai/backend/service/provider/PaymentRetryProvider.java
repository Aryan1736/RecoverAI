package com.recoverai.backend.service.provider;

import com.recoverai.backend.service.provider.dto.PaymentRetryRequest;
import com.recoverai.backend.service.provider.dto.PaymentRetryResult;

public interface PaymentRetryProvider {

    PaymentRetryResult retryCharge(PaymentRetryRequest request);
}
