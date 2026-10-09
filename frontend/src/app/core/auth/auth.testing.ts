import { TestBed } from '@angular/core/testing';
import { Auth, UserRole } from './auth';

/** Test-only: signs in as agent1 or admin1 without calling the backend. */
export function signInAs(role: UserRole): void {
  const username = role === 'ADMIN' ? 'admin1' : 'agent1';
  TestBed.inject(Auth).startSession({
    accessToken: `test-token-${username}`,
    tokenType: 'Bearer',
    username,
    role,
  });
}
