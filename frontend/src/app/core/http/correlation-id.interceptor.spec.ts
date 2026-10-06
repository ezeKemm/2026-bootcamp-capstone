import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import {
  CORRELATION_HEADER,
  DEFAULT_CORRELATION_ID,
  correlationIdInterceptor,
} from './correlation-id.interceptor';

describe('correlationIdInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([correlationIdInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('adds the correlation header when missing', () => {
    http.get('/api/v1/customers/CUS-1001').subscribe();
    const req = httpMock.expectOne('/api/v1/customers/CUS-1001');
    expect(req.request.headers.get(CORRELATION_HEADER)).toBe(DEFAULT_CORRELATION_ID);
    req.flush({});
  });

  it('keeps an existing correlation header', () => {
    http
      .get('/api/v1/customers/CUS-1001', {
        headers: { [CORRELATION_HEADER]: 'custom-id' },
      })
      .subscribe();
    const req = httpMock.expectOne('/api/v1/customers/CUS-1001');
    expect(req.request.headers.get(CORRELATION_HEADER)).toBe('custom-id');
    req.flush({});
  });
});
