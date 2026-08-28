package com.recoverai.backend.service;

import com.recoverai.backend.dto.dashboard.DashboardSummaryResponseDto;
import com.recoverai.backend.repository.RecoveryCaseRepository;
import com.recoverai.backend.repository.projection.DashboardSummaryProjection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

@Service
public class DashboardService {

    private final RecoveryCaseRepository recoveryCaseRepository;

    public DashboardService(RecoveryCaseRepository recoveryCaseRepository) {
        this.recoveryCaseRepository = recoveryCaseRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponseDto getDashboardSummary(UUID merchantId) {
        Objects.requireNonNull(merchantId, "Merchant ID cannot be null");

        DashboardSummaryProjection projection = recoveryCaseRepository.getDashboardSummary(merchantId);

        long totalCases = projection != null && projection.getTotalCases() != null ? projection.getTotalCases() : 0L;
        long openCases = projection != null && projection.getOpenCases() != null ? projection.getOpenCases() : 0L;
        long inProgressCases = projection != null && projection.getInProgressCases() != null ? projection.getInProgressCases() : 0L;
        long recoveredCases = projection != null && projection.getRecoveredCases() != null ? projection.getRecoveredCases() : 0L;
        long expiredCases = projection != null && projection.getExpiredCases() != null ? projection.getExpiredCases() : 0L;
        long cancelledCases = projection != null && projection.getCancelledCases() != null ? projection.getCancelledCases() : 0L;
        long failedCases = projection != null && projection.getFailedCases() != null ? projection.getFailedCases() : 0L;
        long expiredOrCancelledCases = expiredCases + cancelledCases;

        BigDecimal totalEstimated = projection != null && projection.getTotalEstimatedRecoverableAmount() != null
                ? projection.getTotalEstimatedRecoverableAmount().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalRecovered = projection != null && projection.getTotalRecoveredAmount() != null
                ? projection.getTotalRecoveredAmount().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal recoveryRate;
        if (totalCases > 0) {
            recoveryRate = BigDecimal.valueOf(recoveredCases)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalCases), 2, RoundingMode.HALF_UP);
        } else {
            recoveryRate = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return new DashboardSummaryResponseDto(
                totalCases,
                openCases,
                inProgressCases,
                recoveredCases,
                expiredCases,
                cancelledCases,
                expiredOrCancelledCases,
                failedCases,
                totalEstimated,
                totalRecovered,
                recoveryRate
        );
    }
}
