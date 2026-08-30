import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { Header } from '../components/layout/Header';
import * as notificationsApi from '../api/notifications';
import * as authApi from '../api/auth';

vi.mock('../api/notifications');
vi.mock('../api/auth', () => ({
  fetchHealth: vi.fn().mockResolvedValue({ status: 'UP', service: 'recover-ai-backend' }),
}));
vi.mock('../hooks/useAuth', () => ({
  useAuth: () => ({
    user: {
      id: 'm-123',
      name: 'Acme Merchant',
      email: 'acme@example.com',
      status: 'ACTIVE',
    },
    logout: vi.fn(),
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

describe('Header Notification Experience', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(authApi.fetchHealth).mockResolvedValue({ status: 'UP', service: 'recover-ai-backend' });
  });

  it('renders unread notification badge when unread notifications exist', async () => {
    vi.mocked(notificationsApi.getUnreadCount).mockResolvedValueOnce(4);

    render(
      <MemoryRouter initialEntries={['/app']}>
        <Header onOpenMobileMenu={() => {}} />
      </MemoryRouter>
    );

    await waitFor(() => {
      const badge = screen.getByTestId('notification-unread-badge');
      expect(badge).toBeInTheDocument();
      expect(badge).toHaveTextContent('4');
    });

    const notifLink = screen.getByRole('link', { name: /Notifications \(4 unread\)/i });
    expect(notifLink).toHaveAttribute('href', '/notifications');
  });

  it('does not render badge count when unread count is 0', async () => {
    vi.mocked(notificationsApi.getUnreadCount).mockResolvedValueOnce(0);

    render(
      <MemoryRouter initialEntries={['/app']}>
        <Header onOpenMobileMenu={() => {}} />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.queryByTestId('notification-unread-badge')).not.toBeInTheDocument();
    });

    const notifLink = screen.getByRole('link', { name: /^Notifications$/i });
    expect(notifLink).toHaveAttribute('href', '/notifications');
  });
});
