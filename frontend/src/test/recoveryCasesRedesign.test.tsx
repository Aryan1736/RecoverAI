import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { RecoveryCasesPage } from '../pages/recovery-cases/RecoveryCasesPage';
import * as recoveryCasesApi from '../api/recovery-cases';
import type { PageResponse, RecoveryCase } from '../types/recovery-case';

vi.mock('../api/recovery-cases');

const mockCases: PageResponse<RecoveryCase> = {
  content: [
    {
      id: 'c1234567-89ab-cdef-0123-456789abcdef',
      merchantId: 'm1',
      paymentId: 'pay_H93k8sL2mP0xQ1',
      customerId: 'cust1',
      customerName: 'Sarah Connor',
      customerEmail: 'sarah@example.com',
      status: 'IN_PROGRESS',
      priority: 'CRITICAL',
      failureReasonCategory: 'INSUFFICIENT_FUNDS',
      estimatedRecoverableAmount: 4999,
      recoveredAmount: 0,
      currency: 'INR',
      expiresAt: '2026-09-05T00:00:00Z',
      recoveredAt: null,
      closedAt: null,
      createdAt: '2026-08-30T10:00:00Z',
      updatedAt: '2026-08-30T10:05:00Z',
    },
    {
      id: 'd9876543-21ba-fedc-3210-9876543210fe',
      merchantId: 'm1',
      paymentId: 'pay_K72m9nP1xR4yZ9',
      customerId: 'cust2',
      customerName: 'John Doe',
      customerEmail: 'john@example.com',
      status: 'RECOVERED',
      priority: 'HIGH',
      failureReasonCategory: 'GATEWAY_ERROR',
      estimatedRecoverableAmount: 12500,
      recoveredAmount: 12500,
      currency: 'INR',
      expiresAt: '2026-09-05T00:00:00Z',
      recoveredAt: '2026-08-30T11:00:00Z',
      closedAt: '2026-08-30T11:00:00Z',
      createdAt: '2026-08-30T09:30:00Z',
      updatedAt: '2026-08-30T11:00:00Z',
    },
    {
      id: 'e1122334-5566-7788-9900-aabbccddeeff',
      merchantId: 'm1',
      paymentId: 'pay_P55w4eR8tY1uI3',
      customerId: 'cust3',
      customerName: 'Aarav Sharma',
      customerEmail: 'aarav.sharma@example.com',
      status: 'OPEN',
      priority: 'MEDIUM',
      failureReasonCategory: 'AUTHENTICATION_FAILED',
      estimatedRecoverableAmount: 3200,
      recoveredAmount: 0,
      currency: 'INR',
      expiresAt: '2026-09-05T00:00:00Z',
      recoveredAt: null,
      closedAt: null,
      createdAt: '2026-08-30T08:15:00Z',
      updatedAt: '2026-08-30T08:15:00Z',
    },
    {
      id: 'f9988776-5544-3322-1100-ffeeddccbbaa',
      merchantId: 'm1',
      paymentId: 'pay_Q88x2cM9vB6nO4',
      customerId: 'cust4',
      customerName: 'Priya Patel',
      customerEmail: 'priya.patel@example.com',
      status: 'FAILED',
      priority: 'LOW',
      failureReasonCategory: 'CARD_EXPIRED',
      estimatedRecoverableAmount: 1800,
      recoveredAmount: 0,
      currency: 'INR',
      expiresAt: '2026-09-05T00:00:00Z',
      recoveredAt: null,
      closedAt: '2026-08-30T09:00:00Z',
      createdAt: '2026-08-30T07:45:00Z',
      updatedAt: '2026-08-30T09:00:00Z',
    },
  ],
  totalElements: 4,
  totalPages: 1,
  number: 0,
  size: 20,
  first: true,
  last: true,
  empty: false,
};

