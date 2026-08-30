package com.recoverai.backend.controller;

import com.recoverai.backend.dto.notification.NotificationResponseDto;
import com.recoverai.backend.entity.enums.MerchantNotificationEvent;
import com.recoverai.backend.exception.InvalidCredentialsException;
import com.recoverai.backend.exception.TenantMismatchException;
import com.recoverai.backend.security.MerchantPrincipal;
import com.recoverai.backend.security.SecurityUtils;
import com.recoverai.backend.service.notification.MerchantNotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class NotificationController {

    private final MerchantNotificationService notificationService;

    public NotificationController(MerchantNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/notifications")
    public ResponseEntity<Page<NotificationResponseDto>> listNotifications(
            @RequestHeader(value = "X-Merchant-Id", required = false) UUID headerMerchantId,
            @RequestParam(value = "unreadOnly", required = false) Boolean unreadOnly,
            @RequestParam(value = "event", required = false) MerchantNotificationEvent event,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        UUID merchantId = resolveMerchantId(headerMerchantId);
        Pageable pageable = createPageable(page, size);
        Page<NotificationResponseDto> response = notificationService.getNotifications(merchantId, unreadOnly, event, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/merchants/{merchantId}/notifications")
    public ResponseEntity<Page<NotificationResponseDto>> listNotificationsWithPath(
            @PathVariable("merchantId") UUID pathMerchantId,
            @RequestParam(value = "unreadOnly", required = false) Boolean unreadOnly,
            @RequestParam(value = "event", required = false) MerchantNotificationEvent event,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        UUID merchantId = resolveMerchantId(pathMerchantId);
        Pageable pageable = createPageable(page, size);
        Page<NotificationResponseDto> response = notificationService.getNotifications(merchantId, unreadOnly, event, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/notifications/{id}")
    public ResponseEntity<NotificationResponseDto> getNotification(
            @RequestHeader(value = "X-Merchant-Id", required = false) UUID headerMerchantId,
            @PathVariable("id") UUID id) {
        UUID merchantId = resolveMerchantId(headerMerchantId);
        NotificationResponseDto response = notificationService.getNotification(merchantId, id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/merchants/{merchantId}/notifications/{id}")
    public ResponseEntity<NotificationResponseDto> getNotificationWithPath(
            @PathVariable("merchantId") UUID pathMerchantId,
            @PathVariable("id") UUID id) {
        UUID merchantId = resolveMerchantId(pathMerchantId);
        NotificationResponseDto response = notificationService.getNotification(merchantId, id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/notifications/{id}/read")
    public ResponseEntity<NotificationResponseDto> markAsRead(
            @RequestHeader(value = "X-Merchant-Id", required = false) UUID headerMerchantId,
            @PathVariable("id") UUID id) {
        UUID merchantId = resolveMerchantId(headerMerchantId);
        String actor = resolveActor(merchantId);
        NotificationResponseDto response = notificationService.markAsRead(merchantId, id, actor);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/merchants/{merchantId}/notifications/{id}/read")
    public ResponseEntity<NotificationResponseDto> markAsReadWithPath(
            @PathVariable("merchantId") UUID pathMerchantId,
            @PathVariable("id") UUID id) {
        UUID merchantId = resolveMerchantId(pathMerchantId);
        String actor = resolveActor(merchantId);
        NotificationResponseDto response = notificationService.markAsRead(merchantId, id, actor);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/notifications/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead(
            @RequestHeader(value = "X-Merchant-Id", required = false) UUID headerMerchantId) {
        UUID merchantId = resolveMerchantId(headerMerchantId);
        String actor = resolveActor(merchantId);
        int updated = notificationService.markAllAsRead(merchantId, actor);
        return ResponseEntity.ok(Map.of("markedReadCount", updated, "success", true));
    }

    @PatchMapping("/merchants/{merchantId}/notifications/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsReadWithPath(
            @PathVariable("merchantId") UUID pathMerchantId) {
        UUID merchantId = resolveMerchantId(pathMerchantId);
        String actor = resolveActor(merchantId);
        int updated = notificationService.markAllAsRead(merchantId, actor);
        return ResponseEntity.ok(Map.of("markedReadCount", updated, "success", true));
    }

    private Pageable createPageable(int page, int size) {
        int pageNumber = Math.max(0, page);
        int pageSize = Math.min(Math.max(1, size), 100);
        return PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
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
