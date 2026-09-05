import { useState, useEffect, useCallback, useMemo } from 'react';
import {
  TrendingUp,
  ShieldAlert,
  CheckCircle2,
  Clock,
  RefreshCw,
  Zap,
  Sparkles,
  ArrowUpRight,
} from 'lucide-react';
import {
  getAnalyticsOverview,
  getRecoveryTrends,
  getChannelAnalytics,
  getFailureAnalytics,
} from '../../api/analytics';
import {
  getDemoAnalyticsOverview,
  getDemoRecoveryTrends,
  getDemoChannelAnalytics,
  getDemoFailureAnalytics,
} from '../../api/demo';
import { useDemoMode } from '../../hooks/useDemoMode';
import type {
  AnalyticsOverview,
  RecoveryTrends,
  ChannelAnalytics,
  FailureAnalytics,
  DateRangeParams,
} from '../../types/analytics';
import { Button } from '../../components/ui/Button';
import { SkeletonCard } from '../../components/ui/Skeleton';
import { ErrorState } from '../../components/ui/ErrorState';
import { DateRangeSelector } from '../../components/analytics/DateRangeSelector';
import { RecoveryTrendChart } from '../../components/analytics/RecoveryTrendChart';
import { ChannelAnalyticsCard } from '../../components/analytics/ChannelAnalyticsCard';
import { FailureAnalyticsCard } from '../../components/analytics/FailureAnalyticsCard';
import { Footer } from '../../components/layout/Footer';

// Subtle number count-up animation on initial page load
function CountUpMetric({
  value,
  isFloat = false,
  formatCurrency = false,
}: {
  value: number;
  isFloat?: boolean;
  formatCurrency?: boolean;
}) {
  const [displayValue, setDisplayValue] = useState<number>(() => {
    // In test environment, immediately display target value
    if (import.meta.env?.MODE === 'test') {
      return value;
    }
    return 0;
  });

  useEffect(() => {
    if (import.meta.env?.MODE === 'test') {
      setDisplayValue(value);
      return;
    }
    if (value === 0) {
      setDisplayValue(0);
      return;
    }

    const duration = 600;
    const startTime = performance.now();
    let frameId: number;

    const animate = (currentTime: number) => {
      const elapsed = currentTime - startTime;
      const progress = Math.min(elapsed / duration, 1);
      // easeOutCubic
      const ease = 1 - Math.pow(1 - progress, 3);
      const current = ease * value;
      setDisplayValue(current);

      if (progress < 1) {
        frameId = requestAnimationFrame(animate);
      } else {
        setDisplayValue(value);
      }
    };

    frameId = requestAnimationFrame(animate);
    return () => cancelAnimationFrame(frameId);
  }, [value]);

  if (formatCurrency) {
    return (
      <>
        {new Intl.NumberFormat('en-IN', {
          style: 'currency',
          currency: 'INR',
          maximumFractionDigits: 0,
        }).format(displayValue)}
      </>
    );
  }

  if (isFloat) {
    return <>{displayValue.toFixed(1)}%</>;
  }

  return <>{Math.round(displayValue).toLocaleString('en-IN')}</>;
}

