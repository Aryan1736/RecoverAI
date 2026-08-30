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
  DailyRecoveryTrend,
  ChannelMetric,
  FailureCategoryMetric,
  FailurePriorityMetric,
  RecoveryChannel,
  RecoveryPriority,
  RecoveryAttemptStatus,
} from '../types/analytics';
import type {
  NotificationResponseDto,
  NotificationPreferenceResponseDto,
  MerchantNotificationEvent,
} from '../types/notifications';
import type { ProviderHealthSummary } from '../types/providers';

export const DEMO_STORAGE_STORE_KEY = 'recoverai_demo_store_v1';
export const DEMO_STATE_EVENT = 'recoverai:demo-state-changed';

// ============================================================================
// Realistic 10-Case Initial Demo Dataset
// ============================================================================

export const INITIAL_DEMO_CASES: RecoveryCaseDetail[] = [
  // 1. High-value UPI Authentication Timeout (In Progress -> Ready for Simulation)
  {
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
      modelName: 'Google Gemini 3.7 Flash',
      modelVersion: 'v1-demo',
      promptTokens: 340,
      completionTokens: 92,
      decisionFactors: '{"transientFailure":true,"intentScore":0.85,"channelPreference":"WHATSAPP"}',
      createdAt: '2026-08-30T10:02:00Z',
    },
  },

  // 2. High-Value Insufficient Funds (Historically Recovered)
  {
    id: 'demo-case-002',
    merchantId: 'demo-merchant-evaluator',
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
    customer: {
      id: 'demo_cust_102',
      razorpayCustomerId: 'cust_sim_8820',
      name: 'Priya Patel (Simulated)',
      email: 'priya.patel@example.com',
      phone: '+919876543211',
      createdAt: '2026-08-30T09:15:00Z',
    },
    payment: {
      id: 'demo_pay_8913',
      razorpayPaymentId: 'pay_sim_8913',
      razorpayOrderId: 'order_sim_9913',
      razorpayInvoiceId: null,
      amount: 12500.0,
      currency: 'INR',
      status: 'CAPTURED',
      method: 'NETBANKING',
      errorCode: null,
      errorDescription: null,
      errorSource: null,
      errorReason: null,
      riskLevel: 'LOW',
      paymentCreatedAt: '2026-08-30T09:15:00Z',
      createdAt: '2026-08-30T09:15:00Z',
    },
    attempts: [
      {
        id: 'demo-attempt-002',
        recoveryCaseId: 'demo-case-002',
        merchantId: 'demo-merchant-evaluator',
        attemptNumber: 1,
        channel: 'WHATSAPP',
        status: 'SUCCESS',
        scheduledAt: '2026-08-30T10:00:00Z',
        executedAt: '2026-08-30T10:00:05Z',
        completedAt: '2026-08-30T11:45:00Z',
        resultCode: 'PAYMENT_CAPTURED',
        resultMessage: 'Customer authorized full payment via Netbanking link',
        recoveryLink: 'https://rzp.io/i/demo_link_8913',
        strategyId: 'strat-sim-102',
        strategySnapshot: {
          strategyId: 'strat-sim-102',
          channel: 'WHATSAPP',
          recommendedAction: 'Deliver WhatsApp payment link with delayed reminder option',
          confidenceScore: 0.88,
          priority: 'CRITICAL',
          fallbackChannel: 'EMAIL',
          fallbackAction: 'Send automated email reminder',
          reason: 'High cart value customer with successful historical payments',
        },
        strategyPriority: 'CRITICAL',
        confidenceScore: 0.88,
        recommendedAction: 'Deliver WhatsApp payment link with delayed reminder option',
        createdAt: '2026-08-30T10:00:00Z',
        updatedAt: '2026-08-30T11:45:00Z',
      },
    ],
    latestDiagnosis: {
      id: 'demo-diag-002',
      recoveryCaseId: 'demo-case-002',
      merchantId: 'demo-merchant-evaluator',
      recommendedAction: 'Deliver WhatsApp payment link with delayed reminder option',
      channel: 'WHATSAPP',
      confidenceScore: 0.88,
      reasoning:
        'Account balance failure on high-value transaction. Customer has consistent order history. 45-minute deferred reminder conversion probability is 78%.',
      modelName: 'Google Gemini 3.7 Flash',
      modelVersion: 'v1-demo',
      promptTokens: 310,
      completionTokens: 85,
      decisionFactors: '{"highValue":true,"historicalSuccess":true}',
      createdAt: '2026-08-30T09:20:00Z',
    },
  },

  // 3. Network Gateway Timeout (Open -> Ready for Simulation)
  {
    id: 'demo-case-003',
    merchantId: 'demo-merchant-evaluator',
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
    customer: {
      id: 'demo_cust_103',
      razorpayCustomerId: 'cust_sim_8821',
      name: 'Rohan Gupta (Simulated)',
      email: 'rohan.gupta@example.com',
      phone: '+919876543212',
      createdAt: '2026-08-30T14:20:00Z',
    },
    payment: {
      id: 'demo_pay_8914',
      razorpayPaymentId: 'pay_sim_8914',
      razorpayOrderId: 'order_sim_9914',
      razorpayInvoiceId: null,
      amount: 1999.0,
      currency: 'INR',
      status: 'FAILED',
      method: 'UPI',
      errorCode: 'GATEWAY_ERROR',
      errorDescription: 'Bank switch network timeout during routing',
      errorSource: 'bank',
      errorReason: 'gateway_timeout',
      riskLevel: 'LOW',
      paymentCreatedAt: '2026-08-30T14:20:00Z',
      createdAt: '2026-08-30T14:20:00Z',
    },
    attempts: [],
    latestDiagnosis: {
      id: 'demo-diag-003',
      recoveryCaseId: 'demo-case-003',
      merchantId: 'demo-merchant-evaluator',
      recommendedAction: 'Automated instant retry via secondary banking gateway switch',
      channel: 'SMART_LINK',
      confidenceScore: 0.94,
      reasoning:
        'Temporary NPCI / UPI server timeout during peak traffic window. Zero customer fraud indicators. Instant recovery link has 91% probability.',
      modelName: 'Google Gemini 3.7 Flash',
      modelVersion: 'v1-demo',
      promptTokens: 290,
      completionTokens: 78,
      decisionFactors: '{"networkGlitch":true,"instantRetryRecommended":true}',
      createdAt: '2026-08-30T14:22:00Z',
    },
  },

  // 4. Low-Value Dropoff (Historically Recovered)
  {
    id: 'demo-case-004',
    merchantId: 'demo-merchant-evaluator',
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
    customer: {
      id: 'demo_cust_104',
      razorpayCustomerId: 'cust_sim_8822',
      name: 'Ananya Verma (Simulated)',
      email: 'ananya.verma@example.com',
      phone: '+919876543213',
      createdAt: '2026-08-30T08:00:00Z',
    },
    payment: {
      id: 'demo_pay_8915',
      razorpayPaymentId: 'pay_sim_8915',
      razorpayOrderId: 'order_sim_9915',
      razorpayInvoiceId: null,
      amount: 899.0,
      currency: 'INR',
      status: 'CAPTURED',
      method: 'CARD',
      errorCode: null,
      errorDescription: null,
      errorSource: null,
      errorReason: null,
      riskLevel: 'LOW',
      paymentCreatedAt: '2026-08-30T08:00:00Z',
      createdAt: '2026-08-30T08:00:00Z',
    },
    attempts: [
      {
        id: 'demo-attempt-004',
        recoveryCaseId: 'demo-case-004',
        merchantId: 'demo-merchant-evaluator',
        attemptNumber: 1,
        channel: 'SMS',
        status: 'SUCCESS',
        scheduledAt: '2026-08-30T08:30:00Z',
        executedAt: '2026-08-30T08:30:05Z',
        completedAt: '2026-08-30T10:10:00Z',
        resultCode: 'PAYMENT_CAPTURED',
        resultMessage: 'Customer completed checkout via SMS link',
        recoveryLink: 'https://rzp.io/i/demo_link_8915',
        strategyId: 'strat-sim-104',
        strategySnapshot: {
          strategyId: 'strat-sim-104',
          channel: 'SMS',
          recommendedAction: 'Dispatch SMS with 1-click cart checkout link',
          confidenceScore: 0.79,
          priority: 'LOW',
          fallbackChannel: 'EMAIL',
          fallbackAction: 'Email follow-up',
          reason: 'Lightweight order dropoff',
        },
        strategyPriority: 'LOW',
        confidenceScore: 0.79,
        recommendedAction: 'Dispatch SMS with 1-click cart checkout link',
        createdAt: '2026-08-30T08:30:00Z',
        updatedAt: '2026-08-30T10:10:00Z',
      },
    ],
    latestDiagnosis: {
      id: 'demo-diag-004',
      recoveryCaseId: 'demo-case-004',
      merchantId: 'demo-merchant-evaluator',
      recommendedAction: 'Dispatch SMS with 1-click cart checkout link',
      channel: 'SMS',
      confidenceScore: 0.79,
      reasoning: 'Customer abandoned payment modal at CVV screen. SMS dispatched with pre-populated order.',
      modelName: 'Google Gemini 3.7 Flash',
      modelVersion: 'v1-demo',
      promptTokens: 250,
      completionTokens: 60,
      decisionFactors: '{"dropoffScreen":"CVV"}',
      createdAt: '2026-08-30T08:05:00Z',
    },
  },

  // 5. Card Bank Declined (In Progress -> Ready for Simulation)
  {
    id: 'demo-case-005',
    merchantId: 'demo-merchant-evaluator',
    status: 'IN_PROGRESS',
    priority: 'HIGH',
    failureReasonCategory: 'BANK_DECLINED',
    estimatedRecoverableAmount: 8450.0,
    recoveredAmount: 0,
    currency: 'INR',
    expiresAt: '2026-09-02T16:00:00Z',
    recoveredAt: null,
    closedAt: null,
    createdAt: '2026-08-30T12:30:00Z',
    updatedAt: '2026-08-30T12:35:00Z',
    customer: {
      id: 'demo_cust_105',
      razorpayCustomerId: 'cust_sim_8823',
      name: 'Vikramaditya Singh (Simulated)',
      email: 'vikramaditya.singh@example.com',
      phone: '+919876543214',
      createdAt: '2026-08-30T12:30:00Z',
    },
    payment: {
      id: 'demo_pay_8916',
      razorpayPaymentId: 'pay_sim_8916',
      razorpayOrderId: 'order_sim_9916',
      razorpayInvoiceId: null,
      amount: 8450.0,
      currency: 'INR',
      status: 'FAILED',
      method: 'CARD',
      errorCode: 'PAYMENT_DECLINED_BY_BANK',
      errorDescription: 'Issuing bank declined international transaction limit',
      errorSource: 'bank',
      errorReason: 'bank_limit_exceeded',
      riskLevel: 'LOW',
      paymentCreatedAt: '2026-08-30T12:30:00Z',
      createdAt: '2026-08-30T12:30:00Z',
    },
    attempts: [
      {
        id: 'demo-attempt-005',
        recoveryCaseId: 'demo-case-005',
        merchantId: 'demo-merchant-evaluator',
        attemptNumber: 1,
        channel: 'WHATSAPP',
        status: 'DELIVERED',
        scheduledAt: '2026-08-30T12:35:00Z',
        executedAt: '2026-08-30T12:35:05Z',
        completedAt: '2026-08-30T12:35:10Z',
        resultCode: 'DELIVERED',
        resultMessage: 'WhatsApp payment notification delivered with alternative payment methods',
        recoveryLink: 'https://rzp.io/i/demo_link_8916',
        strategyId: 'strat-sim-105',
        strategySnapshot: {
          strategyId: 'strat-sim-105',
          channel: 'WHATSAPP',
          recommendedAction: 'Dispatch WhatsApp message offering UPI or netbanking alternative',
          confidenceScore: 0.86,
          priority: 'HIGH',
          fallbackChannel: 'SMART_LINK',
          fallbackAction: 'Smart Link retry',
          reason: 'Customer bank card limit declined; UPI offers immediate alternative',
        },
        strategyPriority: 'HIGH',
        confidenceScore: 0.86,
        recommendedAction: 'Dispatch WhatsApp message offering UPI or netbanking alternative',
        createdAt: '2026-08-30T12:35:00Z',
        updatedAt: '2026-08-30T12:35:10Z',
      },
    ],
    latestDiagnosis: {
      id: 'demo-diag-005',
      recoveryCaseId: 'demo-case-005',
      merchantId: 'demo-merchant-evaluator',
      recommendedAction: 'Dispatch WhatsApp message suggesting domestic debit card or UPI alternative',
      channel: 'WHATSAPP',
      confidenceScore: 0.86,
      reasoning:
        'Card issuer declined transaction due to daily e-commerce quota. Presenting UPI or alternate card payment recovery link.',
      modelName: 'Google Gemini 3.7 Flash',
      modelVersion: 'v1-demo',
      promptTokens: 320,
      completionTokens: 82,
      decisionFactors: '{"reason":"limit_exceeded","alternateMethod":"UPI"}',
      createdAt: '2026-08-30T12:32:00Z',
    },
  },

  // 6. Terminal Case: Exhausted Attempts (Failed)
  {
    id: 'demo-case-006',
    merchantId: 'demo-merchant-evaluator',
    status: 'FAILED',
    priority: 'MEDIUM',
    failureReasonCategory: 'CARD_EXPIRED',
    estimatedRecoverableAmount: 3200.0,
    recoveredAmount: 0,
    currency: 'INR',
    expiresAt: '2026-08-30T16:00:00Z',
    recoveredAt: null,
    closedAt: '2026-08-30T16:00:00Z',
    createdAt: '2026-08-29T10:00:00Z',
    updatedAt: '2026-08-30T16:00:00Z',
    customer: {
      id: 'demo_cust_106',
      razorpayCustomerId: 'cust_sim_8824',
      name: 'Sunita Reddy (Simulated)',
      email: 'sunita.reddy@example.com',
      phone: '+919876543215',
      createdAt: '2026-08-29T10:00:00Z',
    },
    payment: {
      id: 'demo_pay_8917',
      razorpayPaymentId: 'pay_sim_8917',
      razorpayOrderId: 'order_sim_9917',
      razorpayInvoiceId: null,
      amount: 3200.0,
      currency: 'INR',
      status: 'FAILED',
      method: 'CARD',
      errorCode: 'EXPIRED_CARD',
      errorDescription: 'Card expiry date precedes transaction date',
      errorSource: 'customer',
      errorReason: 'card_expired',
      riskLevel: 'LOW',
      paymentCreatedAt: '2026-08-29T10:00:00Z',
      createdAt: '2026-08-29T10:00:00Z',
    },
    attempts: [
      {
        id: 'demo-attempt-006-1',
        recoveryCaseId: 'demo-case-006',
        merchantId: 'demo-merchant-evaluator',
        attemptNumber: 1,
        channel: 'EMAIL',
        status: 'FAILED',
        scheduledAt: '2026-08-29T10:30:00Z',
        executedAt: '2026-08-29T10:30:05Z',
        completedAt: '2026-08-29T22:30:00Z',
        resultCode: 'NO_CUSTOMER_ACTION',
        resultMessage: 'Email delivered but customer did not update card details within 12 hours',
        recoveryLink: 'https://rzp.io/i/demo_link_8917',
        strategyId: 'strat-sim-106',
        strategySnapshot: {
          strategyId: 'strat-sim-106',
          channel: 'EMAIL',
          recommendedAction: 'Dispatch card update form via Email with SMS fallback',
          confidenceScore: 0.91,
          priority: 'MEDIUM',
          fallbackChannel: 'SMS',
          fallbackAction: 'SMS alert after 12h',
          reason: 'Expired recurring payment instrument',
        },
        strategyPriority: 'MEDIUM',
        confidenceScore: 0.91,
        recommendedAction: 'Dispatch card update form via Email with SMS fallback',
        createdAt: '2026-08-29T10:30:00Z',
        updatedAt: '2026-08-29T22:30:00Z',
      },
      {
        id: 'demo-attempt-006-2',
        recoveryCaseId: 'demo-case-006',
        merchantId: 'demo-merchant-evaluator',
        attemptNumber: 2,
        channel: 'SMS',
        status: 'FAILED',
        scheduledAt: '2026-08-30T09:00:00Z',
        executedAt: '2026-08-30T09:00:05Z',
        completedAt: '2026-08-30T16:00:00Z',
        resultCode: 'EXHAUSTED',
        resultMessage: 'Customer declined to re-enter card details; recovery policy retry budget reached',
        recoveryLink: 'https://rzp.io/i/demo_link_8917_sms',
        strategyId: 'strat-sim-106',
        strategySnapshot: null,
        strategyPriority: 'MEDIUM',
        confidenceScore: 0.91,
        recommendedAction: 'Dispatch card update form via Email with SMS fallback',
        createdAt: '2026-08-30T09:00:00Z',
        updatedAt: '2026-08-30T16:00:00Z',
      },
    ],
    latestDiagnosis: {
      id: 'demo-diag-006',
      recoveryCaseId: 'demo-case-006',
      merchantId: 'demo-merchant-evaluator',
      recommendedAction: 'Dispatch card update form via Email with SMS fallback',
      channel: 'EMAIL',
      confidenceScore: 0.91,
      reasoning: 'Card is permanently expired. Customer must enter a new card or mandate.',
      modelName: 'Google Gemini 3.7 Flash',
      modelVersion: 'v1-demo',
      promptTokens: 280,
      completionTokens: 75,
      decisionFactors: '{"permanentFailure":true,"mandateUpdateRequired":true}',
      createdAt: '2026-08-29T10:05:00Z',
    },
  },

  // 7. Terminal Case: Cancelled by Merchant Operator
  {
    id: 'demo-case-007',
    merchantId: 'demo-merchant-evaluator',
    status: 'CANCELLED',
    priority: 'CRITICAL',
    failureReasonCategory: 'AUTHENTICATION',
    estimatedRecoverableAmount: 15000.0,
    recoveredAmount: 0,
    currency: 'INR',
    expiresAt: '2026-09-01T12:00:00Z',
    recoveredAt: null,
    closedAt: '2026-08-30T12:00:00Z',
    createdAt: '2026-08-30T11:00:00Z',
    updatedAt: '2026-08-30T12:00:00Z',
    customer: {
      id: 'demo_cust_107',
      razorpayCustomerId: 'cust_sim_8825',
      name: 'Kabir Malhotra (Simulated)',
      email: 'kabir.malhotra@example.com',
      phone: '+919876543216',
      createdAt: '2026-08-30T11:00:00Z',
    },
    payment: {
      id: 'demo_pay_8918',
      razorpayPaymentId: 'pay_sim_8918',
      razorpayOrderId: 'order_sim_9918',
      razorpayInvoiceId: null,
      amount: 15000.0,
      currency: 'INR',
      status: 'FAILED',
      method: 'UPI',
      errorCode: 'TRANSACTION_CANCELLED',
      errorDescription: 'Customer flagged corporate duplicate invoice',
      errorSource: 'customer',
      errorReason: 'duplicate_order',
      riskLevel: 'LOW',
      paymentCreatedAt: '2026-08-30T11:00:00Z',
      createdAt: '2026-08-30T11:00:00Z',
    },
    attempts: [
      {
        id: 'demo-attempt-007',
        recoveryCaseId: 'demo-case-007',
        merchantId: 'demo-merchant-evaluator',
        attemptNumber: 1,
        channel: 'MANUAL',
        status: 'SKIPPED',
        scheduledAt: '2026-08-30T11:30:00Z',
        executedAt: null,
        completedAt: '2026-08-30T12:00:00Z',
        resultCode: 'MERCHANT_CANCELLED',
        resultMessage: 'Case cancelled by merchant operator before automated outreach',
        recoveryLink: null,
        strategyId: 'strat-sim-107',
        strategySnapshot: {
          strategyId: 'strat-sim-107',
          channel: 'MANUAL',
          recommendedAction: 'Flag for merchant operations review before initiating automated outreach',
          confidenceScore: 0.95,
          priority: 'CRITICAL',
          fallbackChannel: null,
          fallbackAction: null,
          reason: 'High value order with repeated manual cancellations',
        },
        strategyPriority: 'CRITICAL',
        confidenceScore: 0.95,
        recommendedAction: 'Flag for merchant operations review before initiating automated outreach',
        createdAt: '2026-08-30T11:30:00Z',
        updatedAt: '2026-08-30T12:00:00Z',
      },
    ],
    latestDiagnosis: {
      id: 'demo-diag-007',
      recoveryCaseId: 'demo-case-007',
      merchantId: 'demo-merchant-evaluator',
      recommendedAction: 'Flag for merchant operations review before initiating automated outreach',
      channel: 'MANUAL',
      confidenceScore: 0.95,
      reasoning:
        'High value corporate purchase cancelled by customer. Automated outreach suspended to avoid customer friction.',
      modelName: 'Google Gemini 3.7 Flash',
      modelVersion: 'v1-demo',
      promptTokens: 360,
      completionTokens: 88,
      decisionFactors: '{"manualReview":true,"corporateCustomer":true}',
      createdAt: '2026-08-30T11:05:00Z',
    },
  },

  // 8. Insufficient Funds Retry (In Progress -> Ready for Simulation)
  {
    id: 'demo-case-008',
    merchantId: 'demo-merchant-evaluator',
    status: 'IN_PROGRESS',
    priority: 'HIGH',
    failureReasonCategory: 'INSUFFICIENT_FUNDS',
    estimatedRecoverableAmount: 6750.0,
    recoveredAmount: 0,
    currency: 'INR',
    expiresAt: '2026-09-02T20:00:00Z',
    recoveredAt: null,
    closedAt: null,
    createdAt: '2026-08-30T15:00:00Z',
    updatedAt: '2026-08-30T15:10:00Z',
    customer: {
      id: 'demo_cust_108',
      razorpayCustomerId: 'cust_sim_8826',
      name: 'Meera Nambiar (Simulated)',
      email: 'meera.nambiar@example.com',
      phone: '+919876543217',
      createdAt: '2026-08-30T15:00:00Z',
    },
    payment: {
      id: 'demo_pay_8919',
      razorpayPaymentId: 'pay_sim_8919',
      razorpayOrderId: 'order_sim_9919',
      razorpayInvoiceId: null,
      amount: 6750.0,
      currency: 'INR',
      status: 'FAILED',
      method: 'UPI',
      errorCode: 'INSUFFICIENT_FUNDS',
      errorDescription: 'Account balance below transaction value',
      errorSource: 'customer',
      errorReason: 'account_insufficient_balance',
      riskLevel: 'LOW',
      paymentCreatedAt: '2026-08-30T15:00:00Z',
      createdAt: '2026-08-30T15:00:00Z',
    },
    attempts: [
      {
        id: 'demo-attempt-008',
        recoveryCaseId: 'demo-case-008',
        merchantId: 'demo-merchant-evaluator',
        attemptNumber: 1,
        channel: 'WHATSAPP',
        status: 'SENT',
        scheduledAt: '2026-08-30T15:10:00Z',
        executedAt: '2026-08-30T15:10:05Z',
        completedAt: null,
        resultCode: 'SENT',
        resultMessage: 'WhatsApp reminder link dispatched to customer device',
        recoveryLink: 'https://rzp.io/i/demo_link_8919',
        strategyId: 'strat-sim-108',
        strategySnapshot: {
          strategyId: 'strat-sim-108',
          channel: 'WHATSAPP',
          recommendedAction: 'Dispatch WhatsApp reminder with 4-hour window',
          confidenceScore: 0.89,
          priority: 'HIGH',
          fallbackChannel: 'EMAIL',
          fallbackAction: 'Email backup',
          reason: 'Transient balance issue, evening reminder has high likelihood',
        },
        strategyPriority: 'HIGH',
        confidenceScore: 0.89,
        recommendedAction: 'Dispatch WhatsApp reminder with 4-hour window',
        createdAt: '2026-08-30T15:10:00Z',
        updatedAt: '2026-08-30T15:10:05Z',
      },
    ],
    latestDiagnosis: {
      id: 'demo-diag-008',
      recoveryCaseId: 'demo-case-008',
      merchantId: 'demo-merchant-evaluator',
      recommendedAction: 'Dispatch WhatsApp reminder with split payment option',
      channel: 'WHATSAPP',
      confidenceScore: 0.89,
      reasoning: 'Payment attempted near salary date. WhatsApp smart link dispatched with 4-hour validity.',
      modelName: 'Google Gemini 3.7 Flash',
      modelVersion: 'v1-demo',
      promptTokens: 300,
      completionTokens: 76,
      decisionFactors: '{"salaryWindow":true,"highEngagement":true}',
      createdAt: '2026-08-30T15:05:00Z',
    },
  },

  // 9. Terminal Case: Expired (Validity Window Elapsed)
  {
    id: 'demo-case-009',
    merchantId: 'demo-merchant-evaluator',
    status: 'EXPIRED',
    priority: 'LOW',
    failureReasonCategory: 'USER_DROPOFF',
    estimatedRecoverableAmount: 2100.0,
    recoveredAmount: 0,
    currency: 'INR',
    expiresAt: '2026-08-30T09:00:00Z',
    recoveredAt: null,
    closedAt: '2026-08-30T09:00:00Z',
    createdAt: '2026-08-30T06:00:00Z',
    updatedAt: '2026-08-30T09:00:00Z',
    customer: {
      id: 'demo_cust_109',
      razorpayCustomerId: 'cust_sim_8827',
      name: 'Rajesh Kulkarni (Simulated)',
      email: 'rajesh.kulkarni@example.com',
      phone: '+919876543218',
      createdAt: '2026-08-30T06:00:00Z',
    },
    payment: {
      id: 'demo_pay_8920',
      razorpayPaymentId: 'pay_sim_8920',
      razorpayOrderId: 'order_sim_9920',
      razorpayInvoiceId: null,
      amount: 2100.0,
      currency: 'INR',
      status: 'FAILED',
      method: 'WALLET',
      errorCode: 'PAYMENT_EXPIRED',
      errorDescription: 'Payment session timed out by gateway',
      errorSource: 'gateway',
      errorReason: 'session_expired',
      riskLevel: 'LOW',
      paymentCreatedAt: '2026-08-30T06:00:00Z',
      createdAt: '2026-08-30T06:00:00Z',
    },
    attempts: [
      {
        id: 'demo-attempt-009',
        recoveryCaseId: 'demo-case-009',
        merchantId: 'demo-merchant-evaluator',
        attemptNumber: 1,
        channel: 'SMS',
        status: 'DELIVERED',
        scheduledAt: '2026-08-30T06:30:00Z',
        executedAt: '2026-08-30T06:30:05Z',
        completedAt: '2026-08-30T09:00:00Z',
        resultCode: 'WINDOW_ELAPSED',
        resultMessage: 'Recovery link expired after inventory reservation window elapsed',
        recoveryLink: 'https://rzp.io/i/demo_link_8920',
        strategyId: 'strat-sim-109',
        strategySnapshot: {
          strategyId: 'strat-sim-109',
          channel: 'SMS',
          recommendedAction: 'Send 2-hour expiring recovery link via SMS',
          confidenceScore: 0.72,
          priority: 'LOW',
          fallbackChannel: null,
          fallbackAction: null,
          reason: 'Flash sale item reservation expires in 2 hours',
        },
        strategyPriority: 'LOW',
        confidenceScore: 0.72,
        recommendedAction: 'Send 2-hour expiring recovery link via SMS',
        createdAt: '2026-08-30T06:30:00Z',
        updatedAt: '2026-08-30T09:00:00Z',
      },
    ],
    latestDiagnosis: {
      id: 'demo-diag-009',
      recoveryCaseId: 'demo-case-009',
      merchantId: 'demo-merchant-evaluator',
      recommendedAction: 'Send 2-hour expiring recovery link via SMS',
      channel: 'SMS',
      confidenceScore: 0.72,
      reasoning: 'Flash sale checkout dropoff. Link expired after inventory reservation window elapsed.',
      modelName: 'Google Gemini 3.7 Flash',
      modelVersion: 'v1-demo',
      promptTokens: 240,
      completionTokens: 64,
      decisionFactors: '{"flashSale":true,"fixedWindowHours":2}',
      createdAt: '2026-08-30T06:05:00Z',
    },
  },

  // 10. Fallback Active Scenario: WhatsApp failed -> Email fallback delivered (In Progress -> Ready for Simulation)
  {
    id: 'demo-case-010',
    merchantId: 'demo-merchant-evaluator',
    status: 'IN_PROGRESS',
    priority: 'MEDIUM',
    failureReasonCategory: 'NETWORK_TIMEOUT',
    estimatedRecoverableAmount: 5400.0,
    recoveredAmount: 0,
    currency: 'INR',
    expiresAt: '2026-09-03T10:00:00Z',
    recoveredAt: null,
    closedAt: null,
    createdAt: '2026-08-30T13:00:00Z',
    updatedAt: '2026-08-30T13:45:00Z',
    customer: {
      id: 'demo_cust_110',
      razorpayCustomerId: 'cust_sim_8828',
      name: 'Deepika Rao (Simulated)',
      email: 'deepika.rao@example.com',
      phone: '+919876543219',
      createdAt: '2026-08-30T13:00:00Z',
    },
    payment: {
      id: 'demo_pay_8921',
      razorpayPaymentId: 'pay_sim_8921',
      razorpayOrderId: 'order_sim_9921',
      razorpayInvoiceId: null,
      amount: 5400.0,
      currency: 'INR',
      status: 'FAILED',
      method: 'UPI',
      errorCode: 'NETWORK_TIMEOUT',
      errorDescription: 'Bank gateway connection reset',
      errorSource: 'gateway',
      errorReason: 'connection_reset',
      riskLevel: 'LOW',
      paymentCreatedAt: '2026-08-30T13:00:00Z',
      createdAt: '2026-08-30T13:00:00Z',
    },
    attempts: [
      {
        id: 'demo-attempt-010-1',
        recoveryCaseId: 'demo-case-010',
        merchantId: 'demo-merchant-evaluator',
        attemptNumber: 1,
        channel: 'WHATSAPP',
        status: 'FAILED',
        scheduledAt: '2026-08-30T13:05:00Z',
        executedAt: '2026-08-30T13:05:05Z',
        completedAt: '2026-08-30T13:05:10Z',
        resultCode: 'DELIVERY_FAILED',
        resultMessage: 'WhatsApp Cloud API delivery failed (number temporarily unreachable)',
        recoveryLink: 'https://rzp.io/i/demo_link_8921',
        strategyId: 'strat-sim-110',
        strategySnapshot: {
          strategyId: 'strat-sim-110',
          channel: 'WHATSAPP',
          recommendedAction: 'Dispatch WhatsApp recovery with Email fallback',
          confidenceScore: 0.87,
          priority: 'MEDIUM',
          fallbackChannel: 'EMAIL',
          fallbackAction: 'Dispatch Email recovery link with 24h validity',
          reason: 'Primary WhatsApp route with deterministic email fallback',
        },
        strategyPriority: 'MEDIUM',
        confidenceScore: 0.87,
        recommendedAction: 'Dispatch WhatsApp recovery with Email fallback',
        createdAt: '2026-08-30T13:05:00Z',
        updatedAt: '2026-08-30T13:05:10Z',
      },
      {
        id: 'demo-attempt-010-2',
        recoveryCaseId: 'demo-case-010',
        merchantId: 'demo-merchant-evaluator',
        attemptNumber: 2,
        channel: 'EMAIL',
        status: 'DELIVERED',
        scheduledAt: '2026-08-30T13:10:00Z',
        executedAt: '2026-08-30T13:10:05Z',
        completedAt: '2026-08-30T13:10:15Z',
        resultCode: 'DELIVERED',
        resultMessage: 'Fallback recovery email delivered with 1-click checkout link',
        recoveryLink: 'https://rzp.io/i/demo_link_8921',
        strategyId: 'strat-sim-110',
        strategySnapshot: null,
        strategyPriority: 'MEDIUM',
        confidenceScore: 0.87,
        recommendedAction: 'Dispatch Email recovery link with 24h validity',
        createdAt: '2026-08-30T13:10:00Z',
        updatedAt: '2026-08-30T13:10:15Z',
      },
    ],
    latestDiagnosis: {
      id: 'demo-diag-010',
      recoveryCaseId: 'demo-case-010',
      merchantId: 'demo-merchant-evaluator',
      recommendedAction: 'Dispatch WhatsApp recovery with Email fallback',
      channel: 'WHATSAPP',
      confidenceScore: 0.87,
      reasoning:
        'Primary WhatsApp attempt bounced due to network unreachability. Switched automatically to Email fallback with smart link.',
      modelName: 'Google Gemini 3.7 Flash',
      modelVersion: 'v1-demo',
      promptTokens: 310,
      completionTokens: 80,
      decisionFactors: '{"primaryFailed":true,"fallbackExecuted":true}',
      createdAt: '2026-08-30T13:02:00Z',
    },
  },
];

