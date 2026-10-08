import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { Auth, LOGIN_URL } from '../../core/auth/auth';
import { Login } from './login';

function setup(returnUrl?: string) {
  TestBed.configureTestingModule({
    providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
  });
  const fixture = TestBed.createComponent(Login);
  if (returnUrl !== undefined) {
    fixture.componentRef.setInput('returnUrl', returnUrl);
  }
  const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
  const http = TestBed.inject(HttpTestingController);
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
  const ok = (username: string, role: 'AGENT' | 'ADMIN') =>
    http.expectOne(LOGIN_URL).flush({ accessToken: 't', tokenType: 'Bearer', username, role });

  return { el, navigate, signIn, http, fixture, ok };
}

describe('Login', () => {
  afterEach(() => TestBed.inject(HttpTestingController).verify());

  it('shows field errors when empty and does not call the backend', () => {
    const { el, navigate, signIn, http } = setup();
    signIn('', '');
    expect(el.querySelectorAll('.error').length).toBe(2);
    http.expectNone(LOGIN_URL);
    expect(navigate).not.toHaveBeenCalled();
  });

  it('shows the backend message for wrong credentials', () => {
    const { el, navigate, signIn, http, fixture } = setup();
    signIn('agent1', 'wrong');
    http
      .expectOne(LOGIN_URL)
      .flush(
        { title: 'Invalid username or password', status: 401 },
        { status: 401, statusText: 'Unauthorized' },
      );
    fixture.detectChanges();

    expect(el.querySelector('[role="alert"]')?.textContent).toContain(
      'Invalid username or password',
    );
    expect(navigate).not.toHaveBeenCalled();
    expect(TestBed.inject(Auth).isLoggedIn()).toBe(false);
  });

  it('says so when the server cannot be reached', () => {
    const { el, signIn, http, fixture } = setup();
    signIn('agent1', 'agent1');
    http.expectOne(LOGIN_URL).error(new ProgressEvent('error'));
    fixture.detectChanges();

    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Cannot reach the server');
  });

  it('logs in and goes to the returnUrl', () => {
    const { navigate, signIn, ok } = setup('/customers/CUS-1001');
    signIn('agent1', 'agent1');
    ok('agent1', 'AGENT');

    expect(TestBed.inject(Auth).isLoggedIn()).toBe(true);
    expect(navigate).toHaveBeenCalledWith('/customers/CUS-1001');
  });

  it('ignores a returnUrl that points outside the app', () => {
    const { navigate, signIn, ok } = setup('https://evil.example');
    signIn('admin1', 'admin1');
    ok('admin1', 'ADMIN');

    expect(navigate).toHaveBeenCalledWith('/');
  });
});
