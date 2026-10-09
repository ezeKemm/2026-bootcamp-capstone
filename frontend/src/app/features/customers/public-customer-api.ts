import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { toApiError } from '../../core/http/api-error';
import { CustomerStatus } from './customer.model';

/** What a guest is allowed to see: name and status only (no id, no email). */
export interface PublicCustomer {
  fullName: string;
  status: CustomerStatus;
}

/** One page of public customers (matches PublicCustomerPage in openapi.yaml). */
export interface PublicCustomerPage {
  items: PublicCustomer[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

@Injectable({ providedIn: 'root' })
export class PublicCustomerApi {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiBaseUrl}/api/v1/public/customers`;

  /** GET /api/v1/public/customers?page=&size=&status= (no login needed) */
  list(status?: CustomerStatus, page = 0, size = 20): Observable<PublicCustomerPage> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) {
      params = params.set('status', status);
    }
    return this.http
      .get<PublicCustomerPage>(this.url, { params })
      .pipe(catchError((error: HttpErrorResponse) => throwError(() => toApiError(error))));
  }
}
