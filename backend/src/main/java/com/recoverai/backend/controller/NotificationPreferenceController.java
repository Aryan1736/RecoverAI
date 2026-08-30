package com.recoverai.backend.controller;

import com.recoverai.backend.dto.notification.NotificationPreferenceResponseDto;
import com.recoverai.backend.dto.notification.NotificationPreferenceUpdateRequestDto;
import com.recoverai.backend.exception.InvalidCredentialsException;
import com.recoverai.backend.exception.TenantMismatchException;
import com.recoverai.backend.security.MerchantPrincipal;
import com.recoverai.backend.security.SecurityUtils;
import com.recoverai.backend.service.notification.MerchantNotificationPreferenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class NotificationPreferenceController {

    private final MerchantNotificationPreferenceService preferenceService;

    public NotificationPreferenceController(MerchantNotificationPreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GetMapping("/notification-preferences")
    public ResponseEntity<NotificationPreferenceResponseDto> getPreferences(
            @RequestHeader(value = "X-Merchant-Id", required = false) UUID headerMerchantId) {
        UUID merchantId = resolveMerchantId(headerMerchantId);
        NotificationPreferenceResponseDto response = preferenceService.getPreferences(merchantId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/merchants/{merchantId}/notification-preferences")
    public ResponseEntity<NotificationPreferenceResponseDto> getPreferencesWithPath(
            @PathVariable("merchantId") UUID pathMerchantId) {
        UUID merchantId = resolveMerchantId(pathMerchantId);
        NotificationPreferenceResponseDto response = preferenceService.getPreferences(merchantId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/notification-preferences")
    public ResponseEntity<NotificationPreferenceResponseDto> updatePreferences(
            @RequestHeader(value = "X-Merchant-Id", required = false) UUID headerMerchantId,
            @RequestBody NotificationPreferenceUpdateRequestDto request) {
        UUID merchantId = resolveMerchantId(headerMerchantId);
        String actor = resolveActor(merchantId);
        NotificationPreferenceResponseDto response = preferenceService.updatePreferences(merchantId, request, actor);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/merchants/{merchantId}/notification-preferences")
    public ResponseEntity<NotificationPreferenceResponseDto> updatePreferencesWithPath(
            @PathVariable("merchantId") UUID pathMerchantId,
            @RequestBody NotificationPreferenceUpdateRequestDto request) {
        UUID merchantId = resolveMerchantId(pathMerchantId);
        String actor = resolveActor(merchantId);
        NotificationPreferenceResponseDto response = preferenceService.updatePreferences(merchantId, request, actor);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/notification-preferences")
    public ResponseEntity<NotificationPreferenceResponseDto> patchPreferences(
            @RequestHeader(value = "X-Merchant-Id", required = false) UUID headerMerchantId,
            @RequestBody NotificationPreferenceUpdateRequestDto request) {
        return updatePreferences(headerMerchantId, request);
    }

    @PatchMapping("/merchants/{merchantId}/notification-preferences")
    public ResponseEntity<NotificationPreferenceResponseDto> patchPreferencesWithPath(
            @PathVariable("merchantId") UUID pathMerchantId,
            @RequestBody NotificationPreferenceUpdateRequestDto request) {
        return updatePreferencesWithPath(pathMerchantId, request);
    }

    private UUID resolveMerchantId(UUID explicitMerchantId) {
        Optional<MerchantPrincipal> principalOpt = SecurityUtils.getCurrentMerchantPrincipal();
        if (principalOpt.isPresent()) {
            UUID authMerchantId = principalOpt.get().getId();
            if (explicitMerchantId != null && !explicitMerchantId.equals(authMerchantId)) {
                throw new TenantMismatchException("Authenticated merchant '" + authMerchantId
                        + "' cannot access resources of merchant '" + explicitMerchantId + "'");
            }
            return authMerchantId;
        }
        if (explicitMerchantId != null) {
            return explicitMerchantId;
        }
        throw new InvalidCredentialsException("No merchant identity provided in authentication token or request");
    }

    private String resolveActor(UUID merchantId) {
        return SecurityUtils.getCurrentMerchantPrincipal()
                .map(MerchantPrincipal::getUsername)
                .orElse(merchantId.toString());
    }
}
