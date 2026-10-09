import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, OperatorFunction, catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { toApiError } from '../../core/http/api-error';
import { Interaction, NewInteraction } from '../interactions/interaction.model';
import { Customer, CustomerStatus } from './customer.model';
import { PublicCustomerPage } from './public-customer-api';

/** Turns any HTTP failure into an ApiError so screens get one predictable error shape. */
function asApiError<T>(): OperatorFunction<T, T> {
  return catchError((error: HttpErrorResponse) => throwError(() => toApiError(error)));
}

/** One page of customers. */
export interface CustomerPage {
  items: Customer[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

@Injectable({ providedIn: 'root' })
export class CustomerApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/v1/customers`;

  /** GET /api/v1/customers/{customerId} */
  getCustomer(customerId: string): Observable<Customer> {
    return this.http.get<Customer>(this.customerUrl(customerId)).pipe(asApiError());
  }

  /** GET /api/v1/customers?status=&page=&size=  */
  list(status?: CustomerStatus, page = 0, size = 20): Observable<CustomerPage> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) {
      params = params.set('status', status);
    }
    return this.http
      .get<CustomerPage>(this.baseUrl, { params })
      .pipe(catchError((error: HttpErrorResponse) => throwError(() => toApiError(error))));
  }

  /** POST /api/v1/customers/{customerId}/activate (no request body) */
  activateCustomer(customerId: string): Observable<Customer> {
    return this.http
      .post<Customer>(`${this.customerUrl(customerId)}/activate`, null)
      .pipe(asApiError());
  }

  /** POST /api/v1/customers/{customerId}/interaction with body { channel, summary } */
  recordInteraction(interaction: NewInteraction): Observable<Interaction> {
    const { customerId, channel, summary } = interaction;
    return this.http
      .post<Interaction>(`${this.customerUrl(customerId)}/interactions`, { channel, summary })
      .pipe(asApiError());
  }

/** GET /api/v1/customers/{customerId}/interactions (newest first, filtered by role on the server) */
listInteractions(customerId: string): Observable<Interaction[]> {
  return this.http
    .get<Interaction[]>(`${this.customerUrl(customerId)}/interactions`)
    .pipe(asApiError());
}

  private customerUrl(customerId: string): string {
    return `${this.baseUrl}/${encodeURIComponent(customerId)}`;
  }
}
