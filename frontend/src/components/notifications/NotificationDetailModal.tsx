import { Link } from 'react-router-dom';
import {
  ArrowRight,
  CheckCircle2,
  AlertOctagon,
  AlertTriangle,
  Radio,
  Clock,
  Send,
  Check,
  Hash,
  Activity,
} from 'lucide-react';
import { Modal } from '../ui/Modal';
import { Badge, type BadgeVariant } from '../ui/Badge';
import { Button } from '../ui/Button';
import type {
  NotificationResponseDto,
  MerchantNotificationEvent,
  NotificationDeliveryStatus,
} from '../../types/notifications';

export interface NotificationDetailModalProps {
  notification: NotificationResponseDto | null;
  isOpen: boolean;
  onClose: () => void;
  onMarkAsRead?: (id: string) => void;
  isMarkingRead?: boolean;
}

interface EventConfig {
  label: string;
  variant: BadgeVariant;
  icon: typeof CheckCircle2;
  containerClass: string;
}

function getEventConfig(event: MerchantNotificationEvent): EventConfig {
  switch (event) {
    case 'PAYMENT_RECOVERED':
      return {
        label: 'Payment Recovered',
        variant: 'success',
        icon: CheckCircle2,
        containerClass: 'bg-[#E8F7F0] text-[#08704F] border-[#0B8F63]/25',
      };
    case 'CASE_EXHAUSTED':
      return {
        label: 'Case Exhausted',
        variant: 'danger',
        icon: AlertOctagon,
        containerClass: 'bg-[#FEE2E2] text-[#DC2626] border-[#FECACA]',
      };
    case 'HIGH_PRIORITY_FAILURE':
      return {
        label: 'High Priority Failure',
        variant: 'warning',
        icon: AlertTriangle,
        containerClass: 'bg-[#FEF3C7] text-[#D97706] border-[#FDE68A]',
      };
    case 'PROVIDER_DEGRADED':
      return {
        label: 'Provider Degraded',
        variant: 'warning',
        icon: Radio,
        containerClass: 'bg-[#FFF7ED] text-[#EA580C] border-[#FED7AA]',
      };
    default:
      return {
        label: event,
        variant: 'default',
        icon: AlertTriangle,
        containerClass: 'bg-[#F1F4F2] text-[#667085] border-[#E5E9E6]',
      };
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

function getDeliveryBadgeVariant(status: NotificationDeliveryStatus): BadgeVariant {
  switch (status) {
    case 'DELIVERED':
    case 'SENT':
      return 'success';
    case 'FAILED':
      return 'danger';
    case 'RETRYING':
    case 'PENDING':
      return 'warning';
    default:
      return 'default';
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
      // Keep as raw string
    }
  }

  // Extract payment ID if present in metadata
  const paymentId =
    parsedMetadata && typeof parsedMetadata.paymentId === 'string'
      ? parsedMetadata.paymentId
      : parsedMetadata && typeof parsedMetadata.payment_id === 'string'
      ? parsedMetadata.payment_id
      : null;

  const primaryChannel =
    notification.deliveries && notification.deliveries.length > 0
      ? notification.deliveries[0].channel
      : 'IN_APP';

  const primaryDeliveryStatus =
    notification.deliveries && notification.deliveries.length > 0
      ? notification.deliveries[0].status
      : notification.status;

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={notification.title}
      size="lg"
      footer={
        <div className="flex flex-wrap items-center justify-between gap-3 w-full font-inter">
          <div className="flex items-center gap-2">
            {isUnread && onMarkAsRead && (
              <Button
                size="sm"
                variant="outline"
                onClick={() => onMarkAsRead(notification.id)}
                isLoading={isMarkingRead}
                leftIcon={<Check className="w-3.5 h-3.5 text-[#0B8F63]" />}
                className="bg-white hover:bg-[#E8F7F0] border-[#E5E9E6] hover:border-[#0B8F63]/30 text-[#111318] text-xs font-semibold cursor-pointer"
              >
                Mark as Read
              </Button>
            )}
          </div>
          <div className="flex items-center gap-2">
            <Button
              size="sm"
              variant="secondary"
              onClick={onClose}
              className="bg-[#F1F4F2] hover:bg-[#E5E9E6] text-[#111318] text-xs font-medium cursor-pointer"
            >
              Close
            </Button>
          </div>
        </div>
      }
    >
      <div className="space-y-5 font-inter text-[#111318]">
        {/* Top Header Metadata Ribbon */}
        <div className="flex flex-wrap items-center justify-between gap-3 p-3.5 rounded-xl bg-[#F7F8F6] border border-[#E5E9E6]">
          <div className="flex flex-wrap items-center gap-2">
            <Badge variant={eventConfig.variant}>
              {eventConfig.label}
            </Badge>
            <Badge variant={isUnread ? 'warning' : 'default'} dot={isUnread}>
              {notification.status}
            </Badge>
          </div>

          <div className="flex items-center gap-1.5 text-xs text-[#667085] font-mono">
            <Clock className="w-3.5 h-3.5 text-[#98A2B3]" />
            <span>{formatFullDate(notification.createdAt)}</span>
          </div>
        </div>

        {/* Section 1: EVENT DETAILS Key-Value Grid */}
        <div className="space-y-2">
          <span className="text-[11px] font-bold uppercase tracking-[0.08em] text-[#667085] block">
            Event Details
          </span>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5 p-3 rounded-xl bg-[#FFFFFF] border border-[#E5E9E6] shadow-2xs">
            <div className="space-y-0.5">
              <span className="text-[10px] uppercase font-semibold text-[#98A2B3] tracking-wider block">
                Case
              </span>
              <span className="text-xs font-mono font-semibold text-[#111318] truncate block">
                {notification.recoveryCaseId || '—'}
              </span>
            </div>

            <div className="space-y-0.5">
              <span className="text-[10px] uppercase font-semibold text-[#98A2B3] tracking-wider block">
                Payment
              </span>
              <span className="text-xs font-mono font-semibold text-[#111318] truncate block">
                {paymentId || '—'}
              </span>
            </div>

            <div className="space-y-0.5">
              <span className="text-[10px] uppercase font-semibold text-[#98A2B3] tracking-wider block">
                Channel
              </span>
              <span className="text-xs font-mono font-semibold text-[#111318] block">
                {primaryChannel}
              </span>
            </div>

            <div className="space-y-0.5">
              <span className="text-[10px] uppercase font-semibold text-[#98A2B3] tracking-wider block">
                Status
              </span>
              <span className="text-xs font-semibold text-[#08704F] block">
                {primaryDeliveryStatus}
              </span>
            </div>
          </div>
        </div>

        {/* Section 2: Associated Recovery Case Banner */}
        {notification.recoveryCaseId && (
          <div className="space-y-2">
            <label className="text-[11px] font-bold uppercase tracking-[0.08em] text-[#667085] block">
              Associated Recovery Case
            </label>
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 p-3.5 rounded-xl bg-[#E8F7F0]/40 border border-[#0B8F63]/30">
              <div className="flex items-center gap-2">
                <div className="w-7 h-7 rounded-lg bg-[#E8F7F0] border border-[#0B8F63]/30 flex items-center justify-center text-[#08704F] shrink-0">
                  <Activity className="w-4 h-4" />
                </div>
                <span className="font-mono text-xs text-[#111318] font-semibold">
                  Case ID: {notification.recoveryCaseId}
                </span>
              </div>
              <Link
                to={`/recovery-cases/${notification.recoveryCaseId}`}
                onClick={onClose}
                className="inline-flex items-center gap-1.5 text-xs font-bold text-[#08704F] hover:text-[#0B8F63] hover:underline"
              >
                <span>VIEW RECOVERY CASE</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </Link>
            </div>
          </div>
        )}

        {/* Section 3: Notification Message */}
        <div className="space-y-2">
          <label className="text-[11px] font-bold uppercase tracking-[0.08em] text-[#667085] block">
            Message
          </label>
          <div className="p-4 rounded-xl bg-[#F7F8F6] border border-[#E5E9E6] text-xs sm:text-sm text-[#111318] leading-relaxed whitespace-pre-wrap font-inter">
            {notification.message}
          </div>
        </div>

        {/* Section 4: Delivery Audit Feed */}
        <div className="space-y-2">
          <label className="text-[11px] font-bold uppercase tracking-[0.08em] text-[#667085] flex items-center gap-1.5">
            <Send className="w-3.5 h-3.5 text-[#0B8F63]" />
            Channel Deliveries ({notification.deliveries.length})
          </label>

          {notification.deliveries.length === 0 ? (
            <p className="text-xs text-[#98A2B3] italic p-3 bg-[#F7F8F6] rounded-xl border border-[#E5E9E6]">
              No delivery records available.
            </p>
          ) : (
            <div className="space-y-2">
              {notification.deliveries.map((delivery) => (
                <div
                  key={delivery.id}
                  className="p-3.5 rounded-xl bg-[#F7F8F6] border border-[#E5E9E6] text-xs space-y-2"
                >
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <div className="flex items-center gap-2">
                      <span className="font-semibold text-[#111318] font-mono">
                        {delivery.channel}
                      </span>
                      <span className="text-[#D1D7D3]">•</span>
                      <span className="text-[#667085]">
                        Provider: <span className="text-[#111318] font-mono font-medium">{delivery.provider}</span>
                      </span>
                    </div>

                    <div className="flex items-center gap-2">
                      <Badge variant={getDeliveryBadgeVariant(delivery.status)}>
                        {delivery.status}
                      </Badge>
                      {delivery.retryCount > 0 && (
                        <span className="text-[11px] text-[#667085] font-mono">
                          Retries: {delivery.retryCount}
                        </span>
                      )}
                    </div>
                  </div>

                  <div className="flex flex-wrap items-center gap-4 text-[11px] text-[#667085] font-mono">
                    {delivery.attemptedAt && (
                      <span>Attempted: {formatFullDate(delivery.attemptedAt)}</span>
                    )}
                    {delivery.deliveredAt && (
                      <span>Delivered: {formatFullDate(delivery.deliveredAt)}</span>
                    )}
                  </div>

                  {delivery.errorMessage && (
                    <div className="p-2.5 rounded-lg bg-[#FEE2E2] border border-[#FECACA] text-[#DC2626] text-[11px] font-mono">
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

        {/* Section 5: Technical Payload */}
        {notification.metadata && (
          <div className="space-y-2">
            <label className="text-[11px] font-bold uppercase tracking-[0.08em] text-[#667085] flex items-center gap-1.5">
              <Hash className="w-3.5 h-3.5 text-[#98A2B3]" />
              Technical Payload
            </label>
            <pre className="p-3.5 rounded-xl bg-[#F7F8F6] border border-[#E5E9E6] text-[11px] font-mono text-[#111318] overflow-x-auto max-h-48 leading-relaxed">
              {parsedMetadata ? JSON.stringify(parsedMetadata, null, 2) : notification.metadata}
            </pre>
          </div>
        )}
      </div>
    </Modal>
  );
}
