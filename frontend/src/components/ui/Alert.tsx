import { useState, type ReactNode } from 'react';
import { AlertCircle, CheckCircle2, Info, AlertTriangle, X } from 'lucide-react';

export type AlertType = 'info' | 'success' | 'warning' | 'error';

export interface AlertProps {
  type?: AlertType;
  title?: string;
  children: ReactNode;
  dismissible?: boolean;
  onDismiss?: () => void;
  className?: string;
}

export function Alert({
  type = 'info',
  title,
  children,
  dismissible = false,
  onDismiss,
  className = '',
}: AlertProps) {
  const [dismissed, setDismissed] = useState(false);

  if (dismissed) return null;

  const handleDismiss = () => {
    setDismissed(true);
    onDismiss?.();
  };

  const typeStyles = {
    info: 'bg-blue-50 border-blue-200 text-blue-900',
    success: 'bg-emerald-50 border-emerald-200 text-emerald-900',
    warning: 'bg-amber-50 border-amber-200 text-amber-900',
    error: 'bg-rose-50 border-rose-200 text-rose-900',
  };

  const icons = {
    info: <Info className="w-4 h-4 text-blue-600 shrink-0 mt-0.5" aria-hidden="true" />,
    success: <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" aria-hidden="true" />,
    warning: <AlertTriangle className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" aria-hidden="true" />,
    error: <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" aria-hidden="true" />,
  };

  return (
    <div
      role="alert"
      className={`flex items-start gap-3 p-3.5 rounded-xl border text-sm ${typeStyles[type]} ${className}`}
    >
      {icons[type]}
      <div className="flex-1 space-y-0.5">
        {title && <h4 className="font-semibold text-xs uppercase tracking-wider">{title}</h4>}
        <div className="text-xs leading-relaxed opacity-95">{children}</div>
      </div>
      {dismissible && (
        <button
          type="button"
          onClick={handleDismiss}
          className="text-slate-400 hover:text-slate-700 transition p-0.5 rounded cursor-pointer"
          aria-label="Dismiss alert"
        >
          <X className="w-3.5 h-3.5" aria-hidden="true" />
        </button>
      )}
    </div>
  );
}
