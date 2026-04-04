/* eslint-disable react-refresh/only-export-components */
import type { PropsWithChildren } from 'react';
import { createContext, useContext, useState } from 'react';

export type AppMode = 'invest' | 'quant';

const MODE_STORAGE_KEY = 'qerp.app-mode';

interface ModeContextValue {
  mode: AppMode;
  setMode: (mode: AppMode) => void;
  toggleMode: () => void;
}

const ModeContext = createContext<ModeContextValue | null>(null);

export function ModeProvider({ children }: PropsWithChildren) {
  const [mode, setModeState] = useState<AppMode>(() => {
    const stored = localStorage.getItem(MODE_STORAGE_KEY);
    return stored === 'quant' ? 'quant' : 'invest';
  });

  const setMode = (nextMode: AppMode) => {
    localStorage.setItem(MODE_STORAGE_KEY, nextMode);
    setModeState(nextMode);
  };

  const toggleMode = () => {
    setMode(mode === 'invest' ? 'quant' : 'invest');
  };

  return (
    <ModeContext.Provider value={{ mode, setMode, toggleMode }}>
      {children}
    </ModeContext.Provider>
  );
}

export function useMode() {
  const context = useContext(ModeContext);
  if (!context) {
    throw new Error('useMode must be used within ModeProvider');
  }
  return context;
}
