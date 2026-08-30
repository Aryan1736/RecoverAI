import { NotificationPreferencesMatrix } from '../../components/settings/NotificationPreferencesMatrix';

export function NotificationSettingsPage() {
  return (
    <div className="space-y-6 max-w-5xl">
      <div className="space-y-1">
        <h3 className="text-base font-semibold text-white">
          Notification Preferences & Delivery Rules
        </h3>
        <p className="text-xs text-slate-400">
          Configure which events trigger multi-channel alerts across email, webhooks, and the in-app notification center.
        </p>
      </div>

      <NotificationPreferencesMatrix />
    </div>
  );
}
