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
} from 'lucide-react';
import { Card } from '../ui/Card';
import { Badge, type BadgeVariant } from '../ui/Badge';
import { Button } from '../ui/Button';
import { Skeleton } from '../ui/Skeleton';
import { ErrorState } from '../ui/ErrorState';
import { getProviderHealth } from '../../api/providers';
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

export function ProviderHealthCard() {
  const [summary, setSummary] = useState<ProviderHealthSummary | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isRefreshing, setIsRefreshing] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const fetchHealth = async (isManualRefresh = false) => {
    if (isManualRefresh) setIsRefreshing(true);
    else setIsLoading(true);
    setError(null);

    try {
      const data = await getProviderHealth();
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
        const data = await getProviderHealth();
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
  }, []);

  if (isLoading) {
    return (
      <div className="space-y-4" role="status" aria-label="Loading provider status">
        <Skeleton className="h-28 w-full rounded-2xl" />
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <Skeleton className="h-36 rounded-xl" />
          <Skeleton className="h-36 rounded-xl" />
          <Skeleton className="h-36 rounded-xl" />
          <Skeleton className="h-36 rounded-xl" />
        </div>
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

  return (
    <div className="space-y-6">
      {/* Overall Health Banner */}
      <div
        className={`p-5 rounded-2xl border flex flex-col sm:flex-row sm:items-center justify-between gap-4 transition-colors ${
          overall === 'HEALTHY'
            ? 'bg-emerald-950/20 border-emerald-500/30'
            : overall === 'DEGRADED'
            ? 'bg-amber-950/20 border-amber-500/30'
            : overall === 'UNAVAILABLE'
            ? 'bg-rose-950/20 border-rose-500/30'
            : 'bg-slate-900/60 border-slate-800'
        }`}
      >
        <div className="flex items-center gap-3.5">
          <div
            className={`w-10 h-10 rounded-xl flex items-center justify-center border ${
              overall === 'HEALTHY'
                ? 'bg-emerald-500/20 border-emerald-500/30 text-emerald-400'
                : overall === 'DEGRADED'
                ? 'bg-amber-500/20 border-amber-500/30 text-amber-400'
                : overall === 'UNAVAILABLE'
                ? 'bg-rose-500/20 border-rose-500/30 text-rose-400'
                : 'bg-slate-800 border-slate-700 text-slate-400'
            }`}
          >
            {overall === 'HEALTHY' ? (
              <CheckCircle2 className="w-5 h-5" />
            ) : overall === 'DEGRADED' ? (
              <AlertTriangle className="w-5 h-5" />
            ) : overall === 'UNAVAILABLE' ? (
              <AlertOctagon className="w-5 h-5" />
            ) : (
              <HelpCircle className="w-5 h-5" />
            )}
          </div>

          <div>
            <div className="flex items-center gap-2">
              <h3 className="text-sm font-bold text-white">
                {overall === 'HEALTHY'
                  ? 'All Recovery & Communication Providers Operational'
                  : overall === 'DEGRADED'
                  ? 'Upstream Provider Degradation Observed'
                  : overall === 'UNAVAILABLE'
                  ? 'Critical Provider Connectivity Interrupted'
                  : 'Provider Telemetry Status'}
              </h3>
              <Badge variant={overallConfig.variant} dot pulse={overallConfig.pulse}>
                {overallConfig.label}
              </Badge>
            </div>
            <p className="text-xs text-slate-400 mt-0.5">
              Live automated health checks from RecoverAI multi-channel dispatch engine.
            </p>
          </div>
        </div>

        <div className="flex items-center gap-3 self-end sm:self-center shrink-0">
          {summary?.lastChecked && (
            <span className="text-[11px] text-slate-400 font-mono hidden sm:inline">
              Checked: {new Date(summary.lastChecked).toLocaleTimeString('en-US', { hour12: false })}
            </span>
          )}
          <Button
            size="sm"
            variant="outline"
            onClick={() => fetchHealth(true)}
            isLoading={isRefreshing}
            leftIcon={<RefreshCw className="w-3.5 h-3.5" />}
          >
            Check Status
          </Button>
        </div>
      </div>

      {/* Provider Status Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        {providers.length === 0 ? (
          <div className="col-span-2 p-8 text-center rounded-xl bg-slate-900/40 border border-slate-800 text-slate-400 text-xs">
            No provider health checks configured.
          </div>
        ) : (
          providers.map((item: ProviderStatusItem) => {
            const ChannelIcon = getChannelIcon(item.channel);
            const badgeConfig = getStatusBadgeConfig(item.status);

            return (
              <Card
                key={item.id}
                className="border-slate-800 bg-slate-950/60 hover:border-slate-700 transition space-y-3"
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="flex items-center gap-3">
                    <div className="w-9 h-9 rounded-xl bg-slate-900 border border-slate-800 text-slate-300 flex items-center justify-center shrink-0">
                      <ChannelIcon className="w-4 h-4" />
                    </div>
                    <div>
                      <h4 className="text-sm font-semibold text-white">
                        {item.name}
                      </h4>
                      <span className="text-xs text-slate-400 font-medium">
                        {item.channel}
                      </span>
                    </div>
                  </div>

                  <Badge variant={badgeConfig.variant} dot pulse={badgeConfig.pulse}>
                    {badgeConfig.label}
                  </Badge>
                </div>

                {item.message ? (
                  <div className="p-2.5 rounded-lg bg-slate-900/80 border border-slate-800/80 text-xs font-mono text-slate-300 break-words">
                    {item.message}
                  </div>
                ) : (
                  <div className="text-xs text-slate-500 italic">
                    Diagnostic check returned nominal response without warnings.
                  </div>
                )}

                <div className="pt-2 border-t border-slate-900 flex items-center justify-between text-[10px] text-slate-500 font-mono">
                  <span>Channel ID: {item.id}</span>
                  <span className="flex items-center gap-1 text-slate-400">
                    <Activity className="w-3 h-3 text-indigo-400" />
                    Monitored
                  </span>
                </div>
              </Card>
            );
          })
        )}
      </div>

      {/* Zero Secret Exposure Security Assurance Banner */}
      <Card className="border-slate-800/80 bg-slate-950/40">
        <div className="flex items-start gap-3">
          <div className="p-2 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 shrink-0">
            <ShieldCheck className="w-4 h-4" />
          </div>
          <div className="text-xs text-slate-400 space-y-1">
            <h5 className="font-semibold text-slate-200">
              Zero Secret Exposure Policy
            </h5>
            <p>
              Upstream communication provider credentials (Twilio tokens, SendGrid API keys, Razorpay secrets)
              remain isolated in secure server-side memory and are never serialized or returned through merchant-facing APIs.
            </p>
          </div>
        </div>
      </Card>
    </div>
  );
}
