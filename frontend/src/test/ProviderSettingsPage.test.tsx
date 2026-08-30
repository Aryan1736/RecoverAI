import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { ProviderSettingsPage } from '../pages/settings/ProviderSettingsPage';
import * as providersApi from '../api/providers';
import type { ProviderHealthSummary } from '../types/providers';

vi.mock('../api/providers');

const mockHealthSummary: ProviderHealthSummary = {
  overallStatus: 'HEALTHY',
  lastChecked: '2026-08-30T12:00:00Z',
  providers: [
    {
      id: 'WHATSAPP_TWILIO',
      name: 'Twilio',
      channel: 'WhatsApp',
      status: 'HEALTHY',
      message: 'Twilio Sandbox connected',
      lastChecked: '2026-08-30T12:00:00Z',
    },
    {
      id: 'EMAIL_SENDGRID',
      name: 'SendGrid',
      channel: 'Email',
      status: 'DEGRADED',
      message: 'SMTP dispatch queue delay',
      lastChecked: '2026-08-30T12:00:00Z',
    },
    {
      id: 'SMS_TWILIO',
      name: 'Twilio',
      channel: 'SMS',
      status: 'UNAVAILABLE',
      message: 'Rate limit threshold exceeded',
      lastChecked: '2026-08-30T12:00:00Z',
    },
    {
      id: 'PAYMENT_RAZORPAY',
      name: 'Razorpay',
      channel: 'Payment Retry',
      status: 'HEALTHY',
      message: 'Webhook and charge APIs operational',
      lastChecked: '2026-08-30T12:00:00Z',
    },
  ],
};

describe('ProviderSettingsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(providersApi.getProviderHealth).mockResolvedValue(mockHealthSummary);
  });

  it('renders all provider cards with channels, statuses, and messages', async () => {
    render(
      <MemoryRouter>
        <ProviderSettingsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Upstream Provider Operational Status')).toBeInTheDocument();
      expect(screen.getByText('Twilio Sandbox connected')).toBeInTheDocument();
      expect(screen.getByText('SMTP dispatch queue delay')).toBeInTheDocument();
      expect(screen.getByText('Rate limit threshold exceeded')).toBeInTheDocument();
      expect(screen.getByText('Webhook and charge APIs operational')).toBeInTheDocument();
      expect(screen.getByText('Payment Retry')).toBeInTheDocument();
    });
  });

  it('verifies zero secret exposure: no tokens, passwords or secrets rendered', async () => {
    render(
      <MemoryRouter>
        <ProviderSettingsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Zero Secret Exposure Policy')).toBeInTheDocument();
    });

    const bodyText = document.body.textContent || '';
    expect(bodyText).not.toContain('sk_live_');
    expect(bodyText).not.toContain('super_secret');
    expect(bodyText).not.toContain('whsec_');
    expect(bodyText).not.toContain('Bearer eyJ');
  });

  it('refreshes provider health telemetry on button click', async () => {
    render(
      <MemoryRouter>
        <ProviderSettingsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Twilio Sandbox connected')).toBeInTheDocument();
    });

    const refreshButton = screen.getByRole('button', { name: /Check Status/i });
    fireEvent.click(refreshButton);

    await waitFor(() => {
      expect(providersApi.getProviderHealth).toHaveBeenCalledTimes(2);
    });
  });
});
