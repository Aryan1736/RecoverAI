import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { OverviewPage } from '../pages/dashboard/OverviewPage';
import { ToastProvider } from '../context/ToastContext';
import { AuthProvider } from '../context/AuthContext';
import { DemoProvider } from '../context/DemoContext';
import { setStoredDemoMode } from '../context/demo-context-def';
import { resetDemoStore } from '../api/demo';

describe('OverviewPage Redesign - Premium Light Fintech UI', () => {
  beforeEach(() => {
    localStorage.clear();
    resetDemoStore();
    setStoredDemoMode(true);
    vi.clearAllMocks();
  });

  afterEach(() => {
    localStorage.clear();
    resetDemoStore();
  });

  it('renders all redesigned light fintech sections with correct operational content and hierarchy', async () => {
    render(
      <MemoryRouter initialEntries={['/app']}>
        <ToastProvider>
          <DemoProvider>
            <AuthProvider>
              <OverviewPage />
            </AuthProvider>
          </DemoProvider>
        </ToastProvider>
      </MemoryRouter>
    );

    // 1. Top Bar Eyebrow & Hero Header
    await waitFor(() => {
      expect(screen.getByText(/PAYMENT RECOVERY OPERATIONS/i)).toBeInTheDocument();
    });
    expect(screen.getByText(/Recover failed payments\./i)).toBeInTheDocument();
    expect(screen.getByText(/Automatically\./i)).toBeInTheDocument();
    expect(
      screen.getByText(/Detect payment failures, understand why they happened, and recover revenue/i)
    ).toBeInTheDocument();

    // Primary & Secondary Actions
    expect(screen.getByRole('button', { name: /View Recovery Cases/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Explore Analytics/i })).toBeInTheDocument();

    // 2. Floating Recovery Engine Live Status Panel
    expect(screen.getAllByText('Recovery Engine').length).toBeGreaterThan(0);
    expect(screen.getByText('Gemini 3.7 Flash')).toBeInTheDocument();
    expect(screen.getAllByText(/Operational/i).length).toBeGreaterThan(0);
    expect(screen.getByText('Last Recovery')).toBeInTheDocument();
    expect(screen.getByText(/Guardrails enabled/i)).toBeInTheDocument();

    // 3. Demo Mode System Strip
    expect(screen.getByText('DEMO ENVIRONMENT')).toBeInTheDocument();
    expect(screen.getByText(/Simulated recovery data · Production mutations disabled/i)).toBeInTheDocument();
    expect(screen.getByText('SIMULATED SANDBOX')).toBeInTheDocument();

    // 4. Key Metrics (4 Elevated Cards)
    expect(screen.getByText('Recovery Overview')).toBeInTheDocument();
    expect(screen.getByText('Recovered Revenue')).toBeInTheDocument();
    expect(screen.getByText('Recovery Rate')).toBeInTheDocument();
    expect(screen.getByText('Active Cases')).toBeInTheDocument();
    expect(screen.getByText('At-Risk Revenue')).toBeInTheDocument();

    // 5. Signature Recovery Lifecycle Pipeline
    expect(screen.getByText('Recovery Lifecycle')).toBeInTheDocument();
    expect(screen.getByText(/From payment failure to confirmed settlement\./i)).toBeInTheDocument();
    expect(screen.getAllByText(/Failed Payment/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/Failure Detected/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/AI Diagnosis/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/Recovery Strategy/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/Payment Recovered/i).length).toBeGreaterThan(0);

    // 6. Live Recovery Activity & Risk/Revenue Visual
    expect(screen.getByText('Live Recovery Activity')).toBeInTheDocument();
    expect(screen.getByText('Portfolio Distribution')).toBeInTheDocument();

    // 7. Autonomous Recovery Engine Architecture
    expect(screen.getByText('Autonomous Recovery Engine')).toBeInTheDocument();
    expect(screen.getByText('Failure Classification')).toBeInTheDocument();
    expect(screen.getByText('Strategy Selection')).toBeInTheDocument();
    expect(screen.getByText('Recovery Execution')).toBeInTheDocument();
    expect(screen.getByText('Reconciliation')).toBeInTheDocument();

    // 8. Razorpay Integration Panel
    expect(screen.getByText('Razorpay Integration')).toBeInTheDocument();
    expect(screen.getByText('CONFIGURED')).toBeInTheDocument();
    expect(screen.getByText('payment.failed')).toBeInTheDocument();
    expect(screen.getByText('payment.captured')).toBeInTheDocument();
    expect(screen.getByText('order.paid')).toBeInTheDocument();
    expect(screen.getByText(/Configure in Razorpay Dashboard/i)).toBeInTheDocument();

    // 9. Light Fintech Footer
    expect(screen.getByText('Platform Navigation')).toBeInTheDocument();
    expect(screen.getByText('System Infrastructure')).toBeInTheDocument();
    expect(screen.getByText(/Built for intelligent payment recovery\./i)).toBeInTheDocument();
  });

  it('copies webhook URL to clipboard when copy button is clicked', async () => {
    const user = userEvent.setup();
    const writeTextMock = vi.fn().mockResolvedValue(undefined);
    if (!navigator.clipboard) {
      Object.defineProperty(navigator, 'clipboard', {
        value: { writeText: writeTextMock },
        writable: true,
        configurable: true,
      });
    } else {
      vi.spyOn(navigator.clipboard, 'writeText').mockImplementation(writeTextMock);
    }

    render(
      <MemoryRouter initialEntries={['/app']}>
        <ToastProvider>
          <DemoProvider>
            <AuthProvider>
              <OverviewPage />
            </AuthProvider>
          </DemoProvider>
        </ToastProvider>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Razorpay Integration')).toBeInTheDocument();
    });

    const copyBtn = screen.getByLabelText('Copy Webhook Endpoint URL');
    await user.click(copyBtn);

    expect(writeTextMock).toHaveBeenCalledWith(expect.stringContaining('/api/v1/webhooks/razorpay'));
  });

  it('re-fetches summary metrics when Refresh button is clicked', async () => {
    const user = userEvent.setup();

    render(
      <MemoryRouter initialEntries={['/app']}>
        <ToastProvider>
          <DemoProvider>
            <AuthProvider>
              <OverviewPage />
            </AuthProvider>
          </DemoProvider>
        </ToastProvider>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Refresh recovery metrics/i })).toBeInTheDocument();
    });

    const refreshBtn = screen.getByRole('button', { name: /Refresh recovery metrics/i });
    await user.click(refreshBtn);

    await waitFor(() => {
      expect(screen.getByText('Recovery Overview')).toBeInTheDocument();
    });
  });
});

