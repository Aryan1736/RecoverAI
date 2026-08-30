import {
  useState,
  useEffect,
  useCallback,
  type ReactNode,
} from 'react';
import type {
  AuthContextType,
  AuthResponse,
  LoginRequest,
  Merchant,
  RegisterRequest,
} from '../types/auth';
import {
  clearStoredAuth,
  getStoredMerchant,
  getStoredToken,
  registerUnauthorizedHandler,
  setStoredMerchant,
  setStoredToken,
} from '../api/client';
import { login as apiLogin, register as apiRegister } from '../api/auth';
import { AuthContext } from './auth-context-def';

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => {
    const t = getStoredToken();
    const m = getStoredMerchant();
    if (t && m) return t;
    clearStoredAuth();
    return null;
  });
  const [user, setUser] = useState<Merchant | null>(() => {
    const t = getStoredToken();
    const m = getStoredMerchant();
    if (t && m) return m;
    return null;
  });
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [sessionExpiredMessage, setSessionExpiredMessage] = useState<string | null>(null);

  // Listen for 401 unauthorized notifications from API client
  useEffect(() => {
    const unregister = registerUnauthorizedHandler((message: string) => {
      setToken(null);
      setUser(null);
      setSessionExpiredMessage(message);
    });
    return unregister;
  }, []);

  const login = useCallback(async (credentials: LoginRequest): Promise<AuthResponse> => {
    setIsLoading(true);
    setSessionExpiredMessage(null);
    try {
      const response = await apiLogin(credentials);
      setStoredToken(response.token);
      setStoredMerchant(response.merchant);
      setToken(response.token);
      setUser(response.merchant);
      return response;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const register = useCallback(async (payload: RegisterRequest): Promise<Merchant> => {
    setIsLoading(true);
    try {
      return await apiRegister(payload);
    } finally {
      setIsLoading(false);
    }
  }, []);

  const logout = useCallback((reason?: string) => {
    clearStoredAuth();
    setToken(null);
    setUser(null);
    if (reason) {
      setSessionExpiredMessage(reason);
    }
  }, []);

  const clearSessionExpiredMessage = useCallback(() => {
    setSessionExpiredMessage(null);
  }, []);

  const value: AuthContextType = {
    user,
    token,
    isAuthenticated: Boolean(token && user),
    isLoading,
    sessionExpiredMessage,
    login,
    register,
    logout,
    clearSessionExpiredMessage,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
