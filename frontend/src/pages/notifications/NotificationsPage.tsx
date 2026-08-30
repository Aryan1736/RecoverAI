import { useState, useEffect, useCallback } from 'react';
import {
  CheckCheck,
  RefreshCw,
  Filter,
  Inbox,
} from 'lucide-react';
import { PageHeader } from '../../components/ui/PageHeader';
import { Button } from '../../components/ui/Button';
import { Select } from '../../components/ui/Select';
import { Pagination } from '../../components/ui/Pagination';
import { Skeleton } from '../../components/ui/Skeleton';
import { EmptyState } from '../../components/ui/EmptyState';
import { ErrorState } from '../../components/ui/ErrorState';
import { NotificationItem } from '../../components/notifications/NotificationItem';
import { NotificationDetailModal } from '../../components/notifications/NotificationDetailModal';
import { useToast } from '../../hooks/useToast';
import {
  getNotifications,
  markAsRead,
  markAllAsRead,
} from '../../api/notifications';
import { getDemoNotifications } from '../../api/demo';
import { useDemoMode } from '../../hooks/useDemoMode';
import type {
  NotificationResponseDto,
  MerchantNotificationEvent,
} from '../../types/notifications';

const PAGE_SIZE = 10;

export function NotificationsPage() {
  const { isDemoMode } = useDemoMode();
  const { toast } = useToast();

  const [notifications, setNotifications] = useState<NotificationResponseDto[]>([]);
  const [totalElements, setTotalElements] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(1);
  const [currentPage, setCurrentPage] = useState<number>(0);

  // Filters
  const [unreadOnly, setUnreadOnly] = useState<boolean>(false);
  const [selectedEvent, setSelectedEvent] = useState<string>('ALL');

  // Loading and error states
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Action states
  const [isMarkingAll, setIsMarkingAll] = useState<boolean>(false);
  const [markingId, setMarkingId] = useState<string | null>(null);

  // Detail Modal State
  const [selectedNotification, setSelectedNotification] = useState<NotificationResponseDto | null>(null);

  const fetchNotifications = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const eventFilter =
        selectedEvent !== 'ALL' ? (selectedEvent as MerchantNotificationEvent) : undefined;

      const response = isDemoMode
        ? await getDemoNotifications()
        : await getNotifications({
            page: currentPage,
            size: PAGE_SIZE,
            unreadOnly: unreadOnly ? true : undefined,
            event: eventFilter,
          });

      setNotifications(response.content || []);
      setTotalElements(response.totalElements || 0);
      setTotalPages(response.totalPages || 1);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to load notifications';
      setError(message);
    } finally {
      setIsLoading(false);
    }
  }, [currentPage, unreadOnly, selectedEvent, isDemoMode]);

  useEffect(() => {
    let cancelled = false;

    async function loadData() {
      try {
        const eventFilter =
          selectedEvent !== 'ALL' ? (selectedEvent as MerchantNotificationEvent) : undefined;

        const response = isDemoMode
          ? await getDemoNotifications()
          : await getNotifications({
              page: currentPage,
              size: PAGE_SIZE,
              unreadOnly: unreadOnly ? true : undefined,
              event: eventFilter,
            });

        if (!cancelled) {
          setNotifications(response.content || []);
          setTotalElements(response.totalElements || 0);
          setTotalPages(response.totalPages || 1);
          setError(null);
        }
      } catch (err: unknown) {
        if (!cancelled) {
          const message = err instanceof Error ? err.message : 'Failed to load notifications';
          setError(message);
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    }

    loadData();
    return () => {
      cancelled = true;
    };
  }, [currentPage, unreadOnly, selectedEvent, isDemoMode]);

  const handleMarkAsRead = async (id: string) => {
    setMarkingId(id);
    try {
      if (isDemoMode) {
        setNotifications((prev) =>
          prev.map((item) => (item.id === id ? { ...item, read: true } : item))
        );
        if (selectedNotification && selectedNotification.id === id) {
          setSelectedNotification({ ...selectedNotification, read: true });
        }
        toast.success('Notification marked as read (Simulated)');
        return;
      }
      const updated = await markAsRead(id);
      setNotifications((prev) =>
        prev.map((item) => (item.id === id ? updated : item))
      );
      if (selectedNotification && selectedNotification.id === id) {
        setSelectedNotification(updated);
      }
      toast.success('Notification marked as read');
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to mark notification as read';
      toast.error(message);
    } finally {
      setMarkingId(null);
    }
  };

  const handleMarkAllAsRead = async () => {
    setIsMarkingAll(true);
    try {
      if (isDemoMode) {
        setNotifications((prev) => prev.map((item) => ({ ...item, read: true })));
        toast.success('Marked all notifications as read (Simulated)');
        return;
      }
      const result = await markAllAsRead();
      toast.success(`Marked ${result.markedReadCount} notifications as read`);
      await fetchNotifications();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to mark all as read';
      toast.error(message);
    } finally {
      setIsMarkingAll(false);
    }
  };

  const handleOpenDetail = (item: NotificationResponseDto) => {
    setSelectedNotification(item);
    if (!item.read) {
      // Opportunistically mark as read on view
      handleMarkAsRead(item.id);
    }
  };

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <PageHeader
        title="Notifications"
        description="Monitor system alerts, recovery case completions, and channel delivery events"
        actions={
          <div className="flex items-center gap-2.5">
            <Button
              variant="outline"
              size="sm"
              onClick={fetchNotifications}
              disabled={isLoading}
              title="Refresh notifications"
              leftIcon={<RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin' : ''}`} />}
            >
              Refresh
            </Button>
            <Button
              variant="secondary"
              size="sm"
              onClick={handleMarkAllAsRead}
              isLoading={isMarkingAll}
              disabled={isLoading || notifications.length === 0}
              leftIcon={<CheckCheck className="w-3.5 h-3.5" />}
            >
              Mark All Read
            </Button>
          </div>
        }
      />

      {/* Filter and Control Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-4 rounded-xl bg-slate-900/60 border border-slate-800/80">
        <div className="flex flex-wrap items-center gap-3">
          {/* Event Filter Select */}
          <div className="w-48 sm:w-56">
            <Select
              value={selectedEvent}
              onChange={(e) => {
                setSelectedEvent(e.target.value);
                setCurrentPage(0);
              }}
              options={[
                { value: 'ALL', label: 'All Event Types' },
                { value: 'PAYMENT_RECOVERED', label: 'Payment Recovered' },
                { value: 'CASE_EXHAUSTED', label: 'Case Exhausted' },
                { value: 'HIGH_PRIORITY_FAILURE', label: 'High Priority Failure' },
                { value: 'PROVIDER_DEGRADED', label: 'Provider Degraded' },
              ]}
              aria-label="Filter by event type"
            />
          </div>

          {/* Unread Only Toggle */}
          <button
            type="button"
            onClick={() => {
              setUnreadOnly((prev) => !prev);
              setCurrentPage(0);
            }}
            className={`inline-flex items-center gap-2 px-3.5 py-2 rounded-lg text-xs font-semibold border transition cursor-pointer ${
              unreadOnly
                ? 'bg-indigo-600/20 text-indigo-300 border-indigo-500/50 shadow-sm'
                : 'bg-slate-900 text-slate-400 border-slate-800 hover:text-slate-200 hover:border-slate-700'
            }`}
            aria-pressed={unreadOnly}
          >
            <Filter className="w-3.5 h-3.5" />
            <span>Unread Only</span>
          </button>
        </div>

        {/* Results summary */}
        <div className="text-xs text-slate-400">
          {!isLoading && (
            <span>
              Showing <span className="font-semibold text-slate-200">{notifications.length}</span> of{' '}
              <span className="font-semibold text-slate-200">{totalElements}</span> notifications
            </span>
          )}
        </div>
      </div>

      {/* Main Content Area */}
      {isLoading ? (
        <div className="space-y-3" role="status" aria-label="Loading notifications">
          {Array.from({ length: 5 }).map((_, i) => (
            <div
              key={i}
              className="p-4 sm:p-5 rounded-xl border border-slate-800 bg-slate-950/60 space-y-2.5"
            >
              <div className="flex items-center gap-2">
                <Skeleton className="h-5 w-28 rounded-full" />
                <Skeleton className="h-4 w-16 ml-auto" />
              </div>
              <Skeleton className="h-4 w-3/4" />
              <Skeleton className="h-3 w-1/2" />
            </div>
          ))}
        </div>
      ) : error ? (
        <ErrorState
          title="Failed to Load Notifications"
          message={error}
          onRetry={fetchNotifications}
        />
      ) : notifications.length === 0 ? (
        <EmptyState
          icon={<Inbox className="w-8 h-8 text-slate-500" />}
          title="No Notifications Found"
          description={
            unreadOnly || selectedEvent !== 'ALL'
              ? 'No notifications match your current filter criteria. Try resetting the filters.'
              : 'You are all caught up! No notifications have been received yet.'
          }
          action={
            (unreadOnly || selectedEvent !== 'ALL') ? (
              <Button
                variant="outline"
                size="sm"
                onClick={() => {
                  setUnreadOnly(false);
                  setSelectedEvent('ALL');
                  setCurrentPage(0);
                }}
              >
                Reset Filters
              </Button>
            ) : undefined
          }
        />
      ) : (
        <div className="space-y-3">
          <div className="space-y-2.5" role="feed" aria-label="Notifications list">
            {notifications.map((item) => (
              <NotificationItem
                key={item.id}
                notification={item}
                onSelect={handleOpenDetail}
                onMarkAsRead={handleMarkAsRead}
                isMarkingRead={markingId === item.id}
              />
            ))}
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="pt-4 border-t border-slate-800">
              <Pagination
                page={currentPage}
                totalPages={totalPages}
                totalElements={totalElements}
                size={PAGE_SIZE}
                onPageChange={(p) => setCurrentPage(p)}
              />
            </div>
          )}
        </div>
      )}

      {/* Detail Modal */}
      <NotificationDetailModal
        notification={selectedNotification}
        isOpen={Boolean(selectedNotification)}
        onClose={() => setSelectedNotification(null)}
        onMarkAsRead={handleMarkAsRead}
        isMarkingRead={Boolean(selectedNotification && markingId === selectedNotification.id)}
      />
    </div>
  );
}
