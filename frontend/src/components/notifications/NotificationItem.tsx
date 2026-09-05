import {
  CheckCircle2,
  AlertOctagon,
  AlertTriangle,
  Radio,
  ChevronRight,
  Eye,
  Check,
  Clock,
} from 'lucide-react';
import { Badge, type BadgeVariant } from '../ui/Badge';
import { Button } from '../ui/Button';
import type {
  NotificationResponseDto,
  MerchantNotificationEvent,
  NotificationDeliveryStatus,
} from '../../types/notifications';

export interface NotificationItemProps {
  notification: NotificationResponseDto;
  onSelect: (notification: NotificationResponseDto) => void;
  onMarkAsRead: (id: string) => void;
  isMarkingRead?: boolean;
}

interface EventConfig {
  label: string;
  variant: BadgeVariant;
  icon: typeof CheckCircle2;
  iconContainerClass: string;
  dotColorClass: string;
}

function getEventConfig(event: MerchantNotificationEvent): EventConfig {
  switch (event) {
    case 'PAYMENT_RECOVERED':
      return {
        label: 'Payment Recovered',
        variant: 'success',
        icon: CheckCircle2,
        iconContainerClass: 'bg-[#E8F7F0] text-[#08704F] border-[#0B8F63]/25',
        dotColorClass: 'bg-[#0B8F63]',
      };
    case 'CASE_EXHAUSTED':
      return {
        label: 'Case Exhausted',
        variant: 'danger',
        icon: AlertOctagon,
        iconContainerClass: 'bg-[#FEE2E2] text-[#DC2626] border-[#FECACA]',
        dotColorClass: 'bg-[#DC2626]',
      };
    case 'HIGH_PRIORITY_FAILURE':
      return {
        label: 'High Priority Failure',
        variant: 'warning',
        icon: AlertTriangle,
        iconContainerClass: 'bg-[#FEF3C7] text-[#D97706] border-[#FDE68A]',
        dotColorClass: 'bg-[#D97706]',
      };
    case 'PROVIDER_DEGRADED':
      return {
        label: 'Provider Degraded',
        variant: 'warning',
        icon: Radio,
        iconContainerClass: 'bg-[#FFF7ED] text-[#EA580C] border-[#FED7AA]',
        dotColorClass: 'bg-[#EA580C]',
      };
    default:
      return {
        label: event,
        variant: 'default',
        icon: AlertTriangle,
        iconContainerClass: 'bg-[#F1F4F2] text-[#667085] border-[#E5E9E6]',
        dotColorClass: 'bg-[#98A2B3]',
      };
  }
}

function formatRelativeTime(isoString: string): string {
  try {
    const past = new Date(isoString).getTime();
    if (isNaN(past)) return isoString;
    const diffSec = Math.floor((Date.now() - past) / 1000);
    if (diffSec < 60) return 'just now';
    const diffMin = Math.floor(diffSec / 60);
    if (diffMin < 60) return `${diffMin}m ago`;
    const diffHour = Math.floor(diffMin / 60);
    if (diffHour < 24) return `${diffHour}h ago`;
    const diffDay = Math.floor(diffHour / 24);
    if (diffDay < 30) return `${diffDay}d ago`;
    return new Date(isoString).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
    });
  } catch {
    return isoString;
  }
}

function getDeliveryStatusBadge(status: NotificationDeliveryStatus) {
  switch (status) {
    case 'DELIVERED':
    case 'SENT':
      return {
        dot: 'bg-[#0B8F63]',
        pill: 'bg-[#E8F7F0] text-[#08704F] border-[#0B8F63]/25',
      };
    case 'FAILED':
      return {
        dot: 'bg-[#DC2626]',
        pill: 'bg-[#FEE2E2] text-[#DC2626] border-[#FECACA]',
      };
    case 'RETRYING':
    case 'PENDING':
      return {
        dot: 'bg-[#D97706]',
        pill: 'bg-[#FEF3C7] text-[#D97706] border-[#FDE68A]',
      };
    case 'SKIPPED':
    default:
      return {
        dot: 'bg-[#98A2B3]',
        pill: 'bg-[#F1F4F2] text-[#667085] border-[#E5E9E6]',
      };
  }
}

