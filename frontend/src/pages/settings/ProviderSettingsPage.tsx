import { ProviderHealthCard } from '../../components/settings/ProviderHealthCard';

export function ProviderSettingsPage() {
  return (
    <div className="space-y-6 max-w-5xl">
      <div className="space-y-1">
        <h3 className="text-base font-semibold text-slate-900">
          Upstream Provider Operational Status
        </h3>
        <p className="text-xs text-slate-500">
          Telemetry and availability metrics across communications providers (WhatsApp, Email, SMS) and payment recovery gateways.
        </p>
      </div>

      <ProviderHealthCard />
    </div>
  );
}
