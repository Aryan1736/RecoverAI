import { useState, useEffect, useCallback } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import {
  ShieldAlert,
  Search,
  Filter,
  X,
  RefreshCw,
  ArrowRight,
  User,
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
import { PageHeader } from '../../components/ui/PageHeader';
import { Card, CardContent } from '../../components/ui/Card';
import { Badge } from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { Select } from '../../components/ui/Select';
import { Pagination } from '../../components/ui/Pagination';
import { EmptyState } from '../../components/ui/EmptyState';
import { ErrorState } from '../../components/ui/ErrorState';

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

  const formatCurrency = (amount: number, currency: string) => {
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

  const getStatusBadge = (status: RecoveryCaseStatus) => {
    switch (status) {
      case 'RECOVERED':
        return <Badge variant="success" dot>{status}</Badge>;
      case 'IN_PROGRESS':
        return <Badge variant="info" dot pulse>{status}</Badge>;
      case 'OPEN':
        return <Badge variant="warning" dot>{status}</Badge>;
      case 'FAILED':
        return <Badge variant="danger">{status}</Badge>;
      case 'EXPIRED':
      case 'CANCELLED':
      default:
        return <Badge variant="default">{status}</Badge>;
    }
  };

  const getPriorityBadge = (priority: RecoveryPriority) => {
    switch (priority) {
      case 'CRITICAL':
        return <Badge variant="danger">{priority}</Badge>;
      case 'HIGH':
        return <Badge variant="warning">{priority}</Badge>;
      case 'MEDIUM':
        return <Badge variant="info">{priority}</Badge>;
      case 'LOW':
      default:
        return <Badge variant="default">{priority}</Badge>;
    }
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <PageHeader
        title="Recovery Cases"
        description="Monitor failed transactions, track autonomous recovery progress, and review execution outcomes."
        actions={
          <Button
            variant="outline"
            size="sm"
            onClick={handleRefresh}
            isLoading={loading}
            leftIcon={<RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />}
          >
            Refresh
          </Button>
        }
      />

      {/* Filter Toolbar */}
      <Card className="p-4 bg-slate-900/70 border-slate-800 space-y-3">
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
              <label htmlFor="category-search" className="block text-xs font-semibold uppercase tracking-wider text-slate-300">
                Failure Category
              </label>
              <div className="relative flex items-center">
                <input
                  id="category-search"
                  type="text"
                  placeholder="e.g. INSUFFICIENT_FUNDS"
                  value={categorySearch}
                  onChange={(e) => setCategorySearch(e.target.value)}
                  className="w-full rounded-lg bg-slate-900 text-slate-100 placeholder-slate-500 text-sm border border-slate-800 pl-3.5 pr-8 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
                <button
                  type="submit"
                  aria-label="Search failure category"
                  className="absolute right-2 text-slate-400 hover:text-white p-1"
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
              className="text-slate-400 hover:text-white self-start md:self-end"
            >
              Clear Filters
            </Button>
          )}
        </div>

        {/* Active Filter Indicators */}
        {hasActiveFilters && (
          <div className="flex flex-wrap items-center gap-2 pt-2 border-t border-slate-800/80 text-xs text-slate-400">
            <span className="flex items-center gap-1 font-medium text-slate-300">
              <Filter className="w-3.5 h-3.5" />
              Active filters:
            </span>
            {statusFilter !== 'ALL' && (
              <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-slate-800 text-slate-200">
                Status: {statusFilter}
                <button
                  type="button"
                  onClick={() => handleStatusChange('ALL')}
                  aria-label="Remove status filter"
                  className="hover:text-rose-400 ml-1"
                >
                  ×
                </button>
              </span>
            )}
            {priorityFilter !== 'ALL' && (
              <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-slate-800 text-slate-200">
                Priority: {priorityFilter}
                <button
                  type="button"
                  onClick={() => handlePriorityChange('ALL')}
                  aria-label="Remove priority filter"
                  className="hover:text-rose-400 ml-1"
                >
                  ×
                </button>
              </span>
            )}
            {categorySearch.trim() && (
              <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-slate-800 text-slate-200">
                Category: "{categorySearch.trim()}"
                <button
                  type="button"
                  onClick={() => {
                    setCategorySearch('');
                    updateUrlParams(statusFilter, priorityFilter, '', page);
                  }}
                  aria-label="Remove category filter"
                  className="hover:text-rose-400 ml-1"
                >
                  ×
                </button>
              </span>
            )}
          </div>
        )}
      </Card>

      {/* Main Content Area */}
      {loading ? (
        <Card>
          <CardContent className="p-0">
            <div className="p-6 space-y-4">
              {[1, 2, 3, 4, 5].map((i) => (
                <div key={i} className="h-12 w-full bg-slate-900/60 rounded-lg animate-pulse" />
              ))}
            </div>
          </CardContent>
        </Card>
      ) : error ? (
        <ErrorState title="Failed to Load Cases" message={error} onRetry={handleRefresh} />
      ) : !data || data.content.length === 0 ? (
        <EmptyState
          icon={<ShieldAlert className="w-8 h-8 text-indigo-400" />}
          title="No Recovery Cases Found"
          description={
            hasActiveFilters
              ? 'No recovery cases match your active filters. Try adjusting or clearing the filters.'
              : 'No failed payment recovery cases recorded in your merchant account yet.'
          }
          action={
            hasActiveFilters ? (
              <Button size="sm" variant="secondary" onClick={handleClearFilters}>
                Clear Active Filters
              </Button>
            ) : undefined
          }
        />
      ) : (
        <Card className="overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-slate-800 bg-slate-950/70 text-slate-400 uppercase tracking-wider font-semibold">
                  <th className="py-3 px-4">Case ID / Ref</th>
                  <th className="py-3 px-4">Customer</th>
                  <th className="py-3 px-4">Recoverable Amount</th>
                  <th className="py-3 px-4">Failure Category</th>
                  <th className="py-3 px-4">Priority</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4">Created Date</th>
                  <th className="py-3 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 text-slate-300">
                {data.content.map((rc) => (
                  <tr key={rc.id} className="hover:bg-slate-900/60 transition group">
                    <td className="py-3 px-4">
                      <Link
                        to={`/recovery-cases/${rc.id}`}
                        className="font-mono text-indigo-400 hover:text-indigo-300 font-semibold flex items-center gap-1.5"
                        title={rc.id}
                      >
                        <span className="truncate max-w-[120px]">{rc.id.slice(0, 8)}...</span>
                      </Link>
                    </td>

                    <td className="py-3 px-4">
                      <div className="flex items-center gap-2">
                        <div className="w-6 h-6 rounded-full bg-slate-800 flex items-center justify-center text-slate-400 shrink-0">
                          <User className="w-3 h-3" />
                        </div>
                        <div className="truncate max-w-[150px]">
                          <div className="font-medium text-slate-200 truncate">
                            {rc.customerName || 'Anonymous Customer'}
                          </div>
                          <div className="text-[10px] text-slate-400 truncate">
                            {rc.customerEmail || 'No email provided'}
                          </div>
                        </div>
                      </div>
                    </td>

                    <td className="py-3 px-4 font-mono font-semibold text-slate-200">
                      <div>{formatCurrency(rc.estimatedRecoverableAmount, rc.currency)}</div>
                      {rc.recoveredAmount > 0 && (
                        <div className="text-[10px] text-emerald-400">
                          Recovered: {formatCurrency(rc.recoveredAmount, rc.currency)}
                        </div>
                      )}
                    </td>

                    <td className="py-3 px-4">
                      <span className="px-2 py-0.5 rounded-md bg-slate-950 border border-slate-800 font-mono text-[11px] text-slate-300">
                        {rc.failureReasonCategory || 'Uncategorized'}
                      </span>
                    </td>

                    <td className="py-3 px-4">{getPriorityBadge(rc.priority)}</td>

                    <td className="py-3 px-4">{getStatusBadge(rc.status)}</td>

                    <td className="py-3 px-4 font-mono text-slate-400 text-[11px]">
                      {formatDateTime(rc.createdAt)}
                    </td>

                    <td className="py-3 px-4 text-right">
                      <Link to={`/recovery-cases/${rc.id}`}>
                        <Button
                          variant="ghost"
                          size="sm"
                          className="text-indigo-400 hover:text-white group-hover:bg-slate-800"
                          rightIcon={<ArrowRight className="w-3.5 h-3.5" />}
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

          {/* Pagination Controls */}
          <div className="p-4 border-t border-slate-800 bg-slate-950/40">
            <Pagination
              page={data.number}
              totalPages={data.totalPages}
              totalElements={data.totalElements}
              size={data.size}
              onPageChange={handlePageChange}
            />
          </div>
        </Card>
      )}
    </div>
  );
}
