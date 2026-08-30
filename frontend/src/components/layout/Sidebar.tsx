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
  { name: 'Notifications', href: '#notifications', icon: Bell, isUpcoming: true },
  { name: 'Settings', href: '#settings', icon: Settings, isUpcoming: true },
];

export function Sidebar({
  isCollapsed,
  setIsCollapsed,
  isMobileOpen,
  setIsMobileOpen,
}: SidebarProps) {
  const location = useLocation();

  const sidebarContent = (
    <div className="flex flex-col h-full bg-slate-950/95 border-r border-slate-800 text-slate-300 select-none">
      {/* Brand Header */}
      <div className="h-16 flex items-center justify-between px-4 border-b border-slate-800/80">
        <Link
          to="/app"
          className="flex items-center gap-2.5 group overflow-hidden focus:outline-none"
          onClick={() => setIsMobileOpen(false)}
        >
          <div className="w-9 h-9 rounded-xl bg-indigo-600/20 border border-indigo-500/30 text-indigo-400 flex items-center justify-center group-hover:bg-indigo-600/30 transition shrink-0">
            <Zap className="w-5 h-5 text-indigo-400" />
          </div>
          {!isCollapsed && (
            <div className="flex flex-col truncate">
              <span className="font-bold text-sm tracking-tight text-white flex items-center gap-1.5">
                RecoverAI
                <span className="text-[10px] uppercase font-semibold tracking-wider text-indigo-400 bg-indigo-500/10 px-1.5 py-0.5 rounded border border-indigo-500/20">
                  Track 3
                </span>
              </span>
              <span className="text-[11px] text-slate-400 truncate">Payment Recovery Ops</span>
            </div>
          )}
        </Link>

        {/* Mobile close button */}
        <button
          onClick={() => setIsMobileOpen(false)}
          className="md:hidden text-slate-400 hover:text-white p-1 rounded-lg hover:bg-slate-800 transition"
          aria-label="Close navigation"
        >
          <X className="w-5 h-5" />
        </button>
      </div>

      {/* Navigation items */}
      <div className="flex-1 py-4 px-2 space-y-1.5 overflow-y-auto">
        <div className="px-2 pb-2 text-[10px] font-semibold uppercase tracking-wider text-slate-300">
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
                className={`flex items-center gap-3 px-3 py-2 rounded-lg text-xs font-medium text-slate-300 cursor-not-allowed opacity-75 ${
                  isCollapsed ? 'justify-center' : 'justify-between'
                }`}
                title={`${item.name} (Available in PR #21)`}
              >
                <div className="flex items-center gap-3 truncate">
                  <Icon className="w-4 h-4 text-slate-300 shrink-0" aria-hidden="true" />
                  {!isCollapsed && <span className="truncate">{item.name}</span>}
                </div>
                {!isCollapsed && (
                  <Badge variant="outline" className="text-[10px] py-0 px-1.5 text-slate-300">
                    PR #21
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
              className={`flex items-center gap-3 px-3 py-2 rounded-lg text-xs font-medium transition-colors ${
                isActive
                  ? 'bg-indigo-600/15 text-indigo-300 border border-indigo-500/30'
                  : 'text-slate-300 hover:bg-slate-900 hover:text-white'
              } ${isCollapsed ? 'justify-center' : ''}`}
              title={item.name}
            >
              <Icon
                className={`w-4 h-4 shrink-0 ${isActive ? 'text-indigo-400' : 'text-slate-400'}`}
                aria-hidden="true"
              />
              {!isCollapsed && <span className="truncate">{item.name}</span>}
            </Link>
          );
        })}
      </div>

      {/* Live Engine Status Footer */}
      <div className="p-3 border-t border-slate-800/80">
        {!isCollapsed ? (
          <div className="p-2.5 rounded-lg bg-slate-900/80 border border-slate-800/90 space-y-1">
            <div className="flex items-center justify-between text-[11px]">
              <span className="text-slate-400 font-medium">Recovery Engine</span>
              <span className="inline-flex items-center gap-1 text-emerald-400 font-medium">
                <Radio className="w-3 h-3 animate-pulse" />
                Active
              </span>
            </div>
            <div className="text-[10px] text-slate-500 font-mono truncate">
              Gemini 3.7 Flash • Guardrails ON
            </div>
          </div>
        ) : (
          <div className="flex justify-center" title="Recovery Engine Active">
            <Radio className="w-3.5 h-3.5 text-emerald-400 animate-pulse" />
          </div>
        )}
      </div>

      {/* Desktop Collapse Toggle */}
      <div className="hidden md:flex items-center justify-end p-2 border-t border-slate-800/80">
        <button
          onClick={() => setIsCollapsed((prev) => !prev)}
          className="w-full flex items-center justify-center p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-900 transition text-xs font-medium cursor-pointer"
          aria-label={isCollapsed ? 'Expand sidebar' : 'Collapse sidebar'}
        >
          {isCollapsed ? (
            <ChevronRight className="w-4 h-4" />
          ) : (
            <div className="flex items-center gap-2">
              <ChevronLeft className="w-4 h-4" />
              <span className="text-xs text-slate-400">Collapse</span>
            </div>
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
            className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm transition-opacity animate-in fade-in"
            onClick={() => setIsMobileOpen(false)}
            aria-hidden="true"
          />

          {/* Drawer content */}
          <div className="relative flex-1 flex flex-col max-w-xs w-full bg-slate-950 shadow-2xl z-10 animate-in slide-in-from-left duration-200">
            {sidebarContent}
          </div>
        </div>
      )}
    </>
  );
}
