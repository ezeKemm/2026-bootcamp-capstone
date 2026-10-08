import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  Router,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';
import { Auth } from './auth';
import { authGuard } from './auth.guard';

function runGuard(url: string) {
  return TestBed.runInInjectionContext(() =>
    authGuard({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot),
  );
}

describe('authGuard', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
  });

  it('redirects to login with a returnUrl when logged out', () => {
    const result = runGuard('/customers/CUS-1001') as UrlTree;
    expect(TestBed.inject(Router).serializeUrl(result).startsWith('/login')).toBe(true);
    expect(result.queryParams['returnUrl']).toBe('/customers/CUS-1001');
  });

  it('allows the page when logged in', () => {
    TestBed.inject(Auth).login('agent', 'agent123').subscribe();
    expect(runGuard('/customers/CUS-1001')).toBe(true);
  });
});
