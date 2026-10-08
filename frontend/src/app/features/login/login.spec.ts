import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Auth } from '../../core/auth/auth';
import { Login } from './login';

function setup(returnUrl?: string) {
  TestBed.configureTestingModule({ providers: [provideRouter([])] });
  const fixture = TestBed.createComponent(Login);
  if (returnUrl !== undefined) {
    fixture.componentRef.setInput('returnUrl', returnUrl);
  }
  const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
  fixture.detectChanges();
  const el = fixture.nativeElement as HTMLElement;

  const signIn = (username: string, password: string) => {
    const [user, pass] = Array.from(el.querySelectorAll('input'));
    user.value = username;
    user.dispatchEvent(new Event('input'));
    pass.value = password;
    pass.dispatchEvent(new Event('input'));
    el.querySelector('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  };
  return { el, navigate, signIn };
}

describe('Login', () => {
  it('shows field errors when empty and does not log in', () => {
    const { el, navigate, signIn } = setup();
    signIn('', '');
    expect(el.querySelectorAll('.error').length).toBe(2);
    expect(navigate).not.toHaveBeenCalled();
  });

  it('shows an error for wrong credentials', () => {
    const { el, navigate, signIn } = setup();
    signIn('agent', 'wrong');
    expect(el.querySelector('[role="alert"]')?.textContent).toContain(
      'Invalid username or password',
    );
    expect(navigate).not.toHaveBeenCalled();
    expect(TestBed.inject(Auth).isLoggedIn()).toBe(false);
  });

  it('logs in and goes to the returnUrl', () => {
    const { navigate, signIn } = setup('/customers/CUS-1001');
    signIn('agent', 'agent123');
    expect(TestBed.inject(Auth).isLoggedIn()).toBe(true);
    expect(navigate).toHaveBeenCalledWith('/customers/CUS-1001');
  });

  it('ignores a returnUrl that points outside the app', () => {
    const { navigate, signIn } = setup('https://evil.example');
    signIn('admin', 'admin123');
    expect(navigate).toHaveBeenCalledWith('/');
  });
});
