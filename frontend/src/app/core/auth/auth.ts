import { Injectable, computed, signal } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';

export type UserRole = 'AGENT' | 'ADMIN';

export interface AuthUser {
  username: string;
  role: UserRole;
}

// DEMO ONLY - not real security. Replaced by the backend login endpoint once it exists.
const DEMO_ACCOUNTS: Record<string, { password: string; role: UserRole }> = {
  agent: { password: 'agent123', role: 'AGENT' },
  admin: { password: 'admin123', role: 'ADMIN' },
};

@Injectable({ providedIn: 'root' })
export class Auth {
  private readonly currentUser = signal<AuthUser | null>(null);
  private token: string | null = null;

  /** The logged-in user, or null. Read-only from outside. */
  readonly user = this.currentUser.asReadonly();
  readonly isLoggedIn = computed(() => this.currentUser() !== null);

  login(username: string, password: string): Observable<AuthUser> {
    const name = username.trim().toLowerCase();
    const account = Object.hasOwn(DEMO_ACCOUNTS, name) ? DEMO_ACCOUNTS[name] : undefined;

    if (!account || account.password !== password) {
      return throwError(() => new Error('Invalid username or password'));
    }

    const user: AuthUser = { username: name, role: account.role };
    this.token = `demo-token-${name}`;
    this.currentUser.set(user);
    return of(user);
  }

  logout(): void {
    this.token = null;
    this.currentUser.set(null);
  }

  /** Token for the Authorization header (null when logged out). */
  accessToken(): string | null {
    return this.token;
  }
}
