import {
  CheckCircle2,
  Clock,
  AlertCircle,
  Cpu,
  Zap,
  Send,
  ExternalLink,
  Ban,
  Radio,
} from 'lucide-react';
import type {
  RecoveryCaseDetail,
  RecoveryAttempt,
  RecoveryAttemptStatus,
} from '../../types/recovery-case';
import { Badge } from '../ui/Badge';

export interface RecoveryTimelineProps {
  caseDetail: RecoveryCaseDetail;
  attempts: RecoveryAttempt[];
}

function formatDateTime(dateStr: string | null | undefined): string {
  if (!dateStr) return '—';
  try {
    const d = new Date(dateStr);
    return d.toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
  } catch {
    return dateStr;
  }
}

function getAttemptStatusBadgeVariant(status: RecoveryAttemptStatus): 'success' | 'warning' | 'danger' | 'info' | 'default' {
  switch (status) {
    case 'SUCCESS':
    case 'DELIVERED':
    case 'CLICKED':
      return 'success';
    case 'SENT':
    case 'IN_FLIGHT':
      return 'info';
    case 'SCHEDULED':
      return 'warning';
    case 'FAILED':
      return 'danger';
    case 'SKIPPED':
    default:
      return 'default';
  }
}

export function RecoveryTimeline({ caseDetail, attempts }: RecoveryTimelineProps) {
  // Sort attempts chronologically by attemptNumber or createdAt
  const sortedAttempts = [...attempts].sort((a, b) => {
    if (a.attemptNumber !== b.attemptNumber) {
      return a.attemptNumber - b.attemptNumber;
    }
    return new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime();
  });

  return (
    <div className="space-y-6">
      <div className="relative pl-6 sm:pl-8 border-l border-slate-800 space-y-8 my-2">
        {/* Step 1: Ingestion / Case Created */}
        <div className="relative group">
          {/* Node Icon */}
          <div className="absolute -left-[31px] sm:-left-[39px] top-0 w-7 h-7 rounded-full bg-slate-900 border-2 border-indigo-500 flex items-center justify-center text-indigo-400">
            <Zap className="w-3.5 h-3.5" />
          </div>

          <div className="space-y-1">
            <div className="flex flex-wrap items-center gap-2">
              <span className="text-xs font-semibold text-white">Case Ingested & Registered</span>
              <span className="text-[11px] font-mono text-slate-400">
                {formatDateTime(caseDetail.createdAt)}
              </span>
            </div>
            <p className="text-xs text-slate-400">
              Payment failure detected via webhook. Failure category:{' '}
              <span className="text-slate-200 font-medium">
                {caseDetail.failureReasonCategory || 'Uncategorized'}
              </span>
            </p>
          </div>
        </div>

        {/* Step 2: AI Diagnosis (if completed) */}
        {caseDetail.latestDiagnosis && (
          <div className="relative group">
            <div className="absolute -left-[31px] sm:-left-[39px] top-0 w-7 h-7 rounded-full bg-slate-900 border-2 border-purple-500 flex items-center justify-center text-purple-400">
              <Cpu className="w-3.5 h-3.5" />
            </div>

            <div className="space-y-1">
              <div className="flex flex-wrap items-center gap-2">
                <span className="text-xs font-semibold text-white">AI Failure Diagnosis Engine</span>
                <span className="text-[11px] font-mono text-slate-400">
                  {formatDateTime(caseDetail.latestDiagnosis.createdAt)}
                </span>
                <Badge variant="info">
                  {Math.round(Number(caseDetail.latestDiagnosis.confidenceScore || 0) * 100)}% Confidence
                </Badge>
              </div>
              <p className="text-xs text-slate-300">
                Recommended Action:{' '}
                <span className="font-semibold text-indigo-300">
                  {caseDetail.latestDiagnosis.recommendedAction}
                </span>{' '}
                via{' '}
                <span className="font-medium text-slate-200">
                  {caseDetail.latestDiagnosis.channel}
                </span>
              </p>
              {caseDetail.latestDiagnosis.reasoning && (
                <div className="mt-2 p-2.5 rounded-lg bg-slate-950/70 border border-slate-800/80 text-xs text-slate-400 italic">
                  "{caseDetail.latestDiagnosis.reasoning}"
                </div>
              )}
            </div>
          </div>
        )}

        {/* Step 3: Recovery Attempts (Chronological) */}
        {sortedAttempts.map((attempt) => {
          const statusVariant = getAttemptStatusBadgeVariant(attempt.status);
          const isSuccess = attempt.status === 'SUCCESS';
          const isFailed = attempt.status === 'FAILED';
          const isPending = attempt.status === 'SCHEDULED' || attempt.status === 'IN_FLIGHT';

          return (
            <div key={attempt.id} className="relative group">
              <div
                className={`absolute -left-[31px] sm:-left-[39px] top-0 w-7 h-7 rounded-full bg-slate-900 border-2 flex items-center justify-center ${
                  isSuccess
                    ? 'border-emerald-500 text-emerald-400'
                    : isFailed
                    ? 'border-rose-500 text-rose-400'
                    : isPending
                    ? 'border-amber-500 text-amber-400'
                    : 'border-slate-600 text-slate-400'
                }`}
              >
                {isSuccess ? (
                  <CheckCircle2 className="w-3.5 h-3.5" />
                ) : isFailed ? (
                  <AlertCircle className="w-3.5 h-3.5" />
                ) : isPending ? (
                  <Clock className="w-3.5 h-3.5" />
                ) : (
                  <Send className="w-3.5 h-3.5" />
                )}
              </div>

              <div className="p-3.5 rounded-xl bg-slate-900/60 border border-slate-800 space-y-2">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-semibold text-white">
                      Attempt #{attempt.attemptNumber}: {attempt.channel}
                    </span>
                    <Badge variant={statusVariant}>{attempt.status}</Badge>
                  </div>
                  <span className="text-[11px] font-mono text-slate-400">
                    {formatDateTime(attempt.executedAt || attempt.scheduledAt || attempt.createdAt)}
                  </span>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs text-slate-400 font-mono">
                  {attempt.scheduledAt && (
                    <div>
                      Scheduled: <span className="text-slate-300">{formatDateTime(attempt.scheduledAt)}</span>
                    </div>
                  )}
                  {attempt.executedAt && (
                    <div>
                      Executed: <span className="text-slate-300">{formatDateTime(attempt.executedAt)}</span>
                    </div>
                  )}
                  {attempt.completedAt && (
                    <div>
                      Completed: <span className="text-slate-300">{formatDateTime(attempt.completedAt)}</span>
                    </div>
                  )}
                  {attempt.resultCode && (
                    <div>
                      Result Code: <span className="text-slate-300">{attempt.resultCode}</span>
                    </div>
                  )}
                </div>

                {attempt.resultMessage && (
                  <p className="text-xs text-slate-300">
                    Message: <span className="text-slate-400">{attempt.resultMessage}</span>
                  </p>
                )}

                {attempt.recoveryLink && (
                  <div className="pt-1">
                    <a
                      href={attempt.recoveryLink}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="inline-flex items-center gap-1.5 text-xs text-indigo-400 hover:text-indigo-300 underline"
                    >
                      <span>View Recovery Link</span>
                      <ExternalLink className="w-3 h-3" />
                    </a>
                  </div>
                )}
              </div>
            </div>
          );
        })}

        {/* Step 4: Final Outcome (if closed / recovered / cancelled / expired) */}
        {caseDetail.closedAt && (
          <div className="relative group">
            <div
              className={`absolute -left-[31px] sm:-left-[39px] top-0 w-7 h-7 rounded-full bg-slate-900 border-2 flex items-center justify-center ${
                caseDetail.status === 'RECOVERED'
                  ? 'border-emerald-500 text-emerald-400'
                  : caseDetail.status === 'CANCELLED'
                  ? 'border-slate-500 text-slate-400'
                  : 'border-rose-500 text-rose-400'
              }`}
            >
              {caseDetail.status === 'RECOVERED' ? (
                <CheckCircle2 className="w-3.5 h-3.5" />
              ) : caseDetail.status === 'CANCELLED' ? (
                <Ban className="w-3.5 h-3.5" />
              ) : (
                <AlertCircle className="w-3.5 h-3.5" />
              )}
            </div>

            <div className="space-y-1">
              <div className="flex flex-wrap items-center gap-2">
                <span className="text-xs font-semibold text-white">Case Resolved ({caseDetail.status})</span>
                <span className="text-[11px] font-mono text-slate-400">
                  {formatDateTime(caseDetail.closedAt)}
                </span>
                <Badge
                  variant={
                    caseDetail.status === 'RECOVERED'
                      ? 'success'
                      : caseDetail.status === 'CANCELLED'
                      ? 'default'
                      : 'danger'
                  }
                >
                  {caseDetail.status}
                </Badge>
              </div>
              <p className="text-xs text-slate-400">
                {caseDetail.status === 'RECOVERED'
                  ? `Revenue successfully recovered: ${new Intl.NumberFormat('en-IN', {
                      style: 'currency',
                      currency: caseDetail.currency || 'INR',
                    }).format(Number(caseDetail.recoveredAmount || 0))}`
                  : caseDetail.status === 'CANCELLED'
                  ? 'Case recovery cancelled by merchant.'
                  : 'Case closed without successful recovery.'}
              </p>
            </div>
          </div>
        )}

        {/* Live Active Status if not closed */}
        {!caseDetail.closedAt && (
          <div className="relative group">
            <div className="absolute -left-[31px] sm:-left-[39px] top-0 w-7 h-7 rounded-full bg-slate-900 border-2 border-indigo-500 flex items-center justify-center text-indigo-400">
              <Radio className="w-3.5 h-3.5 animate-pulse" />
            </div>

            <div className="space-y-1">
              <div className="flex items-center gap-2">
                <span className="text-xs font-semibold text-white">Recovery Active</span>
                <Badge variant="info" dot pulse>
                  {caseDetail.status}
                </Badge>
              </div>
              <p className="text-xs text-slate-400">
                Automated scheduler and fallback listeners are monitoring this transaction.
              </p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
