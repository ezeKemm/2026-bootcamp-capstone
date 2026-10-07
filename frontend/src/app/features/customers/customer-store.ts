import { Injectable, signal } from '@angular/core';
import { FAKE_CUSTOMERS } from './customer.fake-data';
import { Customer } from './customer.model';

// TEMPORARY in-memory data until the screens use CustomerApi. Resets on refresh.
@Injectable({ providedIn: 'root' })
export class CustomerStore {
  private readonly customers = signal<Customer[]>(FAKE_CUSTOMERS.map((c) => ({ ...c })));

  find(customerId: string): Customer | null {
    return this.customers().find((c) => c.customerId === customerId) ?? null;
  }

  activate(customerId: string): void {
    this.customers.update((list) =>
      list.map((c) => (c.customerId === customerId ? { ...c, status: 'ACTIVE' } : c)),
    );
  }
}
