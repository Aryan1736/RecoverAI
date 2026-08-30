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
  ],
};

describe('AnalyticsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(analyticsApi.getAnalyticsOverview).mockResolvedValue(mockOverview);
    vi.mocked(analyticsApi.getRecoveryTrends).mockResolvedValue(mockTrends);
    vi.mocked(analyticsApi.getChannelAnalytics).mockResolvedValue(mockChannels);
    vi.mocked(analyticsApi.getFailureAnalytics).mockResolvedValue(mockFailures);
  });

  it('renders page header, reporting window, and KPI metric cards', async () => {
    render(
      <MemoryRouter>
        <AnalyticsPage />
      </MemoryRouter>
    );

    expect(screen.getByText('Analytics & Intelligence')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('42')).toBeInTheDocument(); // Total cases
      expect(screen.getByText('25')).toBeInTheDocument(); // Recovered cases
      expect(screen.getAllByText('59.5%').length).toBeGreaterThan(0); // Recovery rate
      expect(screen.getByText('2h 0m')).toBeInTheDocument(); // Duration formatted
    });
  });

  it('renders recovery performance trends and supports switching to accessible data table', async () => {
    render(
      <MemoryRouter>
        <AnalyticsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Recovery Performance Trends')).toBeInTheDocument();
    });

    // Toggle data table view
    const tableButton = screen.getByRole('button', { name: /Data Table/i });
    fireEvent.click(tableButton);

    expect(screen.getByText('Cases Ingested')).toBeInTheDocument();
    expect(screen.getAllByText('Amount at Risk').length).toBeGreaterThan(0);
  });

  it('renders channel performance metrics and failure categories', async () => {
    render(
      <MemoryRouter>
        <AnalyticsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Channel Performance')).toBeInTheDocument();
      expect(screen.getByText('WhatsApp')).toBeInTheDocument();
      expect(screen.getByText('Email')).toBeInTheDocument();
      expect(screen.getByText('Failure Root-Causes & Priority Distribution')).toBeInTheDocument();
      expect(screen.getByText('INSUFFICIENT_FUNDS')).toBeInTheDocument();
    });
  });

  it('updates date range when preset button is clicked', async () => {
    render(
      <MemoryRouter>
        <AnalyticsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('42')).toBeInTheDocument();
    });

    const last7DaysBtn = screen.getByRole('button', { name: 'Last 7 Days' });
    fireEvent.click(last7DaysBtn);

    await waitFor(() => {
      expect(analyticsApi.getAnalyticsOverview).toHaveBeenCalledTimes(2);
    });
  });

  it('displays ErrorState with retry button when API call fails', async () => {
    vi.mocked(analyticsApi.getAnalyticsOverview).mockRejectedValueOnce(
      new Error('Network error loading overview')
    );

    render(
      <MemoryRouter>
        <AnalyticsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Failed to Load Analytics')).toBeInTheDocument();
      expect(screen.getByText('Network error loading overview')).toBeInTheDocument();
    });

    const retryBtn = screen.getByRole('button', { name: /Try Again/i });
    expect(retryBtn).toBeInTheDocument();
  });
});
