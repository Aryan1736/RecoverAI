package com.recoverai.backend.security;

import com.recoverai.backend.exception.InvalidCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<MerchantPrincipal> getCurrentMerchantPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof MerchantPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    public static UUID getCurrentMerchantId() {
        return getCurrentMerchantPrincipal()
                .map(MerchantPrincipal::getId)
                .orElseThrow(() -> new InvalidCredentialsException("No authenticated merchant found in security context"));
    }

    public static Optional<UUID> getOptionalCurrentMerchantId() {
        return getCurrentMerchantPrincipal().map(MerchantPrincipal::getId);
    }
}
