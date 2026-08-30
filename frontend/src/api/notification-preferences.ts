import { apiClient } from './client';
import type {
  NotificationPreferenceResponseDto,
  NotificationPreferenceUpdateRequestDto,
} from '../types/notifications';

/**
 * Fetch the current merchant's notification channel preferences and webhook configuration.
 */
export async function getNotificationPreferences(): Promise<NotificationPreferenceResponseDto> {
  return apiClient.get<NotificationPreferenceResponseDto>('/api/v1/notification-preferences');
}

/**
 * Update the current merchant's notification channel preferences and webhook configuration.
 */
export async function updateNotificationPreferences(
  payload: NotificationPreferenceUpdateRequestDto
): Promise<NotificationPreferenceResponseDto> {
  return apiClient.put<NotificationPreferenceResponseDto>(
    '/api/v1/notification-preferences',
    payload
  );
}
