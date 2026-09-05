import { Link, useLocation } from 'react-router-dom';
import {
  Zap,
  LayoutDashboard,
  ShieldAlert,
  BarChart3,
  Bell,
  Settings,
  ChevronLeft,
  ChevronRight,
  X,
  Radio,
} from 'lucide-react';
import { Badge } from '../ui/Badge';

export interface SidebarProps {
  isCollapsed: boolean;
  setIsCollapsed: (value: boolean | ((prev: boolean) => boolean)) => void;
  isMobileOpen: boolean;
  setIsMobileOpen: (value: boolean) => void;
}

interface NavItem {
  name: string;
  href: string;
  icon: typeof LayoutDashboard;
  isUpcoming?: boolean;
}

const navItems: NavItem[] = [
  { name: 'Overview', href: '/app', icon: LayoutDashboard },
  { name: 'Recovery Cases', href: '/recovery-cases', icon: ShieldAlert },
  { name: 'Analytics', href: '/analytics', icon: BarChart3 },
  { name: 'Notifications', href: '/notifications', icon: Bell },
  { name: 'Settings', href: '/settings', icon: Settings },
];

export function Sidebar({
  isCollapsed,
  setIsCollapsed,
  isMobileOpen,
  setIsMobileOpen,
}: SidebarProps) {
  const location = useLocation();

  const sidebarContent = (
    <div className="flex flex-col h-full bg-white border-r border-[#E5E9E6] text-[#667085] select-none">
      {/* Brand Header */}
      <div
        className={`h-16 flex items-center justify-between px-3.5 border-b border-[#E5E9E6] ${
          isCollapsed ? 'justify-center px-2' : ''
        }`}
      >
        {!isCollapsed ? (
          <div className="flex items-center gap-2">
            <Link
              to="/app"
              className="flex items-center gap-2.5 focus:outline-none focus-visible:ring-2 focus-visible:ring-[#0B8F63] rounded-lg transition-opacity hover:opacity-90"
              onClick={() => setIsMobileOpen(false)}
              aria-label="RecoverAI Home"
            >
              <img
                src="/img.png"
                alt="RecoverAI"
                data-testid="sidebar-brand-favicon"
                className="h-9 w-auto object-contain shrink-0"
              />
              <div className="flex flex-col truncate">
                <div className="flex items-center gap-1.5">
                  <span className="font-bold text-[15px] tracking-tight text-[#111318] font-inter">
                    RecoverAI
                  </span>
                  <span className="text-[9px] uppercase font-semibold tracking-wider text-[#08704F] bg-[#E8F7F0] px-1.5 py-0.5 rounded border border-[#0B8F63]/25 font-inter">
                    Track 3
                  </span>
                </div>
                <span className="text-[11px] text-[#667085] font-medium truncate font-inter">
                  Payment Recovery Ops
                </span>
              </div>
            </Link>
          </div>
        ) : (
          <Link
            to="/app"
            className="flex items-center justify-center p-1.5 rounded-xl hover:bg-[#F1F4F2] transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-[#0B8F63]"
            onClick={() => setIsMobileOpen(false)}
            aria-label="RecoverAI Home"
            title="RecoverAI"
          >
            <div
              className="w-10 h-10 rounded-xl bg-[#E8F7F0] border border-[#0B8F63]/25 text-[#08704F] flex items-center justify-center shadow-2xs group-hover:bg-[#d8f3e5] transition"
              data-testid="sidebar-collapsed-brand-mark"
            >
              <Zap className="w-5 h-5 text-[#08704F]" aria-hidden="true" />
              <span className="sr-only">RecoverAI</span>
            </div>
          </Link>
        )}

        {/* Mobile close button */}
        <button
          type="button"
          onClick={() => setIsMobileOpen(false)}
          className="md:hidden text-[#98A2B3] hover:text-[#111318] p-1.5 rounded-lg hover:bg-[#F1F4F2] transition cursor-pointer"
          aria-label="Close navigation"
        >
          <X className="w-5 h-5" />
        </button>
      </div>

      {/* Navigation items */}
      <nav aria-label="Sidebar navigation" className="flex-1 py-4 px-2 space-y-1 overflow-y-auto">
        <div className="px-2.5 pb-2 text-[10px] font-semibold uppercase tracking-wider text-[#98A2B3] font-inter">
          {!isCollapsed && 'Platform'}
        </div>

        {navItems.map((item) => {
          const isActive =
            !item.isUpcoming &&
            (location.pathname === item.href ||
              (item.href !== '/app' && location.pathname.startsWith(item.href)));
          const Icon = item.icon;

          if (item.isUpcoming) {
            return (
              <div
                key={item.name}
                className={`flex items-center gap-3 px-3 py-2.5 rounded-lg text-xs font-medium text-[#98A2B3] cursor-not-allowed opacity-70 ${
                  isCollapsed ? 'justify-center px-2' : 'justify-between'
                }`}
                title={`${item.name} (Upcoming)`}
                aria-disabled="true"
              >
                <div className="flex items-center gap-3 truncate">
                  <Icon className="w-4 h-4 text-[#98A2B3] shrink-0" aria-hidden="true" />
                  {!isCollapsed && <span className="truncate">{item.name}</span>}
                </div>
                {!isCollapsed && (
                  <Badge variant="outline" className="text-[10px] py-0 px-1.5 text-[#98A2B3] border-[#E5E9E6]">
                    Upcoming
                  </Badge>
                )}
              </div>
            );
          }

          return (
            <Link
              key={item.name}
              to={item.href}
              onClick={() => setIsMobileOpen(false)}
              aria-current={isActive ? 'page' : undefined}
              className={`group flex items-center gap-3 px-3 py-2.5 rounded-lg text-xs font-medium transition-colors duration-150 relative ${
                isActive
                  ? 'bg-[#E8F7F0] text-[#08704F] font-semibold border border-[#0B8F63]/20 shadow-2xs'
                  : 'text-[#667085] hover:bg-[#F1F4F2] hover:text-[#111318] border border-transparent'
              } ${isCollapsed ? 'justify-center px-2' : ''}`}
              title={isCollapsed ? item.name : undefined}
            >
              {isActive && (
                <span
                  className="absolute left-0 top-1.5 bottom-1.5 w-1 bg-[#0B8F63] rounded-r"
                  aria-hidden="true"
                />
              )}
              <Icon
                className={`w-4 h-4 shrink-0 transition-colors duration-150 ${
                  isActive ? 'text-[#08704F]' : 'text-[#667085] group-hover:text-[#111318]'
                }`}
                aria-hidden="true"
              />
              {!isCollapsed ? (
                <span className="truncate">{item.name}</span>
              ) : (
                <span className="sr-only">{item.name}</span>
              )}
            </Link>
          );
        })}
      </nav>

      {/* Live Engine Status Footer */}
      <div className="p-3 border-t border-[#E5E9E6]">
        {!isCollapsed ? (
          <div className="p-2.5 rounded-lg bg-[#F7F8F6] border border-[#E5E9E6] space-y-1.5">
            <div className="flex items-center justify-between text-[11px]">
              <span className="text-[#111318] font-semibold font-inter">Recovery Engine</span>
              <span className="inline-flex items-center gap-1.5 text-[#0B8F63] font-bold text-[10px] uppercase tracking-wide">
                <Radio className="w-3 h-3 pulse-subtle" aria-hidden="true" />
                Active
              </span>
            </div>
            <div className="text-[10px] text-[#667085] font-mono truncate">
              Gemini 3.7 Flash • Guardrails ON
            </div>
          </div>
        ) : (
          <div
            className="flex justify-center p-2 rounded-lg hover:bg-[#F1F4F2] transition-colors"
            title="Recovery Engine: Active (Gemini 3.7 Flash • Guardrails ON)"
            aria-label="Recovery Engine: Active"
            role="status"
          >
            <Radio className="w-4 h-4 text-[#0B8F63] pulse-subtle" aria-hidden="true" />
            <span className="sr-only">Recovery Engine Active</span>
          </div>
        )}
      </div>

      {/* Desktop Collapse Toggle */}
      <div className="hidden md:flex items-center p-2 border-t border-[#E5E9E6]">
        <button
          type="button"
          onClick={() => setIsCollapsed((prev) => !prev)}
          className="w-full flex items-center justify-center gap-2 p-2 rounded-lg text-[#667085] hover:text-[#111318] hover:bg-[#F1F4F2] transition-colors duration-150 text-xs font-medium cursor-pointer focus:outline-none focus-visible:ring-2 focus-visible:ring-[#0B8F63]"
          aria-label={isCollapsed ? 'Expand sidebar' : 'Collapse sidebar'}
        >
          {isCollapsed ? (
            <ChevronRight className="w-4 h-4 text-[#667085]" aria-hidden="true" />
          ) : (
            <>
              <ChevronLeft className="w-4 h-4 text-[#667085]" aria-hidden="true" />
              <span className="text-xs text-[#667085]">Collapse sidebar</span>
            </>
          )}
        </button>
      </div>
    </div>
  );

  return (
    <>
      {/* Desktop Sidebar */}
      <aside
        className={`hidden md:block shrink-0 transition-all duration-200 ${
          isCollapsed ? 'w-16' : 'w-64'
        }`}
      >
        <div className="fixed inset-y-0 left-0 z-30 transition-all duration-200">
          <div className={`h-full ${isCollapsed ? 'w-16' : 'w-64'}`}>{sidebarContent}</div>
        </div>
      </aside>

      {/* Mobile Drawer */}
      {isMobileOpen && (
        <div className="fixed inset-0 z-50 md:hidden flex">
          {/* Backdrop */}
          <div
            className="fixed inset-0 bg-slate-900/40 backdrop-blur-xs transition-opacity animate-in fade-in"
            onClick={() => setIsMobileOpen(false)}
            aria-hidden="true"
          />

          {/* Drawer content */}
          <div className="relative flex-1 flex flex-col max-w-xs w-full bg-white shadow-2xl z-10 animate-in slide-in-from-left duration-200">
            {sidebarContent}
          </div>
        </div>
      )}
    </>
  );
}
