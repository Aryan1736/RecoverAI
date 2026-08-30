import { Link, useLocation, Outlet } from 'react-router-dom';
import { Settings, Bell, Server } from 'lucide-react';
import { PageHeader } from '../../components/ui/PageHeader';

interface TabItem {
  name: string;
  href: string;
  icon: typeof Settings;
}

const SETTINGS_TABS: TabItem[] = [
  { name: 'General & Account', href: '/settings/general', icon: Settings },
  { name: 'Notification Preferences', href: '/settings/notifications', icon: Bell },
  { name: 'Provider Status', href: '/settings/providers', icon: Server },
];

export function SettingsLayout() {
  const location = useLocation();

  return (
    <div className="space-y-6">
      <PageHeader
        title="Settings & Operations"
        description="Manage your merchant account settings, notification preferences, and monitor upstream provider health"
      />

      {/* Secondary Settings Navigation Tabs */}
      <div className="border-b border-slate-200">
        <nav
          className="flex space-x-2 sm:space-x-4 overflow-x-auto pb-px"
          aria-label="Settings navigation"
        >
          {SETTINGS_TABS.map((tab) => {
            const isActive =
              location.pathname === tab.href ||
              (tab.href === '/settings/general' && location.pathname === '/settings');
            const Icon = tab.icon;

            return (
              <Link
                key={tab.name}
                to={tab.href}
                className={`inline-flex items-center gap-2 px-3.5 py-2.5 text-xs sm:text-sm font-medium border-b-2 whitespace-nowrap transition-colors duration-150 ${
                  isActive
                    ? 'border-emerald-600 text-emerald-700 font-semibold'
                    : 'border-transparent text-slate-500 hover:text-slate-900 hover:border-slate-300'
                }`}
                aria-current={isActive ? 'page' : undefined}
              >
                <Icon
                  className={`w-4 h-4 ${isActive ? 'text-emerald-600' : 'text-slate-400'}`}
                  aria-hidden="true"
                />
                <span>{tab.name}</span>
              </Link>
            );
          })}
        </nav>
      </div>

      {/* Nested Settings Content */}
      <div className="pt-2">
        <Outlet />
      </div>
    </div>
  );
}
