import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, OperatorFunction, catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { toApiError } from '../../core/http/api-error';
import { Interaction, NewInteraction } from '../interactions/interaction.model';
import { Customer } from './customer.model';

/** Turns any HTTP failure into an ApiError so screens get one predictable error shape. */
function asApiError<T>(): OperatorFunction<T, T> {
  return catchError((error: HttpErrorResponse) => throwError(() => toApiError(error)));
}

@Injectable({ providedIn: 'root' })
export class CustomerApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/v1/customers`;

  /** GET /api/v1/customers/{customerId} */
  getCustomer(customerId: string): Observable<Customer> {
    return this.http.get<Customer>(this.customerUrl(customerId)).pipe(asApiError());
  }

  /** POST /api/v1/customers/{customerId}/activate (no body) */
  activateCustomer(customerId: string): Observable<Customer> {
    return this.http
      .post<Customer>(`${this.customerUrl(customerId)}/activate`, null)
      .pipe(asApiError());
  }

  /** POST /api/v1/customers/{customerId}/interaction with body { channel, summary } */
  recordInteraction(interaction: NewInteraction): Observable<Interaction> {
    const { customerId, channel, summary } = interaction;
    return this.http
      .post<Interaction>(`${this.customerUrl(customerId)}/interaction`, { channel, summary })
      .pipe(asApiError());
  }

  private customerUrl(customerId: string): string {
    return `${this.baseUrl}/${encodeURIComponent(customerId)}`;
  }
}
