import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { environment } from '../../../../environments/environment';
import { UserRole } from '../../../core/auth/auth';
import { signInAs } from '../../../core/auth/auth.testing';
import { RecordInteractionForm } from '../../interactions/record-interaction-form/record-interaction-form';
import { Customer } from '../customer.model';
import { CustomerProfile } from './customer-profile';

const BASE = `${environment.apiBaseUrl}/api/v1/customers`;
const AMINA_ID = '5d1c2e0c-3a0d-4b1e-9f9d-8a7d9f4a1c11';
const RAVI_ID = '7d3b8c73-99f7-4d06-8ce5-f8d8668ed2a1';

const AMINA: Customer = {
  customerId: AMINA_ID,
  fullName: 'Amina Khan',
  email: 'amina.khan@example.com',
  status: 'ACTIVE',
};
const RAVI: Customer = {
  customerId: RAVI_ID,
  fullName: 'Ravi Singh',
  email: 'ravi.singh@example.com',
  status: 'PROSPECT',
};

function render(id: string, role: UserRole = 'AGENT') {
  signInAs(role);
  const fixture = TestBed.createComponent(CustomerProfile);
  fixture.componentRef.setInput('id', id);
  fixture.detectChanges();
  const http = TestBed.inject(HttpTestingController);
  return { fixture, http, el: fixture.nativeElement as HTMLElement };
}

/** Answers the GET for this customer, then re-renders. */
function respond(
  http: HttpTestingController,
  fixture: ComponentFixture<CustomerProfile>,
  customer: Customer,
) {
  http.expectOne(`${BASE}/${customer.customerId}`).flush(customer);
  fixture.detectChanges();
}

describe('CustomerProfile', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CustomerProfile],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
  });

  afterEach(() => TestBed.inject(HttpTestingController).verify());

  it('shows loading, then the customer from the API', () => {
    const { fixture, http, el } = render(AMINA_ID);
    expect(el.textContent).toContain('Loading customer');

    respond(http, fixture, AMINA);

    expect(el.querySelector('.profile-header h2')?.textContent).toContain('Amina Khan');
    expect(el.querySelector('.customer-id')?.textContent).toContain(AMINA_ID);
    expect(el.querySelector('.status')?.textContent).toContain('ACTIVE');
    expect(el.textContent).toContain('amina.khan@example.com');
  });

  it('shows not found when the backend returns 404', () => {
    const { fixture, http, el } = render(AMINA_ID);

    http
      .expectOne(`${BASE}/${AMINA_ID}`)
      .flush(
        { title: 'Customer not found', status: 404 },
        { status: 404, statusText: 'Not Found' },
      );
    fixture.detectChanges();

    expect(el.querySelector('.not-found')?.textContent).toContain(AMINA_ID);
    expect(el.querySelector('.profile-page')).toBeNull();
  });

  it('treats an invalid id (400) as not found', () => {
    const { fixture, http, el } = render('CUS-1001');

    http
      .expectOne(`${BASE}/CUS-1001`)
      .flush({ title: 'Bad Request', status: 400 }, { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges();

    expect(el.querySelector('.not-found')?.textContent).toContain('CUS-1001');
  });

  it('shows an error with retry when the server is unreachable', () => {
    const { fixture, http, el } = render(AMINA_ID);

    http.expectOne(`${BASE}/${AMINA_ID}`).error(new ProgressEvent('error'));
    fixture.detectChanges();
    expect(el.querySelector('.error-box')?.textContent).toContain('Cannot reach the server');

    el.querySelector<HTMLButtonElement>('.retry-btn')!.click();
    fixture.detectChanges();
    respond(http, fixture, AMINA);

    expect(el.querySelector('.profile-header h2')?.textContent).toContain('Amina Khan');
  });

  describe('as an agent', () => {
    it('shows the record form for an active customer', () => {
      const { fixture, http, el } = render(AMINA_ID, 'AGENT');
      respond(http, fixture, AMINA);

      expect(el.querySelector('#timeline-heading')?.textContent).toContain('My interactions');
      expect(el.querySelector('app-record-interaction-form')).not.toBeNull();
    });

    it('adds a recorded interaction to the timeline', () => {
      const { fixture, http, el } = render(AMINA_ID, 'AGENT');
      respond(http, fixture, AMINA);

      const form = fixture.debugElement.query(By.directive(RecordInteractionForm))
        .componentInstance as RecordInteractionForm;
      form.recorded.emit({ customerId: AMINA_ID, channel: 'PHONE', summary: 'Quarterly review.' });
      fixture.detectChanges();

      const items = el.querySelectorAll('.timeline li');
      expect(items.length).toBe(1);
      expect(items[0].textContent).toContain('Quarterly review.');
    });

    it('shows the hint instead of the form for a prospect', () => {
      const { fixture, http, el } = render(RAVI_ID, 'AGENT');
      respond(http, fixture, RAVI);

      expect(el.querySelector('app-record-interaction-form')).toBeNull();
      expect(el.querySelector('.hint')).not.toBeNull();
    });

    it('activates a prospect through the API', () => {
      const { fixture, http, el } = render(RAVI_ID, 'AGENT');
      respond(http, fixture, RAVI);

      el.querySelector<HTMLButtonElement>('.activate')!.click();
      fixture.detectChanges();

      const req = http.expectOne(`${BASE}/${RAVI_ID}/activate`);
      expect(req.request.method).toBe('POST');
      req.flush({ ...RAVI, status: 'ACTIVE' });
      fixture.detectChanges();

      expect(el.querySelector('.status')?.textContent).toContain('ACTIVE');
      expect(el.querySelector('.activate')).toBeNull();
      expect(el.querySelector('app-record-interaction-form')).not.toBeNull();
    });

    it('shows an error and stays a prospect when activation fails', () => {
      const { fixture, http, el } = render(RAVI_ID, 'AGENT');
      respond(http, fixture, RAVI);

      el.querySelector<HTMLButtonElement>('.activate')!.click();
      fixture.detectChanges();
      http
        .expectOne(`${BASE}/${RAVI_ID}/activate`)
        .flush(
          { title: 'Business rule violated', status: 422 },
          { status: 422, statusText: 'Unprocessable Content' },
        );
      fixture.detectChanges();

      expect(el.querySelector('.activate-error')?.textContent).toContain('Business rule violated');
      expect(el.querySelector('.status')?.textContent).toContain('PROSPECT');
    });
  });

  describe('as an admin', () => {
    it('shows the full timeline heading and no record form', () => {
      const { fixture, http, el } = render(AMINA_ID, 'ADMIN');
      respond(http, fixture, AMINA);

      expect(el.querySelector('#timeline-heading')?.textContent).toContain(
        'Full interaction timeline',
      );
      expect(el.querySelector('app-record-interaction-form')).toBeNull();
    });
  });
});
