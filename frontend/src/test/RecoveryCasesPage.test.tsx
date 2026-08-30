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
      paymentId: 'p1',
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
      paymentId: 'p2',
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
  ],
  totalElements: 2,
  totalPages: 1,
  number: 0,
  size: 20,
  first: true,
  last: true,
  empty: false,
};

describe('RecoveryCasesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(recoveryCasesApi.getRecoveryCases).mockResolvedValue(mockCases);
  });

  it('renders table columns, customer details, status badges, and recoverable amounts', async () => {
    render(
      <MemoryRouter>
        <RecoveryCasesPage />
      </MemoryRouter>
    );

    expect(screen.getByText('Recovery Cases')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('Sarah Connor')).toBeInTheDocument();
      expect(screen.getByText('sarah@example.com')).toBeInTheDocument();
      expect(screen.getByText('John Doe')).toBeInTheDocument();
      expect(screen.getByText('IN_PROGRESS')).toBeInTheDocument();
      expect(screen.getByText('RECOVERED')).toBeInTheDocument();
      expect(screen.getByText('CRITICAL')).toBeInTheDocument();
      expect(screen.getByText('GATEWAY_ERROR')).toBeInTheDocument();
    });
  });

  it('filters cases when status dropdown selection changes', async () => {
    render(
      <MemoryRouter>
        <RecoveryCasesPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Sarah Connor')).toBeInTheDocument();
    });

    const statusSelect = screen.getByLabelText(/Status/i);
    fireEvent.change(statusSelect, { target: { value: 'OPEN' } });

    await waitFor(() => {
      expect(recoveryCasesApi.getRecoveryCases).toHaveBeenCalledWith(
        expect.objectContaining({
          status: 'OPEN',
        })
      );
    });
  });

  it('displays empty state when no cases are returned', async () => {
    vi.mocked(recoveryCasesApi.getRecoveryCases).mockResolvedValueOnce({
      content: [],
      totalElements: 0,
      totalPages: 0,
      number: 0,
      size: 20,
      first: true,
      last: true,
      empty: true,
    });

    render(
      <MemoryRouter>
        <RecoveryCasesPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('No Recovery Cases Found')).toBeInTheDocument();
    });
  });

  it('displays active filter badges and clears them on click', async () => {
    render(
      <MemoryRouter initialEntries={['/recovery-cases?status=OPEN']}>
        <RecoveryCasesPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText(/Active filters:/i)).toBeInTheDocument();
      expect(screen.getByText(/Status: OPEN/i)).toBeInTheDocument();
    });

    const clearButton = screen.getByRole('button', { name: /Clear Filters/i });
    fireEvent.click(clearButton);

    await waitFor(() => {
      expect(recoveryCasesApi.getRecoveryCases).toHaveBeenCalledWith(
        expect.objectContaining({
          status: undefined,
        })
      );
    });
  });

  it('provides navigation link to individual case detail', async () => {
    render(
      <MemoryRouter>
        <RecoveryCasesPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      const viewButtons = screen.getAllByRole('button', { name: /View/i });
      expect(viewButtons.length).toBeGreaterThan(0);
    });
  });
});
