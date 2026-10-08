import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, map, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { toApiError } from '../http/api-error';

export type UserRole = 'AGENT' | 'ADMIN';

export interface AuthUser {
  username: string;
  role: UserRole;
}

/** Body of a successful POST /api/v1/auth/login (see openapi.yaml). */
export interface LoginResponse {
  accessToken: string;
  tokenType: 'Bearer';
  username: string;
  role: UserRole;
}

export const LOGIN_URL = `${environment.apiBaseUrl}/api/v1/auth/login`;

@Injectable({ providedIn: 'root' })
export class Auth {
  private readonly http = inject(HttpClient);
  private readonly currentUser = signal<AuthUser | null>(null);
  private token: string | null = null;

  /** The logged-in user, or null. Read-only from outside. */
  readonly user = this.currentUser.asReadonly();
  readonly isLoggedIn = computed(() => this.currentUser() !== null);

  /** Signs in with the backend. The token is kept in memory only (refresh = logged out). */
  login(username: string, password: string): Observable<AuthUser> {
    return this.http.post<LoginResponse>(LOGIN_URL, { username: username.trim(), password }).pipe(
      map((response) => this.startSession(response)),
      catchError((error: HttpErrorResponse) => throwError(() => toApiError(error))),
    );
  }

  /** Stores a session from a login response. Public so tests can sign in without HTTP. */
  startSession(response: LoginResponse): AuthUser {
    const user: AuthUser = { username: response.username, role: response.role };
    this.token = response.accessToken;
    this.currentUser.set(user);
    return user;
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
