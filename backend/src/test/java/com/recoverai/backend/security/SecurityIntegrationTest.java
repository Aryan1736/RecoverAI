package com.recoverai.backend.security;

import com.recoverai.backend.client.GeminiClient;
import com.recoverai.backend.dto.diagnosis.DiagnosisContext;
import com.recoverai.backend.dto.diagnosis.StructuredDiagnosisResponse;
import com.recoverai.backend.entity.Customer;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.enums.MerchantStatus;
import com.recoverai.backend.entity.enums.PaymentMethod;
import com.recoverai.backend.entity.enums.PaymentStatus;
import com.recoverai.backend.entity.enums.RecoveryCaseStatus;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.entity.enums.RecoveryPriority;
import com.recoverai.backend.entity.enums.RiskLevel;
import com.recoverai.backend.repository.CustomerRepository;
import com.recoverai.backend.repository.MerchantRepository;
import com.recoverai.backend.repository.PaymentRepository;
import com.recoverai.backend.repository.RecoveryCaseRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RecoveryCaseRepository recoveryCaseRepository;

    @MockBean
    private GeminiClient geminiClient;

    @Value("${recoverai.security.jwt.secret}")
    private String jwtSecret;

    private Merchant merchantA;
    private Merchant merchantB;
    private RecoveryCase caseA;
    private RecoveryCase caseB;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        merchantA = merchantRepository.save(Merchant.builder()
                .name("Merchant Alpha")
                .email("alpha_" + UUID.randomUUID() + "@test.com")
                .razorpayAccountId("acc_alpha_" + UUID.randomUUID().toString().substring(0, 8))
                .webhookSecret("sec_alpha_123")
                .status(MerchantStatus.ACTIVE)
                .build());

        merchantB = merchantRepository.save(Merchant.builder()
                .name("Merchant Beta")
                .email("beta_" + UUID.randomUUID() + "@test.com")
                .status(MerchantStatus.ACTIVE)
                .build());

        tokenA = jwtTokenProvider.generateToken(merchantA);
        tokenB = jwtTokenProvider.generateToken(merchantB);

        Customer customerA = customerRepository.save(Customer.builder()
                .merchant(merchantA)
                .name("Customer Alpha")
                .email("alpha_cust_" + UUID.randomUUID() + "@example.com")
                .build());

        Customer customerB = customerRepository.save(Customer.builder()
                .merchant(merchantB)
                .name("Customer Beta")
                .email("beta_cust_" + UUID.randomUUID() + "@example.com")
                .build());

        Payment paymentA = paymentRepository.save(Payment.builder()
                .merchant(merchantA)
                .customer(customerA)
                .razorpayPaymentId("pay_a_" + UUID.randomUUID().toString().substring(0, 8))
                .amount(new BigDecimal("1000.00"))
                .currency("INR")
                .status(PaymentStatus.FAILED)
                .method(PaymentMethod.UPI)
                .riskLevel(RiskLevel.LOW)
                .build());

        Payment paymentB = paymentRepository.save(Payment.builder()
                .merchant(merchantB)
                .customer(customerB)
                .razorpayPaymentId("pay_b_" + UUID.randomUUID().toString().substring(0, 8))
                .amount(new BigDecimal("2000.00"))
                .currency("INR")
                .status(PaymentStatus.FAILED)
                .method(PaymentMethod.CARD)
                .riskLevel(RiskLevel.LOW)
                .build());

        caseA = recoveryCaseRepository.save(RecoveryCase.builder()
                .merchant(merchantA)
                .payment(paymentA)
                .customer(customerA)
                .status(RecoveryCaseStatus.OPEN)
                .priority(RecoveryPriority.HIGH)
                .failureReasonCategory("PAYMENT_DECLINED")
                .estimatedRecoverableAmount(new BigDecimal("1000.00"))
                .currency("INR")
                .build());

        caseB = recoveryCaseRepository.save(RecoveryCase.builder()
                .merchant(merchantB)
                .payment(paymentB)
                .customer(customerB)
                .status(RecoveryCaseStatus.OPEN)
                .priority(RecoveryPriority.HIGH)
                .failureReasonCategory("INSUFFICIENT_FUNDS")
                .estimatedRecoverableAmount(new BigDecimal("2000.00"))
                .currency("INR")
                .build());

        when(geminiClient.diagnose(any(DiagnosisContext.class))).thenReturn(
                StructuredDiagnosisResponse.builder()
                        .recommendedAction("SEND_LINK")
                        .recommendedChannel(RecoveryChannel.WHATSAPP)
                        .confidenceScore(new BigDecimal("0.9000"))
                        .reasoning("Valid AI decision")
                        .modelName("gemini-3.7-flash")
                        .build()
        );
    }

    @Test
    @DisplayName("Protected endpoint without Authorization header should return 401 Unauthorized")
    void testProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/recovery-cases/{id}/diagnose", caseA.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Protected endpoint with malformed token should return 401 Unauthorized")
    void testProtectedEndpointWithMalformedToken() throws Exception {
        mockMvc.perform(post("/api/v1/recovery-cases/{id}/diagnose", caseA.getId())
                        .header("Authorization", "Bearer not.a.valid.jwt.token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Protected endpoint with expired token should return 401 Unauthorized")
    void testProtectedEndpointWithExpiredToken() throws Exception {
        Instant now = Instant.now().minus(2, ChronoUnit.HOURS);
        Instant expiredAt = now.plus(1, ChronoUnit.HOURS);
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        String expiredToken = Jwts.builder()
                .issuer("RecoverAITest")
                .subject(merchantA.getId().toString())
                .claim("merchantId", merchantA.getId().toString())
                .claim("email", merchantA.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiredAt))
                .signWith(key)
                .compact();

        mockMvc.perform(post("/api/v1/recovery-cases/{id}/diagnose", caseA.getId())
                        .header("Authorization", "Bearer " + expiredToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Protected endpoint with valid token should succeed")
    void testProtectedEndpointWithValidToken() throws Exception {
        mockMvc.perform(post("/api/v1/recovery-cases/{id}/diagnose", caseA.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.recommendedAction").value("SEND_LINK"));
    }

    @Test
    @DisplayName("Tenant Isolation: Merchant A token attempting to access Merchant B via header should return 403 Forbidden")
    void testTenantIsolationHeaderSpoofingRejected() throws Exception {
        // Authenticated as Merchant A (tokenA), but sending X-Merchant-Id for Merchant B
        mockMvc.perform(post("/api/v1/recovery-cases/{id}/diagnose", caseB.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Merchant-Id", merchantB.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Authenticated merchant '" + merchantA.getId() + "' cannot access resources of merchant '" + merchantB.getId() + "'"));
    }

    @Test
    @DisplayName("Tenant Isolation: Merchant A token attempting to access Merchant B via path should return 403 Forbidden")
    void testTenantIsolationPathSpoofingRejected() throws Exception {
        // Authenticated as Merchant A (tokenA), but calling /merchants/{merchantB}/recovery-cases/{id}/diagnose
        mockMvc.perform(post("/api/v1/merchants/{merchantId}/recovery-cases/{id}/diagnose", merchantB.getId(), caseB.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("Tenant Isolation: Merchant A cannot diagnose Merchant B case even without explicit header")
    void testCannotDiagnoseOtherMerchantCase() throws Exception {
        // Merchant A tries to diagnose caseB (which belongs to Merchant B)
        mockMvc.perform(post("/api/v1/recovery-cases/{id}/diagnose", caseB.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()); // Repository query is merchant-scoped: findByIdAndMerchantId
    }

    @Test
    @DisplayName("Public endpoints should be accessible without Authorization header")
    void testPublicEndpointsAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Webhooks should be accessible without JWT (protected independently via HMAC)")
    void testWebhooksAccessibleWithoutJwt() throws Exception {
        String payload = "{\"account_id\":\"" + merchantA.getRazorpayAccountId() + "\",\"event\":\"payment.failed\"}";

        // Calling webhook endpoint without JWT but without signature returns 401 due to HMAC failure, not missing JWT
        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Missing X-Razorpay-Signature header"));
    }
}
