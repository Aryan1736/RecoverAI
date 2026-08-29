package com.recoverai.backend.service.provider.health;

public interface ProviderHealthCheck {

    ProviderHealthResult checkHealth();

    String getProviderIdentifier();

    String getProviderCategory();
}