export const INITIAL_DEMO_NOTIFICATIONS: NotificationResponseDto[] = [
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
  {
    id: 'demo-notif-003',
    merchantId: 'demo-merchant-evaluator',
    eventType: 'CASE_EXHAUSTED',
    title: 'Recovery Attempts Exhausted',
    message: 'Recovery attempts for Sunita Reddy (₹3,200) exhausted after card expiration retry failures.',
    status: 'READ',
    read: true,
    recoveryCaseId: 'demo-case-006',
    deliveries: [
      {
        id: 'demo-del-003',
        channel: 'IN_APP',
        provider: 'InAppChannelProvider',
        status: 'DELIVERED',
        attemptedAt: '2026-08-30T16:00:00Z',
        deliveredAt: '2026-08-30T16:00:00Z',
        errorCode: null,
        errorMessage: null,
        retryCount: 0,
      },
    ],
    createdAt: '2026-08-30T16:00:00Z',
    updatedAt: '2026-08-30T16:00:00Z',
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

// Backwards-compatible constants for legacy imports in tests if needed
export const DEMO_CASES: RecoveryCase[] = INITIAL_DEMO_CASES.map((c) => ({
  id: c.id,
  merchantId: c.merchantId,
  paymentId: c.payment?.id || null,
  customerId: c.customer?.id || null,
  customerName: c.customer?.name || null,
  customerEmail: c.customer?.email || null,
  status: c.status,
  priority: c.priority,
  failureReasonCategory: c.failureReasonCategory,
  estimatedRecoverableAmount: c.estimatedRecoverableAmount,
  recoveredAmount: c.recoveredAmount,
  currency: c.currency,
  expiresAt: c.expiresAt,
  recoveredAt: c.recoveredAt,
  closedAt: c.closedAt,
  createdAt: c.createdAt,
  updatedAt: c.updatedAt,
}));

export const DEMO_CASE_DETAIL: RecoveryCaseDetail = INITIAL_DEMO_CASES[0];

// ============================================================================
// Centralized In-Memory Demo Store with Safe LocalStorage Persistence
// ============================================================================

interface StoredDemoData {
  cases: RecoveryCaseDetail[];
  notifications: NotificationResponseDto[];
}

class DemoStore {
  private cases: RecoveryCaseDetail[] = [];
  private notifications: NotificationResponseDto[] = [];
  private initialized = false;

  private deepClone<T>(val: T): T {
    return JSON.parse(JSON.stringify(val));
  }

  private init() {
    if (this.initialized) return;
    this.initialized = true;

    try {
      if (typeof window !== 'undefined' && window.localStorage) {
        const stored = localStorage.getItem(DEMO_STORAGE_STORE_KEY);
        if (stored) {
          const parsed: StoredDemoData = JSON.parse(stored);
          if (Array.isArray(parsed.cases) && Array.isArray(parsed.notifications)) {
            this.cases = parsed.cases;
            this.notifications = parsed.notifications;
            return;
          }
        }
      }
    } catch {
      // LocalStorage read failure fallback to defaults
    }

    this.cases = this.deepClone(INITIAL_DEMO_CASES);
    this.notifications = this.deepClone(INITIAL_DEMO_NOTIFICATIONS);
  }

  private persist() {
    try {
      if (typeof window !== 'undefined' && window.localStorage) {
        const payload: StoredDemoData = {
          cases: this.cases,
          notifications: this.notifications,
        };
        localStorage.setItem(DEMO_STORAGE_STORE_KEY, JSON.stringify(payload));
      }
    } catch {
      // LocalStorage write safety
    }

    // Broadcast state update event for mounted components
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent(DEMO_STATE_EVENT));
    }
  }

  public resetStore() {
    this.cases = this.deepClone(INITIAL_DEMO_CASES);
    this.notifications = this.deepClone(INITIAL_DEMO_NOTIFICATIONS);
    try {
      if (typeof window !== 'undefined' && window.localStorage) {
        localStorage.removeItem(DEMO_STORAGE_STORE_KEY);
      }
    } catch {
      // Ignored
    }
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent(DEMO_STATE_EVENT));
    }
  }

  public getCases(): RecoveryCaseDetail[] {
    this.init();
    return this.deepClone(this.cases);
  }

  public getCase(id: string): RecoveryCaseDetail | undefined {
    this.init();
    const found = this.cases.find((c) => c.id === id);
    return found ? this.deepClone(found) : undefined;
  }

  public getNotifications(): NotificationResponseDto[] {
    this.init();
    return this.deepClone(this.notifications);
  }

  public markNotificationRead(id: string): NotificationResponseDto {
    this.init();
    const target = this.notifications.find((n) => n.id === id);
    if (target) {
      target.read = true;
      target.status = 'READ';
      target.updatedAt = new Date().toISOString();
      this.persist();
      return this.deepClone(target);
    }
    throw new Error(`Demo notification with id ${id} not found`);
  }

  public markAllNotificationsRead(): { markedReadCount: number } {
    this.init();
    let count = 0;
    for (const n of this.notifications) {
      if (!n.read) {
        n.read = true;
        n.status = 'READ';
        n.updatedAt = new Date().toISOString();
        count++;
      }
    }
    this.persist();
    return { markedReadCount: count };
  }

  public simulateRecovery(caseId: string): RecoveryCaseDetail {
    this.init();
    const caseIndex = this.cases.findIndex((c) => c.id === caseId);
    if (caseIndex === -1) {
      throw new Error(`Demo recovery case with id "${caseId}" not found`);
    }

    const c = this.cases[caseIndex];

    // Terminal Case Protection (Requirement 7)
    if (
      c.status === 'RECOVERED' ||
      c.status === 'CANCELLED' ||
      c.status === 'EXPIRED' ||
      c.status === 'FAILED'
    ) {
      throw new Error(
        `Cannot simulate recovery: Case ${c.id} is in terminal status "${c.status}". Terminal cases cannot be simulated.`
      );
    }

    const now = new Date().toISOString();

    // 1. Transition Case State: OPEN / IN_PROGRESS -> RECOVERED
    c.status = 'RECOVERED';
    c.recoveredAmount = c.estimatedRecoverableAmount;
    c.recoveredAt = now;
    c.closedAt = now;
    c.updatedAt = now;

    // 2. Transition Payment State: FAILED -> CAPTURED
    if (c.payment) {
      c.payment.status = 'CAPTURED';
      c.payment.errorCode = null;
      c.payment.errorDescription = null;
      c.payment.errorReason = null;
    }

    // 3. Transition or Append Recovery Attempt: Latest attempt -> SUCCESS
    if (c.attempts && c.attempts.length > 0) {
      const latest = c.attempts[c.attempts.length - 1];
      latest.status = 'SUCCESS';
      latest.completedAt = now;
      latest.resultCode = 'PAYMENT_CAPTURED';
      latest.resultMessage = 'Customer accessed smart recovery link and completed payment authorization';
      latest.updatedAt = now;
    } else {
      c.attempts = [
        {
          id: `demo-attempt-sim-${Date.now()}`,
          recoveryCaseId: c.id,
          merchantId: c.merchantId,
          attemptNumber: 1,
          channel: c.latestDiagnosis?.channel || 'WHATSAPP',
          status: 'SUCCESS',
          scheduledAt: c.createdAt,
          executedAt: now,
          completedAt: now,
          resultCode: 'PAYMENT_CAPTURED',
          resultMessage: 'Customer authorized and captured payment via smart link',
          recoveryLink: `https://rzp.io/i/demo_link_${c.id}`,
          strategyId: 'strat-sim-instant',
          strategySnapshot: null,
          strategyPriority: c.priority,
          confidenceScore: c.latestDiagnosis?.confidenceScore || 0.9,
          recommendedAction: c.latestDiagnosis?.recommendedAction || 'Smart payment recovery',
          createdAt: c.createdAt,
          updatedAt: now,
        },
      ];
    }

    // 4. Generate PAYMENT_RECOVERED Notification (Requirement 11 - Deduplication enforced)
    const existingNotif = this.notifications.find(
      (n) => n.recoveryCaseId === caseId && n.eventType === 'PAYMENT_RECOVERED'
    );

    if (!existingNotif) {
      const formattedAmount = new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: c.currency || 'INR',
        maximumFractionDigits: 0,
      }).format(c.recoveredAmount);

      const channelName = c.latestDiagnosis?.channel || 'Smart Recovery Link';
      const customerName = c.customer?.name || 'Customer';

      const recoveryNotif: NotificationResponseDto = {
        id: `demo-notif-rec-${Date.now()}`,
        merchantId: c.merchantId,
        eventType: 'PAYMENT_RECOVERED',
        title: 'Payment Successfully Recovered (Simulated)',
        message: `A payment of ${formattedAmount} for ${customerName} was successfully recovered via ${channelName}. Case status updated to RECOVERED.`,
        status: 'UNREAD',
        read: false,
        recoveryCaseId: c.id,
        deliveries: [
          {
            id: `demo-del-${Date.now()}`,
            channel: 'IN_APP',
            provider: 'InAppChannelProvider',
            status: 'DELIVERED',
            attemptedAt: now,
            deliveredAt: now,
            errorCode: null,
            errorMessage: null,
            retryCount: 0,
          },
        ],
        createdAt: now,
        updatedAt: now,
      };

      // Prepend so it appears at top of notification feed
      this.notifications.unshift(recoveryNotif);
    }

    this.persist();
    return this.deepClone(c);
  }
}