export function NotificationItem({
  notification,
  onSelect,
  onMarkAsRead,
  isMarkingRead = false,
}: NotificationItemProps) {
  const isUnread = !notification.read;
  const eventConfig = getEventConfig(notification.eventType);
  const Icon = eventConfig.icon;

  return (
    <div
      onClick={() => onSelect(notification)}
      className={`group relative p-4 sm:p-4.5 rounded-xl border transition-all duration-250 cursor-pointer ${
        isUnread
          ? 'bg-[#F5FBF8] hover:bg-[#EDF8F3] border-[#A7E3CB] shadow-2xs hover:border-[#0B8F63]/50 -translate-y-0 hover:-translate-y-0.5'
          : 'bg-white hover:bg-[#F9FAF9] border-[#E5E9E6] hover:border-[#D1D7D3] -translate-y-0 hover:-translate-y-0.5 hover:shadow-2xs'
      }`}
      role="article"
      aria-label={`Notification: ${notification.title}`}
    >
      <div className="flex items-start gap-3.5 sm:gap-4">
        {/* Left: Event Icon container with subtle pulse indicator if unread */}
        <div className="relative shrink-0 pt-0.5">
          <div
            className={`w-10 h-10 rounded-xl flex items-center justify-center border shadow-2xs transition-colors duration-200 ${eventConfig.iconContainerClass}`}
          >
            <Icon className="w-5 h-5 shrink-0" />
          </div>
          {isUnread && (
            <span
              className="absolute -top-1 -right-1 w-2.5 h-2.5 rounded-full bg-[#0B8F63] ring-2 ring-white pulse-subtle"
              aria-hidden="true"
            />
          )}
        </div>

        {/* Center: Main Content Area */}
        <div className="flex-1 min-w-0">
          {/* Top metadata row */}
          <div className="flex flex-wrap items-center gap-2 mb-1.5">
            <Badge variant={eventConfig.variant} dot={isUnread}>
              {eventConfig.label}
            </Badge>

            {isUnread && (
              <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold bg-[#E8F7F0] text-[#08704F] border border-[#0B8F63]/30 uppercase tracking-wider">
                Unread
              </span>
            )}

            {notification.recoveryCaseId && (
              <span className="inline-flex items-center gap-1 font-mono text-[11px] font-medium text-[#667085] bg-[#F1F4F2] px-2 py-0.5 rounded border border-[#E5E9E6]">
                Case: <span className="text-[#111318] font-semibold">{notification.recoveryCaseId}</span>
              </span>
            )}

            <span className="text-xs text-[#98A2B3] ml-auto flex items-center gap-1 shrink-0 font-inter">
              <Clock className="w-3 h-3 text-[#98A2B3]" />
              {formatRelativeTime(notification.createdAt)}
            </span>
          </div>

          {/* Notification Title */}
          <h4
            className={`text-sm font-semibold truncate leading-tight font-inter ${
              isUnread ? 'text-[#111318]' : 'text-[#344054]'
            }`}
          >
            {notification.title}
          </h4>

          {/* Short description */}
          <p className="text-xs text-[#667085] mt-1 line-clamp-2 leading-relaxed font-inter">
            {notification.message}
          </p>

          {/* Delivery Channel Badges */}
          {notification.deliveries && notification.deliveries.length > 0 && (
            <div className="mt-2.5 flex flex-wrap items-center gap-1.5">
              <span className="text-[11px] text-[#98A2B3] font-inter">Delivered via:</span>
              {notification.deliveries.map((delivery) => {
                const badgeInfo = getDeliveryStatusBadge(delivery.status);
                return (
                  <span
                    key={delivery.id}
                    className={`inline-flex items-center gap-1 text-[10px] px-2 py-0.5 rounded font-mono border ${badgeInfo.pill}`}
                    title={`${delivery.channel} (${delivery.status}) via ${delivery.provider}`}
                  >
                    <span className={`w-1.5 h-1.5 rounded-full ${badgeInfo.dot}`} aria-hidden="true" />
                    <span>{delivery.channel}</span>
                  </span>
                );
              })}
            </div>
          )}
        </div>

        {/* Right: Actions */}
        <div className="shrink-0 flex items-center gap-1.5 self-center sm:self-start sm:pt-1">
          {isUnread && (
            <Button
              size="sm"
              variant="outline"
              onClick={(e) => {
                e.stopPropagation();
                onMarkAsRead(notification.id);
              }}
              isLoading={isMarkingRead}
              title="Mark as read"
              aria-label={`Mark "${notification.title}" as read`}
              className="text-xs py-1 px-2.5 h-auto bg-white hover:bg-[#E8F7F0] border-[#E5E9E6] hover:border-[#0B8F63]/40 text-[#111318] hover:text-[#08704F] transition shadow-2xs cursor-pointer"
            >
              <Check className="w-3.5 h-3.5 mr-1 text-[#0B8F63]" />
              <span className="hidden sm:inline">Mark read</span>
            </Button>
          )}

          <Button
            size="sm"
            variant="ghost"
            onClick={(e) => {
              e.stopPropagation();
              onSelect(notification);
            }}
            title="View details"
            aria-label={`View details for ${notification.title}`}
            className="p-1.5 h-auto text-[#667085] hover:text-[#111318] hover:bg-[#F1F4F2] transition cursor-pointer"
          >
            <Eye className="w-3.5 h-3.5" />
            <ChevronRight className="w-3.5 h-3.5 ml-0.5 text-[#98A2B3] group-hover:text-[#111318] group-hover:translate-x-0.5 transition-transform duration-200" />
          </Button>
        </div>
      </div>
    </div>
  );
}
