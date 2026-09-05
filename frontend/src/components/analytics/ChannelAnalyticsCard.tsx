import { useMemo } from 'react';
import { MessageSquare, Mail, Smartphone, RefreshCw, Link2, UserCheck, Radio, Zap } from 'lucide-react';
import type { ChannelMetric, RecoveryChannel } from '../../types/analytics';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '../ui/Card';
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

  // Dynamically calculate the best performing channel from active data
  const bestChannel = useMemo(() => {
    if (!channels || channels.length === 0) return null;
    const poolWithAttempts = channels.filter((c) => (c.totalAttempts || 0) > 0);
    const pool = poolWithAttempts.length > 0 ? poolWithAttempts : channels;
    return [...pool].sort((a, b) => {
      const rateDiff = (b.successRate || 0) - (a.successRate || 0);
      if (Math.abs(rateDiff) > 0.01) return rateDiff;
      return (b.recoveredAmount || 0) - (a.recoveredAmount || 0);
    })[0];
  }, [channels]);

  if (isLoading) {
    return (
      <Card className="shadow-2xs border-[#E5E9E6] bg-white">
        <CardHeader>
          <div className="h-5 w-44 bg-slate-100 rounded animate-pulse" />
          <div className="h-3 w-60 bg-slate-100 rounded animate-pulse mt-1" />
        </CardHeader>
        <CardContent>
          <div className="space-y-3">
            {[1, 2, 3].map((i) => (
              <div key={i} className="h-16 w-full bg-[#F1F4F2] rounded-xl animate-pulse" />
            ))}
          </div>
        </CardContent>
      </Card>
    );
  }

  if (!channels || channels.length === 0) {
    return (
      <Card className="shadow-2xs border-[#E5E9E6] bg-white">
        <CardHeader>
          <CardTitle className="font-space-grotesk text-lg font-bold text-[#111318]">Channel Performance</CardTitle>
          <CardDescription className="text-xs text-[#667085]">Multi-channel orchestration engagement &amp; conversion metrics</CardDescription>
        </CardHeader>
        <CardContent>
          <EmptyState
            icon={<Radio className="w-8 h-8 text-[#0B8F63]" />}
            title="No Channel Activity"
            description="No recovery dispatches have occurred over the selected date range."
          />
        </CardContent>
      </Card>
    );
  }

  const bestConf = bestChannel ? channelConfig[bestChannel.channel] || { label: bestChannel.channel } : null;

  return (
    <Card className="shadow-2xs border-[#E5E9E6] bg-white font-inter">
      <CardHeader className="pb-3 border-b border-[#E5E9E6]/60">
        <div className="flex items-center justify-between">
          <div className="space-y-0.5">
            <div className="flex items-center gap-2">
              <span className="w-1.5 h-1.5 rounded-full bg-[#0B8F63]" />
              <span className="text-[11px] font-bold uppercase tracking-[0.08em] text-[#08704F]">
                Channel Conversion
              </span>
            </div>
            <CardTitle className="font-space-grotesk text-lg font-bold text-[#111318]">
              Channel Performance
            </CardTitle>
            <CardDescription className="text-xs text-[#667085]">
              Efficiency across {channels.length} active recovery avenues ({totalAttempts} total attempts)
            </CardDescription>
          </div>
          <span className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold bg-[#F1F4F2] text-[#111318] border border-[#E5E9E6]">
            {totalAttempts} Dispatched
          </span>
        </div>
      </CardHeader>

      <CardContent className="pt-4 space-y-4">
        {/* Dynamic Channel Highlight: Best Performing Channel */}
        {bestChannel && (
          <div className="flex items-center justify-between p-3 rounded-xl bg-[#E8F7F0] border border-[#0B8F63]/25 shadow-2xs">
            <div className="flex items-center gap-3">
              <div className="p-2 rounded-lg bg-white text-[#08704F] shadow-2xs border border-[#0B8F63]/20">
                <Zap className="w-4 h-4 text-[#0B8F63]" />
              </div>
              <div>
                <span className="text-[10px] font-bold uppercase tracking-[0.08em] text-[#08704F] block">
                  Best Performing Channel
                </span>
                <div className="text-sm font-bold text-[#111318] flex items-center gap-2">
                  <span>{bestConf?.label} (Top Performer)</span>
                  <span className="text-xs font-mono font-bold text-[#08704F] bg-white px-2 py-0.5 rounded-md border border-[#0B8F63]/20">
                    {Number(bestChannel.successRate || 0).toFixed(1)}% success rate
                  </span>
                </div>
              </div>
            </div>
            <div className="text-right font-mono">
              <div className="text-sm font-bold text-[#08704F]">
                {formatCurrency(Number(bestChannel.recoveredAmount || 0))}
              </div>
              <div className="text-[10px] text-[#667085]">recovered</div>
            </div>
          </div>
        )}

        {/* Compact Horizontal Success Indicator Channel Rows */}
        <div className="space-y-2.5">
          {channels.map((metric) => {
            const conf = channelConfig[metric.channel] || {
              label: metric.channel,
              icon: MessageSquare,
              badgeVariant: 'default' as const,
            };
            const Icon = conf.icon;
            const rate = Number(metric.successRate || 0);

            return (
              <div
                key={metric.channel}
                className="p-3 rounded-xl bg-[#F7F8F6] border border-[#E5E9E6] hover:bg-white hover:border-[#D1D7D3] hover:shadow-2xs transition-all duration-200 space-y-2"
              >
                <div className="flex items-center justify-between gap-3">
                  <div className="flex items-center gap-2.5 min-w-0">
                    <div className="p-1.5 rounded-lg bg-white border border-[#E5E9E6] text-[#08704F] shadow-2xs shrink-0">
                      <Icon className="w-4 h-4 text-[#0B8F63]" />
                    </div>
                    <div className="min-w-0">
                      <div className="font-semibold text-xs text-[#111318] truncate flex items-center gap-2">
                        <span>{conf.label}</span>
                        <span className="text-[10px] font-mono text-[#98A2B3] uppercase">
                          {metric.channel}
                        </span>
                      </div>
                      <div className="text-[11px] text-[#667085] flex items-center gap-2 mt-0.5">
                        <span className="font-mono">{metric.totalAttempts} attempts</span>
                        <span className="text-[#D1D7D3]">•</span>
                        <span>{metric.deliveredAttempts} del / {metric.clickedAttempts} clk</span>
                      </div>
                    </div>
                  </div>

                  <div className="text-right shrink-0">
                    <div className="font-space-grotesk font-bold text-xs text-[#08704F] tabular-nums">
                      {formatCurrency(Number(metric.recoveredAmount || 0))}
                    </div>
                    <div className="text-[10px] text-[#667085] font-mono">
                      {metric.successfulAttempts} ok / {metric.failedAttempts} err
                    </div>
                  </div>
                </div>

                {/* Horizontal Success Indicator */}
                <div className="flex items-center gap-3 pt-0.5">
                  <div className="flex-1 bg-[#E5E9E6] h-2 rounded-full overflow-hidden">
                    <div
                      className="bg-[#0B8F63] h-full rounded-full transition-all duration-500"
                      style={{ width: `${Math.min(100, Math.max(0, rate))}%` }}
                    />
                  </div>
                  <div className="w-12 text-right font-mono font-bold text-xs text-[#111318] tabular-nums">
                    {rate.toFixed(1)}%
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </CardContent>
    </Card>
  );
}
