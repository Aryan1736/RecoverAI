import { apiClient } from './client';
import type {
  AnalyticsOverview,
  RecoveryTrends,
  FailureAnalytics,
  ChannelAnalytics,
  AttemptAnalytics,
  DateRangeParams,
} from '../types/analytics';

function buildDateRangeQuery(range?: DateRangeParams): string {
  if (!range) return '';
  const params = new URLSearchParams();
  if (range.from) {
    params.set('from', range.from);
  }
  if (range.to) {
    params.set('to', range.to);
  }
  const queryString = params.toString();
  return queryString ? `?${queryString}` : '';
}

export async function getAnalyticsOverview(range?: DateRangeParams): Promise<AnalyticsOverview> {
  const query = buildDateRangeQuery(range);
  return apiClient.get<AnalyticsOverview>(`/api/v1/analytics/overview${query}`);
}

export async function getRecoveryTrends(range?: DateRangeParams): Promise<RecoveryTrends> {
  const query = buildDateRangeQuery(range);
  return apiClient.get<RecoveryTrends>(`/api/v1/analytics/recovery-trends${query}`);
}

export async function getFailureAnalytics(range?: DateRangeParams): Promise<FailureAnalytics> {
  const query = buildDateRangeQuery(range);
  return apiClient.get<FailureAnalytics>(`/api/v1/analytics/failures${query}`);
}

export async function getChannelAnalytics(range?: DateRangeParams): Promise<ChannelAnalytics> {
  const query = buildDateRangeQuery(range);
  return apiClient.get<ChannelAnalytics>(`/api/v1/analytics/channels${query}`);
}

export async function getAttemptAnalytics(range?: DateRangeParams): Promise<AttemptAnalytics> {
  const query = buildDateRangeQuery(range);
  return apiClient.get<AttemptAnalytics>(`/api/v1/analytics/attempts${query}`);
}