describe('RecoveryCasesPage Redesign - Premium Light Fintech UI', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(recoveryCasesApi.getRecoveryCases).mockResolvedValue(mockCases);
  });

  it('renders refined operations header, metrics, summary strip, and table hierarchy', async () => {
    render(
      <MemoryRouter>
        <RecoveryCasesPage />
      </MemoryRouter>
    );

    // 1. Header and Kicker
    expect(screen.getByText('Recovery Operations')).toBeInTheDocument();
    expect(
      screen.getByText(/Monitor failed payments, track autonomous recovery progress, and review execution outcomes\./i)
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Refresh/i })).toBeInTheDocument();

    await waitFor(() => {
      // 2. Summary Strip
      expect(screen.getByText('Total Cases')).toBeInTheDocument();
      expect(screen.getAllByText(/Active/i).length).toBeGreaterThan(0);
      expect(screen.getByText('At Risk')).toBeInTheDocument();

      // 3. Table Column Headers
      expect(screen.getByRole('columnheader', { name: /Case ID/i })).toBeInTheDocument();
      expect(screen.getByRole('columnheader', { name: /Customer/i })).toBeInTheDocument();
      expect(screen.getByRole('columnheader', { name: /Payment/i })).toBeInTheDocument();
      expect(screen.getByRole('columnheader', { name: /Amount/i })).toBeInTheDocument();
      expect(screen.getByRole('columnheader', { name: /Priority/i })).toBeInTheDocument();
      expect(screen.getByRole('columnheader', { name: /Category/i })).toBeInTheDocument();
      expect(screen.getByRole('columnheader', { name: /Status/i })).toBeInTheDocument();
      expect(screen.getByRole('columnheader', { name: /Date/i })).toBeInTheDocument();
      expect(screen.getByRole('columnheader', { name: /Action/i })).toBeInTheDocument();

      // 4. Case Rows Content
      expect(screen.getByText('Sarah Connor')).toBeInTheDocument();
      expect(screen.getByText('sarah@example.com')).toBeInTheDocument();
      expect(screen.getByText('John Doe')).toBeInTheDocument();
      expect(screen.getByText('Aarav Sharma')).toBeInTheDocument();
      expect(screen.getByText('Priya Patel')).toBeInTheDocument();

      // Statuses
      expect(screen.getByText('IN_PROGRESS')).toBeInTheDocument();
      expect(screen.getByText('RECOVERED')).toBeInTheDocument();
      expect(screen.getByText('OPEN')).toBeInTheDocument();
      expect(screen.getByText('FAILED')).toBeInTheDocument();

      // Priorities
      expect(screen.getByText('CRITICAL')).toBeInTheDocument();
      expect(screen.getByText('HIGH')).toBeInTheDocument();
      expect(screen.getByText('MEDIUM')).toBeInTheDocument();
      expect(screen.getByText('LOW')).toBeInTheDocument();

      // Categories
      expect(screen.getByText('INSUFFICIENT_FUNDS')).toBeInTheDocument();
      expect(screen.getByText('GATEWAY_ERROR')).toBeInTheDocument();

      // View buttons
      const viewButtons = screen.getAllByRole('button', { name: /View/i });
      expect(viewButtons.length).toBe(4);
    });

    // 5. Global Light Fintech Footer
    expect(screen.getByText('Platform Navigation')).toBeInTheDocument();
    expect(screen.getByText('System Infrastructure')).toBeInTheDocument();
    expect(screen.getByText(/Built for intelligent payment recovery\./i)).toBeInTheDocument();
  });

  it('handles category filter search and clearing', async () => {
    render(
      <MemoryRouter>
        <RecoveryCasesPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Sarah Connor')).toBeInTheDocument();
    });

    const categoryInput = screen.getByPlaceholderText(/e\.g\. INSUFFICIENT_FUNDS/i);
    fireEvent.change(categoryInput, { target: { value: 'GATEWAY_ERROR' } });

    const searchForm = categoryInput.closest('form');
    expect(searchForm).toBeInTheDocument();
    fireEvent.submit(searchForm!);

    await waitFor(() => {
      expect(recoveryCasesApi.getRecoveryCases).toHaveBeenCalledWith(
        expect.objectContaining({
          failureReasonCategory: 'GATEWAY_ERROR',
        })
      );
    });
  });
});
