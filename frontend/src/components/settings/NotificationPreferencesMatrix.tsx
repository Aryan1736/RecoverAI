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

interface ChannelMeta {
  key: MerchantNotificationChannel;
  label: string;
  icon: typeof Mail;
}

const CHANNELS: ChannelMeta[] = [
  { key: 'EMAIL', label: 'Email', icon: Mail },
  { key: 'WEBHOOK', label: 'Webhook', icon: Webhook },
  { key: 'IN_APP', label: 'In-App', icon: Bell },
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
        <Skeleton className="h-40 w-full rounded-2xl" />
        <Skeleton className="h-64 w-full rounded-2xl" />
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
      {/* Top Action Bar with Dirty indicator */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-4 rounded-xl bg-white border border-slate-200 shadow-2xs">
        <div className="flex items-center gap-2.5">
          <h3 className="text-sm font-semibold text-slate-900">Event Channel Matrix</h3>
          {isDirty ? (
            <Badge variant="warning" dot pulse>
              Unsaved Changes
            </Badge>
          ) : (
            <Badge variant="success" dot>
              Persisted
            </Badge>
          )}
        </div>

        <div className="flex items-center gap-2">
          <Button
            size="sm"
            variant="outline"
            onClick={handleReset}
            disabled={!isDirty || isSaving}
            leftIcon={<RotateCcw className="w-3.5 h-3.5" />}
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
          >
            Save Changes
          </Button>
        </div>
      </div>

      {/* Preferences Matrix Table */}
      <Card className="overflow-hidden border-slate-200 p-0 shadow-2xs">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm" role="grid" aria-label="Notification Preferences Matrix">
            <thead>
              <tr className="border-b border-slate-200 bg-slate-50 text-slate-600">
                <th className="py-3.5 px-4 sm:px-6 text-xs font-semibold uppercase tracking-wider">
                  Lifecycle Event
                </th>
                {CHANNELS.map((ch) => {
                  const Icon = ch.icon;
                  return (
                    <th
                      key={ch.key}
                      className="py-3.5 px-4 sm:px-6 text-center text-xs font-semibold uppercase tracking-wider"
                    >
                      <div className="inline-flex items-center gap-1.5 justify-center">
                        <Icon className="w-4 h-4 text-slate-500" aria-hidden="true" />
                        <span>{ch.label}</span>
                      </div>
                    </th>
                  );
                })}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {EVENTS.map((event) => {
                const EventIcon = event.icon;
                return (
                  <tr
                    key={event.key}
                    className="hover:bg-slate-50/70 transition-colors duration-150"
                  >
                    <td className="py-4 px-4 sm:px-6">
                      <div className="flex items-start gap-3">
                        <div className="p-2 rounded-lg bg-slate-100 border border-slate-200 text-slate-700 shrink-0 mt-0.5">
                          <EventIcon className="w-4 h-4" />
                        </div>
                        <div>
                          <div className="font-semibold text-slate-900 text-sm">
                            {event.title}
                          </div>
                          <div className="text-xs text-slate-500 mt-0.5 max-w-md">
                            {event.description}
                          </div>
                        </div>
                      </div>
                    </td>

                    {CHANNELS.map((channel) => {
                      const enabled = Boolean(preferences[event.key]?.[channel.key]);
                      const controlId = `pref-${event.key}-${channel.key}`;

                      return (
                        <td
                          key={channel.key}
                          className="py-4 px-4 sm:px-6 text-center align-middle"
                        >
                          <button
                            type="button"
                            role="switch"
                            id={controlId}
                            aria-checked={enabled}
                            aria-label={`Enable ${channel.label} for ${event.title}`}
                            onClick={() => handleToggle(event.key, channel.key)}
                            className={`relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:ring-offset-2 ${
                              enabled ? 'bg-emerald-600' : 'bg-slate-200'
                            }`}
                          >
                            <span
                              className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow-sm ring-0 transition duration-200 ease-in-out flex items-center justify-center ${
                                enabled ? 'translate-x-5' : 'translate-x-0'
                              }`}
                            >
                              {enabled && (
                                <Check className="w-3 h-3 text-emerald-600 stroke-[3]" />
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

      {/* Webhook Configuration Section */}
      <Card className="border-slate-200 shadow-2xs space-y-4">
        <div className="flex items-center justify-between pb-3 border-b border-slate-100">
          <div className="flex items-center gap-2">
            <Webhook className="w-4 h-4 text-emerald-600" />
            <h3 className="text-sm font-semibold text-slate-900">Merchant Webhook Endpoint</h3>
          </div>
          {webhookUrl ? (
            <Badge variant="success" dot>
              Endpoint Configured
            </Badge>
          ) : (
            <Badge variant="outline">Not Configured</Badge>
          )}
        </div>

        <div className="space-y-3">
          <p className="text-xs text-slate-500">
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

          {/* Security Notice */}
          <div className="p-3 rounded-xl bg-slate-50 border border-slate-200 flex items-start gap-2.5">
            <ShieldCheck className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
            <div className="text-xs text-slate-600 space-y-1">
              <span className="font-semibold text-slate-900">Zero Secret Exposure &amp; Payload Verification:</span>
              <p className="text-slate-500">
                Webhook payloads are cryptographically signed using HMAC-SHA256 sent via the{' '}
                <code className="text-slate-800 font-mono bg-slate-100 px-1 py-0.5 rounded border border-slate-200">X-Recovery-Signature</code> header.
                Signing secrets are securely persisted server-side and never exposed to frontend code or client state.
              </p>
            </div>
          </div>
        </div>
      </Card>
    </div>
  );
}
