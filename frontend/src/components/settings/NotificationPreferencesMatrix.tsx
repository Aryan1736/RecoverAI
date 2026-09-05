import { useState, useEffect, useMemo } from 'react';
import {
  Save,
  RotateCcw,
  Webhook,
  Mail,
  Bell,
  ShieldCheck,
  CheckCircle2,
  AlertOctagon,
  AlertTriangle,
  Radio,
  Check,
  Shield,
  Sparkles,
} from 'lucide-react';
import { Card } from '../ui/Card';
import { Button } from '../ui/Button';
import { Badge } from '../ui/Badge';
import { Input } from '../ui/Input';
import { Skeleton } from '../ui/Skeleton';
import { ErrorState } from '../ui/ErrorState';
import { useToast } from '../../hooks/useToast';
import { useDemoMode } from '../../hooks/useDemoMode';
import {
  getNotificationPreferences,
  updateNotificationPreferences,
} from '../../api/notification-preferences';
import { getDemoNotificationPreferences } from '../../api/demo';
import type {
  MerchantNotificationEvent,
  MerchantNotificationChannel,
  NotificationPreferenceResponseDto,
} from '../../types/notifications';

interface EventMeta {
  key: MerchantNotificationEvent;
  title: string;
  description: string;
  icon: typeof CheckCircle2;
}

const EVENTS: EventMeta[] = [
  {
    key: 'PAYMENT_RECOVERED',
    title: 'Payment Recovered',
    description: 'Triggered when a previously failed payment is successfully recovered.',
    icon: CheckCircle2,
  },
  {
    key: 'CASE_EXHAUSTED',
    title: 'Case Exhausted',
    description: 'Triggered when a recovery case reaches terminal failure after all retry attempts.',
    icon: AlertOctagon,
  },
  {
    key: 'HIGH_PRIORITY_FAILURE',
    title: 'High Priority Failure',
    description: 'Triggered when a high-value payment case encounters an immediate blocker.',
    icon: AlertTriangle,
  },
  {
    key: 'PROVIDER_DEGRADED',
    title: 'Provider Degraded',
    description: 'Triggered when an upstream gateway or communications provider experiences downtime.',
    icon: Radio,
  },
];

const EVENT_CONFIG: Record<
  MerchantNotificationEvent,
  {
    iconBg: string;
    iconBorder: string;
    iconText: string;
    badgeBg: string;
    badgeText: string;
  }
> = {
  PAYMENT_RECOVERED: {
    iconBg: 'bg-[#E8F7F0]',
    iconBorder: 'border-[#0B8F63]/30',
    iconText: 'text-[#08704F]',
    badgeBg: 'bg-[#E8F7F0]',
    badgeText: 'text-[#08704F]',
  },
  CASE_EXHAUSTED: {
    iconBg: 'bg-[#FEE2E2]',
    iconBorder: 'border-[#DC2626]/30',
    iconText: 'text-[#DC2626]',
    badgeBg: 'bg-[#FEE2E2]',
    badgeText: 'text-[#DC2626]',
  },
  HIGH_PRIORITY_FAILURE: {
    iconBg: 'bg-[#FEF3C7]',
    iconBorder: 'border-[#D97706]/30',
    iconText: 'text-[#D97706]',
    badgeBg: 'bg-[#FEF3C7]',
    badgeText: 'text-[#D97706]',
  },
  PROVIDER_DEGRADED: {
    iconBg: 'bg-[#FFF7ED]',
    iconBorder: 'border-[#EA580C]/30',
    iconText: 'text-[#C2410C]',
    badgeBg: 'bg-[#FFF7ED]',
    badgeText: 'text-[#C2410C]',
  },
};

interface ChannelMeta {
  key: MerchantNotificationChannel;
  label: string;
  sublabel: string;
  icon: typeof Mail;
  description: string;
}

const CHANNELS: ChannelMeta[] = [
  {
    key: 'EMAIL',
    label: 'Email',
    sublabel: 'Merchant inbox',
    icon: Mail,
    description: 'Operational recovery notifications dispatched to your verified administrative email address.',
  },
  {
    key: 'WEBHOOK',
    label: 'Webhook',
    sublabel: 'System integration',
    icon: Webhook,
    description: 'Machine-readable JSON event payloads dispatched to your configured endpoint with HMAC-SHA256 signatures.',
  },
  {
    key: 'IN_APP',
    label: 'In-App',
    sublabel: 'RecoverAI console',
    icon: Bell,
    description: 'Real-time alerts and activity entries published directly to the RecoverAI console & notification feed.',
  },
];

