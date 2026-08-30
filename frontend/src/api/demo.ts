import type { DashboardSummary } from '../types/dashboard';
import type {
  RecoveryCase,
  RecoveryCaseDetail,
  PageResponse,
  RecoveryCaseListParams,
} from '../types/recovery-case';
import type {
  AnalyticsOverview,
  RecoveryTrends,
  ChannelAnalytics,
  FailureAnalytics,
  AttemptAnalytics,
  DateRangeParams,
} from '../types/analytics';
import type {
  NotificationResponseDto,
  NotificationPreferenceResponseDto,
} from '../types/notifications';
import type { ProviderHealthSummary } from '../types/providers';

// In-memory Simulated Demo Data Fixtures

export const DEMO_DASHBOARD_SUMMARY: DashboardSummary = {
  totalRecoveryCases: 38,
  openCases: 7,
  inProgressCases: 4,
  recoveredCases: 24,
  expiredCases: 2,
  cancelledCases: 1,
  expiredOrCancelledCases: 3,
  failedCases: 0,
  totalEstimatedRecoverableAmount: 184500.0,
  totalRecoveredAmount: 142000.0,
  recoveryRate: 75.0,
};

export const DEMO_CASES: RecoveryCase[] = [
  {
    id: 'demo-case-001',
    merchantId: 'demo-merchant-evaluator',
    paymentId: 'demo_pay_8912',
    customerId: 'demo_cust_101',
    customerName: 'Aarav Sharma (Simulated)',
    customerEmail: 'aarav.sharma@example.com',
    status: 'IN_PROGRESS',
    priority: 'HIGH',
    failureReasonCategory: 'AUTHENTICATION',
    estimatedRecoverableAmount: 4999.0,
    recoveredAmount: 0,
    currency: 'INR',
    expiresAt: '2026-09-02T12:00:00Z',
    recoveredAt: null,
    closedAt: null,
    createdAt: '2026-08-30T10:00:00Z',
    updatedAt: '2026-08-30T10:30:00Z',
  },
  {
    id: 'demo-case-002',
    merchantId: 'demo-merchant-evaluator',
    paymentId: 'demo_pay_8913',
    customerId: 'demo_cust_102',
    customerName: 'Priya Patel (Simulated)',
    customerEmail: 'priya.patel@example.com',
    status: 'RECOVERED',
    priority: 'CRITICAL',
    failureReasonCategory: 'INSUFFICIENT_FUNDS',
    estimatedRecoverableAmount: 12500.0,
    recoveredAmount: 12500.0,
    currency: 'INR',
    expiresAt: '2026-09-01T15:00:00Z',
    recoveredAt: '2026-08-30T11:45:00Z',
    closedAt: '2026-08-30T11:45:00Z',
    createdAt: '2026-08-30T09:15:00Z',
    updatedAt: '2026-08-30T11:45:00Z',
  },
  {
    id: 'demo-case-003',
    merchantId: 'demo-merchant-evaluator',
    paymentId: 'demo_pay_8914',
    customerId: 'demo_cust_103',
    customerName: 'Rohan Gupta (Simulated)',
    customerEmail: 'rohan.gupta@example.com',
    status: 'OPEN',
    priority: 'MEDIUM',
    failureReasonCategory: 'NETWORK_TIMEOUT',
    estimatedRecoverableAmount: 1999.0,
    recoveredAmount: 0,
    currency: 'INR',
    expiresAt: '2026-09-03T18:00:00Z',
    recoveredAt: null,
    closedAt: null,
    createdAt: '2026-08-30T14:20:00Z',
    updatedAt: '2026-08-30T14:20:00Z',
  },
  {
    id: 'demo-case-004',
    merchantId: 'demo-merchant-evaluator',
    paymentId: 'demo_pay_8915',
    customerId: 'demo_cust_104',
    customerName: 'Ananya Verma (Simulated)',
    customerEmail: 'ananya.verma@example.com',
    status: 'RECOVERED',
    priority: 'LOW',
    failureReasonCategory: 'USER_DROPOFF',
    estimatedRecoverableAmount: 899.0,
    recoveredAmount: 899.0,
    currency: 'INR',
    expiresAt: '2026-09-01T08:00:00Z',
    recoveredAt: '2026-08-30T10:10:00Z',
    closedAt: '2026-08-30T10:10:00Z',
    createdAt: '2026-08-30T08:00:00Z',
    updatedAt: '2026-08-30T10:10:00Z',
  },
];

