import { useState, useEffect, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import {
  ArrowLeft,
  CreditCard,
  User,
  Cpu,
  RefreshCw,
  Ban,
  Sparkles,
  Layers,
  CheckCircle2,
  PlayCircle,
} from 'lucide-react';
import { getRecoveryCase, cancelRecoveryCase } from '../../api/recovery-cases';
import { getDemoRecoveryCase, simulateDemoRecovery } from '../../api/demo';
import { useDemoMode } from '../../hooks/useDemoMode';
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
  const { isDemoMode } = useDemoMode();
  const { toast } = useToast();

  const [caseDetail, setCaseDetail] = useState<RecoveryCaseDetail | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Cancellation modal state
  const [cancelModalOpen, setCancelModalOpen] = useState<boolean>(false);
  const [isCancelling, setIsCancelling] = useState<boolean>(false);

  // Simulation modal state
  const [simulateModalOpen, setSimulateModalOpen] = useState<boolean>(false);
  const [isSimulating, setIsSimulating] = useState<boolean>(false);
  const [simulationStep, setSimulationStep] = useState<number>(0);

  const fetchCaseDetail = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    setError(null);
    try {
      const data = isDemoMode ? await getDemoRecoveryCase(id) : await getRecoveryCase(id);
      setCaseDetail(data);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to load recovery case detail';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, [id, isDemoMode]);

  useEffect(() => {
    let cancelled = false;

    async function loadData() {
      if (!id) return;
      setLoading(true);
      setError(null);
      try {
        const data = isDemoMode ? await getDemoRecoveryCase(id) : await getRecoveryCase(id);
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
  }, [id, isDemoMode]);

  const handleCancelCase = async () => {
    if (!id) return;
    setIsCancelling(true);
    try {
      const updated = await cancelRecoveryCase(id);
      toast.success('Recovery case was successfully cancelled.');
      setCancelModalOpen(false);
      setCaseDetail((prev) => (prev ? { ...prev, status: updated.status } : null));
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to cancel recovery case';
      toast.error(msg);
    } finally {
      setIsCancelling(false);
    }
  };

  const handleExecuteSimulation = async () => {
    if (!id) return;
    setIsSimulating(true);
    try {
      const updated = await simulateDemoRecovery(id);
      setCaseDetail(updated);
      toast.success('Simulation complete: Payment captured and case recovered!');
      setSimulateModalOpen(false);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Simulation failed';
      toast.error(msg);
      setSimulateModalOpen(false);
    } finally {
      setIsSimulating(false);
      setSimulationStep(0);
    }
  };

  const formatCurrency = (amount: number | null | undefined, currency?: string) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: currency || 'INR',
      maximumFractionDigits: 2,
    }).format(amount || 0);
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
        <div className="h-6 w-32 bg-slate-200 rounded animate-pulse" />
        <div className="h-20 w-full bg-white border border-slate-200 rounded-xl animate-pulse" />
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2 h-96 bg-white border border-slate-200 rounded-xl animate-pulse" />
          <div className="h-96 bg-white border border-slate-200 rounded-xl animate-pulse" />
        </div>
      </div>
    );
  }

  if (error || !caseDetail) {
    return (
      <div className="space-y-6">
        <Link
          to="/recovery-cases"
          className="inline-flex items-center gap-1.5 text-xs text-slate-500 hover:text-slate-900 transition"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          <span>Back to Cases</span>
        </Link>
        <ErrorState
          title="Recovery Case Not Found"
          message={error || 'The requested recovery case could not be retrieved.'}
          onRetry={fetchCaseDetail}
        />
      </div>
    );
  }

  const isCancellable = caseDetail.status === 'OPEN' || caseDetail.status === 'IN_PROGRESS';
  const isSimulatable = caseDetail.status === 'OPEN' || caseDetail.status === 'IN_PROGRESS';

  return (
    <div className="space-y-6">
      {/* Top Navigation */}
      <div className="flex items-center justify-between">
        <Link
          to="/recovery-cases"
          className="inline-flex items-center gap-1.5 text-xs text-slate-500 hover:text-slate-900 font-medium transition"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          <span>Back to Recovery Cases</span>
        </Link>
      </div>

      {/* Case Header Banner */}
      <div className="bg-white border border-slate-200 rounded-2xl p-5 sm:p-6 shadow-2xs flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="space-y-1.5">
          <div className="flex flex-wrap items-center gap-2.5">
            <h1 className="text-xl font-bold text-slate-900 font-mono flex items-center gap-2">
              <span className="text-slate-500 font-sans text-sm font-normal">Case:</span>
              {caseDetail.id}
            </h1>
            {getStatusBadge(caseDetail.status)}
            {getPriorityBadge(caseDetail.priority)}
          </div>
          <div className="flex items-center gap-3 text-xs text-slate-500">
            <span>Created {formatDateTime(caseDetail.createdAt)}</span>
            <span>•</span>
            <span className="font-semibold text-slate-900 font-mono">
              {formatCurrency(caseDetail.estimatedRecoverableAmount, caseDetail.currency)}
            </span>
          </div>
        </div>

        <div className="flex items-center gap-2.5 flex-wrap">
          <Button
            variant="outline"
            size="sm"
            onClick={fetchCaseDetail}
            leftIcon={<RefreshCw className="w-3.5 h-3.5" />}
          >
            Refresh
          </Button>

          {isDemoMode && isSimulatable && (
            <Button
              variant="primary"
              size="sm"
              onClick={() => {
                setSimulationStep(0);
                setSimulateModalOpen(true);
              }}
              leftIcon={<Sparkles className="w-3.5 h-3.5" />}
              data-testid="simulate-recovery-btn"
            >
              Simulate Customer Recovery
            </Button>
          )}

          {isDemoMode && !isSimulatable && (
            <Button
              variant="outline"
              size="sm"
              disabled
              title="Recovery simulation unavailable for terminal cases"
              className="opacity-60 cursor-not-allowed text-slate-400"
              leftIcon={<Sparkles className="w-3.5 h-3.5 opacity-40 text-slate-400" />}
              data-testid="simulate-recovery-disabled-btn"
            >
              Simulation Unavailable ({caseDetail.status})
            </Button>
          )}

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

      {/* Interactive Demo Mode Simulator Banner */}
      {isDemoMode && (
        <div
          role="status"
          aria-label="Interactive Demo Simulation Status"
          className="p-4 rounded-xl bg-emerald-50/70 border border-emerald-200 text-xs flex flex-col sm:flex-row sm:items-center justify-between gap-3 shadow-2xs"
        >
          <div className="flex items-start sm:items-center gap-2.5">
            <Sparkles className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5 sm:mt-0" />
            <div>
              <span className="font-semibold text-slate-900">Interactive Recovery Simulator:</span>{' '}
              <span className="text-slate-600">
                {isSimulatable
                  ? `Case is ${caseDetail.status}. Simulate customer accessing recovery link and completing checkout to verify closed-loop reconciliation.`
                  : `Simulation is disabled because case is in terminal status "${caseDetail.status}". Terminal cases cannot be re-recovered.`}
              </span>
            </div>
          </div>

          {isSimulatable ? (
            <Button
              size="sm"
              variant="primary"
              onClick={() => {
                setSimulationStep(0);
                setSimulateModalOpen(true);
              }}
              leftIcon={<PlayCircle className="w-3.5 h-3.5" />}
              className="shrink-0 self-start sm:self-auto"
              data-testid="simulate-recovery-banner-btn"
            >
              Simulate Recovery
            </Button>
          ) : (
            <Badge variant="outline" className="shrink-0 self-start sm:self-auto font-mono text-[10px]">
              Terminal Case ({caseDetail.status})
            </Badge>
          )}
        </div>
      )}

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
        <div className="space-y-3 text-xs text-slate-600">
          <p>
            Cancelling this case will immediately halt all pending and future recovery attempts for transaction{' '}
            <span className="font-mono text-slate-900 font-semibold">
              {caseDetail.payment?.razorpayPaymentId || caseDetail.id}
            </span>.
          </p>
          <p className="text-slate-500">
            Any currently scheduled communications (WhatsApp, Email, SMS) will be marked as{' '}
            <code className="text-amber-800 bg-amber-50 px-1 py-0.5 rounded border border-amber-200 font-mono">SKIPPED</code> and will not be dispatched.
          </p>
        </div>
      </Modal>

      {/* Simulation Progression Modal */}
      <Modal
        isOpen={simulateModalOpen}
        onClose={() => {
          if (!isSimulating) setSimulateModalOpen(false);
        }}
        title="Simulate Customer Recovery"
        description="Simulates the end-to-end recovery journey as experienced by the payer."
        footer={
          simulationStep === 3 ? (
            <Button
              variant="primary"
              size="sm"
              onClick={() => setSimulateModalOpen(false)}
              data-testid="simulation-done-btn"
            >
              Close &amp; Review Case
            </Button>
          ) : (
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setSimulateModalOpen(false)}
                disabled={isSimulating}
              >
                Cancel
              </Button>
              <Button
                variant="primary"
                size="sm"
                onClick={handleExecuteSimulation}
                isLoading={isSimulating}
                disabled={simulationStep > 0}
                leftIcon={<PlayCircle className="w-3.5 h-3.5" />}
                data-testid="confirm-simulation-btn"
              >
                {simulationStep === 0 ? 'Confirm & Simulate' : 'Simulating...'}
              </Button>
            </div>
          )
        }
      >
        <div className="space-y-4 text-xs text-slate-600">
          <div className="flex items-center gap-2 text-slate-700">
            <span className="font-semibold">Target Case:</span>
            <span className="font-mono text-slate-900 font-bold">{caseDetail.id}</span>
          </div>

          <p>
            This demo simulation fires a synthetic recovery event for{' '}
            <span className="font-semibold text-slate-900 font-mono">
              {formatCurrency(caseDetail.estimatedRecoverableAmount, caseDetail.currency)}
            </span>. It simulates:
          </p>

          <ol className="space-y-2.5 list-decimal list-inside text-slate-600 pl-1">
            <li className={simulationStep >= 1 ? 'text-emerald-700 font-medium' : ''}>
              Customer accessed recovery link
            </li>
            <li className={simulationStep >= 2 ? 'text-emerald-700 font-medium' : ''}>
              Customer completed checkout via UPI or Card
            </li>
            <li className={simulationStep >= 3 ? 'text-emerald-700 font-medium' : ''}>
              Webhook reconciled payment, transitions status to{' '}
              <strong className="text-emerald-700 font-mono">RECOVERED</strong>, and emits merchant notifications.
            </li>
          </ol>

          {simulationStep === 3 && (
            <div className="p-3.5 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-900 space-y-1">
              <div className="flex items-center gap-2 font-bold text-xs">
                <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                <span>Payment Recaptured Successfully!</span>
              </div>
              <p className="text-[11px] text-emerald-800">
                The case is now closed with status <code className="font-mono">RECOVERED</code>. All dashboard metrics, charts, and notifications have synchronized.
              </p>
            </div>
          )}
        </div>
      </Modal>

      {/* Main 2-Column Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column (2 Cols): Core Diagnosis, Strategy, Timeline */}
        <div className="lg:col-span-2 space-y-6">
          {/* AI Diagnosis Card */}
          <Card className="shadow-2xs">
            <CardHeader>
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2.5">
                  <div className="p-2 rounded-lg bg-blue-50 text-blue-600">
                    <Cpu className="w-4 h-4" />
                  </div>
                  <div>
                    <CardTitle>AI Diagnosis &amp; Reasoning</CardTitle>
                    <CardDescription>
                      {caseDetail.latestDiagnosis?.modelName || 'Gemini 3.7 Flash'} autonomous analysis of failure telemetry
                    </CardDescription>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  {isDemoMode && (
                    <Badge variant="warning">Simulated AI Data</Badge>
                  )}
                  {caseDetail.latestDiagnosis?.confidenceScore && (
                    <Badge variant="info">
                      {Math.round(Number(caseDetail.latestDiagnosis.confidenceScore) * 100)}% Confidence
                    </Badge>
                  )}
                </div>
              </div>
            </CardHeader>

            <CardContent className="space-y-4 text-xs">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="p-3 rounded-xl bg-slate-50 border border-slate-200 space-y-1">
                  <span className="text-[11px] font-semibold uppercase tracking-wider text-slate-500">
                    Failure Category
                  </span>
                  <p className="font-mono font-bold text-slate-900 text-sm">
                    {caseDetail.failureReasonCategory || 'UNKNOWN'}
                  </p>
                </div>

                <div className="p-3 rounded-xl bg-slate-50 border border-slate-200 space-y-1">
                  <span className="text-[11px] font-semibold uppercase tracking-wider text-slate-500">
                    Recommended Action
                  </span>
                  <p className="font-semibold text-emerald-700 text-sm">
                    {caseDetail.latestDiagnosis?.recommendedAction || 'AUTOMATED_DISPATCH'}
                  </p>
                </div>
              </div>

              {caseDetail.latestDiagnosis?.reasoning && (
                <div className="space-y-1.5">
                  <span className="text-[11px] font-semibold uppercase tracking-wider text-slate-500 block">
                    AI Diagnosis Rationale
                  </span>
                  <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200 text-slate-700 leading-relaxed italic">
                    &ldquo;{caseDetail.latestDiagnosis.reasoning}&rdquo;
                  </div>
                </div>
              )}
            </CardContent>
          </Card>

          {/* Recovery Strategy Card */}
          <Card className="shadow-2xs">
            <CardHeader>
              <div className="flex items-center gap-2.5">
                <div className="p-2 rounded-lg bg-emerald-50 text-emerald-600">
                  <Layers className="w-4 h-4" />
                </div>
                <div>
                  <CardTitle>Recovery Strategy</CardTitle>
                  <CardDescription>
                    Selected channel orchestration and dispatch parameters
                  </CardDescription>
                </div>
              </div>
            </CardHeader>

            <CardContent className="space-y-4 text-xs">
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                <div className="p-3 rounded-xl bg-slate-50 border border-slate-200 space-y-1">
                  <span className="text-[10px] font-semibold uppercase tracking-wider text-slate-500 block">
                    Primary Channel
                  </span>
                  <span className="font-semibold text-slate-900">
                    {caseDetail.latestDiagnosis?.channel || 'WHATSAPP'}
                  </span>
                </div>

                <div className="p-3 rounded-xl bg-slate-50 border border-slate-200 space-y-1">
                  <span className="text-[10px] font-semibold uppercase tracking-wider text-slate-500 block">
                    Priority
                  </span>
                  <span className="font-semibold text-slate-900">
                    Level: {caseDetail.priority}
                  </span>
                </div>

                <div className="p-3 rounded-xl bg-slate-50 border border-slate-200 space-y-1">
                  <span className="text-[10px] font-semibold uppercase tracking-wider text-slate-500 block">
                    Delay: Immediate
                  </span>
                  <span className="font-semibold text-slate-900 font-mono">
                    Immediate (0s)
                  </span>
                </div>

                <div className="p-3 rounded-xl bg-slate-50 border border-slate-200 space-y-1">
                  <span className="text-[10px] font-semibold uppercase tracking-wider text-slate-500 block">
                    Fallback Channel
                  </span>
                  <span className="font-semibold text-slate-900">
                    {caseDetail.attempts?.[0]?.strategySnapshot?.fallbackChannel || 'EMAIL'} (Delay: 2 Hours)
                  </span>
                </div>
              </div>

              {(caseDetail.attempts?.[0]?.strategySnapshot?.recommendedAction || caseDetail.latestDiagnosis?.recommendedAction) && (
                <div className="pt-2.5 border-t border-slate-100 text-xs text-slate-600">
                  <span className="font-semibold text-slate-700">Strategy Action: </span>
                  <span>{caseDetail.attempts?.[0]?.strategySnapshot?.recommendedAction || caseDetail.latestDiagnosis?.recommendedAction}</span>
                </div>
              )}
            </CardContent>
          </Card>

          {/* Recovery Timeline Card */}
          <Card className="shadow-2xs">
            <CardHeader>
              <CardTitle>Execution Timeline &amp; Attempts</CardTitle>
              <CardDescription>
                Chronological event stream from webhook ingestion to final settlement
              </CardDescription>
            </CardHeader>

            <CardContent>
              <RecoveryTimeline
                caseDetail={caseDetail}
                attempts={caseDetail.attempts || []}
              />
            </CardContent>
          </Card>
        </div>

        {/* Right Column (1 Col): Customer, Payment & Gateway Info */}
        <div className="space-y-6">
          {/* Payment Card */}
          <Card className="shadow-2xs">
            <CardHeader>
              <div className="flex items-center gap-2.5">
                <div className="p-2 rounded-lg bg-slate-100 text-slate-700">
                  <CreditCard className="w-4 h-4" />
                </div>
                <div>
                  <CardTitle>Payment Details</CardTitle>
                  <CardDescription>Failed transaction details</CardDescription>
                </div>
              </div>
            </CardHeader>

            <CardContent className="space-y-3 text-xs">
              <div className="flex items-center justify-between py-1.5 border-b border-slate-100">
                <span className="text-slate-500">Amount</span>
                <span className="font-mono font-bold text-slate-900 text-sm">
                  {formatCurrency(caseDetail.estimatedRecoverableAmount, caseDetail.currency)}
                </span>
              </div>

              <div className="flex items-center justify-between py-1.5 border-b border-slate-100">
                <span className="text-slate-500">Payment Status</span>
                <Badge variant={caseDetail.payment?.status === 'CAPTURED' ? 'success' : 'danger'}>
                  {caseDetail.payment?.status || 'FAILED'}
                </Badge>
              </div>

              <div className="flex items-center justify-between py-1.5 border-b border-slate-100">
                <span className="text-slate-500">Payment ID</span>
                <span className="font-mono text-slate-800">
                  {caseDetail.payment?.razorpayPaymentId || caseDetail.payment?.id || '—'}
                </span>
              </div>

              {caseDetail.payment?.errorDescription && (
                <div className="flex items-start justify-between py-1.5 border-b border-slate-100">
                  <span className="text-slate-500">Error</span>
                  <span className="text-slate-700 text-right max-w-[200px]">
                    {caseDetail.payment.errorDescription}
                  </span>
                </div>
              )}

              <div className="flex items-center justify-between py-1.5">
                <span className="text-slate-500">Payment Method</span>
                <span className="font-mono text-slate-800 uppercase">
                  {caseDetail.payment?.method || 'UPI / Card'}
                </span>
              </div>
            </CardContent>
          </Card>

          {/* Customer Profile Card */}
          <Card className="shadow-2xs">
            <CardHeader>
              <div className="flex items-center gap-2.5">
                <div className="p-2 rounded-lg bg-slate-100 text-slate-700">
                  <User className="w-4 h-4" />
                </div>
                <div>
                  <CardTitle>Customer Profile</CardTitle>
                  <CardDescription>Payer contact information</CardDescription>
                </div>
              </div>
            </CardHeader>

            <CardContent className="space-y-3 text-xs">
              <div className="flex items-center justify-between py-1.5 border-b border-slate-100">
                <span className="text-slate-500">Name</span>
                <span className="font-semibold text-slate-900">
                  {caseDetail.customer?.name || 'Anonymous Payer'}
                </span>
              </div>

              <div className="flex items-center justify-between py-1.5 border-b border-slate-100">
                <span className="text-slate-500">Email</span>
                <span className="text-slate-800">
                  {caseDetail.customer?.email || '—'}
                </span>
              </div>

              <div className="flex items-center justify-between py-1.5">
                <span className="text-slate-500">Phone</span>
                <span className="font-mono text-slate-800">
                  {caseDetail.customer?.phone || '—'}
                </span>
              </div>
            </CardContent>
          </Card>

          {/* Attempts Summary Card */}
          <Card className="shadow-2xs">
            <CardHeader>
              <CardTitle>Recovery Attempts</CardTitle>
              <CardDescription>
                {caseDetail.attempts?.length ?? 0} total attempts executed
              </CardDescription>
            </CardHeader>

            <CardContent className="space-y-2.5 text-xs">
              {(!caseDetail.attempts || caseDetail.attempts.length === 0) ? (
                <p className="text-slate-400 italic">No recovery attempts dispatched yet.</p>
              ) : (
                caseDetail.attempts.map((att) => (
                  <div
                    key={att.id}
                    className="p-2.5 rounded-lg bg-slate-50 border border-slate-200 flex items-center justify-between gap-2"
                  >
                    <div>
                      <span className="font-semibold text-slate-800 block">
                        Attempt #{att.attemptNumber} ({att.channel})
                      </span>
                      <span className="text-[10px] text-slate-400 font-mono">
                        {formatDateTime(att.createdAt)}
                      </span>
                    </div>
                    <Badge variant={att.status === 'SUCCESS' ? 'success' : 'default'} className="text-[10px]">
                      {att.status}
                    </Badge>
                  </div>
                ))
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
