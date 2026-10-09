import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { LOGIN_URL } from '../auth/auth';
import { signInAs } from '../auth/auth.testing';
import { authInterceptor } from './auth.interceptor';

const API = `${environment.apiBaseUrl}/api/v1/customers`;

describe('authInterceptor', () => {
  let http: HttpClient;
  let controller: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controller.verify());

  it('adds no Authorization header when logged out', () => {
    http.get(API).subscribe();
    const req = controller.expectOne(API);
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('adds the Bearer token to our API when logged in', () => {
    signInAs('AGENT');
    http.get(API).subscribe();
    const req = controller.expectOne(API);
    expect(req.request.headers.get('Authorization')).toBe('Bearer test-token-agent1');
    req.flush({});
  });

  it('never sends the token to another site', () => {
    signInAs('AGENT');
    http.get('https://example.com/data').subscribe();
    const req = controller.expectOne('https://example.com/data');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('does not add the token to the login call', () => {
    signInAs('AGENT');
    http.post(LOGIN_URL, {}).subscribe();
    const req = controller.expectOne(LOGIN_URL);
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });
});
