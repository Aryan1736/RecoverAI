package com.recoverai.backend.controller;

import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Notification;
import com.recoverai.backend.entity.enums.MerchantNotificationEvent;
import com.recoverai.backend.entity.enums.MerchantStatus;
import com.recoverai.backend.entity.enums.NotificationStatus;
import com.recoverai.backend.repository.MerchantRepository;
import com.recoverai.backend.repository.NotificationDeliveryRepository;
import com.recoverai.backend.repository.NotificationRepository;
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

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationDeliveryRepository deliveryRepository;

    @Autowired
    private com.recoverai.backend.repository.AuditEventRepository auditEventRepository;

    private Merchant merchantA;
    private Merchant merchantB;
    private String tokenA;
    private String tokenB;
    private Notification notifA;
    private Notification notifB;

    @BeforeEach
    void setUp() {
        auditEventRepository.deleteAll();
        deliveryRepository.deleteAll();
        notificationRepository.deleteAll();
        merchantRepository.deleteAll();

        merchantA = merchantRepository.save(Merchant.builder()
                .name("Merchant Alpha")
                .email("alpha-" + UUID.randomUUID() + "@merchant.com")
                .status(MerchantStatus.ACTIVE)
                .build());

        merchantB = merchantRepository.save(Merchant.builder()
                .name("Merchant Beta")
                .email("beta-" + UUID.randomUUID() + "@merchant.com")
                .status(MerchantStatus.ACTIVE)
                .build());

        tokenA = jwtTokenProvider.generateToken(merchantA);
        tokenB = jwtTokenProvider.generateToken(merchantB);

        notifA = notificationRepository.save(Notification.builder()
                .merchant(merchantA)
                .eventType(MerchantNotificationEvent.PAYMENT_RECOVERED)
                .title("Payment Recovered")
                .message("Payment of 1000 INR recovered")
                .status(NotificationStatus.UNREAD)
                .build());

        notifB = notificationRepository.save(Notification.builder()
                .merchant(merchantB)
                .eventType(MerchantNotificationEvent.CASE_EXHAUSTED)
                .title("Case Exhausted")
                .message("Case reached terminal state")
                .status(NotificationStatus.UNREAD)
                .build());
    }

    @Test
    @DisplayName("Unauthenticated request to list notifications should return 401")
    void testUnauthenticatedAccessReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Merchant A should only see their own notifications")
    void testMerchantIsolationListing() throws Exception {
        mockMvc.perform(get("/api/v1/notifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(notifA.getId().toString())))
                .andExpect(jsonPath("$.content[0].eventType", is("PAYMENT_RECOVERED")))
                .andExpect(jsonPath("$.content[0].read", is(false)));
    }

    @Test
    @DisplayName("Merchant B attempting to view Merchant A's notification should return 404")
    void testCrossTenantNotificationAccessReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/" + notifA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Explicit mismatched X-Merchant-Id header should return 403 Forbidden")
    void testMismatchedHeaderReturns403() throws Exception {
        mockMvc.perform(get("/api/v1/notifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .header("X-Merchant-Id", merchantB.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Merchant can mark their notification as read")
    void testMarkAsRead() throws Exception {
        mockMvc.perform(patch("/api/v1/notifications/" + notifA.getId() + "/read")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(notifA.getId().toString())))
                .andExpect(jsonPath("$.status", is("READ")))
                .andExpect(jsonPath("$.read", is(true)));
    }

    @Test
    @DisplayName("Merchant can mark all unread notifications as read")
    void testMarkAllAsRead() throws Exception {
        mockMvc.perform(patch("/api/v1/notifications/read-all")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.markedReadCount", is(1)))
                .andExpect(jsonPath("$.success", is(true)));
    }
}
