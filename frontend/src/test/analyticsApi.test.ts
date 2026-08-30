import { describe, it, expect, vi, beforeEach } from 'vitest';
import {
  getAnalyticsOverview,
  getRecoveryTrends,
  getFailureAnalytics,
  getChannelAnalytics,
  getAttemptAnalytics,
} from '../api/analytics';
import { apiClient } from '../api/client';
import type {
  AnalyticsOverview,
  RecoveryTrends,
  FailureAnalytics,
  ChannelAnalytics,
  AttemptAnalytics,
} from '../types/analytics';

vi.mock('../api/client', () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('Analytics API Client', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('getAnalyticsOverview', () => {
    it('requests /api/v1/analytics/overview without query params when range is omitted', async () => {
      const mockOverview: AnalyticsOverview = {
        totalCases: 10,
        openCases: 2,
        inProgressCases: 3,
        recoveredCases: 4,
        failedCases: 1,
        expiredCases: 0,
        cancelledCases: 0,
        expiredOrCancelledCases: 0,
        totalEstimatedRecoverableAmount: 15000,
        totalRecoveredAmount: 8500,
        recoveryRate: 40.0,
        averageRecoveredAmount: 2125,
        averageTimeToRecoverySeconds: 3600,
        from: '2026-08-01',
        to: '2026-08-30',
      };
      vi.mocked(apiClient.get).mockResolvedValueOnce(mockOverview);

      const result = await getAnalyticsOverview();
      expect(apiClient.get).toHaveBeenCalledWith('/api/v1/analytics/overview');
      expect(result).toEqual(mockOverview);
    });

    it('correctly appends from and to query parameters', async () => {
      vi.mocked(apiClient.get).mockResolvedValueOnce({} as AnalyticsOverview);

      await getAnalyticsOverview({ from: '2026-08-01', to: '2026-08-15' });
      expect(apiClient.get).toHaveBeenCalledWith(
        '/api/v1/analytics/overview?from=2026-08-01&to=2026-08-15'
      );
    });
  });

  describe('getRecoveryTrends', () => {
    it('requests /api/v1/analytics/recovery-trends with formatted range query', async () => {
      const mockTrends: RecoveryTrends = {
        from: '2026-08-01',
        to: '2026-08-07',
        totalCases: 5,
        totalAmountAtRisk: 10000,
        totalRecoveredAmount: 6000,
        overallRecoveryRate: 60,
        trends: [],
      };
      vi.mocked(apiClient.get).mockResolvedValueOnce(mockTrends);

      const result = await getRecoveryTrends({ from: '2026-08-01', to: '2026-08-07' });
      expect(apiClient.get).toHaveBeenCalledWith(
        '/api/v1/analytics/recovery-trends?from=2026-08-01&to=2026-08-07'
      );
      expect(result).toEqual(mockTrends);
    });
  });

  describe('getFailureAnalytics', () => {
    it('requests /api/v1/analytics/failures endpoint', async () => {
      const mockFailures: FailureAnalytics = {
        from: '2026-08-01',
        to: '2026-08-30',
        totalCases: 12,
        categories: [],
        priorities: [],
      };
      vi.mocked(apiClient.get).mockResolvedValueOnce(mockFailures);

      const result = await getFailureAnalytics();
      expect(apiClient.get).toHaveBeenCalledWith('/api/v1/analytics/failures');
      expect(result.totalCases).toBe(12);
    });
  });

  describe('getChannelAnalytics', () => {
    it('requests /api/v1/analytics/channels with date range', async () => {
      const mockChannels: ChannelAnalytics = {
        from: '2026-08-01',
        to: '2026-08-30',
        totalAttempts: 20,
        channels: [],
      };
      vi.mocked(apiClient.get).mockResolvedValueOnce(mockChannels);

      const result = await getChannelAnalytics({ from: '2026-08-01', to: '2026-08-30' });
      expect(apiClient.get).toHaveBeenCalledWith(
        '/api/v1/analytics/channels?from=2026-08-01&to=2026-08-30'
      );
      expect(result.totalAttempts).toBe(20);
    });
  });

  describe('getAttemptAnalytics', () => {
    it('requests /api/v1/analytics/attempts endpoint', async () => {
      const mockAttempts: AttemptAnalytics = {
        from: '2026-08-01',
        to: '2026-08-30',
        totalAttempts: 15,
        successfulAttempts: 10,
        failedAttempts: 5,
        scheduledAttempts: 0,
        inFlightAttempts: 0,
        sentAttempts: 15,
        deliveredAttempts: 14,
        clickedAttempts: 12,
        skippedAttempts: 0,
        successRate: 66.7,
        averageAttemptsPerRecoveryCase: 1.5,
        attemptsByStatus: {
          SCHEDULED: 0,
          IN_FLIGHT: 0,
          SENT: 15,
          DELIVERED: 14,
          CLICKED: 12,
          SUCCESS: 10,
          FAILED: 5,
          SKIPPED: 0,
        },
        attemptsByChannel: {
          WHATSAPP: 10,
          EMAIL: 5,
          SMS: 0,
          RETRY_CHARGE: 0,
          SMART_LINK: 0,
          MANUAL: 0,
        },
      };
      vi.mocked(apiClient.get).mockResolvedValueOnce(mockAttempts);

      const result = await getAttemptAnalytics({ from: '2026-08-01', to: '2026-08-30' });
      expect(apiClient.get).toHaveBeenCalledWith(
        '/api/v1/analytics/attempts?from=2026-08-01&to=2026-08-30'
      );
      expect(result.successfulAttempts).toBe(10);
    });
  });
});
