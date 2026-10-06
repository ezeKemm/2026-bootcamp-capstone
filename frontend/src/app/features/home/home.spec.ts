import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { Home } from './home';

function setup() {
  TestBed.configureTestingModule({ providers: [provideRouter([])] });
  const fixture = TestBed.createComponent(Home);
  const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  fixture.detectChanges();
  const el = fixture.nativeElement as HTMLElement;

  const submit = (value: string) => {
    const input = el.querySelector('input')!;
    input.value = value;
    input.dispatchEvent(new Event('input'));
    el.querySelector('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  };
  return { el, navigate, submit };
}

describe('Home', () => {
  it('shows an error and does not navigate when empty', () => {
    const { el, navigate, submit } = setup();
    submit('   ');
    expect(el.querySelector('.error')?.textContent).toContain('Enter a customer ID');
    expect(navigate).not.toHaveBeenCalled();
  });

  it('navigates to the customer profile with a trimmed ID', () => {
    const { navigate, submit } = setup();
    submit('  CUS-1001  ');
    expect(navigate).toHaveBeenCalledWith(['/customers', 'CUS-1001']);
  });
});
