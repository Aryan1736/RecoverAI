export type MerchantStatus = 'ACTIVE' | 'SUSPENDED' | 'INACTIVE';

export interface Merchant {
  id: string;
  name: string;
  email: string;
  razorpayAccountId?: string | null;
  status: MerchantStatus;
  createdAt: string;
  updatedAt: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresInMs: number;
  merchant: Merchant;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
  razorpayAccountId?: string;
  webhookSecret?: string;
}

export interface AuthContextType {
  user: Merchant | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  sessionExpiredMessage: string | null;
  login: (credentials: LoginRequest) => Promise<AuthResponse>;
  register: (payload: RegisterRequest) => Promise<Merchant>;
  logout: (reason?: string) => void;
  clearSessionExpiredMessage: () => void;
}
