import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { DemoProvider } from '../context/DemoContext';
import {
  getStoredDemoMode,
  setStoredDemoMode,
  DEMO_STORAGE_KEY,
} from '../context/demo-context-def';
import { AuthProvider } from '../context/AuthContext';
import { ToastProvider } from '../context/ToastContext';
import { useAuth } from '../hooks/useAuth';
import { useDemoMode } from '../hooks/useDemoMode';
import { AppRoutes } from '../routes/AppRoutes';
import { LoginPage } from '../pages/auth/LoginPage';
import { AnalyticsPage } from '../pages/analytics/AnalyticsPage';
import { Header } from '../components/layout/Header';
import { DemoModeBadge } from '../components/layout/DemoModeBadge';
import { apiClient, TOKEN_STORAGE_KEY, MERCHANT_STORAGE_KEY, setStoredToken, setStoredMerchant } from '../api/client';
import * as authApi from '../api/auth';
import * as analyticsApi from '../api/analytics';
import type { Merchant } from '../types/auth';

const mockMerchant: Merchant = {
  id: 'merchant-test-123',
  name: 'Acme Payments',
  email: 'merchant@acme.com',
  status: 'ACTIVE',
  createdAt: '2026-08-30T10:00:00Z',
  updatedAt: '2026-08-30T10:00:00Z',
};

function DemoConsumerComponent() {
  const { isDemoMode, enterDemoMode, exitDemoMode } = useDemoMode();
  const { isAuthenticated } = useAuth();
  return (
    <div>
      <span data-testid="demo-status">{isDemoMode ? 'DEMO_ACTIVE' : 'DEMO_INACTIVE'}</span>
      <span data-testid="auth-status">{isAuthenticated ? 'AUTH_ACTIVE' : 'AUTH_INACTIVE'}</span>
      <button onClick={enterDemoMode}>Enter Demo</button>
      <button onClick={exitDemoMode}>Exit Demo</button>
    </div>
  );
}