export const demoStoreInstance = new DemoStore();

// ============================================================================
// Asynchronous Service Functions (Clean API matching real backend contracts)
// ============================================================================

export async function resetDemoStore(): Promise<void> {
  demoStoreInstance.resetStore();
}

export async function getDemoDashboard(): Promise<DashboardSummary> {
  const cases = demoStoreInstance.getCases();
  const total = cases.length;
  const openCases = cases.filter((c) => c.status === 'OPEN').length;
  const inProgressCases = cases.filter((c) => c.status === 'IN_PROGRESS').length;
  const recoveredCases = cases.filter((c) => c.status === 'RECOVERED').length;
  const expiredCases = cases.filter((c) => c.status === 'EXPIRED').length;
  const cancelledCases = cases.filter((c) => c.status === 'CANCELLED').length;
  const failedCases = cases.filter((c) => c.status === 'FAILED').length;
  const expiredOrCancelledCases = expiredCases + cancelledCases;

  const totalEstimatedRecoverableAmount = cases.reduce(
    (acc, c) => acc + (c.estimatedRecoverableAmount || 0),
    0
  );
  const totalRecoveredAmount = cases.reduce(
    (acc, c) => acc + (c.recoveredAmount || 0),
    0
  );

  const recoveryRate =
    totalEstimatedRecoverableAmount > 0
      ? Number(((totalRecoveredAmount / totalEstimatedRecoverableAmount) * 100).toFixed(1))
      : 0;

  return {
    totalRecoveryCases: total,
    openCases,
    inProgressCases,
    recoveredCases,
    expiredCases,
    cancelledCases,
    expiredOrCancelledCases,
    failedCases,
    totalEstimatedRecoverableAmount,
    totalRecoveredAmount,
    recoveryRate,
  };
}

