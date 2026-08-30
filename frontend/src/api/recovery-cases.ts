import { apiClient } from './client';
import type {
  RecoveryCase,
  RecoveryCaseDetail,
  RecoveryAttempt,
  PageResponse,
  RecoveryCaseListParams,
} from '../types/recovery-case';

function buildRecoveryCaseListQuery(params?: RecoveryCaseListParams): string {
  if (!params) return '';
  const searchParams = new URLSearchParams();

  if (params.status) {
    searchParams.set('status', params.status);
  }
  if (params.priority) {
    searchParams.set('priority', params.priority);
  }
  if (params.failureReasonCategory && params.failureReasonCategory.trim() !== '') {
    searchParams.set('failureReasonCategory', params.failureReasonCategory.trim());
  }
  if (params.page !== undefined && params.page !== null) {
    searchParams.set('page', params.page.toString());
  }
  if (params.size !== undefined && params.size !== null) {
    searchParams.set('size', params.size.toString());
  }
  if (params.sort) {
    searchParams.set('sort', params.sort);
  }

  const queryString = searchParams.toString();
  return queryString ? `?${queryString}` : '';
}

export async function getRecoveryCases(
  params?: RecoveryCaseListParams
): Promise<PageResponse<RecoveryCase>> {
  const query = buildRecoveryCaseListQuery(params);
  return apiClient.get<PageResponse<RecoveryCase>>(`/api/v1/recovery-cases${query}`);
}

export async function getRecoveryCase(id: string): Promise<RecoveryCaseDetail> {
  const normalizedId = encodeURIComponent(id.trim());
  return apiClient.get<RecoveryCaseDetail>(`/api/v1/recovery-cases/${normalizedId}`);
}

export async function getRecoveryCaseAttempts(id: string): Promise<RecoveryAttempt[]> {
  const normalizedId = encodeURIComponent(id.trim());
  return apiClient.get<RecoveryAttempt[]>(`/api/v1/recovery-cases/${normalizedId}/attempts`);
}

export async function cancelRecoveryCase(id: string): Promise<RecoveryCase> {
  const normalizedId = encodeURIComponent(id.trim());
  return apiClient.patch<RecoveryCase>(`/api/v1/recovery-cases/${normalizedId}/cancel`);
}
