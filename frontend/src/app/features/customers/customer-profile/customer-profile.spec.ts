import { TestBed } from '@angular/core/testing';
import { CustomerProfile } from './customer-profile';

function render(id: string) {
  const fixture = TestBed.createComponent(CustomerProfile);
  fixture.componentRef.setInput('id', id);
  fixture.detectChanges();
  return fixture.nativeElement as HTMLElement;
}

describe('CustomerProfile', () => {
  it('shows a known customer', () => {
    const el = render('CUS-1001');
    expect(el.querySelector('h2')?.textContent).toContain('Amina');
    expect(el.textContent).toContain('ACTIVE');
  });

  it('shows not found for an unknown id', () => {
    const el = render('CUS-9999');
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('CUS-9999');
  });
});