type PreferencesMap = Record<
  MerchantNotificationEvent,
  Record<MerchantNotificationChannel, boolean>
>;

function buildDefaultMatrix(): PreferencesMap {
  const map: Partial<PreferencesMap> = {};
  for (const ev of EVENTS) {
    map[ev.key] = {
      EMAIL: true,
      WEBHOOK: false,
      IN_APP: true,
    };
  }
  return map as PreferencesMap;
}

export function NotificationPreferencesMatrix() {
  const { isDemoMode } = useDemoMode();
  const { toast } = useToast();

  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isSaving, setIsSaving] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  // Persisted state from server
  const [persistedData, setPersistedData] = useState<NotificationPreferenceResponseDto | null>(null);

  // Form local state
  const [preferences, setPreferences] = useState<PreferencesMap>(buildDefaultMatrix());
  const [webhookUrl, setWebhookUrl] = useState<string>('');
  const [urlError, setUrlError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function loadData() {
      setIsLoading(true);
      setError(null);
      try {
        const data = isDemoMode
          ? await getDemoNotificationPreferences()
          : await getNotificationPreferences();
        if (!cancelled) {
          setPersistedData(data);
          setWebhookUrl(data.webhookUrl || '');

          // Initialize preferences matrix from server response
          const initialMap = buildDefaultMatrix();
          if (data.preferences) {
            for (const ev of EVENTS) {
              if (data.preferences[ev.key]) {
                initialMap[ev.key] = {
                  EMAIL: Boolean(data.preferences[ev.key]?.EMAIL),
                  WEBHOOK: Boolean(data.preferences[ev.key]?.WEBHOOK),
                  IN_APP: Boolean(data.preferences[ev.key]?.IN_APP),
                };
              }
            }
          }
          setPreferences(initialMap);
        }
      } catch (err: unknown) {
        if (!cancelled) {
          const msg = err instanceof Error ? err.message : 'Failed to load preferences';
          setError(msg);
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
  }, [isDemoMode]);

  const fetchPreferences = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = isDemoMode
        ? await getDemoNotificationPreferences()
        : await getNotificationPreferences();
      setPersistedData(data);
      setWebhookUrl(data.webhookUrl || '');

      const initialMap = buildDefaultMatrix();
      if (data.preferences) {
        for (const ev of EVENTS) {
          if (data.preferences[ev.key]) {
            initialMap[ev.key] = {
              EMAIL: Boolean(data.preferences[ev.key]?.EMAIL),
              WEBHOOK: Boolean(data.preferences[ev.key]?.WEBHOOK),
              IN_APP: Boolean(data.preferences[ev.key]?.IN_APP),
            };
          }
        }
      }
      setPreferences(initialMap);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to load preferences';
      setError(msg);
    } finally {
      setIsLoading(false);
    }
  };

  // Validate URL format if provided
  const validateUrl = (url: string): boolean => {
    if (!url.trim()) {
      setUrlError(null);
      return true;
    }
    try {
      const parsed = new URL(url.trim());
      if (parsed.protocol !== 'http:' && parsed.protocol !== 'https:') {
        setUrlError('URL must use HTTP or HTTPS protocol');
        return false;
      }
      setUrlError(null);
      return true;
    } catch {
      setUrlError('Please enter a valid URL');
      return false;
    }
  };

  // Determine dirty state (unsaved changes)
  const isDirty = useMemo(() => {
    if (!persistedData) return false;

    // Check webhook URL
    const persistedUrl = persistedData.webhookUrl || '';
    if (webhookUrl.trim() !== persistedUrl.trim()) return true;

    // Check matrix checkboxes
    for (const ev of EVENTS) {
      for (const ch of CHANNELS) {
        const currentVal = Boolean(preferences[ev.key]?.[ch.key]);
        const serverVal = Boolean(persistedData.preferences?.[ev.key]?.[ch.key]);
        if (currentVal !== serverVal) return true;
      }
    }
    return false;
  }, [persistedData, preferences, webhookUrl]);

  // Derived operational summary metrics
  const summaryMetrics = useMemo(() => {
    const totalEvents = EVENTS.length;
    let enabledEventsCount = 0;
    let activeRulesCount = 0;

    for (const ev of EVENTS) {
      const hasAnyEnabled = CHANNELS.some((ch) => Boolean(preferences[ev.key]?.[ch.key]));
      if (hasAnyEnabled) {
        enabledEventsCount++;
      }
      for (const ch of CHANNELS) {
        if (preferences[ev.key]?.[ch.key]) {
          activeRulesCount++;
        }
      }
    }

    return {
      totalEvents,
      enabledEventsCount,
      deliveryChannelsCount: CHANNELS.length,
      activeRulesCount,
      totalRulesCapacity: totalEvents * CHANNELS.length,
    };
  }, [preferences]);

  const handleToggle = (event: MerchantNotificationEvent, channel: MerchantNotificationChannel) => {
    setPreferences((prev) => ({
      ...prev,
      [event]: {
        ...prev[event],
        [channel]: !prev[event]?.[channel],
      },
    }));
  };

  const handleReset = () => {
    if (!persistedData) return;
    setWebhookUrl(persistedData.webhookUrl || '');
    setUrlError(null);
    const merged = buildDefaultMatrix();
    if (persistedData.preferences) {
      for (const ev of EVENTS) {
        if (persistedData.preferences[ev.key]) {
          merged[ev.key] = {
            EMAIL: Boolean(persistedData.preferences[ev.key]?.EMAIL),
            WEBHOOK: Boolean(persistedData.preferences[ev.key]?.WEBHOOK),
            IN_APP: Boolean(persistedData.preferences[ev.key]?.IN_APP),
          };
        }
      }
    }
    setPreferences(merged);
    toast.info('Preferences reset to saved values');
  };

  const handleSave = async () => {
    if (!validateUrl(webhookUrl)) {
      toast.error('Please fix invalid webhook URL before saving');
      return;
    }

    setIsSaving(true);
    try {
      if (isDemoMode) {
        toast.info('Simulated in Demo Mode: preferences updated locally.');
        setPersistedData({
          merchantId: 'demo-merchant-evaluator',
          webhookUrl: webhookUrl.trim() || null,
          preferences,
        });
        return;
      }
      const payload = {
        webhookUrl: webhookUrl.trim() || null,
        preferences,
      };
      const updated = await updateNotificationPreferences(payload);
      setPersistedData(updated);
      setWebhookUrl(updated.webhookUrl || '');
      toast.success('Notification preferences updated successfully');
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to save preferences';
      toast.error(msg);
    } finally {
      setIsSaving(false);
    }
  };

  if (isLoading) {
    return (
      <div className="space-y-6" role="status" aria-label="Loading preferences">
        {/* Summary Skeleton */}
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
          {[1, 2, 3, 4].map((i) => (
            <Skeleton key={i} className="h-24 w-full rounded-xl bg-white border border-[#E5E9E6]" />
          ))}
        </div>
        {/* Toolbar Skeleton */}
        <Skeleton className="h-16 w-full rounded-xl bg-white border border-[#E5E9E6]" />
        {/* Matrix Table Skeleton */}
        <Skeleton className="h-72 w-full rounded-xl bg-white border border-[#E5E9E6]" />
        {/* Webhook Card Skeleton */}
        <Skeleton className="h-44 w-full rounded-xl bg-white border border-[#E5E9E6]" />
      </div>
    );
  }

  if (error) {
    return (
      <ErrorState
        title="Failed to Load Preferences"
        message={error}
        onRetry={fetchPreferences}
      />
    );
  }

  return (
    <div className="space-y-6">
      {/* ==================================================
          1. COMPACT OPERATIONAL SUMMARY STRIP
          ================================================== */}
      <section
        aria-label="Notification Operations Summary"
        className="bg-white border border-[#E5E9E6] rounded-xl p-4 sm:p-5 shadow-2xs"
      >
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 divide-y md:divide-y-0 md:divide-x divide-[#ECEFEA]">
          {/* Total Configurable Events */}
          <div className="flex flex-col justify-between pt-2 md:pt-0 first:pt-0">
            <span className="text-[11px] font-semibold text-[#667085] uppercase tracking-[0.06em]">
              TOTAL EVENTS
            </span>
            <div className="mt-2 flex items-baseline gap-2">
              <span className="font-space-grotesk text-2xl sm:text-3xl font-bold text-[#111318] tracking-tight">
                {summaryMetrics.totalEvents}
              </span>
              <span className="text-xs text-[#667085]">lifecycle types</span>
            </div>
            <p className="mt-1 text-[11px] text-[#98A2B3]">
              Deterministic recovery states
            </p>
          </div>

          {/* Enabled Events */}
          <div className="flex flex-col justify-between pt-3 md:pt-0 md:pl-5">
            <span className="text-[11px] font-semibold text-[#667085] uppercase tracking-[0.06em]">
              ENABLED EVENTS
            </span>
            <div className="mt-2 flex items-baseline gap-2">
              <span className="font-space-grotesk text-2xl sm:text-3xl font-bold text-[#08704F] tracking-tight">
                {summaryMetrics.enabledEventsCount}
              </span>
              <span className="text-xs text-[#667085]">
                of {summaryMetrics.totalEvents} active
              </span>
            </div>
            <p className="mt-1 text-[11px] text-[#98A2B3]">
              Events with ≥1 channel active
            </p>
          </div>

          {/* Supported Delivery Channels */}
          <div className="flex flex-col justify-between pt-3 md:pt-0 md:pl-5">
            <span className="text-[11px] font-semibold text-[#667085] uppercase tracking-[0.06em]">
              DELIVERY CHANNELS
            </span>
            <div className="mt-2 flex items-baseline gap-2">
              <span className="font-space-grotesk text-2xl sm:text-3xl font-bold text-[#111318] tracking-tight">
                {summaryMetrics.deliveryChannelsCount}
              </span>
              <span className="text-xs text-[#667085]">supported</span>
            </div>
            <p className="mt-1 text-[11px] text-[#98A2B3]">
              Email, Webhook, and In-App
            </p>
          </div>

          {/* Active Rules Count */}
          <div className="flex flex-col justify-between pt-3 md:pt-0 md:pl-5">
            <span className="text-[11px] font-semibold text-[#667085] uppercase tracking-[0.06em]">
              ACTIVE RULES
            </span>
            <div className="mt-2 flex items-baseline gap-2">
              <span className="font-space-grotesk text-2xl sm:text-3xl font-bold text-[#0B8F63] tracking-tight">
                {summaryMetrics.activeRulesCount}
              </span>
              <span className="text-xs text-[#667085]">
                / {summaryMetrics.totalRulesCapacity} routes
              </span>
            </div>
            <p className="mt-1 text-[11px] text-[#98A2B3]">
              Event-to-channel dispatches
            </p>
          </div>
        </div>
      </section>

      {/* ==================================================
          2. ACTION TOOLBAR & STATE STATUS
          ================================================== */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-4 rounded-xl bg-white border border-[#E5E9E6] shadow-2xs">
        <div className="flex flex-wrap items-center gap-2.5">
          <div>
            <h2 className="text-sm font-semibold text-[#111318]">Event Channel Matrix</h2>
            <p className="text-xs text-[#667085]">Notification Preferences &amp; Delivery Rules</p>
          </div>
          {isDirty ? (
            <Badge variant="warning" dot pulse>
              Unsaved Changes
            </Badge>
          ) : (
            <Badge variant="success" dot>
              Persisted
            </Badge>
          )}
          {isDemoMode && (
            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-semibold bg-[#FEF3C7] text-[#D97706] border border-[#D97706]/30">
              <Sparkles className="w-3 h-3 text-[#D97706]" />
              <span>Sandbox State</span>
            </span>
          )}
        </div>

        <div className="flex items-center gap-2">
          <Button
            size="sm"
            variant="outline"
            onClick={handleReset}
            disabled={!isDirty || isSaving}
            leftIcon={<RotateCcw className="w-3.5 h-3.5" />}
            className="border-[#E5E9E6] text-[#111318] hover:bg-[#F1F4F2]"
          >
            Reset
          </Button>
          <Button
            size="sm"
            variant="primary"
            onClick={handleSave}
            disabled={!isDirty}
            isLoading={isSaving}
            leftIcon={<Save className="w-3.5 h-3.5" />}
            className="bg-[#0B8F63] hover:bg-[#08704F] text-white border-transparent shadow-2xs"
          >
            Save Changes
          </Button>
        </div>
      </div>

      {/* ==================================================
          3. NOTIFICATION PREFERENCES MATRIX TABLE
          ================================================== */}
      <Card className="overflow-hidden border-[#E5E9E6] p-0 shadow-2xs bg-white">
        <div className="overflow-x-auto">
          <table
            className="w-full text-left text-sm block md:table"
            role="grid"
            aria-label="Notification Preferences Matrix"
          >
            <thead className="hidden md:table-header-group">
              <tr className="border-b border-[#E5E9E6] bg-[#F1F4F2]/70 text-[#667085]">
                <th className="py-3.5 px-4 sm:px-6 text-[11px] font-semibold uppercase tracking-[0.06em] min-w-[280px]">
                  Lifecycle Event
                </th>
                {CHANNELS.map((ch) => {
                  const Icon = ch.icon;
                  return (
                    <th
                      key={ch.key}
                      className="py-3.5 px-4 sm:px-6 text-center text-[11px] font-semibold uppercase tracking-[0.06em] min-w-[130px]"
                    >
                      <div className="inline-flex flex-col items-center justify-center gap-1">
                        <div className="inline-flex items-center gap-1.5 justify-center text-[#111318]">
                          <Icon className="w-3.5 h-3.5 text-[#0B8F63]" aria-hidden="true" />
                          <span>{ch.label}</span>
                        </div>
                        <span className="text-[10px] text-[#98A2B3] normal-case tracking-normal font-normal">
                          {ch.sublabel}
                        </span>
                      </div>
                    </th>
                  );
                })}
              </tr>
            </thead>
            <tbody className="block divide-y divide-[#ECEFEA] md:table-row-group">
              {EVENTS.map((event) => {
                const EventIcon = event.icon;
                const config = EVENT_CONFIG[event.key];
                return (
                  <tr
                    key={event.key}
                    className="block p-4 sm:p-5 space-y-3 hover:bg-[#F7F8F6] transition-colors duration-150 md:table-row md:p-0 md:space-y-0"
                  >
                    <td className="block p-0 md:table-cell md:py-4 md:px-6">
                      <div className="flex items-start gap-3">
                        <div
                          className={`p-2 rounded-lg border shrink-0 mt-0.5 ${config.iconBg} ${config.iconBorder} ${config.iconText}`}
                        >
                          <EventIcon className="w-4 h-4" />
                        </div>
                        <div className="space-y-1">
                          <div className="flex flex-wrap items-center gap-2">
                            <span className="font-semibold text-[#111318] text-sm">
                              {event.title}
                            </span>
                            <span className="font-mono text-[10px] px-1.5 py-0.5 rounded bg-[#F1F4F2] text-[#667085] border border-[#E5E9E6]">
                              {event.key}
                            </span>
                          </div>
                          <p className="text-xs text-[#667085] leading-relaxed max-w-md">
                            {event.description}
                          </p>
                        </div>
                      </div>
                    </td>

                    {CHANNELS.map((channel) => {
                      const enabled = Boolean(preferences[event.key]?.[channel.key]);
                      const controlId = `pref-${event.key}-${channel.key}`;
                      const ChannelIcon = channel.icon;

                      return (
                        <td
                          key={channel.key}
                          className="flex items-center justify-between py-2 px-2 rounded-lg bg-[#F7F8F6]/60 border border-[#ECEFEA] md:border-none md:bg-transparent md:table-cell md:py-4 md:px-6 md:text-center md:align-middle"
                        >
                          {/* Mobile-only channel identifier */}
                          <div className="flex items-center gap-2 md:hidden">
                            <div className="w-5 h-5 rounded bg-[#E8F7F0] text-[#08704F] flex items-center justify-center">
                              <ChannelIcon className="w-3 h-3" />
                            </div>
                            <span className="text-xs font-medium text-[#111318]">
                              {channel.label} Channel
                            </span>
                          </div>

                          {/* Accessible Toggle Switch */}
                          <button
                            type="button"
                            role="switch"
                            id={controlId}
                            aria-checked={enabled}
                            aria-label={`Enable ${channel.label} for ${event.title}`}
                            onClick={() => handleToggle(event.key, channel.key)}
                            className={`relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none focus:ring-2 focus:ring-[#0B8F63]/30 focus:ring-offset-2 ${
                              enabled ? 'bg-[#0B8F63]' : 'bg-[#E5E9E6]'
                            }`}
                          >
                            <span
                              className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow-sm ring-0 transition duration-200 ease-in-out flex items-center justify-center ${
                                enabled ? 'translate-x-5' : 'translate-x-0'
                              }`}
                            >
                              {enabled && (
                                <Check className="w-3 h-3 text-[#0B8F63] stroke-[3]" />
                              )}
                            </span>
                          </button>
                        </td>
                      );
                    })}
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </Card>

      {/* ==================================================
          4. CHANNEL DELIVERY SCOPE CARDS
          ================================================== */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {CHANNELS.map((ch) => {
          const Icon = ch.icon;
          return (
            <div
              key={ch.key}
              className="p-4 rounded-xl bg-white border border-[#E5E9E6] shadow-2xs space-y-1.5"
            >
              <div className="flex items-center gap-2 text-xs font-semibold text-[#111318]">
                <div className="w-6 h-6 rounded-md bg-[#E8F7F0] text-[#08704F] flex items-center justify-center">
                  <Icon className="w-3.5 h-3.5" />
                </div>
                <span>{ch.label} Delivery</span>
              </div>
              <p className="text-[11px] text-[#667085] leading-relaxed">
                {ch.description}
              </p>
            </div>
          );
        })}
      </div>

      {/* ==================================================
          5. WEBHOOK CONFIGURATION SECTION
          ================================================== */}
      <Card className="border-[#E5E9E6] shadow-2xs space-y-4 bg-white p-5 sm:p-6">
        <div className="flex items-center justify-between pb-3 border-b border-[#ECEFEA]">
          <div className="flex items-center gap-2">
            <Webhook className="w-4 h-4 text-[#0B8F63]" />
            <h3 className="text-sm font-semibold text-[#111318]">Merchant Webhook Endpoint</h3>
          </div>
          {webhookUrl ? (
            <Badge variant="success" dot>
              Endpoint Configured
            </Badge>
          ) : (
            <Badge variant="outline">Not Configured</Badge>
          )}
        </div>

        <div className="space-y-4">
          <p className="text-xs text-[#667085] leading-relaxed">
            RecoverAI delivers real-time JSON payloads to this endpoint whenever recovery events occur with the Webhook channel enabled.
          </p>

          <Input
            label="Webhook Destination URL"
            placeholder="https://api.yourdomain.com/webhooks/recoverai"
            value={webhookUrl}
            onChange={(e) => {
              setWebhookUrl(e.target.value);
              validateUrl(e.target.value);
            }}
            error={urlError || undefined}
            helperText="Must be a valid HTTP or HTTPS endpoint reachable by RecoverAI"
          />

          {/* Security & Verification Notice */}
          <div className="p-3.5 rounded-xl bg-[#F7F8F6] border border-[#E5E9E6] flex items-start gap-3">
            <ShieldCheck className="w-4 h-4 text-[#0B8F63] shrink-0 mt-0.5" />
            <div className="text-xs text-[#667085] space-y-1">
              <span className="font-semibold text-[#111318]">
                Zero Secret Exposure &amp; Payload Verification:
              </span>
              <p className="text-[#667085] leading-relaxed">
                Webhook payloads are cryptographically signed using HMAC-SHA256 sent via the{' '}
                <code className="text-[#111318] font-mono bg-[#E5E9E6] px-1 py-0.5 rounded text-[11px]">
                  X-Recovery-Signature
                </code>{' '}
                header. Signing secrets are securely persisted server-side and never exposed to frontend code or client state.
              </p>
            </div>
          </div>

          {/* Operational Safety Notice */}
          <div className="p-3.5 rounded-xl bg-[#F7F8F6] border border-[#E5E9E6] flex items-start gap-3">
            <Shield className="w-4 h-4 text-[#667085] shrink-0 mt-0.5" />
            <div className="text-xs text-[#667085] space-y-1">
              <span className="font-semibold text-[#111318]">
                Fintech Delivery Safety Notice:
              </span>
              <p className="text-[#667085] leading-relaxed">
                Notification preferences control multi-channel alert dispatch only. Toggling delivery channels does not execute payment retries, capture transactions, or alter algorithmic recovery strategy.
              </p>
            </div>
          </div>
        </div>
      </Card>
    </div>
  );
}
