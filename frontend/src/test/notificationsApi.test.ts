import { describe, it, expect, vi, beforeEach } from 'vitest';
import {
  getNotifications,
  getNotification,
  markAsRead,
  markAllAsRead,
  getUnreadCount,
} from '../api/notifications';
import { apiClient } from '../api/client';
import type {
  NotificationPageResponse,
  NotificationResponseDto,
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

describe('Notifications API Client', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('getNotifications', () => {
    it('requests /api/v1/notifications without query parameters when omitted', async () => {
      const mockPage: NotificationPageResponse = {
        content: [],
        totalElements: 0,
        totalPages: 0,
        number: 0,
        size: 20,
        first: true,
        last: true,
        empty: true,
      };
      vi.mocked(apiClient.get).mockResolvedValueOnce(mockPage);

      const result = await getNotifications();
      expect(apiClient.get).toHaveBeenCalledWith('/api/v1/notifications');
      expect(result).toEqual(mockPage);
    });

    it('correctly formats unreadOnly, event, page, and size query parameters', async () => {
      vi.mocked(apiClient.get).mockResolvedValueOnce({} as NotificationPageResponse);

      await getNotifications({
        unreadOnly: true,
        event: 'PAYMENT_RECOVERED',
        page: 1,
        size: 15,
      });

      expect(apiClient.get).toHaveBeenCalledWith(
        '/api/v1/notifications?unreadOnly=true&event=PAYMENT_RECOVERED&page=1&size=15'
      );
    });
  });

  describe('getNotification', () => {
    it('requests /api/v1/notifications/:id with URL encoding', async () => {
      const mockNotification: Partial<NotificationResponseDto> = {
        id: 'notif-123',
        title: 'Recovery Succeeded',
      };
      vi.mocked(apiClient.get).mockResolvedValueOnce(mockNotification as NotificationResponseDto);

      const result = await getNotification('notif-123');
      expect(apiClient.get).toHaveBeenCalledWith('/api/v1/notifications/notif-123');
      expect(result.id).toBe('notif-123');
    });
  });

  describe('markAsRead', () => {
    it('sends PATCH request to /api/v1/notifications/:id/read', async () => {
      const mockUpdated: Partial<NotificationResponseDto> = {
        id: 'notif-123',
        read: true,
        status: 'READ',
      };
      vi.mocked(apiClient.patch).mockResolvedValueOnce(mockUpdated as NotificationResponseDto);

      const result = await markAsRead('notif-123');
      expect(apiClient.patch).toHaveBeenCalledWith('/api/v1/notifications/notif-123/read');
      expect(result.read).toBe(true);
    });
  });

  describe('markAllAsRead', () => {
    it('sends PATCH request to /api/v1/notifications/read-all', async () => {
      vi.mocked(apiClient.patch).mockResolvedValueOnce({ markedReadCount: 5, success: true });

      const result = await markAllAsRead();
      expect(apiClient.patch).toHaveBeenCalledWith('/api/v1/notifications/read-all');
      expect(result.markedReadCount).toBe(5);
    });
  });

  describe('getUnreadCount', () => {
    it('extracts totalElements from unread query response', async () => {
      vi.mocked(apiClient.get).mockResolvedValueOnce({
        content: [],
        totalElements: 7,
      } as unknown as NotificationPageResponse);

      const count = await getUnreadCount();
      expect(apiClient.get).toHaveBeenCalledWith('/api/v1/notifications?unreadOnly=true&size=1');
      expect(count).toBe(7);
    });

    it('returns 0 when request encounters an error', async () => {
      vi.mocked(apiClient.get).mockRejectedValueOnce(new Error('Network error'));

      const count = await getUnreadCount();
      expect(count).toBe(0);
    });
  });
});
