import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { AnalyticsPage } from '../pages/analytics/AnalyticsPage';
import * as analyticsApi from '../api/analytics';
import type {
  AnalyticsOverview,
  RecoveryTrends,
  ChannelAnalytics,
  FailureAnalytics,
} from '../types/analytics';

vi.mock('../api/analytics');

const mockOverview: AnalyticsOverview = {
  totalCases: 42,
  openCases: 5,
  inProgressCases: 7,
  recoveredCases: 25,
  failedCases: 5,
  expiredCases: 0,
  cancelledCases: 0,
  expiredOrCancelledCases: 0,
  totalEstimatedRecoverableAmount: 125000,
  totalRecoveredAmount: 85000,
  recoveryRate: 59.5,
  averageRecoveredAmount: 3400,
  averageTimeToRecoverySeconds: 7200,
  from: '2026-08-01',
  to: '2026-08-30',
};

const mockTrends: RecoveryTrends = {
  from: '2026-08-01',
  to: '2026-08-30',
  totalCases: 42,
  totalAmountAtRisk: 125000,
  totalRecoveredAmount: 85000,
  overallRecoveryRate: 59.5,
  trends: [
    {
      date: '2026-08-01',
      recoveryCasesCreated: 5,
      amountAtRisk: 15000,
      amountRecovered: 10000,
      recoveredCaseCount: 3,
      recoveryRate: 60.0,
    },
    {
      date: '2026-08-02',
      recoveryCasesCreated: 8,
      amountAtRisk: 25000,
      amountRecovered: 18000,
      recoveredCaseCount: 5,
      recoveryRate: 62.5,
    },
  ],
};

const mockChannels: ChannelAnalytics = {
  from: '2026-08-01',
  to: '2026-08-30',
  totalAttempts: 50,
  channels: [
    {
      channel: 'WHATSAPP',
      totalAttempts: 30,
      successfulAttempts: 20,
      failedAttempts: 2,
      sentAttempts: 30,
      deliveredAttempts: 28,
      clickedAttempts: 24,
      successRate: 66.7,
      recoveredAmount: 65000,
    },
    {
      channel: 'EMAIL',
      totalAttempts: 20,
      successfulAttempts: 5,
      failedAttempts: 3,
      sentAttempts: 20,
      deliveredAttempts: 19,
      clickedAttempts: 8,
      successRate: 25.0,
      recoveredAmount: 20000,
    },
  ],
};

const mockFailures: FailureAnalytics = {
  from: '2026-08-01',
  to: '2026-08-30',
  totalCases: 42,
  categories: [
    {
      failureReasonCategory: 'INSUFFICIENT_FUNDS',
      caseCount: 25,
      estimatedRecoverableAmount: 75000,
      recoveredAmount: 50000,
      recoveredCaseCount: 16,
      recoveryRate: 64.0,
    },
    {
      failureReasonCategory: 'AUTHENTICATION_FAILED',
      caseCount: 17,
      estimatedRecoverableAmount: 50000,
      recoveredAmount: 35000,
      recoveredCaseCount: 9,
      recoveryRate: 52.9,
    },
  ],
  priorities: [
    {
      priority: 'HIGH',
      caseCount: 20,
      estimatedRecoverableAmount: 60000,
      recoveredAmount: 40000,
      recoveredCaseCount: 13,
      recoveryRate: 65.0,
    },
    {
      priority: 'CRITICAL',
      caseCount: 15,
      estimatedRecoverableAmount: 45000,
      recoveredAmount: 30000,
      recoveredCaseCount: 8,
      recoveryRate: 53.3,
    },
  ],
};

