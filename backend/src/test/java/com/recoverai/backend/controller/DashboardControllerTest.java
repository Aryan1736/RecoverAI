package com.recoverai.backend.controller;

import com.recoverai.backend.dto.dashboard.DashboardSummaryResponseDto;
import com.recoverai.backend.security.JwtTokenProvider;
import com.recoverai.backend.security.MerchantPrincipal;
import com.recoverai.backend.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private DashboardService dashboardService;

    private UUID merchantId;
    private String validToken;
    private DashboardSummaryResponseDto sampleSummary;

    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();
        com.recoverai.backend.entity.Merchant merchant = com.recoverai.backend.entity.Merchant.builder()
                .id(merchantId)
                .name("Test Merchant")
                .email("test@merchant.com")
                .status(com.recoverai.backend.entity.enums.MerchantStatus.ACTIVE)
                .build();
        validToken = jwtTokenProvider.generateToken(merchant);

        sampleSummary = new DashboardSummaryResponseDto(
                25L, 5L, 10L, 8L, 1L, 1L, 2L, 0L,
                new BigDecimal("50000.00"), new BigDecimal("16000.00"), new BigDecimal("32.00")
        );
    }

    @Test
    @DisplayName("GET /api/v1/dashboard/summary without token returns 401 Unauthorized")
    void testGetSummaryUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/dashboard/summary with valid token returns aggregate metrics")
    void testGetSummaryAuthenticated() throws Exception {
        when(dashboardService.getDashboardSummary(merchantId)).thenReturn(sampleSummary);

        mockMvc.perform(get("/api/v1/dashboard/summary")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecoveryCases").value(25))
                .andExpect(jsonPath("$.openCases").value(5))
                .andExpect(jsonPath("$.inProgressCases").value(10))
                .andExpect(jsonPath("$.recoveredCases").value(8))
                .andExpect(jsonPath("$.expiredCases").value(1))
                .andExpect(jsonPath("$.cancelledCases").value(1))
                .andExpect(jsonPath("$.expiredOrCancelledCases").value(2))
                .andExpect(jsonPath("$.totalEstimatedRecoverableAmount").value(50000.00))
                .andExpect(jsonPath("$.totalRecoveredAmount").value(16000.00))
                .andExpect(jsonPath("$.recoveryRate").value(32.00));
    }

    @Test
    @DisplayName("GET /api/v1/dashboard/summary with mismatched X-Merchant-Id header returns 403 Forbidden")
    void testGetSummaryMismatchedHeader() throws Exception {
        UUID otherMerchantId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/dashboard/summary")
                        .header("Authorization", "Bearer " + validToken)
                        .header("X-Merchant-Id", otherMerchantId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }
}
