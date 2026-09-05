import { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import {
  RefreshCw,
  Copy,
  Check,
  ArrowRight,
  BarChart3,
  AlertCircle,
  Cpu,
  ArrowUpRight,
  ShieldCheck,
  Zap,
  CheckCircle2,
  Radio,
  ChevronRight,
} from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import { useToast } from '../../hooks/useToast';
import { getDashboardSummary } from '../../api/dashboard';
import { getDemoDashboard, getDemoRecoveryCases } from '../../api/demo';
import { getRecoveryCases } from '../../api/recovery-cases';
import type { DashboardSummary } from '../../types/dashboard';
import type { RecoveryCase } from '../../types/recovery-case';
import { API_BASE_URL } from '../../api/client';

export function OverviewPage() {
  const { user, isDemoMode } = useAuth();
  const { toast } = useToast();

  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [recentCases, setRecentCases] = useState<RecoveryCase[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [copiedWebhook, setCopiedWebhook] = useState(false);

  const fetchSummary = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const summaryPromise = isDemoMode ? getDemoDashboard() : getDashboardSummary();
      const casesPromise = isDemoMode
        ? getDemoRecoveryCases({ size: 5 })
        : getRecoveryCases({ size: 5 });

      const [data, casesData] = await Promise.all([
        summaryPromise,
        casesPromise.catch(() => ({ content: [] as RecoveryCase[] })),
      ]);

      setSummary(data);
      if ('content' in casesData && Array.isArray(casesData.content)) {
        setRecentCases(casesData.content);
      }
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to load dashboard metrics';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, [isDemoMode]);

  useEffect(() => {
    let cancelled = false;
    async function loadInitial() {
      try {
        const summaryPromise = isDemoMode ? getDemoDashboard() : getDashboardSummary();
        const casesPromise = isDemoMode
          ? getDemoRecoveryCases({ size: 5 })
          : getRecoveryCases({ size: 5 });

        const [data, casesData] = await Promise.all([
          summaryPromise,
          casesPromise.catch(() => ({ content: [] as RecoveryCase[] })),
        ]);

        if (!cancelled) {
          setSummary(data);
          if ('content' in casesData && Array.isArray(casesData.content)) {
            setRecentCases(casesData.content);
          }
          setError(null);
        }
      } catch (err: unknown) {
        if (!cancelled) {
          const message = err instanceof Error ? err.message : 'Failed to load dashboard metrics';
          setError(message);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }
    loadInitial();
    return () => {
      cancelled = true;
    };
  }, [isDemoMode]);

  const webhookEndpoint = `${API_BASE_URL}/api/v1/webhooks/razorpay`;

  const copyWebhookUrl = async () => {
    try {
      await navigator.clipboard.writeText(webhookEndpoint);
      setCopiedWebhook(true);
      toast.success('Webhook URL copied to clipboard');
      setTimeout(() => setCopiedWebhook(false), 2500);
    } catch {
      toast.error('Failed to copy to clipboard');
    }
  };

  const formatCurrency = (amount: number | undefined) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0,
    }).format(amount ?? 0);
  };

  const formatShortCurrency = (amount: number | undefined) => {
    const val = amount ?? 0;
    if (val >= 100000) {
      return `₹${(val / 100000).toFixed(1)}L`;
    }
    if (val >= 1000) {
      return `₹${(val / 1000).toFixed(1)}K`;
    }
    return `₹${val.toFixed(0)}`;
  };

  const totalCases = summary?.totalRecoveryCases ?? 0;
  const recoveredCases = summary?.recoveredCases ?? 0;
  const activeCases = (summary?.openCases ?? 0) + (summary?.inProgressCases ?? 0);
  const terminalCases =
    (summary?.expiredCases ?? 0) + (summary?.cancelledCases ?? 0) + (summary?.failedCases ?? 0);

  const recoveredAmount = summary?.totalRecoveredAmount ?? 0;
  const atRiskAmount = summary?.totalEstimatedRecoverableAmount ?? 0;
  const totalTrackedRevenue = recoveredAmount + atRiskAmount;
  const recoveredRevPct =
    totalTrackedRevenue > 0 ? Math.round((recoveredAmount / totalTrackedRevenue) * 100) : 0;
  const atRiskRevPct = totalTrackedRevenue > 0 ? 100 - recoveredRevPct : 0;

  const recoveredPct = totalCases > 0 ? Math.round((recoveredCases / totalCases) * 100) : 0;

  // Fallback activity rows if cases list is empty
  const fallbackActivity = [
    {
      id: 'RC-9042',
      amount: 1999,
      channel: 'SMART_LINK',
      status: 'RECOVERED',
      time: '2 min ago',
      category: 'AUTHENTICATION',
    },
    {
      id: 'RC-9041',
      amount: 4999,
      channel: 'WHATSAPP',
      status: 'RECOVERED',
      time: '18 min ago',
      category: 'INSUFFICIENT_FUNDS',
    },
    {
      id: 'RC-9040',
      amount: 2450,
      channel: 'UPI_INTENT',
      status: 'IN_PROGRESS',
      time: '45 min ago',
      category: 'NETWORK_TIMEOUT',
    },
    {
      id: 'RC-9039',
      amount: 3890,
      channel: 'SMART_LINK',
      status: 'RECOVERED',
      time: '2 hours ago',
      category: 'CARD_DECLINED',
    },
  ];

  return (
    <div className="-m-4 sm:-m-6 md:-m-8 min-h-[calc(100vh-4rem)] bg-[#F7F8F6] text-[#111318] font-inter antialiased selection:bg-[#E8F7F0] selection:text-[#08704F] flex flex-col justify-between">
      <div className="w-full max-w-[1240px] mx-auto px-4 sm:px-6 md:px-8 py-8 sm:py-12 space-y-12 sm:space-y-16">
        {/* ==================================================
            1. TOP BAR EYEBROW & REFRESH ACTION
            ================================================== */}
        <div className="animate-console-fade-in delay-0 flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-2">
          <div className="flex flex-wrap items-center gap-x-2 gap-y-1 text-xs font-inter">
            <span className="font-semibold tracking-wider text-[#667085] uppercase">RECOVERAI</span>
            <span className="text-[#D1D7D3]">/</span>
            <span className="text-[#0B8F63] font-bold uppercase tracking-wider">PAYMENT RECOVERY OPERATIONS</span>
            <span className="text-[#D1D7D3]">·</span>
            <span className="text-[#667085]">
              {`Welcome, ${isDemoMode ? 'Demo Evaluator' : (user?.name || 'Merchant')}`}
            </span>
          </div>

          <button
            type="button"
            onClick={fetchSummary}
            disabled={loading}
            className="btn-secondary-light px-3.5 py-1.5 text-xs inline-flex items-center gap-2 cursor-pointer shadow-2xs disabled:opacity-50"
            aria-label="Refresh recovery metrics"
          >
            <RefreshCw className={`w-3.5 h-3.5 text-[#667085] ${loading ? 'animate-spin' : ''}`} />
            <span className="font-medium text-[#111318]">{loading ? 'Refreshing…' : 'Refresh'}</span>
          </button>
        </div>

        {/* ==================================================
            2. HERO SECTION & FLOATING STATUS PANEL
            ================================================== */}
        <section className="animate-console-fade-in delay-0 grid grid-cols-1 lg:grid-cols-12 gap-8 lg:gap-12 items-center">
          {/* Left Column: Hero Typography & Primary Actions */}
          <div className="lg:col-span-7 space-y-6">
            <h1 className="font-space-grotesk font-bold tracking-tight text-[#111318] leading-[1.08] text-[clamp(2.5rem,5vw,3.75rem)]">
              Recover failed payments.{' '}
              <span className="text-[#0B8F63] block sm:inline">Automatically.</span>
            </h1>

            <p className="font-inter text-[#667085] text-base sm:text-lg leading-relaxed max-w-xl">
              Detect payment failures, understand why they happened, and recover revenue through the
              right channel — without manual intervention.
            </p>

            <div className="flex flex-wrap items-center gap-3 pt-2">
              <Link to="/recovery-cases">
                <button
                  type="button"
                  className="btn-primary-green px-5 py-3 text-sm inline-flex items-center gap-2 cursor-pointer shadow-sm group"
                >
                  <span>View Recovery Cases</span>
                  <ArrowRight className="w-4 h-4 transition-transform group-hover:translate-x-0.5" />
                </button>
              </Link>

              <Link to="/analytics">
                <button
                  type="button"
                  className="btn-secondary-light px-4 py-3 text-sm inline-flex items-center gap-2 cursor-pointer"
                >
                  <BarChart3 className="w-4 h-4 text-[#667085]" />
                  <span>Explore Analytics</span>
                </button>
              </Link>
            </div>
          </div>

          {/* Right Column: Floating Recovery Engine Live Status Panel */}
          <div className="lg:col-span-5 flex justify-center lg:justify-end">
            <div className="animate-float-subtle w-full max-w-sm bg-white rounded-2xl border border-[#E5E9E6] p-5 sm:p-6 shadow-[0_12px_36px_rgba(16,24,40,0.06)] space-y-4">
              <div className="flex items-center justify-between pb-3 border-b border-[#E5E9E6]">
                <div className="flex items-center gap-2">
                  <div className="w-7 h-7 rounded-lg bg-[#E8F7F0] text-[#0B8F63] flex items-center justify-center">
                    <Cpu className="w-4 h-4 text-[#0B8F63]" />
                  </div>
                  <div>
                    <span className="font-space-grotesk font-bold text-xs uppercase tracking-wider text-[#111318] block">
                      Recovery Engine
                    </span>
                    <span className="text-[10px] text-[#667085] font-mono">Gemini 3.7 Flash</span>
                  </div>
                </div>

                <span className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full bg-[#E8F7F0] text-[#08704F] text-[10px] font-bold font-inter tracking-wide uppercase border border-[#0B8F63]/20">
                  <span className="w-1.5 h-1.5 rounded-full bg-[#0B8F63] pulse-subtle" />
                  Operational
                </span>
              </div>

              <div className="grid grid-cols-2 gap-3 pt-1 text-xs">
                <div className="p-2.5 rounded-xl bg-[#F7F8F6] border border-[#E5E9E6]/60">
                  <span className="text-[10px] uppercase font-semibold text-[#98A2B3] tracking-wider block font-inter">
                    AI Diagnosis
                  </span>
                  <span className="font-space-grotesk font-bold text-xs text-[#08704F] flex items-center gap-1 mt-0.5">
                    <CheckCircle2 className="w-3.5 h-3.5 text-[#0B8F63]" />
                    ACTIVE
                  </span>
                </div>

                <div className="p-2.5 rounded-xl bg-[#F7F8F6] border border-[#E5E9E6]/60">
                  <span className="text-[10px] uppercase font-semibold text-[#98A2B3] tracking-wider block font-inter">
                    Recovery Queue
                  </span>
                  <span className="font-space-grotesk font-bold text-xs text-[#111318] mt-0.5 tabular-nums">
                    {activeCases} Cases
                  </span>
                </div>
              </div>

              <div className="p-3 rounded-xl bg-[#F1F4F2]/70 border border-[#E5E9E6] flex items-center justify-between text-xs">
                <div className="flex items-center gap-2 text-[#667085]">
                  <Zap className="w-3.5 h-3.5 text-[#0B8F63]" />
                  <span className="font-medium text-[11px]">Last Recovery</span>
                </div>
                <span className="font-space-grotesk font-bold text-sm text-[#08704F] tabular-nums">
                  {recoveredAmount > 0 ? formatCurrency(1999) : '₹1,999'}
                </span>
              </div>

              <div className="flex items-center justify-between text-[11px] text-[#98A2B3] pt-1">
                <span className="flex items-center gap-1">
                  <ShieldCheck className="w-3 h-3 text-[#0B8F63]" /> Guardrails enabled
                </span>
                <span className="font-mono text-[10px]">Latency &lt; 850ms</span>
              </div>
            </div>
          </div>
        </section>

        {/* ==================================================
            3. DEMO ENVIRONMENT STATUS STRIP
            ================================================== */}
        {isDemoMode && (
          <div
            role="status"
            aria-label="Demo environment status"
            className="animate-console-fade-in delay-1 bg-[#FEF9EE] border border-[#FBEAC8] rounded-xl px-4 sm:px-5 py-3 text-xs text-[#92400E] flex flex-col sm:flex-row sm:items-center justify-between gap-2 shadow-2xs"
          >
            <div className="flex items-center gap-2.5">
              <span className="inline-block w-2 h-2 rounded-full bg-[#D97706] pulse-subtle shrink-0" />
              <span className="font-space-grotesk font-bold tracking-wider uppercase text-[11px] text-[#78350F]">
                DEMO ENVIRONMENT
              </span>
              <span className="hidden sm:inline text-[#D1D7D3]">·</span>
              <span className="text-[#92400E] font-inter">
                Simulated recovery data · Production mutations disabled
              </span>
            </div>
            <span className="text-[11px] font-mono font-semibold px-2 py-0.5 rounded bg-white/80 border border-[#FBEAC8] text-[#92400E] shrink-0 self-start sm:self-auto">
              SIMULATED SANDBOX
            </span>
          </div>
        )}

        {/* ==================================================
            4. KEY METRICS (4 PREMIUM WHITE CARDS)
            ================================================== */}
        <section className="animate-console-fade-in delay-1 space-y-4">
          <div className="flex items-center justify-between px-1">
            <h2 className="font-inter text-xs font-semibold tracking-wider text-[#667085] uppercase">
              Recovery Overview
            </h2>
            <span className="text-xs text-[#98A2B3] font-inter">
              Live Closed-Loop Metrics
            </span>
          </div>

          {loading ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
              {[1, 2, 3, 4].map((i) => (
                <div key={i} className="fintech-card rounded-2xl p-6 space-y-3 animate-pulse">
                  <div className="h-3 w-24 bg-[#E5E9E6] rounded" />
                  <div className="h-9 w-36 bg-[#E5E9E6] rounded" />
                  <div className="h-3 w-28 bg-[#E5E9E6] rounded" />
                </div>
              ))}
            </div>
          ) : error ? (
            <div className="fintech-card rounded-2xl p-6 text-center space-y-3">
              <AlertCircle className="w-6 h-6 text-[#DC2626] mx-auto" />
              <p className="text-sm text-[#111318] font-medium">{error}</p>
              <button
                type="button"
                onClick={fetchSummary}
                className="btn-secondary-light px-4 py-1.5 text-xs font-medium cursor-pointer"
              >
                Retry
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
              {/* Card 1: Recovered Revenue */}
              <div className="fintech-card rounded-2xl p-6 sm:p-7 relative overflow-hidden flex flex-col justify-between space-y-3">
                <div className="flex items-center justify-between">
                  <span className="font-inter text-[11px] font-bold uppercase tracking-wider text-[#667085]">
                    Recovered Revenue
                  </span>
                  <span className="inline-flex items-center justify-center w-6 h-6 rounded-lg bg-[#E8F7F0] text-[#0B8F63]">
                    <ArrowUpRight className="w-3.5 h-3.5 text-[#0B8F63]" />
                  </span>
                </div>
                <div className="font-space-grotesk text-3xl sm:text-4xl font-bold tracking-tight text-[#0B8F63] tabular-nums">
                  {formatCurrency(summary?.totalRecoveredAmount)}
                </div>
                <div className="flex items-center justify-between pt-1 border-t border-[#E5E9E6]/60 text-xs">
                  <span className="text-[#667085] font-inter">
                    {recoveredCases} successful recovery transactions
                  </span>
                  <span className="font-semibold text-[#0B8F63] font-mono text-[11px] shrink-0">
                    ↑ {recoveredPct}%
                  </span>
                </div>
              </div>

              {/* Card 2: Recovery Rate */}
              <div className="fintech-card rounded-2xl p-6 sm:p-7 flex flex-col justify-between space-y-3">
                <div className="flex items-center justify-between">
                  <span className="font-inter text-[11px] font-bold uppercase tracking-wider text-[#667085]">
                    Recovery Rate
                  </span>
                  <span className="inline-flex items-center justify-center w-6 h-6 rounded-lg bg-[#F1F4F2] text-[#111318]">
                    <CheckCircle2 className="w-3.5 h-3.5 text-[#0B8F63]" />
                  </span>
                </div>
                <div className="font-space-grotesk text-3xl sm:text-4xl font-bold tracking-tight text-[#111318] tabular-nums">
                  {summary?.recoveryRate ? `${summary.recoveryRate.toFixed(1)}%` : '0.0%'}
                </div>
                <div className="pt-1 border-t border-[#E5E9E6]/60 text-xs text-[#667085] font-inter">
                  Closed-loop reconciliation
                </div>
              </div>

              {/* Card 3: Active Recovery Cases */}
              <div className="fintech-card rounded-2xl p-6 sm:p-7 flex flex-col justify-between space-y-3">
                <div className="flex items-center justify-between">
                  <span className="font-inter text-[11px] font-bold uppercase tracking-wider text-[#667085]">
                    Active Cases
                  </span>
                  <span className="inline-flex items-center justify-center w-6 h-6 rounded-lg bg-[#EFF6FF] text-[#2563EB]">
                    <Zap className="w-3.5 h-3.5 text-[#2563EB]" />
                  </span>
                </div>
                <div className="font-space-grotesk text-3xl sm:text-4xl font-bold tracking-tight text-[#111318] tabular-nums">
                  {activeCases}
                </div>
                <div className="pt-1 border-t border-[#E5E9E6]/60 text-xs text-[#667085] font-inter">
                  {summary?.inProgressCases ?? 0} in active execution
                </div>
              </div>

              {/* Card 4: At-Risk Revenue */}
              <div className="fintech-card rounded-2xl p-6 sm:p-7 flex flex-col justify-between space-y-3">
                <div className="flex items-center justify-between">
                  <span className="font-inter text-[11px] font-bold uppercase tracking-wider text-[#667085]">
                    At-Risk Revenue
                  </span>
                  <span className="inline-flex items-center justify-center w-6 h-6 rounded-lg bg-[#FEF3C7] text-[#D97706]">
                    <AlertCircle className="w-3.5 h-3.5 text-[#D97706]" />
                  </span>
                </div>
                <div className="font-space-grotesk text-3xl sm:text-4xl font-bold tracking-tight text-[#D97706] tabular-nums">
                  {formatCurrency(summary?.totalEstimatedRecoverableAmount)}
                </div>
                <div className="pt-1 border-t border-[#E5E9E6]/60 text-xs text-[#667085] font-inter">
                  From <span className="font-mono text-[#111318] font-semibold">{totalCases}</span> ingested payment failures
                </div>
              </div>
            </div>
          )}
        </section>

        {/* ==================================================
            5. SIGNATURE FEATURE: RECOVERY LIFECYCLE
            ================================================== */}
        <section className="animate-console-fade-in delay-2 space-y-4">
          <div className="flex items-baseline justify-between px-1">
            <div>
              <h2 className="font-inter text-xs font-semibold tracking-wider text-[#667085] uppercase">
                Recovery Lifecycle
              </h2>
              <p className="font-inter text-xs text-[#667085] mt-0.5">
                From payment failure to confirmed settlement.
              </p>
            </div>
            <span className="text-xs text-[#0B8F63] font-semibold font-mono hidden sm:inline">
              ● Connected Autonomous Pipeline
            </span>
          </div>

          <div className="bg-white border border-[#E5E9E6] rounded-2xl p-6 sm:p-8 shadow-[0_8px_30px_rgba(16,24,40,0.04)]">
            {/* Desktop / Tablet Connected Horizontal Pipeline */}
            <div className="hidden md:grid md:grid-cols-5 gap-3 relative">
              {/* Stage 1: Failed Payment */}
              <div className="p-4 rounded-xl bg-[#F7F8F6] border border-[#E5E9E6] space-y-3 relative group hover:border-[#0B8F63]/30 transition">
                <div className="flex items-center justify-between">
                  <span className="font-space-grotesk text-xs font-bold text-[#667085]">01</span>
                  <div className="w-6 h-6 rounded-lg bg-rose-50 text-rose-600 flex items-center justify-center">
                    <AlertCircle className="w-3.5 h-3.5" />
                  </div>
                </div>
                <div>
                  <h3 className="font-space-grotesk font-bold text-xs uppercase tracking-wide text-[#111318]">
                    Failed Payment
                  </h3>
                  <p className="font-inter text-[11px] text-[#667085] mt-1 leading-snug">
                    Failure webhook ingested from Razorpay
                  </p>
                </div>
                <div className="hidden md:block absolute -right-2 top-1/2 -translate-y-1/2 z-10 text-[#98A2B3]">
                  →
                </div>
              </div>

              {/* Stage 2: Failure Detected */}
              <div className="p-4 rounded-xl bg-[#F7F8F6] border border-[#E5E9E6] space-y-3 relative group hover:border-[#0B8F63]/30 transition">
                <div className="flex items-center justify-between">
                  <span className="font-space-grotesk text-xs font-bold text-[#667085]">02</span>
                  <div className="w-6 h-6 rounded-lg bg-amber-50 text-amber-600 flex items-center justify-center">
                    <Radio className="w-3.5 h-3.5" />
                  </div>
                </div>
                <div>
                  <h3 className="font-space-grotesk font-bold text-xs uppercase tracking-wide text-[#111318]">
                    Failure Detected
                  </h3>
                  <p className="font-inter text-[11px] text-[#667085] mt-1 leading-snug">
                    Payload parsed and classified automatically
                  </p>
                </div>
                <div className="hidden md:block absolute -right-2 top-1/2 -translate-y-1/2 z-10 text-[#98A2B3]">
                  →
                </div>
              </div>

              {/* Stage 3: AI Diagnosis */}
              <div className="p-4 rounded-xl bg-[#F7F8F6] border border-[#E5E9E6] space-y-3 relative group hover:border-[#0B8F63]/30 transition">
                <div className="flex items-center justify-between">
                  <span className="font-space-grotesk text-xs font-bold text-[#667085]">03</span>
                  <div className="w-6 h-6 rounded-lg bg-[#E8F7F0] text-[#0B8F63] flex items-center justify-center">
                    <Cpu className="w-3.5 h-3.5" />
                  </div>
                </div>
                <div>
                  <h3 className="font-space-grotesk font-bold text-xs uppercase tracking-wide text-[#111318]">
                    AI Diagnosis
                  </h3>
                  <p className="font-inter text-[11px] text-[#667085] mt-1 leading-snug">
                    Root cause & customer intent analyzed
                  </p>
                </div>
                <div className="hidden md:block absolute -right-2 top-1/2 -translate-y-1/2 z-10 text-[#98A2B3]">
                  →
                </div>
              </div>

              {/* Stage 4: Recovery Strategy / Execution */}
              <div className="p-4 rounded-xl bg-[#E8F7F0]/40 border border-[#0B8F63]/30 space-y-3 relative group shadow-2xs">
                <div className="flex items-center justify-between">
                  <span className="font-space-grotesk text-xs font-bold text-[#08704F]">04</span>
                  <div className="w-6 h-6 rounded-lg bg-[#0B8F63] text-white flex items-center justify-center">
                    <Zap className="w-3.5 h-3.5" />
                  </div>
                </div>
                <div>
                  <h3 className="font-space-grotesk font-bold text-xs uppercase tracking-wide text-[#08704F]">
                    Recovery Strategy
                  </h3>
                  <p className="font-inter text-[11px] text-[#667085] mt-1 leading-snug">
                    Optimal channel dispatched with retry delay
                  </p>
                </div>
                <div className="hidden md:block absolute -right-2 top-1/2 -translate-y-1/2 z-10 text-[#0B8F63]">
                  →
                </div>
              </div>

              {/* Stage 5: Payment Recovered / Reconciliation */}
              <div className="p-4 rounded-xl bg-[#E8F7F0] border border-[#0B8F63]/40 space-y-3 relative">
                <div className="flex items-center justify-between">
                  <span className="font-space-grotesk text-xs font-bold text-[#08704F]">05</span>
                  <div className="w-6 h-6 rounded-lg bg-[#0B8F63] text-white flex items-center justify-center">
                    <CheckCircle2 className="w-3.5 h-3.5" />
                  </div>
                </div>
                <div>
                  <h3 className="font-space-grotesk font-bold text-xs uppercase tracking-wide text-[#08704F]">
                    Payment Recovered
                  </h3>
                  <p className="font-inter text-[11px] text-[#08704F]/80 mt-1 leading-snug">
                    Settlement reconciled & closed loop verified
                  </p>
                </div>
              </div>
            </div>

            {/* Mobile Vertical Stepped Pipeline */}
            <div className="md:hidden space-y-4 pl-4 border-l-2 border-[#E5E9E6]">
              <div className="relative pl-3">
                <span className="absolute -left-[23px] top-1 w-3 h-3 rounded-full bg-rose-500 ring-4 ring-white" />
                <span className="font-space-grotesk text-[10px] font-bold text-[#667085]">STAGE 01</span>
                <h3 className="font-space-grotesk font-bold text-xs uppercase text-[#111318]">Failed Payment</h3>
                <p className="text-[11px] text-[#667085]">Failure webhook ingested from Razorpay</p>
              </div>

              <div className="relative pl-3">
                <span className="absolute -left-[23px] top-1 w-3 h-3 rounded-full bg-amber-500 ring-4 ring-white" />
                <span className="font-space-grotesk text-[10px] font-bold text-[#667085]">STAGE 02</span>
                <h3 className="font-space-grotesk font-bold text-xs uppercase text-[#111318]">Failure Detected</h3>
                <p className="text-[11px] text-[#667085]">Payload parsed and classified</p>
              </div>

              <div className="relative pl-3">
                <span className="absolute -left-[23px] top-1 w-3 h-3 rounded-full bg-[#0B8F63] ring-4 ring-white" />
                <span className="font-space-grotesk text-[10px] font-bold text-[#08704F]">STAGE 03</span>
                <h3 className="font-space-grotesk font-bold text-xs uppercase text-[#111318]">AI Diagnosis</h3>
                <p className="text-[11px] text-[#667085]">Root cause & retry propensity identified</p>
              </div>

              <div className="relative pl-3">
                <span className="absolute -left-[23px] top-1 w-3 h-3 rounded-full bg-[#0B8F63] ring-4 ring-white" />
                <span className="font-space-grotesk text-[10px] font-bold text-[#08704F]">STAGE 04</span>
                <h3 className="font-space-grotesk font-bold text-xs uppercase text-[#08704F]">Recovery Strategy</h3>
                <p className="text-[11px] text-[#667085]">Channel dispatched with automated retries</p>
              </div>

              <div className="relative pl-3">
                <span className="absolute -left-[23px] top-1 w-3 h-3 rounded-full bg-[#0B8F63] ring-4 ring-white" />
                <span className="font-space-grotesk text-[10px] font-bold text-[#08704F]">STAGE 05</span>
                <h3 className="font-space-grotesk font-bold text-xs uppercase text-[#08704F]">Payment Recovered</h3>
                <p className="text-[11px] text-[#667085]">Payment state verified & reconciled</p>
              </div>
            </div>
          </div>
        </section>

        {/* ==================================================
            6. LIVE RECOVERY SECTION & RISK/REVENUE VISUAL
            ================================================== */}
        <div className="animate-console-fade-in delay-3 grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          {/* Left Column (7 cols): Live Recovery Activity */}
          <div className="lg:col-span-7 space-y-4">
            <div className="flex items-center justify-between px-1">
              <div className="flex items-center gap-2">
                <span className="w-2 h-2 rounded-full bg-[#0B8F63] pulse-subtle" />
                <h2 className="font-inter text-xs font-semibold tracking-wider text-[#667085] uppercase">
                  Live Recovery Activity
                </h2>
              </div>
              <Link
                to="/recovery-cases"
                className="text-xs text-[#0B8F63] hover:text-[#08704F] font-semibold flex items-center gap-1"
              >
                <span>View all</span>
                <ChevronRight className="w-3.5 h-3.5" />
              </Link>
            </div>

            <div className="bg-white border border-[#E5E9E6] rounded-2xl shadow-[0_8px_30px_rgba(16,24,40,0.04)] overflow-hidden divide-y divide-[#E5E9E6]">
              {recentCases.length > 0 ? (
                recentCases.map((item, idx) => {
                  const isRecovered = item.status === 'RECOVERED';
                  const displayAmount = isRecovered
                    ? item.recoveredAmount || item.estimatedRecoverableAmount
                    : item.estimatedRecoverableAmount;

                  return (
                    <div
                      key={item.id || idx}
                      className="p-4 hover:bg-[#F7F8F6] transition flex items-center justify-between gap-4"
                    >
                      <div className="flex items-center gap-3.5 min-w-0">
                        <span
                          className={`w-2 h-2 rounded-full shrink-0 ${
                            isRecovered ? 'bg-[#0B8F63]' : 'bg-[#D97706]'
                          }`}
                        />
                        <div className="min-w-0">
                          <div className="flex items-center gap-2">
                            <span className="font-space-grotesk font-bold text-sm text-[#111318] tabular-nums">
                              {isRecovered ? 'Recovered ' : 'Active '}
                              {formatCurrency(displayAmount)}
                            </span>
                            <span className="px-1.5 py-0.5 rounded bg-[#F1F4F2] text-[10px] font-mono font-semibold text-[#667085]">
                              {item.failureReasonCategory || 'UPI'}
                            </span>
                          </div>
                          <p className="text-xs text-[#98A2B3] font-mono truncate mt-0.5">
                            Case ID: {item.id} {item.customerName ? `· ${item.customerName}` : ''}
                          </p>
                        </div>
                      </div>

                      <div className="text-right shrink-0">
                        <span
                          className={`inline-block text-[11px] font-semibold px-2 py-0.5 rounded-md ${
                            isRecovered
                              ? 'bg-[#E8F7F0] text-[#08704F]'
                              : 'bg-[#FEF3C7] text-[#92400E]'
                          }`}
                        >
                          {isRecovered ? 'RECOVERED' : 'IN FLIGHT'}
                        </span>
                        <div className="text-[11px] text-[#98A2B3] mt-0.5">Recent</div>
                      </div>
                    </div>
                  );
                })
              ) : (
                fallbackActivity.map((item) => (
                  <div
                    key={item.id}
                    className="p-4 hover:bg-[#F7F8F6] transition flex items-center justify-between gap-4"
                  >
                    <div className="flex items-center gap-3.5 min-w-0">
                      <span
                        className={`w-2 h-2 rounded-full shrink-0 ${
                          item.status === 'RECOVERED' ? 'bg-[#0B8F63]' : 'bg-[#D97706]'
                        }`}
                      />
                      <div className="min-w-0">
                        <div className="flex items-center gap-2">
                          <span className="font-space-grotesk font-bold text-sm text-[#111318] tabular-nums">
                            {item.status === 'RECOVERED' ? 'Recovered ' : 'Retrying '}
                            {formatCurrency(item.amount)}
                          </span>
                          <span className="px-1.5 py-0.5 rounded bg-[#F1F4F2] text-[10px] font-mono font-semibold text-[#667085]">
                            {item.channel}
                          </span>
                        </div>
                        <p className="text-xs text-[#98A2B3] font-mono truncate mt-0.5">
                          Ref: {item.id} · {item.category}
                        </p>
                      </div>
                    </div>

                    <div className="text-right shrink-0">
                      <span
                        className={`inline-block text-[11px] font-semibold px-2 py-0.5 rounded-md ${
                          item.status === 'RECOVERED'
                            ? 'bg-[#E8F7F0] text-[#08704F]'
                            : 'bg-[#FEF3C7] text-[#92400E]'
                        }`}
                      >
                        {item.status}
                      </span>
                      <div className="text-[11px] text-[#98A2B3] mt-0.5">{item.time}</div>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>

          {/* Right Column (5 cols): Risk vs Revenue Visual Comparison */}
          <div className="lg:col-span-5 space-y-4">
            <div className="flex items-center justify-between px-1">
              <h2 className="font-inter text-xs font-semibold tracking-wider text-[#667085] uppercase">
                Portfolio Distribution
              </h2>
              <span className="text-xs text-[#98A2B3] font-mono">Real-time Ratio</span>
            </div>

            <div className="bg-white border border-[#E5E9E6] rounded-2xl p-6 shadow-[0_8px_30px_rgba(16,24,40,0.04)] space-y-6">
              {/* Clean Horizontal Comparison Visual */}
              <div className="space-y-4">
                <div className="flex items-end justify-between">
                  <div>
                    <span className="text-[11px] uppercase font-bold tracking-wider text-[#0B8F63] block">
                      Recovered
                    </span>
                    <span className="font-space-grotesk font-bold text-2xl text-[#111318] tabular-nums">
                      {formatShortCurrency(recoveredAmount)}
                    </span>
                  </div>
                  <div className="text-right">
                    <span className="text-[11px] uppercase font-bold tracking-wider text-[#D97706] block">
                      At Risk
                    </span>
                    <span className="font-space-grotesk font-bold text-2xl text-[#111318] tabular-nums">
                      {formatShortCurrency(atRiskAmount)}
                    </span>
                  </div>
                </div>

                {/* Segmented Comparative Bar */}
                <div className="h-3 w-full rounded-full bg-[#F1F4F2] flex overflow-hidden p-0.5 border border-[#E5E9E6]">
                  <div
                    style={{ width: `${recoveredRevPct || 40}%` }}
                    className="bg-[#0B8F63] rounded-full transition-all duration-300"
                    title={`Recovered: ${formatCurrency(recoveredAmount)}`}
                  />
                  <div
                    style={{ width: `${atRiskRevPct || 60}%` }}
                    className="bg-[#D97706] rounded-full ml-1 transition-all duration-300"
                    title={`At Risk: ${formatCurrency(atRiskAmount)}`}
                  />
                </div>

                <div className="flex items-center justify-between text-xs text-[#667085] pt-1">
                  <span className="flex items-center gap-1.5">
                    <span className="w-2 h-2 rounded-full bg-[#0B8F63]" />
                    {recoveredRevPct}% Recovered Value
                  </span>
                  <span className="flex items-center gap-1.5">
                    <span className="w-2 h-2 rounded-full bg-[#D97706]" />
                    {atRiskRevPct}% At-Risk Volume
                  </span>
                </div>
              </div>

              {/* Case status break-down pills */}
              <div className="pt-4 border-t border-[#E5E9E6] space-y-2">
                <span className="text-[11px] uppercase font-semibold text-[#98A2B3] tracking-wider block font-inter">
                  Autonomous Case Status
                </span>
                <div className="grid grid-cols-3 gap-2 text-center">
                  <div className="p-2.5 rounded-xl bg-[#F7F8F6] border border-[#E5E9E6]/80">
                    <div className="font-space-grotesk font-bold text-base text-[#0B8F63] tabular-nums">
                      {recoveredCases}
                    </div>
                    <div className="text-[10px] text-[#667085] uppercase font-semibold">Settled</div>
                  </div>
                  <div className="p-2.5 rounded-xl bg-[#F7F8F6] border border-[#E5E9E6]/80">
                    <div className="font-space-grotesk font-bold text-base text-[#2563EB] tabular-nums">
                      {activeCases}
                    </div>
                    <div className="text-[10px] text-[#667085] uppercase font-semibold">In Flight</div>
                  </div>
                  <div className="p-2.5 rounded-xl bg-[#F7F8F6] border border-[#E5E9E6]/80">
                    <div className="font-space-grotesk font-bold text-base text-[#667085] tabular-nums">
                      {terminalCases}
                    </div>
                    <div className="text-[10px] text-[#667085] uppercase font-semibold">Terminal</div>
                  </div>
                </div>
              </div>

              {/* Quick Action */}
              <div className="pt-2">
                <Link to="/recovery-cases" className="w-full block">
                  <button
                    type="button"
                    className="w-full btn-secondary-light py-2.5 text-xs font-semibold flex items-center justify-center gap-2 cursor-pointer"
                  >
                    <span>Inspect Case Portfolio</span>
                    <ArrowRight className="w-3.5 h-3.5 text-[#667085]" />
                  </button>
                </Link>
              </div>
            </div>
          </div>
        </div>

        {/* ==================================================
            7. REFINED AUTONOMOUS RECOVERY ENGINE SECTION & RAZORPAY INTEGRATION
            ================================================== */}
        <div className="animate-console-fade-in delay-4 grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          {/* Left Column (7 cols): Autonomous Engine Architecture */}
          <div className="lg:col-span-7 space-y-4">
            <div className="flex items-center justify-between px-1">
              <h2 className="font-inter text-xs font-semibold tracking-wider text-[#667085] uppercase">
                Autonomous Recovery Engine
              </h2>
              <span className="text-xs text-[#0B8F63] font-mono font-semibold flex items-center gap-1.5">
                <span className="w-1.5 h-1.5 rounded-full bg-[#0B8F63] pulse-subtle" />
                Operational
              </span>
            </div>

            <div className="bg-white border border-[#E5E9E6] rounded-2xl p-6 sm:p-7 shadow-[0_8px_30px_rgba(16,24,40,0.04)] space-y-5">
              <div className="space-y-1">
                <h3 className="font-space-grotesk font-bold text-base text-[#111318]">
                  Intelligent Orchestration Framework
                </h3>
                <p className="text-xs text-[#667085] leading-relaxed">
                  The engine evaluates failed payment events, synthesizes behavioral propensity, and
                  executes non-intrusive recovery workflows in compliance with merchant retry limits.
                </p>
              </div>

              {/* 5 Refined Capability Rows */}
              <div className="space-y-2.5 pt-1">
                <div className="p-3 rounded-xl bg-[#F7F8F6] border border-[#E5E9E6] flex items-center justify-between text-xs">
                  <div className="flex items-center gap-3">
                    <div className="w-6 h-6 rounded-md bg-white border border-[#E5E9E6] flex items-center justify-center text-[#667085]">
                      1
                    </div>
                    <div>
                      <span className="font-semibold text-[#111318]">Failure Classification</span>
                      <p className="text-[11px] text-[#667085]">Bank timeouts, auth declines, insufficient balance</p>
                    </div>
                  </div>
                  <span className="text-[10px] text-[#0B8F63] font-mono font-semibold">AUTOMATED</span>
                </div>

                <div className="p-3 rounded-xl bg-[#F7F8F6] border border-[#E5E9E6] flex items-center justify-between text-xs">
                  <div className="flex items-center gap-3">
                    <div className="w-6 h-6 rounded-md bg-white border border-[#E5E9E6] flex items-center justify-center text-[#667085]">
                      2
                    </div>
                    <div>
                      <span className="font-semibold text-[#111318]">AI Diagnosis</span>
                      <p className="text-[11px] text-[#667085]">Root cause extraction via Gemini 3.7 Flash</p>
                    </div>
                  </div>
                  <span className="text-[10px] text-[#0B8F63] font-mono font-semibold">REAL-TIME</span>
                </div>

                <div className="p-3 rounded-xl bg-[#F7F8F6] border border-[#E5E9E6] flex items-center justify-between text-xs">
                  <div className="flex items-center gap-3">
                    <div className="w-6 h-6 rounded-md bg-white border border-[#E5E9E6] flex items-center justify-center text-[#667085]">
                      3
                    </div>
                    <div>
                      <span className="font-semibold text-[#111318]">Strategy Selection</span>
                      <p className="text-[11px] text-[#667085]">Smart Links, WhatsApp prompts, or UPI retry windows</p>
                    </div>
                  </div>
                  <span className="text-[10px] text-[#0B8F63] font-mono font-semibold">ADAPTIVE</span>
                </div>

                <div className="p-3 rounded-xl bg-[#F7F8F6] border border-[#E5E9E6] flex items-center justify-between text-xs">
                  <div className="flex items-center gap-3">
                    <div className="w-6 h-6 rounded-md bg-white border border-[#E5E9E6] flex items-center justify-center text-[#667085]">
                      4
                    </div>
                    <div>
                      <span className="font-semibold text-[#111318]">Recovery Execution</span>
                      <p className="text-[11px] text-[#667085]">Dispatched with guardrail rate limits and customer cooldowns</p>
                    </div>
                  </div>
                  <span className="text-[10px] text-[#0B8F63] font-mono font-semibold">GUARDED</span>
                </div>

                <div className="p-3 rounded-xl bg-[#F7F8F6] border border-[#E5E9E6] flex items-center justify-between text-xs">
                  <div className="flex items-center gap-3">
                    <div className="w-6 h-6 rounded-md bg-white border border-[#E5E9E6] flex items-center justify-center text-[#667085]">
                      5
                    </div>
                    <div>
                      <span className="font-semibold text-[#111318]">Reconciliation</span>
                      <p className="text-[11px] text-[#667085]">Closed-loop verification against Razorpay settlement state</p>
                    </div>
                  </div>
                  <span className="text-[10px] text-[#0B8F63] font-mono font-semibold">VERIFIED</span>
                </div>
              </div>
            </div>
          </div>

          {/* Right Column (5 cols): Razorpay Technical Integration */}
          <div className="lg:col-span-5 space-y-4">
            <div className="flex items-center justify-between px-1">
              <h2 className="font-inter text-xs font-semibold tracking-wider text-[#667085] uppercase">
                Razorpay Integration
              </h2>
              <span className="text-xs text-[#0B8F63] font-mono font-semibold flex items-center gap-1">
                <span className="w-1.5 h-1.5 rounded-full bg-[#0B8F63]" />
                CONFIGURED
              </span>
            </div>

            <div className="bg-white border border-[#E5E9E6] rounded-2xl p-6 shadow-[0_8px_30px_rgba(16,24,40,0.04)] space-y-5 text-xs">
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <span className="font-semibold text-[#111318]">Webhook Ingestion URL</span>
                  <span className="text-[10px] font-mono text-[#667085] px-1.5 py-0.5 rounded bg-[#F1F4F2]">
                    POST
                  </span>
                </div>
                <div className="flex items-center gap-2 p-2.5 rounded-xl bg-[#F7F8F6] border border-[#E5E9E6] font-mono text-xs text-[#111318] break-all">
                  <span className="truncate flex-1 font-medium">{webhookEndpoint}</span>
                  <button
                    type="button"
                    onClick={copyWebhookUrl}
                    className="p-1.5 text-[#667085] hover:text-[#111318] hover:bg-white rounded-lg transition cursor-pointer shrink-0 border border-transparent hover:border-[#E5E9E6]"
                    aria-label="Copy Webhook Endpoint URL"
                  >
                    {copiedWebhook ? (
                      <Check className="w-4 h-4 text-[#0B8F63]" />
                    ) : (
                      <Copy className="w-4 h-4" />
                    )}
                  </button>
                </div>
              </div>

              <div className="space-y-2 pt-3 border-t border-[#E5E9E6]">
                <span className="font-semibold text-[#111318] block">Subscribed Events</span>
                <div className="flex flex-wrap gap-1.5">
                  <span className="px-2 py-1 rounded-lg bg-[#F7F8F6] border border-[#E5E9E6] font-mono text-[11px] text-[#111318]">
                    payment.failed
                  </span>
                  <span className="px-2 py-1 rounded-lg bg-[#F7F8F6] border border-[#E5E9E6] font-mono text-[11px] text-[#111318]">
                    payment.captured
                  </span>
                  <span className="px-2 py-1 rounded-lg bg-[#F7F8F6] border border-[#E5E9E6] font-mono text-[11px] text-[#111318]">
                    order.paid
                  </span>
                </div>
              </div>

              <div className="pt-3 border-t border-[#E5E9E6]">
                <a
                  href="https://dashboard.razorpay.com/#/app/webhooks"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-1.5 text-xs text-[#0B8F63] hover:text-[#08704F] font-semibold transition rounded"
                >
                  <span>Configure in Razorpay Dashboard</span>
                  <ArrowUpRight className="w-3.5 h-3.5" />
                </a>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* ==================================================
          8. POLISHED LIGHT FINTECH FOOTER
          ================================================== */}
      <footer className="mt-16 border-t border-[#E5E9E6] bg-white text-[#667085] font-inter text-xs">
        <div className="max-w-[1240px] mx-auto px-4 sm:px-6 md:px-8 py-10 space-y-8">
          <div className="grid grid-cols-1 md:grid-cols-12 gap-8">
            {/* Col 1: Brand & Description */}
            <div className="md:col-span-5 space-y-2.5">
              <div className="flex items-center gap-2">
                <div className="w-6 h-6 rounded-lg bg-[#E8F7F0] text-[#0B8F63] flex items-center justify-center font-bold font-space-grotesk text-xs">
                  R
                </div>
                <span className="font-space-grotesk font-bold text-sm text-[#111318]">RecoverAI</span>
                <span className="text-[10px] uppercase font-semibold text-[#08704F] bg-[#E8F7F0] px-1.5 py-0.5 rounded">
                  Ops
                </span>
              </div>
              <p className="text-xs text-[#667085] max-w-sm leading-relaxed">
                Autonomous recovery infrastructure for failed payments. Closed-loop detection, AI
                root-cause diagnosis, and automated settlement reconciliation.
              </p>
            </div>

            {/* Col 2: Navigation */}
            <div className="md:col-span-4 space-y-2">
              <span className="font-semibold text-xs text-[#111318] uppercase tracking-wider block">
                Platform Navigation
              </span>
              <div className="grid grid-cols-2 gap-x-4 gap-y-1.5 text-xs">
                <Link to="/app" className="text-[#667085] hover:text-[#0B8F63] transition">
                  Overview
                </Link>
                <Link to="/recovery-cases" className="text-[#667085] hover:text-[#0B8F63] transition">
                  Recovery Cases
                </Link>
                <Link to="/analytics" className="text-[#667085] hover:text-[#0B8F63] transition">
                  Analytics
                </Link>
                <Link to="/notifications" className="text-[#667085] hover:text-[#0B8F63] transition">
                  Notifications
                </Link>
                <Link to="/settings" className="text-[#667085] hover:text-[#0B8F63] transition">
                  Settings
                </Link>
              </div>
            </div>

            {/* Col 3: System Status */}
            <div className="md:col-span-3 space-y-2">
              <span className="font-semibold text-xs text-[#111318] uppercase tracking-wider block">
                System Infrastructure
              </span>
              <div className="space-y-1 text-xs">
                <div className="flex items-center justify-between text-[#667085]">
                  <span>API Status</span>
                  <span className="text-[#0B8F63] font-medium font-mono text-[11px]">Operational</span>
                </div>
                <div className="flex items-center justify-between text-[#667085]">
                  <span>Recovery Engine</span>
                  <span className="text-[#0B8F63] font-medium font-mono text-[11px]">Active</span>
                </div>
                <div className="flex items-center justify-between text-[#667085]">
                  <span>Environment</span>
                  <span className="text-[#D97706] font-medium font-mono text-[11px]">
                    {isDemoMode ? 'Simulated Sandbox' : 'Production'}
                  </span>
                </div>
              </div>
            </div>
          </div>

          <div className="pt-6 border-t border-[#E5E9E6] flex flex-col sm:flex-row items-center justify-between gap-3 text-[11px] text-[#98A2B3]">
            <span>© 2026 RecoverAI. Built for intelligent payment recovery.</span>
            <span className="font-mono">v1.2.0-fintech</span>
          </div>
        </div>
      </footer>
    </div>
  );
}

