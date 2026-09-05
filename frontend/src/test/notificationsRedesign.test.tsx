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

const mockRedesignNotifications: NotificationPageResponse = {
  content: [
    {
      id: 'notif-redesign-1',
      merchantId: 'm-1',
      eventType: 'PAYMENT_RECOVERED',
      title: 'Payment Successfully Recovered via WhatsApp',
      message: 'Payment of ₹4,999 for customer Aarav Sharma was recovered following an automated WhatsApp recovery prompt.',
      status: 'UNREAD',
      read: false,
      recoveryCaseId: 'demo-case-001',
      recoveryAttemptId: 'att-001',
      metadata: '{"amount": 4999, "currency": "INR", "paymentId": "pay_sim_8912"}',
      createdAt: '2026-08-30T10:15:00Z',
      updatedAt: '2026-08-30T10:15:00Z',
      deliveries: [
        {
          id: 'del-1',
          channel: 'WEBHOOK',
          provider: 'MerchantWebhookDispatcher',
          status: 'DELIVERED',
          attemptedAt: '2026-08-30T10:05:00Z',
          deliveredAt: '2026-08-30T10:05:10Z',
          errorCode: null,
          errorMessage: null,
          retryCount: 0,
        },
      ],
    },
    {
      id: 'notif-redesign-2',
      merchantId: 'm-1',
      eventType: 'CASE_EXHAUSTED',
      title: 'Recovery Attempts Exhausted for Subscription',
      message: 'All 3 automated payment recovery attempts were exhausted for Sunita Reddy.',
      status: 'READ',
      read: true,
      recoveryCaseId: 'demo-case-006',
      recoveryAttemptId: 'att-006',
      metadata: '{"amount": 3200, "currency": "INR"}',
      createdAt: '2026-08-30T09:00:00Z',
      updatedAt: '2026-08-30T09:30:00Z',
      deliveries: [
        {
          id: 'del-2',
          channel: 'EMAIL',
          provider: 'AWS SES',
          status: 'FAILED',
          attemptedAt: '2026-08-30T09:05:00Z',
          deliveredAt: null,
          errorCode: 'BOUNCE_ERROR',
          errorMessage: 'Mailbox temporarily suspended',
          retryCount: 2,
        },
      ],
    },
  ],
  totalElements: 2,
  totalPages: 1,
  number: 0,
  size: 15,
  first: true,
  last: true,
  empty: false,
};

describe('NotificationsPage Redesign - Merchant Notification & Event Center', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(notificationsApi.getNotifications).mockResolvedValue(mockRedesignNotifications);
    vi.mocked(notificationsApi.markAsRead).mockImplementation(async (id: string) => {
      const found = mockRedesignNotifications.content.find((n) => n.id === id);
      return {
        ...(found || mockRedesignNotifications.content[0]),
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

  it('renders operational header, unread status pill, compact summary strip, and feed', async () => {
    render(
      <MemoryRouter>
        <NotificationsPage />
      </MemoryRouter>
    );

    // 1. Header & Kicker
    expect(screen.getByText('Notification Center')).toBeInTheDocument();
    expect(
      screen.getByText(/Monitor recovery events, system alerts, and delivery activity in real time\./i)
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Refresh/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Mark All Read/i })).toBeInTheDocument();

    await waitFor(() => {
      // 2. Dynamic Unread Badge
      expect(screen.getByText('unread')).toBeInTheDocument();

      // 3. Compact Operational Summary Strip
      expect(screen.getByText('Total Events')).toBeInTheDocument();
      expect(screen.getByText('Unread Events')).toBeInTheDocument();
      expect(screen.getByText('Recovery Events')).toBeInTheDocument();
      expect(screen.getByText('System Alerts')).toBeInTheDocument();

      // 4. Control Toolbar
      expect(screen.getByLabelText(/Filter by event type/i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Unread Only/i })).toBeInTheDocument();
      expect(screen.getByText(/Showing/i)).toBeInTheDocument();

      // 5. Notification Content & Delivery Channel Badges
      expect(screen.getByText('Payment Successfully Recovered via WhatsApp')).toBeInTheDocument();
      expect(screen.getByText('Recovery Attempts Exhausted for Subscription')).toBeInTheDocument();
      expect(screen.getByText(/demo-case-001/)).toBeInTheDocument();
      expect(screen.getByText(/demo-case-006/)).toBeInTheDocument();
      expect(screen.getByText('WEBHOOK')).toBeInTheDocument();
      expect(screen.getByText('EMAIL')).toBeInTheDocument();

      // 6. Action Controls
      expect(
        screen.getByRole('button', {
          name: /Mark "Payment Successfully Recovered via WhatsApp" as read/i,
        })
      ).toBeInTheDocument();
      expect(
        screen.getByRole('button', {
          name: /View details for Payment Successfully Recovered via WhatsApp/i,
        })
      ).toBeInTheDocument();
    });

    // 7. Global Light Fintech Footer
    expect(screen.getByText('Platform Navigation')).toBeInTheDocument();
    expect(screen.getByText('System Infrastructure')).toBeInTheDocument();
    expect(screen.getByText(/Built for intelligent payment recovery\./i)).toBeInTheDocument();
  });

  it('opens detailed Event Details modal with recovery link and technical payload', async () => {
    render(
      <MemoryRouter>
        <NotificationsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Payment Successfully Recovered via WhatsApp')).toBeInTheDocument();
    });

    const viewButton = screen.getByRole('button', {
      name: /View details for Payment Successfully Recovered via WhatsApp/i,
    });
    fireEvent.click(viewButton);

    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeInTheDocument();
      expect(screen.getByText('Event Details')).toBeInTheDocument();
      expect(screen.getByText('pay_sim_8912')).toBeInTheDocument();
      expect(screen.getByText(/Associated Recovery Case/i)).toBeInTheDocument();
      expect(screen.getByText('VIEW RECOVERY CASE')).toBeInTheDocument();
      expect(screen.getByText(/Technical Payload/i)).toBeInTheDocument();
      expect(screen.getByText(/MerchantWebhookDispatcher/i)).toBeInTheDocument();
    });
  });

  it('displays All caught up pill when there are zero unread items', async () => {
    vi.mocked(notificationsApi.getNotifications).mockResolvedValueOnce({
      content: [
        {
          ...mockRedesignNotifications.content[1],
          id: 'notif-read-only',
          read: true,
          status: 'READ',
        },
      ],
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 15,
      first: true,
      last: true,
      empty: false,
    });

    render(
      <MemoryRouter>
        <NotificationsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('All caught up')).toBeInTheDocument();
    });
  });
});
