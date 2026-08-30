import {
  Shield,
  Building2,
  Key,
  Calendar,
  CheckCircle2,
  CreditCard,
  Lock,
} from 'lucide-react';
import { Card } from '../../components/ui/Card';
import { Badge } from '../../components/ui/Badge';
import { useAuth } from '../../hooks/useAuth';

function formatDate(isoString?: string | null): string {
  if (!isoString) return 'N/A';
  try {
    const d = new Date(isoString);
    if (isNaN(d.getTime())) return isoString;
    return d.toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false,
    });
  } catch {
    return isoString;
  }
}

export function GeneralSettingsPage() {
  const { user } = useAuth();

  return (
    <div className="space-y-6 max-w-4xl">
      {/* Merchant Profile Card */}
      <Card className="border-slate-800 space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-800">
          <div className="flex items-center gap-3.5">
            <div className="w-12 h-12 rounded-2xl bg-indigo-600/20 border border-indigo-500/30 text-indigo-400 flex items-center justify-center font-bold text-lg">
              {user?.name ? user.name.charAt(0).toUpperCase() : 'M'}
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-base font-bold text-white">{user?.name || 'Merchant Account'}</h3>
                <Badge variant="success" dot>
                  {user?.status || 'ACTIVE'}
                </Badge>
              </div>
              <p className="text-xs text-slate-400 mt-0.5">{user?.email}</p>
            </div>
          </div>
        </div>

        {/* Account Details Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div className="p-3.5 rounded-xl bg-slate-950/60 border border-slate-800/80 space-y-1">
            <div className="flex items-center gap-1.5 text-xs text-slate-400">
              <Building2 className="w-3.5 h-3.5 text-indigo-400" />
              <span>Merchant Identity</span>
            </div>
            <p className="text-sm font-semibold text-white">{user?.name || '—'}</p>
          </div>

          <div className="p-3.5 rounded-xl bg-slate-950/60 border border-slate-800/80 space-y-1">
            <div className="flex items-center gap-1.5 text-xs text-slate-400">
              <Shield className="w-3.5 h-3.5 text-indigo-400" />
              <span>Tenant ID (UUID)</span>
            </div>
            <p className="text-xs font-mono text-slate-200 truncate select-all">
              {user?.id || '—'}
            </p>
          </div>

          <div className="p-3.5 rounded-xl bg-slate-950/60 border border-slate-800/80 space-y-1">
            <div className="flex items-center gap-1.5 text-xs text-slate-400">
              <CreditCard className="w-3.5 h-3.5 text-indigo-400" />
              <span>Razorpay Account ID</span>
            </div>
            <p className="text-xs font-mono text-slate-200">
              {user?.razorpayAccountId ? (
                <span className="text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded border border-emerald-500/20">
                  {user.razorpayAccountId}
                </span>
              ) : (
                <span className="text-slate-500 italic">Not configured</span>
              )}
            </p>
          </div>

          <div className="p-3.5 rounded-xl bg-slate-950/60 border border-slate-800/80 space-y-1">
            <div className="flex items-center gap-1.5 text-xs text-slate-400">
              <Calendar className="w-3.5 h-3.5 text-indigo-400" />
              <span>Account Created</span>
            </div>
            <p className="text-xs text-slate-300 font-mono">
              {formatDate(user?.createdAt)}
            </p>
          </div>
        </div>

        {/* Read-Only Notice */}
        <div className="p-3.5 rounded-xl bg-slate-950 border border-slate-800/80 flex items-start gap-2.5">
          <Lock className="w-4 h-4 text-slate-400 shrink-0 mt-0.5" />
          <div className="text-xs text-slate-400 space-y-0.5">
            <span className="font-semibold text-slate-300">Account Profile Governance</span>
            <p>
              Merchant identity and payment processor integrations are locked to maintain tenant security and auditing standards. Contact platform operations to modify merchant profile fields.
            </p>
          </div>
        </div>
      </Card>

      {/* Session & Security Info */}
      <Card className="border-slate-800 space-y-4">
        <div className="flex items-center gap-2 pb-3 border-b border-slate-800">
          <Key className="w-4 h-4 text-indigo-400" />
          <h3 className="text-sm font-semibold text-white">Security & Active Session</h3>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
          <div className="p-3 rounded-xl bg-slate-950/60 border border-slate-800/80 space-y-1">
            <span className="text-slate-400 font-medium">Authentication Protocol</span>
            <div className="flex items-center gap-1.5 text-slate-200">
              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
              <span className="font-mono">Stateless JWT (HMAC-SHA256)</span>
            </div>
          </div>

          <div className="p-3 rounded-xl bg-slate-950/60 border border-slate-800/80 space-y-1">
            <span className="text-slate-400 font-medium">Tenant Isolation</span>
            <div className="flex items-center gap-1.5 text-slate-200">
              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
              <span>Strict Merchant Header & Claim Scoping</span>
            </div>
          </div>
        </div>
      </Card>
    </div>
  );
}
