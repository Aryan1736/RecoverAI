import { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import {
  ShieldAlert,
  Zap,
  TrendingUp,
  RefreshCw,
  Cpu,
  CheckCircle2,
  Clock,
  Radio,
  ExternalLink,
  Copy,
  Check,
  ArrowRight,
  BarChart3,
  Sparkles,
} from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import { useToast } from '../../hooks/useToast';
import { getDashboardSummary } from '../../api/dashboard';
import { getDemoDashboard } from '../../api/demo';
import type { DashboardSummary } from '../../types/dashboard';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '../../components/ui/Card';
import { Badge } from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { SkeletonCard } from '../../components/ui/Skeleton';
import { EmptyState } from '../../components/ui/EmptyState';
import { ErrorState } from '../../components/ui/ErrorState';
import { PageHeader } from '../../components/ui/PageHeader';
import { API_BASE_URL } from '../../api/client';

export function OverviewPage() {
  const { user, isDemoMode } = useAuth();
  const { toast } = useToast();

  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [copiedWebhook, setCopiedWebhook] = useState(false);

  const fetchSummary = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = isDemoMode ? await getDemoDashboard() : await getDashboardSummary();
      setSummary(data);
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
        const data = isDemoMode ? await getDemoDashboard() : await getDashboardSummary();
        if (!cancelled) {
          setSummary(data);
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
      maximumFractionDigits: 2,
    }).format(amount ?? 0);
  };

  const totalCases = summary?.totalRecoveryCases ?? 0;
  const recoveredCases = summary?.recoveredCases ?? 0;
  const activeCases = (summary?.openCases ?? 0) + (summary?.inProgressCases ?? 0);
  const terminalCases = (summary?.expiredCases ?? 0) + (summary?.cancelledCases ?? 0) + (summary?.failedCases ?? 0);

  const recoveredPct = totalCases > 0 ? Math.round((recoveredCases / totalCases) * 100) : 0;
  const activePct = totalCases > 0 ? Math.round((activeCases / totalCases) * 100) : 0;
  const terminalPct = totalCases > 0 ? Math.max(0, 100 - recoveredPct - activePct) : 0;

  return (
    <div className="space-y-8">
      {/* Page Header */}
      <PageHeader
        title={isDemoMode ? 'RecoverAI Overview • Welcome, Demo Evaluator' : `RecoverAI Overview • Welcome, ${user?.name || 'Merchant'}`}
        description={
          isDemoMode
            ? 'Interactive sandbox environment with preloaded simulated payment recovery cases and analytics.'
            : 'Autonomous revenue recovery operations and intelligent payment failure diagnosis.'
        }
        badge={
          <Badge variant={isDemoMode ? 'warning' : 'success'} dot pulse>
            {isDemoMode ? 'DEMO ENVIRONMENT' : user?.status || 'ACTIVE'}
          </Badge>
        }
        actions={
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={fetchSummary}
              isLoading={loading}
              leftIcon={<RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />}
            >
              Refresh
            </Button>
          </div>
        }
      />

      {/* Demo Mode Notice Banner */}
      {isDemoMode && (
        <div
          role="status"
          aria-label="Interactive Demo Environment active"
          className="p-4 rounded-xl bg-amber-50 border border-amber-200 text-amber-900 text-xs flex flex-col sm:flex-row sm:items-center justify-between gap-3 shadow-2xs animate-in fade-in"
        >
          <div className="flex items-center gap-2.5">
            <Sparkles className="w-4 h-4 text-amber-600 shrink-0" />
            <span>
              <strong>Demo Environment Active:</strong> You are exploring RecoverAI with preloaded simulated metrics, cases, and analytics. Real production APIs and backend database mutations are disabled.
            </span>
          </div>
          <Badge variant="warning" className="shrink-0 self-start sm:self-auto">
            Simulated Sandbox
          </Badge>
        </div>
      )}

      {/* Live System Engine & Safety Status Banner */}
      <div className="p-4 sm:p-5 rounded-2xl bg-white border border-slate-200 shadow-2xs flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="flex items-start sm:items-center gap-3.5">
          <div className="p-2.5 rounded-xl bg-emerald-50 text-emerald-600 border border-emerald-200 shrink-0">
            <Cpu className="w-5 h-5" />
          </div>
          <div className="space-y-0.5">
            <div className="flex items-center gap-2">
              <h2 className="text-sm font-semibold text-slate-900">Autonomous Agent Active</h2>
              <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-emerald-600">
                <Radio className="w-2.5 h-2.5 animate-pulse" />
                Live
              </span>
            </div>
            <p className="text-xs text-slate-500">
              Powered by <span className="text-slate-700 font-semibold font-mono">Google Gemini 3.7 Flash</span> • Deterministic guardrails & idempotency verified
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2.5 shrink-0 text-xs">
          {user?.razorpayAccountId ? (
            <span className="px-2.5 py-1 rounded-lg bg-slate-50 border border-slate-200 text-slate-700 font-mono">
              Account: {user.razorpayAccountId}
            </span>
          ) : (
            <span className="px-2.5 py-1 rounded-lg bg-amber-50 border border-amber-200 text-amber-800">
              Razorpay Account unlinked
            </span>
          )}
        </div>
      </div>

      {/* KPI Metrics Area */}
      {loading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <SkeletonCard />
          <SkeletonCard />
          <SkeletonCard />
          <SkeletonCard />
        </div>
      ) : error ? (
        <ErrorState
          title="Failed to Load Overview Metrics"
          message={error}
          onRetry={fetchSummary}
        />
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {/* Card 1: Recovered Revenue */}
          <Card className="hover:border-slate-300 transition-colors shadow-2xs">
            <div className="flex items-center justify-between text-slate-500 pb-2">
              <span className="text-xs font-semibold uppercase tracking-wider">Recovered Revenue</span>
              <div className="p-1.5 rounded-lg bg-emerald-50 text-emerald-600">
                <CheckCircle2 className="w-4 h-4" />
              </div>
            </div>
            <div className="space-y-1">
              <div className="text-2xl font-bold text-emerald-700 font-mono">
                {formatCurrency(summary?.totalRecoveredAmount)}
              </div>
              <p className="text-[11px] text-slate-500">
                {recoveredCases} successful recovery transactions
              </p>
            </div>
          </Card>

          {/* Card 2: Recovery Rate */}
          <Card className="hover:border-slate-300 transition-colors shadow-2xs">
            <div className="flex items-center justify-between text-slate-500 pb-2">
              <span className="text-xs font-semibold uppercase tracking-wider">Recovery Rate</span>
              <div className="p-1.5 rounded-lg bg-emerald-50 text-emerald-600">
                <TrendingUp className="w-4 h-4" />
              </div>
            </div>
            <div className="space-y-1">
              <div className="text-2xl font-bold text-slate-900 font-mono">
                {summary?.recoveryRate ? `${summary.recoveryRate.toFixed(1)}%` : '0.0%'}
              </div>
              <p className="text-[11px] text-slate-500">Closed-loop reconciliation</p>
            </div>
          </Card>

          {/* Card 3: Active Recovery Cases */}
          <Card className="hover:border-slate-300 transition-colors shadow-2xs">
            <div className="flex items-center justify-between text-slate-500 pb-2">
              <span className="text-xs font-semibold uppercase tracking-wider">Active Recovery Cases</span>
              <div className="p-1.5 rounded-lg bg-blue-50 text-blue-600">
                <Clock className="w-4 h-4" />
              </div>
            </div>
            <div className="space-y-1">
              <div className="text-2xl font-bold text-slate-900 font-mono">
                {activeCases}
              </div>
              <p className="text-[11px] text-slate-500">
                {summary?.inProgressCases ?? 0} in active execution
              </p>
            </div>
          </Card>

          {/* Card 4: At-Risk Revenue */}
          <Card className="hover:border-slate-300 transition-colors shadow-2xs">
            <div className="flex items-center justify-between text-slate-500 pb-2">
              <span className="text-xs font-semibold uppercase tracking-wider">At-Risk Revenue</span>
              <div className="p-1.5 rounded-lg bg-amber-50 text-amber-600">
                <ShieldAlert className="w-4 h-4" />
              </div>
            </div>
            <div className="space-y-1">
              <div className="text-2xl font-bold text-slate-900 font-mono">
                {formatCurrency(summary?.totalEstimatedRecoverableAmount)}
              </div>
              <p className="text-[11px] text-slate-500">
                From {totalCases} ingested payment failures
              </p>
            </div>
          </Card>
        </div>
      )}

      {/* Main Content Section: Activity / Performance */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left 2 Cols: Activity or Performance Breakdown */}
        <div className="lg:col-span-2 space-y-6">
          <Card className="shadow-2xs">
            <CardHeader>
              <div className="flex items-center justify-between">
                <div>
                  <CardTitle>Autonomous Recovery Performance</CardTitle>
                  <CardDescription>
                    Real-time transaction diagnosis, queue health, and recovery outcomes
                  </CardDescription>
                </div>
                <Badge variant="outline">Live Stream</Badge>
              </div>
            </CardHeader>

            <CardContent>
              {!loading && (!summary || summary.totalRecoveryCases === 0) ? (
                <EmptyState
                  icon={<Zap className="w-8 h-8 text-emerald-600" />}
                  title="No Recovery Cases Recorded Yet"
                  description="Your RecoverAI agent is active and standing by. Ingest payment failure webhooks from Razorpay or trigger a test event to see autonomous recovery in real-time."
                  action={
                    <div className="flex flex-col sm:flex-row items-center gap-2">
                      <Button
                        size="sm"
                        variant="secondary"
                        onClick={copyWebhookUrl}
                        leftIcon={
                          copiedWebhook ? (
                            <Check className="w-3.5 h-3.5 text-emerald-600" />
                          ) : (
                            <Copy className="w-3.5 h-3.5" />
                          )
                        }
                      >
                        {copiedWebhook ? 'Copied URL' : 'Copy Webhook URL'}
                      </Button>
                    </div>
                  }
                />
              ) : (
                <div className="space-y-5">
                  {/* Status distribution bar */}
                  <div className="space-y-2">
                    <div className="flex items-center justify-between text-xs text-slate-600">
                      <span>Case Portfolio Distribution</span>
                      <span className="font-semibold text-slate-900"><span className="font-mono">{totalCases}</span> Total Cases</span>
                    </div>
                    <div className="h-3 w-full rounded-full bg-slate-100 flex overflow-hidden">
                      <div
                        style={{ width: `${recoveredPct}%` }}
                        className="bg-emerald-500 transition-all duration-300"
                        title={`Recovered: ${recoveredCases} (${recoveredPct}%)`}
                      />
                      <div
                        style={{ width: `${activePct}%` }}
                        className="bg-blue-500 transition-all duration-300"
                        title={`Active: ${activeCases} (${activePct}%)`}
                      />
                      <div
                        style={{ width: `${terminalPct}%` }}
                        className="bg-slate-300 transition-all duration-300"
                        title={`Closed / Exhausted: ${terminalCases} (${terminalPct}%)`}
                      />
                    </div>
                    <div className="flex flex-wrap items-center gap-4 text-xs text-slate-600 pt-1">
                      <span className="flex items-center gap-1.5">
                        <span className="w-2.5 h-2.5 rounded-full bg-emerald-500" />
                        Recovered: {recoveredCases} ({recoveredPct}%)
                      </span>
                      <span className="flex items-center gap-1.5">
                        <span className="w-2.5 h-2.5 rounded-full bg-blue-500" />
                        In Flight: {activeCases} ({activePct}%)
                      </span>
                      <span className="flex items-center gap-1.5">
                        <span className="w-2.5 h-2.5 rounded-full bg-slate-300" />
                        Closed/Terminal: {terminalCases} ({terminalPct}%)
                      </span>
                    </div>
                  </div>

                  {/* Summary Callout Banner */}
                  <div className="p-4 rounded-xl bg-slate-50 border border-slate-200 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs">
                    <div className="space-y-0.5">
                      <p className="font-semibold text-slate-900">
                        {activeCases > 0
                          ? `${activeCases} cases actively progressing through AI recovery workflows`
                          : 'All cases currently reconciled and processed'}
                      </p>
                      <p className="text-slate-500">
                        Total recovered value: <span className="font-mono font-semibold text-emerald-700">{formatCurrency(summary?.totalRecoveredAmount)}</span>
                      </p>
                    </div>
                    <div className="flex items-center gap-2 shrink-0">
                      <Link to="/recovery-cases">
                        <Button size="sm" variant="primary" rightIcon={<ArrowRight className="w-3.5 h-3.5" />}>
                          View Cases
                        </Button>
                      </Link>
                      <Link to="/analytics">
                        <Button size="sm" variant="secondary" leftIcon={<BarChart3 className="w-3.5 h-3.5" />}>
                          Analytics
                        </Button>
                      </Link>
                    </div>
                  </div>
                </div>
              )}
            </CardContent>
          </Card>
        </div>

        {/* Right 1 Col: Quick Integration Guide Card */}
        <div className="space-y-6">
          <Card className="shadow-2xs">
            <CardHeader>
              <CardTitle>Razorpay Integration</CardTitle>
              <CardDescription>
                Direct failed payments into RecoverAI's autonomous queue
              </CardDescription>
            </CardHeader>

            <CardContent className="space-y-4 text-xs">
              <div>
                <span className="font-semibold text-slate-700 block mb-1.5">
                  Webhook Ingestion URL:
                </span>
                <div className="flex items-center gap-2 p-2 rounded-lg bg-slate-50 border border-slate-200 font-mono text-[11px] text-slate-800 break-all">
                  <span className="truncate flex-1">{webhookEndpoint}</span>
                  <button
                    onClick={copyWebhookUrl}
                    className="p-1 text-slate-400 hover:text-slate-700 rounded transition cursor-pointer shrink-0"
                    aria-label="Copy Webhook Endpoint URL"
                  >
                    {copiedWebhook ? (
                      <Check className="w-3.5 h-3.5 text-emerald-600" />
                    ) : (
                      <Copy className="w-3.5 h-3.5" />
                    )}
                  </button>
                </div>
              </div>

              <div className="space-y-2 pt-2 border-t border-slate-100">
                <span className="font-semibold text-slate-700 block">Subscribed Events:</span>
                <ul className="space-y-1.5 text-slate-500 list-disc list-inside">
                  <li><code className="text-slate-800 font-mono">payment.failed</code></li>
                  <li><code className="text-slate-800 font-mono">payment.captured</code></li>
                  <li><code className="text-slate-800 font-mono">order.paid</code></li>
                </ul>
              </div>

              <div className="pt-2 border-t border-slate-100">
                <a
                  href="https://dashboard.razorpay.com/#/app/webhooks"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-1.5 text-xs text-emerald-600 hover:text-emerald-700 hover:underline font-semibold"
                >
                  Configure in Razorpay Dashboard
                  <ExternalLink className="w-3 h-3" />
                </a>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
