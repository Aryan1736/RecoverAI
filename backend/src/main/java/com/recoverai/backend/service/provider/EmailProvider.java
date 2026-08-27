package com.recoverai.backend.service.provider;

import com.recoverai.backend.service.provider.dto.CommunicationDeliveryResult;
import com.recoverai.backend.service.provider.dto.EmailMessageRequest;

public interface EmailProvider {

    CommunicationDeliveryResult sendEmail(EmailMessageRequest request);
}
