import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { NotificationsPage } from '../pages/notifications/NotificationsPage';
import * as notificationsApi from '../api/notifications';
import type {
  NotificationPageResponse,
  NotificationResponseDto,
} from '../types/notifications';

vi.mock('../api/notifications');
vi.mock('../hooks/useToast', () => ({
  useToast: () => ({
    toast: {
      success: vi.fn(),
      error: vi.fn(),
      info: vi.fn(),
    },
  }),
}));

const mockNotifications: NotificationPageResponse = {
  content: [
    {
      id: 'notif-1',
      merchantId: 'm-1',
      eventType: 'PAYMENT_RECOVERED',
      title: 'Payment Successfully Recovered',
      message: 'Case c-101 for customer Alice was recovered via WhatsApp recovery link.',
      status: 'UNREAD',
      read: false,
      recoveryCaseId: 'c-101',
      recoveryAttemptId: 'att-101',
      metadata: '{"amount": 4500, "currency": "INR"}',
      createdAt: '2026-08-30T12:00:00Z',
      updatedAt: '2026-08-30T12:00:00Z',
      deliveries: [
        {
          id: 'del-1',
          channel: 'EMAIL',
          provider: 'SendGrid',
          status: 'DELIVERED',
          attemptedAt: '2026-08-30T12:00:05Z',
          deliveredAt: '2026-08-30T12:00:10Z',
          errorCode: null,
          errorMessage: null,
          retryCount: 0,
        },
      ],
    },
    {
      id: 'notif-2',
      merchantId: 'm-1',
      eventType: 'HIGH_PRIORITY_FAILURE',
      title: 'High Priority Transaction Halted',
      message: 'Case c-102 encountered persistent authentication gateway error.',
      status: 'READ',
      read: true,
      recoveryCaseId: 'c-102',
      recoveryAttemptId: null,
      metadata: null,
      createdAt: '2026-08-30T11:00:00Z',
      updatedAt: '2026-08-30T11:30:00Z',
      deliveries: [],
    },
  ],
  totalElements: 2,
  totalPages: 1,
  number: 0,
  size: 10,
  first: true,
  last: true,
  empty: false,
};

describe('NotificationsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(notificationsApi.getNotifications).mockResolvedValue(mockNotifications);
    vi.mocked(notificationsApi.markAsRead).mockImplementation(async (id: string) => {
      const found = mockNotifications.content.find((n) => n.id === id);
      return {
        ...(found || mockNotifications.content[0]),
        id,
        read: true,
        status: 'READ',
      } as NotificationResponseDto;
    });
    vi.mocked(notificationsApi.markAllAsRead).mockResolvedValue({
      markedReadCount: 1,
      success: true,
    });
  });

  it('renders notification list with badges, titles, and preview text', async () => {
    render(
      <MemoryRouter>
        <NotificationsPage />
      </MemoryRouter>
    );

    expect(screen.getByText('Notifications')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('Payment Successfully Recovered')).toBeInTheDocument();
      expect(screen.getByText('High Priority Transaction Halted')).toBeInTheDocument();
      expect(screen.getAllByText('Payment Recovered').length).toBeGreaterThan(0);
      expect(screen.getAllByText('High Priority Failure').length).toBeGreaterThan(0);
      expect(screen.getByText('Unread')).toBeInTheDocument();
    });
  });

  it('toggles unread only filter when filter button clicked', async () => {
    render(
      <MemoryRouter>
        <NotificationsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Payment Successfully Recovered')).toBeInTheDocument();
    });

    const unreadButton = screen.getByRole('button', { name: /Unread Only/i });
    fireEvent.click(unreadButton);

    await waitFor(() => {
      expect(notificationsApi.getNotifications).toHaveBeenCalledWith(
        expect.objectContaining({
          unreadOnly: true,
        })
      );
    });
  });

  it('filters notifications by event type', async () => {
    render(
      <MemoryRouter>
        <NotificationsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Payment Successfully Recovered')).toBeInTheDocument();
    });

    const select = screen.getByLabelText(/Filter by event type/i);
    fireEvent.change(select, { target: { value: 'PAYMENT_RECOVERED' } });

    await waitFor(() => {
      expect(notificationsApi.getNotifications).toHaveBeenCalledWith(
        expect.objectContaining({
          event: 'PAYMENT_RECOVERED',
        })
      );
    });
  });

  it('marks a single notification as read on button click', async () => {
    render(
      <MemoryRouter>
        <NotificationsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Payment Successfully Recovered')).toBeInTheDocument();
    });

    const markReadButton = screen.getByRole('button', {
      name: /Mark "Payment Successfully Recovered" as read/i,
    });
    fireEvent.click(markReadButton);

    await waitFor(() => {
      expect(notificationsApi.markAsRead).toHaveBeenCalledWith('notif-1');
    });
  });

  it('marks all notifications as read on "Mark All Read" click', async () => {
    render(
      <MemoryRouter>
        <NotificationsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Payment Successfully Recovered')).toBeInTheDocument();
    });

    const markAllButton = screen.getByRole('button', { name: /Mark All Read/i });
    fireEvent.click(markAllButton);

    await waitFor(() => {
      expect(notificationsApi.markAllAsRead).toHaveBeenCalled();
    });
  });

  it('opens detail modal and displays delivery channels on selection', async () => {
    render(
      <MemoryRouter>
        <NotificationsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Payment Successfully Recovered')).toBeInTheDocument();
    });

    const viewButton = screen.getByRole('button', {
      name: /View details for Payment Successfully Recovered/i,
    });
    fireEvent.click(viewButton);

    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeInTheDocument();
      expect(screen.getByText(/Channel Deliveries/i)).toBeInTheDocument();
      expect(screen.getByText(/Associated Recovery Case/i)).toBeInTheDocument();
      expect(screen.getByText('Case ID: c-101')).toBeInTheDocument();
    });
  });

  it('renders empty state when no notifications are returned', async () => {
    vi.mocked(notificationsApi.getNotifications).mockResolvedValueOnce({
      content: [],
      totalElements: 0,
      totalPages: 0,
      number: 0,
      size: 10,
      first: true,
      last: true,
      empty: true,
    });

    render(
      <MemoryRouter>
        <NotificationsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('No Notifications Found')).toBeInTheDocument();
    });
  });

  it('renders error state and retries on failure', async () => {
    vi.mocked(notificationsApi.getNotifications).mockRejectedValueOnce(
      new Error('Failed to reach server')
    );

    render(
      <MemoryRouter>
        <NotificationsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Failed to Load Notifications')).toBeInTheDocument();
      expect(screen.getByText('Failed to reach server')).toBeInTheDocument();
    });

    vi.mocked(notificationsApi.getNotifications).mockResolvedValueOnce(mockNotifications);

    const retryButton = screen.getByRole('button', { name: /Try Again/i });
    fireEvent.click(retryButton);

    await waitFor(() => {
      expect(screen.getByText('Payment Successfully Recovered')).toBeInTheDocument();
    });
  });
});
