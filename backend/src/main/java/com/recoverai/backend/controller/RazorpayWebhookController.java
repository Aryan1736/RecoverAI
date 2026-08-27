package com.recoverai.backend.controller;

import com.recoverai.backend.dto.webhook.WebhookResponse;
import com.recoverai.backend.service.RazorpayWebhookService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks")
public class RazorpayWebhookController {

    private final RazorpayWebhookService razorpayWebhookService;

    public RazorpayWebhookController(RazorpayWebhookService razorpayWebhookService) {
        this.razorpayWebhookService = razorpayWebhookService;
    }

    @PostMapping("/razorpay")
    public ResponseEntity<WebhookResponse> handleRazorpayWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signatureHeader,
            HttpServletRequest request) {

        String clientIp = extractClientIp(request);
        WebhookResponse response = razorpayWebhookService.processWebhook(rawPayload, signatureHeader, clientIp);
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
