import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import {
  apiClient,
  getStoredToken,
  setStoredToken,
  getStoredMerchant,
  setStoredMerchant,
  clearStoredAuth,
  registerUnauthorizedHandler,
  TOKEN_STORAGE_KEY,
} from '../api/client';
import { ApiError } from '../types/api';
import type { Merchant } from '../types/auth';

const mockMerchant: Merchant = {
  id: '7bf08f22-b14f-42a6-88a5-7abaafe92c26',
  name: 'Acme Corp',
  email: 'acme@example.com',
  status: 'ACTIVE',
  createdAt: '2026-08-30T10:00:00Z',
  updatedAt: '2026-08-30T10:00:00Z',
};

describe('API Client & Storage', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('Storage Management', () => {
    it('stores, retrieves, and clears JWT token', () => {
      expect(getStoredToken()).toBeNull();
      setStoredToken('test-jwt-token');
      expect(getStoredToken()).toBe('test-jwt-token');
      expect(localStorage.getItem(TOKEN_STORAGE_KEY)).toBe('test-jwt-token');

      clearStoredAuth();
      expect(getStoredToken()).toBeNull();
      expect(localStorage.getItem(TOKEN_STORAGE_KEY)).toBeNull();
    });

    it('stores, retrieves, and clears merchant profile', () => {
      expect(getStoredMerchant()).toBeNull();
      setStoredMerchant(mockMerchant);

      const retrieved = getStoredMerchant();
      expect(retrieved).not.toBeNull();
      expect(retrieved?.email).toBe('acme@example.com');
      expect(retrieved?.id).toBe(mockMerchant.id);

      clearStoredAuth();
      expect(getStoredMerchant()).toBeNull();
    });
  });

  describe('HTTP Request Interception', () => {
    it('attaches Authorization: Bearer token when token is stored', async () => {
      setStoredToken('mock-access-token');

      const mockFetch = vi.fn().mockResolvedValue({
        ok: true,
        status: 200,
        headers: new Headers({ 'content-type': 'application/json' }),
        json: async () => ({ status: 'UP', service: 'RecoverAI' }),
      });
      globalThis.fetch = mockFetch as unknown as typeof fetch;

      const result = await apiClient.get<{ status: string }>('/api/v1/health');
      expect(result.status).toBe('UP');

      expect(mockFetch).toHaveBeenCalledOnce();
      const calledHeaders = mockFetch.mock.calls[0][1]?.headers as Headers;
      expect(calledHeaders.get('Authorization')).toBe('Bearer mock-access-token');
    });

    it('does not attach Authorization header when skipAuth is true', async () => {
      setStoredToken('mock-access-token');

      const mockFetch = vi.fn().mockResolvedValue({
        ok: true,
        status: 200,
        headers: new Headers({ 'content-type': 'application/json' }),
        json: async () => ({ success: true }),
      });
      globalThis.fetch = mockFetch as unknown as typeof fetch;

      await apiClient.get('/api/v1/health', { skipAuth: true });

      const calledHeaders = mockFetch.mock.calls[0][1]?.headers as Headers;
      expect(calledHeaders.get('Authorization')).toBeNull();
    });

    it('parses backend ApiErrorResponse on 400 Bad Request', async () => {
      const errorPayload = {
        status: 400,
        error: 'Validation Error',
        message: 'email: Merchant email must be valid',
        timestamp: '2026-08-30T12:00:00Z',
      };

      const mockFetch = vi.fn().mockResolvedValue({
        ok: false,
        status: 400,
        statusText: 'Bad Request',
        headers: new Headers({ 'content-type': 'application/json' }),
        json: async () => errorPayload,
      });
      globalThis.fetch = mockFetch as unknown as typeof fetch;

      await expect(apiClient.get('/test')).rejects.toThrow(ApiError);
      try {
        await apiClient.get('/test');
      } catch (err) {
        expect(err).toBeInstanceOf(ApiError);
        const apiErr = err as ApiError;
        expect(apiErr.status).toBe(400);
        expect(apiErr.errorTitle).toBe('Validation Error');
        expect(apiErr.originalMessage).toBe('email: Merchant email must be valid');
      }
    });

    it('triggers unauthorized handler and clears auth on 401 response', async () => {
      setStoredToken('expired-jwt');
      setStoredMerchant(mockMerchant);

      const onUnauthorized = vi.fn();
      const unregister = registerUnauthorizedHandler(onUnauthorized);

      const mockFetch = vi.fn().mockResolvedValue({
        ok: false,
        status: 401,
        statusText: 'Unauthorized',
        headers: new Headers({ 'content-type': 'application/json' }),
        json: async () => ({
          status: 401,
          error: 'Invalid Credentials',
          message: 'Token expired',
        }),
      });
      globalThis.fetch = mockFetch as unknown as typeof fetch;

      await expect(apiClient.get('/api/v1/dashboard/summary')).rejects.toThrow(ApiError);

      expect(onUnauthorized).toHaveBeenCalledWith('Your session has expired. Please sign in again.');
      expect(getStoredToken()).toBeNull();
      expect(getStoredMerchant()).toBeNull();

      unregister();
    });

    it('safely handles network disconnection errors', async () => {
      const mockFetch = vi.fn().mockRejectedValue(new Error('Failed to fetch'));
      globalThis.fetch = mockFetch as unknown as typeof fetch;

      await expect(apiClient.get('/test')).rejects.toThrow(ApiError);
      try {
        await apiClient.get('/test');
      } catch (err) {
        expect(err).toBeInstanceOf(ApiError);
        const apiErr = err as ApiError;
        expect(apiErr.status).toBe(0);
        expect(apiErr.errorTitle).toBe('Network Error');
        expect(apiErr.originalMessage).toContain('Failed to fetch');
      }
    });
  });
});
