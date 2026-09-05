import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { ToastProvider } from '../context/ToastContext';
import { SettingsLayout } from '../pages/settings/SettingsLayout';
import { NotificationSettingsPage } from '../pages/settings/NotificationSettingsPage';
import * as prefsApi from '../api/notification-preferences';
import * as demoApi from '../api/demo';
import * as useDemoModeHook from '../hooks/useDemoMode';
import type { NotificationPreferenceResponseDto } from '../types/notifications';

vi.mock('../api/notification-preferences');
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

const mockPersistedPrefs: NotificationPreferenceResponseDto = {
  merchantId: 'm-tenant-999',
  webhookUrl: 'https://api.acme.com/webhooks/recoverai',
  preferences: {
    PAYMENT_RECOVERED: { EMAIL: true, WEBHOOK: true, IN_APP: true },
    CASE_EXHAUSTED: { EMAIL: true, WEBHOOK: false, IN_APP: true },
    HIGH_PRIORITY_FAILURE: { EMAIL: true, WEBHOOK: true, IN_APP: true },
    PROVIDER_DEGRADED: { EMAIL: false, WEBHOOK: false, IN_APP: true },
  },
};

describe('Notification Preferences Workspace Redesign', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.spyOn(useDemoModeHook, 'useDemoMode').mockReturnValue({
      isDemoMode: false,
      enterDemoMode: vi.fn(),
      exitDemoMode: vi.fn(),
    });
    vi.mocked(prefsApi.getNotificationPreferences).mockResolvedValue(mockPersistedPrefs);
    vi.mocked(prefsApi.updateNotificationPreferences).mockImplementation(async (payload) => ({
      merchantId: 'm-tenant-999',
      webhookUrl: payload.webhookUrl || null,
      preferences: payload.preferences,
    }));
    vi.mocked(demoApi.getDemoNotificationPreferences).mockResolvedValue({
      merchantId: 'demo-merchant-evaluator',
      webhookUrl: 'https://demo.example.com/webhooks/recoverai',
      preferences: {
        PAYMENT_RECOVERED: { EMAIL: true, WEBHOOK: true, IN_APP: true },
        CASE_EXHAUSTED: { EMAIL: true, WEBHOOK: false, IN_APP: true },
        HIGH_PRIORITY_FAILURE: { EMAIL: true, WEBHOOK: true, IN_APP: true },
        PROVIDER_DEGRADED: { EMAIL: false, WEBHOOK: false, IN_APP: true },
      },
    });
  });

  function renderNotificationPreferences(initialEntry = '/settings/notifications') {
    return render(
      <MemoryRouter initialEntries={[initialEntry]}>
        <ToastProvider>
          <Routes>
            <Route path="/settings" element={<SettingsLayout />}>
              <Route path="notifications" element={<NotificationSettingsPage />} />
            </Route>
          </Routes>
        </ToastProvider>
      </MemoryRouter>
    );
  }

  it('renders redesigned header with NOTIFICATION PREFERENCES eyebrow, title, and concise operational description', async () => {
    renderNotificationPreferences();

    expect(screen.getByText('NOTIFICATION PREFERENCES')).toBeInTheDocument();
    expect(screen.getByText('Settings & Operations')).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 1, name: 'Notification Preferences' })).toBeInTheDocument();
    expect(
      screen.getByText('Control which recovery events trigger notifications and where they are delivered.')
    ).toBeInTheDocument();
  });

  it('renders settings subnavigation with Notification Preferences marked active with aria-current="page"', async () => {
    renderNotificationPreferences();

    const notifTab = screen.getByRole('link', { name: /Notification Preferences/i });
    const generalTab = screen.getByRole('link', { name: /General & Account/i });
    const providersTab = screen.getByRole('link', { name: /Provider Status/i });

    expect(notifTab).toBeInTheDocument();
    expect(generalTab).toBeInTheDocument();
    expect(providersTab).toBeInTheDocument();

    expect(notifTab).toHaveAttribute('aria-current', 'page');
    expect(generalTab).not.toHaveAttribute('aria-current');
    expect(providersTab).not.toHaveAttribute('aria-current');
  });

  it('renders operational summary strip derived dynamically from current preference state', async () => {
    renderNotificationPreferences();

    await waitFor(() => {
      expect(screen.getByLabelText('Notification Operations Summary')).toBeInTheDocument();
    });

    // TOTAL EVENTS: 4
    expect(screen.getByText('TOTAL EVENTS')).toBeInTheDocument();
    expect(screen.getAllByText('4').length).toBeGreaterThanOrEqual(2);
    expect(screen.getByText('lifecycle types')).toBeInTheDocument();

    // ENABLED EVENTS: 4 (all 4 have >= 1 channel enabled in mockPersistedPrefs)
    expect(screen.getByText('ENABLED EVENTS')).toBeInTheDocument();
    expect(screen.getByText('of 4 active')).toBeInTheDocument();

    // DELIVERY CHANNELS: 3
    expect(screen.getByText('DELIVERY CHANNELS')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();

    // ACTIVE RULES: 8 out of 12 (PAYMENT_RECOVERED:3, CASE_EXHAUSTED:2, HIGH_PRIORITY_FAILURE:3, PROVIDER_DEGRADED:1 = 9)
    expect(screen.getByText('ACTIVE RULES')).toBeInTheDocument();
    expect(screen.getByText('/ 12 routes')).toBeInTheDocument();
  });

  it('renders all four lifecycle events with semantic icons, human titles, descriptions, and technical enum badges', async () => {
    renderNotificationPreferences();

    await waitFor(() => {
      expect(screen.getByText('Payment Recovered')).toBeInTheDocument();
      expect(screen.getByText('PAYMENT_RECOVERED')).toBeInTheDocument();

      expect(screen.getByText('Case Exhausted')).toBeInTheDocument();
      expect(screen.getByText('CASE_EXHAUSTED')).toBeInTheDocument();

      expect(screen.getByText('High Priority Failure')).toBeInTheDocument();
      expect(screen.getByText('HIGH_PRIORITY_FAILURE')).toBeInTheDocument();

      expect(screen.getByText('Provider Degraded')).toBeInTheDocument();
      expect(screen.getByText('PROVIDER_DEGRADED')).toBeInTheDocument();
    });
  });

  it('renders channel columns with sub-descriptors (Merchant inbox, System integration, RecoverAI console)', async () => {
    renderNotificationPreferences();

    await waitFor(() => {
      expect(screen.getByText('Merchant inbox')).toBeInTheDocument();
      expect(screen.getByText('System integration')).toBeInTheDocument();
      expect(screen.getByText('RecoverAI console')).toBeInTheDocument();
    });
  });

  it('exposes accessible switch controls with descriptive aria labels and state', async () => {
    renderNotificationPreferences();

    await waitFor(() => {
      const emailSwitch = screen.getByRole('switch', {
        name: 'Enable Email for Payment Recovered',
      });
      expect(emailSwitch).toBeInTheDocument();
      expect(emailSwitch).toHaveAttribute('aria-checked', 'true');

      const webhookSwitch = screen.getByRole('switch', {
        name: 'Enable Webhook for Case Exhausted',
      });
      expect(webhookSwitch).toBeInTheDocument();
      expect(webhookSwitch).toHaveAttribute('aria-checked', 'false');
    });
  });

  it('tracks dirty state and allows saving changes to API', async () => {
    renderNotificationPreferences();

    await waitFor(() => {
      expect(screen.getByText('Persisted')).toBeInTheDocument();
    });

    const saveButton = screen.getByRole('button', { name: /Save Changes/i });
    const resetButton = screen.getByRole('button', { name: /Reset/i });
    expect(saveButton).toBeDisabled();
    expect(resetButton).toBeDisabled();

    // Toggle Webhook for Case Exhausted from false to true
    const webhookSwitch = screen.getByRole('switch', {
      name: 'Enable Webhook for Case Exhausted',
    });
    fireEvent.click(webhookSwitch);

    await waitFor(() => {
      expect(screen.getByText('Unsaved Changes')).toBeInTheDocument();
      expect(saveButton).not.toBeDisabled();
      expect(resetButton).not.toBeDisabled();
    });

    fireEvent.click(saveButton);

    await waitFor(() => {
      expect(prefsApi.updateNotificationPreferences).toHaveBeenCalledWith(
        expect.objectContaining({
          preferences: expect.objectContaining({
            CASE_EXHAUSTED: expect.objectContaining({
              WEBHOOK: true,
            }),
          }),
        })
      );
      expect(screen.getByText('Persisted')).toBeInTheDocument();
    });
  });

  it('supports resetting modified preferences back to persisted values', async () => {
    renderNotificationPreferences();

    await waitFor(() => {
      expect(screen.getByText('Persisted')).toBeInTheDocument();
    });

    const input = screen.getByDisplayValue('https://api.acme.com/webhooks/recoverai');
    fireEvent.change(input, { target: { value: 'https://new.acme.com/hook' } });

    await waitFor(() => {
      expect(screen.getByText('Unsaved Changes')).toBeInTheDocument();
    });

    const resetButton = screen.getByRole('button', { name: /Reset/i });
    fireEvent.click(resetButton);

    await waitFor(() => {
      expect(screen.getByDisplayValue('https://api.acme.com/webhooks/recoverai')).toBeInTheDocument();
      expect(screen.getByText('Persisted')).toBeInTheDocument();
    });
  });

  it('displays channel delivery scope cards explaining Email, Webhook, and In-App delivery', async () => {
    renderNotificationPreferences();

    await waitFor(() => {
      expect(screen.getByText('Email Delivery')).toBeInTheDocument();
      expect(screen.getByText('Webhook Delivery')).toBeInTheDocument();
      expect(screen.getByText('In-App Delivery')).toBeInTheDocument();
    });
  });

  it('renders Zero Secret Exposure and Fintech Delivery Safety notices', async () => {
    renderNotificationPreferences();

    await waitFor(() => {
      expect(screen.getByText('Zero Secret Exposure & Payload Verification:')).toBeInTheDocument();
      expect(screen.getByText('X-Recovery-Signature')).toBeInTheDocument();
      expect(screen.getByText('Fintech Delivery Safety Notice:')).toBeInTheDocument();
    });
  });

  it('validates invalid webhook URLs before allowing save', async () => {
    renderNotificationPreferences();

    await waitFor(() => {
      expect(screen.getByDisplayValue('https://api.acme.com/webhooks/recoverai')).toBeInTheDocument();
    });

    const input = screen.getByDisplayValue('https://api.acme.com/webhooks/recoverai');
    fireEvent.change(input, { target: { value: 'ftp://not-allowed.com' } });

    await waitFor(() => {
      expect(screen.getByText('URL must use HTTP or HTTPS protocol')).toBeInTheDocument();
    });
  });

  it('simulates saving in Demo Mode without calling production API', async () => {
    vi.spyOn(useDemoModeHook, 'useDemoMode').mockReturnValue({
      isDemoMode: true,
      enterDemoMode: vi.fn(),
      exitDemoMode: vi.fn(),
    });

    renderNotificationPreferences();

    await waitFor(() => {
      expect(screen.getByText('Sandbox State')).toBeInTheDocument();
    });

    // Toggle switch
    const switchControl = screen.getByRole('switch', {
      name: 'Enable Webhook for Case Exhausted',
    });
    fireEvent.click(switchControl);

    const saveButton = screen.getByRole('button', { name: /Save Changes/i });
    fireEvent.click(saveButton);

    await waitFor(() => {
      // Production API should not be called in demo mode
      expect(prefsApi.updateNotificationPreferences).not.toHaveBeenCalled();
      expect(screen.getByText('Persisted')).toBeInTheDocument();
    });
  });

  it('renders skeleton loading state during preference fetch', () => {
    vi.mocked(prefsApi.getNotificationPreferences).mockReturnValue(new Promise(() => {}));

    renderNotificationPreferences();

    expect(screen.getByRole('status', { name: 'Loading preferences' })).toBeInTheDocument();
  });

  it('renders error state with retry button when fetching preferences fails', async () => {
    vi.mocked(prefsApi.getNotificationPreferences).mockRejectedValue(new Error('Network connection timeout'));

    renderNotificationPreferences();

    await waitFor(() => {
      expect(screen.getByText('Failed to Load Preferences')).toBeInTheDocument();
      expect(screen.getByText('Network connection timeout')).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Try Again/i })).toBeInTheDocument();
    });
  });

  it('renders global shared Footer', async () => {
    renderNotificationPreferences();

    await waitFor(() => {
      expect(
        screen.getByText(/Autonomous recovery infrastructure for failed payments/i)
      ).toBeInTheDocument();
      expect(screen.getByText('System Infrastructure')).toBeInTheDocument();
    });
  });

  it('never exposes sensitive secrets, tokens, or private keys in the DOM', async () => {
    renderNotificationPreferences();

    await waitFor(() => {
      expect(screen.getByText('Merchant Webhook Endpoint')).toBeInTheDocument();
    });

    const bodyText = document.body.textContent || '';
    expect(bodyText).not.toContain('fake-token-do-not-expose');
    expect(bodyText).not.toContain('secret_key');
    expect(bodyText).not.toContain('private_key');
  });
});
