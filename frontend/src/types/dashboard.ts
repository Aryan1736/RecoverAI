export interface DashboardSummary {
  totalRecoveryCases: number;
  openCases: number;
  inProgressCases: number;
  recoveredCases: number;
  expiredCases: number;
  cancelledCases: number;
  expiredOrCancelledCases: number;
  failedCases: number;
  totalEstimatedRecoverableAmount: number;
  totalRecoveredAmount: number;
  recoveryRate: number;
}
