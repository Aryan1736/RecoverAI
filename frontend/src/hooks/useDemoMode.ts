import { useContext } from 'react';
import { DemoContext, type DemoContextType } from '../context/demo-context-def';

export function useDemoMode(): DemoContextType {
  const context = useContext(DemoContext);
  if (!context) {
    return {
      isDemoMode: false,
      enterDemoMode: () => {},
      exitDemoMode: () => {},
    };
  }
  return context;
}
