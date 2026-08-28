package com.recoverai.backend.controller;

import com.recoverai.backend.dto.webhook.WebhookResponse;
import com.recoverai.backend.service.RecoveryOutcomeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks")
public class RecoveryOutcomeWebhookController {

    private final RecoveryOutcomeService recoveryOutcomeService;

    public RecoveryOutcomeWebhookController(RecoveryOutcomeService recoveryOutcomeService) {
        this.recoveryOutcomeService = recoveryOutcomeService;
    }

    @PostMapping("/recovery-outcome")
    public ResponseEntity<WebhookResponse> handleRecoveryOutcome(
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-Recovery-Signature", required = false) String recoverySignature,
            @RequestHeader(value = "X-Webhook-Signature", required = false) String webhookSignature,
            HttpServletRequest request) {

        String signatureHeader = recoverySignature != null && !recoverySignature.isBlank()
                ? recoverySignature
                : webhookSignature;

        String clientIp = extractClientIp(request);
        WebhookResponse response = recoveryOutcomeService.processOutcomeWebhook(rawPayload, signatureHeader, clientIp);
        return ResponseEntity.ok(response);
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
