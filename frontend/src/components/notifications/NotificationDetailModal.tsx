import { Link } from 'react-router-dom';
import {
  ExternalLink,
  CheckCircle2,
  AlertOctagon,
  AlertTriangle,
  Radio,
  Clock,
  Send,
  Check,
} from 'lucide-react';
import { Modal } from '../ui/Modal';
import { Badge, type BadgeVariant } from '../ui/Badge';
import { Button } from '../ui/Button';
import type {
  NotificationResponseDto,
  MerchantNotificationEvent,
} from '../../types/notifications';

export interface NotificationDetailModalProps {
  notification: NotificationResponseDto | null;
  isOpen: boolean;
  onClose: () => void;
  onMarkAsRead?: (id: string) => void;
  isMarkingRead?: boolean;
}

function getEventConfig(event: MerchantNotificationEvent): {
  label: string;
  variant: BadgeVariant;
  icon: typeof CheckCircle2;
} {
  switch (event) {
    case 'PAYMENT_RECOVERED':
      return { label: 'Payment Recovered', variant: 'success', icon: CheckCircle2 };
    case 'CASE_EXHAUSTED':
      return { label: 'Case Exhausted', variant: 'danger', icon: AlertOctagon };
    case 'HIGH_PRIORITY_FAILURE':
      return { label: 'High Priority Failure', variant: 'warning', icon: AlertTriangle };
    case 'PROVIDER_DEGRADED':
      return { label: 'Provider Degraded', variant: 'warning', icon: Radio };
    default:
      return { label: event, variant: 'default', icon: AlertTriangle };
  }
}

function formatFullDate(isoString?: string | null): string {
  if (!isoString) return '—';
  try {
    const d = new Date(isoString);
    if (isNaN(d.getTime())) return isoString;
    return d.toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false,
    });
  } catch {
    return isoString;
  }
}

