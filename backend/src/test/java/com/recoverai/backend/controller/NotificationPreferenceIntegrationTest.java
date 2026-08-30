package com.recoverai.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recoverai.backend.dto.notification.NotificationPreferenceUpdateRequestDto;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.enums.MerchantNotificationChannel;
import com.recoverai.backend.entity.enums.MerchantNotificationEvent;
import com.recoverai.backend.entity.enums.MerchantStatus;
import com.recoverai.backend.repository.MerchantRepository;
import com.recoverai.backend.repository.NotificationPreferenceRepository;
import com.recoverai.backend.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationPreferenceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private NotificationPreferenceRepository preferenceRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.recoverai.backend.repository.AuditEventRepository auditEventRepository;

    @Autowired
    private com.recoverai.backend.repository.NotificationRepository notificationRepository;

    @Autowired
    private com.recoverai.backend.repository.NotificationDeliveryRepository deliveryRepository;

    private Merchant merchantA;
    private Merchant merchantB;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        auditEventRepository.deleteAll();
        deliveryRepository.deleteAll();
        notificationRepository.deleteAll();
        preferenceRepository.deleteAll();
        merchantRepository.deleteAll();

        merchantA = merchantRepository.save(Merchant.builder()
                .name("Merchant Alpha")
                .email("pref-alpha-" + UUID.randomUUID() + "@merchant.com")
                .status(MerchantStatus.ACTIVE)
                .webhookUrl("https://alpha.com/webhook")
                .build());

        merchantB = merchantRepository.save(Merchant.builder()
                .name("Merchant Beta")
                .email("pref-beta-" + UUID.randomUUID() + "@merchant.com")
                .status(MerchantStatus.ACTIVE)
                .build());

        tokenA = jwtTokenProvider.generateToken(merchantA);
        tokenB = jwtTokenProvider.generateToken(merchantB);
    }

    @Test
    @DisplayName("Unauthenticated request to notification preferences should return 401")
    void testUnauthenticatedPreferencesReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/notification-preferences"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return default preferences when merchant has not configured any")
    void testGetDefaultPreferences() throws Exception {
        mockMvc.perform(get("/api/v1/notification-preferences")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.merchantId", is(merchantA.getId().toString())))
                .andExpect(jsonPath("$.webhookUrl", is("https://alpha.com/webhook")))
                .andExpect(jsonPath("$.preferences.PAYMENT_RECOVERED.EMAIL", is(true)))
                .andExpect(jsonPath("$.preferences.PAYMENT_RECOVERED.WEBHOOK", is(true)))
                .andExpect(jsonPath("$.preferences.PAYMENT_RECOVERED.IN_APP", is(true)))
                .andExpect(jsonPath("$.preferences.CASE_EXHAUSTED.WEBHOOK", is(false)));
    }

    @Test
    @DisplayName("Should update merchant preferences and webhook URL")
    void testUpdatePreferences() throws Exception {
        Map<MerchantNotificationEvent, Map<MerchantNotificationChannel, Boolean>> updateMap = new EnumMap<>(MerchantNotificationEvent.class);
        Map<MerchantNotificationChannel, Boolean> paymentPrefs = new EnumMap<>(MerchantNotificationChannel.class);
        paymentPrefs.put(MerchantNotificationChannel.EMAIL, true);
        paymentPrefs.put(MerchantNotificationChannel.WEBHOOK, false);
        paymentPrefs.put(MerchantNotificationChannel.IN_APP, true);
        updateMap.put(MerchantNotificationEvent.PAYMENT_RECOVERED, paymentPrefs);

        NotificationPreferenceUpdateRequestDto updateRequest = new NotificationPreferenceUpdateRequestDto(
                "https://alpha.com/new-webhook",
                updateMap
        );

        mockMvc.perform(put("/api/v1/notification-preferences")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.webhookUrl", is("https://alpha.com/new-webhook")))
                .andExpect(jsonPath("$.preferences.PAYMENT_RECOVERED.WEBHOOK", is(false)));

        // Verify changes persist in subsequent GET
        mockMvc.perform(get("/api/v1/notification-preferences")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.webhookUrl", is("https://alpha.com/new-webhook")))
                .andExpect(jsonPath("$.preferences.PAYMENT_RECOVERED.WEBHOOK", is(false)));
    }

    @Test
    @DisplayName("Cross-tenant preference update via header should return 403 Forbidden")
    void testCrossTenantPreferenceUpdateReturns403() throws Exception {
        NotificationPreferenceUpdateRequestDto updateRequest = new NotificationPreferenceUpdateRequestDto(
                "https://malicious.com/webhook",
                Map.of()
        );

        mockMvc.perform(put("/api/v1/notification-preferences")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .header("X-Merchant-Id", merchantB.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }
}
