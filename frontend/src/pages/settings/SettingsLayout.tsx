import { Link, useLocation, Outlet } from 'react-router-dom';
import { Settings, Bell, Server } from 'lucide-react';

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

  const isNotifications = location.pathname.startsWith('/settings/notifications');
  const isProviders = location.pathname.startsWith('/settings/providers');
  const isGeneral = !isNotifications && !isProviders;

  const currentTitle = isGeneral
    ? 'General & Account'
    : isNotifications
    ? 'Notification Preferences'
    : 'Provider Status';

  const currentDescription = isGeneral
    ? 'Manage your RecoverAI account, merchant identity, and workspace configuration.'
    : isNotifications
    ? 'Control which recovery events trigger notifications and where they are delivered.'
    : 'Monitor payment provider connectivity, health, and recovery readiness.';

  return (
    <div className="space-y-6 font-inter animate-console-fade-in delay-0">
      {/* Settings Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between pt-1">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-[#0B8F63] pulse-subtle" />
            <span className="text-[11px] font-bold uppercase tracking-[0.08em] text-[#08704F]">
              {isNotifications ? 'NOTIFICATION PREFERENCES' : isProviders ? 'PROVIDER STATUS' : 'SETTINGS'}
            </span>
            <span className="text-[#98A2B3] text-xs">/</span>
            <span className="text-[11px] font-semibold text-[#667085]">
              Settings &amp; Operations
            </span>
          </div>
          <h1 className="font-space-grotesk font-bold text-2xl sm:text-3xl text-[#111318] tracking-tight">
            {currentTitle}
          </h1>
          <p className="text-xs text-[#667085] max-w-xl">
            {currentDescription}
          </p>
        </div>
      </div>

      {/* Secondary Settings Navigation Tabs */}
      <div className="border-b border-[#E5E9E6]">
        <nav
          className="flex space-x-2 overflow-x-auto pb-2 -mb-px"
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
                className={`inline-flex items-center gap-2 px-3.5 py-2 text-xs sm:text-sm font-medium rounded-lg whitespace-nowrap transition-all duration-150 ${
                  isActive
                    ? 'bg-[#E8F7F0] text-[#08704F] font-semibold border border-[#0B8F63]/30 shadow-2xs'
                    : 'text-[#667085] hover:text-[#111318] hover:bg-[#F1F4F2] border border-transparent'
                }`}
                aria-current={isActive ? 'page' : undefined}
              >
                <Icon
                  className={`w-4 h-4 ${isActive ? 'text-[#0B8F63]' : 'text-[#98A2B3]'}`}
                  aria-hidden="true"
                />
                <span>{tab.name}</span>
              </Link>
            );
          })}
        </nav>
      </div>

      {/* Nested Settings Content */}
      <div className="pt-1">
        <Outlet />
      </div>
    </div>
  );
}
