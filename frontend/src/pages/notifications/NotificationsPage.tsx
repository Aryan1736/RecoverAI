import { useState, useEffect, useCallback, useMemo } from 'react';
import {
  RefreshCw,
  CheckCheck,
  Filter,
  Inbox,
  CheckCircle2,
  X,
} from 'lucide-react';
import {
  getNotifications,
  markAsRead,
  markAllAsRead,
} from '../../api/notifications';
import {
  getDemoNotifications,
  markDemoNotificationRead,
  markAllDemoNotificationsRead,
} from '../../api/demo';
import { useDemoMode } from '../../hooks/useDemoMode';
import type {
  NotificationResponseDto,
  MerchantNotificationEvent,
} from '../../types/notifications';
import { Button } from '../../components/ui/Button';
import { Select } from '../../components/ui/Select';
import { Pagination } from '../../components/ui/Pagination';
import { EmptyState } from '../../components/ui/EmptyState';
import { ErrorState } from '../../components/ui/ErrorState';
import { Skeleton } from '../../components/ui/Skeleton';
import { NotificationItem } from '../../components/notifications/NotificationItem';
import { NotificationDetailModal } from '../../components/notifications/NotificationDetailModal';
import { Footer } from '../../components/layout/Footer';
import { useToast } from '../../hooks/useToast';

const PAGE_SIZE = 15;

