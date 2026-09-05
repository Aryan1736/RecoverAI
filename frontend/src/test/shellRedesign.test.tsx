import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { Sidebar } from '../components/layout/Sidebar';
import { Header } from '../components/layout/Header';
import { AppShell } from '../components/layout/AppShell';
import * as authApi from '../api/auth';
import * as notificationsApi from '../api/notifications';

const mockLogout = vi.fn();
const mockExitDemoMode = vi.fn();
let mockIsDemoMode = false;
let mockUser = {
  id: 'm-tenant-987654321',
  name: 'Acme Recovery Ops',
  email: 'ops@acme.com',
  status: 'ACTIVE',
  razorpayAccountId: 'acc_rzp_live_12345',
};

vi.mock('../hooks/useAuth', () => ({
  useAuth: () => ({
    user: mockUser,
    isDemoMode: mockIsDemoMode,
    logout: mockLogout,
  }),
}));

vi.mock('../hooks/useDemoMode', () => ({
  useDemoMode: () => ({
    isDemoMode: mockIsDemoMode,
    exitDemoMode: mockExitDemoMode,
  }),
}));

vi.mock('../hooks/useToast', () => ({
  useToast: () => ({
    toast: {
      success: vi.fn(),
      error: vi.fn(),
      info: vi.fn(),
    },
  }),
}));

vi.mock('../api/auth', () => ({
  fetchHealth: vi.fn(),
}));

vi.mock('../api/notifications', () => ({
  getUnreadCount: vi.fn(),
}));

