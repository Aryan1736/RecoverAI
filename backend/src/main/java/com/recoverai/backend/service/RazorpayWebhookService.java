package com.recoverai.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recoverai.backend.dto.webhook.RazorpayPaymentEntityDto;
import com.recoverai.backend.dto.webhook.RazorpayWebhookPayload;
import com.recoverai.backend.dto.webhook.WebhookResponse;
import com.recoverai.backend.entity.Customer;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.WebhookEvent;
import com.recoverai.backend.entity.enums.ActorType;
import com.recoverai.backend.entity.enums.MerchantStatus;
import com.recoverai.backend.entity.enums.PaymentMethod;
import com.recoverai.backend.entity.enums.PaymentStatus;
import com.recoverai.backend.entity.enums.RecoveryCaseStatus;
import com.recoverai.backend.entity.enums.RecoveryPriority;
import com.recoverai.backend.entity.enums.WebhookProcessingStatus;
import com.recoverai.backend.exception.MerchantResolutionException;
import com.recoverai.backend.exception.WebhookProcessingException;
import com.recoverai.backend.exception.WebhookSignatureException;
import com.recoverai.backend.repository.CustomerRepository;
import com.recoverai.backend.repository.MerchantRepository;
import com.recoverai.backend.repository.PaymentRepository;
import com.recoverai.backend.repository.RecoveryCaseRepository;
import com.recoverai.backend.repository.WebhookEventRepository;
import com.recoverai.backend.security.RazorpaySignatureVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

@Service
public class RazorpayWebhookService {

    private static final Logger log = LoggerFactory.getLogger(RazorpayWebhookService.class);

