import { useState, useEffect } from 'react';
import {
  Activity,
  RefreshCw,
  CheckCircle2,
  AlertTriangle,
  AlertOctagon,
  HelpCircle,
  MessageSquare,
  Mail,
  Smartphone,
  CreditCard,
  ShieldCheck,
  Radio,
  Sparkles,
  Lock,
  Cpu,
} from 'lucide-react';
import { Card } from '../ui/Card';
import { Badge, type BadgeVariant } from '../ui/Badge';
import { Button } from '../ui/Button';
import { Skeleton } from '../ui/Skeleton';
import { ErrorState } from '../ui/ErrorState';
import { getProviderHealth } from '../../api/providers';
import { getDemoProviderHealth } from '../../api/demo';
import { useDemoMode } from '../../hooks/useDemoMode';
import type {
  ProviderHealthSummary,
  ProviderStatusItem,
  ProviderHealthState,
} from '../../types/providers';

function getStatusBadgeConfig(status: ProviderHealthState): {
  label: string;
  variant: BadgeVariant;
  pulse: boolean;
} {
  switch (status) {
    case 'HEALTHY':
      return { label: 'Operational', variant: 'success', pulse: false };
    case 'DEGRADED':
      return { label: 'Degraded', variant: 'warning', pulse: true };
    case 'UNAVAILABLE':
      return { label: 'Unavailable', variant: 'danger', pulse: true };
    case 'DISABLED':
      return { label: 'Disabled', variant: 'default', pulse: false };
    case 'UNKNOWN':
    default:
      return { label: 'Unknown', variant: 'outline', pulse: false };
  }
}

function getChannelIcon(channel: string) {
  const c = channel.toLowerCase();
  if (c.includes('whatsapp')) return MessageSquare;
  if (c.includes('email')) return Mail;
  if (c.includes('sms')) return Smartphone;
  if (c.includes('payment') || c.includes('razorpay')) return CreditCard;
  return Radio;
}

function formatCheckedTime(isoString?: string | null): string {
  if (!isoString) return '—';
  try {
    const d = new Date(isoString);
    if (isNaN(d.getTime())) return isoString;
    return d.toLocaleTimeString('en-US', { hour12: false });
  } catch {
    return isoString;
  }
}