describe('Global Application Shell Redesign', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockIsDemoMode = false;
    mockUser = {
      id: 'm-tenant-987654321',
      name: 'Acme Recovery Ops',
      email: 'ops@acme.com',
      status: 'ACTIVE',
      razorpayAccountId: 'acc_rzp_live_12345',
    };
    vi.mocked(authApi.fetchHealth).mockResolvedValue({ status: 'UP', service: 'recover-ai-backend' });
    vi.mocked(notificationsApi.getUnreadCount).mockResolvedValue(0);
  });

  describe('1. Brand & Favicon Integration', () => {
    it('renders RecoverAI brand mark with img.png, RecoverAI, Track 3, and Payment Recovery Ops in expanded sidebar', () => {
      render(
        <MemoryRouter initialEntries={['/app']}>
          <Sidebar
            isCollapsed={false}
            setIsCollapsed={vi.fn()}
            isMobileOpen={false}
            setIsMobileOpen={vi.fn()}
          />
        </MemoryRouter>
      );

      // Brand image is rendered with alt and src
      const faviconImg = screen.getByTestId('sidebar-brand-favicon');
      expect(faviconImg).toBeInTheDocument();
      expect(faviconImg).toHaveAttribute('src', '/img.png');
      expect(faviconImg).toHaveAttribute('alt', 'RecoverAI');

      // Brand title RecoverAI is rendered
      expect(screen.getByText('RecoverAI')).toBeInTheDocument();

      // Track 3 badge renders cleanly in the brand area
      expect(screen.getByText('Track 3')).toBeInTheDocument();

      // Payment Recovery Ops subtitle is present
      expect(screen.getByText('Payment Recovery Ops')).toBeInTheDocument();
    });

    it('renders the project brand mark in collapsed state without squeezing full favicon.svg into a square', () => {
      render(
        <MemoryRouter initialEntries={['/app']}>
          <Sidebar
            isCollapsed={true}
            setIsCollapsed={vi.fn()}
            isMobileOpen={false}
            setIsMobileOpen={vi.fn()}
          />
        </MemoryRouter>
      );

      // In collapsed mode, expanded mark image is not shown
      expect(screen.queryByTestId('sidebar-brand-favicon')).not.toBeInTheDocument();

      // Instead, the appropriate existing brand mark is rendered in the square slot
      const collapsedMark = screen.getByTestId('sidebar-collapsed-brand-mark');
      expect(collapsedMark).toBeInTheDocument();
      expect(collapsedMark.querySelector('svg.lucide-zap')).toBeInTheDocument();

      // Brand home link is accessible
      expect(screen.getByLabelText('RecoverAI Home')).toBeInTheDocument();

      // Badge and subtitle are hidden in collapsed mode
      expect(screen.queryByText('Track 3')).not.toBeInTheDocument();
      expect(screen.queryByText('Payment Recovery Ops')).not.toBeInTheDocument();
    });
  });

  describe('2. Sidebar Navigation & Active States', () => {
    it('renders all platform navigation items with correct routes', () => {
      render(
        <MemoryRouter initialEntries={['/app']}>
          <Sidebar
            isCollapsed={false}
            setIsCollapsed={vi.fn()}
            isMobileOpen={false}
            setIsMobileOpen={vi.fn()}
          />
        </MemoryRouter>
      );

      const overviewLink = screen.getByRole('link', { name: /overview/i });
      expect(overviewLink).toHaveAttribute('href', '/app');

      const casesLink = screen.getByRole('link', { name: /recovery cases/i });
      expect(casesLink).toHaveAttribute('href', '/recovery-cases');

      const analyticsLink = screen.getByRole('link', { name: /analytics/i });
      expect(analyticsLink).toHaveAttribute('href', '/analytics');

      const notificationsLink = screen.getByRole('link', { name: /notifications/i });
      expect(notificationsLink).toHaveAttribute('href', '/notifications');

      const settingsLink = screen.getByRole('link', { name: /settings/i });
      expect(settingsLink).toHaveAttribute('href', '/settings');
    });

    it('sets aria-current="page" on the active route and not on inactive routes', () => {
      render(
        <MemoryRouter initialEntries={['/recovery-cases']}>
          <Sidebar
            isCollapsed={false}
            setIsCollapsed={vi.fn()}
            isMobileOpen={false}
            setIsMobileOpen={vi.fn()}
          />
        </MemoryRouter>
      );

      const casesLink = screen.getByRole('link', { name: /recovery cases/i });
      expect(casesLink).toHaveAttribute('aria-current', 'page');

      const overviewLink = screen.getByRole('link', { name: /overview/i });
      expect(overviewLink).not.toHaveAttribute('aria-current');

      const analyticsLink = screen.getByRole('link', { name: /analytics/i });
      expect(analyticsLink).not.toHaveAttribute('aria-current');
    });

    it('triggers collapse toggle when collapse button is clicked', async () => {
      const user = userEvent.setup();
      const setIsCollapsed = vi.fn();

      render(
        <MemoryRouter initialEntries={['/app']}>
          <Sidebar
            isCollapsed={false}
            setIsCollapsed={setIsCollapsed}
            isMobileOpen={false}
            setIsMobileOpen={vi.fn()}
          />
        </MemoryRouter>
      );

      const collapseBtn = screen.getByRole('button', { name: /collapse sidebar/i });
      await user.click(collapseBtn);
      expect(setIsCollapsed).toHaveBeenCalledOnce();
    });

    it('renders expand sidebar button when currently collapsed', () => {
      render(
        <MemoryRouter initialEntries={['/app']}>
          <Sidebar
            isCollapsed={true}
            setIsCollapsed={vi.fn()}
            isMobileOpen={false}
            setIsMobileOpen={vi.fn()}
          />
        </MemoryRouter>
      );

      const expandBtn = screen.getByRole('button', { name: /expand sidebar/i });
      expect(expandBtn).toBeInTheDocument();
    });
  });

  describe('3. Recovery Engine Operational Status Panel', () => {
    it('renders operational Recovery Engine card with Active status and model metadata in expanded mode', () => {
      render(
        <MemoryRouter initialEntries={['/app']}>
          <Sidebar
            isCollapsed={false}
            setIsCollapsed={vi.fn()}
            isMobileOpen={false}
            setIsMobileOpen={vi.fn()}
          />
        </MemoryRouter>
      );

      expect(screen.getByText('Recovery Engine')).toBeInTheDocument();
      expect(screen.getByText('Active')).toBeInTheDocument();
      expect(screen.getByText(/Gemini 3.7 Flash/i)).toBeInTheDocument();
      expect(screen.getByText(/Guardrails ON/i)).toBeInTheDocument();
    });

    it('renders accessible operational status indicator in collapsed mode', () => {
      render(
        <MemoryRouter initialEntries={['/app']}>
          <Sidebar
            isCollapsed={true}
            setIsCollapsed={vi.fn()}
            isMobileOpen={false}
            setIsMobileOpen={vi.fn()}
          />
        </MemoryRouter>
      );

      const statusEl = screen.getByRole('status', { name: /Recovery Engine: Active/i });
      expect(statusEl).toBeInTheDocument();
    });
  });

  describe('4. Topbar / Header Context & Operational Telemetry', () => {
    it('renders semantic breadcrumb with dynamic page title', () => {
      render(
        <MemoryRouter initialEntries={['/analytics']}>
          <Header onOpenMobileMenu={vi.fn()} />
        </MemoryRouter>
      );

      const breadcrumb = screen.getByRole('navigation', { name: /breadcrumb/i });
      expect(breadcrumb).toBeInTheDocument();
      expect(screen.getByText('Merchant Portal')).toBeInTheDocument();
      expect(screen.getByText('Analytics')).toHaveAttribute('aria-current', 'page');
    });

    it('displays API operational status pill reflecting backend health', async () => {
      vi.mocked(authApi.fetchHealth).mockResolvedValueOnce({ status: 'UP', service: 'recover-ai-backend' });

      render(
        <MemoryRouter initialEntries={['/app']}>
          <Header onOpenMobileMenu={vi.fn()} />
        </MemoryRouter>
      );

      await waitFor(() => {
        const apiStatus = screen.getByRole('status', { name: /API Status: UP/i });
        expect(apiStatus).toBeInTheDocument();
        expect(apiStatus).toHaveTextContent('UP');
      });
    });

    it('displays offline status pill when backend health fails', async () => {
      vi.mocked(authApi.fetchHealth).mockRejectedValueOnce(new Error('Network error'));

      render(
        <MemoryRouter initialEntries={['/app']}>
          <Header onOpenMobileMenu={vi.fn()} />
        </MemoryRouter>
      );

      await waitFor(() => {
        const apiStatus = screen.getByRole('status', { name: /API Status: OFFLINE/i });
        expect(apiStatus).toBeInTheDocument();
        expect(apiStatus).toHaveTextContent('OFFLINE');
      });
    });

    it('renders reactive unread notification count badge and preserves notification link', async () => {
      vi.mocked(notificationsApi.getUnreadCount).mockResolvedValueOnce(3);

      render(
        <MemoryRouter initialEntries={['/app']}>
          <Header onOpenMobileMenu={vi.fn()} />
        </MemoryRouter>
      );

      await waitFor(() => {
        const badge = screen.getByTestId('notification-unread-badge');
        expect(badge).toBeInTheDocument();
        expect(badge).toHaveTextContent('3');
      });

      const notifLink = screen.getByRole('link', { name: /Notifications \(3 unread\)/i });
      expect(notifLink).toHaveAttribute('href', '/notifications');
    });
  });

  describe('5. Demo Mode Controls & Profile Menu', () => {
    it('renders Demo Mode badge with simulated data pill and exit demo button when demo is active', async () => {
      const user = userEvent.setup();
      mockIsDemoMode = true;

      render(
        <MemoryRouter initialEntries={['/app']}>
          <Header onOpenMobileMenu={vi.fn()} />
        </MemoryRouter>
      );

      expect(screen.getByRole('status', { name: /interactive demo mode active/i })).toBeInTheDocument();
      expect(screen.getByText('DEMO MODE')).toBeInTheDocument();
      expect(screen.getByText('Simulated Data')).toBeInTheDocument();

      const exitBtn = screen.getByRole('button', { name: /exit demo mode/i });
      await user.click(exitBtn);
      expect(mockExitDemoMode).toHaveBeenCalledOnce();
    });

    it('opens account dropdown, displays user details, and triggers logout', async () => {
      const user = userEvent.setup();

      render(
        <MemoryRouter initialEntries={['/app']}>
          <Header onOpenMobileMenu={vi.fn()} />
        </MemoryRouter>
      );

      const profileBtn = screen.getByRole('button', { name: /merchant account menu/i });
      expect(profileBtn).toHaveAttribute('aria-expanded', 'false');

      await user.click(profileBtn);
      expect(profileBtn).toHaveAttribute('aria-expanded', 'true');

      // Dropdown details
      expect(screen.getAllByText('Acme Recovery Ops').length).toBeGreaterThan(0);
      expect(screen.getAllByText('ops@acme.com').length).toBeGreaterThan(0);
      expect(screen.getByText('ACTIVE')).toBeInTheDocument();
      expect(screen.getByText('acc_rzp_live_12345')).toBeInTheDocument();
      expect(screen.getByText(/m-tenant/i)).toBeInTheDocument();

      const signOutBtn = screen.getByRole('button', { name: /sign out/i });
      await user.click(signOutBtn);
      expect(mockLogout).toHaveBeenCalledOnce();
    });
  });

  describe('6. Security & Sensitive Data Protection', () => {
    it('never exposes raw secrets, JWT tokens, webhook secrets, or passwords in shell', async () => {
      const user = userEvent.setup();
      const { container } = render(
        <MemoryRouter initialEntries={['/app']}>
          <AppShell>
            <div data-testid="test-content">Dashboard Content</div>
          </AppShell>
        </MemoryRouter>
      );

      // Open profile menu to inspect all rendered text
      const profileBtn = screen.getByRole('button', { name: /merchant account menu/i });
      await user.click(profileBtn);

      const html = container.innerHTML;
      expect(html).not.toMatch(/eyJ[a-zA-Z0-9_-]{10,}/); // No JWTs
      expect(html).not.toMatch(/whsec_[a-zA-Z0-9]+/); // No webhook secrets
      expect(html).not.toMatch(/rzp_live_[a-zA-Z0-9]{16,}/); // No full private live secrets
      expect(html).not.toMatch(/password/i);
    });
  });
});
