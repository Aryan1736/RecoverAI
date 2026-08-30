import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { DemoProvider } from '../context/DemoContext';
import { AuthProvider } from '../context/AuthContext';
import { ToastProvider } from '../context/ToastContext';
import { setStoredDemoMode } from '../context/demo-context-def';
import { RecoveryCaseDetailPage } from '../pages/recovery-cases/RecoveryCaseDetailPage';
import { RecoveryCasesPage } from '../pages/recovery-cases/RecoveryCasesPage';
import { OverviewPage } from '../pages/dashboard/OverviewPage';
import { AnalyticsPage } from '../pages/analytics/AnalyticsPage';
import { NotificationsPage } from '../pages/notifications/NotificationsPage';
import { Header } from '../components/layout/Header';
import { apiClient, TOKEN_STORAGE_KEY } from '../api/client';
import {
  resetDemoStore,
  getDemoDashboard,
  getDemoRecoveryCases,
  getDemoRecoveryCase,
  simulateDemoRecovery,
  getDemoAnalyticsOverview,
  getDemoNotifications,
  getDemoUnreadCount,
  markDemoNotificationRead,
} from '../api/demo';

describe('PR #24: Complete Interactive Demo Workflow', () => {
  beforeEach(() => {
    localStorage.clear();
    resetDemoStore();
    setStoredDemoMode(true);
    vi.clearAllMocks();
  });

  afterEach(() => {
    localStorage.clear();
    resetDemoStore();
  });

  describe('1. Realistic Demo Dataset & Internal Consistency', () => {
    it('initializes with a realistic 10-case dataset spanning all required states and categories', async () => {
      const casesResponse = await getDemoRecoveryCases();
      expect(casesResponse.totalElements).toBe(10);
      expect(casesResponse.content).toHaveLength(10);

      const statuses = new Set(casesResponse.content.map((c) => c.status));
      expect(statuses.has('OPEN')).toBe(true);
      expect(statuses.has('IN_PROGRESS')).toBe(true);
      expect(statuses.has('RECOVERED')).toBe(true);
      expect(statuses.has('FAILED')).toBe(true);
      expect(statuses.has('CANCELLED')).toBe(true);
      expect(statuses.has('EXPIRED')).toBe(true);

      const categories = new Set(casesResponse.content.map((c) => c.failureReasonCategory));
      expect(categories.has('AUTHENTICATION')).toBe(true);
      expect(categories.has('INSUFFICIENT_FUNDS')).toBe(true);
      expect(categories.has('NETWORK_TIMEOUT')).toBe(true);
      expect(categories.has('USER_DROPOFF')).toBe(true);
      expect(categories.has('BANK_DECLINED')).toBe(true);
      expect(categories.has('CARD_EXPIRED')).toBe(true);

      const priorities = new Set(casesResponse.content.map((c) => c.priority));
      expect(priorities.has('CRITICAL')).toBe(true);
      expect(priorities.has('HIGH')).toBe(true);
      expect(priorities.has('MEDIUM')).toBe(true);
      expect(priorities.has('LOW')).toBe(true);
    });

    it('each case contains consistent customer, payment, AI diagnosis, and strategy data', async () => {
      const caseDetail = await getDemoRecoveryCase('demo-case-001');

      expect(caseDetail.id).toBe('demo-case-001');
      expect(caseDetail.customer).toBeDefined();
      expect(caseDetail.customer?.name).toBe('Aarav Sharma (Simulated)');
      expect(caseDetail.customer?.email).toBe('aarav.sharma@example.com');
      expect(caseDetail.customer?.phone).toBe('+919876543210');

      expect(caseDetail.payment).toBeDefined();
      expect(caseDetail.payment?.status).toBe('FAILED');
      expect(caseDetail.payment?.amount).toBe(4999.0);
      expect(caseDetail.payment?.currency).toBe('INR');
      expect(caseDetail.payment?.razorpayPaymentId).toBe('pay_sim_8912');
      expect(caseDetail.payment?.errorCode).toBe('BAD_REQUEST_ERROR');

      expect(caseDetail.latestDiagnosis).toBeDefined();
      expect(caseDetail.latestDiagnosis?.confidenceScore).toBe(0.92);
      expect(caseDetail.latestDiagnosis?.channel).toBe('WHATSAPP');
      expect(caseDetail.latestDiagnosis?.reasoning).toContain('3D Secure / MPIN authentication');

      expect(caseDetail.attempts).toHaveLength(1);
      expect(caseDetail.attempts[0].channel).toBe('WHATSAPP');
      expect(caseDetail.attempts[0].status).toBe('DELIVERED');
      expect(caseDetail.attempts[0].strategySnapshot?.fallbackChannel).toBe('EMAIL');
    });
  });

  describe('2. Recovery Case Detail Workflow', () => {
    function renderCaseDetail(caseId: string) {
      return render(
        <MemoryRouter initialEntries={[`/recovery-cases/${caseId}`]}>
          <ToastProvider>
            <DemoProvider>
              <AuthProvider>
                <Routes>
                  <Route path="/recovery-cases/:id" element={<RecoveryCaseDetailPage />} />
                </Routes>
              </AuthProvider>
            </DemoProvider>
          </ToastProvider>
        </MemoryRouter>
      );
    }

    it('renders payment, AI diagnosis, recovery strategy, attempts, and timeline for an in-progress case', async () => {
      renderCaseDetail('demo-case-001');

      // Header & Breadcrumb
      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /demo-case-001/i })).toBeInTheDocument();
      });
      expect(screen.getAllByText('IN_PROGRESS').length).toBeGreaterThan(0);
      expect(screen.getByText('HIGH')).toBeInTheDocument();

      // Payment Details Card
      expect(screen.getByText('Payment Details')).toBeInTheDocument();
      expect(screen.getAllByText('FAILED').length).toBeGreaterThan(0);
      expect(screen.getByText('pay_sim_8912')).toBeInTheDocument();

      // AI Failure Diagnosis Card
      expect(screen.getByText('AI Diagnosis & Reasoning')).toBeInTheDocument();
      expect(screen.getAllByText(/92% Confidence/i).length).toBeGreaterThan(0);
      expect(screen.getByText(/Simulated AI Data/i)).toBeInTheDocument();
      expect(screen.getAllByText(/Send 1-click UPI recovery link via WhatsApp/i).length).toBeGreaterThan(0);
      expect(screen.getAllByText(/3D Secure \/ MPIN authentication timeout/i).length).toBeGreaterThan(0);

      // Recovery Strategy Card
      expect(screen.getByText('Recovery Strategy')).toBeInTheDocument();
      expect(screen.getByText(/Delay: Immediate/i)).toBeInTheDocument();
      expect(screen.getByText(/Fallback Channel/i)).toBeInTheDocument();
      expect(screen.getByText(/Delay: 2 Hours/i)).toBeInTheDocument();

      // Recovery Attempts & Timeline
      expect(screen.getByText(/Execution Timeline & Attempts/i)).toBeInTheDocument();
      expect(screen.getByText(/Attempt #1: WHATSAPP/i)).toBeInTheDocument();
      expect(screen.getAllByText('DELIVERED').length).toBeGreaterThan(0);
      expect(screen.getByText(/Smart Payment Link delivered via WhatsApp Business API/i)).toBeInTheDocument();

      // Interactive Simulation CTAs are visible for eligible case
      expect(screen.getByTestId('simulate-recovery-btn')).toBeInTheDocument();
      expect(screen.getByTestId('simulate-recovery-banner-btn')).toBeInTheDocument();
    });
  });

  describe('3. Simulate Customer Recovery Action', () => {
    it('simulates recovery: transitions payment FAILED -> CAPTURED, case -> RECOVERED, attempt -> SUCCESS, generates notification', async () => {
      const user = userEvent.setup();

      render(
        <MemoryRouter initialEntries={['/recovery-cases/demo-case-001']}>
          <ToastProvider>
            <DemoProvider>
              <AuthProvider>
                <Routes>
                  <Route path="/recovery-cases/:id" element={<RecoveryCaseDetailPage />} />
                </Routes>
              </AuthProvider>
            </DemoProvider>
          </ToastProvider>
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getByTestId('simulate-recovery-btn')).toBeInTheDocument();
      });

      // Click "Simulate Customer Recovery"
      await user.click(screen.getByTestId('simulate-recovery-btn'));

      // Modal appears
      expect(screen.getByRole('dialog')).toBeInTheDocument();
      expect(screen.getByText('Target Case:')).toBeInTheDocument();
      expect(screen.getByText('Customer accessed recovery link')).toBeInTheDocument();

      // Click Confirm & Simulate
      const confirmBtn = screen.getByTestId('confirm-simulation-btn');
      await user.click(confirmBtn);

      // Wait for simulation completion
      await waitFor(() => {
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      });

      // Assert page UI updated to RECOVERED and CAPTURED
      await waitFor(() => {
        expect(screen.getAllByText('RECOVERED').length).toBeGreaterThan(0);
        expect(screen.getAllByText('CAPTURED').length).toBeGreaterThan(0);
        expect(screen.getAllByText('SUCCESS').length).toBeGreaterThan(0);
      });

      // Button is now updated to "Simulation Unavailable (RECOVERED)"
      expect(screen.getByTestId('simulate-recovery-disabled-btn')).toBeInTheDocument();
      expect(screen.queryByTestId('simulate-recovery-btn')).not.toBeInTheDocument();

      // Verify central demo store was updated
      const updatedCase = await getDemoRecoveryCase('demo-case-001');
      expect(updatedCase.status).toBe('RECOVERED');
      expect(updatedCase.recoveredAmount).toBe(4999.0);
      expect(updatedCase.payment?.status).toBe('CAPTURED');
      expect(updatedCase.attempts[0].status).toBe('SUCCESS');
      expect(updatedCase.recoveredAt).toBeDefined();

      // Verify PAYMENT_RECOVERED notification was generated in central store
      const notifs = await getDemoNotifications();
      const recoveryNotif = notifs.content.find(
        (n) => n.recoveryCaseId === 'demo-case-001' && n.eventType === 'PAYMENT_RECOVERED'
      );
      expect(recoveryNotif).toBeDefined();
      expect(recoveryNotif?.title).toContain('Payment Successfully Recovered');
      expect(recoveryNotif?.message).toContain('₹4,999');
      expect(recoveryNotif?.read).toBe(false);
    });

    it('prevents duplicate PAYMENT_RECOVERED notifications if simulated again', async () => {
      // Direct store simulation verification
      const updated = await simulateDemoRecovery('demo-case-003');
      expect(updated.status).toBe('RECOVERED');

      const notifs1 = await getDemoNotifications();
      const count1 = notifs1.content.filter(
        (n) => n.recoveryCaseId === 'demo-case-003' && n.eventType === 'PAYMENT_RECOVERED'
      ).length;
      expect(count1).toBe(1);

      // Attempting to simulate a terminal case throws error and does not duplicate
      await expect(simulateDemoRecovery('demo-case-003')).rejects.toThrow(
        /terminal status "RECOVERED"/i
      );

      const notifs2 = await getDemoNotifications();
      const count2 = notifs2.content.filter(
        (n) => n.recoveryCaseId === 'demo-case-003' && n.eventType === 'PAYMENT_RECOVERED'
      ).length;
      expect(count2).toBe(1);
    });
  });

  describe('4. Terminal Case Protection', () => {
    it('disables simulation action for already RECOVERED case and displays terminal status', async () => {
      render(
        <MemoryRouter initialEntries={['/recovery-cases/demo-case-002']}>
          <ToastProvider>
            <DemoProvider>
              <AuthProvider>
                <Routes>
                  <Route path="/recovery-cases/:id" element={<RecoveryCaseDetailPage />} />
                </Routes>
              </AuthProvider>
            </DemoProvider>
          </ToastProvider>
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /demo-case-002/i })).toBeInTheDocument();
      });

      expect(screen.getByTestId('simulate-recovery-disabled-btn')).toBeInTheDocument();
      expect(screen.getByTestId('simulate-recovery-disabled-btn')).toBeDisabled();
      expect(screen.queryByTestId('simulate-recovery-btn')).not.toBeInTheDocument();
    });

    it('disables simulation for CANCELLED, EXPIRED, and FAILED cases', async () => {
      // Test CANCELLED case
      const resCancelled = await getDemoRecoveryCase('demo-case-007');
      expect(resCancelled.status).toBe('CANCELLED');
      await expect(simulateDemoRecovery('demo-case-007')).rejects.toThrow(/terminal status "CANCELLED"/i);

      // Test EXPIRED case
      const resExpired = await getDemoRecoveryCase('demo-case-009');
      expect(resExpired.status).toBe('EXPIRED');
      await expect(simulateDemoRecovery('demo-case-009')).rejects.toThrow(/terminal status "EXPIRED"/i);

      // Test FAILED/EXHAUSTED case
      const resFailed = await getDemoRecoveryCase('demo-case-006');
      expect(resFailed.status).toBe('FAILED');
      await expect(simulateDemoRecovery('demo-case-006')).rejects.toThrow(/terminal status "FAILED"/i);
    });
  });

  describe('5. Dashboard Synchronization', () => {
    it('synchronizes KPIs when a case is recovered in demo mode', async () => {
      // 1. Initial Dashboard Summary
      const initialKpi = await getDemoDashboard();
      expect(initialKpi.totalRecoveryCases).toBe(10);
      expect(initialKpi.recoveredCases).toBe(2);
      expect(initialKpi.inProgressCases).toBe(4);
      expect(initialKpi.totalRecoveredAmount).toBe(13399.0);

      // 2. Simulate recovery of demo-case-001 (₹4,999)
      await simulateDemoRecovery('demo-case-001');

      // 3. Updated Dashboard Summary
      const updatedKpi = await getDemoDashboard();
      expect(updatedKpi.totalRecoveryCases).toBe(10);
      expect(updatedKpi.recoveredCases).toBe(3);
      expect(updatedKpi.inProgressCases).toBe(3);
      expect(updatedKpi.totalRecoveredAmount).toBe(13399.0 + 4999.0);
      expect(updatedKpi.recoveryRate).toBeGreaterThan(initialKpi.recoveryRate);

      // 4. Render OverviewPage to verify UI renders updated metrics
      render(
        <MemoryRouter initialEntries={['/app']}>
          <ToastProvider>
            <DemoProvider>
              <AuthProvider>
                <OverviewPage />
              </AuthProvider>
            </DemoProvider>
          </ToastProvider>
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getAllByText('10').length).toBeGreaterThan(0); // total cases
      });
      expect(screen.getByText('DEMO ENVIRONMENT')).toBeInTheDocument();
    });
  });

  describe('6. Analytics Synchronization', () => {
    it('derives analytics dynamically from central demo store and updates after recovery', async () => {
      const initialOverview = await getDemoAnalyticsOverview();
      expect(initialOverview.totalCases).toBe(10);
      expect(initialOverview.recoveredCases).toBe(2);
      expect(initialOverview.totalRecoveredAmount).toBe(13399.0);

      // Recover demo-case-005 (₹8,450)
      await simulateDemoRecovery('demo-case-005');

      const updatedOverview = await getDemoAnalyticsOverview();
      expect(updatedOverview.recoveredCases).toBe(3);
      expect(updatedOverview.totalRecoveredAmount).toBe(13399.0 + 8450.0);
      expect(updatedOverview.recoveryRate).toBeGreaterThan(initialOverview.recoveryRate);

      // Verify no backend analytics APIs called
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
        expect(screen.getByText(/Analytics & Intelligence/i)).toBeInTheDocument();
      });

      const analyticsCalls = apiGetSpy.mock.calls.filter((c) =>
        c[0].includes('/api/v1/analytics')
      );
      expect(analyticsCalls).toHaveLength(0);
    });
  });

  describe('7. Notifications & Real-Time Header Synchronization', () => {
    it('increments unread count and generates notification when case is recovered', async () => {
      const initialUnread = await getDemoUnreadCount();
      expect(initialUnread).toBe(2); // demo-notif-001 and demo-notif-002 are UNREAD

      // Simulate recovery of demo-case-008
      await simulateDemoRecovery('demo-case-008');

      // Unread count increases to 3
      const updatedUnread = await getDemoUnreadCount();
      expect(updatedUnread).toBe(3);

      // Render NotificationsPage
      render(
        <MemoryRouter initialEntries={['/notifications']}>
          <ToastProvider>
            <DemoProvider>
              <AuthProvider>
                <NotificationsPage />
              </AuthProvider>
            </DemoProvider>
          </ToastProvider>
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getByText(/Payment Successfully Recovered \(Simulated\)/i)).toBeInTheDocument();
        expect(screen.getByText(/₹6,750/i)).toBeInTheDocument();
      });
    });

    it('marking notification as read updates central store and decrements unread count', async () => {
      const notifs = await getDemoNotifications();
      const unread = notifs.content.find((n) => !n.read);
      expect(unread).toBeDefined();

      const initialCount = await getDemoUnreadCount();
      await markDemoNotificationRead(unread!.id);

      const newCount = await getDemoUnreadCount();
      expect(newCount).toBe(initialCount - 1);
    });

    it('Header dynamically receives DEMO_STATE_EVENT and updates badge', async () => {
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

      // Initially 2 unread
      await waitFor(() => {
        expect(screen.getByTestId('notification-unread-badge')).toHaveTextContent('2');
      });

      // Simulate recovery in central store
      await simulateDemoRecovery('demo-case-001');

      // Header updates reactively to 3 without page refresh!
      await waitFor(() => {
        expect(screen.getByTestId('notification-unread-badge')).toHaveTextContent('3');
      });
    });
  });

  describe('8. Security & Sandbox Boundary Guarantees', () => {
    it('demo mode makes zero protected backend API calls and creates no JWT', async () => {
      const fetchSpy = vi.spyOn(window, 'fetch');

      render(
        <MemoryRouter initialEntries={['/recovery-cases']}>
          <ToastProvider>
            <DemoProvider>
              <AuthProvider>
                <RecoveryCasesPage />
              </AuthProvider>
            </DemoProvider>
          </ToastProvider>
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /Recovery Cases/i })).toBeInTheDocument();
        expect(screen.getAllByRole('row').length).toBeGreaterThan(1);
      });

      // Zero calls to /api/v1/recovery-cases backend
      const backendCalls = fetchSpy.mock.calls.filter((c) =>
        String(c[0]).includes('/api/v1/recovery-cases')
      );
      expect(backendCalls).toHaveLength(0);

      // Security check: no JWT stored
      expect(localStorage.getItem(TOKEN_STORAGE_KEY)).toBeNull();
    });
  });
});