export async function getDemoRecoveryCases(
  params?: RecoveryCaseListParams
): Promise<PageResponse<RecoveryCase>> {
  const fullCases = demoStoreInstance.getCases();

  // Map to flat RecoveryCase summary objects
  let mapped: RecoveryCase[] = fullCases.map((c) => ({
    id: c.id,
    merchantId: c.merchantId,
    paymentId: c.payment?.id || null,
    customerId: c.customer?.id || null,
    customerName: c.customer?.name || null,
    customerEmail: c.customer?.email || null,
    status: c.status,
    priority: c.priority,
    failureReasonCategory: c.failureReasonCategory,
    estimatedRecoverableAmount: c.estimatedRecoverableAmount,
    recoveredAmount: c.recoveredAmount,
    currency: c.currency,
    expiresAt: c.expiresAt,
    recoveredAt: c.recoveredAt,
    closedAt: c.closedAt,
    createdAt: c.createdAt,
    updatedAt: c.updatedAt,
  }));

  if (params?.status) {
    mapped = mapped.filter((c) => c.status === params.status);
  }
  if (params?.priority) {
    mapped = mapped.filter((c) => c.priority === params.priority);
  }
  if (params?.failureReasonCategory) {
    const q = params.failureReasonCategory.toLowerCase();
    mapped = mapped.filter((c) => c.failureReasonCategory?.toLowerCase().includes(q));
  }

  const page = params?.page || 0;
  const size = params?.size || 20;
  const startIndex = page * size;
  const pagedContent = mapped.slice(startIndex, startIndex + size);
  const totalElements = mapped.length;
  const totalPages = Math.max(1, Math.ceil(totalElements / size));

  return {
    content: pagedContent,
    totalElements,
    totalPages,
    number: page,
    size,
    first: page === 0,
    last: page >= totalPages - 1,
    empty: totalElements === 0,
  };
}

