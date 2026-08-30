import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { ToastProvider } from '../context/ToastContext';
import { SettingsLayout } from '../pages/settings/SettingsLayout';
import { GeneralSettingsPage } from '../pages/settings/GeneralSettingsPage';
import { NotificationSettingsPage } from '../pages/settings/NotificationSettingsPage';
import { ProviderSettingsPage } from '../pages/settings/ProviderSettingsPage';
import * as prefsApi from '../api/notification-preferences';
import * as providersApi from '../api/providers';

vi.mock('../api/notification-preferences');
vi.mock('../api/providers');
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

describe('SettingsLayout and Navigation', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(prefsApi.getNotificationPreferences).mockResolvedValue({
      merchantId: 'm-tenant-999',
      webhookUrl: 'https://acme.com/webhook',
      preferences: {
        PAYMENT_RECOVERED: { EMAIL: true, WEBHOOK: false, IN_APP: true },
        CASE_EXHAUSTED: { EMAIL: true, WEBHOOK: false, IN_APP: true },
        HIGH_PRIORITY_FAILURE: { EMAIL: true, WEBHOOK: false, IN_APP: true },
        PROVIDER_DEGRADED: { EMAIL: false, WEBHOOK: false, IN_APP: true },
      },
    });
    vi.mocked(providersApi.getProviderHealth).mockResolvedValue({
      overallStatus: 'HEALTHY',
      providers: [],
      lastChecked: '2026-08-30T12:00:00Z',
    });
  });

  it('renders all settings subnavigation links with icons and labels', () => {
    render(
      <MemoryRouter initialEntries={['/settings/general']}>
        <ToastProvider>
          <Routes>
            <Route path="/settings" element={<SettingsLayout />}>
              <Route path="general" element={<GeneralSettingsPage />} />
            </Route>
          </Routes>
        </ToastProvider>
      </MemoryRouter>
    );

    expect(screen.getByText('Settings & Operations')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /General & Account/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Notification Preferences/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Provider Status/i })).toBeInTheDocument();
  });

  it('renders general settings details including merchant identity, tenant UUID, and razorpay ID', () => {
    render(
      <MemoryRouter initialEntries={['/settings/general']}>
        <ToastProvider>
          <Routes>
            <Route path="/settings" element={<SettingsLayout />}>
              <Route path="general" element={<GeneralSettingsPage />} />
            </Route>
          </Routes>
        </ToastProvider>
      </MemoryRouter>
    );

    expect(screen.getAllByText('Acme Corp').length).toBeGreaterThan(0);
    expect(screen.getByText('ops@acmecorp.com')).toBeInTheDocument();
    expect(screen.getByText('m-tenant-999')).toBeInTheDocument();
    expect(screen.getByText('acc_rzp_12345')).toBeInTheDocument();
    expect(screen.getByText('Account Profile Governance')).toBeInTheDocument();
  });

  it('navigates to notifications preferences tab route', async () => {
    render(
      <MemoryRouter initialEntries={['/settings/notifications']}>
        <ToastProvider>
          <Routes>
            <Route path="/settings" element={<SettingsLayout />}>
              <Route path="notifications" element={<NotificationSettingsPage />} />
            </Route>
          </Routes>
        </ToastProvider>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Notification Preferences & Delivery Rules')).toBeInTheDocument();
      expect(screen.getByText('Event Channel Matrix')).toBeInTheDocument();
    });
  });

  it('navigates to providers tab route', async () => {
    render(
      <MemoryRouter initialEntries={['/settings/providers']}>
        <ToastProvider>
          <Routes>
            <Route path="/settings" element={<SettingsLayout />}>
              <Route path="providers" element={<ProviderSettingsPage />} />
            </Route>
          </Routes>
        </ToastProvider>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Upstream Provider Operational Status')).toBeInTheDocument();
    });
  });
});
