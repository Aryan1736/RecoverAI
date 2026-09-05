import { Link } from 'react-router-dom';
import { useDemoMode } from '../../hooks/useDemoMode';

export interface FooterProps {
  isDemoMode?: boolean;
}

export function Footer({ isDemoMode: isDemoModeProp }: FooterProps = {}) {
  const { isDemoMode: contextDemoMode } = useDemoMode();
  const isDemoMode = isDemoModeProp !== undefined ? isDemoModeProp : contextDemoMode;

  return (
    <footer className="mt-16 border-t border-[#E5E9E6] bg-white text-[#667085] font-inter text-xs">
      <div className="max-w-[1240px] mx-auto px-4 sm:px-6 md:px-8 py-10 space-y-8">
        <div className="grid grid-cols-1 md:grid-cols-12 gap-8">
          {/* Col 1: Brand & Description */}
          <div className="md:col-span-5 space-y-2.5">
            <div className="flex items-center gap-2">
              <div className="w-6 h-6 rounded-lg bg-[#E8F7F0] text-[#0B8F63] flex items-center justify-center font-bold font-space-grotesk text-xs">
                R
              </div>
              <span className="font-space-grotesk font-bold text-sm text-[#111318]">RecoverAI</span>
              <span className="text-[10px] uppercase font-semibold text-[#08704F] bg-[#E8F7F0] px-1.5 py-0.5 rounded">
                Ops
              </span>
            </div>
            <p className="text-xs text-[#667085] max-w-sm leading-relaxed">
              Autonomous recovery infrastructure for failed payments. Closed-loop detection, AI
              root-cause diagnosis, and automated settlement reconciliation.
            </p>
          </div>

          {/* Col 2: Navigation */}
          <div className="md:col-span-4 space-y-2">
            <span className="font-semibold text-xs text-[#111318] uppercase tracking-wider block">
              Platform Navigation
            </span>
            <div className="grid grid-cols-2 gap-x-4 gap-y-1.5 text-xs">
              <Link to="/app" className="text-[#667085] hover:text-[#0B8F63] transition">
                Overview
              </Link>
              <Link to="/recovery-cases" className="text-[#667085] hover:text-[#0B8F63] transition">
                Recovery Cases{'\u200B'}
              </Link>
              <Link to="/analytics" className="text-[#667085] hover:text-[#0B8F63] transition">
                Analytics
              </Link>
              <Link to="/notifications" className="text-[#667085] hover:text-[#0B8F63] transition">
                Notifications{'\u200B'}
              </Link>
              <Link to="/settings" className="text-[#667085] hover:text-[#0B8F63] transition">
                Settings
              </Link>
            </div>
          </div>

          {/* Col 3: System Status */}
          <div className="md:col-span-3 space-y-2">
            <span className="font-semibold text-xs text-[#111318] uppercase tracking-wider block">
              System Infrastructure
            </span>
            <div className="space-y-1 text-xs">
              <div className="flex items-center justify-between text-[#667085]">
                <span>API Status</span>
                <span className="text-[#0B8F63] font-medium font-mono text-[11px]">Operational</span>
              </div>
              <div className="flex items-center justify-between text-[#667085]">
                <span>Recovery Engine</span>
                <span className="text-[#0B8F63] font-medium font-mono text-[11px]">Active</span>
              </div>
              <div className="flex items-center justify-between text-[#667085]">
                <span>Environment</span>
                <span className="text-[#D97706] font-medium font-mono text-[11px]">
                  {isDemoMode ? 'Simulated Sandbox' : 'Production'}
                </span>
              </div>
            </div>
          </div>
        </div>

        <div className="pt-6 border-t border-[#E5E9E6] flex flex-col sm:flex-row items-center justify-between gap-3 text-[11px] text-[#98A2B3]">
          <span>© 2026 RecoverAI. Built for intelligent payment recovery.</span>
          <span className="font-mono">v1.2.0-fintech</span>
        </div>
      </div>
    </footer>
  );
}
