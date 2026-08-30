import { useState, useCallback, type ReactNode } from 'react';
import {
  DemoContext,
  getStoredDemoMode,
  setStoredDemoMode,
  clearStoredDemoMode,
  type DemoContextType,
} from './demo-context-def';

export function DemoProvider({ children }: { children: ReactNode }) {
  const [isDemoMode, setIsDemoMode] = useState<boolean>(() => getStoredDemoMode());

  const enterDemoMode = useCallback(() => {
    setStoredDemoMode(true);
    setIsDemoMode(true);
  }, []);

  const exitDemoMode = useCallback(() => {
    clearStoredDemoMode();
    setIsDemoMode(false);
  }, []);

  const value: DemoContextType = {
    isDemoMode,
    enterDemoMode,
    exitDemoMode,
  };

  return <DemoContext.Provider value={value}>{children}</DemoContext.Provider>;
}
