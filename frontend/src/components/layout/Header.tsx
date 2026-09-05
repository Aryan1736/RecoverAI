import { useState, useRef, useEffect } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Menu, LogOut, ShieldCheck, ChevronDown, Bell } from 'lucide-react';
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
    <header className="h-16 shrink-0 bg-white/95 backdrop-blur-md border-b border-[#E5E9E6] px-4 sm:px-6 flex items-center justify-between sticky top-0 z-20">
      {/* Left side: mobile toggle + page breadcrumb */}
      <div className="flex items-center gap-3">
        <button
          type="button"
          onClick={onOpenMobileMenu}
          className="md:hidden p-2 rounded-lg text-[#667085] hover:text-[#111318] hover:bg-[#F1F4F2] transition-colors cursor-pointer focus:outline-none focus-visible:ring-2 focus-visible:ring-[#0B8F63]"
          aria-label="Open mobile navigation menu"
        >
          <Menu className="w-5 h-5" aria-hidden="true" />
        </button>

        <nav aria-label="Breadcrumb" className="flex items-center gap-2 text-xs font-medium font-inter">
          <span className="text-[#667085]">Merchant Portal</span>
          <span className="text-[#D1D7D3]" aria-hidden="true">/</span>
          <span className="text-[#111318] font-semibold tracking-tight" aria-current="page">
            {getBreadcrumbTitle()}
          </span>
        </nav>
      </div>

      {/* Right side: Demo badge, Backend health indicator & Merchant Account Dropdown */}
      <div className="flex items-center gap-2.5 sm:gap-3.5">
        {/* Demo Mode persistent badge */}
        {isDemoMode && <DemoModeBadge onExit={handleExitDemo} />}

        {/* Backend health status badge */}
        <div
          className={`hidden sm:flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-mono border transition-colors ${
            backendStatus === 'UP'
              ? 'bg-[#E8F7F0] border-[#0B8F63]/25 text-[#08704F]'
              : backendStatus === 'OFFLINE'
              ? 'bg-[#FEE2E2] border-[#DC2626]/25 text-[#DC2626]'
              : 'bg-[#FEF3C7] border-[#D97706]/25 text-[#92400E]'
          }`}
          title={`Backend API status: ${backendStatus}`}
          role="status"
          aria-label={`API Status: ${backendStatus}`}
        >
          <span
            className={`w-1.5 h-1.5 rounded-full shrink-0 ${
              backendStatus === 'UP'
                ? 'bg-[#0B8F63]'
                : backendStatus === 'OFFLINE'
                ? 'bg-[#DC2626]'
                : 'bg-[#D97706] animate-pulse'
            }`}
            aria-hidden="true"
          />
          <span className="opacity-75">API:</span>
          <span className="font-semibold">{backendStatus}</span>
        </div>

        {/* Notification indicator button */}
        <Link
          to="/notifications"
          className="relative p-2 rounded-lg text-[#667085] hover:text-[#111318] hover:bg-[#F1F4F2] border border-transparent hover:border-[#E5E9E6] transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-[#0B8F63] cursor-pointer"
          aria-label={unreadCount > 0 ? `Notifications (${unreadCount} unread)` : 'Notifications'}
          title={unreadCount > 0 ? `${unreadCount} unread notification${unreadCount > 1 ? 's' : ''}` : 'Notifications'}
        >
          <Bell className="w-4 h-4" aria-hidden="true" />
          {unreadCount > 0 && (
            <span
              className="absolute -top-0.5 -right-0.5 flex items-center justify-center min-w-[16px] h-4 px-1 rounded-full bg-[#0B8F63] text-[10px] font-bold text-white shadow-2xs ring-2 ring-white"
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
            className="flex items-center gap-2.5 p-1.5 rounded-lg hover:bg-[#F1F4F2] border border-transparent hover:border-[#E5E9E6] transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-[#0B8F63] cursor-pointer"
            aria-expanded={dropdownOpen}
            aria-haspopup="true"
            aria-label="Merchant account menu"
          >
            <Avatar name={isDemoMode ? 'Demo Evaluator' : user?.name || 'Merchant'} size="sm" />
            <div className="hidden md:flex flex-col text-left">
              <span className="text-xs font-semibold text-[#111318] leading-none truncate max-w-[140px]">
                {isDemoMode ? 'Demo Evaluator' : user?.name || 'Merchant'}
              </span>
              <span className="text-[10px] text-[#667085] truncate max-w-[140px] mt-0.5">
                {isDemoMode ? 'demo@recoverai.local' : user?.email}
              </span>
            </div>
            <ChevronDown
              className={`w-3.5 h-3.5 text-[#98A2B3] transition-transform duration-150 ${
                dropdownOpen ? 'rotate-180' : ''
              }`}
              aria-hidden="true"
            />
          </button>

          {/* Account Dropdown Menu */}
          {dropdownOpen && (
            <div className="absolute right-0 mt-2 w-64 rounded-xl bg-white border border-[#E5E9E6] shadow-lg py-1.5 z-50 animate-in fade-in slide-in-from-top-1">
              <div className="px-3.5 py-2.5 border-b border-[#E5E9E6] space-y-1">
                <p className="text-xs font-bold text-[#111318] truncate">
                  {isDemoMode ? 'Demo Evaluator' : user?.name}
                </p>
                <p className="text-[11px] text-[#667085] truncate font-mono">
                  {isDemoMode ? 'demo@recoverai.local (Simulated)' : user?.email}
                </p>
                <div className="flex items-center gap-2 pt-1">
                  <Badge variant={isDemoMode ? 'warning' : 'success'} dot className="text-[10px] py-0 px-1.5">
                    {isDemoMode ? 'DEMO MODE' : user?.status || 'ACTIVE'}
                  </Badge>
                  {!isDemoMode && user?.razorpayAccountId && (
                    <span className="text-[10px] text-[#667085] font-mono bg-[#F7F8F6] px-1.5 py-0.5 rounded border border-[#E5E9E6] truncate">
                      {user.razorpayAccountId}
                    </span>
                  )}
                  {isDemoMode && (
                    <span className="text-[10px] text-[#92400E] font-mono bg-[#FEF9EE] px-1.5 py-0.5 rounded border border-[#FBEAC8] truncate">
                      Sandbox
                    </span>
                  )}
                </div>
              </div>

              <div className="px-1.5 py-1">
                <div className="px-2 py-1.5 text-[11px] text-[#667085] flex items-center gap-2">
                  <ShieldCheck className="w-3.5 h-3.5 text-[#0B8F63]" aria-hidden="true" />
                  <span>
                    {isDemoMode ? 'Mode: Simulated Sandbox' : 'Tenant ID: '}
                    {!isDemoMode && (
                      <code className="text-[#111318] font-mono">
                        {user?.id ? `${user.id.slice(0, 8)}...` : 'N/A'}
                      </code>
                    )}
                  </span>
                </div>
              </div>

              <div className="border-t border-[#E5E9E6] px-1.5 pt-1">
                {isDemoMode ? (
                  <button
                    type="button"
                    onClick={handleExitDemo}
                    className="w-full flex items-center gap-2 px-2.5 py-2 rounded-lg text-xs font-medium text-[#92400E] hover:bg-[#FEF9EE] transition-colors cursor-pointer text-left focus:outline-none focus-visible:ring-2 focus-visible:ring-amber-500"
                  >
                    <LogOut className="w-4 h-4" aria-hidden="true" />
                    Exit Demo Mode
                  </button>
                ) : (
                  <button
                    type="button"
                    onClick={handleLogout}
                    className="w-full flex items-center gap-2 px-2.5 py-2 rounded-lg text-xs font-medium text-[#DC2626] hover:bg-[#FEE2E2] transition-colors cursor-pointer text-left focus:outline-none focus-visible:ring-2 focus-visible:ring-red-500"
                  >
                    <LogOut className="w-4 h-4" aria-hidden="true" />
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

