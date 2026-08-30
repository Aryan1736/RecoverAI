import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { ToastProvider } from '../context/ToastContext';
import { NotificationPreferencesMatrix } from '../components/settings/NotificationPreferencesMatrix';
import * as prefsApi from '../api/notification-preferences';
import type { NotificationPreferenceResponseDto } from '../types/notifications';

vi.mock('../api/notification-preferences');

const mockPersistedPrefs: NotificationPreferenceResponseDto = {
  merchantId: 'm-123',
  webhookUrl: 'https://api.merchant.com/webhook',
  preferences: {
    PAYMENT_RECOVERED: { EMAIL: true, WEBHOOK: true, IN_APP: true },
    CASE_EXHAUSTED: { EMAIL: true, WEBHOOK: false, IN_APP: true },
    HIGH_PRIORITY_FAILURE: { EMAIL: true, WEBHOOK: true, IN_APP: true },
    PROVIDER_DEGRADED: { EMAIL: false, WEBHOOK: false, IN_APP: true },
  },
};

function renderMatrix() {
  return render(
    <MemoryRouter>
      <ToastProvider>
        <NotificationPreferencesMatrix />
      </ToastProvider>
    </MemoryRouter>
  );
}

describe('NotificationPreferencesMatrix', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(prefsApi.getNotificationPreferences).mockResolvedValue(mockPersistedPrefs);
    vi.mocked(prefsApi.updateNotificationPreferences).mockImplementation(async (payload) => ({
      merchantId: 'm-123',
      webhookUrl: payload.webhookUrl || null,
      preferences: payload.preferences,
    }));
  });

  it('renders event matrix, channel columns, and webhook input', async () => {
    renderMatrix();

    await waitFor(() => {
      expect(screen.getByText('Event Channel Matrix')).toBeInTheDocument();
      expect(screen.getByText('Payment Recovered')).toBeInTheDocument();
      expect(screen.getByText('Case Exhausted')).toBeInTheDocument();
      expect(screen.getByText('High Priority Failure')).toBeInTheDocument();
      expect(screen.getByText('Provider Degraded')).toBeInTheDocument();
      expect(screen.getByText('Email')).toBeInTheDocument();
      expect(screen.getByText('Webhook')).toBeInTheDocument();
      expect(screen.getByText('In-App')).toBeInTheDocument();
      expect(screen.getByDisplayValue('https://api.merchant.com/webhook')).toBeInTheDocument();
    });
  });

  it('detects dirty state when a preference switch is toggled', async () => {
    renderMatrix();

    await waitFor(() => {
      expect(screen.getByText('Persisted')).toBeInTheDocument();
    });

    const saveButton = screen.getByRole('button', { name: /Save Changes/i });
    const resetButton = screen.getByRole('button', { name: /Reset/i });
    expect(saveButton).toBeDisabled();
    expect(resetButton).toBeDisabled();

    // Toggle Webhook for Case Exhausted
    const switchControl = screen.getByRole('switch', {
      name: /Enable Webhook for Case Exhausted/i,
    });
    fireEvent.click(switchControl);

    await waitFor(() => {
      expect(screen.getByText('Unsaved Changes')).toBeInTheDocument();
      expect(saveButton).not.toBeDisabled();
      expect(resetButton).not.toBeDisabled();
    });
  });

  it('resets changes when Reset button is clicked', async () => {
    renderMatrix();

    await waitFor(() => {
      expect(screen.getByText('Persisted')).toBeInTheDocument();
    });

    const input = screen.getByDisplayValue('https://api.merchant.com/webhook');
    fireEvent.change(input, { target: { value: 'https://new-url.com/hook' } });

    await waitFor(() => {
      expect(screen.getByText('Unsaved Changes')).toBeInTheDocument();
    });

    const resetButton = screen.getByRole('button', { name: /Reset/i });
    fireEvent.click(resetButton);

    await waitFor(() => {
      expect(screen.getByDisplayValue('https://api.merchant.com/webhook')).toBeInTheDocument();
      expect(screen.getByText('Persisted')).toBeInTheDocument();
    });
  });

  it('saves preferences and displays success toast', async () => {
    renderMatrix();

    await waitFor(() => {
      expect(screen.getByText('Persisted')).toBeInTheDocument();
    });

    // Toggle email for Provider Degraded
    const switchControl = screen.getByRole('switch', {
      name: /Enable Email for Provider Degraded/i,
    });
    fireEvent.click(switchControl);

    const saveButton = screen.getByRole('button', { name: /Save Changes/i });
    fireEvent.click(saveButton);

    await waitFor(() => {
      expect(prefsApi.updateNotificationPreferences).toHaveBeenCalledWith(
        expect.objectContaining({
          preferences: expect.objectContaining({
            PROVIDER_DEGRADED: expect.objectContaining({
              EMAIL: true,
            }),
          }),
        })
      );
      expect(screen.getByText('Persisted')).toBeInTheDocument();
    });
  });

  it('validates invalid webhook URL format', async () => {
    renderMatrix();

    await waitFor(() => {
      expect(screen.getByDisplayValue('https://api.merchant.com/webhook')).toBeInTheDocument();
    });

    const input = screen.getByDisplayValue('https://api.merchant.com/webhook');
    fireEvent.change(input, { target: { value: 'ftp://invalid-url.com' } });

    await waitFor(() => {
      expect(screen.getByText(/URL must use HTTP or HTTPS protocol/i)).toBeInTheDocument();
    });
  });
});
