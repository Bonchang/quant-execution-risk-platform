/* eslint-disable react-refresh/only-export-components */
import type { PropsWithChildren } from 'react';
import { createContext, useContext, useState } from 'react';
import type { AuthTokenResponse, Role } from '../types/api';
import { decodeRoleFromJwt, ROLE_STORAGE_KEY, TOKEN_STORAGE_KEY } from './token';

interface AuthContextValue {
  token: string;
  role: Role;
  login: (payload: AuthTokenResponse) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: PropsWithChildren) {
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_STORAGE_KEY) ?? '');
  const [role, setRole] = useState<Role>(() => (localStorage.getItem(ROLE_STORAGE_KEY) as Role) || decodeRoleFromJwt(localStorage.getItem(TOKEN_STORAGE_KEY) ?? ''));

  const login = (payload: AuthTokenResponse) => {
    localStorage.setItem(TOKEN_STORAGE_KEY, payload.accessToken);
    localStorage.setItem(ROLE_STORAGE_KEY, payload.role);
    setToken(payload.accessToken);
    setRole(payload.role || decodeRoleFromJwt(payload.accessToken));
  };

  const logout = () => {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    localStorage.removeItem(ROLE_STORAGE_KEY);
    setToken('');
    setRole('');
  };

  return (
    <AuthContext.Provider value={{ token, role, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
}
