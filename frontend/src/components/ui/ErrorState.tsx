import { AlertTriangle, RefreshCw } from 'lucide-react';
import { Button } from './Button';

export interface ErrorStateProps {
  title?: string;
  message: string;
  onRetry?: () => void;
  className?: string;
}

export function ErrorState({
  title = 'Unable to Load Data',
  message,
  onRetry,
  className = '',
}: ErrorStateProps) {
  return (
    <div
      className={`p-6 sm:p-8 text-center rounded-xl border border-rose-200 bg-rose-50/50 max-w-lg mx-auto flex flex-col items-center justify-center space-y-4 shadow-2xs ${className}`}
    >
      <div className="p-3 rounded-xl bg-rose-100 border border-rose-200 text-rose-600">
        <AlertTriangle className="w-6 h-6" aria-hidden="true" />
      </div>
      <div className="space-y-1 max-w-sm">
        <h4 className="text-sm font-semibold text-slate-900 tracking-tight">{title}</h4>
        <p className="text-xs text-rose-700 leading-relaxed">{message}</p>
      </div>
      {onRetry && (
        <Button
          size="sm"
          variant="outline"
          onClick={onRetry}
          leftIcon={<RefreshCw className="w-3.5 h-3.5" />}
        >
          Try Again
        </Button>
      )}
    </div>
  );
}