export const DEMO_CASE_DETAIL: RecoveryCaseDetail = {
  id: 'demo-case-001',
  merchantId: 'demo-merchant-evaluator',
  status: 'IN_PROGRESS',
  priority: 'HIGH',
  failureReasonCategory: 'AUTHENTICATION',
  estimatedRecoverableAmount: 4999.0,
  recoveredAmount: 0,
  currency: 'INR',
  expiresAt: '2026-09-02T12:00:00Z',
  recoveredAt: null,
  closedAt: null,
  createdAt: '2026-08-30T10:00:00Z',
  updatedAt: '2026-08-30T10:30:00Z',
  customer: {
    id: 'demo_cust_101',
    razorpayCustomerId: 'cust_sim_8819',
    name: 'Aarav Sharma (Simulated)',
    email: 'aarav.sharma@example.com',
    phone: '+919876543210',
    createdAt: '2026-08-30T10:00:00Z',
  },
  payment: {
    id: 'demo_pay_8912',
    razorpayPaymentId: 'pay_sim_8912',
    razorpayOrderId: 'order_sim_9912',
    razorpayInvoiceId: null,
    amount: 4999.0,
    currency: 'INR',
    status: 'FAILED',
    method: 'UPI',
    errorCode: 'BAD_REQUEST_ERROR',
    errorDescription: 'Payment failed due to 3D Secure / MPIN authentication timeout',
    errorSource: 'customer',
    errorReason: 'payment_authentication_failed',
    riskLevel: 'LOW',
    paymentCreatedAt: '2026-08-30T10:00:00Z',
    createdAt: '2026-08-30T10:00:00Z',
  },
  attempts: [
    {
      id: 'demo-attempt-001',
      recoveryCaseId: 'demo-case-001',
      merchantId: 'demo-merchant-evaluator',
      attemptNumber: 1,
      channel: 'WHATSAPP',
      status: 'DELIVERED',
      scheduledAt: '2026-08-30T10:05:00Z',
      executedAt: '2026-08-30T10:05:05Z',
      completedAt: '2026-08-30T10:05:10Z',
      resultCode: 'DELIVERED',
      resultMessage: 'Smart Payment Link delivered via WhatsApp Business API',
      recoveryLink: 'https://rzp.io/i/demo_link_8912',
      strategyId: 'strat-sim-101',
      strategySnapshot: {
        strategyId: 'strat-sim-101',
        channel: 'WHATSAPP',
        recommendedAction: 'Send 1-click UPI recovery link via WhatsApp',
        confidenceScore: 0.92,
        priority: 'HIGH',
        fallbackChannel: 'EMAIL',
        fallbackAction: 'Send email reminder after 2 hours',
        reason: 'Customer has active WhatsApp engagement and UPI failure was transient',
      },
      strategyPriority: 'HIGH',
      confidenceScore: 0.92,
      recommendedAction: 'Send 1-click UPI recovery link via WhatsApp',
      createdAt: '2026-08-30T10:05:00Z',
      updatedAt: '2026-08-30T10:05:10Z',
    },
  ],
  latestDiagnosis: {
    id: 'demo-diag-001',
    recoveryCaseId: 'demo-case-001',
    merchantId: 'demo-merchant-evaluator',
    recommendedAction: 'Dispatch WhatsApp smart payment link with 15-minute validity window',
    channel: 'WHATSAPP',
    confidenceScore: 0.92,
    reasoning:
      'Payment failed due to 3D Secure / MPIN authentication timeout on UPI app. Customer has valid phone number and high intent score (85%). WhatsApp smart link offers highest conversion probability with low friction.',
    modelName: 'gemini-3.7-flash',
    modelVersion: 'v1-demo',
    promptTokens: 340,
    completionTokens: 92,
    decisionFactors: '{"transientFailure":true,"intentScore":0.85,"channelPreference":"WHATSAPP"}',
    createdAt: '2026-08-30T10:02:00Z',
  },
};

