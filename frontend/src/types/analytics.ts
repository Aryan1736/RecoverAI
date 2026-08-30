export type RecoveryChannel =
  | 'WHATSAPP'
  | 'EMAIL'
  | 'SMS'
  | 'RETRY_CHARGE'
  | 'SMART_LINK'
  | 'MANUAL';

export type RecoveryPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export type RecoveryAttemptStatus =
  | 'SCHEDULED'
  | 'IN_FLIGHT'
  | 'SENT'
  | 'DELIVERED'
  | 'CLICKED'
  | 'SUCCESS'
  | 'FAILED'
  | 'SKIPPED';

export interface DateRangeParams {
  from?: string; // ISO 8601 string, e.g. YYYY-MM-DD
  to?: string;   // ISO 8601 string, e.g. YYYY-MM-DD
}

export type DateRangePreset = '7d' | '30d' | '90d' | '12m' | 'custom';

export interface AnalyticsOverview {
  totalCases: number;
  openCases: number;
  inProgressCases: number;
  recoveredCases: number;
  failedCases: number;
  expiredCases: number;
  cancelledCases: number;
  expiredOrCancelledCases: number;
  totalEstimatedRecoverableAmount: number;
  totalRecoveredAmount: number;
  recoveryRate: number;
  averageRecoveredAmount: number;
  averageTimeToRecoverySeconds: number | null;
  from: string;
  to: string;
}

export interface DailyRecoveryTrend {
  date: string; // YYYY-MM-DD
  recoveryCasesCreated: number;
  amountAtRisk: number;
  amountRecovered: number;
  recoveredCaseCount: number;
  recoveryRate: number;
}

export interface RecoveryTrends {
  from: string;
  to: string;
  totalCases: number;
  totalAmountAtRisk: number;
  totalRecoveredAmount: number;
  overallRecoveryRate: number;
  trends: DailyRecoveryTrend[];
}

export interface ChannelMetric {
  channel: RecoveryChannel;
  totalAttempts: number;
  successfulAttempts: number;
  failedAttempts: number;
  sentAttempts: number;
  deliveredAttempts: number;
  clickedAttempts: number;
  successRate: number;
  recoveredAmount: number;
}

export interface ChannelAnalytics {
  from: string;
  to: string;
  totalAttempts: number;
  channels: ChannelMetric[];
}

export interface FailureCategoryMetric {
  failureReasonCategory: string;
  caseCount: number;
  estimatedRecoverableAmount: number;
  recoveredAmount: number;
  recoveredCaseCount: number;
  recoveryRate: number;
}

export interface FailurePriorityMetric {
  priority: RecoveryPriority;
  caseCount: number;
  estimatedRecoverableAmount: number;
  recoveredAmount: number;
  recoveredCaseCount: number;
  recoveryRate: number;
}

export interface FailureAnalytics {
  from: string;
  to: string;
  totalCases: number;
  categories: FailureCategoryMetric[];
  priorities: FailurePriorityMetric[];
}

export interface AttemptAnalytics {
  from: string;
  to: string;
  totalAttempts: number;
  successfulAttempts: number;
  failedAttempts: number;
  scheduledAttempts: number;
  inFlightAttempts: number;
  sentAttempts: number;
  deliveredAttempts: number;
  clickedAttempts: number;
  skippedAttempts: number;
  successRate: number;
  averageAttemptsPerRecoveryCase: number;
  attemptsByStatus: Record<RecoveryAttemptStatus, number>;
  attemptsByChannel: Record<RecoveryChannel, number>;
}