export async function getDemoRecoveryCase(id: string): Promise<RecoveryCaseDetail> {
  const found = demoStoreInstance.getCase(id);
  if (found) {
    return found;
  }
  // Fallback to first case if unknown id requested in testing
  const all = demoStoreInstance.getCases();
  if (all.length > 0) {
    return all[0];
  }
  return INITIAL_DEMO_CASES[0];
}

export async function simulateDemoRecovery(caseId: string): Promise<RecoveryCaseDetail> {
  return demoStoreInstance.simulateRecovery(caseId);
}

export async function getDemoAnalyticsOverview(
  params?: DateRangeParams
): Promise<AnalyticsOverview> {
  const cases = demoStoreInstance.getCases();
  const total = cases.length;
  const openCases = cases.filter((c) => c.status === 'OPEN').length;
  const inProgressCases = cases.filter((c) => c.status === 'IN_PROGRESS').length;
  const recoveredCases = cases.filter((c) => c.status === 'RECOVERED').length;
  const expiredCases = cases.filter((c) => c.status === 'EXPIRED').length;
  const cancelledCases = cases.filter((c) => c.status === 'CANCELLED').length;
  const failedCases = cases.filter((c) => c.status === 'FAILED').length;
  const expiredOrCancelledCases = expiredCases + cancelledCases;

  const totalEstimatedRecoverableAmount = cases.reduce(
    (acc, c) => acc + (c.estimatedRecoverableAmount || 0),
    0
  );
  const totalRecoveredAmount = cases.reduce(
    (acc, c) => acc + (c.recoveredAmount || 0),
    0
  );

  const recoveryRate =
    totalEstimatedRecoverableAmount > 0
      ? Number(((totalRecoveredAmount / totalEstimatedRecoverableAmount) * 100).toFixed(1))
      : 0;

  const averageRecoveredAmount =
    recoveredCases > 0
      ? Number((totalRecoveredAmount / recoveredCases).toFixed(2))
      : 0;

  return {
    totalCases: total,
    openCases,
    inProgressCases,
    recoveredCases,
    failedCases,
    expiredCases,
    cancelledCases,
    expiredOrCancelledCases,
    totalEstimatedRecoverableAmount,
    totalRecoveredAmount,
    recoveryRate,
    averageRecoveredAmount,
    averageTimeToRecoverySeconds: 1420,
    from: params?.from || '2026-08-01',
    to: params?.to || '2026-08-30',
  };
}