    private final MerchantRepository merchantRepository;
    private final CustomerRepository customerRepository;
    private final PaymentRepository paymentRepository;
    private final RecoveryCaseRepository recoveryCaseRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final RazorpaySignatureVerifier signatureVerifier;
    private final FailureReasonClassifier failureReasonClassifier;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public RazorpayWebhookService(MerchantRepository merchantRepository,
                                  CustomerRepository customerRepository,
                                  PaymentRepository paymentRepository,
                                  RecoveryCaseRepository recoveryCaseRepository,
                                  WebhookEventRepository webhookEventRepository,
                                  RazorpaySignatureVerifier signatureVerifier,
                                  FailureReasonClassifier failureReasonClassifier,
                                  AuditService auditService,
                                  ObjectMapper objectMapper) {
        this.merchantRepository = merchantRepository;
        this.customerRepository = customerRepository;
        this.paymentRepository = paymentRepository;
        this.recoveryCaseRepository = recoveryCaseRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.signatureVerifier = signatureVerifier;
        this.failureReasonClassifier = failureReasonClassifier;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    /**
     * Ingests and processes an incoming Razorpay webhook payload with strict verification,
     * merchant isolation, idempotency checks, payment/customer persistence, and recovery case creation.
     */
    @Transactional
    public WebhookResponse processWebhook(String rawPayload, String signatureHeader, String clientIp) {
        if (rawPayload == null || rawPayload.isBlank()) {
            throw new WebhookProcessingException("Webhook payload cannot be empty");
        }

        // 1. Parse JSON payload
        RazorpayWebhookPayload payload;
        try {
            payload = objectMapper.readValue(rawPayload, RazorpayWebhookPayload.class);
        } catch (Exception e) {
            log.warn("Failed to parse webhook JSON payload from IP {}: {}", clientIp, e.getMessage());
            auditService.recordEvent(null, "WEBHOOK_PARSE_FAILED", ActorType.WEBHOOK, "RazorpayWebhook",
                    "WEBHOOK", "UNKNOWN", "REJECTED", "Malformed JSON payload", clientIp);
            throw new WebhookProcessingException("Invalid or malformed JSON payload: " + e.getMessage(), e);
        }

        // 2. Compute payload SHA-256 hash for idempotency and tracking
        String payloadHash = calculateSha256(rawPayload);

        // 3. Resolve Merchant from account_id
        String accountId = payload.getAccountId();
        if (accountId == null || accountId.isBlank()) {
            log.warn("Webhook rejected: missing account_id in payload from IP {}", clientIp);
            auditService.recordEvent(null, "MERCHANT_RESOLUTION_FAILED", ActorType.WEBHOOK, "RazorpayWebhook",
                    "MERCHANT", "UNKNOWN", "REJECTED", "Missing account_id in webhook payload", clientIp);
            throw new MerchantResolutionException("Webhook payload is missing Razorpay account_id");
        }

        Merchant merchant = merchantRepository.findByRazorpayAccountId(accountId)
                .orElseThrow(() -> {
                    log.warn("Webhook rejected: unknown Razorpay account_id '{}' from IP {}", accountId, clientIp);
                    auditService.recordEvent(null, "MERCHANT_RESOLUTION_FAILED", ActorType.WEBHOOK, "RazorpayWebhook",
                            "MERCHANT", accountId, "REJECTED", "Merchant not found for account_id", clientIp);
                    return new MerchantResolutionException("Merchant not found for Razorpay account_id: " + accountId);
                });

        if (merchant.getStatus() != MerchantStatus.ACTIVE) {
            log.warn("Webhook rejected: merchant '{}' is not ACTIVE (status: {})", merchant.getId(), merchant.getStatus());
            auditService.recordEvent(merchant, "MERCHANT_INACTIVE", ActorType.WEBHOOK, "RazorpayWebhook",
                    "MERCHANT", merchant.getId().toString(), "REJECTED", "Merchant is not active", clientIp);
            throw new MerchantResolutionException("Merchant account is inactive or suspended");
        }

        // 4. Verify HMAC-SHA256 signature using merchant's secret
        try {
            signatureVerifier.verifySignature(rawPayload, signatureHeader, merchant.getWebhookSecret());
        } catch (WebhookSignatureException e) {
            auditService.recordEvent(merchant, "WEBHOOK_SIGNATURE_FAILED", ActorType.WEBHOOK, "RazorpayWebhook",
                    "WEBHOOK", payload.getEventId() != null ? payload.getEventId() : payloadHash,
                    "SIGNATURE_VERIFICATION_FAILED", "Invalid signature header", clientIp);
            throw e;
        }

        // Record successful webhook verification audit
        auditService.recordEvent(merchant, "WEBHOOK_RECEIVED", ActorType.WEBHOOK, "RazorpayWebhook",
                "WEBHOOK", payload.getEventId() != null ? payload.getEventId() : payloadHash,
                "RECEIVED", String.format("{\"event\":\"%s\",\"accountId\":\"%s\"}", payload.getEvent(), accountId), clientIp);

        // 5. Idempotency Check
        String eventId = payload.getEventId();
        Optional<WebhookEvent> existingEvent = Optional.empty();
        if (eventId != null && !eventId.isBlank()) {
            existingEvent = webhookEventRepository.findByMerchantIdAndRazorpayEventId(merchant.getId(), eventId);
        }
        if (existingEvent.isEmpty()) {
            existingEvent = webhookEventRepository.findByMerchantIdAndPayloadHash(merchant.getId(), payloadHash);
        }

        if (existingEvent.isPresent() && existingEvent.get().getProcessingStatus() == WebhookProcessingStatus.PROCESSED) {
            log.info("Webhook event already processed (idempotency hit). Merchant: {}, Event: {}", merchant.getId(), payload.getEvent());
            auditService.recordEvent(merchant, "WEBHOOK_DUPLICATE_SKIPPED", ActorType.WEBHOOK, "RazorpayWebhook",
                    "WEBHOOK", existingEvent.get().getId().toString(), "SKIPPED", "Duplicate webhook delivery skipped", clientIp);
            return WebhookResponse.accepted("Duplicate webhook event already processed");
        }

        // 6. Route and handle event types
        String eventType = payload.getEvent() != null ? payload.getEvent().toLowerCase(Locale.ROOT) : "";
        WebhookEvent webhookRecord = existingEvent.orElseGet(() -> WebhookEvent.builder()
                .merchant(merchant)
                .razorpayEventId(eventId)
                .eventType(eventType)
                .payloadHash(payloadHash)
                .processingStatus(WebhookProcessingStatus.PENDING)
                .receivedAt(Instant.now())
                .build());

        try {
            switch (eventType) {
                case "payment.failed", "payment.captured", "payment.authorized", "payment.refunded" -> {
                    handlePaymentEvent(merchant, payload, eventType, clientIp);
                    webhookRecord.setProcessingStatus(WebhookProcessingStatus.PROCESSED);
                    webhookRecord.setProcessedAt(Instant.now());
                }
                default -> {
                    log.info("Ignored unsupported webhook event type '{}' for merchant {}", eventType, merchant.getId());
                    webhookRecord.setProcessingStatus(WebhookProcessingStatus.IGNORED);
                    webhookRecord.setProcessedAt(Instant.now());
                    auditService.recordEvent(merchant, "WEBHOOK_EVENT_IGNORED", ActorType.WEBHOOK, "RazorpayWebhook",
                            "WEBHOOK", eventType, "IGNORED", "Unsupported event type ignored", clientIp);
                }
            }
            webhookEventRepository.save(webhookRecord);
            return WebhookResponse.accepted();
        } catch (Exception e) {
            log.error("Failed to process webhook event '{}' for merchant {}: {}", eventType, merchant.getId(), e.getMessage(), e);
            webhookRecord.setProcessingStatus(WebhookProcessingStatus.FAILED);
            webhookRecord.setErrorMessage(e.getMessage());
            webhookRecord.setProcessedAt(Instant.now());
            webhookEventRepository.save(webhookRecord);
            throw new WebhookProcessingException("Error processing webhook: " + e.getMessage(), e);
        }
    }

    private void handlePaymentEvent(Merchant merchant, RazorpayWebhookPayload payload, String eventType, String clientIp) {
        if (payload.getPayload() == null || payload.getPayload().getPayment() == null ||
                payload.getPayload().getPayment().getEntity() == null) {
            throw new WebhookProcessingException("Missing payment entity in webhook payload for event: " + eventType);
        }

        RazorpayPaymentEntityDto paymentDto = payload.getPayload().getPayment().getEntity();
        String razorpayPaymentId = paymentDto.getId();
        if (razorpayPaymentId == null || razorpayPaymentId.isBlank()) {
            throw new WebhookProcessingException("Missing payment ID in Razorpay payload");
        }

        // Customer Ingestion / Upsert
        Customer customer = resolveCustomer(merchant, paymentDto);

        // Payment Ingestion / Upsert
        Payment payment = resolveAndSavePayment(merchant, customer, paymentDto, eventType, clientIp);

        // Failed payment -> Recovery Case creation
        if (payment.getStatus() == PaymentStatus.FAILED) {
            resolveAndSaveRecoveryCase(merchant, customer, payment, paymentDto, clientIp);
        }
    }

    private Customer resolveCustomer(Merchant merchant, RazorpayPaymentEntityDto paymentDto) {
        String email = paymentDto.getEmail();
        String contact = paymentDto.getContact();
        String customerId = paymentDto.getCustomerId();

        // If email is missing, we use customer ID or payment reference to ensure a valid email format
        if (email == null || email.isBlank()) {
            if (customerId != null && !customerId.isBlank()) {
                email = customerId + "@customer.razorpay.local";
            } else {
                email = paymentDto.getId() + "@customer.razorpay.local";
            }
        }

        final String customerEmail = email.trim().toLowerCase(Locale.ROOT);

        Optional<Customer> existingCustomer = Optional.empty();
        if (customerId != null && !customerId.isBlank()) {
            existingCustomer = customerRepository.findByMerchantIdAndRazorpayCustomerId(merchant.getId(), customerId);
        }
        if (existingCustomer.isEmpty()) {
            existingCustomer = customerRepository.findByMerchantIdAndEmail(merchant.getId(), customerEmail);
        }

        if (existingCustomer.isPresent()) {
            Customer customer = existingCustomer.get();
            if (contact != null && !contact.isBlank()) {
                customer.setPhone(contact);
            }
            if (customerId != null && !customerId.isBlank() && customer.getRazorpayCustomerId() == null) {
                customer.setRazorpayCustomerId(customerId);
            }
            return customerRepository.save(customer);
        } else {
            Customer newCustomer = Customer.builder()
                    .merchant(merchant)
                    .email(customerEmail)
                    .phone(contact)
                    .razorpayCustomerId(customerId)
                    .build();
            return customerRepository.save(newCustomer);
        }
    }

    private Payment resolveAndSavePayment(Merchant merchant, Customer customer,
                                         RazorpayPaymentEntityDto paymentDto, String eventType, String clientIp) {
        String razorpayPaymentId = paymentDto.getId();
        Optional<Payment> existingPaymentOpt = paymentRepository.findByMerchantIdAndRazorpayPaymentId(merchant.getId(), razorpayPaymentId);

        BigDecimal amount = paymentDto.getAmount() != null
                ? BigDecimal.valueOf(paymentDto.getAmount()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        String currency = paymentDto.getCurrency() != null ? paymentDto.getCurrency().toUpperCase(Locale.ROOT) : "INR";
        PaymentStatus status = mapPaymentStatus(paymentDto.getStatus(), eventType);
        PaymentMethod method = mapPaymentMethod(paymentDto.getMethod());

        Instant paymentCreatedAt = paymentDto.getCreatedAt() != null
                ? Instant.ofEpochSecond(paymentDto.getCreatedAt())
                : Instant.now();

        Payment payment = existingPaymentOpt.orElseGet(() -> Payment.builder()
                .merchant(merchant)
                .razorpayPaymentId(razorpayPaymentId)
                .build());

        payment.setCustomer(customer);
        payment.setRazorpayOrderId(paymentDto.getOrderId());
        payment.setRazorpayInvoiceId(paymentDto.getInvoiceId());
        payment.setAmount(amount);
        payment.setCurrency(currency);
        payment.setStatus(status);
        payment.setMethod(method);
        payment.setErrorCode(paymentDto.getErrorCode());
        payment.setErrorDescription(paymentDto.getErrorDescription());
        payment.setErrorSource(paymentDto.getErrorSource());
        payment.setErrorReason(paymentDto.getErrorReason());
        payment.setPaymentCreatedAt(paymentCreatedAt);

        Payment savedPayment = paymentRepository.save(payment);

        String action = existingPaymentOpt.isPresent() ? "PAYMENT_UPDATED" : "PAYMENT_CREATED";
        auditService.recordEvent(merchant, action, ActorType.WEBHOOK, "RazorpayWebhook",
                "PAYMENT", savedPayment.getId().toString(), action,
                String.format("{\"razorpayPaymentId\":\"%s\",\"status\":\"%s\",\"amount\":%s}",
                        razorpayPaymentId, status.name(), amount), clientIp);

        if (status == PaymentStatus.FAILED) {
            auditService.recordEvent(merchant, "PAYMENT_FAILED", ActorType.WEBHOOK, "RazorpayWebhook",
                    "PAYMENT", savedPayment.getId().toString(), "PAYMENT_FAILED",
                    String.format("{\"errorCode\":\"%s\",\"errorReason\":\"%s\"}",
                            paymentDto.getErrorCode(), paymentDto.getErrorReason()), clientIp);
        }

        return savedPayment;
    }

    private void resolveAndSaveRecoveryCase(Merchant merchant, Customer customer, Payment payment,
                                            RazorpayPaymentEntityDto paymentDto, String clientIp) {
        Optional<RecoveryCase> existingCase = recoveryCaseRepository.findByPaymentId(payment.getId());
        if (existingCase.isPresent()) {
            log.info("Recovery case already exists for payment {}", payment.getId());
            return;
        }

        String failureCategory = failureReasonClassifier.classifyFailure(
                paymentDto.getErrorCode(),
                paymentDto.getErrorSource(),
                paymentDto.getErrorReason(),
                paymentDto.getErrorDescription()
        );

        RecoveryPriority priority = failureReasonClassifier.determinePriority(payment.getAmount());

        RecoveryCase recoveryCase = RecoveryCase.builder()
                .merchant(merchant)
                .payment(payment)
                .customer(customer)
                .status(RecoveryCaseStatus.OPEN)
                .priority(priority)
                .failureReasonCategory(failureCategory)
                .estimatedRecoverableAmount(payment.getAmount())
                .recoveredAmount(BigDecimal.ZERO)
                .currency(payment.getCurrency())
                .expiresAt(Instant.now().plus(Duration.ofDays(7)))
                .build();

        RecoveryCase savedCase = recoveryCaseRepository.save(recoveryCase);
        log.info("Created RecoveryCase {} for failed payment {}", savedCase.getId(), payment.getId());

        auditService.recordEvent(merchant, "RECOVERY_CASE_CREATED", ActorType.WEBHOOK, "RazorpayWebhook",
                "RECOVERY_CASE", savedCase.getId().toString(), "CREATED",
                String.format("{\"failureCategory\":\"%s\",\"priority\":\"%s\",\"estimatedAmount\":%s}",
                        failureCategory, priority.name(), payment.getAmount()), clientIp);
    }

    private PaymentStatus mapPaymentStatus(String statusStr, String eventType) {
        if ("payment.failed".equalsIgnoreCase(eventType) || "failed".equalsIgnoreCase(statusStr)) {
            return PaymentStatus.FAILED;
        }
        if ("payment.captured".equalsIgnoreCase(eventType) || "captured".equalsIgnoreCase(statusStr)) {
            return PaymentStatus.CAPTURED;
        }
        if ("payment.authorized".equalsIgnoreCase(eventType) || "authorized".equalsIgnoreCase(statusStr)) {
            return PaymentStatus.AUTHORIZED;
        }
        if ("payment.refunded".equalsIgnoreCase(eventType) || "refunded".equalsIgnoreCase(statusStr)) {
            return PaymentStatus.REFUNDED;
        }
        if ("created".equalsIgnoreCase(statusStr)) {
            return PaymentStatus.CREATED;
        }
        if ("pending".equalsIgnoreCase(statusStr)) {
            return PaymentStatus.PENDING;
        }
        return PaymentStatus.CREATED;
    }

    private PaymentMethod mapPaymentMethod(String methodStr) {
        if (methodStr == null) {
            return PaymentMethod.OTHER;
        }
        return switch (methodStr.toLowerCase(Locale.ROOT)) {
            case "card" -> PaymentMethod.CARD;
            case "upi" -> PaymentMethod.UPI;
            case "netbanking" -> PaymentMethod.NETBANKING;
            case "wallet" -> PaymentMethod.WALLET;
            case "emandate", "emi" -> PaymentMethod.EMANDATE;
            default -> PaymentMethod.OTHER;
        };
    }

    private String calculateSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
