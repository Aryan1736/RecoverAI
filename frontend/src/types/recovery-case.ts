import type { RecoveryChannel, RecoveryPriority, RecoveryAttemptStatus } from './analytics';

export type { RecoveryChannel, RecoveryPriority, RecoveryAttemptStatus };

export type RecoveryCaseStatus =
  | 'OPEN'
  | 'IN_PROGRESS'
  | 'RECOVERED'
  | 'FAILED'
  | 'EXPIRED'
  | 'CANCELLED';

export type PaymentStatus =
  | 'CREATED'
  | 'AUTHORIZED'
  | 'CAPTURED'
  | 'FAILED'
  | 'REFUNDED'
  | 'PENDING';

export type PaymentMethod =
  | 'CARD'
  | 'UPI'
  | 'NETBANKING'
  | 'WALLET'
  | 'EMANDATE'
  | 'OTHER';

export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH';

export interface CustomerInfo {
  id: string;
  razorpayCustomerId: string | null;
  name: string | null;
  email: string | null;
  phone: string | null;
  createdAt: string;
}

export interface PaymentInfo {
  id: string;
  razorpayPaymentId: string | null;
  razorpayOrderId: string | null;
  razorpayInvoiceId: string | null;
  amount: number;
  currency: string;
  status: PaymentStatus;
  method: PaymentMethod;
  errorCode: string | null;
  errorDescription: string | null;
  errorSource: string | null;
  errorReason: string | null;
  riskLevel: RiskLevel;
  paymentCreatedAt: string | null;
  createdAt: string;
}

export interface AgentDecision {
  id: string;
  recoveryCaseId: string;
  merchantId: string;
  recommendedAction: string;
  channel: RecoveryChannel;
  confidenceScore: number;
  reasoning: string;
  modelName: string;
  modelVersion: string;
  promptTokens: number | null;
  completionTokens: number | null;
  decisionFactors: string | null;
  createdAt: string;
}

export interface RecoveryStrategySnapshot {
  strategyId: string;
  channel: RecoveryChannel;
  recommendedAction: string;
  confidenceScore: number;
  priority: RecoveryPriority;
  fallbackChannel: RecoveryChannel | null;
  fallbackAction: string | null;
  reason: string | null;
}

export interface RecoveryAttempt {
  id: string;
  recoveryCaseId: string;
  merchantId: string;
  attemptNumber: number;
  channel: RecoveryChannel;
  status: RecoveryAttemptStatus;
  scheduledAt: string | null;
  executedAt: string | null;
  completedAt: string | null;
  resultCode: string | null;
  resultMessage: string | null;
  recoveryLink: string | null;
  strategyId: string | null;
  strategySnapshot: RecoveryStrategySnapshot | null;
  strategyPriority: RecoveryPriority | null;
  confidenceScore: number | null;
  recommendedAction: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface RecoveryCase {
  id: string;
  merchantId: string;
  paymentId: string | null;
  customerId: string | null;
  customerName: string | null;
  customerEmail: string | null;
  status: RecoveryCaseStatus;
  priority: RecoveryPriority;
  failureReasonCategory: string | null;
  estimatedRecoverableAmount: number;
  recoveredAmount: number;
  currency: string;
  expiresAt: string | null;
  recoveredAt: string | null;
  closedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface RecoveryCaseDetail {
  id: string;
  merchantId: string;
  status: RecoveryCaseStatus;
  priority: RecoveryPriority;
  failureReasonCategory: string | null;
  estimatedRecoverableAmount: number;
  recoveredAmount: number;
  currency: string;
  expiresAt: string | null;
  recoveredAt: string | null;
  closedAt: string | null;
  createdAt: string;
  updatedAt: string;
  payment: PaymentInfo | null;
  customer: CustomerInfo | null;
  attempts: RecoveryAttempt[];
  latestDiagnosis: AgentDecision | null;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number; // 0-indexed current page
  size: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface RecoveryCaseListParams {
  status?: RecoveryCaseStatus;
  priority?: RecoveryPriority;
  failureReasonCategory?: string;
  page?: number;
  size?: number;
  sort?: string;
}