export async function getDemoRecoveryTrends(
  params?: DateRangeParams
): Promise<RecoveryTrends> {
  const cases = demoStoreInstance.getCases();
  const totalAmountAtRisk = cases.reduce((acc, c) => acc + c.estimatedRecoverableAmount, 0);
  const totalRecoveredAmount = cases.reduce((acc, c) => acc + c.recoveredAmount, 0);
  const totalCases = cases.length;
  const overallRecoveryRate =
    totalAmountAtRisk > 0 ? Number(((totalRecoveredAmount / totalAmountAtRisk) * 100).toFixed(1)) : 0;

  // Base 7-day trend array scaled dynamically with total recovered amount
  const trends: DailyRecoveryTrend[] = [
    {
      date: '2026-08-24',
      recoveryCasesCreated: 1,
      amountAtRisk: 8450.0,
      amountRecovered: 0,
      recoveredCaseCount: 0,
      recoveryRate: 0,
    },
    {
      date: '2026-08-25',
      recoveryCasesCreated: 2,
      amountAtRisk: 15700.0,
      amountRecovered: 0,
      recoveredCaseCount: 0,
      recoveryRate: 0,
    },
    {
      date: '2026-08-26',
      recoveryCasesCreated: 1,
      amountAtRisk: 3200.0,
      amountRecovered: 0,
      recoveredCaseCount: 0,
      recoveryRate: 0,
    },
    {
      date: '2026-08-27',
      recoveryCasesCreated: 1,
      amountAtRisk: 6750.0,
      amountRecovered: 0,
      recoveredCaseCount: 0,
      recoveryRate: 0,
    },
    {
      date: '2026-08-28',
      recoveryCasesCreated: 1,
      amountAtRisk: 2100.0,
      amountRecovered: 0,
      recoveredCaseCount: 0,
      recoveryRate: 0,
    },
    {
      date: '2026-08-29',
      recoveryCasesCreated: 1,
      amountAtRisk: 3200.0,
      amountRecovered: 0,
      recoveredCaseCount: 0,
      recoveryRate: 0,
    },
    {
      date: '2026-08-30',
      recoveryCasesCreated: 3,
      amountAtRisk: 21897.0,
      amountRecovered: totalRecoveredAmount,
      recoveredCaseCount: cases.filter((c) => c.status === 'RECOVERED').length,
      recoveryRate: overallRecoveryRate,
    },
  ];

  let filtered = trends;
  if (params?.from) {
    filtered = filtered.filter((t) => t.date >= params.from!);
  }
  if (params?.to) {
    filtered = filtered.filter((t) => t.date <= params.to!);
  }
  if (filtered.length === 0) {
    filtered = trends;
  }

  return {
    from: params?.from || '2026-08-24',
    to: params?.to || '2026-08-30',
    totalCases,
    totalAmountAtRisk,
    totalRecoveredAmount,
    overallRecoveryRate,
    trends: filtered,
  };
}

