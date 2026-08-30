import { ApiError, type ApiErrorResponse } from '../types/api';
import type { Merchant } from '../types/auth';

export const TOKEN_STORAGE_KEY = 'recoverai_token';
export const MERCHANT_STORAGE_KEY = 'recoverai_merchant';

export const API_BASE_URL: string =
  (import.meta.env.VITE_API_BASE_URL as string | undefined)?.replace(/\/+$/, '') || 'http://localhost:8080';

type UnauthorizedHandler = (message: string) => void;
let unauthorizedHandler: UnauthorizedHandler | null = null;

export function registerUnauthorizedHandler(handler: UnauthorizedHandler): () => void {
  unauthorizedHandler = handler;
  return () => {
    if (unauthorizedHandler === handler) {
      unauthorizedHandler = null;
    }
  };
}

export function getStoredToken(): string | null {
  try {
    return localStorage.getItem(TOKEN_STORAGE_KEY);
  } catch {
    return null;
  }
}

export function setStoredToken(token: string): void {
  try {
    localStorage.setItem(TOKEN_STORAGE_KEY, token);
  } catch {
    // LocalStorage write error handling
  }
}

export function getStoredMerchant(): Merchant | null {
  try {
    const raw = localStorage.getItem(MERCHANT_STORAGE_KEY);
    if (!raw) return null;
    return JSON.parse(raw) as Merchant;
  } catch {
    return null;
  }
}

export function setStoredMerchant(merchant: Merchant): void {
  try {
    localStorage.setItem(MERCHANT_STORAGE_KEY, JSON.stringify(merchant));
  } catch {
    // LocalStorage write error handling
  }
}

export function clearStoredAuth(): void {
  try {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    localStorage.removeItem(MERCHANT_STORAGE_KEY);
  } catch {
    // LocalStorage clear error handling
  }
}

export interface RequestOptions extends RequestInit {
  skipAuth?: boolean;
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  const url = `${API_BASE_URL}${normalizedPath}`;

  const headers = new Headers(options.headers || {});
  if (!headers.has('Content-Type') && options.body && typeof options.body === 'string') {
    headers.set('Content-Type', 'application/json');
  }
  if (!headers.has('Accept')) {
    headers.set('Accept', 'application/json');
  }

  if (!options.skipAuth) {
    const token = getStoredToken();
    if (token && !headers.has('Authorization')) {
      headers.set('Authorization', `Bearer ${token}`);
    }
  }

  let response: Response;
  try {
    response = await fetch(url, {
      ...options,
      headers,
    });
  } catch (err: unknown) {
    const detail = err instanceof Error ? err.message : 'Network request failed';
    throw new ApiError(0, 'Network Error', `Unable to connect to RecoverAI (${detail})`);
  }

  if (!response.ok) {
    let errorData: ApiErrorResponse | null = null;
    try {
      const parsed = await response.json();
      if (parsed && typeof parsed === 'object') {
        errorData = parsed as ApiErrorResponse;
      }
    } catch {
      // Non-JSON error response
    }

    const status = response.status;
    const title = errorData?.error || `HTTP ${status}`;
    const message = errorData?.message || response.statusText || 'Request failed';

    // Trigger unauthorized callback on 401 (excluding public login/register routes)
    if (status === 401 && !path.includes('/auth/login') && !path.includes('/auth/register')) {
      clearStoredAuth();
      if (unauthorizedHandler) {
        unauthorizedHandler('Your session has expired. Please sign in again.');
      }
    }

    throw new ApiError(status, title, message);
  }

  if (response.status === 204) {
    return null as T;
  }

  const contentType = response.headers.get('content-type');
  if (contentType && contentType.includes('application/json')) {
    return (await response.json()) as T;
  }

  return (await response.text()) as unknown as T;
}

export const apiClient = {
  get: <T>(path: string, options?: RequestOptions) =>
    request<T>(path, { ...options, method: 'GET' }),
  post: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>(path, {
      ...options,
      method: 'POST',
      body: body !== undefined ? JSON.stringify(body) : undefined,
    }),
  put: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>(path, {
      ...options,
      method: 'PUT',
      body: body !== undefined ? JSON.stringify(body) : undefined,
    }),
  delete: <T>(path: string, options?: RequestOptions) =>
    request<T>(path, { ...options, method: 'DELETE' }),
};
