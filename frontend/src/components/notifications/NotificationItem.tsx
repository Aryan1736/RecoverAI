import {
  CheckCircle2,
  AlertOctagon,
  AlertTriangle,
  Radio,
  ChevronRight,
  Eye,
  Check,
} from 'lucide-react';
import { Badge, type BadgeVariant } from '../ui/Badge';
import { Button } from '../ui/Button';
import type {
  NotificationResponseDto,
  MerchantNotificationEvent,
} from '../../types/notifications';

export interface NotificationItemProps {
  notification: NotificationResponseDto;
  onSelect: (notification: NotificationResponseDto) => void;
  onMarkAsRead: (id: string) => void;
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
      className={`group relative p-4 sm:p-5 rounded-xl border transition-all duration-150 cursor-pointer ${
        isUnread
          ? 'bg-emerald-50/20 hover:bg-emerald-50/40 border-emerald-300 shadow-2xs'
          : 'bg-white hover:bg-slate-50 border-slate-200'
      }`}
      role="article"
      aria-label={`Notification: ${notification.title}`}
    >
      <div className="flex items-start gap-3.5">
        {/* Event Icon or Unread Indicator */}
        <div
          className={`shrink-0 w-9 h-9 rounded-xl flex items-center justify-center border transition ${
            isUnread
              ? 'bg-emerald-50 border-emerald-200 text-emerald-600'
              : 'bg-slate-50 border-slate-200 text-slate-400'
          }`}
        >
          <Icon className="w-4 h-4" />
        </div>

        {/* Main Content Area */}
        <div className="flex-1 min-w-0">
          <div className="flex flex-wrap items-center gap-2 mb-1">
            <Badge variant={eventConfig.variant} dot={isUnread}>
              {eventConfig.label}
            </Badge>

            {isUnread && (
              <span className="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-semibold bg-emerald-100 text-emerald-800 uppercase tracking-wider">
                Unread
              </span>
            )}

            <span className="text-xs text-slate-400 ml-auto">
              {formatRelativeTime(notification.createdAt)}
            </span>
          </div>

          <h4
            className={`text-sm font-semibold truncate ${
              isUnread ? 'text-slate-900' : 'text-slate-700'
            }`}
          >
            {notification.title}
          </h4>

          <p className="text-xs text-slate-500 mt-1 line-clamp-2 leading-relaxed">
            {notification.message}
          </p>

          {/* Delivery Channel Badges */}
          {notification.deliveries && notification.deliveries.length > 0 && (
            <div className="mt-2.5 flex flex-wrap items-center gap-1.5">
              <span className="text-[11px] text-slate-400">Delivered via:</span>
              {notification.deliveries.map((delivery) => (
                <span
                  key={delivery.id}
                  className={`text-[10px] px-2 py-0.5 rounded font-mono border ${
                    delivery.status === 'DELIVERED' || delivery.status === 'SENT'
                      ? 'bg-emerald-50 text-emerald-800 border-emerald-200'
                      : delivery.status === 'FAILED'
                      ? 'bg-rose-50 text-rose-800 border-rose-200'
                      : 'bg-slate-100 text-slate-600 border-slate-200'
                  }`}
                  title={`${delivery.channel} (${delivery.status}) via ${delivery.provider}`}
                >
                  {delivery.channel}
                </span>
              ))}
            </div>
          )}
        </div>

        {/* Action Controls */}
        <div className="shrink-0 flex items-center gap-1.5 self-center">
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
              className="text-xs py-1 px-2.5 h-auto"
            >
              <Check className="w-3.5 h-3.5 mr-1" />
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
            className="p-1.5 h-auto"
          >
            <Eye className="w-3.5 h-3.5" />
            <ChevronRight className="w-3.5 h-3.5 ml-0.5 text-slate-400" />
          </Button>
        </div>
      </div>
    </div>
  );
}