export async function getDemoChannelAnalytics(
  params?: DateRangeParams
): Promise<ChannelAnalytics> {
  const cases = demoStoreInstance.getCases();
  const allAttempts = cases.flatMap((c) => c.attempts || []);

  const channelMap: Record<RecoveryChannel, ChannelMetric> = {
    WHATSAPP: {
      channel: 'WHATSAPP',
      totalAttempts: 0,
      successfulAttempts: 0,
      failedAttempts: 0,
      sentAttempts: 0,
      deliveredAttempts: 0,
      clickedAttempts: 0,
      successRate: 0,
      recoveredAmount: 0,
    },
    EMAIL: {
      channel: 'EMAIL',
      totalAttempts: 0,
      successfulAttempts: 0,
      failedAttempts: 0,
      sentAttempts: 0,
      deliveredAttempts: 0,
      clickedAttempts: 0,
      successRate: 0,
      recoveredAmount: 0,
    },
    SMS: {
      channel: 'SMS',
      totalAttempts: 0,
      successfulAttempts: 0,
      failedAttempts: 0,
      sentAttempts: 0,
      deliveredAttempts: 0,
      clickedAttempts: 0,
      successRate: 0,
      recoveredAmount: 0,
    },
    SMART_LINK: {
      channel: 'SMART_LINK',
      totalAttempts: 0,
      successfulAttempts: 0,
      failedAttempts: 0,
      sentAttempts: 0,
      deliveredAttempts: 0,
      clickedAttempts: 0,
      successRate: 0,
      recoveredAmount: 0,
    },
    MANUAL: {
      channel: 'MANUAL',
      totalAttempts: 0,
      successfulAttempts: 0,
      failedAttempts: 0,
      sentAttempts: 0,
      deliveredAttempts: 0,
      clickedAttempts: 0,
      successRate: 0,
      recoveredAmount: 0,
    },
    RETRY_CHARGE: {
      channel: 'RETRY_CHARGE',
      totalAttempts: 0,
      successfulAttempts: 0,
      failedAttempts: 0,
      sentAttempts: 0,
      deliveredAttempts: 0,
      clickedAttempts: 0,
      successRate: 0,
      recoveredAmount: 0,
    },
  };

  for (const att of allAttempts) {
    const ch = channelMap[att.channel] || channelMap.WHATSAPP;
    ch.totalAttempts++;
    if (att.status === 'SUCCESS') ch.successfulAttempts++;
    if (att.status === 'FAILED') ch.failedAttempts++;
    if (att.status === 'SENT' || att.status === 'DELIVERED' || att.status === 'SUCCESS') {
      ch.sentAttempts++;
    }
    if (att.status === 'DELIVERED' || att.status === 'SUCCESS') ch.deliveredAttempts++;
    if (att.status === 'SUCCESS') ch.clickedAttempts++;
  }

  // Calculate recovered amount per channel from resolved cases
  for (const c of cases) {
    if (c.status === 'RECOVERED' && c.attempts && c.attempts.length > 0) {
      const successfulAttempt = c.attempts.find((a) => a.status === 'SUCCESS') || c.attempts[c.attempts.length - 1];
      if (successfulAttempt && channelMap[successfulAttempt.channel]) {
        channelMap[successfulAttempt.channel].recoveredAmount += c.recoveredAmount;
      }
    }
  }

  // Compute success rates
  for (const ch of Object.values(channelMap)) {
    ch.successRate =
      ch.totalAttempts > 0
        ? Number(((ch.successfulAttempts / ch.totalAttempts) * 100).toFixed(1))
        : 0;
  }

  const activeChannels = Object.values(channelMap).filter((ch) => ch.totalAttempts > 0);

  return {
    from: params?.from || '2026-08-01',
    to: params?.to || '2026-08-30',
    totalAttempts: allAttempts.length,
    channels: activeChannels.length > 0 ? activeChannels : [channelMap.WHATSAPP, channelMap.EMAIL, channelMap.SMS],
  };
}

