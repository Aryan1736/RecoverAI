import { Sparkles, LogOut } from 'lucide-react';

export interface DemoModeBadgeProps {
  onExit: () => void;
  className?: string;
}

export function DemoModeBadge({ onExit, className = '' }: DemoModeBadgeProps) {
  return (
    <div
      role="status"
      aria-label="Interactive Demo Mode active"
      className={`inline-flex items-center gap-1.5 sm:gap-2 px-2.5 py-1 rounded-full bg-amber-50 border border-amber-200 text-amber-800 text-xs shadow-2xs ${className}`}
    >
      <span className="relative flex h-2 w-2">
        <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-amber-400 opacity-75" />
        <span className="relative inline-flex rounded-full h-2 w-2 bg-amber-500" />
      </span>

      <div className="flex items-center gap-1 font-semibold tracking-wide text-[11px] uppercase text-amber-900">
        <Sparkles className="w-3 h-3 text-amber-600 hidden sm:inline" />
        <span>DEMO MODE</span>
      </div>

      <span className="text-amber-300 text-[10px] hidden md:inline">|</span>
      <span className="text-amber-700 text-[10px] hidden md:inline font-medium">Simulated Data</span>

      <button
        type="button"
        onClick={onExit}
        aria-label="Exit demo mode"
        className="ml-1 px-2 py-0.5 rounded-full text-[10px] font-medium bg-amber-100 hover:bg-amber-200 text-amber-900 border border-amber-300/80 transition-colors flex items-center gap-1 cursor-pointer focus:outline-none focus:ring-1 focus:ring-amber-500"
      >
        <LogOut className="w-2.5 h-2.5" />
        <span>Exit Demo</span>
      </button>
    </div>
  );
}
