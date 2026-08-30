import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from '../context/AuthContext';
import { ToastProvider } from '../context/ToastContext';
import { LoginPage } from '../pages/auth/LoginPage';
import { RegisterPage } from '../pages/auth/RegisterPage';
import * as authApi from '../api/auth';
import type { AuthResponse, Merchant } from '../types/auth';
import { ApiError } from '../types/api';

const mockMerchant: Merchant = {
  id: 'b14f3eaf-abca-4d07-8f79-2fa47d508b35',
  name: 'Acme Payments',
  email: 'merchant@acme.com',
  razorpayAccountId: 'acc_1234567890',
  status: 'ACTIVE',
  createdAt: '2026-08-30T10:00:00Z',
  updatedAt: '2026-08-30T10:00:00Z',
};

const mockAuthResponse: AuthResponse = {
  token: 'mock-jwt-token-12345',
  tokenType: 'Bearer',
  expiresInMs: 86400000,
  merchant: mockMerchant,
};

function renderWithProviders(ui: React.ReactNode, { initialEntries = ['/login'] } = {}) {
  return render(
    <MemoryRouter initialEntries={initialEntries}>
      <ToastProvider>
        <AuthProvider>{ui}</AuthProvider>
      </ToastProvider>
    </MemoryRouter>
  );
}

