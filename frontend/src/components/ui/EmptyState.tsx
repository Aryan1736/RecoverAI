import type { ReactNode } from 'react';
import { Inbox } from 'lucide-react';

export interface EmptyStateProps {
  icon?: ReactNode;
  title: string;
  description: string;
  action?: ReactNode;
  className?: string;
}

export function EmptyState({
  icon,
  title,
  description,
  action,
  className = '',
}: EmptyStateProps) {
  return (
    <div
      className={`p-8 sm:p-12 text-center rounded-xl border border-dashed border-slate-800 bg-slate-900/30 flex flex-col items-center justify-center space-y-4 max-w-xl mx-auto ${className}`}
    >
      <div className="p-3.5 rounded-2xl bg-slate-800/80 border border-slate-700/60 text-slate-400">
        {icon || <Inbox className="w-8 h-8" aria-hidden="true" />}
      </div>
      <div className="space-y-1.5 max-w-sm">
        <h4 className="text-sm font-semibold text-white tracking-tight">{title}</h4>
        <p className="text-xs text-slate-400 leading-relaxed">{description}</p>
      </div>
      {action && <div className="pt-2">{action}</div>}
    </div>
  );
}
