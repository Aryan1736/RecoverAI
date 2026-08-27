export const API_BASE_URL: string =
  import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export interface HealthCheckResponse {
  status: string;
  service: string;
}

export async function fetchBackendHealth(): Promise<HealthCheckResponse> {
  const response = await fetch(`${API_BASE_URL}/api/v1/health`);
  if (!response.ok) {
    throw new Error(`Health check failed with status: ${response.status}`);
  }
  return response.json();
}
