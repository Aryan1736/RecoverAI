import { Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { LoginPage } from '../pages/auth/LoginPage';
import { RegisterPage } from '../pages/auth/RegisterPage';
import { OverviewPage } from '../pages/dashboard/OverviewPage';
import { AnalyticsPage } from '../pages/analytics/AnalyticsPage';
import { RecoveryCasesPage } from '../pages/recovery-cases/RecoveryCasesPage';
import { RecoveryCaseDetailPage } from '../pages/recovery-cases/RecoveryCaseDetailPage';
import { NotificationsPage } from '../pages/notifications/NotificationsPage';
import { SettingsLayout } from '../pages/settings/SettingsLayout';
import { GeneralSettingsPage } from '../pages/settings/GeneralSettingsPage';
import { NotificationSettingsPage } from '../pages/settings/NotificationSettingsPage';
import { ProviderSettingsPage } from '../pages/settings/ProviderSettingsPage';
import { NotFoundPage } from '../pages/NotFoundPage';
import { AppShell } from '../components/layout/AppShell';
import { Skeleton } from '../components/ui/Skeleton';
import type { ReactNode } from 'react';

function PageLoadingFallback() {
  return (
    <div className="min-h-screen bg-slate-950 flex flex-col items-center justify-center p-6 space-y-4">
      <div className="w-10 h-10 rounded-2xl bg-indigo-600/20 border border-indigo-500/30 flex items-center justify-center animate-pulse" />
      <Skeleton className="h-4 w-40" />
    </div>
  );
}

export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { isAuthenticated, isDemoMode, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return <PageLoadingFallback />;
  }

  if (!isAuthenticated && !isDemoMode) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return <>{children}</>;
}

export function PublicRoute({ children }: { children: ReactNode }) {
  const { isAuthenticated, isLoading } = useAuth();

  if (isLoading) {
    return <PageLoadingFallback />;
  }

  if (isAuthenticated) {
    return <Navigate to="/app" replace />;
  }

  return <>{children}</>;
}

export function AppRoutes() {
  return (
    <Routes>
      {/* Root redirect */}
      <Route path="/" element={<Navigate to="/app" replace />} />

      {/* Public Auth Routes */}
      <Route
        path="/login"
        element={
          <PublicRoute>
            <LoginPage />
          </PublicRoute>
        }
      />
      <Route
        path="/register"
        element={
          <PublicRoute>
            <RegisterPage />
          </PublicRoute>
        }
      />

      {/* Protected App Routes */}
      <Route
        path="/app"
        element={
          <ProtectedRoute>
            <AppShell>
              <OverviewPage />
            </AppShell>
          </ProtectedRoute>
        }
      />
      <Route
        path="/recovery-cases"
        element={
          <ProtectedRoute>
            <AppShell>
              <RecoveryCasesPage />
            </AppShell>
          </ProtectedRoute>
        }
      />
      <Route
        path="/recovery-cases/:id"
        element={
          <ProtectedRoute>
            <AppShell>
              <RecoveryCaseDetailPage />
            </AppShell>
          </ProtectedRoute>
        }
      />
      <Route
        path="/analytics"
        element={
          <ProtectedRoute>
            <AppShell>
              <AnalyticsPage />
            </AppShell>
          </ProtectedRoute>
        }
      />
      <Route
        path="/notifications"
        element={
          <ProtectedRoute>
            <AppShell>
              <NotificationsPage />
            </AppShell>
          </ProtectedRoute>
        }
      />
      <Route
        path="/settings"
        element={
          <ProtectedRoute>
            <AppShell>
              <SettingsLayout />
            </AppShell>
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to="/settings/general" replace />} />
        <Route path="general" element={<GeneralSettingsPage />} />
        <Route path="notifications" element={<NotificationSettingsPage />} />
        <Route path="providers" element={<ProviderSettingsPage />} />
      </Route>

      {/* Fallback 404 Route */}
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
