import { useState, useEffect, useCallback, useMemo } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import {
  ShieldAlert,
  Search,
  Filter,
  X,
  RefreshCw,
  ArrowRight,
  User,
  CreditCard,
} from 'lucide-react';
import { getRecoveryCases } from '../../api/recovery-cases';
import { getDemoRecoveryCases } from '../../api/demo';
import { useDemoMode } from '../../hooks/useDemoMode';
import type {
  RecoveryCase,
  RecoveryCaseStatus,
  RecoveryPriority,
  PageResponse,
} from '../../types/recovery-case';
import { Button } from '../../components/ui/Button';
import { Select } from '../../components/ui/Select';
import { Pagination } from '../../components/ui/Pagination';
import { ErrorState } from '../../components/ui/ErrorState';
import { Footer } from '../../components/layout/Footer';

export function RecoveryCasesPage() {
  const { isDemoMode } = useDemoMode();
  const [searchParams, setSearchParams] = useSearchParams();

  // Read state from URL query parameters
  const statusParam = searchParams.get('status') as RecoveryCaseStatus | null;
  const priorityParam = searchParams.get('priority') as RecoveryPriority | null;
  const categoryParam = searchParams.get('category') || '';
  const pageParam = parseInt(searchParams.get('page') || '0', 10);

  const [statusFilter, setStatusFilter] = useState<RecoveryCaseStatus | 'ALL'>(statusParam || 'ALL');
  const [priorityFilter, setPriorityFilter] = useState<RecoveryPriority | 'ALL'>(priorityParam || 'ALL');
  const [categorySearch, setCategorySearch] = useState<string>(categoryParam);
  const [page, setPage] = useState<number>(isNaN(pageParam) ? 0 : pageParam);

  const [data, setData] = useState<PageResponse<RecoveryCase> | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function loadData() {
      setLoading(true);
      setError(null);
      try {
        const params = {
          status: statusFilter === 'ALL' ? undefined : statusFilter,
          priority: priorityFilter === 'ALL' ? undefined : priorityFilter,
          failureReasonCategory: categorySearch.trim() || undefined,
          page,
          size: 20,
          sort: 'createdAt,desc',
        };
        const response = isDemoMode
          ? await getDemoRecoveryCases(params)
          : await getRecoveryCases(params);
        if (!cancelled) {
          setData(response);
        }
      } catch (err: unknown) {
        if (!cancelled) {
          const message = err instanceof Error ? err.message : 'Failed to load recovery cases';
          setError(message);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    loadData();

    return () => {
      cancelled = true;
    };
  }, [statusFilter, priorityFilter, categorySearch, page, isDemoMode]);

  const handleRefresh = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = {
        status: statusFilter === 'ALL' ? undefined : statusFilter,
        priority: priorityFilter === 'ALL' ? undefined : priorityFilter,
        failureReasonCategory: categorySearch.trim() || undefined,
        page,
        size: 20,
        sort: 'createdAt,desc',
      };
      const response = isDemoMode
        ? await getDemoRecoveryCases(params)
        : await getRecoveryCases(params);
      setData(response);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to refresh recovery cases';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, [statusFilter, priorityFilter, categorySearch, page, isDemoMode]);

  // Sync state to URL search parameters
  const updateUrlParams = (
    newStatus: RecoveryCaseStatus | 'ALL',
    newPriority: RecoveryPriority | 'ALL',
    newCategory: string,
    newPage: number
  ) => {
    const params = new URLSearchParams();
    if (newStatus !== 'ALL') params.set('status', newStatus);
    if (newPriority !== 'ALL') params.set('priority', newPriority);
    if (newCategory.trim()) params.set('category', newCategory.trim());
    if (newPage > 0) params.set('page', newPage.toString());
    setSearchParams(params, { replace: true });
  };

  const handleStatusChange = (val: string) => {
    const s = val as RecoveryCaseStatus | 'ALL';
    setStatusFilter(s);
    setPage(0);
    updateUrlParams(s, priorityFilter, categorySearch, 0);
  };

  const handlePriorityChange = (val: string) => {
    const p = val as RecoveryPriority | 'ALL';
    setPriorityFilter(p);
    setPage(0);
    updateUrlParams(statusFilter, p, categorySearch, 0);
  };

  const handleCategorySearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    updateUrlParams(statusFilter, priorityFilter, categorySearch, 0);
  };

  const handleClearFilters = () => {
    setStatusFilter('ALL');
    setPriorityFilter('ALL');
    setCategorySearch('');
    setPage(0);
    setSearchParams(new URLSearchParams(), { replace: true });
  };

  const handlePageChange = (newPage: number) => {
    setPage(newPage);
    updateUrlParams(statusFilter, priorityFilter, categorySearch, newPage);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const hasActiveFilters = statusFilter !== 'ALL' || priorityFilter !== 'ALL' || categorySearch.trim() !== '';

  const formatCurrency = (amount: number, currency: string = 'INR') => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: currency || 'INR',
      maximumFractionDigits: 2,
    }).format(amount || 0);
  };

  const formatDateTime = (dateStr: string | null | undefined) => {
    if (!dateStr) return '—';
    try {
      const d = new Date(dateStr);
      return d.toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
      });
    } catch {
      return dateStr;
    }
  };

  // Metrics derived from actual data
  const totalElements = data?.totalElements ?? 0;
  const content = data?.content ?? [];
  const recoveredCount = useMemo(
    () => content.filter((c) => c.status === 'RECOVERED').length,
    [content]
  );
  const activeCount = useMemo(
    () => content.filter((c) => c.status === 'OPEN' || c.status === 'IN_PROGRESS').length,
    [content]
  );
  const atRiskAmount = useMemo(
    () =>
      content.reduce(
        (acc, c) => (c.status === 'OPEN' || c.status === 'IN_PROGRESS' ? acc + (c.estimatedRecoverableAmount || 0) : acc),
        0
      ),
    [content]
  );

  const getStatusBadge = (status: RecoveryCaseStatus) => {
    switch (status) {
      case 'RECOVERED':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-[#E8F7F0] text-[#08704F] border border-[#0B8F63]/25 shadow-2xs">
            <span className="w-1.5 h-1.5 rounded-full bg-[#0B8F63]" aria-hidden="true" />
            {status}
          </span>
        );
      case 'IN_PROGRESS':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-[#EFF6FF] text-[#2563EB] border border-[#BFDBFE] shadow-2xs">
            <span className="w-1.5 h-1.5 rounded-full bg-[#2563EB] animate-pulse" aria-hidden="true" />
            {status}
          </span>
        );
      case 'OPEN':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-[#FEF3C7] text-[#D97706] border border-[#FDE68A] shadow-2xs">
            <span className="w-1.5 h-1.5 rounded-full bg-[#D97706]" aria-hidden="true" />
            {status}
          </span>
        );
      case 'FAILED':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-[#FEE2E2] text-[#DC2626] border border-[#FECACA] shadow-2xs">
            <span className="w-1.5 h-1.5 rounded-full bg-[#DC2626]" aria-hidden="true" />
            {status}
          </span>
        );
      case 'EXPIRED':
      case 'CANCELLED':
      default:
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium bg-[#F1F4F2] text-[#667085] border border-[#E5E9E6]">
            {status}
          </span>
        );
    }
  };

  const getPriorityBadge = (priority: RecoveryPriority) => {
    switch (priority) {
      case 'CRITICAL':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded-md text-[11px] font-bold uppercase tracking-wider bg-[#FEE2E2] text-[#DC2626] border border-[#FECACA]">
            {priority}
          </span>
        );
      case 'HIGH':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded-md text-[11px] font-bold uppercase tracking-wider bg-[#FEF3C7] text-[#D97706] border border-[#FDE68A]">
            {priority}
          </span>
        );
      case 'MEDIUM':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded-md text-[11px] font-semibold uppercase tracking-wider bg-[#EFF6FF] text-[#2563EB] border border-[#BFDBFE]">
            {priority}
          </span>
        );
      case 'LOW':
      default:
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded-md text-[11px] font-medium uppercase tracking-wider bg-[#F1F4F2] text-[#667085] border border-[#E5E9E6]">
            {priority}
          </span>
        );
    }
  };

  return (
    <div className="space-y-6 animate-console-fade-in font-inter">
      {/* ==================================================
          1. REFINED OPERATIONS HEADER
          ================================================== */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between pt-1">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-[#0B8F63] pulse-subtle" />
            <span className="text-[11px] font-bold uppercase tracking-[0.08em] text-[#08704F]">
              Recovery Operations
            </span>
          </div>
          <h1 className="font-space-grotesk font-bold text-2xl sm:text-3xl text-[#111318] tracking-tight">
            Recovery Cases
          </h1>
          <p className="text-xs sm:text-sm text-[#667085] leading-relaxed max-w-2xl">
            Monitor failed payments, track autonomous recovery progress, and review execution outcomes.
          </p>
        </div>

        <div className="flex items-center gap-3 self-start sm:self-center">
          {/* Small contextual metric pill */}
          <div className="hidden lg:flex items-center gap-2.5 px-3 py-1.5 rounded-full bg-white border border-[#E5E9E6] text-xs shadow-2xs font-inter">
            <span className="font-semibold text-[#111318] tabular-nums">{totalElements}</span>
            <span className="text-[#667085]">total cases</span>
            <span className="text-[#D1D7D3]">•</span>
            <span className="font-semibold text-[#08704F] tabular-nums">{recoveredCount}</span>
            <span className="text-[#667085]">recovered</span>
            <span className="text-[#D1D7D3]">•</span>
            <span className="font-semibold text-[#2563EB] tabular-nums">{activeCount}</span>
            <span className="text-[#667085]">active</span>
          </div>

          <Button
            variant="outline"
            size="sm"
            onClick={handleRefresh}
            isLoading={loading}
            leftIcon={<RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin text-[#0B8F63]' : 'text-[#667085]'}`} />}
            className="bg-white border-[#E5E9E6] text-[#111318] hover:border-[#D1D7D3] hover:bg-[#F1F4F2] shadow-2xs text-xs font-semibold px-3 py-2 rounded-lg cursor-pointer transition-all duration-200"
          >
            Refresh
          </Button>
        </div>
      </div>

      {/* ==================================================
          2. COMPACT SUMMARY STRIP
          ================================================== */}
      <div className="bg-white border border-[#E5E9E6] rounded-xl p-4 shadow-2xs grid grid-cols-2 sm:grid-cols-4 gap-4 divide-y sm:divide-y-0 sm:divide-x divide-[#E5E9E6] animate-console-fade-in delay-1">
        <div className="pt-2 sm:pt-0 sm:px-3 first:pt-0 first:px-0">
          <span className="text-[11px] font-semibold uppercase tracking-wider text-[#667085] block">
            Total Cases
          </span>
          <div className="mt-1 flex items-baseline gap-2">
            <span className="font-space-grotesk font-bold text-xl sm:text-2xl text-[#111318] tabular-nums">
              {totalElements}
            </span>
            <span className="text-[11px] text-[#98A2B3] font-mono">queued</span>
          </div>
        </div>

        <div className="pt-2 sm:pt-0 sm:px-3">
          <span className="text-[11px] font-semibold uppercase tracking-wider text-[#667085] block">
            Recovered
          </span>
          <div className="mt-1 flex items-baseline gap-2">
            <span className="font-space-grotesk font-bold text-xl sm:text-2xl text-[#08704F] tabular-nums">
              {recoveredCount}
            </span>
            <span className="inline-flex items-center gap-1 text-[11px] text-[#08704F] font-semibold bg-[#E8F7F0] px-1.5 py-0.5 rounded-full">
              <span className="w-1.5 h-1.5 rounded-full bg-[#0B8F63]" />
              settled
            </span>
          </div>
        </div>

        <div className="pt-2 sm:pt-0 sm:px-3">
          <span className="text-[11px] font-semibold uppercase tracking-wider text-[#667085] block">
            Active
          </span>
          <div className="mt-1 flex items-baseline gap-2">
            <span className="font-space-grotesk font-bold text-xl sm:text-2xl text-[#2563EB] tabular-nums">
              {activeCount}
            </span>
            <span className="inline-flex items-center gap-1 text-[11px] text-[#2563EB] font-semibold bg-[#EFF6FF] px-1.5 py-0.5 rounded-full">
              <span className="w-1.5 h-1.5 rounded-full bg-[#2563EB] animate-pulse" />
              in flight
            </span>
          </div>
        </div>

        <div className="pt-2 sm:pt-0 sm:px-3">
          <span className="text-[11px] font-semibold uppercase tracking-wider text-[#667085] block">
            At Risk
          </span>
          <div className="mt-1 flex items-baseline gap-2">
            <span className="font-space-grotesk font-bold text-xl sm:text-2xl text-[#111318] tabular-nums">
              {formatCurrency(atRiskAmount, 'INR')}
            </span>
            <span className="text-[11px] text-[#98A2B3] font-mono">recoverable</span>
          </div>
        </div>
      </div>

      {/* ==================================================
          3. REFINED FILTER TOOLBAR
          ================================================== */}
      <div className="bg-white border border-[#E5E9E6] rounded-xl p-4 shadow-2xs space-y-3 animate-console-fade-in delay-2">
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-3">
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 flex-1">
            {/* Status Filter */}
            <Select
              label="Status"
              value={statusFilter}
              onChange={(e) => handleStatusChange(e.target.value)}
              options={[
                { value: 'ALL', label: 'All Statuses' },
                { value: 'OPEN', label: 'Open' },
                { value: 'IN_PROGRESS', label: 'In Progress' },
                { value: 'RECOVERED', label: 'Recovered' },
                { value: 'FAILED', label: 'Failed' },
                { value: 'EXPIRED', label: 'Expired' },
                { value: 'CANCELLED', label: 'Cancelled' },
              ]}
            />

            {/* Priority Filter */}
            <Select
              label="Priority"
              value={priorityFilter}
              onChange={(e) => handlePriorityChange(e.target.value)}
              options={[
                { value: 'ALL', label: 'All Priorities' },
                { value: 'CRITICAL', label: 'Critical' },
                { value: 'HIGH', label: 'High' },
                { value: 'MEDIUM', label: 'Medium' },
                { value: 'LOW', label: 'Low' },
              ]}
            />

            {/* Failure Category Search */}
            <form onSubmit={handleCategorySearchSubmit} className="space-y-1.5 text-left">
              <label
                htmlFor="category-search"
                className="block text-xs font-semibold uppercase tracking-wider text-[#111318]"
              >
                Failure Category
              </label>
              <div className="relative flex items-center">
                <input
                  id="category-search"
                  type="text"
                  placeholder="e.g. INSUFFICIENT_FUNDS"
                  value={categorySearch}
                  onChange={(e) => setCategorySearch(e.target.value)}
                  className="w-full rounded-lg bg-white text-[#111318] placeholder-[#98A2B3] text-sm border border-[#E5E9E6] pl-3.5 pr-9 py-2 focus:outline-none focus:ring-2 focus:ring-[#0B8F63]/20 focus:border-[#0B8F63] hover:border-[#D1D7D3] shadow-2xs transition-colors"
                />
                <button
                  type="submit"
                  aria-label="Search failure category"
                  className="absolute right-2 text-[#98A2B3] hover:text-[#111318] p-1.5 rounded-md hover:bg-[#F1F4F2] transition cursor-pointer"
                >
                  <Search className="w-3.5 h-3.5" />
                </button>
              </div>
            </form>
          </div>

          {/* Clear Filters Action */}
          {hasActiveFilters && (
            <Button
              variant="ghost"
              size="sm"
              onClick={handleClearFilters}
              leftIcon={<X className="w-3.5 h-3.5" />}
              className="text-[#667085] hover:text-[#111318] hover:bg-[#F1F4F2] self-start md:self-end text-xs font-semibold cursor-pointer"
            >
              Clear Filters
            </Button>
          )}
        </div>

        {/* Active Filter Indicators */}
        {hasActiveFilters && (
          <div className="flex flex-wrap items-center gap-2 pt-3 border-t border-[#E5E9E6] text-xs text-[#667085]">
            <span className="flex items-center gap-1.5 font-semibold text-[#111318]">
              <Filter className="w-3.5 h-3.5 text-[#0B8F63]" />
              Active filters:
            </span>
            {statusFilter !== 'ALL' && (
              <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-lg bg-[#F1F4F2] text-[#111318] border border-[#E5E9E6] text-xs font-medium">
                Status: {statusFilter}
                <button
                  type="button"
                  onClick={() => handleStatusChange('ALL')}
                  aria-label="Remove status filter"
                  className="hover:text-[#DC2626] ml-1 cursor-pointer font-bold text-sm leading-none"
                >
                  ×
                </button>
              </span>
            )}
            {priorityFilter !== 'ALL' && (
              <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-lg bg-[#F1F4F2] text-[#111318] border border-[#E5E9E6] text-xs font-medium">
                Priority: {priorityFilter}
                <button
                  type="button"
                  onClick={() => handlePriorityChange('ALL')}
                  aria-label="Remove priority filter"
                  className="hover:text-[#DC2626] ml-1 cursor-pointer font-bold text-sm leading-none"
                >
                  ×
                </button>
              </span>
            )}
            {categorySearch.trim() && (
              <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-lg bg-[#F1F4F2] text-[#111318] border border-[#E5E9E6] text-xs font-medium">
                Category: &ldquo;{categorySearch.trim()}&rdquo;
                <button
                  type="button"
                  onClick={() => {
                    setCategorySearch('');
                    updateUrlParams(statusFilter, priorityFilter, '', page);
                  }}
                  aria-label="Remove category filter"
                  className="hover:text-[#DC2626] ml-1 cursor-pointer font-bold text-sm leading-none"
                >
                  ×
                </button>
              </span>
            )}
          </div>
        )}
      </div>

      {/* ==================================================
          4. MAIN CASE TABLE / STATES
          ================================================== */}
      {loading ? (
        <div className="bg-white border border-[#E5E9E6] rounded-xl shadow-2xs overflow-hidden animate-console-fade-in delay-3">
          <div className="p-4 bg-[#F7F9F7] border-b border-[#E5E9E6] flex items-center justify-between">
            <div className="h-4 w-32 bg-[#E5E9E6] rounded animate-pulse" />
            <div className="h-4 w-20 bg-[#E5E9E6] rounded animate-pulse" />
          </div>
          <div className="divide-y divide-[#E5E9E6]/60">
            {[1, 2, 3, 4, 5, 6].map((i) => (
              <div key={i} className="p-4 flex items-center justify-between gap-4 animate-pulse">
                <div className="h-4 w-24 bg-[#F1F4F2] rounded" />
                <div className="flex items-center gap-2.5">
                  <div className="w-7 h-7 rounded-full bg-[#F1F4F2]" />
                  <div className="space-y-1">
                    <div className="h-3.5 w-24 bg-[#F1F4F2] rounded" />
                    <div className="h-2.5 w-28 bg-[#F1F4F2] rounded" />
                  </div>
                </div>
                <div className="h-4 w-20 bg-[#F1F4F2] rounded" />
                <div className="h-4 w-16 bg-[#F1F4F2] rounded font-bold" />
                <div className="h-5 w-16 bg-[#F1F4F2] rounded-md" />
                <div className="h-5 w-24 bg-[#F1F4F2] rounded" />
                <div className="h-5 w-20 bg-[#F1F4F2] rounded-full" />
                <div className="h-4 w-16 bg-[#F1F4F2] rounded" />
                <div className="h-7 w-16 bg-[#F1F4F2] rounded-lg" />
              </div>
            ))}
          </div>
        </div>
      ) : error ? (
        <ErrorState title="Failed to Load Cases" message={error} onRetry={handleRefresh} />
      ) : !data || data.content.length === 0 ? (
        <div className="bg-white border border-[#E5E9E6] rounded-xl p-12 text-center shadow-2xs space-y-4 animate-console-fade-in delay-3">
          <div className="w-12 h-12 rounded-2xl bg-[#E8F7F0] border border-[#0B8F63]/20 text-[#0B8F63] mx-auto flex items-center justify-center shadow-2xs">
            <ShieldAlert className="w-6 h-6 text-[#0B8F63]" />
          </div>
          <div className="space-y-1.5 max-w-md mx-auto">
            <h3 className="font-space-grotesk font-bold text-lg text-[#111318]">
              No Recovery Cases Found
            </h3>
            <p className="text-xs text-[#667085] leading-relaxed">
              {hasActiveFilters
                ? 'No recovery cases match the selected filters. Try clearing or broadening your search parameters.'
                : 'There are currently no active or historical recovery cases in your queue.'}
            </p>
          </div>
          {hasActiveFilters && (
            <Button
              size="sm"
              variant="secondary"
              onClick={handleClearFilters}
              className="bg-[#F1F4F2] hover:bg-[#E5E9E6] text-[#111318] border border-[#E5E9E6] font-semibold text-xs px-4 py-2 rounded-lg cursor-pointer"
            >
              Clear All Filters
            </Button>
          )}
        </div>
      ) : (
        <div className="space-y-4 animate-console-fade-in delay-3">
          <div className="bg-white border border-[#E5E9E6] rounded-xl shadow-2xs overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs text-[#667085] border-collapse">
                <thead className="bg-[#F7F9F7] border-b border-[#E5E9E6] text-[#667085] font-semibold uppercase tracking-[0.04em] text-[11px]">
                  <tr>
                    <th scope="col" className="py-3 px-4">Case ID</th>
                    <th scope="col" className="py-3 px-4">Customer</th>
                    <th scope="col" className="py-3 px-4">Payment</th>
                    <th scope="col" className="py-3 px-4">Amount</th>
                    <th scope="col" className="py-3 px-4">Priority</th>
                    <th scope="col" className="py-3 px-4">Category</th>
                    <th scope="col" className="py-3 px-4">Status</th>
                    <th scope="col" className="py-3 px-4">Date</th>
                    <th scope="col" className="py-3 px-4 text-right">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#E5E9E6]/60">
                  {data.content.map((c) => (
                    <tr
                      key={c.id}
                      className="hover:bg-[#F7F9F7] transition-all duration-200 group cursor-pointer"
                    >
                      {/* CASE ID */}
                      <td className="py-3.5 px-4 font-mono font-medium text-[#111318] whitespace-nowrap">
                        <Link
                          to={`/recovery-cases/${encodeURIComponent(c.id)}`}
                          className="group-hover:text-[#0B8F63] transition-colors focus:outline-none focus:ring-1 focus:ring-[#0B8F63] rounded"
                          title={c.id}
                        >
                          {c.id.length > 16 ? `${c.id.slice(0, 16)}...` : c.id}
                        </Link>
                      </td>

                      {/* CUSTOMER */}
                      <td className="py-3.5 px-4">
                        <div className="flex items-center gap-2.5">
                          <div className="w-7 h-7 rounded-full bg-[#F1F4F2] border border-[#E5E9E6] text-[#111318] flex items-center justify-center text-[10px] font-bold shrink-0 shadow-2xs">
                            {c.customerName
                              ? c.customerName
                                  .split(' ')
                                  .map((n) => n[0])
                                  .join('')
                                  .slice(0, 2)
                                  .toUpperCase()
                              : <User className="w-3.5 h-3.5 text-[#667085]" />}
                          </div>
                          <div className="min-w-0">
                            <div className="truncate max-w-[130px] font-semibold text-[#111318] text-xs">
                              {c.customerName || 'Anonymous'}
                            </div>
                            {c.customerEmail && (
                              <div className="text-[11px] text-[#667085] truncate max-w-[150px]">
                                {c.customerEmail}
                              </div>
                            )}
                          </div>
                        </div>
                      </td>

                      {/* PAYMENT */}
                      <td className="py-3.5 px-4 font-mono text-[11px] text-[#667085] whitespace-nowrap">
                        <div className="flex items-center gap-1.5">
                          <CreditCard className="w-3.5 h-3.5 text-[#98A2B3] shrink-0" />
                          <span>{c.paymentId ? (c.paymentId.length > 14 ? `${c.paymentId.slice(0, 14)}...` : c.paymentId) : '—'}</span>
                        </div>
                      </td>

                      {/* AMOUNT */}
                      <td className="py-3.5 px-4 whitespace-nowrap">
                        <span className="font-space-grotesk font-bold text-sm text-[#111318] tabular-nums">
                          {formatCurrency(c.estimatedRecoverableAmount, c.currency)}
                        </span>
                      </td>

                      {/* PRIORITY */}
                      <td className="py-3.5 px-4 whitespace-nowrap">
                        {getPriorityBadge(c.priority)}
                      </td>

                      {/* CATEGORY */}
                      <td className="py-3.5 px-4 whitespace-nowrap">
                        <span className="text-[11px] font-mono text-[#475467] bg-[#F1F4F2] px-2 py-0.5 rounded border border-[#E5E9E6]">
                          {c.failureReasonCategory || 'UNKNOWN'}
                        </span>
                      </td>

                      {/* STATUS */}
                      <td className="py-3.5 px-4 whitespace-nowrap">
                        {getStatusBadge(c.status)}
                      </td>

                      {/* DATE */}
                      <td className="py-3.5 px-4 whitespace-nowrap text-[#667085] font-inter">
                        {formatDateTime(c.createdAt)}
                      </td>

                      {/* ACTION */}
                      <td className="py-3.5 px-4 text-right whitespace-nowrap">
                        <Link
                          to={`/recovery-cases/${encodeURIComponent(c.id)}`}
                          className="inline-flex items-center"
                        >
                          <Button
                            size="sm"
                            variant="outline"
                            rightIcon={<ArrowRight className="w-3.5 h-3.5 transition-transform group-hover:translate-x-0.5" />}
                            className="bg-white border-[#E5E9E6] text-[#111318] hover:border-[#0B8F63] hover:text-[#08704F] hover:bg-[#E8F7F0]/40 -translate-y-px hover:shadow-2xs transition-all duration-200 text-xs px-2.5 py-1 font-semibold cursor-pointer"
                          >
                            View
                          </Button>
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* ==================================================
              5. PAGINATION
              ================================================== */}
          <Pagination
            page={data.number}
            totalPages={data.totalPages}
            totalElements={data.totalElements}
            size={data.size}
            onPageChange={handlePageChange}
          />
        </div>
      )}

      {/* ==================================================
          6. GLOBAL LIGHT FINTECH FOOTER
          ================================================== */}
      <Footer />
    </div>
  );
}
