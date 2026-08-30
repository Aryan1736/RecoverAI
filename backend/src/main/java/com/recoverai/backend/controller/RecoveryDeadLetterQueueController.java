package com.recoverai.backend.controller;

import com.recoverai.backend.dto.queue.DeadLetterQueueItemResponseDto;
import com.recoverai.backend.exception.InvalidCredentialsException;
import com.recoverai.backend.exception.TenantMismatchException;
import com.recoverai.backend.security.MerchantPrincipal;
import com.recoverai.backend.security.SecurityUtils;
import com.recoverai.backend.service.RecoveryDeadLetterQueueService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class RecoveryDeadLetterQueueController {

    private final RecoveryDeadLetterQueueService dlqService;

    public RecoveryDeadLetterQueueController(RecoveryDeadLetterQueueService dlqService) {
        this.dlqService = dlqService;
    }

    @GetMapping("/recovery-queue/dead-letter")
    public ResponseEntity<Page<DeadLetterQueueItemResponseDto>> listDeadLetterItems(
            @RequestHeader(value = "X-Merchant-Id", required = false) UUID headerMerchantId,
            @RequestParam(value = "caseId", required = false) UUID caseId,
            @RequestParam(value = "errorCode", required = false) String errorCode,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        UUID merchantId = resolveMerchantId(headerMerchantId);
        int pageNumber = Math.max(0, page);
        int pageSize = Math.min(Math.max(1, size), 100);
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<DeadLetterQueueItemResponseDto> response = dlqService.getDeadLetterItems(
                merchantId, caseId, errorCode, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/merchants/{merchantId}/recovery-queue/dead-letter")
    public ResponseEntity<Page<DeadLetterQueueItemResponseDto>> listDeadLetterItemsWithPath(
            @PathVariable("merchantId") UUID pathMerchantId,
            @RequestParam(value = "caseId", required = false) UUID caseId,
            @RequestParam(value = "errorCode", required = false) String errorCode,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        UUID merchantId = resolveMerchantId(pathMerchantId);
        int pageNumber = Math.max(0, page);
        int pageSize = Math.min(Math.max(1, size), 100);
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<DeadLetterQueueItemResponseDto> response = dlqService.getDeadLetterItems(
                merchantId, caseId, errorCode, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/recovery-queue/dead-letter/{id}")
    public ResponseEntity<DeadLetterQueueItemResponseDto> getDeadLetterItem(
            @RequestHeader(value = "X-Merchant-Id", required = false) UUID headerMerchantId,
            @PathVariable("id") UUID id) {
        UUID merchantId = resolveMerchantId(headerMerchantId);
        DeadLetterQueueItemResponseDto response = dlqService.getDeadLetterItem(merchantId, id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/merchants/{merchantId}/recovery-queue/dead-letter/{id}")
    public ResponseEntity<DeadLetterQueueItemResponseDto> getDeadLetterItemWithPath(
            @PathVariable("merchantId") UUID pathMerchantId,
            @PathVariable("id") UUID id) {
        UUID merchantId = resolveMerchantId(pathMerchantId);
        DeadLetterQueueItemResponseDto response = dlqService.getDeadLetterItem(merchantId, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/recovery-queue/dead-letter/{id}/redrive")
    public ResponseEntity<DeadLetterQueueItemResponseDto> redriveDeadLetterItem(
            @RequestHeader(value = "X-Merchant-Id", required = false) UUID headerMerchantId,
            @PathVariable("id") UUID id) {
        UUID merchantId = resolveMerchantId(headerMerchantId);
        String actor = resolveActor(merchantId);
        DeadLetterQueueItemResponseDto response = dlqService.redriveDeadLetterItem(merchantId, id, actor);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/merchants/{merchantId}/recovery-queue/dead-letter/{id}/redrive")
    public ResponseEntity<DeadLetterQueueItemResponseDto> redriveDeadLetterItemWithPath(
            @PathVariable("merchantId") UUID pathMerchantId,
            @PathVariable("id") UUID id) {
        UUID merchantId = resolveMerchantId(pathMerchantId);
        String actor = resolveActor(merchantId);
        DeadLetterQueueItemResponseDto response = dlqService.redriveDeadLetterItem(merchantId, id, actor);
        return ResponseEntity.ok(response);
    }

    private UUID resolveMerchantId(UUID explicitMerchantId) {
        Optional<MerchantPrincipal> principalOpt = SecurityUtils.getCurrentMerchantPrincipal();
        if (principalOpt.isPresent()) {
            UUID authMerchantId = principalOpt.get().getId();
            if (explicitMerchantId != null && !explicitMerchantId.equals(authMerchantId)) {
                throw new TenantMismatchException("Authenticated merchant '" + authMerchantId + "' cannot access resources of merchant '" + explicitMerchantId + "'");
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