describe('AnalyticsPage Redesign - Premium Payment Recovery Intelligence Console', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(analyticsApi.getAnalyticsOverview).mockResolvedValue(mockOverview);
    vi.mocked(analyticsApi.getRecoveryTrends).mockResolvedValue(mockTrends);
    vi.mocked(analyticsApi.getChannelAnalytics).mockResolvedValue(mockChannels);
    vi.mocked(analyticsApi.getFailureAnalytics).mockResolvedValue(mockFailures);
  });

  it('renders all redesigned sections with light fintech hierarchy and analytical intelligence', async () => {
    render(
      <MemoryRouter>
        <AnalyticsPage />
      </MemoryRouter>
    );

    // 1. Page Header (eyebrow, title, copy, refresh)
    expect(screen.getByText(/Recovery Intelligence/i)).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 1, name: /Analytics & Intelligence/i })).toBeInTheDocument();
    expect(
      screen.getByText(/Understand recovery performance, channel efficiency, and the root causes behind failed payments/i)
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Refresh/i })).toBeInTheDocument();

    // 2. Reporting Window Toolbar
    expect(screen.getByText(/Reporting Window/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Last 7 Days' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Last 30 Days' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Last 90 Days' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Last 12 Months' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Custom' })).toBeInTheDocument();

    // 3. Dominant Recovery Rate Card & Supporting KPIs
    await waitFor(() => {
      // Primary Dominant Metric
      expect(screen.getAllByText('Recovery Rate').length).toBeGreaterThan(0);
      expect(screen.getByText('Closed-loop recovery efficiency')).toBeInTheDocument();
      expect(screen.getAllByText('59.5%').length).toBeGreaterThan(0);

      // Supporting KPIs
      expect(screen.getByText('Recovered Revenue')).toBeInTheDocument();
      expect(screen.getByText('42')).toBeInTheDocument(); // Total cases
      expect(screen.getByText('25')).toBeInTheDocument(); // Recovered cases
      expect(screen.getByText('Avg Recovery')).toBeInTheDocument();
      expect(screen.getByText('Avg Duration')).toBeInTheDocument();
      expect(screen.getByText('2h 0m')).toBeInTheDocument();
    });

    // 4. Recovery Performance Trends (Chart and accessible Data Table)
    expect(screen.getAllByText(/Recovery Performance/i).length).toBeGreaterThan(0);
    expect(screen.getByText('Recovery Performance Trends')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Revenue \(INR\)/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Cases Volume/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Data Table/i })).toBeInTheDocument();

    // 5. Channel Performance with Dynamic Best Channel Highlight
    expect(screen.getByText('Channel Performance')).toBeInTheDocument();
    expect(screen.getByText(/Best Performing Channel/i)).toBeInTheDocument();
    expect(screen.getByText(/WhatsApp \(Top Performer\)/i)).toBeInTheDocument();
    expect(screen.getByText(/66.7% success rate/i)).toBeInTheDocument();
    expect(screen.getByText('WhatsApp')).toBeInTheDocument();
    expect(screen.getByText('Email')).toBeInTheDocument();

    // 6. Failure Root Causes & Priority Distribution
    expect(screen.getByText('Failure Root-Causes & Priority Distribution')).toBeInTheDocument();
    expect(screen.getByText('Failure Root Causes')).toBeInTheDocument();
    expect(screen.getByText('INSUFFICIENT_FUNDS')).toBeInTheDocument();
    expect(screen.getByText('AUTHENTICATION_FAILED')).toBeInTheDocument();
    expect(screen.getByText('Priority Distribution')).toBeInTheDocument();
    expect(screen.getByText('HIGH')).toBeInTheDocument();
    expect(screen.getByText('CRITICAL')).toBeInTheDocument();

    // 7. Dynamic Data-Derived Recovery Insight Callout
    expect(screen.getByText(/Recovery Insight/i)).toBeInTheDocument();
    expect(
      screen.getByText(
        /WhatsApp currently has the highest conversion efficiency, while insufficient funds represents a significant share of recoverable revenue/i
      )
    ).toBeInTheDocument();

    // 8. Shared Light Fintech Footer
    expect(screen.getByText('RecoverAI')).toBeInTheDocument();
    expect(screen.getByText('Platform Navigation')).toBeInTheDocument();
    expect(screen.getByText('System Infrastructure')).toBeInTheDocument();
  });

  it('allows toggling between visual chart and accessible data table view', async () => {
    render(
      <MemoryRouter>
        <AnalyticsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Recovery Performance Trends')).toBeInTheDocument();
    });

    const dataTableBtn = screen.getByRole('button', { name: /Data Table/i });
    fireEvent.click(dataTableBtn);

    // Verify Data Table columns rendered
    expect(screen.getByText('Cases Ingested')).toBeInTheDocument();
    expect(screen.getByText('Cases Recovered')).toBeInTheDocument();
    expect(screen.getAllByText('Amount at Risk').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Amount Recovered').length).toBeGreaterThan(0);
  });
});
