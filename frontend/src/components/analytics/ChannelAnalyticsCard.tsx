import { MessageSquare, Mail, Smartphone, RefreshCw, Link2, UserCheck, Radio } from 'lucide-react';
import type { ChannelMetric, RecoveryChannel } from '../../types/analytics';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '../ui/Card';
import { Badge } from '../ui/Badge';
import { EmptyState } from '../ui/EmptyState';

export interface ChannelAnalyticsCardProps {
  channels: ChannelMetric[];
  totalAttempts?: number;
  isLoading?: boolean;
}

const channelConfig: Record<
  RecoveryChannel,
  { label: string; icon: typeof MessageSquare; badgeVariant: 'info' | 'success' | 'warning' | 'default' }
> = {
  WHATSAPP: { label: 'WhatsApp', icon: MessageSquare, badgeVariant: 'success' },
  EMAIL: { label: 'Email', icon: Mail, badgeVariant: 'info' },
  SMS: { label: 'SMS', icon: Smartphone, badgeVariant: 'warning' },
  SMART_LINK: { label: 'Smart Link', icon: Link2, badgeVariant: 'info' },
  RETRY_CHARGE: { label: 'Retry Charge', icon: RefreshCw, badgeVariant: 'default' },
  MANUAL: { label: 'Manual Ops', icon: UserCheck, badgeVariant: 'default' },
};

export function ChannelAnalyticsCard({
  channels,
  totalAttempts = 0,
  isLoading = false,
}: ChannelAnalyticsCardProps) {
  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0,
    }).format(val || 0);
  };

  if (isLoading) {
    return (
      <Card>
        <CardHeader>
          <div className="h-5 w-44 bg-slate-800 rounded animate-pulse" />
          <div className="h-3 w-60 bg-slate-800/60 rounded animate-pulse mt-1" />
        </CardHeader>
        <CardContent>
          <div className="space-y-3">
            {[1, 2, 3].map((i) => (
              <div key={i} className="h-16 w-full bg-slate-900/60 rounded-xl animate-pulse" />
            ))}
          </div>
        </CardContent>
      </Card>
    );
  }

  if (!channels || channels.length === 0) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Recovery Channels</CardTitle>
          <CardDescription>Multi-channel orchestration engagement & conversion metrics</CardDescription>
        </CardHeader>
        <CardContent>
          <EmptyState
            icon={<Radio className="w-8 h-8 text-indigo-400" />}
            title="No Channel Activity"
            description="No recovery dispatches have occurred over the selected date range."
          />
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <CardTitle>Channel Performance</CardTitle>
            <CardDescription>
              Conversion efficiency across {channels.length} active recovery avenues ({totalAttempts} total attempts)
            </CardDescription>
          </div>
          <Badge variant="outline">{totalAttempts} Dispatched</Badge>
        </div>
      </CardHeader>

      <CardContent>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="border-b border-slate-800 text-slate-400 uppercase tracking-wider font-semibold">
                <th className="py-2.5 px-3">Channel</th>
                <th className="py-2.5 px-3 text-right">Attempts</th>
                <th className="py-2.5 px-3 text-right">Delivered / Clicked</th>
                <th className="py-2.5 px-3 text-right">Success Rate</th>
                <th className="py-2.5 px-3 text-right">Revenue Recovered</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/70">
              {channels.map((metric) => {
                const conf = channelConfig[metric.channel] || {
                  label: metric.channel,
                  icon: MessageSquare,
                  badgeVariant: 'default' as const,
                };
                const Icon = conf.icon;
                const rate = Number(metric.successRate || 0);

                return (
                  <tr key={metric.channel} className="hover:bg-slate-900/50 transition">
                    <td className="py-3 px-3">
                      <div className="flex items-center gap-2.5">
                        <div className="p-1.5 rounded-lg bg-slate-950 border border-slate-800 text-slate-300">
                          <Icon className="w-4 h-4 text-indigo-400" />
                        </div>
                        <div>
                          <div className="font-medium text-slate-200">{conf.label}</div>
                          <div className="text-[10px] text-slate-400 font-mono">{metric.channel}</div>
                        </div>
                      </div>
                    </td>

                    <td className="py-3 px-3 text-right font-mono text-slate-200">
                      <div>{metric.totalAttempts}</div>
                      <div className="text-[10px] text-emerald-400">
                        {metric.successfulAttempts} successful / {metric.failedAttempts} failed
                      </div>
                    </td>

                    <td className="py-3 px-3 text-right font-mono text-slate-300">
                      <div>{metric.deliveredAttempts} delivered</div>
                      <div className="text-[10px] text-slate-400">{metric.clickedAttempts} clicked</div>
                    </td>

                    <td className="py-3 px-3 text-right">
                      <div className="flex items-center justify-end gap-2">
                        <div className="w-16 bg-slate-800 h-1.5 rounded-full overflow-hidden hidden sm:block">
                          <div
                            className="bg-emerald-500 h-full rounded-full"
                            style={{ width: `${Math.min(100, Math.max(0, rate))}%` }}
                          />
                        </div>
                        <span className="font-mono font-semibold text-slate-200">{rate.toFixed(1)}%</span>
                      </div>
                    </td>

                    <td className="py-3 px-3 text-right font-mono font-semibold text-emerald-400">
                      {formatCurrency(Number(metric.recoveredAmount || 0))}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </CardContent>
    </Card>
  );
}
