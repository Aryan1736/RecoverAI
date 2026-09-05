import { NotificationPreferencesMatrix } from '../../components/settings/NotificationPreferencesMatrix';
import { Footer } from '../../components/layout/Footer';

export function NotificationSettingsPage() {
  return (
    <div className="space-y-8 max-w-5xl font-inter animate-console-fade-in delay-1 pb-6">
      <NotificationPreferencesMatrix />
      <Footer />
    </div>
  );
}
