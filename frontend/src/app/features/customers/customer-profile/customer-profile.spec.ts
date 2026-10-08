import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { Auth } from '../../../core/auth/auth';
import { RecordInteractionForm } from '../../interactions/record-interaction-form/record-interaction-form';
import { CustomerProfile } from './customer-profile';

const AGENT: [string, string] = ['agent', 'agent123'];
const ADMIN: [string, string] = ['admin', 'admin123'];

function render(id: string, login?: [string, string]) {
  if (login) {
    TestBed.inject(Auth)
      .login(...login)
      .subscribe();
  }
  const fixture = TestBed.createComponent(CustomerProfile);
  fixture.componentRef.setInput('id', id);
  fixture.detectChanges();
  return { fixture, el: fixture.nativeElement as HTMLElement };
}

describe('CustomerProfile', () => {
  it('shows a known customer', () => {
    const { el } = render('CUS-1001', AGENT);
    expect(el.querySelector('h2')?.textContent).toContain('Amina');
    expect(el.querySelector('.status')?.textContent).toContain('ACTIVE');
  });

  it('shows not found for an unknown id', () => {
    const { el } = render('CUS-9999', AGENT);
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('CUS-9999');
  });

  describe('as an agent', () => {
    it('shows the record form and only my own interactions on an active customer', () => {
      const { el } = render('CUS-1001', AGENT);
      expect(el.querySelector('app-record-interaction-form')).not.toBeNull();
      expect(el.querySelector('#timeline-heading')?.textContent).toContain('My interactions');
      const items = el.querySelectorAll('.timeline li');
      expect(items.length).toBe(1);
      expect(items[0].textContent).toContain('Sent welcome pack.');
    });

    it('adds a recorded interaction to my list', () => {
      const { fixture, el } = render('CUS-1001', AGENT);
      const form = fixture.debugElement.query(By.directive(RecordInteractionForm))
        .componentInstance as RecordInteractionForm;
      form.recorded.emit({
        customerId: 'CUS-1001',
        channel: 'BRANCH',
        summary: 'Quarterly review.',
      });
      fixture.detectChanges();

      const items = el.querySelectorAll('.timeline li');
      expect(items.length).toBe(2);
      expect(items[0].textContent).toContain('Quarterly review.');
    });

    it('cannot record for a prospect until they are activated', () => {
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
    it('sees the full timeline but cannot record', () => {
      const { el } = render('CUS-1001', ADMIN);
      expect(el.querySelector('#timeline-heading')?.textContent).toContain('Full timeline');
      expect(el.querySelectorAll('.timeline li').length).toBe(2);
      expect(el.querySelector('app-record-interaction-form')).toBeNull();
    });

    it('can activate a prospect', () => {
      const { fixture, el } = render('CUS-1002', ADMIN);
      el.querySelector<HTMLButtonElement>('.activate')!.click();
      fixture.detectChanges();
      expect(el.querySelector('.status')?.textContent).toContain('ACTIVE');
    });
  });
});
