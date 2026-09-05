import { useState } from 'react';
import {
  Shield,
  Building2,
  Key,
  Calendar,
  CheckCircle2,
  CreditCard,
  Lock,
  Mail,
  Copy,
  Check,
  LogOut,
  Sparkles,
  Activity,
} from 'lucide-react';
import { Badge } from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { Footer } from '../../components/layout/Footer';
import { useAuth } from '../../hooks/useAuth';
import { useDemoMode } from '../../hooks/useDemoMode';

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
  const { user, logout } = useAuth();
  const { isDemoMode } = useDemoMode();
  const [copiedField, setCopiedField] = useState<string | null>(null);

  const handleCopy = async (text: string, fieldName: string) => {
    if (!text || text === '—') return;
    try {
      if (navigator?.clipboard?.writeText) {
        await navigator.clipboard.writeText(text);
      }
      setCopiedField(fieldName);
      setTimeout(() => {
        setCopiedField((current) => (current === fieldName ? null : current));
      }, 2000);
    } catch {
      setCopiedField(fieldName);
      setTimeout(() => {
        setCopiedField((current) => (current === fieldName ? null : current));
      }, 2000);
    }
  };

  const merchantInitial = user?.name ? user.name.charAt(0).toUpperCase() : 'M';

  return (
    <div className="space-y-8 max-w-5xl font-inter animate-console-fade-in delay-1 pb-6">
      {/* ==================================================
          1. MERCHANT ACCOUNT IDENTITY HERO CARD
          ================================================== */}
      <div className="bg-white border border-[#E5E9E6] rounded-xl p-5 sm:p-6 shadow-2xs transition-all">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-5 pb-5 border-b border-[#ECEFEA]">
          <div className="flex items-center gap-4">
            <div className="w-14 h-14 rounded-2xl bg-[#E8F7F0] border border-[#0B8F63]/30 text-[#08704F] flex items-center justify-center font-space-grotesk font-bold text-xl shadow-2xs">
              {merchantInitial}
            </div>
            <div className="space-y-1">
              <div className="flex flex-wrap items-center gap-2.5">
                <h2 className="font-space-grotesk text-lg sm:text-xl font-bold text-[#111318] tracking-tight">
                  {user?.name || 'Merchant Account'}
                </h2>
                <Badge variant="success" dot>
                  {user?.status || 'ACTIVE'}
                </Badge>
                {isDemoMode ? (
                  <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-semibold bg-[#FEF3C7] text-[#D97706] border border-[#D97706]/30">
                    <Sparkles className="w-3 h-3 text-[#D97706]" />
                    <span>Simulated Sandbox</span>
                  </span>
                ) : (
                  <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-semibold bg-[#E8F7F0] text-[#08704F] border border-[#0B8F63]/30">
                    <span className="w-1.5 h-1.5 rounded-full bg-[#0B8F63]" />
                    <span>Production</span>
                  </span>
                )}
              </div>
              <div className="flex items-center gap-2 text-xs text-[#667085]">
                <Mail className="w-3.5 h-3.5 text-[#98A2B3]" />
                <span>{user?.email}</span>
              </div>
            </div>
          </div>

          {/* Quick tenant identifier copy control */}
          <div className="flex items-center gap-2 self-start sm:self-auto bg-[#F1F4F2] px-3 py-1.5 rounded-lg border border-[#E5E9E6]">
            <span className="text-[11px] uppercase tracking-wider font-semibold text-[#667085]">Workspace</span>
            <span className="font-mono text-xs text-[#08704F] font-semibold">Active Tenant</span>
            <button
              type="button"
              onClick={() => handleCopy(user?.id || '', 'hero-tenant')}
              className="p-1 rounded text-[#667085] hover:text-[#0B8F63] hover:bg-white transition-colors focus-ring-green ml-1"
              title="Copy Tenant UUID"
              aria-label="Copy Tenant ID"
            >
              {copiedField === 'hero-tenant' ? (
                <Check className="w-3.5 h-3.5 text-[#0B8F63]" />
              ) : (
                <Copy className="w-3.5 h-3.5" />
              )}
            </button>
          </div>
        </div>

        {/* Identity Meta Strip */}
        <div className="pt-4 grid grid-cols-2 sm:grid-cols-4 gap-4 text-xs">
          <div>
            <span className="text-[#98A2B3] block font-medium text-[11px]">Role / Access</span>
            <span className="font-semibold text-[#111318] mt-0.5 block">Merchant Principal</span>
          </div>
          <div>
            <span className="text-[#98A2B3] block font-medium text-[11px]">Tenant Scope</span>
            <span className="font-semibold text-[#111318] mt-0.5 block">Isolated Tenant</span>
          </div>
          <div>
            <span className="text-[#98A2B3] block font-medium text-[11px]">Payment Gateway</span>
            <span className="font-semibold text-[#111318] mt-0.5 block">
              {user?.razorpayAccountId ? 'Razorpay Linked' : 'Unlinked'}
            </span>
          </div>
          <div>
            <span className="text-[#98A2B3] block font-medium text-[11px]">Onboarding Completed</span>
            <span className="font-semibold text-[#111318] mt-0.5 block font-mono text-[11px]">
              {user?.createdAt ? new Date(user.createdAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }) : 'Verified'}
            </span>
          </div>
        </div>
      </div>

      {/* ==================================================
          2. MERCHANT PROFILE SECTION (2-Column Fintech Settings)
          ================================================== */}
      <section className="grid grid-cols-1 md:grid-cols-12 gap-6 pt-2 border-t border-[#ECEFEA]">
        <div className="md:col-span-4 space-y-1">
          <h3 className="font-space-grotesk text-base font-bold text-[#111318] tracking-tight">
            Merchant Profile
          </h3>
          <p className="text-xs text-[#667085] leading-relaxed">
            Primary merchant identity, account contacts, and platform registration details.
          </p>
        </div>

        <div className="md:col-span-8 space-y-4">
          <div className="bg-white border border-[#E5E9E6] rounded-xl p-5 sm:p-6 shadow-2xs space-y-5">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {/* Merchant Identity */}
              <div className="p-3.5 rounded-xl bg-[#F7F8F6] border border-[#E5E9E6] space-y-1.5">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider text-[#667085]">
                    <Building2 className="w-3.5 h-3.5 text-[#0B8F63]" />
                    <span>Merchant Identity</span>
                  </div>
                  <span className="text-[10px] uppercase font-bold tracking-wider px-1.5 py-0.5 rounded bg-white text-[#667085] border border-[#E5E9E6]">
                    READ ONLY
                  </span>
                </div>
                <div className="flex items-center justify-between pt-1">
                  <p className="text-sm font-semibold text-[#111318]">{user?.name || '—'}</p>
                  <Lock className="w-3.5 h-3.5 text-[#98A2B3]" />
                </div>
              </div>

              {/* Account Governance / Role */}
              <div className="p-3.5 rounded-xl bg-[#F7F8F6] border border-[#E5E9E6] space-y-1.5">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider text-[#667085]">
                    <Activity className="w-3.5 h-3.5 text-[#0B8F63]" />
                    <span>Account Status</span>
                  </div>
                  <span className="text-[10px] uppercase font-bold tracking-wider px-1.5 py-0.5 rounded bg-white text-[#667085] border border-[#E5E9E6]">
                    READ ONLY
                  </span>
                </div>
                <div className="flex items-center justify-between pt-1">
                  <div className="flex items-center gap-1.5">
                    <span className="w-2 h-2 rounded-full bg-[#0B8F63]" />
                    <span className="text-sm text-[#111318] font-semibold">{user?.status || 'ACTIVE'}</span>
                  </div>
                  <Lock className="w-3.5 h-3.5 text-[#98A2B3]" />
                </div>
              </div>

              {/* Tenant ID (UUID) */}
              <div className="p-3.5 rounded-xl bg-[#F7F8F6] border border-[#E5E9E6] space-y-1.5">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider text-[#667085]">
                    <Shield className="w-3.5 h-3.5 text-[#0B8F63]" />
                    <span>Tenant ID (UUID)</span>
                  </div>
                  <span className="text-[10px] uppercase font-bold tracking-wider px-1.5 py-0.5 rounded bg-white text-[#667085] border border-[#E5E9E6]">
                    READ ONLY
                  </span>
                </div>
                <div className="flex items-center justify-between gap-2 pt-1">
                  <p className="text-xs font-mono text-[#111318] font-medium truncate select-all">
                    {user?.id || '—'}
                  </p>
                  <button
                    type="button"
                    onClick={() => handleCopy(user?.id || '', 'tenant-id')}
                    className="shrink-0 px-2 py-0.5 rounded-md text-xs font-medium text-[#08704F] bg-[#E8F7F0] hover:bg-[#d8f2e5] border border-[#0B8F63]/30 transition-colors flex items-center gap-1"
                    aria-label="Copy Tenant ID UUID"
                  >
                    {copiedField === 'tenant-id' ? (
                      <>
                        <Check className="w-3 h-3 text-[#0B8F63]" />
                        <span>Copied</span>
                      </>
                    ) : (
                      <>
                        <Copy className="w-3 h-3" />
                        <span>Copy</span>
                      </>
                    )}
                  </button>
                </div>
              </div>

              {/* Account Created Date */}
              <div className="p-3.5 rounded-xl bg-[#F7F8F6] border border-[#E5E9E6] space-y-1.5">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider text-[#667085]">
                    <Calendar className="w-3.5 h-3.5 text-[#0B8F63]" />
                    <span>Account Created</span>
                  </div>
                  <span className="text-[10px] uppercase font-bold tracking-wider px-1.5 py-0.5 rounded bg-white text-[#667085] border border-[#E5E9E6]">
                    READ ONLY
                  </span>
                </div>
                <div className="flex items-center justify-between pt-1">
                  <p className="text-xs font-mono text-[#111318]">
                    {formatDate(user?.createdAt)}
                  </p>
                  <Lock className="w-3.5 h-3.5 text-[#98A2B3]" />
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ==================================================
          3. PAYMENT INTEGRATION & WORKSPACE CONFIGURATION
          ================================================== */}
      <section className="grid grid-cols-1 md:grid-cols-12 gap-6 pt-4 border-t border-[#ECEFEA]">
        <div className="md:col-span-4 space-y-1">
          <h3 className="font-space-grotesk text-base font-bold text-[#111318] tracking-tight">
            Payment Integration
          </h3>
          <p className="text-xs text-[#667085] leading-relaxed">
            Connected payment gateway and settlement reconciliation credentials.
          </p>
        </div>

        <div className="md:col-span-8 space-y-4">
          <div className="bg-white border border-[#E5E9E6] rounded-xl p-5 sm:p-6 shadow-2xs space-y-5">
            {/* Razorpay Account Field */}
            <div className="p-4 rounded-xl bg-[#F7F8F6] border border-[#E5E9E6] space-y-2">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider text-[#667085]">
                  <CreditCard className="w-3.5 h-3.5 text-[#0B8F63]" />
                  <span>Razorpay Account ID</span>
                </div>
                {user?.razorpayAccountId ? (
                  <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-[#08704F] bg-[#E8F7F0] px-2 py-0.5 rounded-full border border-[#0B8F63]/30">
                    <span className="w-1.5 h-1.5 rounded-full bg-[#0B8F63]" />
                    <span>Linked Gateway</span>
                  </span>
                ) : (
                  <span className="text-[11px] font-medium text-[#D97706] bg-[#FEF3C7] px-2 py-0.5 rounded-full border border-[#D97706]/30">
                    Not configured
                  </span>
                )}
              </div>

              <div className="flex items-center justify-between gap-3 pt-1">
                <div className="font-mono text-sm">
                  {user?.razorpayAccountId ? (
                    <span className="text-[#08704F] bg-[#E8F7F0] px-2.5 py-1 rounded-md border border-[#0B8F63]/30 font-semibold inline-block">
                      {user.razorpayAccountId}
                    </span>
                  ) : (
                    <span className="text-[#98A2B3] italic">Not configured</span>
                  )}
                </div>
                {user?.razorpayAccountId && (
                  <button
                    type="button"
                    onClick={() => handleCopy(user.razorpayAccountId || '', 'razorpay-id')}
                    className="px-2.5 py-1 rounded-lg text-xs font-medium text-[#08704F] bg-white hover:bg-[#E8F7F0] border border-[#E5E9E6] hover:border-[#0B8F63]/30 transition-colors flex items-center gap-1 shadow-2xs"
                    aria-label="Copy Razorpay Account identifier"
                  >
                    {copiedField === 'razorpay-id' ? (
                      <>
                        <Check className="w-3 h-3 text-[#0B8F63]" />
                        <span>Copied</span>
                      </>
                    ) : (
                      <>
                        <Copy className="w-3 h-3" />
                        <span>Copy</span>
                      </>
                    )}
                  </button>
                )}
              </div>
              <p className="text-[11px] text-[#667085] pt-1">
                Upstream webhook events and autonomous recovery retries are dispatched against this processor account.
              </p>
            </div>

            {/* Read-Only Notice / Account Profile Governance */}
            <div className="p-4 rounded-xl bg-[#F1F4F2] border border-[#E5E9E6] flex items-start gap-3">
              <Lock className="w-4 h-4 text-[#667085] shrink-0 mt-0.5" />
              <div className="text-xs text-[#667085] space-y-1">
                <span className="font-semibold text-[#111318] block">Account Profile Governance</span>
                <p className="text-[#667085] leading-relaxed">
                  Merchant identity and payment processor integrations are locked to maintain tenant security and auditing standards. Contact platform operations to modify merchant profile fields.
                </p>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ==================================================
          4. SECURITY & SESSION SECTION
          ================================================== */}
      <section className="grid grid-cols-1 md:grid-cols-12 gap-6 pt-4 border-t border-[#ECEFEA]">
        <div className="md:col-span-4 space-y-1">
          <h3 className="font-space-grotesk text-base font-bold text-[#111318] tracking-tight">
            Security &amp; Active Session
          </h3>
          <p className="text-xs text-[#667085] leading-relaxed">
            Tenant boundary isolation, token issuance protocols, and operational session details.
          </p>
        </div>

        <div className="md:col-span-8 space-y-4">
          <div className="bg-white border border-[#E5E9E6] rounded-xl p-5 sm:p-6 shadow-2xs space-y-5">
            <div className="flex items-center gap-2 pb-3 border-b border-[#ECEFEA]">
              <Key className="w-4 h-4 text-[#0B8F63]" />
              <h4 className="text-sm font-semibold text-[#111318]">Security &amp; Active Session</h4>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
              <div className="p-3.5 rounded-xl bg-[#F1F4F2] border border-[#E5E9E6] space-y-1.5">
                <span className="text-[#667085] font-medium text-[11px] block">Authentication Protocol</span>
                <div className="flex items-center gap-1.5 text-[#111318]">
                  <CheckCircle2 className="w-3.5 h-3.5 text-[#0B8F63] shrink-0" />
                  <span className="font-mono font-semibold">Stateless JWT (HMAC-SHA256)</span>
                </div>
                <p className="text-[11px] text-[#667085] leading-tight">
                  Cryptographically signed tokens; zero client-side storage of private signing keys.
                </p>
              </div>

              <div className="p-3.5 rounded-xl bg-[#F1F4F2] border border-[#E5E9E6] space-y-1.5">
                <span className="text-[#667085] font-medium text-[11px] block">Tenant Isolation</span>
                <div className="flex items-center gap-1.5 text-[#111318]">
                  <CheckCircle2 className="w-3.5 h-3.5 text-[#0B8F63] shrink-0" />
                  <span className="font-semibold">Strict Merchant Header &amp; Claim Scoping</span>
                </div>
                <p className="text-[11px] text-[#667085] leading-tight">
                  Deterministic scoping ensures strict isolation across data queries and recovery workflows.
                </p>
              </div>
            </div>

            {/* Session Management Action */}
            <div className="pt-2 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs border-t border-[#ECEFEA]">
              <div className="space-y-0.5">
                <span className="font-semibold text-[#111318]">Active Operational Session</span>
                <p className="text-[#667085] text-[11px]">
                  Sign out to terminate your current session token and purge local authentication context.
                </p>
              </div>
              <Button
                variant="outline"
                size="sm"
                onClick={() => logout()}
                className="self-start sm:self-auto border-[#E5E9E6] text-[#111318] hover:bg-[#FEE2E2] hover:text-[#DC2626] hover:border-[#DC2626]/30 transition-colors gap-1.5 font-medium"
              >
                <LogOut className="w-3.5 h-3.5" />
                <span>Sign Out</span>
              </Button>
            </div>
          </div>
        </div>
      </section>

      {/* ==================================================
          5. GLOBAL LIGHT FINTECH FOOTER
          ================================================== */}
      <Footer />
    </div>
  );
}
