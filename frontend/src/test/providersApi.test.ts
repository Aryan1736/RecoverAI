import { describe, it, expect, vi, beforeEach } from 'vitest';
import { getProviderHealth } from '../api/providers';
import { apiClient } from '../api/client';
import type {
  ActuatorHealthIndicatorResponse,
  ActuatorAggregatedHealthResponse,
} from '../types/providers';

vi.mock('../api/client', () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('Providers API Client', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('queries dedicated indicator endpoint and maps statuses accurately', async () => {
    const mockActuatorResponse: ActuatorHealthIndicatorResponse = {
      status: 'UP',
      details: {
        components: {
          WHATSAPP_TWILIO: 'UP',
          EMAIL_SENDGRID: 'DEGRADED',
          SMS_TWILIO: 'DOWN',
          PAYMENT_RAZORPAY: 'UP',
        },
        messages: {
          EMAIL_SENDGRID: 'Elevated latency observed',
          SMS_TWILIO: 'Gateway timeout',
        },
      },
    };

    vi.mocked(apiClient.get).mockResolvedValueOnce(mockActuatorResponse);

    const summary = await getProviderHealth();

    expect(apiClient.get).toHaveBeenCalledWith(
      '/actuator/health/providerHealthIndicator',
      { skipAuth: true }
    );
    expect(summary.overallStatus).toBe('HEALTHY');
    expect(summary.providers).toHaveLength(4);

    const twilioWhatsApp = summary.providers.find((p) => p.id === 'WHATSAPP_TWILIO');
    expect(twilioWhatsApp?.status).toBe('HEALTHY');
    expect(twilioWhatsApp?.channel).toBe('WhatsApp');
    expect(twilioWhatsApp?.name).toBe('Twilio');

    const sendgrid = summary.providers.find((p) => p.id === 'EMAIL_SENDGRID');
    expect(sendgrid?.status).toBe('DEGRADED');
    expect(sendgrid?.message).toBe('Elevated latency observed');

    const sms = summary.providers.find((p) => p.id === 'SMS_TWILIO');
    expect(sms?.status).toBe('UNAVAILABLE');
    expect(sms?.message).toBe('Gateway timeout');
  });

  it('falls back to /actuator/health when dedicated indicator returns 404 or fails', async () => {
    // Dedicated endpoint fails
    vi.mocked(apiClient.get).mockRejectedValueOnce(new Error('404 Not Found'));

    // Aggregated endpoint succeeds
    const mockAggregated: ActuatorAggregatedHealthResponse = {
      status: 'UP',
      components: {
        providerHealthIndicator: {
          status: 'UP',
          details: {
            components: {
              PAYMENT_RAZORPAY: 'UP',
            },
          },
        },
      },
    };
    vi.mocked(apiClient.get).mockResolvedValueOnce(mockAggregated);

    const summary = await getProviderHealth();

    expect(apiClient.get).toHaveBeenNthCalledWith(
      1,
      '/actuator/health/providerHealthIndicator',
      { skipAuth: true }
    );
    expect(apiClient.get).toHaveBeenNthCalledWith(2, '/actuator/health', { skipAuth: true });
    expect(summary.overallStatus).toBe('HEALTHY');
    expect(summary.providers).toHaveLength(1);
    expect(summary.providers[0].name).toBe('Razorpay');
  });

  it('returns UNKNOWN summary when all actuator endpoints fail', async () => {
    vi.mocked(apiClient.get).mockRejectedValue(new Error('Connection refused'));

    const summary = await getProviderHealth();

    expect(summary.overallStatus).toBe('UNKNOWN');
    expect(summary.providers).toEqual([]);
    expect(summary.lastChecked).toBeDefined();
  });
});
