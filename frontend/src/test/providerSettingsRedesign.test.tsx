import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { ToastProvider } from '../context/ToastContext';
import { SettingsLayout } from '../pages/settings/SettingsLayout';
import { ProviderSettingsPage } from '../pages/settings/ProviderSettingsPage';
import * as providersApi from '../api/providers';
import * as demoApi from '../api/demo';
import * as useDemoModeHook from '../hooks/useDemoMode';
import type { ProviderHealthSummary } from '../types/providers';

vi.mock('../api/providers');
vi.mock('../api/demo');
vi.mock('../hooks/useAuth', () => ({
  useAuth: () => ({
    user: {
      id: 'm-tenant-999',
      name: 'Acme Corp',
      email: 'ops@acmecorp.com',
      status: 'ACTIVE',
      razorpayAccountId: 'acc_rzp_12345',
      createdAt: '2026-08-01T00:00:00Z',
      updatedAt: '2026-08-30T00:00:00Z',
    },
    logout: vi.fn(),
  }),
}));

const mockHealthySummary: ProviderHealthSummary = {
  overallStatus: 'HEALTHY',
  lastChecked: '2026-08-30T12:00:00Z',
  providers: [
    {
      id: 'WHATSAPP_TWILIO',
      name: 'Twilio',
      channel: 'WhatsApp',
      status: 'HEALTHY',
      message: 'Twilio Sandbox connected with low latency',
      lastChecked: '2026-08-30T12:00:00Z',
    },
    {
      id: 'EMAIL_SENDGRID',
      name: 'SendGrid',
      channel: 'Email',
      status: 'HEALTHY',
      message: 'SMTP dispatch nominal, zero bounce rate',
      lastChecked: '2026-08-30T12:00:00Z',
    },
    {
      id: 'SMS_TWILIO',
      name: 'Twilio',
      channel: 'SMS',
      status: 'HEALTHY',
      message: 'DLT routes active and verified',
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

const mockDegradedSummary: ProviderHealthSummary = {
  overallStatus: 'DEGRADED',
  lastChecked: '2026-08-30T12:15:00Z',
  providers: [
    {
      id: 'WHATSAPP_TWILIO',
      name: 'Twilio',
      channel: 'WhatsApp',
      status: 'HEALTHY',
      message: 'Nominal',
      lastChecked: '2026-08-30T12:15:00Z',
    },
    {
      id: 'EMAIL_SENDGRID',
      name: 'SendGrid',
      channel: 'Email',
      status: 'DEGRADED',
      message: 'Elevated queue latency 3400ms',
      lastChecked: '2026-08-30T12:15:00Z',
    },
  ],
};

const mockUnavailableSummary: ProviderHealthSummary = {
  overallStatus: 'UNAVAILABLE',
  lastChecked: '2026-08-30T12:30:00Z',
  providers: [
    {
      id: 'SMS_TWILIO',
      name: 'Twilio',
      channel: 'SMS',
      status: 'UNAVAILABLE',
      message: 'Rate limit threshold exceeded, circuit breaker open',
      lastChecked: '2026-08-30T12:30:00Z',
    },
  ],
};

describe('Provider Settings Console Redesign', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.spyOn(useDemoModeHook, 'useDemoMode').mockReturnValue({
      isDemoMode: false,
      enterDemoMode: vi.fn(),
      exitDemoMode: vi.fn(),
    });
    vi.mocked(providersApi.getProviderHealth).mockResolvedValue(mockHealthySummary);
    vi.mocked(demoApi.getDemoProviderHealth).mockResolvedValue(mockHealthySummary);
  });

  function renderProviderSettings(initialEntry = '/settings/providers') {
    return render(
      <MemoryRouter initialEntries={[initialEntry]}>
        <ToastProvider>
          <Routes>
            <Route path="/settings" element={<SettingsLayout />}>
              <Route path="providers" element={<ProviderSettingsPage />} />
            </Route>
          </Routes>
        </ToastProvider>
      </MemoryRouter>
    );
  }

  it('renders redesigned header with PROVIDER STATUS eyebrow, title, and concise operational description', async () => {
    renderProviderSettings();

    expect(screen.getByText('PROVIDER STATUS')).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 1, name: 'Provider Status' })).toBeInTheDocument();
    expect(
      screen.getByText('Monitor payment provider connectivity, health, and recovery readiness.')
    ).toBeInTheDocument();
  });

  it('highlights Provider Status as the active navigation tab', () => {
    renderProviderSettings();

    const providerTab = screen.getByRole('link', { name: /Provider Status/i });
    expect(providerTab).toHaveAttribute('aria-current', 'page');
    expect(providerTab.className).toContain('bg-[#E8F7F0]');
    expect(providerTab.className).toContain('text-[#08704F]');
  });

  it('renders operational System Health Hero with nominal status and production badge', async () => {
    renderProviderSettings();

    await waitFor(() => {
      expect(
        screen.getByText('All Recovery & Communication Providers Operational')
      ).toBeInTheDocument();
      expect(screen.getByText('PRODUCTION')).toBeInTheDocument();
      expect(screen.getByText('Last Refreshed')).toBeInTheDocument();
    });
  });

  it('renders operational summary strip with accurate derived metrics', async () => {
    renderProviderSettings();

    await waitFor(() => {
      expect(screen.getByText('Configured Providers')).toBeInTheDocument();
      expect(screen.getByText('Nominal status verified')).toBeInTheDocument();
      expect(screen.getByText('Zero degraded channels')).toBeInTheDocument();
      expect(screen.getByText('Zero offline channels')).toBeInTheDocument();
    });

    // Check count values (4 configured, 4 operational, 0 degraded, 0 unavailable)
    const metricElements = screen.getAllByText('4');
    expect(metricElements.length).toBeGreaterThanOrEqual(2);
  });

  it('renders provider cards with channel icons, status badges, and messages', async () => {
    renderProviderSettings();

    await waitFor(() => {
      expect(screen.getByText('Twilio Sandbox connected with low latency')).toBeInTheDocument();
      expect(screen.getByText('SMTP dispatch nominal, zero bounce rate')).toBeInTheDocument();
      expect(screen.getByText('DLT routes active and verified')).toBeInTheDocument();
      expect(screen.getByText('Webhook and charge APIs operational')).toBeInTheDocument();
      expect(screen.getByText('Payment Retry')).toBeInTheDocument();
    });
  });

  it('highlights Razorpay as Primary Gateway integration', async () => {
    renderProviderSettings();

    await waitFor(() => {
      expect(screen.getByText('Primary Gateway')).toBeInTheDocument();
      expect(screen.getByText('Razorpay')).toBeInTheDocument();
    });
  });

  it('renders degraded state properly when upstream provider issues occur', async () => {
    vi.mocked(providersApi.getProviderHealth).mockResolvedValue(mockDegradedSummary);

    renderProviderSettings();

    await waitFor(() => {
      expect(screen.getByText('Upstream Provider Degradation Observed')).toBeInTheDocument();
      expect(screen.getByText('Elevated queue latency 3400ms')).toBeInTheDocument();
      expect(screen.getByText('Queue latency warning')).toBeInTheDocument();
    });
  });

  it('renders unavailable state properly when critical channels are down', async () => {
    vi.mocked(providersApi.getProviderHealth).mockResolvedValue(mockUnavailableSummary);

    renderProviderSettings();

    await waitFor(() => {
      expect(screen.getByText('Critical Provider Connectivity Interrupted')).toBeInTheDocument();
      expect(screen.getByText('Requires immediate action')).toBeInTheDocument();
      expect(
        screen.getByText('Rate limit threshold exceeded, circuit breaker open')
      ).toBeInTheDocument();
    });
  });

  it('handles manual refresh via Check Status button', async () => {
    renderProviderSettings();

    await waitFor(() => {
      expect(screen.getByText('Twilio Sandbox connected with low latency')).toBeInTheDocument();
    });

    const refreshButton = screen.getByRole('button', { name: /Check Status/i });
    fireEvent.click(refreshButton);

    await waitFor(() => {
      expect(providersApi.getProviderHealth).toHaveBeenCalledTimes(2);
    });
  });

  it('renders simulated sandbox environment in Demo Mode', async () => {
    vi.spyOn(useDemoModeHook, 'useDemoMode').mockReturnValue({
      isDemoMode: true,
      enterDemoMode: vi.fn(),
      exitDemoMode: vi.fn(),
    });

    renderProviderSettings();

    await waitFor(() => {
      expect(screen.getByText('SIMULATED SANDBOX')).toBeInTheDocument();
      expect(demoApi.getDemoProviderHealth).toHaveBeenCalled();
    });
  });

  it('verifies Zero Secret Exposure policy is enforced and displayed', async () => {
    renderProviderSettings();

    await waitFor(() => {
      expect(screen.getByText('Zero Secret Exposure Policy')).toBeInTheDocument();
      expect(screen.getByText('VERIFIED ENCLAVE')).toBeInTheDocument();
    });

    const bodyText = document.body.textContent || '';
    expect(bodyText).not.toContain('sk_live_');
    expect(bodyText).not.toContain('super_secret');
    expect(bodyText).not.toContain('whsec_');
    expect(bodyText).not.toContain('Bearer eyJ');
  });

  it('renders empty state when no providers are configured', async () => {
    vi.mocked(providersApi.getProviderHealth).mockResolvedValue({
      overallStatus: 'UNKNOWN',
      providers: [],
      lastChecked: '2026-08-30T12:00:00Z',
    });

    renderProviderSettings();

    await waitFor(() => {
      expect(screen.getByText('No provider health checks configured.')).toBeInTheDocument();
    });
  });

  it('renders error state when provider health fetch fails', async () => {
    vi.mocked(providersApi.getProviderHealth).mockRejectedValue(
      new Error('Actuator service unreachable')
    );

    renderProviderSettings();

    await waitFor(() => {
      expect(screen.getByText('Failed to Load Provider Status')).toBeInTheDocument();
      expect(screen.getByText('Actuator service unreachable')).toBeInTheDocument();
    });
  });

  it('renders shared Footer component at the bottom of the page', async () => {
    renderProviderSettings();

    await waitFor(() => {
      expect(screen.getByText('System Infrastructure')).toBeInTheDocument();
      expect(screen.getByText('Platform Navigation')).toBeInTheDocument();
      expect(screen.getByText(/Autonomous recovery infrastructure/i)).toBeInTheDocument();
    });
  });
});
