import { useState, useCallback, type ReactNode } from 'react';
import { CheckCircle2, AlertCircle, Info, AlertTriangle, X } from 'lucide-react';
import { ToastContext, type ToastItem, type ToastType } from './toast-context-def';

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([]);

  const dismissToast = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const showToast = useCallback(
    (type: ToastType, message: string, durationMs = 4000) => {
      const id = `${Date.now()}-${Math.random().toString(36).slice(2, 7)}`;
      setToasts((prev) => [...prev, { id, type, message }]);

      if (durationMs > 0) {
        setTimeout(() => {
          dismissToast(id);
        }, durationMs);
      }
    },
    [dismissToast]
  );

  const toast = {
    success: useCallback((message: string) => showToast('success', message), [showToast]),
    error: useCallback((message: string) => showToast('error', message), [showToast]),
    info: useCallback((message: string) => showToast('info', message), [showToast]),
    warning: useCallback((message: string) => showToast('warning', message), [showToast]),
  };

  return (
    <ToastContext.Provider value={{ toasts, showToast, dismissToast, toast }}>
      {children}
      {/* Accessible Toast Container */}
      <div
        aria-live="polite"
        aria-atomic="true"
        className="fixed bottom-5 right-5 z-50 flex flex-col gap-2 max-w-md w-full pointer-events-none px-4"
      >
        {toasts.map((item) => (
          <div
            key={item.id}
            role="status"
            className={`pointer-events-auto flex items-start gap-3 p-4 rounded-xl border bg-white shadow-lg transition-all duration-200 animate-in fade-in slide-in-from-bottom-2 ${
              item.type === 'success'
                ? 'border-emerald-200 text-slate-800'
                : item.type === 'error'
                ? 'border-rose-200 text-slate-800'
                : item.type === 'warning'
                ? 'border-amber-200 text-slate-800'
                : 'border-slate-200 text-slate-800'
            }`}
          >
            <div className="shrink-0 mt-0.5">
              {item.type === 'success' && <CheckCircle2 className="w-5 h-5 text-emerald-600" />}
              {item.type === 'error' && <AlertCircle className="w-5 h-5 text-rose-600" />}
              {item.type === 'warning' && <AlertTriangle className="w-5 h-5 text-amber-600" />}
              {item.type === 'info' && <Info className="w-5 h-5 text-blue-600" />}
            </div>
            <div className="flex-1 text-sm font-medium leading-snug">{item.message}</div>
            <button
              onClick={() => dismissToast(item.id)}
              className="shrink-0 text-slate-400 hover:text-slate-600 transition p-0.5 rounded cursor-pointer"
              aria-label="Dismiss notification"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}