export const DEMO_ANALYTICS_OVERVIEW: AnalyticsOverview = {
  totalCases: 38,
  openCases: 7,
  inProgressCases: 4,
  recoveredCases: 24,
  failedCases: 0,
  expiredCases: 2,
  cancelledCases: 1,
  expiredOrCancelledCases: 3,
  totalEstimatedRecoverableAmount: 184500.0,
  totalRecoveredAmount: 142000.0,
  recoveryRate: 75.0,
  averageRecoveredAmount: 5916.67,
  averageTimeToRecoverySeconds: 1420,
  from: '2026-08-01',
  to: '2026-08-30',
};

export const DEMO_RECOVERY_TRENDS: RecoveryTrends = {
  from: '2026-08-24',
  to: '2026-08-30',
  totalCases: 38,
  totalAmountAtRisk: 184500.0,
  totalRecoveredAmount: 142000.0,
  overallRecoveryRate: 75.0,
  trends: [
    {
      date: '2026-08-24',
      recoveryCasesCreated: 5,
      amountAtRisk: 22000.0,
      amountRecovered: 18000.0,
      recoveredCaseCount: 4,
      recoveryRate: 80.0,
    },
    {
      date: '2026-08-25',
      recoveryCasesCreated: 6,
      amountAtRisk: 31000.0,
      amountRecovered: 24000.0,
      recoveredCaseCount: 4,
      recoveryRate: 66.7,
    },
    {
      date: '2026-08-26',
      recoveryCasesCreated: 4,
      amountAtRisk: 19500.0,
      amountRecovered: 15500.0,
      recoveredCaseCount: 3,
      recoveryRate: 75.0,
    },
    {
      date: '2026-08-27',
      recoveryCasesCreated: 7,
      amountAtRisk: 38000.0,
      amountRecovered: 29000.0,
      recoveredCaseCount: 5,
      recoveryRate: 71.4,
    },
    {
      date: '2026-08-28',
      recoveryCasesCreated: 5,
      amountAtRisk: 26000.0,
      amountRecovered: 21000.0,
      recoveredCaseCount: 4,
      recoveryRate: 80.0,
    },
    {
      date: '2026-08-29',
      recoveryCasesCreated: 6,
      amountAtRisk: 28000.0,
      amountRecovered: 20000.0,
      recoveredCaseCount: 4,
      recoveryRate: 66.7,
    },
    {
      date: '2026-08-30',
      recoveryCasesCreated: 5,
      amountAtRisk: 20000.0,
      amountRecovered: 14500.0,
      recoveredCaseCount: 0,
      recoveryRate: 72.5,
    },
  ],
};

export const DEMO_CHANNEL_ANALYTICS: ChannelAnalytics = {
  from: '2026-08-01',
  to: '2026-08-30',
  totalAttempts: 45,
  channels: [
    {
      channel: 'WHATSAPP',
      totalAttempts: 24,
      successfulAttempts: 18,
      failedAttempts: 2,
      sentAttempts: 24,
      deliveredAttempts: 23,
      clickedAttempts: 20,
      successRate: 75.0,
      recoveredAmount: 98000.0,
    },
    {
      channel: 'EMAIL',
      totalAttempts: 12,
      successfulAttempts: 5,
      failedAttempts: 1,
      sentAttempts: 12,
      deliveredAttempts: 12,
      clickedAttempts: 7,
      successRate: 41.7,
      recoveredAmount: 32000.0,
    },
    {
      channel: 'SMS',
      totalAttempts: 9,
      successfulAttempts: 3,
      failedAttempts: 1,
      sentAttempts: 9,
      deliveredAttempts: 8,
      clickedAttempts: 4,
      successRate: 33.3,
      recoveredAmount: 12000.0,
    },
  ],
};

