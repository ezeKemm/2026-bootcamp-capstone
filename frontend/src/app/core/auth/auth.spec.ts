import { TestBed } from '@angular/core/testing';
import { Auth } from './auth';

describe('Auth', () => {
  let auth: Auth;

  beforeEach(() => {
    auth = TestBed.inject(Auth);
  });

  it('logs in a demo agent', () => {
    auth.login('agent', 'agent123').subscribe();
    expect(auth.isLoggedIn()).toBe(true);
    expect(auth.user()).toEqual({ username: 'agent', role: 'AGENT' });
    expect(auth.accessToken()).toBe('demo-token-agent');
  });

  it('logs in a demo admin, ignoring case and spaces in the username', () => {
    auth.login('  Admin ', 'admin123').subscribe();
    expect(auth.user()?.role).toBe('ADMIN');
  });

  it('rejects a wrong password', () => {
    let message = '';
    auth.login('agent', 'nope').subscribe({ error: (e: Error) => (message = e.message) });
    expect(message).toBe('Invalid username or password');
    expect(auth.isLoggedIn()).toBe(false);
    expect(auth.accessToken()).toBeNull();
  });

  it('logs out', () => {
    auth.login('agent', 'agent123').subscribe();
    auth.logout();
    expect(auth.isLoggedIn()).toBe(false);
    expect(auth.accessToken()).toBeNull();
  });
});
