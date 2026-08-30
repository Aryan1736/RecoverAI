import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { AuthProvider } from '../context/AuthContext';
import { ToastProvider } from '../context/ToastContext';
import { AppRoutes } from '../routes/AppRoutes';
import { setStoredMerchant, setStoredToken } from '../api/client';
import type { Merchant } from '../types/auth';

const mockMerchant: Merchant = {
  id: '7bf08f22-b14f-42a6-88a5-7abaafe92c26',
  name: 'Acme Payments',
  email: 'merchant@acme.com',
  status: 'ACTIVE',
  createdAt: '2026-08-30T10:00:00Z',
  updatedAt: '2026-08-30T10:00:00Z',
};

function renderAppWithRoute(route: string) {
  return render(
    <MemoryRouter initialEntries={[route]}>
      <ToastProvider>
        <AuthProvider>
          <AppRoutes />
        </AuthProvider>
      </ToastProvider>
    </MemoryRouter>
  );
}

describe('Client-side Routing & Route Guards', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('redirects unauthenticated user accessing /app to /login', () => {
    renderAppWithRoute('/app');

    // Should redirect to /login and render login page
    expect(screen.getByRole('heading', { name: /Sign in to RecoverAI/i })).toBeInTheDocument();
    expect(screen.queryByText(/Welcome, Acme/i)).not.toBeInTheDocument();
  });

  it('redirects unauthenticated user accessing root / to /login', () => {
    renderAppWithRoute('/');

    expect(screen.getByRole('heading', { name: /Sign in to RecoverAI/i })).toBeInTheDocument();
  });

  it('allows authenticated merchant to access /app', () => {
    setStoredToken('valid-token-xyz');
    setStoredMerchant(mockMerchant);

    renderAppWithRoute('/app');

    expect(screen.getByText(/Welcome, Acme Payments/i)).toBeInTheDocument();
  });

  it('redirects authenticated merchant visiting /login to /app', () => {
    setStoredToken('valid-token-xyz');
    setStoredMerchant(mockMerchant);

    renderAppWithRoute('/login');

    expect(screen.getByText(/Welcome, Acme Payments/i)).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: /Sign in to RecoverAI/i })).not.toBeInTheDocument();
  });

  it('redirects authenticated merchant visiting /register to /app', () => {
    setStoredToken('valid-token-xyz');
    setStoredMerchant(mockMerchant);

    renderAppWithRoute('/register');

    expect(screen.getByText(/Welcome, Acme Payments/i)).toBeInTheDocument();
  });

  it('renders 404 page for nonexistent route', () => {
    renderAppWithRoute('/non-existent-page');

    expect(screen.getByText('404')).toBeInTheDocument();
    expect(screen.getByText('Page Not Found')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Back to Sign In/i })).toBeInTheDocument();
  });
});
