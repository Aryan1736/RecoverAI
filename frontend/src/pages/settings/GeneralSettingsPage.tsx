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
      <Card className="border-slate-200 shadow-2xs space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-100">
          <div className="flex items-center gap-3.5">
            <div className="w-12 h-12 rounded-2xl bg-emerald-50 border border-emerald-200 text-emerald-700 flex items-center justify-center font-bold text-lg shadow-2xs">
              {user?.name ? user.name.charAt(0).toUpperCase() : 'M'}
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-base font-bold text-slate-900">{user?.name || 'Merchant Account'}</h3>
                <Badge variant="success" dot>
                  {user?.status || 'ACTIVE'}
                </Badge>
              </div>
              <p className="text-xs text-slate-500 mt-0.5">{user?.email}</p>
            </div>
          </div>
        </div>

        {/* Account Details Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200 space-y-1">
            <div className="flex items-center gap-1.5 text-xs text-slate-500">
              <Building2 className="w-3.5 h-3.5 text-emerald-600" />
              <span>Merchant Identity</span>
            </div>
            <p className="text-sm font-semibold text-slate-900">{user?.name || '—'}</p>
          </div>

          <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200 space-y-1">
            <div className="flex items-center gap-1.5 text-xs text-slate-500">
              <Shield className="w-3.5 h-3.5 text-emerald-600" />
              <span>Tenant ID (UUID)</span>
            </div>
            <p className="text-xs font-mono text-slate-800 truncate select-all">
              {user?.id || '—'}
            </p>
          </div>

          <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200 space-y-1">
            <div className="flex items-center gap-1.5 text-xs text-slate-500">
              <CreditCard className="w-3.5 h-3.5 text-emerald-600" />
              <span>Razorpay Account ID</span>
            </div>
            <p className="text-xs font-mono text-slate-800">
              {user?.razorpayAccountId ? (
                <span className="text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded border border-emerald-200 font-semibold">
                  {user.razorpayAccountId}
                </span>
              ) : (
                <span className="text-slate-400 italic">Not configured</span>
              )}
            </p>
          </div>

          <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200 space-y-1">
            <div className="flex items-center gap-1.5 text-xs text-slate-500">
              <Calendar className="w-3.5 h-3.5 text-emerald-600" />
              <span>Account Created</span>
            </div>
            <p className="text-xs text-slate-700 font-mono">
              {formatDate(user?.createdAt)}
            </p>
          </div>
        </div>

        {/* Read-Only Notice */}
        <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200 flex items-start gap-2.5">
          <Lock className="w-4 h-4 text-slate-400 shrink-0 mt-0.5" />
          <div className="text-xs text-slate-600 space-y-0.5">
            <span className="font-semibold text-slate-900">Account Profile Governance</span>
            <p className="text-slate-500">
              Merchant identity and payment processor integrations are locked to maintain tenant security and auditing standards. Contact platform operations to modify merchant profile fields.
            </p>
          </div>
        </div>
      </Card>

      {/* Session & Security Info */}
      <Card className="border-slate-200 shadow-2xs space-y-4">
        <div className="flex items-center gap-2 pb-3 border-b border-slate-100">
          <Key className="w-4 h-4 text-emerald-600" />
          <h3 className="text-sm font-semibold text-slate-900">Security &amp; Active Session</h3>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
          <div className="p-3 rounded-xl bg-slate-50 border border-slate-200 space-y-1">
            <span className="text-slate-500 font-medium">Authentication Protocol</span>
            <div className="flex items-center gap-1.5 text-slate-800">
              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
              <span className="font-mono">Stateless JWT (HMAC-SHA256)</span>
            </div>
          </div>

          <div className="p-3 rounded-xl bg-slate-50 border border-slate-200 space-y-1">
            <span className="text-slate-500 font-medium">Tenant Isolation</span>
            <div className="flex items-center gap-1.5 text-slate-800">
              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
              <span>Strict Merchant Header &amp; Claim Scoping</span>
            </div>
          </div>
        </div>
      </Card>
    </div>
  );
}
