import { useState, useEffect, useCallback } from 'react';
import {
  TrendingUp,
  ShieldAlert,
  CheckCircle2,
  Clock,
  RefreshCw,
  Zap,
} from 'lucide-react';
import {
  getAnalyticsOverview,
  getRecoveryTrends,
  getChannelAnalytics,
  getFailureAnalytics,
} from '../../api/analytics';
import type {
  AnalyticsOverview,
  RecoveryTrends,
  ChannelAnalytics,
  FailureAnalytics,
  DateRangeParams,
} from '../../types/analytics';
import { PageHeader } from '../../components/ui/PageHeader';
import { Card } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { SkeletonCard } from '../../components/ui/Skeleton';
import { ErrorState } from '../../components/ui/ErrorState';
import { DateRangeSelector } from '../../components/analytics/DateRangeSelector';
import { RecoveryTrendChart } from '../../components/analytics/RecoveryTrendChart';
import { ChannelAnalyticsCard } from '../../components/analytics/ChannelAnalyticsCard';
import { FailureAnalyticsCard } from '../../components/analytics/FailureAnalyticsCard';

export function AnalyticsPage() {
  const [dateRange, setDateRange] = useState<DateRangeParams>(() => {
    const now = new Date();
    const to = now.toISOString().split('T')[0];
    const fromDate = new Date(now);
    fromDate.setDate(now.getDate() - 30);
    return { from: fromDate.toISOString().split('T')[0], to };
  });

  const [overview, setOverview] = useState<AnalyticsOverview | null>(null);
  const [trends, setTrends] = useState<RecoveryTrends | null>(null);
  const [channels, setChannels] = useState<ChannelAnalytics | null>(null);
  const [failures, setFailures] = useState<FailureAnalytics | null>(null);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchAllAnalytics = useCallback(async (range: DateRangeParams) => {
    setLoading(true);
    setError(null);
    try {
      const [overviewData, trendsData, channelsData, failuresData] = await Promise.all([
        getAnalyticsOverview(range),
        getRecoveryTrends(range),
        getChannelAnalytics(range),
        getFailureAnalytics(range),
      ]);

      setOverview(overviewData);
      setTrends(trendsData);
      setChannels(channelsData);
      setFailures(failuresData);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to load analytics datasets';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    let cancelled = false;

    async function loadData() {
      setLoading(true);
      setError(null);
      try {
        const [overviewData, trendsData, channelsData, failuresData] = await Promise.all([
          getAnalyticsOverview(dateRange),
          getRecoveryTrends(dateRange),
          getChannelAnalytics(dateRange),
          getFailureAnalytics(dateRange),
        ]);

        if (!cancelled) {
          setOverview(overviewData);
          setTrends(trendsData);
          setChannels(channelsData);
          setFailures(failuresData);
        }
      } catch (err: unknown) {
        if (!cancelled) {
          const message = err instanceof Error ? err.message : 'Failed to load analytics datasets';
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
  }, [dateRange]);

  const handleDateRangeChange = (newRange: DateRangeParams) => {
    setDateRange(newRange);
  };

  const handleRefresh = () => {
    fetchAllAnalytics(dateRange);
  };

  const formatCurrency = (val: number | undefined) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0,
    }).format(val || 0);
  };

  const formatDuration = (seconds: number | null | undefined): string => {
    if (seconds === null || seconds === undefined || seconds <= 0) return '—';
    if (seconds < 60) return `${Math.round(seconds)}s`;
    const minutes = Math.floor(seconds / 60);
    if (minutes < 60) return `${minutes}m`;
    const hours = Math.floor(minutes / 60);
    const remMin = minutes % 60;
    return `${hours}h ${remMin}m`;
  };

  return (
    <div className="space-y-8">
      {/* Page Header */}
      <PageHeader
        title="Analytics & Intelligence"
        description="Comprehensive recovery performance metrics, channel conversion dynamics, and root-cause failure breakdown."
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

      {/* Date Range Control Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-4 rounded-2xl bg-slate-900/60 border border-slate-800/80">
        <div>
          <div className="text-xs font-semibold text-white">Reporting Window</div>
          <div className="text-[11px] text-slate-400">
            {dateRange.from && dateRange.to ? `${dateRange.from} to ${dateRange.to}` : 'All time'}
          </div>
        </div>

        <DateRangeSelector initialPreset="30d" onChange={handleDateRangeChange} disabled={loading} />
      </div>

      {/* KPI Metrics Area */}
      {loading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4">
          {[1, 2, 3, 4, 5, 6].map((i) => (
            <SkeletonCard key={i} />
          ))}
        </div>
      ) : error ? (
        <ErrorState title="Failed to Load Analytics" message={error} onRetry={handleRefresh} />
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4">
          {/* Card 1: Total Recovery Cases */}
          <Card className="hover:border-slate-700 transition">
            <div className="flex items-center justify-between text-slate-400 pb-2">
              <span className="text-[11px] font-semibold uppercase tracking-wider">Total Cases</span>
              <ShieldAlert className="w-4 h-4 text-indigo-400" />
            </div>
            <div className="space-y-1">
              <div className="text-xl font-bold text-white font-mono">{overview?.totalCases ?? 0}</div>
              <p className="text-[10px] text-slate-400">
                {overview?.openCases ?? 0} open • {overview?.inProgressCases ?? 0} in flight
              </p>
            </div>
          </Card>

          {/* Card 2: Recovered Cases */}
          <Card className="hover:border-slate-700 transition">
            <div className="flex items-center justify-between text-slate-400 pb-2">
              <span className="text-[11px] font-semibold uppercase tracking-wider">Recovered</span>
              <CheckCircle2 className="w-4 h-4 text-emerald-400" />
            </div>
            <div className="space-y-1">
              <div className="text-xl font-bold text-emerald-400 font-mono">
                {overview?.recoveredCases ?? 0}
              </div>
              <p className="text-[10px] text-slate-400">Closed-loop successes</p>
            </div>
          </Card>

          {/* Card 3: Recovery Rate */}
          <Card className="hover:border-slate-700 transition">
            <div className="flex items-center justify-between text-slate-400 pb-2">
              <span className="text-[11px] font-semibold uppercase tracking-wider">Recovery Rate</span>
              <TrendingUp className="w-4 h-4 text-indigo-400" />
            </div>
            <div className="space-y-1">
              <div className="text-xl font-bold text-white font-mono">
                {overview?.recoveryRate ? `${Number(overview.recoveryRate).toFixed(1)}%` : '0.0%'}
              </div>
              <p className="text-[10px] text-slate-400">Resolution efficiency</p>
            </div>
          </Card>

          {/* Card 4: Total Recovered Amount */}
          <Card className="hover:border-slate-700 transition">
            <div className="flex items-center justify-between text-slate-400 pb-2">
              <span className="text-[11px] font-semibold uppercase tracking-wider">Recovered Revenue</span>
              <Zap className="w-4 h-4 text-emerald-400" />
            </div>
            <div className="space-y-1">
              <div className="text-xl font-bold text-emerald-400 font-mono truncate">
                {formatCurrency(overview?.totalRecoveredAmount)}
              </div>
              <p className="text-[10px] text-slate-400 truncate">
                Of {formatCurrency(overview?.totalEstimatedRecoverableAmount)} at risk
              </p>
            </div>
          </Card>

          {/* Card 5: Average Recovered Amount */}
          <Card className="hover:border-slate-700 transition">
            <div className="flex items-center justify-between text-slate-400 pb-2">
              <span className="text-[11px] font-semibold uppercase tracking-wider">Avg Recovery</span>
              <TrendingUp className="w-4 h-4 text-slate-400" />
            </div>
            <div className="space-y-1">
              <div className="text-xl font-bold text-white font-mono truncate">
                {formatCurrency(overview?.averageRecoveredAmount)}
              </div>
              <p className="text-[10px] text-slate-400">Per recovered transaction</p>
            </div>
          </Card>

          {/* Card 6: Average Recovery Time */}
          <Card className="hover:border-slate-700 transition">
            <div className="flex items-center justify-between text-slate-400 pb-2">
              <span className="text-[11px] font-semibold uppercase tracking-wider">Avg Duration</span>
              <Clock className="w-4 h-4 text-amber-400" />
            </div>
            <div className="space-y-1">
              <div className="text-xl font-bold text-white font-mono">
                {formatDuration(overview?.averageTimeToRecoverySeconds)}
              </div>
              <p className="text-[10px] text-slate-400">Time to payment reconciliation</p>
            </div>
          </Card>
        </div>
      )}

      {/* Main Charts & Breakdown Section */}
      <div className="space-y-6">
        {/* Chart: Recovery Trends */}
        <RecoveryTrendChart
          trends={trends?.trends || []}
          totalAmountAtRisk={Number(trends?.totalAmountAtRisk || 0)}
          totalRecoveredAmount={Number(trends?.totalRecoveredAmount || 0)}
          overallRecoveryRate={Number(trends?.overallRecoveryRate || 0)}
          isLoading={loading}
        />

        {/* 2-Column Split: Channel Analytics & Failure Analytics */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <ChannelAnalyticsCard
            channels={channels?.channels || []}
            totalAttempts={channels?.totalAttempts || 0}
            isLoading={loading}
          />

          <FailureAnalyticsCard
            categories={failures?.categories || []}
            priorities={failures?.priorities || []}
            totalCases={failures?.totalCases || 0}
            isLoading={loading}
          />
        </div>
      </div>
    </div>
  );
}
