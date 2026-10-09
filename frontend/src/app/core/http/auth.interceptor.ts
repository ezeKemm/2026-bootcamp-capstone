import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { Auth, LOGIN_URL } from '../auth/auth';

/** Adds "Authorization: Bearer <token>" only to our own API (never to other sites or the login call). */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = inject(Auth).accessToken();
  const isOurApi = req.url.startsWith(`${environment.apiBaseUrl}/api/v1/`);

  if (!token || !isOurApi || req.url === LOGIN_URL || req.headers.has('Authorization')) {
    return next(req);
  }
  return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};
