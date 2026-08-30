import { createContext } from 'react';

export type ToastType = 'success' | 'error' | 'info' | 'warning';

export interface ToastItem {
  id: string;
  type: ToastType;
  message: string;
}

export interface ToastContextValue {
  toasts: ToastItem[];
  showToast: (type: ToastType, message: string, durationMs?: number) => void;
  dismissToast: (id: string) => void;
  toast: {
    success: (message: string) => void;
    error: (message: string) => void;
    info: (message: string) => void;
    warning: (message: string) => void;
  };
}

export const ToastContext = createContext<ToastContextValue | undefined>(undefined);
