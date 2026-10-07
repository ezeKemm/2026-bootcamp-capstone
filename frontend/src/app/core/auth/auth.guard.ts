import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Auth } from './auth';

/** Lets logged-in users through; otherwise sends them to /login?returnUrl=<where they were going>. */
export const authGuard: CanActivateFn = (_route, state) => {
  if (inject(Auth).isLoggedIn()) {
    return true;
  }
  return inject(Router).createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};
