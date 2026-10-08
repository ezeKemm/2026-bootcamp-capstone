import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Auth } from '../auth/auth';
import { authInterceptor } from './auth.interceptor';

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
    http.get('/api/test').subscribe();
    const req = controller.expectOne('/api/test');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('adds the Bearer token when logged in', () => {
    TestBed.inject(Auth).login('agent', 'agent123').subscribe();
    http.get('/api/test').subscribe();
    const req = controller.expectOne('/api/test');
    expect(req.request.headers.get('Authorization')).toBe('Bearer demo-token-agent');
    req.flush({});
  });
});
