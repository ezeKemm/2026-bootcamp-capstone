import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { environment } from '../../../environments/environment';
import { Auth, LOGIN_URL } from '../auth/auth';
import { signInAs } from '../auth/auth.testing';
import { unauthorizedInterceptor } from './unauthorized.interceptor';

const API = `${environment.apiBaseUrl}/api/v1/customers`;

describe('unauthorizedInterceptor', () => {
  let http: HttpClient;
  let controller: HttpTestingController;
  let navigate: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([unauthorizedInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
    navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  });

  afterEach(() => controller.verify());

  it('logs out and goes to login on a 401', () => {
    signInAs('AGENT');
    http.get(API).subscribe({ error: () => undefined });
    controller.expectOne(API).flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(TestBed.inject(Auth).isLoggedIn()).toBe(false);
    expect(navigate).toHaveBeenCalled();
    expect(navigate.mock.calls[0][0]).toEqual(['/login']);
  });

  it('stays logged in on a 403', () => {
    signInAs('ADMIN');
    http.get(API).subscribe({ error: () => undefined });
    controller.expectOne(API).flush({}, { status: 403, statusText: 'Forbidden' });

    expect(TestBed.inject(Auth).isLoggedIn()).toBe(true);
    expect(navigate).not.toHaveBeenCalled();
  });

  it('ignores a 401 from the login call itself', () => {
    http.post(LOGIN_URL, {}).subscribe({ error: () => undefined });
    controller.expectOne(LOGIN_URL).flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(navigate).not.toHaveBeenCalled();
  });
});
