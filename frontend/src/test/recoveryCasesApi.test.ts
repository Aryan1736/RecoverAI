import { describe, it, expect, vi, beforeEach } from 'vitest';
import {
  getRecoveryCases,
  getRecoveryCase,
  getRecoveryCaseAttempts,
  cancelRecoveryCase,
} from '../api/recovery-cases';
import { apiClient } from '../api/client';
import type {
  RecoveryCase,
  RecoveryCaseDetail,
  RecoveryAttempt,
  PageResponse,
} from '../types/recovery-case';

vi.mock('../api/client', () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('Recovery Cases API Client', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('getRecoveryCases', () => {
    it('requests /api/v1/recovery-cases without parameters when omitted', async () => {
      const mockPage: PageResponse<RecoveryCase> = {
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

      const result = await getRecoveryCases();
      expect(apiClient.get).toHaveBeenCalledWith('/api/v1/recovery-cases');
      expect(result).toEqual(mockPage);
    });

    it('correctly serializes status, priority, category, pagination and sort', async () => {
      vi.mocked(apiClient.get).mockResolvedValueOnce({} as PageResponse<RecoveryCase>);

      await getRecoveryCases({
        status: 'IN_PROGRESS',
        priority: 'CRITICAL',
        failureReasonCategory: 'INSUFFICIENT_FUNDS',
        page: 2,
        size: 25,
        sort: 'createdAt,desc',
      });

      expect(apiClient.get).toHaveBeenCalledWith(
        '/api/v1/recovery-cases?status=IN_PROGRESS&priority=CRITICAL&failureReasonCategory=INSUFFICIENT_FUNDS&page=2&size=25&sort=createdAt%2Cdesc'
      );
    });
  });

  describe('getRecoveryCase', () => {
    it('requests /api/v1/recovery-cases/:id with URL encoding', async () => {
      const mockDetail: Partial<RecoveryCaseDetail> = {
        id: 'case-123',
        status: 'OPEN',
        priority: 'HIGH',
      };
      vi.mocked(apiClient.get).mockResolvedValueOnce(mockDetail as RecoveryCaseDetail);

      const result = await getRecoveryCase('case-123');
      expect(apiClient.get).toHaveBeenCalledWith('/api/v1/recovery-cases/case-123');
      expect(result.id).toBe('case-123');
    });
  });

  describe('getRecoveryCaseAttempts', () => {
    it('requests /api/v1/recovery-cases/:id/attempts', async () => {
      const mockAttempts: Partial<RecoveryAttempt>[] = [
        { id: 'att-1', attemptNumber: 1, channel: 'WHATSAPP', status: 'SENT' },
      ];
      vi.mocked(apiClient.get).mockResolvedValueOnce(mockAttempts as RecoveryAttempt[]);

      const result = await getRecoveryCaseAttempts('case-123');
      expect(apiClient.get).toHaveBeenCalledWith('/api/v1/recovery-cases/case-123/attempts');
      expect(result).toHaveLength(1);
    });
  });

  describe('cancelRecoveryCase', () => {
    it('sends PATCH request to /api/v1/recovery-cases/:id/cancel', async () => {
      const mockCancelled: Partial<RecoveryCase> = {
        id: 'case-123',
        status: 'CANCELLED',
      };
      vi.mocked(apiClient.patch).mockResolvedValueOnce(mockCancelled as RecoveryCase);

      const result = await cancelRecoveryCase('case-123');
      expect(apiClient.patch).toHaveBeenCalledWith('/api/v1/recovery-cases/case-123/cancel');
      expect(result.status).toBe('CANCELLED');
    });
  });
});
