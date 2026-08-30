import { useState, useEffect, useCallback } from 'react';
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
} from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import { useToast } from '../../hooks/useToast';
import { getDashboardSummary } from '../../api/dashboard';
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
  const { user } = useAuth();
  const { toast } = useToast();

  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [copiedWebhook, setCopiedWebhook] = useState(false);

  const fetchSummary = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getDashboardSummary();
      setSummary(data);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to load dashboard metrics';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    let cancelled = false;
    async function loadInitial() {
      try {
        const data = await getDashboardSummary();
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
  }, []);

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

  return (
    <div className="space-y-8">
      {/* Page Header */}
      <PageHeader
        title={`Welcome, ${user?.name || 'Merchant'}`}
        description="Autonomous revenue recovery operations and intelligent payment failure diagnosis."
        badge={
          <Badge variant="success" dot pulse>
            {user?.status || 'ACTIVE'}
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

      {/* Live System Engine & Safety Status Banner */}
      <div className="p-4 sm:p-5 rounded-2xl bg-gradient-to-r from-indigo-950/40 via-slate-900/60 to-slate-900/40 border border-indigo-500/20 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="flex items-start sm:items-center gap-3.5">
          <div className="p-2.5 rounded-xl bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 shrink-0">
            <Cpu className="w-5 h-5" />
          </div>
          <div className="space-y-0.5">
            <div className="flex items-center gap-2">
              <h2 className="text-sm font-semibold text-white">Autonomous Agent Active</h2>
              <span className="inline-flex items-center gap-1 text-[11px] font-medium text-emerald-400">
                <Radio className="w-2.5 h-2.5 animate-pulse" />
                Live
              </span>
            </div>
            <p className="text-xs text-slate-400">
              Powered by <span className="text-indigo-300 font-mono">Google Gemini 3.7 Flash</span> • Deterministic guardrails & idempotency verified
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2.5 shrink-0 text-xs">
          {user?.razorpayAccountId ? (
            <span className="px-2.5 py-1 rounded-lg bg-slate-900 border border-slate-800 text-slate-300 font-mono">
              Account: {user.razorpayAccountId}
            </span>
          ) : (
            <span className="px-2.5 py-1 rounded-lg bg-amber-500/10 border border-amber-500/20 text-amber-300">
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
          {/* Card 1: Total Recovery Cases */}
          <Card className="hover:border-slate-700 transition">
            <div className="flex items-center justify-between text-slate-400 pb-2">
              <span className="text-xs font-semibold uppercase tracking-wider">Total Cases</span>
              <ShieldAlert className="w-4 h-4 text-indigo-400" />
            </div>
            <div className="space-y-1">
              <div className="text-2xl font-bold text-white font-mono">
                {summary?.totalRecoveryCases ?? 0}
              </div>
              <p className="text-[11px] text-slate-400">Failed payments ingested</p>
            </div>
          </Card>

          {/* Card 2: Open / Pending Cases */}
          <Card className="hover:border-slate-700 transition">
            <div className="flex items-center justify-between text-slate-400 pb-2">
              <span className="text-xs font-semibold uppercase tracking-wider">In Flight / Open</span>
              <Clock className="w-4 h-4 text-amber-400" />
            </div>
            <div className="space-y-1">
              <div className="text-2xl font-bold text-white font-mono">
                {(summary?.openCases ?? 0) + (summary?.inProgressCases ?? 0)}
              </div>
              <p className="text-[11px] text-slate-400">
                {summary?.inProgressCases ?? 0} in active execution
              </p>
            </div>
          </Card>

          {/* Card 3: Recovered Revenue */}
          <Card className="hover:border-slate-700 transition">
            <div className="flex items-center justify-between text-slate-400 pb-2">
              <span className="text-xs font-semibold uppercase tracking-wider">Recovered Revenue</span>
              <CheckCircle2 className="w-4 h-4 text-emerald-400" />
            </div>
            <div className="space-y-1">
              <div className="text-2xl font-bold text-emerald-400 font-mono">
                {formatCurrency(summary?.totalRecoveredAmount)}
              </div>
              <p className="text-[11px] text-slate-400">
                Estimated: {formatCurrency(summary?.totalEstimatedRecoverableAmount)}
              </p>
            </div>
          </Card>

          {/* Card 4: Recovery Success Rate */}
          <Card className="hover:border-slate-700 transition">
            <div className="flex items-center justify-between text-slate-400 pb-2">
              <span className="text-xs font-semibold uppercase tracking-wider">Recovery Rate</span>
              <TrendingUp className="w-4 h-4 text-indigo-400" />
            </div>
            <div className="space-y-1">
              <div className="text-2xl font-bold text-white font-mono">
                {summary?.recoveryRate ? `${summary.recoveryRate.toFixed(1)}%` : '0.0%'}
              </div>
              <p className="text-[11px] text-slate-400">Closed-loop reconciliation</p>
            </div>
          </Card>
        </div>
      )}

      {/* Main Content Section: Activity / Empty State */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left 2 Cols: Activity or Intentional Empty State */}
        <div className="lg:col-span-2 space-y-6">
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <div>
                  <CardTitle>Autonomous Recovery Activity</CardTitle>
                  <CardDescription>
                    Real-time transaction diagnosis and recovery attempt tracking
                  </CardDescription>
                </div>
                <Badge variant="outline">Live Stream</Badge>
              </div>
            </CardHeader>

            <CardContent>
              {!loading && (!summary || summary.totalRecoveryCases === 0) ? (
                <EmptyState
                  icon={<Zap className="w-8 h-8 text-indigo-400" />}
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
                            <Check className="w-3.5 h-3.5 text-emerald-400" />
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
                <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-800 text-xs text-slate-400">
                  <p>
                    {summary?.totalRecoveryCases ?? 0} total cases detected. Complete interactive case management and drilldown will be available in PR #21.
                  </p>
                </div>
              )}
            </CardContent>
          </Card>
        </div>

        {/* Right 1 Col: Quick Integration Guide Card */}
        <div className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>Razorpay Integration</CardTitle>
              <CardDescription>
                Direct failed payments into RecoverAI's autonomous queue
              </CardDescription>
            </CardHeader>

            <CardContent className="space-y-4 text-xs">
              <div>
                <span className="font-semibold text-slate-300 block mb-1.5">
                  Webhook Ingestion URL:
                </span>
                <div className="flex items-center gap-2 p-2 rounded-lg bg-slate-950 border border-slate-800 font-mono text-[11px] text-indigo-300 break-all">
                  <span className="truncate flex-1">{webhookEndpoint}</span>
                  <button
                    onClick={copyWebhookUrl}
                    className="p-1 text-slate-400 hover:text-white rounded transition cursor-pointer shrink-0"
                    aria-label="Copy Webhook Endpoint URL"
                  >
                    {copiedWebhook ? (
                      <Check className="w-3.5 h-3.5 text-emerald-400" />
                    ) : (
                      <Copy className="w-3.5 h-3.5" />
                    )}
                  </button>
                </div>
              </div>

              <div className="space-y-2 pt-2 border-t border-slate-800/80">
                <span className="font-semibold text-slate-300 block">Subscribed Events:</span>
                <ul className="space-y-1.5 text-slate-400 list-disc list-inside">
                  <li><code className="text-slate-300 font-mono">payment.failed</code></li>
                  <li><code className="text-slate-300 font-mono">payment.captured</code></li>
                  <li><code className="text-slate-300 font-mono">order.paid</code></li>
                </ul>
              </div>

              <div className="pt-2 border-t border-slate-800/80">
                <a
                  href="https://dashboard.razorpay.com/#/app/webhooks"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-1.5 text-xs text-indigo-400 hover:text-indigo-300 hover:underline font-medium"
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