describe('Authentication Flow & Pages', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('Login Page', () => {
    it('renders login form with accessible inputs and branding', () => {
      renderWithProviders(<LoginPage />);

      expect(screen.getByRole('heading', { name: /Sign in to RecoverAI/i })).toBeInTheDocument();
      expect(screen.getByLabelText(/Business Email/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/^Password/i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Sign In to Dashboard/i })).toBeInTheDocument();
      expect(screen.getByText(/Register your business/i)).toBeInTheDocument();
    });

    it('validates required fields and email format on submit', async () => {
      const user = userEvent.setup();
      renderWithProviders(<LoginPage />);

      const submitButton = screen.getByRole('button', { name: /Sign In to Dashboard/i });
      await user.click(submitButton);

      expect(screen.getByText(/Email address is required/i)).toBeInTheDocument();
      expect(screen.getByText(/Password is required/i)).toBeInTheDocument();

      // Enter invalid email
      const emailInput = screen.getByLabelText(/Business Email/i);
      await user.type(emailInput, 'invalid-email');
      await user.click(submitButton);

      expect(screen.getByText(/Please enter a valid email address/i)).toBeInTheDocument();
    });

    it('submits valid credentials and successfully logs in', async () => {
      const user = userEvent.setup();
      const loginSpy = vi.spyOn(authApi, 'login').mockResolvedValue(mockAuthResponse);

      renderWithProviders(
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/app" element={<div>App Dashboard Target</div>} />
        </Routes>
      );

      await user.type(screen.getByLabelText(/Business Email/i), 'merchant@acme.com');
      await user.type(screen.getByLabelText(/^Password/i), 'securePassword123');

      await user.click(screen.getByRole('button', { name: /Sign In to Dashboard/i }));

      await waitFor(() => {
        expect(loginSpy).toHaveBeenCalledWith({
          email: 'merchant@acme.com',
          password: 'securePassword123',
        });
        expect(screen.getByText('App Dashboard Target')).toBeInTheDocument();
      });

      expect(localStorage.getItem('recoverai_token')).toBe('mock-jwt-token-12345');
    });

    it('displays human-friendly error banner when login credentials fail', async () => {
      const user = userEvent.setup();
      vi.spyOn(authApi, 'login').mockRejectedValue(
        new ApiError(401, 'Invalid Credentials', 'Invalid email or password')
      );

      renderWithProviders(<LoginPage />);

      await user.type(screen.getByLabelText(/Business Email/i), 'wrong@acme.com');
      await user.type(screen.getByLabelText(/^Password/i), 'wrongPassword');

      await user.click(screen.getByRole('button', { name: /Sign In to Dashboard/i }));

      await waitFor(() => {
        expect(screen.getByRole('alert')).toBeInTheDocument();
        expect(
          screen.getByText(/Invalid email or password\. Please verify your credentials and try again\./i)
        ).toBeInTheDocument();
      });
    });

    it('toggles password visibility when show/hide button is clicked', async () => {
      const user = userEvent.setup();
      renderWithProviders(<LoginPage />);

      const passwordInput = screen.getByLabelText(/^Password/i);
      expect(passwordInput).toHaveAttribute('type', 'password');

      const toggleButton = screen.getByLabelText('Show password');
      await user.click(toggleButton);

      expect(passwordInput).toHaveAttribute('type', 'text');
      expect(screen.getByLabelText('Hide password')).toBeInTheDocument();

      await user.click(screen.getByLabelText('Hide password'));
      expect(passwordInput).toHaveAttribute('type', 'password');
    });
  });

  describe('Registration Page', () => {
    it('renders registration form matching backend DTO fields', () => {
      renderWithProviders(<RegisterPage />, { initialEntries: ['/register'] });

      expect(
        screen.getByRole('heading', { name: /Create your Merchant Account/i })
      ).toBeInTheDocument();
      expect(screen.getByLabelText(/Merchant \/ Business Name/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Business Email/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Razorpay Account ID/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/^Password/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Confirm Password/i)).toBeInTheDocument();
      expect(
        screen.getByRole('button', { name: /Complete Registration & Access Portal/i })
      ).toBeInTheDocument();
    });

    it('validates password length and password match', async () => {
      const user = userEvent.setup();
      renderWithProviders(<RegisterPage />, { initialEntries: ['/register'] });

      const submitButton = screen.getByRole('button', {
        name: /Complete Registration & Access Portal/i,
      });

      await user.type(screen.getByLabelText(/Merchant \/ Business Name/i), 'A');
      await user.type(screen.getByLabelText(/Business Email/i), 'valid@acme.com');
      await user.type(screen.getByLabelText(/^Password/i), 'short');
      await user.type(screen.getByLabelText(/Confirm Password/i), 'mismatch');

      await user.click(submitButton);

      expect(screen.getByText(/Name must be at least 2 characters/i)).toBeInTheDocument();
      expect(screen.getByText(/Password must be at least 8 characters/i)).toBeInTheDocument();
      expect(screen.getByText(/Passwords do not match/i)).toBeInTheDocument();
    });

    it('submits valid registration and auto-logs in', async () => {
      const user = userEvent.setup();
      const registerSpy = vi.spyOn(authApi, 'register').mockResolvedValue(mockMerchant);
      const loginSpy = vi.spyOn(authApi, 'login').mockResolvedValue(mockAuthResponse);

      renderWithProviders(
        <Routes>
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/app" element={<div>Registered Dashboard</div>} />
        </Routes>,
        { initialEntries: ['/register'] }
      );

      await user.type(screen.getByLabelText(/Merchant \/ Business Name/i), 'Acme Payments');
      await user.type(screen.getByLabelText(/Business Email/i), 'merchant@acme.com');
      await user.type(screen.getByLabelText(/Razorpay Account ID/i), 'acc_1234567890');
      await user.type(screen.getByLabelText(/^Password/i), 'securePassword123');
      await user.type(screen.getByLabelText(/Confirm Password/i), 'securePassword123');

      await user.click(
        screen.getByRole('button', { name: /Complete Registration & Access Portal/i })
      );

      await waitFor(() => {
        expect(registerSpy).toHaveBeenCalledWith({
          name: 'Acme Payments',
          email: 'merchant@acme.com',
          password: 'securePassword123',
          razorpayAccountId: 'acc_1234567890',
        });
        expect(loginSpy).toHaveBeenCalledWith({
          email: 'merchant@acme.com',
          password: 'securePassword123',
        });
        expect(screen.getByText('Registered Dashboard')).toBeInTheDocument();
      });
    });

    it('displays error banner when duplicate email error occurs on registration', async () => {
      const user = userEvent.setup();
      vi.spyOn(authApi, 'register').mockRejectedValue(
        new ApiError(409, 'Duplicate Merchant', 'Merchant with email already exists')
      );

      renderWithProviders(<RegisterPage />, { initialEntries: ['/register'] });

      await user.type(screen.getByLabelText(/Merchant \/ Business Name/i), 'Acme Payments');
      await user.type(screen.getByLabelText(/Business Email/i), 'duplicate@acme.com');
      await user.type(screen.getByLabelText(/^Password/i), 'securePassword123');
      await user.type(screen.getByLabelText(/Confirm Password/i), 'securePassword123');

      await user.click(
        screen.getByRole('button', { name: /Complete Registration & Access Portal/i })
      );

      await waitFor(() => {
        expect(
          screen.getByText(/An account with this email address already exists/i)
        ).toBeInTheDocument();
      });
    });
  });
});
