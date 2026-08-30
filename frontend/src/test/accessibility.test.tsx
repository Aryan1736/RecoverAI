import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { LoginPage } from '../pages/auth/LoginPage';
import { RegisterPage } from '../pages/auth/RegisterPage';
import { AuthProvider } from '../context/AuthContext';
import { ToastProvider } from '../context/ToastContext';

describe('Accessibility & Keyboard Navigation', () => {
  it('ensures all login form fields have programmatic labels', () => {
    render(
      <MemoryRouter>
        <ToastProvider>
          <AuthProvider>
            <LoginPage />
          </AuthProvider>
        </ToastProvider>
      </MemoryRouter>
    );

    const emailInput = screen.getByLabelText(/Business Email/i);
    const passwordInput = screen.getByLabelText(/^Password/i);
    const submitButton = screen.getByRole('button', { name: /Sign In to Dashboard/i });

    expect(emailInput).toHaveAttribute('id');
    expect(passwordInput).toHaveAttribute('id');
    expect(submitButton).toHaveAttribute('type', 'submit');
  });

  it('ensures all registration form fields have programmatic labels', () => {
    render(
      <MemoryRouter>
        <ToastProvider>
          <AuthProvider>
            <RegisterPage />
          </AuthProvider>
        </ToastProvider>
      </MemoryRouter>
    );

    expect(screen.getByLabelText(/Merchant \/ Business Name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Business Email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Razorpay Account ID/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^Password/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Confirm Password/i)).toBeInTheDocument();
  });

  it('supports full keyboard tab navigation through login form', async () => {
    const user = userEvent.setup();

    render(
      <MemoryRouter>
        <ToastProvider>
          <AuthProvider>
            <LoginPage />
          </AuthProvider>
        </ToastProvider>
      </MemoryRouter>
    );

    const emailInput = screen.getByLabelText(/Business Email/i);
    const passwordInput = screen.getByLabelText(/^Password/i);
    const togglePasswordBtn = screen.getByLabelText('Show password');
    const submitButton = screen.getByRole('button', { name: /Sign In to Dashboard/i });

    // Focus starts outside
    await user.tab();
    expect(emailInput).toHaveFocus();

    await user.tab();
    expect(passwordInput).toHaveFocus();

    await user.tab();
    expect(togglePasswordBtn).toHaveFocus();

    await user.tab();
    expect(submitButton).toHaveFocus();
  });
});
