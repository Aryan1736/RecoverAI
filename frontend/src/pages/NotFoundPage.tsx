import { Link } from 'react-router-dom';
import { ShieldAlert, ArrowLeft } from 'lucide-react';
import { Button } from '../components/ui/Button';
import { useAuth } from '../hooks/useAuth';

export function NotFoundPage() {
  const { isAuthenticated } = useAuth();

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900 flex flex-col items-center justify-center p-6 text-center selection:bg-emerald-500 selection:text-white">
      <div className="p-4 rounded-2xl bg-emerald-50 text-emerald-600 border border-emerald-200 mb-4 shadow-2xs">
        <ShieldAlert className="w-10 h-10" />
      </div>
      <h1 className="text-4xl font-extrabold tracking-tight text-slate-900 mb-2">404</h1>
      <h2 className="text-lg font-semibold text-slate-800 mb-2">Page Not Found</h2>
      <p className="text-xs text-slate-500 max-w-sm mb-6">
        The route you are looking for does not exist or has been relocated within the RecoverAI platform.
      </p>
      <Link to={isAuthenticated ? '/app' : '/login'}>
        <Button variant="primary" size="md" leftIcon={<ArrowLeft className="w-4 h-4" />}>
          {isAuthenticated ? 'Return to Dashboard' : 'Back to Sign In'}
        </Button>
      </Link>
    </div>
  );
}