export function NotificationsPage() {
  const { isDemoMode } = useDemoMode();
  const { toast } = useToast();

  const [notifications, setNotifications] = useState<NotificationResponseDto[]>([]);
  const [totalElements, setTotalElements] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(1);
  const [currentPage, setCurrentPage] = useState<number>(0);

  const [unreadOnly, setUnreadOnly] = useState<boolean>(false);
  const [selectedEvent, setSelectedEvent] = useState<string>('ALL');

  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const [selectedNotification, setSelectedNotification] =
    useState<NotificationResponseDto | null>(null);
  const [markingId, setMarkingId] = useState<string | null>(null);
  const [isMarkingAll, setIsMarkingAll] = useState<boolean>(false);

  const fetchNotifications = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const eventFilter =
        selectedEvent !== 'ALL' ? (selectedEvent as MerchantNotificationEvent) : undefined;

      const response = isDemoMode
        ? await getDemoNotifications({
            page: currentPage,
            size: PAGE_SIZE,
            unreadOnly: unreadOnly ? true : undefined,
            event: eventFilter,
          })
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
          ? await getDemoNotifications({
              page: currentPage,
              size: PAGE_SIZE,
              unreadOnly: unreadOnly ? true : undefined,
              event: eventFilter,
            })
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
        const updated = await markDemoNotificationRead(id);
        setNotifications((prev) =>
          prev.map((item) => (item.id === id ? updated : item))
        );
        if (selectedNotification && selectedNotification.id === id) {
          setSelectedNotification(updated);
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
        await markAllDemoNotificationsRead();
        setNotifications((prev) =>
          prev.map((item) => ({ ...item, read: true, status: 'READ' }))
        );
        toast.success('All notifications marked as read (Simulated)');
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

  // Operational metrics calculated dynamically from active notifications
  const unreadCount = useMemo(
    () => notifications.filter((n) => !n.read).length,
    [notifications]
  );

  const recoveryEventsCount = useMemo(
    () =>
      notifications.filter(
        (n) => n.eventType === 'PAYMENT_RECOVERED' || n.eventType === 'CASE_EXHAUSTED'
      ).length,
    [notifications]
  );

  const systemAlertsCount = useMemo(
    () =>
      notifications.filter(
        (n) => n.eventType === 'HIGH_PRIORITY_FAILURE' || n.eventType === 'PROVIDER_DEGRADED'
      ).length,
    [notifications]
  );

  return (
    <div className="space-y-6 animate-console-fade-in font-inter">
      {/* ==================================================
          1. REFINED OPERATIONS HEADER
          ================================================== */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between pt-1 delay-0">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-[#0B8F63] pulse-subtle" />
            <span className="text-[11px] font-bold uppercase tracking-[0.08em] text-[#08704F]">
              Notification Center
            </span>
          </div>
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="font-space-grotesk font-bold text-2xl sm:text-3xl text-[#111318] tracking-tight">
              Notifications
            </h1>

            {/* Unread Status Pill near heading */}
            {!isLoading && (
              unreadCount > 0 ? (
                <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-[#E8F7F0] border border-[#0B8F63]/30 text-xs font-semibold text-[#08704F] shadow-2xs">
                  <span className="w-1.5 h-1.5 rounded-full bg-[#0B8F63] pulse-subtle" />
                  <span className="font-space-grotesk font-bold tabular-nums">{unreadCount}</span>
                  <span>unread</span>
                </span>
              ) : (
                <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-[#F1F4F2] border border-[#E5E9E6] text-xs font-medium text-[#667085] shadow-2xs">
                  <CheckCircle2 className="w-3.5 h-3.5 text-[#0B8F63]" />
                  <span className="text-[#111318] font-semibold">All caught up</span>
                </span>
              )
            )}
          </div>
          <p className="text-xs sm:text-sm text-[#667085] leading-relaxed max-w-2xl">
            Monitor recovery events, system alerts, and delivery activity in real time.
          </p>
        </div>

        {/* Compact Actions on the Right */}
        <div className="flex items-center gap-2.5 self-start sm:self-center">
          <Button
            variant="outline"
            size="sm"
            onClick={fetchNotifications}
            disabled={isLoading}
            title="Refresh notifications"
            leftIcon={
              <RefreshCw
                className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin text-[#0B8F63]' : 'text-[#667085]'}`}
              />
            }
            className="bg-white border-[#E5E9E6] text-[#111318] hover:border-[#D1D7D3] hover:bg-[#F1F4F2] shadow-2xs text-xs font-semibold px-3 py-2 rounded-lg cursor-pointer transition-all duration-200"
          >
            Refresh
          </Button>
          <Button
            variant="secondary"
            size="sm"
            onClick={handleMarkAllAsRead}
            isLoading={isMarkingAll}
            disabled={isLoading || notifications.length === 0}
            leftIcon={<CheckCheck className="w-3.5 h-3.5 text-[#08704F]" />}
            className="bg-[#E8F7F0] hover:bg-[#D4EFE3] text-[#08704F] border border-[#0B8F63]/30 shadow-2xs text-xs font-semibold px-3 py-2 rounded-lg cursor-pointer transition-all duration-200"
          >
            Mark All Read
          </Button>
        </div>
      </div>

      {/* ==================================================
          2. COMPACT SUMMARY STRIP
          ================================================== */}
      <div className="bg-white border border-[#E5E9E6] rounded-xl p-4 shadow-2xs grid grid-cols-2 sm:grid-cols-4 gap-4 divide-y sm:divide-y-0 sm:divide-x divide-[#E5E9E6] animate-console-fade-in delay-1">
        <div className="pt-2 sm:pt-0 sm:px-3 first:pt-0 first:px-0">
          <span className="text-[11px] font-semibold uppercase tracking-wider text-[#667085] block">
            Total Events
          </span>
          <div className="mt-1 flex items-baseline gap-2">
            <span className="font-space-grotesk font-bold text-xl sm:text-2xl text-[#111318] tabular-nums">
              {totalElements}
            </span>
            <span className="text-[11px] text-[#98A2B3] font-mono">logged</span>
          </div>
        </div>

        <div className="pt-2 sm:pt-0 sm:px-3">
          <span className="text-[11px] font-semibold uppercase tracking-wider text-[#667085] block">
            Unread Events
          </span>
          <div className="mt-1 flex items-baseline gap-2">
            <span
              className={`font-space-grotesk font-bold text-xl sm:text-2xl tabular-nums ${
                unreadCount > 0 ? 'text-[#08704F]' : 'text-[#667085]'
              }`}
            >
              {unreadCount}
            </span>
            {unreadCount > 0 ? (
              <span className="inline-flex items-center gap-1 text-[11px] text-[#08704F] font-semibold bg-[#E8F7F0] px-1.5 py-0.5 rounded-full">
                <span className="w-1.5 h-1.5 rounded-full bg-[#0B8F63] pulse-subtle" />
                pending
              </span>
            ) : (
              <span className="text-[11px] text-[#98A2B3]">cleared</span>
            )}
          </div>
        </div>

        <div className="pt-2 sm:pt-0 sm:px-3">
          <span className="text-[11px] font-semibold uppercase tracking-wider text-[#667085] block">
            Recovery Events
          </span>
          <div className="mt-1 flex items-baseline gap-2">
            <span className="font-space-grotesk font-bold text-xl sm:text-2xl text-[#0B8F63] tabular-nums">
              {recoveryEventsCount}
            </span>
            <span className="text-[11px] text-[#98A2B3]">captured</span>
          </div>
        </div>

        <div className="pt-2 sm:pt-0 sm:px-3">
          <span className="text-[11px] font-semibold uppercase tracking-wider text-[#667085] block">
            System Alerts
          </span>
          <div className="mt-1 flex items-baseline gap-2">
            <span
              className={`font-space-grotesk font-bold text-xl sm:text-2xl tabular-nums ${
                systemAlertsCount > 0 ? 'text-[#D97706]' : 'text-[#667085]'
              }`}
            >
              {systemAlertsCount}
            </span>
            <span className="text-[11px] text-[#98A2B3]">audited</span>
          </div>
        </div>
      </div>

      {/* ==================================================
          3. CONTROL & FILTER TOOLBAR
          ================================================== */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-4 rounded-xl bg-white border border-[#E5E9E6] shadow-2xs animate-console-fade-in delay-2">
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
                ? 'bg-[#E8F7F0] text-[#08704F] border-[#0B8F63]/30 shadow-2xs'
                : 'bg-white text-[#667085] border-[#E5E9E6] hover:text-[#111318] hover:bg-[#F1F4F2]'
            }`}
            aria-pressed={unreadOnly}
          >
            <Filter className="w-3.5 h-3.5" />
            <span>Unread Only</span>
          </button>

          {/* Clear Filters indicator button */}
          {(unreadOnly || selectedEvent !== 'ALL') && (
            <button
              type="button"
              onClick={() => {
                setUnreadOnly(false);
                setSelectedEvent('ALL');
                setCurrentPage(0);
              }}
              className="inline-flex items-center gap-1.5 text-xs text-[#08704F] hover:text-[#0B8F63] font-semibold hover:underline cursor-pointer ml-1"
            >
              <X className="w-3.5 h-3.5" />
              <span>Clear Filters</span>
            </button>
          )}
        </div>

        {/* Results summary counter */}
        <div className="text-xs text-[#667085] font-inter">
          {!isLoading && (
            <span>
              Showing <span className="font-semibold text-[#111318]">{notifications.length}</span> of{' '}
              <span className="font-semibold text-[#111318]">{totalElements}</span> notifications
            </span>
          )}
        </div>
      </div>

      {/* ==================================================
          4. MAIN CONTENT FEED
          ================================================== */}
      {isLoading ? (
        <div className="space-y-3" role="status" aria-label="Loading notifications">
          {Array.from({ length: 5 }).map((_, i) => (
            <div
              key={i}
              className="p-4 sm:p-5 rounded-xl border border-[#E5E9E6] bg-white space-y-3 shadow-2xs animate-pulse"
            >
              <div className="flex items-center gap-3">
                <Skeleton className="w-10 h-10 rounded-xl" />
                <div className="space-y-1.5 flex-1">
                  <div className="flex items-center gap-2">
                    <Skeleton className="h-5 w-28 rounded-full" />
                    <Skeleton className="h-4 w-16 ml-auto" />
                  </div>
                  <Skeleton className="h-4 w-2/3" />
                </div>
              </div>
              <Skeleton className="h-3 w-full" />
              <Skeleton className="h-3 w-1/3" />
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
          icon={<Inbox className="w-10 h-10 text-[#98A2B3]" />}
          title="No Notifications Found"
          description={
            unreadOnly || selectedEvent !== 'ALL'
              ? 'No notifications match your current filter criteria. Try resetting the filters.'
              : 'You are all caught up! Recovery events and system alerts will appear here in real time.'
          }
          action={
            unreadOnly || selectedEvent !== 'ALL' ? (
              <Button
                variant="outline"
                size="sm"
                onClick={() => {
                  setUnreadOnly(false);
                  setSelectedEvent('ALL');
                  setCurrentPage(0);
                }}
                className="bg-white border-[#E5E9E6] text-[#111318] hover:bg-[#F1F4F2] text-xs font-semibold"
              >
                Reset Filters
              </Button>
            ) : undefined
          }
        />
      ) : (
        <div className="space-y-4 animate-console-fade-in delay-3">
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
            <div className="pt-2 border-t border-[#E5E9E6]">
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

      {/* ==================================================
          5. EVENT DETAIL MODAL
          ================================================== */}
      <NotificationDetailModal
        notification={selectedNotification}
        isOpen={Boolean(selectedNotification)}
        onClose={() => setSelectedNotification(null)}
        onMarkAsRead={handleMarkAsRead}
        isMarkingRead={Boolean(selectedNotification && markingId === selectedNotification.id)}
      />

      {/* ==================================================
          6. SHARED FOOTER
          ================================================== */}
      <Footer />
    </div>
  );
}
