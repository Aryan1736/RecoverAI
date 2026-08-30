export type ProviderHealthState =
  | 'HEALTHY'
  | 'DEGRADED'
  | 'UNAVAILABLE'
  | 'DISABLED'
  | 'UNKNOWN';

export interface ProviderStatusItem {
  id: string;
  name: string;
  channel: string;
  status: ProviderHealthState;
  message?: string;
  lastChecked?: string;
}

export interface ProviderHealthSummary {
  overallStatus: ProviderHealthState;
  providers: ProviderStatusItem[];
  lastChecked: string;
}

export interface ActuatorHealthDetails {
  components?: Record<string, string>;
  messages?: Record<string, string>;
  status?: string;
}

export interface ActuatorHealthIndicatorResponse {
  status: string;
  details?: ActuatorHealthDetails;
}

export interface ActuatorAggregatedHealthResponse {
  status: string;
  components?: {
    providerHealthIndicator?: ActuatorHealthIndicatorResponse;
    [key: string]: unknown;
  };
  details?: ActuatorHealthDetails;
}
