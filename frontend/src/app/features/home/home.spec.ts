import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { Auth } from '../../core/auth/auth';
import { Home } from './home';

const URL = `${environment.apiBaseUrl}/api/v1/public/customers`;

const PAGE = {
  items: [
    { fullName: 'Amina Khan', status: 'ACTIVE' },
    { fullName: 'Ravi Singh', status: 'PROSPECT' },
  ],
  page: 0,
  size: 20,
  totalItems: 2,
  totalPages: 1,
};

/** loggedIn swaps in a fake Auth, so the test doesn't depend on how login works. */
async function setup(loggedIn = false) {
  await TestBed.configureTestingModule({
    imports: [Home],
    providers: [
      provideRouter([]),
      provideHttpClient(),
      provideHttpClientTesting(),
      { provide: Auth, useValue: { isLoggedIn: signal(loggedIn) } },
    ],
  }).compileComponents();

  const fixture = TestBed.createComponent(Home);
  const http = TestBed.inject(HttpTestingController);
  fixture.detectChanges();
  return { fixture, http, element: fixture.nativeElement as HTMLElement };
}

function respond(
  http: HttpTestingController,
  fixture: ComponentFixture<Home>,
  body: object = PAGE,
) {
  http.expectOne((req) => req.url === URL).flush(body);
  fixture.detectChanges();
}

describe('Home (guest customer list)', () => {
  afterEach(() => TestBed.inject(HttpTestingController).verify());

  it('shows loading, then the customers from the public API', async () => {
    const { fixture, http, element } = await setup();
    expect(element.textContent).toContain('Loading customers');

    respond(http, fixture);

    const rows = element.querySelectorAll('tbody tr');
    expect(rows.length).toBe(2);
    expect(rows[0].textContent).toContain('Amina Khan');
    expect(rows[0].textContent).toContain('Active');
    expect(rows[1].textContent).toContain('Prospect');
    expect(element.textContent).toContain('Showing 2 of 2');
  });

  it('asks for the first page with no status filter', async () => {
    const { http } = await setup();

    const req = http.expectOne((r) => r.url === URL);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('20');
    expect(req.request.params.has('status')).toBe(false);
    req.flush(PAGE);
  });

  it('does not let guests click into a customer', async () => {
    const { fixture, http, element } = await setup();
    respond(http, fixture);

    expect(element.querySelector('tbody button, tbody a')).toBeNull();
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

    expect(element.querySelector('.guest-banner')).toBeNull();
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
    expect(element.querySelector('[role="alert"]')?.textContent).toContain(
      'Cannot reach the server',
    );

    element.querySelector<HTMLButtonElement>('.retry-btn')?.click();
    respond(http, fixture);

    expect(element.querySelectorAll('tbody tr').length).toBe(2);
  });
});
