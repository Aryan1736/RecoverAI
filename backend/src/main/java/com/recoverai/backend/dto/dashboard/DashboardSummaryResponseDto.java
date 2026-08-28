package com.recoverai.backend.dto.dashboard;

import java.math.BigDecimal;

public class DashboardSummaryResponseDto {

    private long totalRecoveryCases;
    private long openCases;
    private long inProgressCases;
    private long recoveredCases;
    private long expiredCases;
    private long cancelledCases;
    private long expiredOrCancelledCases;
    private long failedCases;
    private BigDecimal totalEstimatedRecoverableAmount;
    private BigDecimal totalRecoveredAmount;
    private BigDecimal recoveryRate;

    public DashboardSummaryResponseDto() {
    }

    public DashboardSummaryResponseDto(long totalRecoveryCases, long openCases, long inProgressCases,
                                      long recoveredCases, long expiredCases, long cancelledCases,
                                      long expiredOrCancelledCases, long failedCases,
                                      BigDecimal totalEstimatedRecoverableAmount,
                                      BigDecimal totalRecoveredAmount, BigDecimal recoveryRate) {
        this.totalRecoveryCases = totalRecoveryCases;
        this.openCases = openCases;
        this.inProgressCases = inProgressCases;
        this.recoveredCases = recoveredCases;
        this.expiredCases = expiredCases;
        this.cancelledCases = cancelledCases;
        this.expiredOrCancelledCases = expiredOrCancelledCases;
        this.failedCases = failedCases;
        this.totalEstimatedRecoverableAmount = totalEstimatedRecoverableAmount;
        this.totalRecoveredAmount = totalRecoveredAmount;
        this.recoveryRate = recoveryRate;
    }

    public long getTotalRecoveryCases() {
        return totalRecoveryCases;
    }

    public void setTotalRecoveryCases(long totalRecoveryCases) {
        this.totalRecoveryCases = totalRecoveryCases;
    }

    public long getOpenCases() {
        return openCases;
    }

    public void setOpenCases(long openCases) {
        this.openCases = openCases;
    }

    public long getInProgressCases() {
        return inProgressCases;
    }

    public void setInProgressCases(long inProgressCases) {
        this.inProgressCases = inProgressCases;
    }

    public long getRecoveredCases() {
        return recoveredCases;
    }

    public void setRecoveredCases(long recoveredCases) {
        this.recoveredCases = recoveredCases;
    }

    public long getExpiredCases() {
        return expiredCases;
    }

    public void setExpiredCases(long expiredCases) {
        this.expiredCases = expiredCases;
    }

    public long getCancelledCases() {
        return cancelledCases;
    }

    public void setCancelledCases(long cancelledCases) {
        this.cancelledCases = cancelledCases;
    }

    public long getExpiredOrCancelledCases() {
        return expiredOrCancelledCases;
    }

    public void setExpiredOrCancelledCases(long expiredOrCancelledCases) {
        this.expiredOrCancelledCases = expiredOrCancelledCases;
    }

    public long getFailedCases() {
        return failedCases;
    }

    public void setFailedCases(long failedCases) {
        this.failedCases = failedCases;
    }

    public BigDecimal getTotalEstimatedRecoverableAmount() {
        return totalEstimatedRecoverableAmount;
    }

    public void setTotalEstimatedRecoverableAmount(BigDecimal totalEstimatedRecoverableAmount) {
        this.totalEstimatedRecoverableAmount = totalEstimatedRecoverableAmount;
    }

    public BigDecimal getTotalRecoveredAmount() {
        return totalRecoveredAmount;
    }

    public void setTotalRecoveredAmount(BigDecimal totalRecoveredAmount) {
        this.totalRecoveredAmount = totalRecoveredAmount;
    }

    public BigDecimal getRecoveryRate() {
        return recoveryRate;
    }

    public void setRecoveryRate(BigDecimal recoveryRate) {
        this.recoveryRate = recoveryRate;
    }
}
