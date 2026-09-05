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
      className={`inline-flex items-center gap-1.5 sm:gap-2 px-2.5 py-1 rounded-full bg-[#FEF9EE] border border-[#FBEAC8] text-[#92400E] text-xs shadow-2xs ${className}`}
    >
      <span className="relative flex h-2 w-2 shrink-0">
        <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-amber-400 opacity-75 motion-reduce:hidden" />
        <span className="relative inline-flex rounded-full h-2 w-2 bg-amber-500" />
      </span>

      <div className="flex items-center gap-1 font-semibold tracking-wide text-[11px] uppercase text-[#78350F]">
        <Sparkles className="w-3 h-3 text-amber-600 hidden sm:inline" aria-hidden="true" />
        <span>DEMO MODE</span>
      </div>

      <span className="text-[#F59E0B]/40 text-[10px] hidden md:inline" aria-hidden="true">|</span>
      <span className="text-[#92400E] text-[10px] hidden md:inline font-medium">Simulated Data</span>

      <button
        type="button"
        onClick={onExit}
        aria-label="Exit demo mode"
        className="ml-0.5 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-[#FDE68A]/60 hover:bg-[#FDE68A] text-[#78350F] border border-[#F59E0B]/30 transition-colors flex items-center gap-1 cursor-pointer focus:outline-none focus-visible:ring-2 focus-visible:ring-amber-500"
      >
        <LogOut className="w-2.5 h-2.5 shrink-0" aria-hidden="true" />
        <span>Exit Demo</span>
      </button>
    </div>
  );
}

