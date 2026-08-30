import { Link } from 'react-router-dom';
import { ShieldAlert, ArrowLeft } from 'lucide-react';
import { Button } from '../components/ui/Button';
import { useAuth } from '../hooks/useAuth';

export function NotFoundPage() {
  const { isAuthenticated } = useAuth();

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col items-center justify-center p-6 text-center">
      <div className="p-4 rounded-2xl bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 mb-4">
        <ShieldAlert className="w-10 h-10" />
      </div>
      <h1 className="text-4xl font-extrabold tracking-tight text-white mb-2">404</h1>
      <h2 className="text-lg font-semibold text-slate-200 mb-2">Page Not Found</h2>
      <p className="text-xs text-slate-400 max-w-sm mb-6">
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