export const DEMO_FAILURE_ANALYTICS: FailureAnalytics = {
  from: '2026-08-01',
  to: '2026-08-30',
  totalCases: 38,
  categories: [
    {
      failureReasonCategory: 'AUTHENTICATION',
      caseCount: 16,
      estimatedRecoverableAmount: 78000.0,
      recoveredAmount: 62000.0,
      recoveredCaseCount: 13,
      recoveryRate: 79.5,
    },
    {
      failureReasonCategory: 'INSUFFICIENT_FUNDS',
      caseCount: 11,
      estimatedRecoverableAmount: 54000.0,
      recoveredAmount: 41000.0,
      recoveredCaseCount: 8,
      recoveryRate: 75.9,
    },
    {
      failureReasonCategory: 'NETWORK_TIMEOUT',
      caseCount: 7,
      estimatedRecoverableAmount: 34000.0,
      recoveredAmount: 25000.0,
      recoveredCaseCount: 5,
      recoveryRate: 73.5,
    },
    {
      failureReasonCategory: 'USER_DROPOFF',
      caseCount: 4,
      estimatedRecoverableAmount: 18500.0,
      recoveredAmount: 14000.0,
      recoveredCaseCount: 3,
      recoveryRate: 75.7,
    },
  ],
  priorities: [
    {
      priority: 'CRITICAL',
      caseCount: 8,
      estimatedRecoverableAmount: 64000.0,
      recoveredAmount: 52000.0,
      recoveredCaseCount: 6,
      recoveryRate: 81.3,
    },
    {
      priority: 'HIGH',
      caseCount: 14,
      estimatedRecoverableAmount: 68000.0,
      recoveredAmount: 53000.0,
      recoveredCaseCount: 11,
      recoveryRate: 77.9,
    },
    {
      priority: 'MEDIUM',
      caseCount: 10,
      estimatedRecoverableAmount: 35000.0,
      recoveredAmount: 26000.0,
      recoveredCaseCount: 8,
      recoveryRate: 74.3,
    },
    {
      priority: 'LOW',
      caseCount: 6,
      estimatedRecoverableAmount: 17500.0,
      recoveredAmount: 11000.0,
      recoveredCaseCount: 4,
      recoveryRate: 62.9,
    },
  ],
};

export const DEMO_NOTIFICATIONS: NotificationResponseDto[] = [
  {
    id: 'demo-notif-001',
    merchantId: 'demo-merchant-evaluator',
    eventType: 'PAYMENT_RECOVERED',
    title: 'Simulated Payment Recovered',
    message: 'A payment of ₹12,500 for Priya Patel was successfully recovered via WhatsApp Smart Link.',
    status: 'UNREAD',
    read: false,
    recoveryCaseId: 'demo-case-002',
    deliveries: [
      {
        id: 'demo-del-001',
        channel: 'IN_APP',
        provider: 'InAppChannelProvider',
        status: 'DELIVERED',
        attemptedAt: '2026-08-30T11:45:00Z',
        deliveredAt: '2026-08-30T11:45:00Z',
        errorCode: null,
        errorMessage: null,
        retryCount: 0,
      },
    ],
    createdAt: '2026-08-30T11:45:00Z',
    updatedAt: '2026-08-30T11:45:00Z',
  },
  {
    id: 'demo-notif-002',
    merchantId: 'demo-merchant-evaluator',
    eventType: 'HIGH_PRIORITY_FAILURE',
    title: 'High-Priority Failure Diagnosed',
    message: 'High-priority recovery case opened for Aarav Sharma (₹4,999). Gemini 3.7 Flash analysis active.',
    status: 'UNREAD',
    read: false,
    recoveryCaseId: 'demo-case-001',
    deliveries: [
      {
        id: 'demo-del-002',
        channel: 'IN_APP',
        provider: 'InAppChannelProvider',
        status: 'DELIVERED',
        attemptedAt: '2026-08-30T10:00:00Z',
        deliveredAt: '2026-08-30T10:00:00Z',
        errorCode: null,
        errorMessage: null,
        retryCount: 0,
      },
    ],
    createdAt: '2026-08-30T10:00:00Z',
    updatedAt: '2026-08-30T10:00:00Z',
  },
];

