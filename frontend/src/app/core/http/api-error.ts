import { HttpErrorResponse } from '@angular/common/http';

export interface FieldError {
  field: string;
  message: string;
}

/** One predictable error shape for every screen. status 0 = server unreachable. */
export interface ApiError {
  status: number;
  title: string;
  detail?: string;
  fieldErrors: FieldError[];
  correlationId?: string;
}

/** Problem Details body from the backend (see openapi.yaml). */
interface ProblemDetails {
  title?: string;
  detail?: string;
  correlationId?: string;
  errors?: FieldError[];
}

const DEFAULT_TITLES: Record<number, string> = {
  0: 'Cannot reach the server',
  400: 'Some fields are invalid',
  401: 'Please sign in',
  403: 'You do not have permission to do that',
  404: 'Not found',
  409: 'That conflicts with the current state',
  422: 'The request could not be processed',
  503: 'Service temporarily unavailable',
};

export function toApiError(error: HttpErrorResponse): ApiError {
  const body: ProblemDetails =
    error.status !== 0 && typeof error.error === 'object' && error.error !== null
      ? error.error
      : {};

  return {
    status: error.status,
    title: body.title ?? DEFAULT_TITLES[error.status] ?? 'Something went wrong',
    detail: body.detail,
    fieldErrors: Array.isArray(body.errors) ? body.errors : [],
    correlationId: body.correlationId ?? error.headers.get('X-Correlation-Id') ?? undefined,
  };
}
