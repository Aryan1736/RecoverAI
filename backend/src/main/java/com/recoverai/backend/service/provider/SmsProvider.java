package com.recoverai.backend.service.provider;

import com.recoverai.backend.service.provider.dto.CommunicationDeliveryResult;
import com.recoverai.backend.service.provider.dto.SmsMessageRequest;

public interface SmsProvider {

    CommunicationDeliveryResult sendSms(SmsMessageRequest request);
}
