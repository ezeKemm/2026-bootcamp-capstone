import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { Auth } from '../../core/auth/auth';
import { BrowseCustomers } from './browseCustomers';

const URL = `${environment.apiBaseUrl}/api/v1/customers`;

const PAGE = {
  items: [
    {
      customerId: '5d1c2e0c-3a0d-4b1e-9f9d-8a7d9f4a1c11',
      fullName: 'Amina Khan',
      email: 'amina.khan@example.com',
      status: 'ACTIVE',
    },
    {
      customerId: '7d3b8c73-99f7-4d06-8ce5-f8d8668ed2a1',
      fullName: 'Ravi Singh',
      email: 'ravi.singh@example.com',
      status: 'PROSPECT',
    },
  ],
  page: 0,
  size: 20,
  totalItems: 2,
  totalPages: 1,
};

async function setup(loggedIn = false) {
  await TestBed.configureTestingModule({
    imports: [BrowseCustomers],
    providers: [
      provideRouter([]),
      provideHttpClient(),
      provideHttpClientTesting(),
      { provide: Auth, useValue: { isLoggedIn: signal(loggedIn) } },
    ],
  }).compileComponents();

  const fixture = TestBed.createComponent(BrowseCustomers);
  const http = TestBed.inject(HttpTestingController);
  fixture.detectChanges();
  return { fixture, http, element: fixture.nativeElement as HTMLElement };
}

function respond(
  http: HttpTestingController,
  fixture: ComponentFixture<BrowseCustomers>,
  body: object = PAGE,
) {
  http.expectOne((req) => req.url === URL).flush(body);
  fixture.detectChanges();
}

describe('BrowseCustomers', () => {
  afterEach(() => TestBed.inject(HttpTestingController).verify());

  it('loads customer rows from the customer API', async () => {
    const { fixture, http, element } = await setup();
    expect(element.textContent).toContain('Loading customers');

    respond(http, fixture);

    const rows = element.querySelectorAll('tbody tr');
    expect(rows.length).toBe(2);
    expect(rows[0].textContent).toContain('Amina Khan');
    expect(rows[0].textContent).toContain('Active');
    expect(rows[1].textContent).toContain('Ravi Singh');
    expect(rows[1].textContent).toContain('Prospect');
    expect(element.textContent).toContain('Showing 2 of 2');
  });

  it('requests the first page without a status filter', async () => {
    const { http } = await setup();

    const req = http.expectOne((r) => r.url === URL);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('20');
    expect(req.request.params.has('status')).toBe(false);
    req.flush(PAGE);
  });

  it('navigates to the customer detail page when a row is clicked', async () => {
    const { fixture, http, element } = await setup();
    respond(http, fixture);

    const navigateSpy = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const row = element.querySelectorAll<HTMLTableRowElement>('tbody tr')[0];

    row.click();

    expect(navigateSpy).toHaveBeenCalledWith(['/customers', '5d1c2e0c-3a0d-4b1e-9f9d-8a7d9f4a1c11']);
  });

  it('shows the guest banner, and Sign in goes to the login page', async () => {
    const { fixture, http, element } = await setup();
    respond(http, fixture);

    const navigateSpy = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    expect(element.textContent).toContain('browsing as a guest');

    element.querySelector<HTMLButtonElement>('.sign-in-btn')?.click();

    expect(navigateSpy).toHaveBeenCalledWith(['/login']);
  });

  it('hides the guest banner when signed in', async () => {
    const { fixture, http, element } = await setup(true);
    respond(http, fixture);

    expect(element.querySelector('.role-banner')).toBeNull();
  });

  it('reloads from the server with the chosen status', async () => {
    const { fixture, http, element } = await setup();
    respond(http, fixture);

    element.querySelector<HTMLButtonElement>('.filter-btn')?.click();
    fixture.detectChanges();
    Array.from(element.querySelectorAll<HTMLButtonElement>('.menu-item'))
      .find((b) => b.textContent?.trim() === 'Prospect')
      ?.click();
    fixture.detectChanges();

    const req = http.expectOne((r) => r.url === URL);
    expect(req.request.params.get('status')).toBe('PROSPECT');
    req.flush({ ...PAGE, items: [PAGE.items[1]], totalItems: 1 });
    fixture.detectChanges();

    expect(element.querySelectorAll('tbody tr').length).toBe(1);
    expect(element.textContent).toContain('Showing 1 of 1');
  });

  it('shows an error with a retry button when the server is down', async () => {
    const { fixture, http, element } = await setup();

    http.expectOne((r) => r.url === URL).error(new ProgressEvent('error'));
    fixture.detectChanges();
    expect(element.querySelector('[role="alert"]')?.textContent).toContain('Cannot reach the server');

    element.querySelector<HTMLButtonElement>('.retry-btn')?.click();
    respond(http, fixture);

    expect(element.querySelectorAll('tbody tr').length).toBe(2);
  });
});