export const DEMO_PROVIDER_HEALTH: ProviderHealthSummary = {
  overallStatus: 'HEALTHY',
  lastChecked: '2026-08-30T22:00:00Z',
  providers: [
    {
      id: 'prov-whatsapp',
      name: 'WhatsApp Cloud API',
      channel: 'WHATSAPP',
      status: 'HEALTHY',
      message: 'Operating normally with 99.4% delivery rate',
      lastChecked: '2026-08-30T22:00:00Z',
    },
    {
      id: 'prov-email',
      name: 'AWS SES',
      channel: 'EMAIL',
      status: 'HEALTHY',
      message: 'Operating normally, low bounce rate',
      lastChecked: '2026-08-30T22:00:00Z',
    },
    {
      id: 'prov-sms',
      name: 'Gupshup SMS Gateway',
      channel: 'SMS',
      status: 'HEALTHY',
      message: 'DLT headers verified and active',
      lastChecked: '2026-08-30T22:00:00Z',
    },
    {
      id: 'prov-gateway',
      name: 'Razorpay Orders API',
      channel: 'PAYMENT_GATEWAY',
      status: 'HEALTHY',
      message: 'Webhook subscription verified',
      lastChecked: '2026-08-30T22:00:00Z',
    },
  ],
};

export const DEMO_NOTIFICATION_PREFERENCES: NotificationPreferenceResponseDto = {
  merchantId: 'demo-merchant-evaluator',
  webhookUrl: 'https://demo.example.com/webhooks/recoverai',
  preferences: {
    PAYMENT_RECOVERED: { EMAIL: true, WEBHOOK: true, IN_APP: true },
    CASE_EXHAUSTED: { EMAIL: true, WEBHOOK: false, IN_APP: true },
    HIGH_PRIORITY_FAILURE: { EMAIL: true, WEBHOOK: true, IN_APP: true },
    PROVIDER_DEGRADED: { EMAIL: false, WEBHOOK: false, IN_APP: true },
  },
};

// Async Demo Service API Implementations (100% In-Memory, Zero Network Calls)

export async function getDemoDashboard(): Promise<DashboardSummary> {
  return DEMO_DASHBOARD_SUMMARY;
}

export async function getDemoRecoveryCases(
  params?: RecoveryCaseListParams
): Promise<PageResponse<RecoveryCase>> {
  let filtered = [...DEMO_CASES];
  if (params?.status) {
    filtered = filtered.filter((c) => c.status === params.status);
  }
  if (params?.priority) {
    filtered = filtered.filter((c) => c.priority === params.priority);
  }
  if (params?.failureReasonCategory) {
    const q = params.failureReasonCategory.toLowerCase();
    filtered = filtered.filter((c) => c.failureReasonCategory?.toLowerCase().includes(q));
  }

  return {
    content: filtered,
    totalElements: filtered.length,
    totalPages: 1,
    number: 0,
    size: 20,
    first: true,
    last: true,
    empty: filtered.length === 0,
  };
}

export async function getDemoRecoveryCase(id: string): Promise<RecoveryCaseDetail> {
  const found = DEMO_CASES.find((c) => c.id === id);
  if (found) {
    return {
      ...DEMO_CASE_DETAIL,
      id: found.id,
      status: found.status,
      priority: found.priority,
      failureReasonCategory: found.failureReasonCategory,
      estimatedRecoverableAmount: found.estimatedRecoverableAmount,
      recoveredAmount: found.recoveredAmount,
      createdAt: found.createdAt,
      updatedAt: found.updatedAt,
      customer: {
        ...DEMO_CASE_DETAIL.customer!,
        name: found.customerName,
        email: found.customerEmail,
      },
    };
  }
  return DEMO_CASE_DETAIL;
}

