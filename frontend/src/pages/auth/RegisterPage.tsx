import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Zap, ShieldCheck, ArrowRight, Building, CreditCard } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import { useToast } from '../../hooks/useToast';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { PasswordInput } from '../../components/ui/PasswordInput';
import { Alert } from '../../components/ui/Alert';
import { getHumanReadableErrorMessage } from '../../types/api';

export function RegisterPage() {
  const navigate = useNavigate();
  const { register, login } = useAuth();
  const { toast } = useToast();

  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [razorpayAccountId, setRazorpayAccountId] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    const trimmedName = name.trim();
    if (!trimmedName) {
      newErrors.name = 'Business or Merchant name is required';
    } else if (trimmedName.length < 2) {
      newErrors.name = 'Name must be at least 2 characters';
    } else if (trimmedName.length > 255) {
      newErrors.name = 'Name cannot exceed 255 characters';
    }

    const trimmedEmail = email.trim();
    if (!trimmedEmail) {
      newErrors.email = 'Business email address is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmedEmail)) {
      newErrors.email = 'Please enter a valid email address';
    }

    if (!password) {
      newErrors.password = 'Password is required';
    } else if (password.length < 8) {
      newErrors.password = 'Password must be at least 8 characters';
    }

    if (password !== confirmPassword) {
      newErrors.confirmPassword = 'Passwords do not match';
    }

    const trimmedRzp = razorpayAccountId.trim();
    if (trimmedRzp && !/^[a-zA-Z0-9_]{4,40}$/.test(trimmedRzp)) {
      newErrors.razorpayAccountId = 'Invalid format (e.g. acc_1234567890)';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setFormError(null);

    if (!validate()) return;

    setIsSubmitting(true);
    try {
      await register({
        name: name.trim(),
        email: email.trim(),
        password,
        razorpayAccountId: razorpayAccountId.trim() || undefined,
      });

      // Auto login after registration
      await login({
        email: email.trim(),
        password,
      });

      toast.success('Merchant account registered successfully!');
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
            Create your Merchant Account
          </h1>
          <p className="text-xs text-slate-500 max-w-xs">
            Activate autonomous payment recovery and AI diagnostics
          </p>
        </div>

        {/* Global Error Banner */}
        {formError && (
          <div className="mb-4">
            <Alert type="error" title="Registration Failed">
              {formError}
            </Alert>
          </div>
        )}

        {/* Main Registration Card */}
        <div className="bg-white border border-slate-200/90 rounded-2xl p-6 sm:p-8 shadow-xs space-y-5">
          <form onSubmit={handleSubmit} noValidate className="space-y-4">
            <Input
              label="Merchant / Business Name"
              id="register-name"
              placeholder="Acme Corp Pvt Ltd"
              value={name}
              onChange={(e) => setName(e.target.value)}
              error={errors.name}
              required
              leftIcon={<Building className="w-4 h-4" />}
              disabled={isSubmitting}
            />

            <Input
              label="Business Email"
              id="register-email"
              type="email"
              placeholder="finance@acme.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              error={errors.email}
              required
              autoComplete="email"
              disabled={isSubmitting}
            />

            <Input
              label="Razorpay Account ID (Optional)"
              id="register-razorpay"
              placeholder="acc_K3L9abcdef123"
              value={razorpayAccountId}
              onChange={(e) => setRazorpayAccountId(e.target.value)}
              error={errors.razorpayAccountId}
              helperText="Connects your payment gateway directly to RecoverAI"
              leftIcon={<CreditCard className="w-4 h-4" />}
              disabled={isSubmitting}
            />

            <PasswordInput
              label="Password"
              id="register-password"
              placeholder="Minimum 8 characters"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              error={errors.password}
              required
              autoComplete="new-password"
              disabled={isSubmitting}
            />

            <PasswordInput
              label="Confirm Password"
              id="register-confirm-password"
              placeholder="Re-enter your password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              error={errors.confirmPassword}
              required
              autoComplete="new-password"
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
              Complete Registration & Access Portal
            </Button>
          </form>

          {/* Login link */}
          <div className="pt-2 text-center text-xs text-slate-500 border-t border-slate-100">
            <span>Already have an account? </span>
            <Link
              to="/login"
              className="font-semibold text-emerald-600 hover:text-emerald-700 hover:underline focus:outline-none focus:underline"
            >
              Sign in here
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
