import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ApiError } from '../http/api-error';
import { Auth, AuthUser, LOGIN_URL } from './auth';

describe('Auth', () => {
  let auth: Auth;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    auth = TestBed.inject(Auth);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('logs in through the backend and keeps the token in memory', () => {
    let user: AuthUser | undefined;
    auth.login(' agent1 ', 'agent1').subscribe((u) => (user = u));

    const req = http.expectOne(LOGIN_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ username: 'agent1', password: 'agent1' });
    req.flush({ accessToken: 'jwt-123', tokenType: 'Bearer', username: 'agent1', role: 'AGENT' });

    expect(user).toEqual({ username: 'agent1', role: 'AGENT' });
    expect(auth.isLoggedIn()).toBe(true);
    expect(auth.accessToken()).toBe('jwt-123');
  });

  it('turns a 401 into an ApiError and stays logged out', () => {
    let error: ApiError | undefined;
    auth.login('agent1', 'wrong').subscribe({ error: (e: ApiError) => (error = e) });

    http
      .expectOne(LOGIN_URL)
      .flush(
        { title: 'Invalid username or password', status: 401 },
        { status: 401, statusText: 'Unauthorized' },
      );

    expect(error?.status).toBe(401);
    expect(error?.title).toBe('Invalid username or password');
    expect(auth.isLoggedIn()).toBe(false);
    expect(auth.accessToken()).toBeNull();
  });

  it('logs out', () => {
    auth.startSession({ accessToken: 't', tokenType: 'Bearer', username: 'agent1', role: 'AGENT' });
    auth.logout();
    expect(auth.isLoggedIn()).toBe(false);
    expect(auth.accessToken()).toBeNull();
  });
});