export const DEMO_ATTEMPT_ANALYTICS: AttemptAnalytics = {
  from: '2026-08-01',
  to: '2026-08-30',
  totalAttempts: 45,
  successfulAttempts: 26,
  failedAttempts: 4,
  scheduledAttempts: 10,
  inFlightAttempts: 5,
  sentAttempts: 45,
  deliveredAttempts: 43,
  clickedAttempts: 31,
  skippedAttempts: 0,
  successRate: 72.2,
  averageAttemptsPerRecoveryCase: 1.18,
  attemptsByStatus: {
    SCHEDULED: 10,
    IN_FLIGHT: 5,
    SENT: 4,
    DELIVERED: 0,
    CLICKED: 0,
    SUCCESS: 26,
    FAILED: 4,
    SKIPPED: 0,
  },
  attemptsByChannel: {
    WHATSAPP: 24,
    EMAIL: 12,
    SMS: 9,
    RETRY_CHARGE: 0,
    SMART_LINK: 0,
    MANUAL: 0,
  },
};

export async function getDemoAnalyticsOverview(params?: DateRangeParams): Promise<AnalyticsOverview> {
  return {
    ...DEMO_ANALYTICS_OVERVIEW,
    from: params?.from || DEMO_ANALYTICS_OVERVIEW.from,
    to: params?.to || DEMO_ANALYTICS_OVERVIEW.to,
  };
}

export async function getDemoRecoveryTrends(params?: DateRangeParams): Promise<RecoveryTrends> {
  let filtered = [...DEMO_RECOVERY_TRENDS.trends];
  if (params?.from) {
    filtered = filtered.filter((t) => t.date >= params.from!);
  }
  if (params?.to) {
    filtered = filtered.filter((t) => t.date <= params.to!);
  }
  if (filtered.length === 0) {
    filtered = DEMO_RECOVERY_TRENDS.trends;
  }
  const totalAmountAtRisk = filtered.reduce((acc, t) => acc + t.amountAtRisk, 0);
  const totalRecoveredAmount = filtered.reduce((acc, t) => acc + t.amountRecovered, 0);
  const totalCases = filtered.reduce((acc, t) => acc + t.recoveryCasesCreated, 0);
  const overallRecoveryRate = totalAmountAtRisk > 0 ? (totalRecoveredAmount / totalAmountAtRisk) * 100 : 75.0;

  return {
    from: params?.from || DEMO_RECOVERY_TRENDS.from,
    to: params?.to || DEMO_RECOVERY_TRENDS.to,
    totalCases,
    totalAmountAtRisk,
    totalRecoveredAmount,
    overallRecoveryRate: Number(overallRecoveryRate.toFixed(1)),
    trends: filtered,
  };
}

export async function getDemoChannelAnalytics(params?: DateRangeParams): Promise<ChannelAnalytics> {
  return {
    ...DEMO_CHANNEL_ANALYTICS,
    from: params?.from || DEMO_CHANNEL_ANALYTICS.from,
    to: params?.to || DEMO_CHANNEL_ANALYTICS.to,
  };
}

export async function getDemoFailureAnalytics(params?: DateRangeParams): Promise<FailureAnalytics> {
  return {
    ...DEMO_FAILURE_ANALYTICS,
    from: params?.from || DEMO_FAILURE_ANALYTICS.from,
    to: params?.to || DEMO_FAILURE_ANALYTICS.to,
  };
}

export async function getDemoAttemptAnalytics(params?: DateRangeParams): Promise<AttemptAnalytics> {
  return {
    ...DEMO_ATTEMPT_ANALYTICS,
    from: params?.from || DEMO_ATTEMPT_ANALYTICS.from,
    to: params?.to || DEMO_ATTEMPT_ANALYTICS.to,
  };
}

export async function getDemoNotifications(): Promise<PageResponse<NotificationResponseDto>> {
  return {
    content: DEMO_NOTIFICATIONS,
    totalElements: DEMO_NOTIFICATIONS.length,
    totalPages: 1,
    number: 0,
    size: 10,
    first: true,
    last: true,
    empty: DEMO_NOTIFICATIONS.length === 0,
  };
}

export async function getDemoUnreadCount(): Promise<number> {
  return DEMO_NOTIFICATIONS.filter((n) => !n.read).length;
}

export async function getDemoProviderHealth(): Promise<ProviderHealthSummary> {
  return DEMO_PROVIDER_HEALTH;
}

export async function getDemoNotificationPreferences(): Promise<NotificationPreferenceResponseDto> {
  return DEMO_NOTIFICATION_PREFERENCES;
}