export async function getDemoFailureAnalytics(
  params?: DateRangeParams
): Promise<FailureAnalytics> {
  const cases = demoStoreInstance.getCases();

  const categoryMap: Record<string, FailureCategoryMetric> = {};
  const priorityMap: Record<RecoveryPriority, FailurePriorityMetric> = {
    CRITICAL: {
      priority: 'CRITICAL',
      caseCount: 0,
      estimatedRecoverableAmount: 0,
      recoveredAmount: 0,
      recoveredCaseCount: 0,
      recoveryRate: 0,
    },
    HIGH: {
      priority: 'HIGH',
      caseCount: 0,
      estimatedRecoverableAmount: 0,
      recoveredAmount: 0,
      recoveredCaseCount: 0,
      recoveryRate: 0,
    },
    MEDIUM: {
      priority: 'MEDIUM',
      caseCount: 0,
      estimatedRecoverableAmount: 0,
      recoveredAmount: 0,
      recoveredCaseCount: 0,
      recoveryRate: 0,
    },
    LOW: {
      priority: 'LOW',
      caseCount: 0,
      estimatedRecoverableAmount: 0,
      recoveredAmount: 0,
      recoveredCaseCount: 0,
      recoveryRate: 0,
    },
  };

  for (const c of cases) {
    const catKey = c.failureReasonCategory || 'OTHER';
    if (!categoryMap[catKey]) {
      categoryMap[catKey] = {
        failureReasonCategory: catKey,
        caseCount: 0,
        estimatedRecoverableAmount: 0,
        recoveredAmount: 0,
        recoveredCaseCount: 0,
        recoveryRate: 0,
      };
    }
    categoryMap[catKey].caseCount++;
    categoryMap[catKey].estimatedRecoverableAmount += c.estimatedRecoverableAmount;
    categoryMap[catKey].recoveredAmount += c.recoveredAmount;
    if (c.status === 'RECOVERED') {
      categoryMap[catKey].recoveredCaseCount++;
    }

    const prio = priorityMap[c.priority] || priorityMap.MEDIUM;
    prio.caseCount++;
    prio.estimatedRecoverableAmount += c.estimatedRecoverableAmount;
    prio.recoveredAmount += c.recoveredAmount;
    if (c.status === 'RECOVERED') {
      prio.recoveredCaseCount++;
    }
  }

  for (const cat of Object.values(categoryMap)) {
    cat.recoveryRate =
      cat.estimatedRecoverableAmount > 0
        ? Number(((cat.recoveredAmount / cat.estimatedRecoverableAmount) * 100).toFixed(1))
        : 0;
  }

  for (const prio of Object.values(priorityMap)) {
    prio.recoveryRate =
      prio.estimatedRecoverableAmount > 0
        ? Number(((prio.recoveredAmount / prio.estimatedRecoverableAmount) * 100).toFixed(1))
        : 0;
  }

  return {
    from: params?.from || '2026-08-01',
    to: params?.to || '2026-08-30',
    totalCases: cases.length,
    categories: Object.values(categoryMap),
    priorities: Object.values(priorityMap),
  };
}

export async function getDemoAttemptAnalytics(
  params?: DateRangeParams
): Promise<AttemptAnalytics> {
  const cases = demoStoreInstance.getCases();
  const allAttempts = cases.flatMap((c) => c.attempts || []);

  const attemptsByStatus: Record<RecoveryAttemptStatus, number> = {
    SCHEDULED: 0,
    IN_FLIGHT: 0,
    SENT: 0,
    DELIVERED: 0,
    CLICKED: 0,
    SUCCESS: 0,
    FAILED: 0,
    SKIPPED: 0,
  };

  const attemptsByChannel: Record<RecoveryChannel, number> = {
    WHATSAPP: 0,
    EMAIL: 0,
    SMS: 0,
    RETRY_CHARGE: 0,
    SMART_LINK: 0,
    MANUAL: 0,
  };

  let successfulAttempts = 0;
  let failedAttempts = 0;
  let sentAttempts = 0;
  let deliveredAttempts = 0;

  for (const att of allAttempts) {
    if (attemptsByStatus[att.status] !== undefined) {
      attemptsByStatus[att.status]++;
    }
    if (attemptsByChannel[att.channel] !== undefined) {
      attemptsByChannel[att.channel]++;
    }
    if (att.status === 'SUCCESS') successfulAttempts++;
    if (att.status === 'FAILED') failedAttempts++;
    if (att.status === 'SENT' || att.status === 'DELIVERED' || att.status === 'SUCCESS') {
      sentAttempts++;
    }
    if (att.status === 'DELIVERED' || att.status === 'SUCCESS') {
      deliveredAttempts++;
    }
  }

  const successRate =
    allAttempts.length > 0
      ? Number(((successfulAttempts / allAttempts.length) * 100).toFixed(1))
      : 0;

  const averageAttemptsPerRecoveryCase =
    cases.length > 0
      ? Number((allAttempts.length / cases.length).toFixed(2))
      : 0;

  return {
    from: params?.from || '2026-08-01',
    to: params?.to || '2026-08-30',
    totalAttempts: allAttempts.length,
    successfulAttempts,
    failedAttempts,
    scheduledAttempts: attemptsByStatus.SCHEDULED,
    inFlightAttempts: attemptsByStatus.IN_FLIGHT,
    sentAttempts,
    deliveredAttempts,
    clickedAttempts: successfulAttempts,
    skippedAttempts: attemptsByStatus.SKIPPED,
    successRate,
    averageAttemptsPerRecoveryCase,
    attemptsByStatus,
    attemptsByChannel,
  };
}

export async function getDemoNotifications(params?: {
  page?: number;
  size?: number;
  unreadOnly?: boolean;
  event?: MerchantNotificationEvent;
}): Promise<PageResponse<NotificationResponseDto>> {
  let list = demoStoreInstance.getNotifications();

  if (params?.unreadOnly) {
    list = list.filter((n) => !n.read);
  }
  if (params?.event && (params.event as string) !== 'ALL') {
    list = list.filter((n) => n.eventType === params.event);
  }

  const page = params?.page || 0;
  const size = params?.size || 10;
  const startIndex = page * size;
  const pagedContent = list.slice(startIndex, startIndex + size);
  const totalElements = list.length;
  const totalPages = Math.max(1, Math.ceil(totalElements / size));

  return {
    content: pagedContent,
    totalElements,
    totalPages,
    number: page,
    size,
    first: page === 0,
    last: page >= totalPages - 1,
    empty: totalElements === 0,
  };
}

export async function getDemoUnreadCount(): Promise<number> {
  const notifs = demoStoreInstance.getNotifications();
  return notifs.filter((n) => !n.read).length;
}

export async function markDemoNotificationRead(
  id: string
): Promise<NotificationResponseDto> {
  return demoStoreInstance.markNotificationRead(id);
}

export async function markAllDemoNotificationsRead(): Promise<{ markedReadCount: number }> {
  return demoStoreInstance.markAllNotificationsRead();
}

export async function getDemoProviderHealth(): Promise<ProviderHealthSummary> {
  return DEMO_PROVIDER_HEALTH;
}

export async function getDemoNotificationPreferences(): Promise<NotificationPreferenceResponseDto> {
  return DEMO_NOTIFICATION_PREFERENCES;
}
