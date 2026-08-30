import { apiClient } from './client';
import type { AuthResponse, LoginRequest, Merchant, RegisterRequest } from '../types/auth';

export async function login(credentials: LoginRequest): Promise<AuthResponse> {
  return apiClient.post<AuthResponse>('/api/v1/auth/login', credentials, { skipAuth: true });
}

export async function register(payload: RegisterRequest): Promise<Merchant> {
  return apiClient.post<Merchant>('/api/v1/auth/register', payload, { skipAuth: true });
}

export async function fetchHealth(): Promise<{ status: string; service: string }> {
  return apiClient.get<{ status: string; service: string }>('/api/v1/health', { skipAuth: true });
}
