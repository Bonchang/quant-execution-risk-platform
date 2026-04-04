import { describe, expect, it } from 'vitest';
import { decodeRoleFromJwt, hasRequiredRole } from './token';

function fakeJwt(payload: object) {
  return `x.${btoa(JSON.stringify(payload))}.y`;
}

describe('token helpers', () => {
  it('JWT payload에서 권한을 해석한다', () => {
    expect(decodeRoleFromJwt(fakeJwt({ auth: ['ROLE_ADMIN'] }))).toBe('ROLE_ADMIN');
  });

  it('권한 허용 집합을 검사한다', () => {
    expect(hasRequiredRole('ROLE_TRADER', ['ROLE_ADMIN', 'ROLE_TRADER'])).toBe(true);
    expect(hasRequiredRole('ROLE_VIEWER', ['ROLE_ADMIN', 'ROLE_TRADER'])).toBe(false);
  });
});
