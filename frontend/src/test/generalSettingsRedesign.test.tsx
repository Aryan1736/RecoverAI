import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { ToastProvider } from '../context/ToastContext';
import { SettingsLayout } from '../pages/settings/SettingsLayout';
import { GeneralSettingsPage } from '../pages/settings/GeneralSettingsPage';
import * as useAuthHook from '../hooks/useAuth';
import * as useDemoModeHook from '../hooks/useDemoMode';

const mockLogout = vi.fn();

const mockMerchantUser = {
  id: 'm-tenant-999',
  name: 'Acme Corp',
  email: 'ops@acmecorp.com',
  status: 'ACTIVE' as const,
  razorpayAccountId: 'acc_rzp_12345',
  createdAt: '2026-08-01T00:00:00Z',
  updatedAt: '2026-08-30T00:00:00Z',
};

describe('GeneralSettingsPage & SettingsLayout Redesign', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.spyOn(useAuthHook, 'useAuth').mockReturnValue({
      user: mockMerchantUser,
      token: 'fake-token-do-not-expose',
      isAuthenticated: true,
      isDemoMode: false,
      isLoading: false,
      sessionExpiredMessage: null,
      login: vi.fn(),
      register: vi.fn(),
      logout: mockLogout,
      clearSessionExpiredMessage: vi.fn(),
    });
    vi.spyOn(useDemoModeHook, 'useDemoMode').mockReturnValue({
      isDemoMode: false,
      enterDemoMode: vi.fn(),
      exitDemoMode: vi.fn(),
    });
  });

  function renderGeneralSettings(initialEntry = '/settings/general') {
    return render(
      <MemoryRouter initialEntries={[initialEntry]}>
        <ToastProvider>
          <Routes>
            <Route path="/settings" element={<SettingsLayout />}>
              <Route index element={<GeneralSettingsPage />} />
              <Route path="general" element={<GeneralSettingsPage />} />
            </Route>
          </Routes>
        </ToastProvider>
      </MemoryRouter>
    );
  }

  it('renders redesigned header with SETTINGS eyebrow, Settings & Operations context, and General & Account title', () => {
    renderGeneralSettings();

    expect(screen.getByText('SETTINGS')).toBeInTheDocument();
    expect(screen.getByText('Settings & Operations')).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 1, name: 'General & Account' })).toBeInTheDocument();
    expect(
      screen.getByText('Manage your RecoverAI account, merchant identity, and workspace configuration.')
    ).toBeInTheDocument();
  });

  it('renders all settings subnavigation links with General & Account active', () => {
    renderGeneralSettings();

    const generalLink = screen.getByRole('link', { name: /General & Account/i });
    const notifLink = screen.getByRole('link', { name: /Notification Preferences/i });
    const providersLink = screen.getByRole('link', { name: /Provider Status/i });

    expect(generalLink).toBeInTheDocument();
    expect(notifLink).toBeInTheDocument();
    expect(providersLink).toBeInTheDocument();

    expect(generalLink).toHaveAttribute('aria-current', 'page');
    expect(notifLink).not.toHaveAttribute('aria-current');
    expect(providersLink).not.toHaveAttribute('aria-current');
  });

  it('renders merchant identity, avatar, tenant identifier, and active status badge', () => {
    renderGeneralSettings();

    expect(screen.getAllByText('Acme Corp').length).toBeGreaterThan(0);
    expect(screen.getAllByText('ops@acmecorp.com').length).toBeGreaterThan(0);
    expect(screen.getAllByText('m-tenant-999').length).toBeGreaterThan(0);
    expect(screen.getAllByText('ACTIVE').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Production').length).toBeGreaterThan(0);
    expect(screen.getByText('Merchant Principal')).toBeInTheDocument();
  });

  it('renders structured read-only identity cards with lock indicators and READ ONLY tags', () => {
    renderGeneralSettings();

    expect(screen.getByText('Merchant Identity')).toBeInTheDocument();
    expect(screen.getByText('Account Status')).toBeInTheDocument();
    expect(screen.getByText('Tenant ID (UUID)')).toBeInTheDocument();
    expect(screen.getByText('Account Created')).toBeInTheDocument();
    expect(screen.getAllByText('READ ONLY').length).toBeGreaterThanOrEqual(4);
  });

  it('renders payment integration with Razorpay account ID and governance notice', () => {
    renderGeneralSettings();

    expect(screen.getByText('Payment Integration')).toBeInTheDocument();
    expect(screen.getByText('Razorpay Account ID')).toBeInTheDocument();
    expect(screen.getByText('acc_rzp_12345')).toBeInTheDocument();
    expect(screen.getByText('Linked Gateway')).toBeInTheDocument();

    // Preserve critical governance notice
    expect(screen.getByText('Account Profile Governance')).toBeInTheDocument();
    expect(
      screen.getByText(/Merchant identity and payment processor integrations are locked to maintain tenant security/i)
    ).toBeInTheDocument();
  });

  it('renders security protocols and tenant isolation', () => {
    renderGeneralSettings();

    expect(screen.getAllByText('Security & Active Session').length).toBeGreaterThan(0);
    expect(screen.getByText('Authentication Protocol')).toBeInTheDocument();
    expect(screen.getByText('Stateless JWT (HMAC-SHA256)')).toBeInTheDocument();
    expect(screen.getByText('Tenant Isolation')).toBeInTheDocument();
    expect(screen.getByText('Strict Merchant Header & Claim Scoping')).toBeInTheDocument();
  });

  it('allows copying Tenant ID and Razorpay Account ID with visual feedback', async () => {
    const user = userEvent.setup();
    const writeTextMock = vi.fn().mockResolvedValue(undefined);
    if (!navigator.clipboard) {
      Object.defineProperty(navigator, 'clipboard', {
        value: { writeText: writeTextMock },
        writable: true,
        configurable: true,
      });
    } else {
      vi.spyOn(navigator.clipboard, 'writeText').mockImplementation(writeTextMock);
    }

    renderGeneralSettings();

    const copyTenantBtn = screen.getByRole('button', { name: /Copy Tenant ID UUID/i });
    expect(copyTenantBtn).toBeInTheDocument();
    await user.click(copyTenantBtn);

    await waitFor(() => {
      expect(screen.getByText('Copied')).toBeInTheDocument();
    });

    const copyRazorpayBtn = screen.getByRole('button', { name: /Copy Razorpay Account identifier/i });
    expect(copyRazorpayBtn).toBeInTheDocument();
    await user.click(copyRazorpayBtn);

    await waitFor(() => {
      expect(screen.getAllByText('Copied').length).toBeGreaterThan(0);
    });
  });

  it('invokes logout when Sign Out button is clicked', async () => {
    const user = userEvent.setup();
    renderGeneralSettings();

    const signOutBtn = screen.getByRole('button', { name: /Sign Out/i });
    expect(signOutBtn).toBeInTheDocument();

    await user.click(signOutBtn);
    expect(mockLogout).toHaveBeenCalledTimes(1);
  });

  it('renders Simulated Sandbox badge when in Demo Mode', () => {
    vi.spyOn(useAuthHook, 'useAuth').mockReturnValue({
      user: {
        ...mockMerchantUser,
        name: 'Demo Merchant Sandbox',
      },
      token: null,
      isAuthenticated: true,
      isDemoMode: true,
      isLoading: false,
      sessionExpiredMessage: null,
      login: vi.fn(),
      register: vi.fn(),
      logout: mockLogout,
      clearSessionExpiredMessage: vi.fn(),
    });
    vi.spyOn(useDemoModeHook, 'useDemoMode').mockReturnValue({
      isDemoMode: true,
      enterDemoMode: vi.fn(),
      exitDemoMode: vi.fn(),
    });

    renderGeneralSettings();

    expect(screen.getAllByText('Simulated Sandbox').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Demo Merchant Sandbox').length).toBeGreaterThan(0);
  });

  it('renders fallback for unconfigured Razorpay Account ID', () => {
    vi.spyOn(useAuthHook, 'useAuth').mockReturnValue({
      user: {
        ...mockMerchantUser,
        razorpayAccountId: null,
      },
      token: 'fake-token',
      isAuthenticated: true,
      isDemoMode: false,
      isLoading: false,
      sessionExpiredMessage: null,
      login: vi.fn(),
      register: vi.fn(),
      logout: mockLogout,
      clearSessionExpiredMessage: vi.fn(),
    });

    renderGeneralSettings();

    expect(screen.getAllByText('Not configured').length).toBeGreaterThan(0);
  });

  it('does NOT expose sensitive credentials, JWT tokens, or passwords in the DOM', () => {
    const { container } = renderGeneralSettings();

    expect(screen.queryByText('fake-token-do-not-expose')).not.toBeInTheDocument();
    expect(container.innerHTML).not.toContain('fake-token-do-not-expose');
    expect(container.innerHTML).not.toContain('password');
    expect(container.innerHTML).not.toContain('privateKey');
    expect(container.innerHTML).not.toContain('api_secret');
    expect(container.innerHTML).not.toContain('jwt_secret');
  });

  it('renders the shared fintech footer', () => {
    renderGeneralSettings();

    expect(screen.getByText('Platform Navigation')).toBeInTheDocument();
    expect(screen.getByText('System Infrastructure')).toBeInTheDocument();
    expect(screen.getByText(/Built for intelligent payment recovery\./i)).toBeInTheDocument();
  });
});
