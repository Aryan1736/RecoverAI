package com.recoverai.backend.service;

import com.recoverai.backend.dto.dashboard.DashboardSummaryResponseDto;
import com.recoverai.backend.repository.RecoveryCaseRepository;
import com.recoverai.backend.repository.projection.DashboardSummaryProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private RecoveryCaseRepository recoveryCaseRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private UUID merchantId;

    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();
    }

    @Test
    @DisplayName("getDashboardSummary calculates aggregate metrics and recovery rate correctly")
    void testGetDashboardSummaryWithData() {
        DashboardSummaryProjection projection = new DashboardSummaryProjection() {
            @Override
            public Long getTotalCases() {
                return 10L;
            }

            @Override
            public Long getOpenCases() {
                return 3L;
            }

            @Override
            public Long getInProgressCases() {
                return 2L;
            }

            @Override
            public Long getRecoveredCases() {
                return 4L;
            }

            @Override
            public Long getExpiredCases() {
                return 1L;
            }

            @Override
            public Long getCancelledCases() {
                return 0L;
            }

            @Override
            public Long getFailedCases() {
                return 0L;
            }

            @Override
            public BigDecimal getTotalEstimatedRecoverableAmount() {
                return new BigDecimal("15000.00");
            }

            @Override
            public BigDecimal getTotalRecoveredAmount() {
                return new BigDecimal("6000.00");
            }
        };

        when(recoveryCaseRepository.getDashboardSummary(merchantId)).thenReturn(projection);

        DashboardSummaryResponseDto result = dashboardService.getDashboardSummary(merchantId);

        assertNotNull(result);
        assertEquals(10L, result.getTotalRecoveryCases());
        assertEquals(3L, result.getOpenCases());
        assertEquals(2L, result.getInProgressCases());
        assertEquals(4L, result.getRecoveredCases());
        assertEquals(1L, result.getExpiredCases());
        assertEquals(0L, result.getCancelledCases());
        assertEquals(1L, result.getExpiredOrCancelledCases());
        assertEquals(0L, result.getFailedCases());
        assertEquals(new BigDecimal("15000.00"), result.getTotalEstimatedRecoverableAmount());
        assertEquals(new BigDecimal("6000.00"), result.getTotalRecoveredAmount());
        assertEquals(new BigDecimal("40.00"), result.getRecoveryRate()); // 4 / 10 * 100 = 40.00%
    }

    @Test
    @DisplayName("getDashboardSummary handles zero cases cleanly without divide-by-zero")
    void testGetDashboardSummaryZeroCases() {
        DashboardSummaryProjection projection = new DashboardSummaryProjection() {
            @Override
            public Long getTotalCases() {
                return 0L;
            }

            @Override
            public Long getOpenCases() {
                return 0L;
            }

            @Override
            public Long getInProgressCases() {
                return 0L;
            }

            @Override
            public Long getRecoveredCases() {
                return 0L;
            }

            @Override
            public Long getExpiredCases() {
                return 0L;
            }

            @Override
            public Long getCancelledCases() {
                return 0L;
            }

            @Override
            public Long getFailedCases() {
                return 0L;
            }

            @Override
            public BigDecimal getTotalEstimatedRecoverableAmount() {
                return BigDecimal.ZERO;
            }

            @Override
            public BigDecimal getTotalRecoveredAmount() {
                return BigDecimal.ZERO;
            }
        };

        when(recoveryCaseRepository.getDashboardSummary(merchantId)).thenReturn(projection);

        DashboardSummaryResponseDto result = dashboardService.getDashboardSummary(merchantId);

        assertNotNull(result);
        assertEquals(0L, result.getTotalRecoveryCases());
        assertEquals(0L, result.getOpenCases());
        assertEquals(0L, result.getRecoveredCases());
        assertEquals(new BigDecimal("0.00"), result.getTotalEstimatedRecoverableAmount());
        assertEquals(new BigDecimal("0.00"), result.getTotalRecoveredAmount());
        assertEquals(new BigDecimal("0.00"), result.getRecoveryRate());
    }

    @Test
    @DisplayName("getDashboardSummary handles null projection safely")
    void testGetDashboardSummaryNullProjection() {
        when(recoveryCaseRepository.getDashboardSummary(merchantId)).thenReturn(null);

        DashboardSummaryResponseDto result = dashboardService.getDashboardSummary(merchantId);

        assertNotNull(result);
        assertEquals(0L, result.getTotalRecoveryCases());
        assertEquals(new BigDecimal("0.00"), result.getTotalEstimatedRecoverableAmount());
        assertEquals(new BigDecimal("0.00"), result.getTotalRecoveredAmount());
        assertEquals(new BigDecimal("0.00"), result.getRecoveryRate());
    }
}
