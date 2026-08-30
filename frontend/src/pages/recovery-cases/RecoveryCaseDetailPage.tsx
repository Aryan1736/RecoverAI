import { useState, useEffect, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import {
  ArrowLeft,
  ShieldAlert,
  CreditCard,
  User,
  Cpu,
  RefreshCw,
  Ban,
  Clock,
  Sparkles,
  Layers,
} from 'lucide-react';
import { getRecoveryCase, cancelRecoveryCase } from '../../api/recovery-cases';
import type {
  RecoveryCaseDetail,
  RecoveryCaseStatus,
  RecoveryPriority,
} from '../../types/recovery-case';
import { useToast } from '../../hooks/useToast';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '../../components/ui/Card';
import { Badge } from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { Modal } from '../../components/ui/Modal';
import { ErrorState } from '../../components/ui/ErrorState';
import { RecoveryTimeline } from '../../components/recovery-cases/RecoveryTimeline';

export function RecoveryCaseDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { toast } = useToast();

  const [caseDetail, setCaseDetail] = useState<RecoveryCaseDetail | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Cancellation modal state
  const [cancelModalOpen, setCancelModalOpen] = useState<boolean>(false);
  const [isCancelling, setIsCancelling] = useState<boolean>(false);

  const fetchCaseDetail = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    setError(null);
    try {
      const data = await getRecoveryCase(id);
      setCaseDetail(data);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to load recovery case detail';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    let cancelled = false;

    async function loadData() {
      if (!id) return;
      setLoading(true);
      setError(null);
      try {
        const data = await getRecoveryCase(id);
        if (!cancelled) {
          setCaseDetail(data);
        }
      } catch (err: unknown) {
        if (!cancelled) {
          const message = err instanceof Error ? err.message : 'Failed to load recovery case detail';
          setError(message);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    loadData();

    return () => {
      cancelled = true;
    };
  }, [id]);

  const handleCancelCase = async () => {
    if (!id) return;
    setIsCancelling(true);
    try {
      await cancelRecoveryCase(id);
      toast.success('Recovery case was successfully cancelled');
      setCancelModalOpen(false);
      await fetchCaseDetail();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to cancel recovery case';
      toast.error(message);
    } finally {
      setIsCancelling(false);
    }
  };

  // Check if case is cancellable per backend domain rules (OPEN, IN_PROGRESS, FAILED)
  const isCancellable =
    caseDetail &&
    (caseDetail.status === 'OPEN' ||
      caseDetail.status === 'IN_PROGRESS' ||
      caseDetail.status === 'FAILED');

  const formatCurrency = (val: number | undefined, currency?: string) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: currency || 'INR',
      maximumFractionDigits: 2,
    }).format(val || 0);
  };

  const formatDateTime = (dateStr: string | null | undefined) => {
    if (!dateStr) return '—';
    try {
      const d = new Date(dateStr);
      return d.toLocaleString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return dateStr;
    }
  };

  const getStatusBadge = (status: RecoveryCaseStatus) => {
    switch (status) {
      case 'RECOVERED':
        return <Badge variant="success" dot>{status}</Badge>;
      case 'IN_PROGRESS':
        return <Badge variant="info" dot pulse>{status}</Badge>;
      case 'OPEN':
        return <Badge variant="warning" dot>{status}</Badge>;
      case 'FAILED':
        return <Badge variant="danger">{status}</Badge>;
      case 'EXPIRED':
      case 'CANCELLED':
      default:
        return <Badge variant="default">{status}</Badge>;
    }
  };

  const getPriorityBadge = (priority: RecoveryPriority) => {
    switch (priority) {
      case 'CRITICAL':
        return <Badge variant="danger">{priority}</Badge>;
      case 'HIGH':
        return <Badge variant="warning">{priority}</Badge>;
      case 'MEDIUM':
        return <Badge variant="info">{priority}</Badge>;
      case 'LOW':
      default:
        return <Badge variant="default">{priority}</Badge>;
    }
  };

  if (loading) {
    return (
      <div className="space-y-6">
        <div className="h-8 w-60 bg-slate-800 rounded animate-pulse" />
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="h-64 bg-slate-900/60 rounded-xl animate-pulse lg:col-span-2" />
          <div className="h-64 bg-slate-900/60 rounded-xl animate-pulse" />
        </div>
      </div>
    );
  }

  if (error || !caseDetail) {
    return (
      <div className="space-y-6">
        <Link to="/recovery-cases" className="inline-flex items-center gap-1.5 text-xs text-indigo-400 hover:text-indigo-300">
          <ArrowLeft className="w-3.5 h-3.5" /> Back to Recovery Cases
        </Link>
        <ErrorState
          title="Recovery Case Not Found"
          message={error || 'Unable to retrieve case details'}
          onRetry={fetchCaseDetail}
        />
      </div>
    );
  }

  // Extract strategy from attempts or diagnosis if present
  const latestAttempt = caseDetail.attempts && caseDetail.attempts.length > 0
    ? caseDetail.attempts[caseDetail.attempts.length - 1]
    : null;
  const strategySnapshot = latestAttempt?.strategySnapshot;

  return (
    <div className="space-y-6">
      {/* Top Breadcrumb & Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="space-y-2">
          <Link
            to="/recovery-cases"
            className="inline-flex items-center gap-1.5 text-xs text-slate-400 hover:text-indigo-300 transition"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            Back to Recovery Cases
          </Link>

          <div className="flex flex-wrap items-center gap-3">
            <h1 className="text-xl font-bold text-white font-mono flex items-center gap-2">
              <span className="text-slate-400 font-sans text-sm font-normal">Case:</span>
              {caseDetail.id}
            </h1>
            {getStatusBadge(caseDetail.status)}
            {getPriorityBadge(caseDetail.priority)}
          </div>
        </div>

        <div className="flex items-center gap-2.5">
          <Button
            variant="outline"
            size="sm"
            onClick={fetchCaseDetail}
            leftIcon={<RefreshCw className="w-3.5 h-3.5" />}
          >
            Refresh
          </Button>

          {isCancellable && (
            <Button
              variant="danger"
              size="sm"
              onClick={() => setCancelModalOpen(true)}
              leftIcon={<Ban className="w-3.5 h-3.5" />}
            >
              Cancel Case
            </Button>
          )}
        </div>
      </div>

      {/* Confirmation Modal for Case Cancellation */}
      <Modal
        isOpen={cancelModalOpen}
        onClose={() => setCancelModalOpen(false)}
        title="Cancel Recovery Case"
        description="Are you sure you want to cancel this automated recovery process?"
        footer={
          <>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setCancelModalOpen(false)}
              disabled={isCancelling}
            >
              Keep Case Active
            </Button>
            <Button
              variant="danger"
              size="sm"
              onClick={handleCancelCase}
              isLoading={isCancelling}
              leftIcon={<Ban className="w-3.5 h-3.5" />}
            >
              Confirm Cancellation
            </Button>
          </>
        }
      >
        <div className="space-y-3 text-xs text-slate-300">
          <p>
            Cancelling this case will immediately halt all pending and future recovery attempts for transaction{' '}
            <span className="font-mono text-white font-semibold">
              {caseDetail.payment?.razorpayPaymentId || caseDetail.id}
            </span>.
          </p>
          <p className="text-slate-400">
            Any currently scheduled communications (WhatsApp, Email, SMS) will be marked as{' '}
            <code className="text-amber-300 font-mono">SKIPPED</code> and will not be dispatched.
          </p>
        </div>
      </Modal>

      {/* Main Grid: Left 2 Cols (Case Summary, Diagnosis, Strategy, Timeline) & Right 1 Col (Customer, Payment) */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column (2 Cols) */}
        <div className="lg:col-span-2 space-y-6">
          {/* Section A: Case Summary */}
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <ShieldAlert className="w-4 h-4 text-indigo-400" />
                  <CardTitle>Case Summary</CardTitle>
                </div>
                <Badge variant="outline">Failure: {caseDetail.failureReasonCategory || 'Uncategorized'}</Badge>
              </div>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 text-xs">
                <div className="p-3 rounded-xl bg-slate-950/60 border border-slate-800 space-y-1">
                  <span className="text-slate-400 uppercase tracking-wider text-[10px] font-semibold">Recoverable</span>
                  <div className="text-base font-bold text-white font-mono">
                    {formatCurrency(caseDetail.estimatedRecoverableAmount, caseDetail.currency)}
                  </div>
                </div>

                <div className="p-3 rounded-xl bg-slate-950/60 border border-slate-800 space-y-1">
                  <span className="text-slate-400 uppercase tracking-wider text-[10px] font-semibold">Recovered</span>
                  <div className="text-base font-bold text-emerald-400 font-mono">
                    {formatCurrency(caseDetail.recoveredAmount, caseDetail.currency)}
                  </div>
                </div>

                <div className="p-3 rounded-xl bg-slate-950/60 border border-slate-800 space-y-1">
                  <span className="text-slate-400 uppercase tracking-wider text-[10px] font-semibold">Created At</span>
                  <div className="font-mono text-slate-300 text-[11px]">
                    {formatDateTime(caseDetail.createdAt)}
                  </div>
                </div>

                <div className="p-3 rounded-xl bg-slate-950/60 border border-slate-800 space-y-1">
                  <span className="text-slate-400 uppercase tracking-wider text-[10px] font-semibold">Expires At</span>
                  <div className="font-mono text-slate-300 text-[11px]">
                    {formatDateTime(caseDetail.expiresAt)}
                  </div>
                </div>
              </div>

              {(caseDetail.recoveredAt || caseDetail.closedAt) && (
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mt-4 pt-4 border-t border-slate-800 text-xs">
                  {caseDetail.recoveredAt && (
                    <div className="flex items-center gap-2 text-emerald-400 font-mono text-[11px]">
                      <Clock className="w-3.5 h-3.5" />
                      Recovered on: {formatDateTime(caseDetail.recoveredAt)}
                    </div>
                  )}
                  {caseDetail.closedAt && (
                    <div className="flex items-center gap-2 text-slate-400 font-mono text-[11px]">
                      <Clock className="w-3.5 h-3.5" />
                      Closed on: {formatDateTime(caseDetail.closedAt)}
                    </div>
                  )}
                </div>
              )}
            </CardContent>
          </Card>

          {/* Section D: AI Failure Diagnosis */}
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Cpu className="w-4 h-4 text-purple-400" />
                  <CardTitle>AI Diagnosis & Reasoning</CardTitle>
                </div>
                {caseDetail.latestDiagnosis ? (
                  <Badge variant="info">
                    {Math.round(Number(caseDetail.latestDiagnosis.confidenceScore || 0) * 100)}% Confidence
                  </Badge>
                ) : (
                  <Badge variant="default">Pending</Badge>
                )}
              </div>
              <CardDescription>
                Autonomous root-cause deduction via{' '}
                <span className="font-mono text-indigo-300">
                  {caseDetail.latestDiagnosis?.modelName || 'Google Gemini 3.7 Flash'}
                </span>
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4 text-xs">
              {caseDetail.latestDiagnosis ? (
                <>
                  <div className="p-3 rounded-xl bg-purple-950/20 border border-purple-500/20 space-y-1.5">
                    <div className="flex items-center justify-between">
                      <span className="font-semibold text-purple-300 uppercase tracking-wider text-[10px]">
                        Prescribed Recovery Action
                      </span>
                      <span className="text-[11px] font-mono text-slate-400">
                        Channel: {caseDetail.latestDiagnosis.channel}
                      </span>
                    </div>
                    <div className="text-sm font-bold text-white">
                      {caseDetail.latestDiagnosis.recommendedAction}
                    </div>
                  </div>

                  <div className="space-y-1">
                    <span className="text-slate-400 font-semibold uppercase text-[10px] tracking-wider block">
                      Autonomous Reasoning
                    </span>
                    <div className="p-3 rounded-xl bg-slate-950/70 border border-slate-800 text-slate-300 leading-relaxed italic">
                      "{caseDetail.latestDiagnosis.reasoning}"
                    </div>
                  </div>

                  {caseDetail.latestDiagnosis.decisionFactors && (
                    <div className="space-y-1">
                      <span className="text-slate-400 font-semibold uppercase text-[10px] tracking-wider block">
                        Diagnostic Factors
                      </span>
                      <div className="p-2.5 rounded-lg bg-slate-950 border border-slate-800/80 font-mono text-[11px] text-slate-400 break-all">
                        {caseDetail.latestDiagnosis.decisionFactors}
                      </div>
                    </div>
                  )}
                </>
              ) : (
                <div className="p-4 rounded-xl bg-slate-950/50 border border-slate-800 text-slate-400 text-xs">
                  Diagnosis pending orchestration queue execution.
                </div>
              )}
            </CardContent>
          </Card>

          {/* Section E: Recovery Strategy */}
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Sparkles className="w-4 h-4 text-indigo-400" />
                  <CardTitle>Recovery Strategy</CardTitle>
                </div>
                {strategySnapshot?.priority && (
                  <Badge variant="outline">Priority: {strategySnapshot.priority}</Badge>
                )}
              </div>
              <CardDescription>
                Tailored multi-channel dispatch policy and deterministic fallback rules
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-3 text-xs">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div className="p-3 rounded-xl bg-slate-950/60 border border-slate-800 space-y-1">
                  <span className="text-[10px] text-slate-400 uppercase font-semibold">Primary Channel</span>
                  <div className="font-semibold text-slate-100 flex items-center gap-1.5">
                    <span className="w-2 h-2 rounded-full bg-emerald-400" />
                    {strategySnapshot?.channel || caseDetail.latestDiagnosis?.channel || 'Autonomous Selection'}
                  </div>
                  <div className="text-[11px] text-slate-400">
                    Action: {strategySnapshot?.recommendedAction || caseDetail.latestDiagnosis?.recommendedAction || '—'}
                  </div>
                </div>

                <div className="p-3 rounded-xl bg-slate-950/60 border border-slate-800 space-y-1">
                  <span className="text-[10px] text-slate-400 uppercase font-semibold">Fallback Channel</span>
                  <div className="font-semibold text-slate-100 flex items-center gap-1.5">
                    <span className="w-2 h-2 rounded-full bg-amber-400" />
                    {strategySnapshot?.fallbackChannel || 'EMAIL (Automatic)'}
                  </div>
                  <div className="text-[11px] text-slate-400">
                    Action: {strategySnapshot?.fallbackAction || 'Standard Fallback Dispatch'}
                  </div>
                </div>
              </div>

              {strategySnapshot?.reason && (
                <p className="text-slate-400 text-xs italic">
                  Strategy rationale: {strategySnapshot.reason}
                </p>
              )}
            </CardContent>
          </Card>

          {/* Section F: Recovery Attempts & Execution Timeline */}
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Layers className="w-4 h-4 text-emerald-400" />
                  <CardTitle>Execution Timeline & Attempts</CardTitle>
                </div>
                <Badge variant="outline">{caseDetail.attempts.length} Attempts</Badge>
              </div>
              <CardDescription>
                Chronological ledger of autonomous recovery events, provider callbacks, and customer touchpoints
              </CardDescription>
            </CardHeader>
            <CardContent>
              <RecoveryTimeline caseDetail={caseDetail} attempts={caseDetail.attempts} />
            </CardContent>
          </Card>
        </div>

        {/* Right Column (1 Col): Customer & Payment Info */}
        <div className="space-y-6">
          {/* Section B: Customer Card */}
          <Card>
            <CardHeader>
              <div className="flex items-center gap-2">
                <User className="w-4 h-4 text-indigo-400" />
                <CardTitle>Customer Information</CardTitle>
              </div>
            </CardHeader>
            <CardContent className="space-y-3 text-xs">
              <div className="space-y-1">
                <span className="text-[10px] text-slate-400 uppercase font-semibold">Full Name</span>
                <div className="font-semibold text-slate-100 text-sm">
                  {caseDetail.customer?.name || 'Anonymous Customer'}
                </div>
              </div>

              <div className="space-y-1">
                <span className="text-[10px] text-slate-400 uppercase font-semibold">Email Address</span>
                <div className="font-mono text-slate-200 break-all">
                  {caseDetail.customer?.email || '—'}
                </div>
              </div>

              <div className="space-y-1">
                <span className="text-[10px] text-slate-400 uppercase font-semibold">Phone Number</span>
                <div className="font-mono text-slate-200">
                  {caseDetail.customer?.phone || '—'}
                </div>
              </div>

              {caseDetail.customer?.razorpayCustomerId && (
                <div className="space-y-1 pt-2 border-t border-slate-800">
                  <span className="text-[10px] text-slate-400 uppercase font-semibold">Razorpay Customer ID</span>
                  <div className="font-mono text-indigo-300 text-[11px]">
                    {caseDetail.customer.razorpayCustomerId}
                  </div>
                </div>
              )}
            </CardContent>
          </Card>

          {/* Section C: Payment Card */}
          <Card>
            <CardHeader>
              <div className="flex items-center gap-2">
                <CreditCard className="w-4 h-4 text-emerald-400" />
                <CardTitle>Payment Details</CardTitle>
              </div>
              <CardDescription>Underlying transaction data</CardDescription>
            </CardHeader>
            <CardContent className="space-y-3 text-xs">
              <div className="space-y-1">
                <span className="text-[10px] text-slate-400 uppercase font-semibold">Payment Amount</span>
                <div className="text-lg font-bold text-white font-mono">
                  {formatCurrency(caseDetail.payment?.amount, caseDetail.payment?.currency)}
                </div>
              </div>

              <div className="grid grid-cols-2 gap-2 pt-2 border-t border-slate-800">
                <div>
                  <span className="text-[10px] text-slate-400 uppercase font-semibold">Status</span>
                  <div className="font-medium text-slate-200 mt-0.5">
                    <Badge variant={caseDetail.payment?.status === 'CAPTURED' ? 'success' : 'danger'}>
                      {caseDetail.payment?.status || 'UNKNOWN'}
                    </Badge>
                  </div>
                </div>
                <div>
                  <span className="text-[10px] text-slate-400 uppercase font-semibold">Method</span>
                  <div className="font-medium text-slate-200 mt-0.5 font-mono">
                    {caseDetail.payment?.method || 'OTHER'}
                  </div>
                </div>
              </div>

              <div className="space-y-1 pt-2 border-t border-slate-800">
                <span className="text-[10px] text-slate-400 uppercase font-semibold">Razorpay Payment ID</span>
                <div className="font-mono text-slate-200 break-all text-[11px]">
                  {caseDetail.payment?.razorpayPaymentId || '—'}
                </div>
              </div>

              {caseDetail.payment?.razorpayOrderId && (
                <div className="space-y-1">
                  <span className="text-[10px] text-slate-400 uppercase font-semibold">Razorpay Order ID</span>
                  <div className="font-mono text-slate-300 break-all text-[11px]">
                    {caseDetail.payment.razorpayOrderId}
                  </div>
                </div>
              )}

              {caseDetail.payment?.errorCode && (
                <div className="p-3 rounded-xl bg-rose-950/20 border border-rose-500/20 space-y-1">
                  <div className="text-[10px] uppercase font-semibold text-rose-400">
                    Error: {caseDetail.payment.errorCode}
                  </div>
                  <div className="text-slate-300 text-[11px]">
                    {caseDetail.payment.errorDescription || caseDetail.payment.errorReason || 'Payment declined by gateway'}
                  </div>
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
