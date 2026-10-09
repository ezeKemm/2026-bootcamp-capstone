import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { By } from '@angular/platform-browser';
import { UserRole } from '../../../core/auth/auth';
import { signInAs } from '../../../core/auth/auth.testing';
import { RecordInteractionForm } from '../../interactions/record-interaction-form/record-interaction-form';
import { CustomerProfile } from './customer-profile';

const AGENT: UserRole = 'AGENT';
const ADMIN: UserRole = 'ADMIN';

function render(id: string, role?: UserRole) {
  if (role) {
    signInAs(role);
  }
  const fixture = TestBed.createComponent(CustomerProfile);
  fixture.componentRef.setInput('id', id);
  fixture.detectChanges();
  return { fixture, el: fixture.nativeElement as HTMLElement };
}


describe('CustomerProfile', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CustomerProfile],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
  });

  it('shows the profile header and customer details', () => {
    const { el } = render('CUS-1001', AGENT);

    expect(el.querySelector('.profile-header h2')?.textContent).toContain('Amina');
    expect(el.querySelector('.customer-id')?.textContent).toContain('CUS-1001');
    expect(el.querySelector('.status')?.textContent).toContain('ACTIVE');
    expect(el.querySelector('#details-heading')?.textContent).toContain('Contact details');
    expect(el.textContent).toContain('amina@example.com');
  });

  it('shows a not-found message for an unknown customer ID', () => {
    const { el } = render('CUS-9999', AGENT);

    expect(el.querySelector('[role="alert"]')?.textContent).toContain('CUS-9999');
    expect(el.querySelector('.profile-page')).toBeNull();
  });

  describe('as an agent', () => {
    it('shows the record form and only the agent’s interactions for an active customer', () => {
      const { el } = render('CUS-1001', AGENT);

      expect(el.querySelector('app-record-interaction-form')).not.toBeNull();
      expect(el.querySelector('#timeline-heading')?.textContent)
        .toContain('My interactions');

      const items = el.querySelectorAll('.timeline li');
      expect(items.length).toBe(1);
      expect(items[0].textContent).toContain('Sent welcome pack.');
    });

    it('adds a recorded interaction to the timeline', () => {
      const { fixture, el } = render('CUS-1001', AGENT);
      const form = fixture.debugElement
        .query(By.directive(RecordInteractionForm))
        .componentInstance as RecordInteractionForm;

      form.recorded.emit({
        customerId: 'CUS-1001',
        channel: 'PHONE',
        summary: 'Quarterly review.',
      });
      fixture.detectChanges();

      const items = el.querySelectorAll('.timeline li');
      expect(items.length).toBe(2);
      expect(items[0].textContent).toContain('Quarterly review.');
    });

    it('shows the recording hint until a prospect is activated', () => {
      const { fixture, el } = render('CUS-1002', AGENT);

      expect(el.querySelector('app-record-interaction-form')).toBeNull();
      expect(el.querySelector('.hint')).not.toBeNull();

      el.querySelector<HTMLButtonElement>('.activate')!.click();
      fixture.detectChanges();

      expect(el.querySelector('.status')?.textContent).toContain('ACTIVE');
      expect(el.querySelector('.activate')).toBeNull();
      expect(el.querySelector('app-record-interaction-form')).not.toBeNull();
    });
  });

  describe('as an admin', () => {
    it('shows the full timeline and does not show the record form', () => {
      const { el } = render('CUS-1001', ADMIN);

      expect(el.querySelector('#timeline-heading')?.textContent)
        .toContain('Full interaction timeline');
      expect(el.querySelectorAll('.timeline li').length).toBe(2);
      expect(el.querySelector('app-record-interaction-form')).toBeNull();
    });

    it('can activate a prospect', () => {
      const { fixture, el } = render('CUS-1002', ADMIN);

      el.querySelector<HTMLButtonElement>('.activate')!.click();
      fixture.detectChanges();

      expect(el.querySelector('.status')?.textContent).toContain('ACTIVE');
      expect(el.querySelector('.activate')).toBeNull();
    });
  });
});
