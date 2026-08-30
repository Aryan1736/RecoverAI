import { createContext } from 'react';

export const DEMO_STORAGE_KEY = 'recoverai_demo_mode';

export function getStoredDemoMode(): boolean {
  try {
    return localStorage.getItem(DEMO_STORAGE_KEY) === 'true';
  } catch {
    return false;
  }
}

export function setStoredDemoMode(enabled: boolean): void {
  try {
    if (enabled) {
      localStorage.setItem(DEMO_STORAGE_KEY, 'true');
    } else {
      localStorage.removeItem(DEMO_STORAGE_KEY);
    }
  } catch {
    // LocalStorage write safety
  }
}

export function clearStoredDemoMode(): void {
  try {
    localStorage.removeItem(DEMO_STORAGE_KEY);
  } catch {
    // LocalStorage clear safety
  }
}

export interface DemoContextType {
  isDemoMode: boolean;
  enterDemoMode: () => void;
  exitDemoMode: () => void;
}

export const DemoContext = createContext<DemoContextType | undefined>(undefined);
