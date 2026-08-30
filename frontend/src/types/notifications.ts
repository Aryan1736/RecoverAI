export type MerchantNotificationEvent =
  | 'PAYMENT_RECOVERED'
  | 'CASE_EXHAUSTED'
  | 'HIGH_PRIORITY_FAILURE'
  | 'PROVIDER_DEGRADED';

export type NotificationStatus = 'UNREAD' | 'READ' | 'ARCHIVED';

export type MerchantNotificationChannel = 'EMAIL' | 'WEBHOOK' | 'IN_APP';

export type NotificationDeliveryStatus =
  | 'PENDING'
  | 'SENT'
  | 'DELIVERED'
  | 'FAILED'
  | 'RETRYING'
  | 'SKIPPED';

export interface NotificationDeliveryResponseDto {
  id: string;
  channel: MerchantNotificationChannel;
  provider: string;
  status: NotificationDeliveryStatus;
  attemptedAt: string | null;
  deliveredAt: string | null;
  errorCode: string | null;
  errorMessage: string | null;
  retryCount: number;
}

export interface NotificationResponseDto {
  id: string;
  merchantId: string;
  eventType: MerchantNotificationEvent;
  title: string;
  message: string;
  status: NotificationStatus;
  read: boolean;
  recoveryCaseId?: string | null;
  recoveryAttemptId?: string | null;
  metadata?: string | null;
  createdAt: string;
  updatedAt: string;
  deliveries: NotificationDeliveryResponseDto[];
}

export interface NotificationPreferenceResponseDto {
  merchantId: string;
  webhookUrl: string | null;
  preferences: Record<MerchantNotificationEvent, Record<MerchantNotificationChannel, boolean>>;
}

export interface NotificationPreferenceUpdateRequestDto {
  webhookUrl?: string | null;
  preferences: Record<MerchantNotificationEvent, Record<MerchantNotificationChannel, boolean>>;
}

export interface NotificationFilterParams {
  unreadOnly?: boolean;
  event?: MerchantNotificationEvent;
  page?: number;
  size?: number;
}

export interface NotificationPageResponse {
  content: NotificationResponseDto[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface MarkAllReadResponse {
  markedReadCount: number;
  success: boolean;
}