export function ProviderHealthCard() {
  const { isDemoMode } = useDemoMode();
  const [summary, setSummary] = useState<ProviderHealthSummary | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isRefreshing, setIsRefreshing] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const fetchHealth = async (isManualRefresh = false) => {
    if (isManualRefresh) {
      setIsRefreshing(true);
    } else {
      setIsLoading(true);
    }
    setError(null);

    try {
      const data = isDemoMode ? await getDemoProviderHealth() : await getProviderHealth();
      setSummary(data);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to retrieve provider health telemetry';
      setError(msg);
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  };

  useEffect(() => {
    let cancelled = false;

    async function loadData() {
      try {
        const data = isDemoMode ? await getDemoProviderHealth() : await getProviderHealth();
        if (!cancelled) {
          setSummary(data);
          setError(null);
        }
      } catch (err: unknown) {
        if (!cancelled) {
          const msg = err instanceof Error ? err.message : 'Failed to retrieve provider health telemetry';
          setError(msg);
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    }

    loadData();
    return () => {
      cancelled = true;
    };
  }, [isDemoMode]);

  if (isLoading) {
    return (
      <div className="space-y-6 font-inter" role="status" aria-label="Loading provider status">
        {/* Hero skeleton */}
        <div className="bg-white border border-[#E5E9E6] rounded-xl p-6 shadow-2xs space-y-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="flex items-center gap-3.5">
              <Skeleton className="w-12 h-12 rounded-xl" />
              <div className="space-y-2">
                <Skeleton className="h-5 w-64 rounded-md" />
                <Skeleton className="h-3.5 w-80 rounded-md" />
              </div>
            </div>
            <div className="flex items-center gap-3">
              <Skeleton className="h-8 w-28 rounded-lg" />
              <Skeleton className="h-8 w-24 rounded-lg" />
            </div>
          </div>
        </div>

        {/* Summary strip skeleton */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 bg-white border border-[#E5E9E6] rounded-xl p-4 shadow-2xs">
          <Skeleton className="h-14 rounded-lg" />
          <Skeleton className="h-14 rounded-lg" />
          <Skeleton className="h-14 rounded-lg" />
          <Skeleton className="h-14 rounded-lg" />
        </div>

        {/* Section title skeleton */}
        <div className="space-y-2 pt-2">
          <Skeleton className="h-4 w-48 rounded" />
          <Skeleton className="h-3 w-72 rounded" />
        </div>

        {/* Provider cards grid skeleton */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <Skeleton className="h-44 rounded-xl" />
          <Skeleton className="h-44 rounded-xl" />
          <Skeleton className="h-44 rounded-xl" />
          <Skeleton className="h-44 rounded-xl" />
        </div>

        {/* Security banner skeleton */}
        <Skeleton className="h-24 rounded-xl" />
      </div>
    );
  }

  if (error) {
    return (
      <ErrorState
        title="Failed to Load Provider Status"
        message={error}
        onRetry={() => fetchHealth(false)}
      />
    );
  }

  const overall = summary?.overallStatus || 'UNKNOWN';
  const overallConfig = getStatusBadgeConfig(overall);
  const providers = summary?.providers || [];

  const totalProviders = providers.length;
  const healthyCount = providers.filter((p) => p.status === 'HEALTHY').length;
  const degradedCount = providers.filter((p) => p.status === 'DEGRADED').length;
  const unavailableCount = providers.filter(
    (p) => p.status === 'UNAVAILABLE' || p.status === 'DISABLED'
  ).length;

  return (
    <div className="space-y-6 font-inter">
      {/* ==================================================
          1. SYSTEM HEALTH HERO (OPERATIONAL STATUS PANEL)
          ================================================== */}
      <div
        className={`bg-white border rounded-xl p-5 sm:p-6 shadow-2xs transition-all ${
          overall === 'HEALTHY'
            ? 'border-[#E5E9E6]'
            : overall === 'DEGRADED'
            ? 'border-[#D97706]/40 bg-[#FFFDF5]'
            : overall === 'UNAVAILABLE'
            ? 'border-[#DC2626]/40 bg-[#FFF5F5]'
            : 'border-[#E5E9E6]'
        }`}
      >
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-5">
          {/* Status icon + Title + Subtitle */}
          <div className="flex items-start sm:items-center gap-4">
            <div
              className={`w-12 h-12 rounded-xl flex items-center justify-center shrink-0 border shadow-2xs ${
                overall === 'HEALTHY'
                  ? 'bg-[#E8F7F0] border-[#0B8F63]/30 text-[#08704F]'
                  : overall === 'DEGRADED'
                  ? 'bg-[#FEF3C7] border-[#D97706]/30 text-[#D97706]'
                  : overall === 'UNAVAILABLE'
                  ? 'bg-[#FEE2E2] border-[#DC2626]/30 text-[#DC2626]'
                  : 'bg-[#F1F4F2] border-[#E5E9E6] text-[#667085]'
              }`}
            >
              {overall === 'HEALTHY' ? (
                <CheckCircle2 className="w-6 h-6" />
              ) : overall === 'DEGRADED' ? (
                <AlertTriangle className="w-6 h-6" />
              ) : overall === 'UNAVAILABLE' ? (
                <AlertOctagon className="w-6 h-6" />
              ) : (
                <HelpCircle className="w-6 h-6" />
              )}
            </div>

            <div className="space-y-1">
              <div className="flex flex-wrap items-center gap-2.5">
                <h2 className="font-space-grotesk text-lg sm:text-xl font-bold text-[#111318] tracking-tight">
                  {overall === 'HEALTHY'
                    ? 'All Recovery & Communication Providers Operational'
                    : overall === 'DEGRADED'
                    ? 'Upstream Provider Degradation Observed'
                    : overall === 'UNAVAILABLE'
                    ? 'Critical Provider Connectivity Interrupted'
                    : 'Provider Telemetry Status'}
                </h2>
                <Badge
                  variant={overallConfig.variant}
                  dot
                  pulse={overallConfig.pulse}
                  aria-label={`Overall provider status: ${overallConfig.label}`}
                >
                  {overallConfig.label}
                </Badge>
              </div>
              <p className="text-xs text-[#667085] max-w-xl">
                {overall === 'HEALTHY'
                  ? 'RecoverAI automated multi-channel dispatch engine is operational and nominal across all gateways.'
                  : overall === 'DEGRADED'
                  ? 'Elevated latency or partial throttling reported by upstream providers. Fallback retry queues engaged.'
                  : overall === 'UNAVAILABLE'
                  ? 'Critical dispatch channel offline. Recovery operations temporarily halted for affected routes.'
                  : 'Live automated health checks from RecoverAI multi-channel dispatch engine.'}
              </p>
            </div>
          </div>

          {/* Controls: Environment Badge, Last Checked & Refresh Button */}
          <div className="flex flex-wrap sm:flex-nowrap items-center gap-3 pt-3 lg:pt-0 border-t lg:border-t-0 border-[#ECEFEA]">
            {/* Environment Badge */}
            {isDemoMode ? (
              <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-[11px] font-semibold bg-[#FEF3C7] text-[#D97706] border border-[#D97706]/30 font-mono shrink-0">
                <Sparkles className="w-3.5 h-3.5 text-[#D97706]" />
                <span>SIMULATED SANDBOX</span>
              </span>
            ) : (
              <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-[11px] font-semibold bg-[#E8F7F0] text-[#08704F] border border-[#0B8F63]/30 font-mono shrink-0">
                <span className="w-1.5 h-1.5 rounded-full bg-[#0B8F63]" />
                <span>PRODUCTION</span>
              </span>
            )}

            {/* Last Checked indicator */}
            {summary?.lastChecked && (
              <div className="hidden sm:flex flex-col text-right">
                <span className="text-[10px] uppercase font-bold tracking-[0.06em] text-[#98A2B3]">
                  Last Refreshed
                </span>
                <span className="text-[11px] text-[#667085] font-mono tabular-nums">
                  Checked: {formatCheckedTime(summary.lastChecked)}
                </span>
              </div>
            )}

            {/* Manual Refresh Button */}
            <Button
              size="sm"
              variant="outline"
              onClick={() => fetchHealth(true)}
              isLoading={isRefreshing}
              disabled={isRefreshing}
              leftIcon={<RefreshCw className={`w-3.5 h-3.5 ${isRefreshing ? 'animate-spin' : ''}`} />}
              className="border-[#E5E9E6] text-xs font-semibold text-[#111318] hover:bg-[#F1F4F2] shadow-2xs shrink-0"
              aria-label="Check Status"
            >
              Check Status
            </Button>
          </div>
        </div>
      </div>

      {/* ==================================================
          2. OPERATIONAL SUMMARY STRIP
          ================================================== */}
      <div className="bg-white border border-[#E5E9E6] rounded-xl shadow-2xs overflow-hidden">
        <div className="grid grid-cols-2 md:grid-cols-4 divide-y md:divide-y-0 md:divide-x divide-[#ECEFEA]">
          {/* Total Providers */}
          <div className="p-4 sm:p-5 space-y-1">
            <div className="flex items-center justify-between">
              <span className="text-[10px] font-bold uppercase tracking-[0.08em] text-[#667085]">
                Configured Providers
              </span>
              <Cpu className="w-3.5 h-3.5 text-[#98A2B3]" />
            </div>
            <div className="font-space-grotesk font-bold text-2xl text-[#111318] tabular-nums tracking-tight">
              {totalProviders}
            </div>
            <p className="text-[11px] text-[#667085]">
              Active dispatch routes
            </p>
          </div>

          {/* Operational Count */}
          <div className="p-4 sm:p-5 space-y-1">
            <div className="flex items-center justify-between">
              <span className="text-[10px] font-bold uppercase tracking-[0.08em] text-[#08704F]">
                Operational
              </span>
              <span className="w-2 h-2 rounded-full bg-[#0B8F63]" />
            </div>
            <div className="font-space-grotesk font-bold text-2xl text-[#08704F] tabular-nums tracking-tight">
              {healthyCount}
            </div>
            <p className="text-[11px] text-[#667085]">
              Nominal status verified
            </p>
          </div>

          {/* Degraded Count */}
          <div className="p-4 sm:p-5 space-y-1">
            <div className="flex items-center justify-between">
              <span className="text-[10px] font-bold uppercase tracking-[0.08em] text-[#D97706]">
                Degraded
              </span>
              <span className={`w-2 h-2 rounded-full ${degradedCount > 0 ? 'bg-[#D97706] pulse-subtle' : 'bg-[#E5E9E6]'}`} />
            </div>
            <div className={`font-space-grotesk font-bold text-2xl tabular-nums tracking-tight ${degradedCount > 0 ? 'text-[#D97706]' : 'text-[#111318]'}`}>
              {degradedCount}
            </div>
            <p className="text-[11px] text-[#667085]">
              {degradedCount > 0 ? 'Queue latency warning' : 'Zero degraded channels'}
            </p>
          </div>

          {/* Unavailable Count */}
          <div className="p-4 sm:p-5 space-y-1">
            <div className="flex items-center justify-between">
              <span className="text-[10px] font-bold uppercase tracking-[0.08em] text-[#DC2626]">
                Unavailable
              </span>
              <span className={`w-2 h-2 rounded-full ${unavailableCount > 0 ? 'bg-[#DC2626] pulse-subtle' : 'bg-[#E5E9E6]'}`} />
            </div>
            <div className={`font-space-grotesk font-bold text-2xl tabular-nums tracking-tight ${unavailableCount > 0 ? 'text-[#DC2626]' : 'text-[#111318]'}`}>
              {unavailableCount}
            </div>
            <p className="text-[11px] text-[#667085]">
              {unavailableCount > 0 ? 'Requires immediate action' : 'Zero offline channels'}
            </p>
          </div>
        </div>
      </div>

      {/* ==================================================
          3. SECTION HEADER
          ================================================== */}
      <div className="space-y-1 pt-1">
        <div className="flex items-center gap-2">
          <span className="w-1.5 h-1.5 rounded-full bg-[#0B8F63]" />
          <span className="text-[11px] font-bold uppercase tracking-[0.08em] text-[#08704F]">
            CHANNEL &amp; GATEWAY TELEMETRY
          </span>
        </div>
        <h3 className="text-base sm:text-lg font-bold font-space-grotesk text-[#111318]">
          Upstream Provider Operational Status
        </h3>
        <p className="text-xs text-[#667085]">
          Telemetry and availability metrics across communications providers (WhatsApp, Email, SMS) and payment recovery gateways.
        </p>
      </div>

      {/* ==================================================
          4. PROVIDER HEALTH CARDS GRID
          ================================================== */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        {providers.length === 0 ? (
          <div className="col-span-full p-8 text-center rounded-xl bg-white border border-[#E5E9E6] text-[#667085] text-xs shadow-2xs space-y-2">
            <p className="font-semibold text-sm text-[#111318]">
              No provider health checks configured.
            </p>
            <p>
              No upstream messaging or payment gateways are registered for this environment.
            </p>
          </div>
        ) : (
          providers.map((item: ProviderStatusItem) => {
            const ChannelIcon = getChannelIcon(item.channel);
            const badgeConfig = getStatusBadgeConfig(item.status);
            const isPayment =
              item.channel.toLowerCase().includes('payment') ||
              item.name.toLowerCase().includes('razorpay');

            return (
              <Card
                key={item.id}
                className={`border bg-white transition-all duration-150 p-5 space-y-3.5 shadow-2xs ${
                  isPayment
                    ? 'border-[#0B8F63]/30 hover:border-[#0B8F63]/50'
                    : 'border-[#E5E9E6] hover:border-[#CCD4CE]'
                }`}
              >
                {/* Header: Icon, Name, Category, Badges */}
                <div className="flex items-start justify-between gap-3">
                  <div className="flex items-center gap-3 min-w-0">
                    <div
                      className={`w-10 h-10 rounded-xl flex items-center justify-center shrink-0 border shadow-2xs ${
                        isPayment
                          ? 'bg-[#E8F7F0] border-[#0B8F63]/30 text-[#08704F]'
                          : 'bg-[#F7F8F6] border-[#E5E9E6] text-[#667085]'
                      }`}
                    >
                      <ChannelIcon className={`w-5 h-5 ${isPayment ? 'text-[#0B8F63]' : 'text-[#667085]'}`} />
                    </div>
                    <div className="min-w-0">
                      <div className="flex items-center gap-2">
                        <h4 className="text-sm font-bold font-space-grotesk text-[#111318] truncate">
                          {item.name}
                        </h4>
                        {isPayment && (
                          <span className="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider bg-[#E8F7F0] text-[#08704F] border border-[#0B8F63]/30 shrink-0">
                            Primary Gateway
                          </span>
                        )}
                      </div>
                      <span className="text-xs text-[#667085] font-medium block">
                        {item.channel}
                      </span>
                    </div>
                  </div>

                  <Badge
                    variant={badgeConfig.variant}
                    dot
                    pulse={badgeConfig.pulse}
                    aria-label={`${item.name} provider status: ${badgeConfig.label}`}
                    className="shrink-0"
                  >
                    {badgeConfig.label}
                  </Badge>
                </div>

                {/* Diagnostic message telemetry box */}
                {item.message ? (
                  <div className="p-3 rounded-lg bg-[#F7F8F6] border border-[#E5E9E6] text-xs font-mono text-[#111318] break-words flex items-start gap-2.5">
                    <span
                      className={`w-1.5 h-1.5 rounded-full mt-1.5 shrink-0 ${
                        item.status === 'HEALTHY'
                          ? 'bg-[#0B8F63]'
                          : item.status === 'DEGRADED'
                          ? 'bg-[#D97706]'
                          : item.status === 'UNAVAILABLE'
                          ? 'bg-[#DC2626]'
                          : 'bg-[#98A2B3]'
                      }`}
                    />
                    <span className="leading-relaxed">{item.message}</span>
                  </div>
                ) : (
                  <div className="p-3 rounded-lg bg-[#F7F8F6] border border-[#E5E9E6] text-xs font-mono text-[#667085] italic">
                    Diagnostic check returned nominal response without warnings.
                  </div>
                )}

                {/* Card Footer: Channel ID, Monitored badge */}
                <div className="pt-2.5 border-t border-[#ECEFEA] flex items-center justify-between text-[11px] font-mono text-[#667085]">
                  <span className="truncate">Channel ID: {item.id}</span>
                  <span className="flex items-center gap-1.5 text-[#08704F] font-semibold shrink-0 ml-2">
                    <Activity className="w-3.5 h-3.5 text-[#0B8F63]" />
                    Monitored
                  </span>
                </div>
              </Card>
            );
          })
        )}
      </div>

      {/* ==================================================
          5. ZERO SECRET EXPOSURE SECURITY ASSURANCE BANNER
          ================================================== */}
      <Card className="border-[#E5E9E6] bg-white rounded-xl p-5 shadow-2xs">
        <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4">
          <div className="flex items-start gap-3.5">
            <div className="p-2.5 rounded-xl bg-[#E8F7F0] border border-[#0B8F63]/30 text-[#08704F] shrink-0 shadow-2xs">
              <ShieldCheck className="w-5 h-5 text-[#0B8F63]" />
            </div>
            <div className="space-y-1.5">
              <div className="flex items-center gap-2">
                <h5 className="font-space-grotesk font-bold text-sm text-[#111318]">
                  Zero Secret Exposure Policy
                </h5>
                <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider bg-[#F1F4F2] text-[#667085] border border-[#E5E9E6] font-mono">
                  <Lock className="w-2.5 h-2.5" />
                  VERIFIED ENCLAVE
                </span>
              </div>
              <p className="text-xs text-[#667085] leading-relaxed max-w-2xl">
                Upstream communication provider credentials (Twilio tokens, SendGrid API keys, Razorpay secrets)
                remain isolated in secure server-side memory and are never serialized or returned through merchant-facing APIs.
              </p>
            </div>
          </div>

          <div className="flex flex-wrap sm:flex-col items-start sm:items-end gap-1 text-[10px] font-mono text-[#667085] shrink-0 pt-2 sm:pt-0 border-t sm:border-t-0 border-[#ECEFEA]">
            <span className="px-2 py-0.5 rounded bg-[#F7F8F6] border border-[#E5E9E6]">
              AES-256 Server-Side Isolation
            </span>
            <span className="px-2 py-0.5 rounded bg-[#F7F8F6] border border-[#E5E9E6]">
              No Client Secrets Serialized
            </span>
          </div>
        </div>
      </Card>
    </div>
  );
}

