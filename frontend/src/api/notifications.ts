import { apiClient } from './client';
import type {
  NotificationPageResponse,
  NotificationResponseDto,
  NotificationFilterParams,
  MarkAllReadResponse,
} from '../types/notifications';

/**
 * Fetch paginated notifications with optional unread and event filters.
 */
export async function getNotifications(
  params: NotificationFilterParams = {}
): Promise<NotificationPageResponse> {
  const queryParams = new URLSearchParams();

  if (params.unreadOnly !== undefined) {
    queryParams.set('unreadOnly', String(params.unreadOnly));
  }
  if (params.event) {
    queryParams.set('event', params.event);
  }
  if (params.page !== undefined) {
    queryParams.set('page', String(params.page));
  }
  if (params.size !== undefined) {
    queryParams.set('size', String(params.size));
  }

  const queryString = queryParams.toString();
  const path = `/api/v1/notifications${queryString ? `?${queryString}` : ''}`;
  return apiClient.get<NotificationPageResponse>(path);
}

/**
 * Fetch a single notification by UUID.
 */
export async function getNotification(id: string): Promise<NotificationResponseDto> {
  return apiClient.get<NotificationResponseDto>(`/api/v1/notifications/${encodeURIComponent(id)}`);
}

/**
 * Mark a single notification as read.
 */
export async function markAsRead(id: string): Promise<NotificationResponseDto> {
  return apiClient.patch<NotificationResponseDto>(`/api/v1/notifications/${encodeURIComponent(id)}/read`);
}

/**
 * Mark all notifications for the authenticated merchant as read.
 */
export async function markAllAsRead(): Promise<MarkAllReadResponse> {
  return apiClient.patch<MarkAllReadResponse>('/api/v1/notifications/read-all');
}

/**
 * Efficiently retrieve the total number of unread notifications for the merchant.
 * Uses page size 1 to fetch pagination metadata (totalElements) with minimal payload.
 */
export async function getUnreadCount(): Promise<number> {
  try {
    const page = await getNotifications({ unreadOnly: true, size: 1 });
    return page.totalElements ?? 0;
  } catch {
    return 0;
  }
}
