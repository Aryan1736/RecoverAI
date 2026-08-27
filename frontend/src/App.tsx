import { useState } from 'react';
import { API_BASE_URL, fetchBackendHealth, type HealthCheckResponse } from './config/api';
import { Activity, CheckCircle2, Cpu, Database, RefreshCw, Server, ShieldCheck, Zap } from 'lucide-react';

export function App() {
  const [healthStatus, setHealthStatus] = useState<HealthCheckResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const checkConnection = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchBackendHealth();
      setHealthStatus(data);
    } catch (err: any) {
      setError(err.message || 'Unable to connect to backend');
      setHealthStatus(null);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col items-center justify-center p-6 selection:bg-indigo-500 selection:text-white">
      {/* Background glow accents */}
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="absolute -top-40 left-1/2 -translate-x-1/2 w-96 h-96 bg-indigo-600/20 rounded-full blur-3xl" />
        <div className="absolute -bottom-40 right-1/4 w-80 h-80 bg-blue-600/15 rounded-full blur-3xl" />
      </div>

      <main className="relative z-10 max-w-2xl w-full">
        {/* Header Badge */}
        <div className="flex items-center justify-center gap-2 mb-6">
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 shadow-sm">
            <ShieldCheck className="w-3.5 h-3.5" />
            Track 3 Submission • Project Foundation
          </span>
        </div>

        {/* Hero Card */}
        <div className="bg-slate-900/80 border border-slate-800 backdrop-blur-xl rounded-2xl p-8 shadow-2xl space-y-6">
          <div className="text-center space-y-2">
            <div className="inline-flex p-3 rounded-2xl bg-indigo-500/10 text-indigo-400 mb-2 border border-indigo-500/20">
              <Zap className="w-8 h-8" />
            </div>
            <h1 className="text-3xl font-bold tracking-tight text-white">
              RecoverAI
            </h1>
            <p className="text-sm text-slate-400 max-w-md mx-auto">
              Autonomous, safe revenue recovery agent for failed payments. Foundation setup active.
            </p>
          </div>

          {/* Foundation Component Status Grid */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2">
            <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-800/80 flex items-start gap-3">
              <div className="p-2 rounded-lg bg-emerald-500/10 text-emerald-400">
                <CheckCircle2 className="w-4 h-4" />
              </div>
              <div>
                <h3 className="text-xs font-semibold text-slate-200">Frontend Shell</h3>
                <p className="text-xs text-slate-400">React + Vite + TypeScript (Active)</p>
              </div>
            </div>

            <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-800/80 flex items-start gap-3">
              <div className="p-2 rounded-lg bg-indigo-500/10 text-indigo-400">
                <Server className="w-4 h-4" />
              </div>
              <div>
                <h3 className="text-xs font-semibold text-slate-200">Backend API</h3>
                <p className="text-xs text-slate-400">Spring Boot 3.4.x (Java 21)</p>
              </div>
            </div>

            <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-800/80 flex items-start gap-3">
              <div className="p-2 rounded-lg bg-blue-500/10 text-blue-400">
                <Database className="w-4 h-4" />
              </div>
              <div>
                <h3 className="text-xs font-semibold text-slate-200">Persistence & Schema</h3>
                <p className="text-xs text-slate-400">PostgreSQL + Flyway</p>
              </div>
            </div>

            <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-800/80 flex items-start gap-3">
              <div className="p-2 rounded-lg bg-purple-500/10 text-purple-400">
                <Cpu className="w-4 h-4" />
              </div>
              <div>
                <h3 className="text-xs font-semibold text-slate-200">Agent Intelligence</h3>
                <p className="text-xs text-slate-400">Gemini 3.7 Flash Engine</p>
              </div>
            </div>
          </div>

          {/* Backend Connectivity Test Box */}
          <div className="pt-4 border-t border-slate-800/80 space-y-3">
            <div className="flex items-center justify-between text-xs">
              <span className="text-slate-400">Target Backend Base URL:</span>
              <code className="px-2 py-0.5 rounded bg-slate-950 border border-slate-800 font-mono text-indigo-300">
                {API_BASE_URL}
              </code>
            </div>

            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Activity className="w-4 h-4 text-slate-400" />
                <span className="text-xs text-slate-300 font-medium">Backend Health Check:</span>
              </div>
              <button
                onClick={checkConnection}
                disabled={loading}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium bg-indigo-600 hover:bg-indigo-500 active:bg-indigo-700 text-white transition disabled:opacity-50 cursor-pointer"
              >
                <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
                {loading ? 'Checking...' : 'Test Connection'}
              </button>
            </div>

            {healthStatus && (
              <div className="p-3 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-emerald-300 text-xs flex items-center justify-between font-mono">
                <span>Status: {healthStatus.status}</span>
                <span>Service: {healthStatus.service}</span>
              </div>
            )}

            {error && (
              <div className="p-3 rounded-lg bg-amber-500/10 border border-amber-500/20 text-amber-300 text-xs">
                Backend offline or unreachable ({error}). Start Spring Boot backend to verify live link.
              </div>
            )}
          </div>
        </div>

        {/* Footer info */}
        <footer className="text-center mt-6 text-xs text-slate-500">
          RecoverAI Foundation • Ready for feature development
        </footer>
      </main>
    </div>
  );
}

export default App;
