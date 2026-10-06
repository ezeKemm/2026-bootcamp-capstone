import { HttpInterceptorFn } from '@angular/common/http';

export const CORRELATION_HEADER = 'X-Correlation-Id';
// Course fixture used in demos and evidence. Can switch to a per-request ID later.
export const DEFAULT_CORRELATION_ID = 'lab-request-001';

export const correlationIdInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.headers.has(CORRELATION_HEADER)) {
    return next(req);
  }
  return next(req.clone({ setHeaders: { [CORRELATION_HEADER]: DEFAULT_CORRELATION_ID } }));
};