export function NotificationDetailModal({
  notification,
  isOpen,
  onClose,
  onMarkAsRead,
  isMarkingRead = false,
}: NotificationDetailModalProps) {
  if (!notification) return null;

  const eventConfig = getEventConfig(notification.eventType);
  const isUnread = !notification.read;

  let parsedMetadata: Record<string, unknown> | null = null;
  if (notification.metadata) {
    try {
      parsedMetadata = JSON.parse(notification.metadata);
    } catch {
      // Keep as string if not JSON
    }
  }

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={notification.title}
      size="lg"
      footer={
        <div className="flex items-center justify-between w-full">
          <div className="flex items-center gap-2">
            {isUnread && onMarkAsRead && (
              <Button
                size="sm"
                variant="outline"
                onClick={() => onMarkAsRead(notification.id)}
                isLoading={isMarkingRead}
                leftIcon={<Check className="w-4 h-4" />}
              >
                Mark as Read
              </Button>
            )}
          </div>
          <Button size="sm" variant="secondary" onClick={onClose}>
            Close
          </Button>
        </div>
      }
    >
      <div className="space-y-6">
        {/* Header Metadata Ribbon */}
        <div className="flex flex-wrap items-center justify-between gap-3 p-3.5 rounded-xl bg-slate-50 border border-slate-200">
          <div className="flex items-center gap-2.5">
            <Badge variant={eventConfig.variant}>
              {eventConfig.label}
            </Badge>
            <Badge variant={isUnread ? 'warning' : 'default'} dot={isUnread}>
              {notification.status}
            </Badge>
          </div>

          <div className="flex items-center gap-1.5 text-xs text-slate-500 font-mono">
            <Clock className="w-3.5 h-3.5 text-slate-400" />
            <span>{formatFullDate(notification.createdAt)}</span>
          </div>
        </div>

        {/* Message Body */}
        <div className="space-y-2">
          <label className="text-xs font-semibold uppercase tracking-wider text-slate-700">
            Notification Content
          </label>
          <div className="p-4 rounded-xl bg-slate-50 border border-slate-200 text-sm text-slate-800 leading-relaxed whitespace-pre-wrap font-sans">
            {notification.message}
          </div>
        </div>

        {/* Linked Recovery Case */}
        {notification.recoveryCaseId && (
          <div className="space-y-2">
            <label className="text-xs font-semibold uppercase tracking-wider text-slate-700">
              Associated Recovery Case
            </label>
            <div className="flex items-center justify-between p-3 rounded-xl bg-slate-50 border border-slate-200">
              <span className="font-mono text-xs text-slate-800 font-semibold">
                Case ID: {notification.recoveryCaseId}
              </span>
              <Link
                to={`/recovery-cases/${notification.recoveryCaseId}`}
                onClick={onClose}
                className="inline-flex items-center gap-1.5 text-xs font-semibold text-emerald-600 hover:text-emerald-700 hover:underline"
              >
                View Case Details
                <ExternalLink className="w-3.5 h-3.5" />
              </Link>
            </div>
          </div>
        )}

        {/* Deliveries Breakdown */}
        <div className="space-y-2">
          <label className="text-xs font-semibold uppercase tracking-wider text-slate-700 flex items-center gap-1.5">
            <Send className="w-3.5 h-3.5 text-emerald-600" />
            Channel Deliveries ({notification.deliveries.length})
          </label>

          {notification.deliveries.length === 0 ? (
            <p className="text-xs text-slate-500 italic p-3 bg-slate-50 rounded-lg border border-slate-200">
              No delivery records available.
            </p>
          ) : (
            <div className="space-y-2">
              {notification.deliveries.map((delivery) => (
                <div
                  key={delivery.id}
                  className="p-3 rounded-xl bg-slate-50 border border-slate-200 text-xs space-y-2"
                >
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <div className="flex items-center gap-2">
                      <span className="font-semibold text-slate-900 font-mono">
                        {delivery.channel}
                      </span>
                      <span className="text-slate-400">•</span>
                      <span className="text-slate-600">
                        Provider: <span className="text-slate-900 font-mono">{delivery.provider}</span>
                      </span>
                    </div>

                    <div className="flex items-center gap-2">
                      <Badge
                        variant={
                          delivery.status === 'DELIVERED' || delivery.status === 'SENT'
                            ? 'success'
                            : delivery.status === 'FAILED'
                            ? 'danger'
                            : 'default'
                        }
                      >
                        {delivery.status}
                      </Badge>
                      {delivery.retryCount > 0 && (
                        <span className="text-[11px] text-slate-500 font-mono">
                          Retries: {delivery.retryCount}
                        </span>
                      )}
                    </div>
                  </div>

                  <div className="flex flex-wrap items-center gap-4 text-[11px] text-slate-500 font-mono">
                    {delivery.attemptedAt && (
                      <span>Attempted: {formatFullDate(delivery.attemptedAt)}</span>
                    )}
                    {delivery.deliveredAt && (
                      <span>Delivered: {formatFullDate(delivery.deliveredAt)}</span>
                    )}
                  </div>

                  {delivery.errorMessage && (
                    <div className="p-2 rounded bg-rose-50 border border-rose-200 text-rose-800 text-[11px] font-mono">
                      {delivery.errorCode && (
                        <strong className="mr-1">[{delivery.errorCode}]</strong>
                      )}
                      {delivery.errorMessage}
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Metadata section */}
        {notification.metadata && (
          <div className="space-y-2">
            <label className="text-xs font-semibold uppercase tracking-wider text-slate-700">
              Payload Metadata
            </label>
            <pre className="p-3 rounded-xl bg-slate-50 border border-slate-200 text-[11px] font-mono text-slate-800 overflow-x-auto">
              {parsedMetadata ? JSON.stringify(parsedMetadata, null, 2) : notification.metadata}
            </pre>
          </div>
        )}
      </div>
    </Modal>
  );
}