describe('PR #23: Interactive Demo Mode', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  afterEach(() => {
    localStorage.clear();
  });

  describe('1. Demo Session Lifecycle & Persistence', () => {
    it('initializes with demo mode disabled by default', () => {
      render(
        <DemoProvider>
          <AuthProvider>
            <DemoConsumerComponent />
          </AuthProvider>
        </DemoProvider>
      );

      expect(screen.getByTestId('demo-status')).toHaveTextContent('DEMO_INACTIVE');
      expect(screen.getByTestId('auth-status')).toHaveTextContent('AUTH_INACTIVE');
      expect(getStoredDemoMode()).toBe(false);
      expect(localStorage.getItem(TOKEN_STORAGE_KEY)).toBeNull();
      expect(localStorage.getItem(MERCHANT_STORAGE_KEY)).toBeNull();
    });

    it('enters demo mode and persists in localStorage without fake JWT', async () => {
      const user = userEvent.setup();

      render(
        <DemoProvider>
          <AuthProvider>
            <DemoConsumerComponent />
          </AuthProvider>
        </DemoProvider>
      );

      await user.click(screen.getByRole('button', { name: /Enter Demo/i }));

      expect(screen.getByTestId('demo-status')).toHaveTextContent('DEMO_ACTIVE');
      expect(screen.getByTestId('auth-status')).toHaveTextContent('AUTH_INACTIVE');
      expect(getStoredDemoMode()).toBe(true);
      expect(localStorage.getItem(DEMO_STORAGE_KEY)).toBe('true');

      // Crucial security guarantee: NO fake token or merchant stored
      expect(localStorage.getItem(TOKEN_STORAGE_KEY)).toBeNull();
      expect(localStorage.getItem(MERCHANT_STORAGE_KEY)).toBeNull();
    });

    it('restores demo mode from localStorage on provider initialization (page refresh simulation)', () => {
      setStoredDemoMode(true);
      expect(localStorage.getItem(DEMO_STORAGE_KEY)).toBe('true');

      render(
        <DemoProvider>
          <AuthProvider>
            <DemoConsumerComponent />
          </AuthProvider>
        </DemoProvider>
      );

      expect(screen.getByTestId('demo-status')).toHaveTextContent('DEMO_ACTIVE');
      expect(screen.getByTestId('auth-status')).toHaveTextContent('AUTH_INACTIVE');
      expect(localStorage.getItem(TOKEN_STORAGE_KEY)).toBeNull();
    });

    it('exits demo mode and clears localStorage cleanly', async () => {
      const user = userEvent.setup();
      setStoredDemoMode(true);

      render(
        <DemoProvider>
          <AuthProvider>
            <DemoConsumerComponent />
          </AuthProvider>
        </DemoProvider>
      );

      expect(screen.getByTestId('demo-status')).toHaveTextContent('DEMO_ACTIVE');

      await user.click(screen.getByRole('button', { name: /Exit Demo/i }));

      expect(screen.getByTestId('demo-status')).toHaveTextContent('DEMO_INACTIVE');
      expect(getStoredDemoMode()).toBe(false);
      expect(localStorage.getItem(DEMO_STORAGE_KEY)).toBeNull();
    });

    it('clears demo mode when a real merchant signs in', async () => {
      setStoredDemoMode(true);

      vi.spyOn(authApi, 'login').mockResolvedValueOnce({
        token: 'real-jwt-token-12345',
        tokenType: 'Bearer',
        expiresInMs: 86400000,
        merchant: mockMerchant,
      });

      function TestLoginTrigger() {
        const { login, isDemoMode, isAuthenticated } = useAuth();
        return (
          <div>
            <span data-testid="demo-status">{isDemoMode ? 'DEMO_ACTIVE' : 'DEMO_INACTIVE'}</span>
            <span data-testid="auth-status">{isAuthenticated ? 'AUTH_ACTIVE' : 'AUTH_INACTIVE'}</span>
            <button
              onClick={() => login({ email: 'merchant@acme.com', password: 'ValidPassword123!' })}
            >
              Sign In Real Merchant
            </button>
          </div>
        );
      }

      const user = userEvent.setup();

      render(
        <DemoProvider>
          <AuthProvider>
            <TestLoginTrigger />
          </AuthProvider>
        </DemoProvider>
      );

      // Initially in demo mode
      expect(screen.getByTestId('demo-status')).toHaveTextContent('DEMO_ACTIVE');
      expect(screen.getByTestId('auth-status')).toHaveTextContent('AUTH_INACTIVE');

      // Perform real login
      await user.click(screen.getByRole('button', { name: /Sign In Real Merchant/i }));

      await waitFor(() => {
        expect(screen.getByTestId('auth-status')).toHaveTextContent('AUTH_ACTIVE');
        expect(screen.getByTestId('demo-status')).toHaveTextContent('DEMO_INACTIVE');
      });

      expect(localStorage.getItem(DEMO_STORAGE_KEY)).toBeNull();
      expect(localStorage.getItem(TOKEN_STORAGE_KEY)).toBe('real-jwt-token-12345');
    });
  });

  describe('2. Routing Guards & Protected Frontend Access', () => {
    function renderRouterApp(initialRoute: string) {
      return render(
        <MemoryRouter initialEntries={[initialRoute]}>
          <ToastProvider>
            <DemoProvider>
              <AuthProvider>
                <AppRoutes />
              </AuthProvider>
            </DemoProvider>
          </ToastProvider>
        </MemoryRouter>
      );
    }

    it('redirects unauthenticated visitor accessing /app to /login', () => {
      renderRouterApp('/app');

      expect(screen.getByRole('heading', { name: /Sign in to RecoverAI/i })).toBeInTheDocument();
      expect(screen.queryByText(/Welcome, Demo Evaluator/i)).not.toBeInTheDocument();
    });

    it('allows demo mode evaluator to access protected frontend routes (/app)', () => {
      setStoredDemoMode(true);

      renderRouterApp('/app');

      expect(screen.getByText(/Welcome, Demo Evaluator/i)).toBeInTheDocument();
      expect(screen.getAllByText(/DEMO ENVIRONMENT/i).length).toBeGreaterThan(0);
    });

    it('allows demo mode evaluator to access /recovery-cases without redirecting', async () => {
      setStoredDemoMode(true);

      renderRouterApp('/recovery-cases');

      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /Recovery Cases/i })).toBeInTheDocument();
      });
      expect(screen.queryByRole('heading', { name: /Sign in to RecoverAI/i })).not.toBeInTheDocument();
    });

    it('allows authenticated merchant to access protected routes normally', () => {
      setStoredToken('valid-merchant-token');
      setStoredMerchant(mockMerchant);

      renderRouterApp('/app');

      expect(screen.getByText(/Welcome, Acme Payments/i)).toBeInTheDocument();
      expect(screen.queryByText(/DEMO ENVIRONMENT/i)).not.toBeInTheDocument();
    });

    it('preserves public routes for demo mode evaluators without forced redirect', () => {
      setStoredDemoMode(true);

      renderRouterApp('/login');

      // Evaluator can still view /login page (for merchant sign-in or reviewing entry options)
      expect(screen.getByRole('heading', { name: /Sign in to RecoverAI/i })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Try Interactive Demo/i })).toBeInTheDocument();
    });
  });

  describe('3. Demo Entry Experience (LoginPage)', () => {
    it('renders "Try Interactive Demo" CTA with "No account required" badge', () => {
      render(
        <MemoryRouter>
          <ToastProvider>
            <DemoProvider>
              <AuthProvider>
                <LoginPage />
              </AuthProvider>
            </DemoProvider>
          </ToastProvider>
        </MemoryRouter>
      );

      const demoBtn = screen.getByRole('button', { name: /Try Interactive Demo/i });
      expect(demoBtn).toBeInTheDocument();
      expect(screen.getByText(/No account required/i)).toBeInTheDocument();
      expect(screen.getByText(/Simulated demo data/i)).toBeInTheDocument();
    });

    it('clicking "Try Interactive Demo" enters demo mode and does not call auth APIs', async () => {
      const loginSpy = vi.spyOn(authApi, 'login');
      const registerSpy = vi.spyOn(authApi, 'register');
      const user = userEvent.setup();

      function TestEntryWithApp() {
        return (
          <MemoryRouter initialEntries={['/login']}>
            <ToastProvider>
              <DemoProvider>
                <AuthProvider>
                  <AppRoutes />
                </AuthProvider>
              </DemoProvider>
            </ToastProvider>
          </MemoryRouter>
        );
      }

      render(<TestEntryWithApp />);

      const demoBtn = screen.getByRole('button', { name: /Try Interactive Demo/i });
      await user.click(demoBtn);

      await waitFor(() => {
        expect(screen.getByText(/Welcome, Demo Evaluator/i)).toBeInTheDocument();
      });

      // Assert zero backend authentication calls
      expect(loginSpy).not.toHaveBeenCalled();
      expect(registerSpy).not.toHaveBeenCalled();
      expect(getStoredDemoMode()).toBe(true);
      expect(localStorage.getItem(TOKEN_STORAGE_KEY)).toBeNull();
    });
  });

  describe('4. Demo Mode Header Badge & Exit Action', () => {
    it('renders DEMO MODE indicator badge with Exit Demo action when in demo mode', () => {
      setStoredDemoMode(true);

      render(
        <MemoryRouter>
          <ToastProvider>
            <DemoProvider>
              <AuthProvider>
                <Header onOpenMobileMenu={() => {}} />
              </AuthProvider>
            </DemoProvider>
          </ToastProvider>
        </MemoryRouter>
      );

      const badge = screen.getByRole('status', { name: /Interactive Demo Mode active/i });
      expect(badge).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Exit demo mode/i })).toBeInTheDocument();
      expect(screen.getByText(/Demo Evaluator/i)).toBeInTheDocument();
    });

    it('does not render DEMO MODE badge for authenticated real merchants', () => {
      setStoredToken('merchant-valid-token');
      setStoredMerchant(mockMerchant);

      render(
        <MemoryRouter>
          <ToastProvider>
            <DemoProvider>
              <AuthProvider>
                <Header onOpenMobileMenu={() => {}} />
              </AuthProvider>
            </DemoProvider>
          </ToastProvider>
        </MemoryRouter>
      );

      expect(screen.queryByRole('status', { name: /Interactive Demo Mode active/i })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /Exit demo mode/i })).not.toBeInTheDocument();
      expect(screen.getByText(/Acme Payments/i)).toBeInTheDocument();
    });

    it('clicking Exit Demo button clears demo state and redirects to /login', async () => {
      setStoredDemoMode(true);
      const user = userEvent.setup();

      render(
        <MemoryRouter initialEntries={['/app']}>
          <ToastProvider>
            <DemoProvider>
              <AuthProvider>
                <AppRoutes />
              </AuthProvider>
            </DemoProvider>
          </ToastProvider>
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getByText(/Welcome, Demo Evaluator/i)).toBeInTheDocument();
      });

      const exitBtn = screen.getByRole('button', { name: /Exit demo mode/i });
      await user.click(exitBtn);

      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /Sign in to RecoverAI/i })).toBeInTheDocument();
      });

      expect(getStoredDemoMode()).toBe(false);
      expect(localStorage.getItem(DEMO_STORAGE_KEY)).toBeNull();
    });
  });

  describe('5. Security & Authorization Guarantees', () => {
    it('demo mode never creates fake Authorization header in requests', async () => {
      setStoredDemoMode(true);

      const fetchSpy = vi.spyOn(window, 'fetch').mockResolvedValueOnce({
        ok: true,
        status: 200,
        headers: new Headers({ 'content-type': 'application/json' }),
        json: async () => ({ status: 'UP', service: 'recover-ai' }),
      } as Response);

      await apiClient.get('/api/v1/health');

      expect(fetchSpy).toHaveBeenCalled();
      const calledOptions = fetchSpy.mock.calls[0][1] as RequestInit;
      const headers = calledOptions.headers as Headers;

      // Ensure Authorization header was NOT set
      expect(headers.has('Authorization')).toBe(false);
    });

    it('real authenticated session continues attaching Bearer token unchanged', async () => {
      setStoredToken('test-auth-bearer-token');

      const fetchSpy = vi.spyOn(window, 'fetch').mockResolvedValueOnce({
        ok: true,
        status: 200,
        headers: new Headers({ 'content-type': 'application/json' }),
        json: async () => ({ success: true }),
      } as Response);

      await apiClient.get('/api/v1/test-endpoint');

      expect(fetchSpy).toHaveBeenCalled();
      const calledOptions = fetchSpy.mock.calls[0][1] as RequestInit;
      const headers = calledOptions.headers as Headers;

      expect(headers.get('Authorization')).toBe('Bearer test-auth-bearer-token');
    });
  });

  describe('6. Accessibility Semantics', () => {
    it('DemoModeBadge has status role and accessible exit action', () => {
      const exitFn = vi.fn();
      render(<DemoModeBadge onExit={exitFn} />);

      const badge = screen.getByRole('status');
      expect(badge).toHaveAttribute('aria-label', 'Interactive Demo Mode active');

      const exitBtn = screen.getByRole('button', { name: /Exit demo mode/i });
      expect(exitBtn).toBeInTheDocument();
    });

    it('interactive demo button on LoginPage is keyboard accessible and distinguishable', () => {
      render(
        <MemoryRouter>
          <ToastProvider>
            <DemoProvider>
              <AuthProvider>
                <LoginPage />
              </AuthProvider>
            </DemoProvider>
          </ToastProvider>
        </MemoryRouter>
      );

      const demoBtn = screen.getByRole('button', { name: /Try Interactive Demo/i });
      expect(demoBtn).toBeVisible();
      expect(demoBtn).not.toBeDisabled();
      expect(demoBtn.tagName.toLowerCase()).toBe('button');
    });
  });

  describe('7. Analytics in Demo Mode vs Authenticated Mode', () => {
    it('demo mode renders analytics without calling backend analytics APIs', async () => {
      setStoredDemoMode(true);
      const apiGetSpy = vi.spyOn(apiClient, 'get');

      render(
        <MemoryRouter initialEntries={['/analytics']}>
          <ToastProvider>
            <DemoProvider>
              <AuthProvider>
                <AnalyticsPage />
              </AuthProvider>
            </DemoProvider>
          </ToastProvider>
        </MemoryRouter>
      );

      // Wait for analytics data to render
      await waitFor(() => {
        expect(screen.getByText(/Analytics & Intelligence/i)).toBeInTheDocument();
        expect(screen.getByText('38')).toBeInTheDocument(); // total cases from DEMO_ANALYTICS_OVERVIEW
      });

      // Assert error state is NOT shown
      expect(screen.queryByText(/Failed to Load Analytics/i)).not.toBeInTheDocument();

      // Assert zero calls were made to real backend analytics endpoints
      const analyticsCalls = apiGetSpy.mock.calls.filter((call) =>
        call[0].includes('/api/v1/analytics')
      );
      expect(analyticsCalls).toHaveLength(0);
    });

    it('changing date range in demo mode does not trigger backend analytics requests', async () => {
      setStoredDemoMode(true);
      const user = userEvent.setup();
      const apiGetSpy = vi.spyOn(apiClient, 'get');

      render(
        <MemoryRouter initialEntries={['/analytics']}>
          <ToastProvider>
            <DemoProvider>
              <AuthProvider>
                <AnalyticsPage />
              </AuthProvider>
            </DemoProvider>
          </ToastProvider>
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getByText('38')).toBeInTheDocument();
      });

      // Click "Last 7 Days" preset
      const sevenDaysBtn = screen.getByRole('button', { name: /Last 7 Days/i });
      await user.click(sevenDaysBtn);

      // Verify zero backend calls
      const analyticsCalls = apiGetSpy.mock.calls.filter((call) =>
        call[0].includes('/api/v1/analytics')
      );
      expect(analyticsCalls).toHaveLength(0);
      expect(screen.queryByText(/Failed to Load Analytics/i)).not.toBeInTheDocument();
    });

    it('authenticated mode calls real backend analytics endpoints', async () => {
      setStoredToken('real-merchant-token');
      setStoredMerchant(mockMerchant);

      const overviewSpy = vi.spyOn(analyticsApi, 'getAnalyticsOverview').mockResolvedValueOnce({
        totalCases: 50,
        openCases: 5,
        inProgressCases: 5,
        recoveredCases: 40,
        failedCases: 0,
        expiredCases: 0,
        cancelledCases: 0,
        expiredOrCancelledCases: 0,
        totalEstimatedRecoverableAmount: 200000,
        totalRecoveredAmount: 180000,
        recoveryRate: 80,
        averageRecoveredAmount: 4500,
        averageTimeToRecoverySeconds: 1200,
        from: '2026-08-01',
        to: '2026-08-30',
      });
      const trendsSpy = vi.spyOn(analyticsApi, 'getRecoveryTrends').mockResolvedValueOnce({
        from: '2026-08-01',
        to: '2026-08-30',
        totalCases: 50,
        totalAmountAtRisk: 200000,
        totalRecoveredAmount: 180000,
        overallRecoveryRate: 80,
        trends: [],
      });
      const channelSpy = vi.spyOn(analyticsApi, 'getChannelAnalytics').mockResolvedValueOnce({
        from: '2026-08-01',
        to: '2026-08-30',
        totalAttempts: 20,
        channels: [],
      });
      const failureSpy = vi.spyOn(analyticsApi, 'getFailureAnalytics').mockResolvedValueOnce({
        from: '2026-08-01',
        to: '2026-08-30',
        totalCases: 50,
        categories: [],
        priorities: [],
      });

      render(
        <MemoryRouter initialEntries={['/analytics']}>
          <ToastProvider>
            <DemoProvider>
              <AuthProvider>
                <AnalyticsPage />
              </AuthProvider>
            </DemoProvider>
          </ToastProvider>
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(overviewSpy).toHaveBeenCalled();
        expect(trendsSpy).toHaveBeenCalled();
        expect(channelSpy).toHaveBeenCalled();
        expect(failureSpy).toHaveBeenCalled();
      });
    });
  });
});
