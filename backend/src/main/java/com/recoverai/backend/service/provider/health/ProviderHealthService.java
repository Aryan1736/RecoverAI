package com.recoverai.backend.service.provider.health;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class ProviderHealthService {

    private final List<ProviderHealthCheck> healthChecks;

    public ProviderHealthService(List<ProviderHealthCheck> healthChecks) {
        this.healthChecks = healthChecks != null ? healthChecks : Collections.emptyList();
    }

    public List<ProviderHealthResult> checkAll() {
        List<ProviderHealthResult> results = new ArrayList<>();
        for (ProviderHealthCheck check : healthChecks) {
            try {
                results.add(check.checkHealth());
            } catch (Exception e) {
                results.add(ProviderHealthResult.degraded(
                        check.getProviderIdentifier(),
                        check.getProviderCategory(),
                        "Health check failed: " + e.getMessage()
                ));
            }
        }
        return results;
    }

    public Optional<ProviderHealthResult> checkProvider(String category, String providerIdentifier) {
        if (category == null || providerIdentifier == null) {
            return Optional.empty();
        }
        return healthChecks.stream()
                .filter(check -> check.getProviderCategory().equalsIgnoreCase(category)
                        && check.getProviderIdentifier().equalsIgnoreCase(providerIdentifier))
                .findFirst()
                .map(ProviderHealthCheck::checkHealth);
    }
}
