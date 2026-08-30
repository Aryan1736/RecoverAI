import { useState, useRef, useEffect } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Menu, LogOut, ShieldCheck, ChevronDown, Activity, Bell } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import { useDemoMode } from '../../hooks/useDemoMode';
import { useToast } from '../../hooks/useToast';
import { Avatar } from '../ui/Avatar';
import { Badge } from '../ui/Badge';
import { DemoModeBadge } from './DemoModeBadge';
import { fetchHealth } from '../../api/auth';
import { getUnreadCount } from '../../api/notifications';
import { getDemoUnreadCount, DEMO_STATE_EVENT } from '../../api/demo';

export interface HeaderProps {
  onOpenMobileMenu: () => void;
}

export function Header({ onOpenMobileMenu }: HeaderProps) {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, isDemoMode, logout } = useAuth();
  const { exitDemoMode } = useDemoMode();
  const { toast } = useToast();
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [backendStatus, setBackendStatus] = useState<'UP' | 'OFFLINE' | 'CHECKING'>('CHECKING');
  const [unreadCount, setUnreadCount] = useState<number>(0);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // Check backend health on mount
  useEffect(() => {
    let mounted = true;
    fetchHealth()
      .then((res) => {
        if (mounted && res.status === 'UP') {
          setBackendStatus('UP');
        }
      })
      .catch(() => {
        if (mounted) setBackendStatus('OFFLINE');
      });
    return () => {
      mounted = false;
    };
  }, []);

  // Fetch unread notification count on mount and route changes
  useEffect(() => {
    let mounted = true;
    if (isDemoMode) {
      const updateUnread = () => {
        getDemoUnreadCount()
          .then((count) => {
            if (mounted) setUnreadCount(count);
          })
          .catch(() => {
            if (mounted) setUnreadCount(0);
          });
      };

      updateUnread();
      window.addEventListener(DEMO_STATE_EVENT, updateUnread);
      return () => {
        mounted = false;
        window.removeEventListener(DEMO_STATE_EVENT, updateUnread);
      };
    }

    getUnreadCount()
      .then((count) => {
        if (mounted) setUnreadCount(count);
      })
      .catch(() => {
        if (mounted) setUnreadCount(0);
      });
    return () => {
      mounted = false;
    };
  }, [location.pathname, isDemoMode]);

  // Close dropdown on outside click
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setDropdownOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleLogout = () => {
    setDropdownOpen(false);
    logout();
    toast.info('You have been signed out.');
  };

  const handleExitDemo = () => {
    setDropdownOpen(false);
    exitDemoMode();
    toast.info('Exited demo mode.');
    navigate('/login');
  };

  const getBreadcrumbTitle = () => {
    const path = location.pathname;
    if (path.startsWith('/notifications')) return 'Notifications';
    if (path.startsWith('/settings')) return 'Settings & Operations';
    if (path.startsWith('/recovery-cases')) return 'Recovery Cases';
    if (path.startsWith('/analytics')) return 'Analytics';
    return 'Overview';
  };

  return (
    <header className="h-16 shrink-0 bg-white/95 backdrop-blur-md border-b border-slate-200 px-4 sm:px-6 flex items-center justify-between sticky top-0 z-20">
      {/* Left side: mobile toggle + page breadcrumb */}
      <div className="flex items-center gap-3">
        <button
          type="button"
          onClick={onOpenMobileMenu}
          className="md:hidden p-2 rounded-lg text-slate-500 hover:text-slate-800 hover:bg-slate-100 transition cursor-pointer"
          aria-label="Open mobile navigation menu"
        >
          <Menu className="w-5 h-5" />
        </button>

        <div className="flex items-center gap-2 text-xs font-medium">
          <span className="text-slate-400">Merchant Portal</span>
          <span className="text-slate-300">/</span>
          <span className="text-slate-800 font-semibold">{getBreadcrumbTitle()}</span>
        </div>
      </div>

      {/* Right side: Demo badge, Backend health indicator & Merchant Account Dropdown */}
      <div className="flex items-center gap-2.5 sm:gap-4">
        {/* Demo Mode persistent badge */}
        {isDemoMode && <DemoModeBadge onExit={handleExitDemo} />}

        {/* Backend health status badge */}
        <div className="hidden sm:flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-slate-50 border border-slate-200 text-[11px] font-mono">
          <Activity
            className={`w-3.5 h-3.5 ${
              backendStatus === 'UP'
                ? 'text-emerald-600'
                : backendStatus === 'OFFLINE'
                ? 'text-rose-600'
                : 'text-amber-500 animate-spin'
            }`}
          />
          <span className="text-slate-500">API:</span>
          <span
            className={
              backendStatus === 'UP'
                ? 'text-emerald-600 font-semibold'
                : backendStatus === 'OFFLINE'
                ? 'text-rose-600 font-semibold'
                : 'text-amber-600 font-semibold'
            }
          >
            {backendStatus}
          </span>
        </div>

        {/* Notification indicator button */}
        <Link
          to="/notifications"
          className="relative p-2 rounded-xl text-slate-500 hover:text-slate-800 hover:bg-slate-100 border border-transparent hover:border-slate-200 transition focus:outline-none focus:ring-2 focus:ring-emerald-500 cursor-pointer"
          aria-label={unreadCount > 0 ? `Notifications (${unreadCount} unread)` : 'Notifications'}
          title={unreadCount > 0 ? `${unreadCount} unread notification${unreadCount > 1 ? 's' : ''}` : 'Notifications'}
        >
          <Bell className="w-4 h-4" />
          {unreadCount > 0 && (
            <span
              className="absolute -top-0.5 -right-0.5 flex items-center justify-center min-w-[16px] h-4 px-1 rounded-full bg-emerald-600 text-[10px] font-bold text-white shadow-2xs ring-2 ring-white"
              data-testid="notification-unread-badge"
            >
              {unreadCount > 99 ? '99+' : unreadCount}
            </span>
          )}
        </Link>

        {/* Merchant Account Menu */}
        <div className="relative" ref={dropdownRef}>
          <button
            type="button"
            onClick={() => setDropdownOpen((prev) => !prev)}
            className="flex items-center gap-2.5 p-1.5 rounded-xl hover:bg-slate-50 border border-transparent hover:border-slate-200 transition focus:outline-none focus:ring-2 focus:ring-emerald-500 cursor-pointer"
            aria-expanded={dropdownOpen}
            aria-haspopup="true"
            aria-label="Merchant account menu"
          >
            <Avatar name={isDemoMode ? 'Demo Evaluator' : user?.name || 'Merchant'} size="sm" />
            <div className="hidden md:flex flex-col text-left">
              <span className="text-xs font-semibold text-slate-900 leading-none truncate max-w-[140px]">
                {isDemoMode ? 'Demo Evaluator' : user?.name || 'Merchant'}
              </span>
              <span className="text-[10px] text-slate-500 truncate max-w-[140px]">
                {isDemoMode ? 'demo@recoverai.local' : user?.email}
              </span>
            </div>
            <ChevronDown
              className={`w-3.5 h-3.5 text-slate-400 transition-transform duration-150 ${
                dropdownOpen ? 'rotate-180' : ''
              }`}
            />
          </button>

          {/* Account Dropdown Menu */}
          {dropdownOpen && (
            <div className="absolute right-0 mt-2 w-64 rounded-xl bg-white border border-slate-200 shadow-xl py-1.5 z-50 animate-in fade-in slide-in-from-top-1">
              <div className="px-3.5 py-2.5 border-b border-slate-100 space-y-1">
                <p className="text-xs font-bold text-slate-900 truncate">
                  {isDemoMode ? 'Demo Evaluator' : user?.name}
                </p>
                <p className="text-[11px] text-slate-500 truncate font-mono">
                  {isDemoMode ? 'demo@recoverai.local (Simulated)' : user?.email}
                </p>
                <div className="flex items-center gap-2 pt-1">
                  <Badge variant={isDemoMode ? 'warning' : 'success'} dot className="text-[10px] py-0 px-1.5">
                    {isDemoMode ? 'DEMO MODE' : user?.status || 'ACTIVE'}
                  </Badge>
                  {!isDemoMode && user?.razorpayAccountId && (
                    <span className="text-[10px] text-slate-600 font-mono bg-slate-50 px-1.5 py-0.5 rounded border border-slate-200 truncate">
                      {user.razorpayAccountId}
                    </span>
                  )}
                  {isDemoMode && (
                    <span className="text-[10px] text-amber-800 font-mono bg-amber-50 px-1.5 py-0.5 rounded border border-amber-200 truncate">
                      Sandbox
                    </span>
                  )}
                </div>
              </div>

              <div className="px-1.5 py-1">
                <div className="px-2 py-1.5 text-[11px] text-slate-600 flex items-center gap-2">
                  <ShieldCheck className="w-3.5 h-3.5 text-emerald-600" />
                  <span>
                    {isDemoMode ? 'Mode: Simulated Sandbox' : 'Tenant ID: '}
                    {!isDemoMode && (
                      <code className="text-slate-800 font-mono">
                        {user?.id ? `${user.id.slice(0, 8)}...` : 'N/A'}
                      </code>
                    )}
                  </span>
                </div>
              </div>

              <div className="border-t border-slate-100 px-1.5 pt-1">
                {isDemoMode ? (
                  <button
                    type="button"
                    onClick={handleExitDemo}
                    className="w-full flex items-center gap-2 px-2.5 py-2 rounded-lg text-xs font-medium text-amber-800 hover:bg-amber-50 transition cursor-pointer text-left"
                  >
                    <LogOut className="w-4 h-4" />
                    Exit Demo Mode
                  </button>
                ) : (
                  <button
                    type="button"
                    onClick={handleLogout}
                    className="w-full flex items-center gap-2 px-2.5 py-2 rounded-lg text-xs font-medium text-rose-600 hover:bg-rose-50 transition cursor-pointer text-left"
                  >
                    <LogOut className="w-4 h-4" />
                    Sign Out
                  </button>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
