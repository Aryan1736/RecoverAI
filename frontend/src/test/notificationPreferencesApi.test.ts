import { describe, it, expect, vi, beforeEach } from 'vitest';
import {
  getNotificationPreferences,
  updateNotificationPreferences,
} from '../api/notification-preferences';
import { apiClient } from '../api/client';
import type {
  NotificationPreferenceResponseDto,
  NotificationPreferenceUpdateRequestDto,
} from '../types/notifications';

vi.mock('../api/client', () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('Notification Preferences API Client', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('fetches preferences from /api/v1/notification-preferences', async () => {
    const mockPreferences: NotificationPreferenceResponseDto = {
      merchantId: 'm-123',
      webhookUrl: 'https://merchant.example.com/webhook',
      preferences: {
        PAYMENT_RECOVERED: { EMAIL: true, WEBHOOK: true, IN_APP: true },
        CASE_EXHAUSTED: { EMAIL: true, WEBHOOK: false, IN_APP: true },
        HIGH_PRIORITY_FAILURE: { EMAIL: true, WEBHOOK: true, IN_APP: true },
        PROVIDER_DEGRADED: { EMAIL: false, WEBHOOK: false, IN_APP: true },
      },
    };

    vi.mocked(apiClient.get).mockResolvedValueOnce(mockPreferences);

    const result = await getNotificationPreferences();
    expect(apiClient.get).toHaveBeenCalledWith('/api/v1/notification-preferences');
    expect(result).toEqual(mockPreferences);
  });

  it('updates preferences via PUT to /api/v1/notification-preferences', async () => {
    const updatePayload: NotificationPreferenceUpdateRequestDto = {
      webhookUrl: 'https://api.domain.com/v1/hook',
      preferences: {
        PAYMENT_RECOVERED: { EMAIL: true, WEBHOOK: true, IN_APP: true },
        CASE_EXHAUSTED: { EMAIL: false, WEBHOOK: false, IN_APP: true },
        HIGH_PRIORITY_FAILURE: { EMAIL: true, WEBHOOK: true, IN_APP: true },
        PROVIDER_DEGRADED: { EMAIL: true, WEBHOOK: false, IN_APP: true },
      },
    };

    const mockResponse: NotificationPreferenceResponseDto = {
      merchantId: 'm-123',
      webhookUrl: updatePayload.webhookUrl || null,
      preferences: updatePayload.preferences,
    };

    vi.mocked(apiClient.put).mockResolvedValueOnce(mockResponse);

    const result = await updateNotificationPreferences(updatePayload);
    expect(apiClient.put).toHaveBeenCalledWith(
      '/api/v1/notification-preferences',
      updatePayload
    );
    expect(result).toEqual(mockResponse);
  });
});
