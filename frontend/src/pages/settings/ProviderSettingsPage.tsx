import { ProviderHealthCard } from '../../components/settings/ProviderHealthCard';
import { Footer } from '../../components/layout/Footer';

export function ProviderSettingsPage() {
  return (
    <div className="space-y-8 max-w-5xl font-inter animate-console-fade-in delay-1 pb-6">
      <ProviderHealthCard />
      <Footer />
    </div>
  );
}

