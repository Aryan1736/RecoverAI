import { useState, useRef, useEffect } from 'react';
import { Menu, LogOut, ShieldCheck, ChevronDown, Activity } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import { useToast } from '../../hooks/useToast';
import { Avatar } from '../ui/Avatar';
import { Badge } from '../ui/Badge';
import { fetchHealth } from '../../api/auth';

export interface HeaderProps {
  onOpenMobileMenu: () => void;
}

export function Header({ onOpenMobileMenu }: HeaderProps) {
  const { user, logout } = useAuth();
  const { toast } = useToast();
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [backendStatus, setBackendStatus] = useState<'UP' | 'OFFLINE' | 'CHECKING'>('CHECKING');
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

  return (
    <header className="h-16 shrink-0 bg-slate-950/80 backdrop-blur-md border-b border-slate-800/80 px-4 sm:px-6 flex items-center justify-between sticky top-0 z-20">
      {/* Left side: mobile toggle + page breadcrumb */}
      <div className="flex items-center gap-3">
        <button
          type="button"
          onClick={onOpenMobileMenu}
          className="md:hidden p-2 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition cursor-pointer"
          aria-label="Open mobile navigation menu"
        >
          <Menu className="w-5 h-5" />
        </button>

        <div className="flex items-center gap-2 text-xs font-medium">
          <span className="text-slate-400">Merchant Portal</span>
          <span className="text-slate-600">/</span>
          <span className="text-slate-200 font-semibold">Overview</span>
        </div>
      </div>

      {/* Right side: Backend health indicator & Merchant Account Dropdown */}
      <div className="flex items-center gap-3 sm:gap-4">
        {/* Backend health status badge */}
        <div className="hidden sm:flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-slate-900 border border-slate-800 text-[11px] font-mono">
          <Activity
            className={`w-3.5 h-3.5 ${
              backendStatus === 'UP'
                ? 'text-emerald-400'
                : backendStatus === 'OFFLINE'
                ? 'text-rose-400'
                : 'text-amber-400 animate-spin'
            }`}
          />
          <span className="text-slate-400">API:</span>
          <span
            className={
              backendStatus === 'UP'
                ? 'text-emerald-400 font-semibold'
                : backendStatus === 'OFFLINE'
                ? 'text-rose-400 font-semibold'
                : 'text-amber-400'
            }
          >
            {backendStatus}
          </span>
        </div>

        {/* Merchant Account Menu */}
        <div className="relative" ref={dropdownRef}>
          <button
            type="button"
            onClick={() => setDropdownOpen((prev) => !prev)}
            className="flex items-center gap-2.5 p-1.5 rounded-xl hover:bg-slate-900/90 border border-transparent hover:border-slate-800 transition focus:outline-none focus:ring-2 focus:ring-indigo-500 cursor-pointer"
            aria-expanded={dropdownOpen}
            aria-haspopup="true"
            aria-label="Merchant account menu"
          >
            <Avatar name={user?.name || 'Merchant'} size="sm" />
            <div className="hidden md:flex flex-col text-left">
              <span className="text-xs font-semibold text-white leading-none truncate max-w-[140px]">
                {user?.name || 'Merchant'}
              </span>
              <span className="text-[10px] text-slate-400 truncate max-w-[140px]">
                {user?.email}
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
            <div className="absolute right-0 mt-2 w-64 rounded-xl bg-slate-900 border border-slate-800 shadow-2xl py-1.5 z-50 animate-in fade-in slide-in-from-top-1">
              <div className="px-3.5 py-2.5 border-b border-slate-800/80 space-y-1">
                <p className="text-xs font-bold text-white truncate">{user?.name}</p>
                <p className="text-[11px] text-slate-400 truncate font-mono">{user?.email}</p>
                <div className="flex items-center gap-2 pt-1">
                  <Badge variant="success" dot className="text-[10px] py-0 px-1.5">
                    {user?.status || 'ACTIVE'}
                  </Badge>
                  {user?.razorpayAccountId && (
                    <span className="text-[10px] text-slate-400 font-mono bg-slate-950 px-1.5 py-0.5 rounded border border-slate-800 truncate">
                      {user.razorpayAccountId}
                    </span>
                  )}
                </div>
              </div>

              <div className="px-1.5 py-1">
                <div className="px-2 py-1.5 text-[11px] text-slate-400 flex items-center gap-2">
                  <ShieldCheck className="w-3.5 h-3.5 text-indigo-400" />
                  <span>Tenant ID: <code className="text-slate-300 font-mono">{user?.id ? `${user.id.slice(0, 8)}...` : 'N/A'}</code></span>
                </div>
              </div>

              <div className="border-t border-slate-800/80 px-1.5 pt-1">
                <button
                  type="button"
                  onClick={handleLogout}
                  className="w-full flex items-center gap-2 px-2.5 py-2 rounded-lg text-xs font-medium text-rose-400 hover:bg-rose-500/10 hover:text-rose-300 transition cursor-pointer text-left"
                >
                  <LogOut className="w-4 h-4" />
                  Sign Out
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
