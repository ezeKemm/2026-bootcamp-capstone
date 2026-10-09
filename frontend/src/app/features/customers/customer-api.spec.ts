import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { ApiError } from '../../core/http/api-error';
import { CustomerApi } from './customer-api';
import { Customer } from './customer.model';

const base = `${environment.apiBaseUrl}/api/v1/customers`;
const amina: Customer = {
  customerId: 'CUS-1001',
  fullName: 'Amina Example',
  email: 'amina@example.com',
  status: 'ACTIVE',
};

describe('CustomerApi', () => {
  let api: CustomerApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(CustomerApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('gets a customer', () => {
    let result: Customer | undefined;
    api.getCustomer('CUS-1001').subscribe((c) => (result = c));

    const req = http.expectOne(`${base}/CUS-1001`);
    expect(req.request.method).toBe('GET');
    req.flush(amina);

    expect(result).toEqual(amina);
  });

  it('activates a customer with an empty POST', () => {
    let result: Customer | undefined;
    api.activateCustomer('CUS-1002').subscribe((c) => (result = c));

    const req = http.expectOne(`${base}/CUS-1002/activate`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toBeNull();
    req.flush({ ...amina, customerId: 'CUS-1002', status: 'ACTIVE' });

    expect(result?.status).toBe('ACTIVE');
  });

  it('records an interaction with only channel and summary in the body', () => {
    api
      .recordInteraction({
        customerId: 'CUS-1001',
        channel: 'PHONE',
        summary: 'Called about renewal.',
      })
      .subscribe();

    const req = http.expectOne(`${base}/CUS-1001/interactions`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ channel: 'PHONE', summary: 'Called about renewal.' });
    req.flush(
      {
        interactionId: '6f1c2b7e-8d3a-4c51-9b2e-0a7d4e9f1c23',
        customerId: 'CUS-1001',
        channel: 'PHONE',
        summary: 'Called about renewal.',
        actor: 'demo.user',
        occurredAt: '2026-10-07T14:30:00Z',
        correlationId: 'lab-request-001',
      },
      { status: 201, statusText: 'Created' },
    );
  });

  it('turns a 404 Problem Details into an ApiError', () => {
    let error: ApiError | undefined;
    api.getCustomer('CUS-9999').subscribe({ error: (e: ApiError) => (error = e) });

    http.expectOne(`${base}/CUS-9999`).flush(
      {
        title: 'Customer not found',
        status: 404,
        detail: 'No customer with id CUS-9999.',
        correlationId: 'lab-request-001',
      },
      { status: 404, statusText: 'Not Found' },
    );

    expect(error).toEqual({
      status: 404,
      title: 'Customer not found',
      detail: 'No customer with id CUS-9999.',
      fieldErrors: [],
      correlationId: 'lab-request-001',
    });
  });

  it('keeps per-field errors from a 400', () => {
    let error: ApiError | undefined;
    api
      .recordInteraction({ customerId: 'CUS-1001', channel: 'PHONE', summary: ' ' })
      .subscribe({ error: (e: ApiError) => (error = e) });

    http.expectOne(`${base}/CUS-1001/interactions`).flush(
      {
        title: 'Validation failed',
        status: 400,
        errors: [{ field: 'summary', message: 'must not be blank' }],
      },
      { status: 400, statusText: 'Bad Request' },
    );

    expect(error?.status).toBe(400);
    expect(error?.fieldErrors).toEqual([{ field: 'summary', message: 'must not be blank' }]);
  });

  it('reports an unreachable server as status 0', () => {
    let error: ApiError | undefined;
    api.getCustomer('CUS-1001').subscribe({ error: (e: ApiError) => (error = e) });

    http.expectOne(`${base}/CUS-1001`).error(new ProgressEvent('error'));

    expect(error?.status).toBe(0);
    expect(error?.title).toBe('Cannot reach the server');
  });
});