export function AnalyticsPage() {
  const { isDemoMode } = useDemoMode();
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

  const fetchAllAnalytics = useCallback(
    async (range: DateRangeParams) => {
      setLoading(true);
      setError(null);
      try {
        const [overviewData, trendsData, channelsData, failuresData] = isDemoMode
          ? await Promise.all([
              getDemoAnalyticsOverview(range),
              getDemoRecoveryTrends(range),
              getDemoChannelAnalytics(range),
              getDemoFailureAnalytics(range),
            ])
          : await Promise.all([
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
    },
    [isDemoMode]
  );

  useEffect(() => {
    let cancelled = false;

    async function loadData() {
      setLoading(true);
      setError(null);
      try {
        const [overviewData, trendsData, channelsData, failuresData] = isDemoMode
          ? await Promise.all([
              getDemoAnalyticsOverview(dateRange),
              getDemoRecoveryTrends(dateRange),
              getDemoChannelAnalytics(dateRange),
              getDemoFailureAnalytics(dateRange),
            ])
          : await Promise.all([
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
  }, [dateRange, isDemoMode]);

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

  // Dynamically generated analytical insight derived strictly from existing data
  const recoveryInsight = useMemo(() => {
    if (!overview && !channels && !failures) return null;

    // 1. Identify best channel
    const activeChannels = (channels?.channels || []).filter((c) => (c.totalAttempts || 0) > 0);
    const topChannel = [...activeChannels].sort((a, b) => (b.successRate || 0) - (a.successRate || 0))[0];
    const channelLabel = topChannel
      ? topChannel.channel === 'SMART_LINK'
        ? 'Smart Link'
        : topChannel.channel === 'WHATSAPP'
        ? 'WhatsApp'
        : topChannel.channel === 'EMAIL'
        ? 'Email'
        : topChannel.channel === 'SMS'
        ? 'SMS'
        : topChannel.channel === 'RETRY_CHARGE'
        ? 'Retry Charge'
        : 'Manual Ops'
      : null;

    // 2. Identify top failure reason
    const topFailure = [...(failures?.categories || [])].sort((a, b) => (b.caseCount || 0) - (a.caseCount || 0))[0];
    const failureLabel = topFailure
      ? topFailure.failureReasonCategory.replace(/_/g, ' ').toLowerCase()
      : null;

    const rate = overview?.recoveryRate ? Number(overview.recoveryRate).toFixed(1) : null;

    if (channelLabel && failureLabel) {
      return `${channelLabel} currently has the highest conversion efficiency, while ${failureLabel} represents a significant share of recoverable revenue.`;
    }
    if (channelLabel && rate) {
      return `${channelLabel} is delivering peak resolution efficiency with overall portfolio recovery tracking at ${rate}%.`;
    }
    if (failureLabel) {
      return `${failureLabel} accounts for the highest frequency of failed transactions across this reporting window.`;
    }
    return 'Autonomous recovery orchestration is continuously monitoring failed payments and executing multi-channel resolutions.';
  }, [overview, channels, failures]);

  return (
    <div className="space-y-8 font-inter animate-console-fade-in delay-0">
      {/* ==================================================
          1. PAGE HEADER (Directly on page background)
          ================================================== */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between pt-1">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-[#0B8F63] pulse-subtle" />
            <span className="text-[11px] font-bold uppercase tracking-[0.08em] text-[#08704F]">
              Recovery Intelligence
            </span>
          </div>
          <h1 className="font-space-grotesk font-bold text-2xl sm:text-3xl text-[#111318] tracking-tight">
            Analytics &amp; Intelligence
          </h1>
          <p className="text-xs sm:text-sm text-[#667085] leading-relaxed max-w-2xl">
            Understand recovery performance, channel efficiency, and the root causes behind failed payments.
          </p>
        </div>

        <div className="flex items-center gap-3 self-start sm:self-center">
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
          2. REPORTING WINDOW TOOLBAR
          ================================================== */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-3.5 rounded-xl bg-white border border-[#E5E9E6] shadow-2xs animate-console-fade-in delay-1">
        <div>
          <div className="text-[11px] font-bold uppercase tracking-wider text-[#667085]">
            Reporting Window
          </div>
          <div className="text-xs font-mono font-medium text-[#111318]">
            {dateRange.from && dateRange.to ? `${dateRange.from} to ${dateRange.to}` : 'All time'}
          </div>
        </div>

        <DateRangeSelector initialPreset="30d" onChange={handleDateRangeChange} disabled={loading} />
      </div>

      {/* ==================================================
          3. TOP PERFORMANCE SUMMARY (Strong Visual Hierarchy)
          ================================================== */}
      {loading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4 animate-console-fade-in delay-2">
          {[1, 2, 3, 4, 5, 6].map((i) => (
            <SkeletonCard key={i} />
          ))}
        </div>
      ) : error ? (
        <ErrorState title="Failed to Load Analytics" message={error} onRetry={handleRefresh} />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-12 gap-4 animate-console-fade-in delay-2">
          {/* PRIMARY DOMINANT METRIC: RECOVERY RATE */}
          <div className="md:col-span-5 lg:col-span-4 p-5 rounded-2xl bg-white border border-[#E5E9E6] shadow-2xs hover:shadow-md hover:-translate-y-1 transition-all duration-200 flex flex-col justify-between space-y-4">
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-bold uppercase tracking-[0.08em] text-[#08704F]">
                Recovery Rate
              </span>
              <div className="p-2 rounded-lg bg-[#E8F7F0] text-[#08704F] border border-[#0B8F63]/25">
                <TrendingUp className="w-4 h-4 text-[#0B8F63]" />
              </div>
            </div>

            <div className="space-y-1">
              <div className="font-space-grotesk text-4xl sm:text-5xl font-bold text-[#08704F] tracking-tight tabular-nums">
                {overview?.recoveryRate ? (
                  <CountUpMetric value={Number(overview.recoveryRate)} isFloat />
                ) : (
                  '0.0%'
                )}
              </div>
              <p className="text-xs text-[#667085] font-medium flex items-center gap-1.5">
                <span>Closed-loop recovery efficiency</span>
                <span className="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider bg-[#E8F7F0] text-[#08704F]">
                  Primary KPI
                </span>
              </p>
            </div>

            {/* Visual efficiency progress indicator */}
            <div className="space-y-1.5 pt-2 border-t border-[#E5E9E6]/60">
              <div className="flex items-center justify-between text-[11px] text-[#667085] font-mono">
                <span>Resolution efficiency</span>
                <span className="font-bold text-[#111318]">
                  {overview?.recoveryRate ? `${Number(overview.recoveryRate).toFixed(1)}%` : '0.0%'}
                </span>
              </div>
              <div className="w-full bg-[#E5E9E6] h-2 rounded-full overflow-hidden">
                <div
                  className="bg-[#0B8F63] h-full rounded-full transition-all duration-700"
                  style={{
                    width: `${Math.min(100, Math.max(0, Number(overview?.recoveryRate || 0)))}%`,
                  }}
                />
              </div>
            </div>
          </div>

          {/* 5 SUPPORTING METRICS */}
          <div className="md:col-span-7 lg:col-span-8 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
            {/* Supporting 1: Recovered Revenue */}
            <div className="p-4 rounded-xl bg-white border border-[#E5E9E6] shadow-2xs hover:shadow-md hover:-translate-y-0.5 transition-all duration-200 flex flex-col justify-between">
              <div className="flex items-center justify-between text-[#667085] pb-1">
                <span className="text-[11px] font-semibold uppercase tracking-wider text-[#667085]">
                  Recovered Revenue
                </span>
                <div className="p-1 rounded-md bg-[#E8F7F0] text-[#08704F]">
                  <Zap className="w-3.5 h-3.5" />
                </div>
              </div>
              <div className="space-y-0.5 mt-2">
                <div className="font-space-grotesk text-xl font-bold text-[#08704F] tabular-nums truncate">
                  {overview?.totalRecoveredAmount !== undefined ? (
                    <CountUpMetric value={overview.totalRecoveredAmount} formatCurrency />
                  ) : (
                    '₹0'
                  )}
                </div>
                <p className="text-[11px] text-[#667085] truncate font-mono">
                  Of {formatCurrency(overview?.totalEstimatedRecoverableAmount)} at risk
                </p>
              </div>
            </div>

            {/* Supporting 2: Cases Recovered */}
            <div className="p-4 rounded-xl bg-white border border-[#E5E9E6] shadow-2xs hover:shadow-md hover:-translate-y-0.5 transition-all duration-200 flex flex-col justify-between">
              <div className="flex items-center justify-between text-[#667085] pb-1">
                <span className="text-[11px] font-semibold uppercase tracking-wider text-[#667085]">
                  Recovered
                </span>
                <div className="p-1 rounded-md bg-[#E8F7F0] text-[#08704F]">
                  <CheckCircle2 className="w-3.5 h-3.5" />
                </div>
              </div>
              <div className="space-y-0.5 mt-2">
                <div className="font-space-grotesk text-xl font-bold text-[#111318] tabular-nums">
                  {overview?.recoveredCases !== undefined ? (
                    <CountUpMetric value={overview.recoveredCases} />
                  ) : (
                    '0'
                  )}
                </div>
                <p className="text-[11px] text-[#667085] font-mono">Closed-loop successes</p>
              </div>
            </div>

            {/* Supporting 3: Average Recovery */}
            <div className="p-4 rounded-xl bg-white border border-[#E5E9E6] shadow-2xs hover:shadow-md hover:-translate-y-0.5 transition-all duration-200 flex flex-col justify-between">
              <div className="flex items-center justify-between text-[#667085] pb-1">
                <span className="text-[11px] font-semibold uppercase tracking-wider text-[#667085]">
                  Avg Recovery
                </span>
                <div className="p-1 rounded-md bg-[#F1F4F2] text-[#667085]">
                  <ArrowUpRight className="w-3.5 h-3.5" />
                </div>
              </div>
              <div className="space-y-0.5 mt-2">
                <div className="font-space-grotesk text-xl font-bold text-[#111318] tabular-nums truncate">
                  {overview?.averageRecoveredAmount !== undefined ? (
                    <CountUpMetric value={overview.averageRecoveredAmount} formatCurrency />
                  ) : (
                    '₹0'
                  )}
                </div>
                <p className="text-[11px] text-[#667085] font-mono">Per recovered transaction</p>
              </div>
            </div>

            {/* Supporting 4: Average Duration */}
            <div className="p-4 rounded-xl bg-white border border-[#E5E9E6] shadow-2xs hover:shadow-md hover:-translate-y-0.5 transition-all duration-200 flex flex-col justify-between">
              <div className="flex items-center justify-between text-[#667085] pb-1">
                <span className="text-[11px] font-semibold uppercase tracking-wider text-[#667085]">
                  Avg Duration
                </span>
                <div className="p-1 rounded-md bg-[#FEF3C7] text-[#D97706]">
                  <Clock className="w-3.5 h-3.5" />
                </div>
              </div>
              <div className="space-y-0.5 mt-2">
                <div className="font-space-grotesk text-xl font-bold text-[#111318] tabular-nums">
                  {formatDuration(overview?.averageTimeToRecoverySeconds)}
                </div>
                <p className="text-[11px] text-[#667085] font-mono">Time to reconciliation</p>
              </div>
            </div>

            {/* Supporting 5: Total Cases */}
            <div className="p-4 rounded-xl bg-white border border-[#E5E9E6] shadow-2xs hover:shadow-md hover:-translate-y-0.5 transition-all duration-200 flex flex-col justify-between">
              <div className="flex items-center justify-between text-[#667085] pb-1">
                <span className="text-[11px] font-semibold uppercase tracking-wider text-[#667085]">
                  Total Cases
                </span>
                <div className="p-1 rounded-md bg-[#F1F4F2] text-[#667085]">
                  <ShieldAlert className="w-3.5 h-3.5" />
                </div>
              </div>
              <div className="space-y-0.5 mt-2">
                <div className="font-space-grotesk text-xl font-bold text-[#111318] tabular-nums">
                  {overview?.totalCases !== undefined ? (
                    <CountUpMetric value={overview.totalCases} />
                  ) : (
                    '0'
                  )}
                </div>
                <p className="text-[11px] text-[#667085] font-mono">
                  {overview?.openCases ?? 0} open • {overview?.inProgressCases ?? 0} in flight
                </p>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ==================================================
          4. MAIN RECOVERY PERFORMANCE TRENDS
          ================================================== */}
      <div className="space-y-6 animate-console-fade-in delay-3">
        <RecoveryTrendChart
          trends={trends?.trends || []}
          totalAmountAtRisk={Number(trends?.totalAmountAtRisk || 0)}
          totalRecoveredAmount={Number(trends?.totalRecoveredAmount || 0)}
          overallRecoveryRate={Number(trends?.overallRecoveryRate || 0)}
          isLoading={loading}
        />

        {/* ==================================================
            5. CHANNEL PERFORMANCE & FAILURE ROOT CAUSES
            ================================================== */}
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

        {/* ==================================================
            6. DATA-DERIVED RECOVERY INTELLIGENCE INSIGHT
            ================================================== */}
        {recoveryInsight && !loading && (
          <div className="p-4 rounded-xl bg-white border border-[#E5E9E6] shadow-2xs flex items-start gap-3.5 animate-console-fade-in delay-4">
            <div className="p-2 rounded-lg bg-[#E8F7F0] text-[#08704F] border border-[#0B8F63]/20 shrink-0">
              <Sparkles className="w-4 h-4 text-[#0B8F63]" />
            </div>
            <div className="space-y-0.5">
              <div className="text-[10px] font-bold uppercase tracking-[0.08em] text-[#08704F]">
                Recovery Insight
              </div>
              <p className="text-xs text-[#111318] leading-relaxed font-medium">
                {recoveryInsight}
              </p>
            </div>
          </div>
        )}
      </div>

      {/* ==================================================
          7. SHARED LIGHT FINTECH FOOTER
          ================================================== */}
      <Footer />
    </div>
  );
}
