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

  return (
    <Card className="shadow-2xs">
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <CardTitle>Failure Root-Causes &amp; Priority Distribution</CardTitle>
            <CardDescription>
              Analysis of underlying failure categories and priority levels across {totalCases} ingested cases
            </CardDescription>
          </div>
          <Badge variant="outline">{totalCases} Cases</Badge>
        </div>
      </CardHeader>

      <CardContent className="space-y-6">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Categories Breakdown */}
          <div className="space-y-3">
            <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-700 flex items-center gap-1.5">
              <AlertTriangle className="w-3.5 h-3.5 text-amber-500" />
              Failure Categories
            </h3>

            <div className="space-y-2.5">
              {categories.map((cat) => {
                const rate = Number(cat.recoveryRate || 0);
                const percentOfTotal = totalCases > 0 ? (cat.caseCount / totalCases) * 100 : 0;

                return (
                  <div
                    key={cat.failureReasonCategory}
                    className="p-3 rounded-xl bg-slate-50 border border-slate-200 space-y-2 hover:border-slate-300 transition-colors"
                  >
                    <div className="flex items-center justify-between text-xs">
                      <div className="font-semibold text-slate-900">
                        {cat.failureReasonCategory || 'Uncategorized'}
                      </div>
                      <div className="font-mono text-slate-500">
                        <span className="text-slate-900 font-medium">{cat.caseCount} cases</span> ({percentOfTotal.toFixed(1)}%)
                      </div>
                    </div>

                    <div className="w-full bg-slate-200 h-1.5 rounded-full overflow-hidden">
                      <div
                        className="bg-emerald-600 h-full rounded-full"
                        style={{ width: `${Math.min(100, Math.max(0, percentOfTotal))}%` }}
                      />
                    </div>

                    <div className="flex items-center justify-between text-[11px] text-slate-500 font-mono pt-0.5">
                      <span>
                        Recovered: <span className="text-emerald-700 font-bold">{formatCurrency(Number(cat.recoveredAmount || 0))}</span>
                      </span>
                      <span>
                        Rate: <span className="text-slate-900 font-semibold">{rate.toFixed(1)}%</span>
                      </span>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Priority Distribution */}
          <div className="space-y-3">
            <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-700 flex items-center gap-1.5">
              <ShieldAlert className="w-3.5 h-3.5 text-blue-600" />
              Triage Priority Breakdown
            </h3>

            <div className="space-y-2.5">
              {priorities.map((pri) => {
                const variant = priorityBadgeVariants[pri.priority] || 'default';
                const rate = Number(pri.recoveryRate || 0);
                const percentOfTotal = totalCases > 0 ? (pri.caseCount / totalCases) * 100 : 0;

                return (
                  <div
                    key={pri.priority}
                    className="p-3 rounded-xl bg-slate-50 border border-slate-200 space-y-2 hover:border-slate-300 transition-colors"
                  >
                    <div className="flex items-center justify-between text-xs">
                      <Badge variant={variant}>{pri.priority}</Badge>
                      <div className="font-mono text-slate-500">
                        <span className="text-slate-900 font-medium">{pri.caseCount} cases</span> ({percentOfTotal.toFixed(1)}%)
                      </div>
                    </div>

                    <div className="w-full bg-slate-200 h-1.5 rounded-full overflow-hidden">
                      <div
                        className={`h-full rounded-full ${
                          pri.priority === 'CRITICAL'
                            ? 'bg-rose-500'
                            : pri.priority === 'HIGH'
                            ? 'bg-amber-500'
                            : pri.priority === 'MEDIUM'
                            ? 'bg-blue-500'
                            : 'bg-slate-400'
                        }`}
                        style={{ width: `${Math.min(100, Math.max(0, percentOfTotal))}%` }}
                      />
                    </div>

                    <div className="flex items-center justify-between text-[11px] text-slate-500 font-mono pt-0.5">
                      <span>
                        Recoverable: <span className="text-slate-700 font-semibold">{formatCurrency(Number(pri.estimatedRecoverableAmount || 0))}</span>
                      </span>
                      <span>
                        Rate: <span className="text-slate-900 font-semibold">{rate.toFixed(1)}%</span>
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
