import type { Role } from '../types/api';

export const TOKEN_STORAGE_KEY = 'qerp.jwt';
export const ROLE_STORAGE_KEY = 'qerp.role';

export function decodeRoleFromJwt(token: string): Role {
  try {
    const payload = JSON.parse(atob(token.split('.')[1] ?? ''));
    const role = Array.isArray(payload.roles) ? payload.roles[0] : payload.roles;
    return typeof role === 'string' ? (role as Role) : '';
  } catch {
    return '';
  }
}

export function hasRequiredRole(role: Role, allowed: Role[]) {
  return allowed.includes(role);
}
