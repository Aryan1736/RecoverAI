import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { RecoveryCaseDetailPage } from '../pages/recovery-cases/RecoveryCaseDetailPage';
import { ToastProvider } from '../context/ToastContext';
import * as recoveryCasesApi from '../api/recovery-cases';
import type { RecoveryCaseDetail } from '../types/recovery-case';

vi.mock('../api/recovery-cases');

const mockDetail: RecoveryCaseDetail = {
  id: 'case-abc-123',
  merchantId: 'm1',
  status: 'IN_PROGRESS',
  priority: 'CRITICAL',
  failureReasonCategory: 'INSUFFICIENT_FUNDS',
  estimatedRecoverableAmount: 15999,
  recoveredAmount: 0,
  currency: 'INR',
  expiresAt: '2026-09-05T00:00:00Z',
  recoveredAt: null,
  closedAt: null,
  createdAt: '2026-08-30T10:00:00Z',
  updatedAt: '2026-08-30T10:15:00Z',
  customer: {
    id: 'cust-1',
    razorpayCustomerId: 'cust_rzp_987',
    name: 'Ada Lovelace',
    email: 'ada@example.com',
    phone: '+91 98765 43210',
    createdAt: '2026-08-20T00:00:00Z',
  },
  payment: {
    id: 'pay-1',
    razorpayPaymentId: 'pay_rzp_12345',
    razorpayOrderId: 'order_rzp_67890',
    razorpayInvoiceId: null,
    amount: 15999,
    currency: 'INR',
    status: 'FAILED',
    method: 'CARD',
    errorCode: 'BAD_REQUEST_ERROR',
    errorDescription: 'The card has insufficient funds to complete the payment',
    errorSource: 'bank',
    errorReason: 'payment_failed',
    riskLevel: 'LOW',
    paymentCreatedAt: '2026-08-30T09:59:00Z',
    createdAt: '2026-08-30T10:00:00Z',
  },
  latestDiagnosis: {
    id: 'diag-1',
    recoveryCaseId: 'case-abc-123',
    merchantId: 'm1',
    recommendedAction: 'Send Smart Recovery Link via WhatsApp',
    channel: 'WHATSAPP',
    confidenceScore: 0.94,
    reasoning: 'High-intent buyer with debit card insufficient funds. Instant messaging recovery link has 82% conversion within 4 hours.',
    modelName: 'Gemini 3.7 Flash',
    modelVersion: '1.0.0',
    promptTokens: 450,
    completionTokens: 85,
    decisionFactors: '{"cartValue":15999,"customerFrequency":3}',
    createdAt: '2026-08-30T10:02:00Z',
  },
  attempts: [
    {
      id: 'att-1',
      recoveryCaseId: 'case-abc-123',
      merchantId: 'm1',
      attemptNumber: 1,
      channel: 'WHATSAPP',
      status: 'SENT',
      scheduledAt: '2026-08-30T10:05:00Z',
      executedAt: '2026-08-30T10:05:05Z',
      completedAt: null,
      resultCode: 'MESSAGE_QUEUED',
      resultMessage: 'WhatsApp template message delivered to provider queue',
      recoveryLink: 'https://pay.recoverai.io/r/demo-link-1',
      strategyId: 'strat-1',
      strategySnapshot: {
        strategyId: 'strat-1',
        channel: 'WHATSAPP',
        recommendedAction: 'Send Smart Recovery Link via WhatsApp',
        confidenceScore: 0.94,
        priority: 'CRITICAL',
        fallbackChannel: 'EMAIL',
        fallbackAction: 'Send fallback recovery invoice',
        reason: 'Optimal multi-channel dispatch policy',
      },
      strategyPriority: 'CRITICAL',
      confidenceScore: 0.94,
      recommendedAction: 'Send Smart Recovery Link via WhatsApp',
      createdAt: '2026-08-30T10:05:00Z',
      updatedAt: '2026-08-30T10:05:05Z',
    },
  ],
};

describe('RecoveryCaseDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(recoveryCasesApi.getRecoveryCase).mockResolvedValue(mockDetail);
    vi.mocked(recoveryCasesApi.cancelRecoveryCase).mockResolvedValue({
      ...mockDetail,
      paymentId: 'pay-1',
      customerId: 'cust-1',
      customerName: 'Ada Lovelace',
      customerEmail: 'ada@example.com',
      status: 'CANCELLED',
    });
  });

  const renderComponent = () =>
    render(
      <MemoryRouter initialEntries={['/recovery-cases/case-abc-123']}>
        <ToastProvider>
          <Routes>
            <Route path="/recovery-cases/:id" element={<RecoveryCaseDetailPage />} />
          </Routes>
        </ToastProvider>
      </MemoryRouter>
    );

  it('renders case header, status, customer, payment and AI diagnosis cards', async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/case-abc-123/)).toBeInTheDocument();
      expect(screen.getByText('Ada Lovelace')).toBeInTheDocument();
      expect(screen.getByText('ada@example.com')).toBeInTheDocument();
      expect(screen.getByText('pay_rzp_12345')).toBeInTheDocument();
      expect(screen.getByText(/The card has insufficient funds/i)).toBeInTheDocument();
      expect(screen.getAllByText(/Send Smart Recovery Link via WhatsApp/i).length).toBeGreaterThan(0);
      expect(screen.getByText(/Gemini 3.7 Flash/i)).toBeInTheDocument();
      expect(screen.getAllByText(/94% Confidence/i).length).toBeGreaterThan(0);
    });
  });

  it('renders recovery strategy and chronological attempts timeline', async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Recovery Strategy')).toBeInTheDocument();
      expect(screen.getByText('Execution Timeline & Attempts')).toBeInTheDocument();
      expect(screen.getByText('Attempt #1: WHATSAPP')).toBeInTheDocument();
      expect(screen.getByText('View Recovery Link')).toBeInTheDocument();
    });
  });

  it('displays Cancel Case button for active case and handles confirmation flow', async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Cancel Case/i })).toBeInTheDocument();
    });

    // Open confirmation modal
    fireEvent.click(screen.getByRole('button', { name: /Cancel Case/i }));

    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByText('Are you sure you want to cancel this automated recovery process?')).toBeInTheDocument();

    // Confirm cancellation
    const confirmBtn = screen.getByRole('button', { name: /Confirm Cancellation/i });
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(recoveryCasesApi.cancelRecoveryCase).toHaveBeenCalledWith('case-abc-123');
    });
  });

  it('hides Cancel Case button when case is already in a terminal state (RECOVERED)', async () => {
    vi.mocked(recoveryCasesApi.getRecoveryCase).mockResolvedValueOnce({
      ...mockDetail,
      status: 'RECOVERED',
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/case-abc-123/)).toBeInTheDocument();
    });

    expect(screen.queryByRole('button', { name: /Cancel Case/i })).not.toBeInTheDocument();
  });
});
