import { apiClient } from './client';
import type {
  ProviderHealthState,
  ProviderStatusItem,
  ProviderHealthSummary,
  ActuatorAggregatedHealthResponse,
  ActuatorHealthIndicatorResponse,
} from '../types/providers';

function mapActuatorStatus(status?: string): ProviderHealthState {
  if (!status) return 'UNKNOWN';
  const s = status.toUpperCase();
  if (s === 'UP' || s === 'AVAILABLE') return 'HEALTHY';
  if (s === 'DEGRADED') return 'DEGRADED';
  if (s === 'DOWN' || s === 'UNAVAILABLE' || s === 'MISCONFIGURED') return 'UNAVAILABLE';
  if (s === 'DISABLED') return 'DISABLED';
  return 'UNKNOWN';
}

function formatChannelName(rawCategory: string): string {
  switch (rawCategory.toUpperCase()) {
    case 'WHATSAPP':
      return 'WhatsApp';
    case 'EMAIL':
      return 'Email';
    case 'SMS':
      return 'SMS';
    case 'PAYMENT':
      return 'Payment Retry';
    default:
      return rawCategory.charAt(0).toUpperCase() + rawCategory.slice(1).toLowerCase();
  }
}

function formatProviderName(rawProvider: string): string {
  switch (rawProvider.toUpperCase()) {
    case 'TWILIO':
      return 'Twilio';
    case 'SENDGRID':
      return 'SendGrid';
    case 'SMTP':
      return 'SMTP Mailer';
    case 'META':
      return 'Meta Cloud API';
    case 'RAZORPAY':
      return 'Razorpay';
    case 'MOCK':
      return 'Mock Sandbox';
    default:
      return rawProvider.charAt(0).toUpperCase() + rawProvider.slice(1).toLowerCase();
  }
}

/**
 * Fetch external communication and payment provider operational health status.
 * Reuses the backend Actuator ProviderHealthIndicator.
 */
export async function getProviderHealth(): Promise<ProviderHealthSummary> {
  const timestamp = new Date().toISOString();

  let details: { components?: Record<string, string>; messages?: Record<string, string> } | undefined;
  let overallRawStatus = 'UNKNOWN';

  try {
    // Attempt dedicated health indicator endpoint first
    const indResponse = await apiClient.get<ActuatorHealthIndicatorResponse>(
      '/actuator/health/providerHealthIndicator',
      { skipAuth: true }
    );
    if (indResponse && (indResponse.details || indResponse.status)) {
      overallRawStatus = indResponse.status || 'UNKNOWN';
      details = indResponse.details;
    }
  } catch {
    try {
      // Fallback to aggregated actuator health endpoint
      const aggResponse = await apiClient.get<ActuatorAggregatedHealthResponse>(
        '/actuator/health',
        { skipAuth: true }
      );
      if (aggResponse?.components?.providerHealthIndicator) {
        const phi = aggResponse.components.providerHealthIndicator;
        overallRawStatus = phi.status || aggResponse.status || 'UNKNOWN';
        details = phi.details;
      } else if (aggResponse?.details) {
        overallRawStatus = aggResponse.status || 'UNKNOWN';
        details = aggResponse.details;
      }
    } catch {
      // Return UNKNOWN summary on error
      return {
        overallStatus: 'UNKNOWN',
        providers: [],
        lastChecked: timestamp,
      };
    }
  }

  const components = details?.components || {};
  const messages = details?.messages || {};

  const providers: ProviderStatusItem[] = Object.entries(components).map(([key, rawStatus]) => {
    // Key format: CATEGORY_PROVIDER (e.g. WHATSAPP_TWILIO)
    const parts = key.split('_');
    const category = parts[0] || 'GENERAL';
    const provider = parts.slice(1).join('_') || 'PROVIDER';

    return {
      id: key,
      name: formatProviderName(provider),
      channel: formatChannelName(category),
      status: mapActuatorStatus(rawStatus),
      message: messages[key] || undefined,
      lastChecked: timestamp,
    };
  });

  return {
    overallStatus: mapActuatorStatus(overallRawStatus),
    providers,
    lastChecked: timestamp,
  };
}
