import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Zap, ShieldCheck, ArrowRight, Lock, Mail, Sparkles, CheckCircle2 } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import { useDemoMode } from '../../hooks/useDemoMode';
import { useToast } from '../../hooks/useToast';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { PasswordInput } from '../../components/ui/PasswordInput';
import { Alert } from '../../components/ui/Alert';
import { getHumanReadableErrorMessage } from '../../types/api';

export function LoginPage() {
  const navigate = useNavigate();
  const { login, sessionExpiredMessage, clearSessionExpiredMessage } = useAuth();
  const { enterDemoMode } = useDemoMode();
  const { toast } = useToast();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [emailError, setEmailError] = useState<string | null>(null);
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleLaunchDemo = () => {
    enterDemoMode();
    toast.success('Entering interactive demo environment');
    navigate('/app');
  };

  const validateForm = (): boolean => {
    let isValid = true;
    setEmailError(null);
    setPasswordError(null);

    const trimmedEmail = email.trim();
    if (!trimmedEmail) {
      setEmailError('Email address is required');
      isValid = false;
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmedEmail)) {
      setEmailError('Please enter a valid email address');
      isValid = false;
    }

    if (!password) {
      setPasswordError('Password is required');
      isValid = false;
    }

    return isValid;
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setFormError(null);

    if (!validateForm()) return;

    setIsSubmitting(true);
    try {
      await login({
        email: email.trim(),
        password,
      });
      toast.success('Welcome back to RecoverAI');
      navigate('/app', { replace: true });
    } catch (err: unknown) {
      setFormError(getHumanReadableErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col justify-center py-12 sm:px-6 lg:px-8 relative selection:bg-indigo-500 selection:text-white">
      {/* Background glow accents */}
      <div className="fixed inset-0 overflow-hidden pointer-events-none" aria-hidden="true">
        <div className="absolute -top-40 left-1/2 -translate-x-1/2 w-96 h-96 bg-indigo-600/15 rounded-full blur-3xl" />
        <div className="absolute -bottom-40 right-1/4 w-80 h-80 bg-blue-600/10 rounded-full blur-3xl" />
      </div>

      <div className="sm:mx-auto sm:w-full sm:max-w-md relative z-10 px-4">
        {/* Brand Header */}
        <div className="flex flex-col items-center text-center mb-8 space-y-2">
          <div className="inline-flex p-3 rounded-2xl bg-indigo-600/20 text-indigo-400 border border-indigo-500/30 mb-1 shadow-inner">
            <Zap className="w-7 h-7" />
          </div>
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
            <ShieldCheck className="w-3.5 h-3.5" />
            Track 3 • Razorpay Buildathon
          </span>
          <h1 className="text-2xl font-bold tracking-tight text-white mt-1">
            Sign in to RecoverAI
          </h1>
          <p className="text-xs text-slate-400 max-w-xs">
            Autonomous, safe revenue recovery agent for failed payments
          </p>
        </div>

        {/* Login Card */}
        <div className="bg-slate-900/80 border border-slate-800 backdrop-blur-xl rounded-2xl p-6 sm:p-8 shadow-2xl space-y-5">
          {sessionExpiredMessage && (
            <Alert
              type="warning"
              title="Session Expired"
              dismissible
              onDismiss={clearSessionExpiredMessage}
            >
              {sessionExpiredMessage}
            </Alert>
          )}

          {formError && (
            <Alert type="error" title="Sign In Failed" dismissible onDismiss={() => setFormError(null)}>
              {formError}
            </Alert>
          )}

          <form onSubmit={handleSubmit} className="space-y-4" noValidate>
            <Input
              label="Business Email"
              type="email"
              placeholder="merchant@company.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              error={emailError || undefined}
              leftIcon={<Mail className="w-4 h-4" />}
              autoComplete="email"
              required
            />

            <div className="space-y-1">
              <PasswordInput
                label="Password"
                placeholder="Enter your password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                error={passwordError || undefined}
                leftIcon={<Lock className="w-4 h-4" />}
                autoComplete="current-password"
                required
              />
            </div>

            <Button
              type="submit"
              variant="primary"
              size="md"
              className="w-full mt-2 cursor-pointer"
              isLoading={isSubmitting}
              rightIcon={<ArrowRight className="w-4 h-4" />}
            >
              Sign In to Dashboard
            </Button>
          </form>

          <div className="pt-4 border-t border-slate-800 text-center text-xs text-slate-400">
            Don't have a merchant account?{' '}
            <Link
              to="/register"
              className="font-medium text-indigo-400 hover:text-indigo-300 hover:underline transition"
            >
              Register your business
            </Link>
          </div>
        </div>

        {/* Divider */}
        <div className="relative my-6 text-center text-xs">
          <div className="absolute inset-0 flex items-center" aria-hidden="true">
            <div className="w-full border-t border-slate-800" />
          </div>
          <span className="relative bg-slate-950 px-3 text-slate-400 font-medium">
            or explore without credentials
          </span>
        </div>

        {/* Interactive Demo Mode Card */}
        <div className="bg-gradient-to-br from-indigo-950/70 via-slate-900/90 to-purple-950/50 border border-indigo-500/30 backdrop-blur-xl rounded-2xl p-5 sm:p-6 shadow-2xl space-y-4 relative overflow-hidden">
          <div className="flex items-start justify-between gap-3">
            <div className="space-y-1">
              <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
                <Sparkles className="w-3 h-3 text-indigo-400" />
                Interactive Sandbox
              </div>
              <h2 className="text-base sm:text-lg font-bold text-white tracking-tight">
                Try Interactive Demo
              </h2>
              <p className="text-xs text-slate-300 leading-relaxed">
                Explore RecoverAI with a preloaded demo environment. Inspect simulated recovery cases, autonomous AI diagnosis, analytics, and recovery workflows without creating an account.
              </p>
            </div>
          </div>

          <div className="pt-2 flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 border-t border-indigo-500/20">
            <div className="flex items-center gap-1.5 text-[11px] text-indigo-200/90">
              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
              <span>No account required • Simulated demo data</span>
            </div>

            <Button
              type="button"
              variant="primary"
              size="md"
              className="bg-indigo-600 hover:bg-indigo-500 text-white font-semibold shadow-lg shadow-indigo-600/30 shrink-0 cursor-pointer"
              onClick={handleLaunchDemo}
              rightIcon={<ArrowRight className="w-4 h-4" />}
            >
              Try Interactive Demo
            </Button>
          </div>
        </div>

        {/* Security & System footnote */}
        <div className="text-center mt-6 text-xs text-slate-400 space-y-1">
          <p className="flex items-center justify-center gap-1.5">
            <Lock className="w-3 h-3" />
            256-bit encrypted authentication & deterministic safety guardrails
          </p>
        </div>
      </div>
    </div>
  );
}
