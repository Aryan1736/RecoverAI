import { AlertTriangle, ShieldAlert } from 'lucide-react';
import type { FailureCategoryMetric, FailurePriorityMetric, RecoveryPriority } from '../../types/analytics';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '../ui/Card';
import { Badge } from '../ui/Badge';
import { EmptyState } from '../ui/EmptyState';

export interface FailureAnalyticsCardProps {
  categories: FailureCategoryMetric[];
  priorities: FailurePriorityMetric[];
  totalCases?: number;
  isLoading?: boolean;
}

const priorityBadgeVariants: Record<RecoveryPriority, 'danger' | 'warning' | 'info' | 'default'> = {
  CRITICAL: 'danger',
  HIGH: 'warning',
  MEDIUM: 'info',
  LOW: 'default',
};

export function FailureAnalyticsCard({
  categories,
  priorities,
  totalCases = 0,
  isLoading = false,
}: FailureAnalyticsCardProps) {
  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0,
    }).format(val || 0);
  };

  if (isLoading) {
    return (
      <Card className="shadow-2xs">
        <CardHeader>
          <div className="h-5 w-44 bg-slate-100 rounded animate-pulse" />
          <div className="h-3 w-60 bg-slate-100 rounded animate-pulse mt-1" />
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="h-48 bg-slate-50 rounded-xl animate-pulse" />
            <div className="h-48 bg-slate-50 rounded-xl animate-pulse" />
          </div>
        </CardContent>
      </Card>
    );
  }

  if ((!categories || categories.length === 0) && (!priorities || priorities.length === 0)) {
    return (
      <Card className="shadow-2xs">
        <CardHeader>
          <CardTitle>Failure Root Causes &amp; Priority</CardTitle>
          <CardDescription>Breakdown of why transactions failed and how they are triaged</CardDescription>
        </CardHeader>
        <CardContent>
          <EmptyState
            icon={<AlertTriangle className="w-8 h-8 text-amber-500" />}
            title="No Failure Data"
            description="No failed payment cases recorded for this date range."
          />
        </CardContent>
      </Card>
    );
  }

  // Sort categories by frequency descending so highest frequency is on top
  const sortedCategories = [...categories].sort((a, b) => (b.caseCount || 0) - (a.caseCount || 0));

  return (
    <Card className="shadow-2xs border-[#E5E9E6] bg-white font-inter">
      <CardHeader className="pb-3 border-b border-[#E5E9E6]/60">
        <div className="flex items-center justify-between">
          <div className="space-y-0.5">
            <div className="flex items-center gap-2">
              <span className="w-1.5 h-1.5 rounded-full bg-[#0B8F63]" />
              <span className="text-[11px] font-bold uppercase tracking-[0.08em] text-[#08704F]">
                Root-Cause Intelligence
              </span>
            </div>
            <CardTitle className="font-space-grotesk text-lg font-bold text-[#111318]">
              Failure Root-Causes &amp; Priority Distribution
            </CardTitle>
            <CardDescription className="text-xs text-[#667085]">
              Analysis of underlying failure categories and priority levels across {totalCases} ingested cases
            </CardDescription>
          </div>
          <span className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold bg-[#F1F4F2] text-[#111318] border border-[#E5E9E6]">
            {totalCases} Cases
          </span>
        </div>
      </CardHeader>

      <CardContent className="pt-4 space-y-6">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Categories Ranked Breakdown */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="text-xs font-bold uppercase tracking-wider text-[#667085] flex items-center gap-1.5">
                <AlertTriangle className="w-3.5 h-3.5 text-[#D97706]" />
                Failure Root Causes
              </h3>
              <span className="text-[11px] text-[#98A2B3] font-mono">Ranked by volume</span>
            </div>

            <div className="space-y-2.5">
              {sortedCategories.map((cat, idx) => {
                const rate = Number(cat.recoveryRate || 0);
                const percentOfTotal = totalCases > 0 ? (cat.caseCount / totalCases) * 100 : 0;
                const isTop = idx === 0;

                return (
                  <div
                    key={cat.failureReasonCategory}
                    className={`p-3 rounded-xl border transition-all duration-150 space-y-2 ${
                      isTop
                        ? 'bg-[#E8F7F0]/40 border-[#0B8F63]/30 shadow-2xs'
                        : 'bg-[#F7F8F6] border-[#E5E9E6] hover:bg-white hover:border-[#D1D7D3]'
                    }`}
                  >
                    <div className="flex items-center justify-between text-xs">
                      <div className="flex items-center gap-2">
                        <span
                          className={`font-mono text-xs font-bold px-2 py-0.5 rounded-md ${
                            isTop
                              ? 'bg-[#E8F7F0] text-[#08704F] border border-[#0B8F63]/25'
                              : 'bg-white text-[#111318] border border-[#E5E9E6]'
                          }`}
                        >
                          {cat.failureReasonCategory || 'UNCATEGORIZED'}
                        </span>
                        {isTop && (
                          <span className="text-[10px] font-semibold text-[#08704F] uppercase tracking-wider">
                            Top Cause
                          </span>
                        )}
                      </div>
                      <div className="font-mono text-xs text-[#667085]">
                        <span className="text-[#111318] font-bold">{cat.caseCount} cases</span> ({percentOfTotal.toFixed(1)}%)
                      </div>
                    </div>

                    {/* Muted green horizontal frequency bar */}
                    <div className="w-full bg-[#E5E9E6] h-1.5 rounded-full overflow-hidden">
                      <div
                        className={`h-full rounded-full transition-all duration-300 ${
                          isTop ? 'bg-[#08704F]' : 'bg-[#0B8F63]/80'
                        }`}
                        style={{ width: `${Math.min(100, Math.max(0, percentOfTotal))}%` }}
                      />
                    </div>

                    <div className="flex items-center justify-between text-[11px] text-[#667085] font-mono pt-0.5">
                      <span>
                        Recovered: <span className="text-[#08704F] font-bold">{formatCurrency(Number(cat.recoveredAmount || 0))}</span>
                      </span>
                      <span>
                        Efficiency: <span className="text-[#111318] font-semibold">{rate.toFixed(1)}%</span>
                      </span>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Triage Priority Distribution */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="text-xs font-bold uppercase tracking-wider text-[#667085] flex items-center gap-1.5">
                <ShieldAlert className="w-3.5 h-3.5 text-[#2563EB]" />
                Priority Distribution
              </h3>
              <span className="text-[11px] text-[#98A2B3] font-mono">Triage breakdown</span>
            </div>

            {/* Stacked Proportional Distribution Bar */}
            {totalCases > 0 && priorities.length > 0 && (
              <div className="space-y-1.5 p-3 rounded-xl bg-[#F7F8F6] border border-[#E5E9E6]">
                <div className="flex items-center justify-between text-[11px] text-[#667085] font-mono">
                  <span>Proportional Volume</span>
                  <span>100% portfolio</span>
                </div>
                <div className="w-full h-2.5 rounded-full overflow-hidden flex bg-[#E5E9E6]">
                  {priorities.map((pri) => {
                    const widthPct = (pri.caseCount / totalCases) * 100;
                    if (widthPct <= 0) return null;
                    const bgClass =
                      pri.priority === 'CRITICAL'
                        ? 'bg-[#DC2626]'
                        : pri.priority === 'HIGH'
                        ? 'bg-[#D97706]'
                        : pri.priority === 'MEDIUM'
                        ? 'bg-[#2563EB]'
                        : 'bg-[#98A2B3]';

                    return (
                      <div
                        key={pri.priority}
                        title={`${pri.priority}: ${widthPct.toFixed(1)}%`}
                        className={`${bgClass} transition-all`}
                        style={{ width: `${widthPct}%` }}
                      />
                    );
                  })}
                </div>
              </div>
            )}

            {/* Priority Detailed Cards */}
            <div className="space-y-2">
              {priorities.map((pri) => {
                const variant = priorityBadgeVariants[pri.priority] || 'default';
                const rate = Number(pri.recoveryRate || 0);
                const percentOfTotal = totalCases > 0 ? (pri.caseCount / totalCases) * 100 : 0;

                return (
                  <div
                    key={pri.priority}
                    className="p-2.5 rounded-xl bg-[#F7F8F6] border border-[#E5E9E6] space-y-1.5 hover:bg-white hover:border-[#D1D7D3] transition-colors"
                  >
                    <div className="flex items-center justify-between text-xs">
                      <div className="flex items-center gap-2">
                        <Badge variant={variant}>{pri.priority}</Badge>
                        <span className="font-mono text-xs text-[#111318] font-bold">
                          {pri.caseCount} cases
                        </span>
                      </div>
                      <span className="font-mono text-xs text-[#667085]">
                        {percentOfTotal.toFixed(1)}%
                      </span>
                    </div>

                    <div className="flex items-center justify-between text-[11px] text-[#667085] font-mono pt-0.5">
                      <span>
                        Recoverable: <span className="text-[#111318] font-semibold">{formatCurrency(Number(pri.estimatedRecoverableAmount || 0))}</span>
                      </span>
                      <span>
                        Recovery Rate: <span className="text-[#08704F] font-bold">{rate.toFixed(1)}%</span>
                      </span>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
