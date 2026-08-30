import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Zap, ShieldCheck, ArrowRight, Sparkles, CheckCircle2 } from 'lucide-react';
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
    <div className="min-h-screen bg-slate-50 text-slate-900 flex flex-col justify-center py-12 sm:px-6 lg:px-8 relative selection:bg-emerald-500 selection:text-white">
      {/* Subtle ambient light accents */}
      <div className="fixed inset-0 overflow-hidden pointer-events-none" aria-hidden="true">
        <div className="absolute -top-40 left-1/2 -translate-x-1/2 w-96 h-96 bg-emerald-100/40 rounded-full blur-3xl" />
        <div className="absolute -bottom-40 right-1/4 w-80 h-80 bg-teal-100/30 rounded-full blur-3xl" />
      </div>

      <div className="sm:mx-auto sm:w-full sm:max-w-md relative z-10 px-4">
        {/* Brand Header */}
        <div className="flex flex-col items-center text-center mb-8 space-y-2">
          <div className="inline-flex p-3 rounded-2xl bg-emerald-50 text-emerald-600 border border-emerald-200 mb-1 shadow-2xs">
            <Zap className="w-7 h-7" />
          </div>
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
            <ShieldCheck className="w-3.5 h-3.5 text-emerald-600" />
            Track 3 • Razorpay Buildathon
          </span>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 mt-1">
            Sign in to RecoverAI
          </h1>
          <p className="text-xs text-slate-500 max-w-xs">
            Autonomous, safe revenue recovery agent for failed payments
          </p>
        </div>

        {/* Session Expired Banner */}
        {sessionExpiredMessage && (
          <div className="mb-4">
            <Alert
              type="warning"
              title="Session Expired"
              dismissible
              onDismiss={clearSessionExpiredMessage}
            >
              {sessionExpiredMessage}
            </Alert>
          </div>
        )}

        {/* Global Form Error Banner */}
        {formError && (
          <div className="mb-4">
            <Alert type="error" title="Sign In Failed">
              {formError}
            </Alert>
          </div>
        )}

        {/* Main Sign-In Card */}
        <div className="bg-white border border-slate-200/90 rounded-2xl p-6 sm:p-8 shadow-xs space-y-5">
          <form onSubmit={handleSubmit} noValidate className="space-y-4">
            <Input
              label="Business Email"
              id="login-email"
              type="email"
              placeholder="merchant@company.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              error={emailError ?? undefined}
              required
              autoComplete="email"
              disabled={isSubmitting}
            />

            <PasswordInput
              label="Password"
              id="login-password"
              placeholder="••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              error={passwordError ?? undefined}
              required
              autoComplete="current-password"
              disabled={isSubmitting}
            />

            <Button
              type="submit"
              variant="primary"
              size="lg"
              className="w-full mt-2"
              isLoading={isSubmitting}
              rightIcon={<ArrowRight className="w-4 h-4" />}
            >
              Sign In to Dashboard
            </Button>
          </form>

          {/* Registration link */}
          <div className="pt-2 text-center text-xs text-slate-500 border-t border-slate-100">
            <span>Don&apos;t have an account? </span>
            <Link
              to="/register"
              className="font-semibold text-emerald-600 hover:text-emerald-700 hover:underline focus:outline-none focus:underline"
            >
              Register your business
            </Link>
          </div>
        </div>

        {/* Interactive Demo Mode Card */}
        <div className="mt-4 p-5 rounded-2xl bg-amber-50/70 border border-amber-200/80 shadow-2xs space-y-3">
          <div className="flex items-start gap-3">
            <div className="p-2 rounded-xl bg-amber-100/80 text-amber-700 shrink-0 mt-0.5">
              <Sparkles className="w-4 h-4 text-amber-600" />
            </div>
            <div className="space-y-0.5 text-left">
              <h2 className="text-xs font-bold text-slate-900 uppercase tracking-wider">
                Interactive Evaluator Demo
              </h2>
              <p className="text-xs text-slate-600 leading-relaxed">
                No account required • Explore with simulated demo data
              </p>
            </div>
          </div>

          <div className="space-y-1.5 text-[11px] text-slate-500">
            <div className="flex items-center gap-1.5">
              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
              <span>Full merchant dashboard &amp; recovery telemetry</span>
            </div>
            <div className="flex items-center gap-1.5">
              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
              <span>Preloaded recovery cases with AI failure diagnosis</span>
            </div>
            <div className="flex items-center gap-1.5">
              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
              <span>Interactive simulated customer recovery checkout</span>
            </div>
          </div>

          <Button
            type="button"
            variant="secondary"
            size="md"
            onClick={handleLaunchDemo}
            className="w-full bg-white hover:bg-amber-50/80 text-amber-900 border-amber-200/90 font-semibold cursor-pointer shadow-2xs"
            leftIcon={<Sparkles className="w-4 h-4 text-amber-600" />}
          >
            Try Interactive Demo
          </Button>
        </div>
      </div>
    </div>
  );
}
