import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Auth } from '../../core/auth/auth';
import { ApiError } from '../../core/http/api-error';
import { Customer, CustomerStatus } from '../customers/customer.model';
import { CustomerApi } from '../customers/customer-api';

type StatusFilter = 'All' | CustomerStatus;

/** API sends ACTIVE / PROSPECT; the screen shows Active / Prospect. */
const STATUS_LABELS: Record<CustomerStatus, string> = {
  ACTIVE: 'Active',
  PROSPECT: 'Prospect',
};

@Component({
  selector: 'app-browseCustomers',
  templateUrl: './browseCustomers.html',
  styleUrl: './browseCustomers.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BrowseCustomers {
  private readonly router = inject(Router);
  private readonly customerApi = inject(CustomerApi);
  protected readonly auth = inject(Auth);

  // Data from the server, plus loading / error state.
  readonly customers = signal<Customer[]>([]);
  readonly totalItems = signal(0);
  readonly loading = signal(true);
  readonly error = signal<ApiError | null>(null);

  // Filter menu state.
  readonly filter = signal<StatusFilter>('All');
  readonly menuOpen = signal(false);
  readonly filterOptions: StatusFilter[] = ['All', 'ACTIVE', 'PROSPECT'];

  readonly countLabel = computed(
    () => `Showing ${this.customers().length} of ${this.totalItems()}`,
  );

  readonly filterLabel = computed(() =>
    this.filter() === 'All' ? 'Filter' : `Filter: ${this.label(this.filter())}`,
  );

  constructor() {
    this.load();
  }

  /** Fetches the list from the server, using the current filter. */
  load(): void {
    const filter = this.filter();
    this.loading.set(true);
    this.error.set(null);

    this.customerApi.list(filter === 'All' ? undefined : filter).subscribe({
      next: (page) => {
        this.customers.set(page.items);
        this.totalItems.set(page.totalItems);
        this.loading.set(false);
      },
      error: (err: ApiError) => {
        this.error.set(err);
        this.loading.set(false);
      },
    });
  }

  toggleMenu(): void {
    this.menuOpen.update((open) => !open);
  }

  /** Filtering happens on the server, so picking a filter reloads the list. */
  chooseFilter(option: StatusFilter): void {
    this.filter.set(option);
    this.menuOpen.set(false);
    this.load();
  }

  /** A guest's only action: go to the login page. */
  // signIn(): void {
  //   void this.router.navigate(['/login']);
  // }

  openCustomer(customer: { customerId: string }): void {
    void this.router.navigate(['/customers', customer.customerId]);
  }

  label(option: StatusFilter): string {
    return option === 'All' ? 'All' : STATUS_LABELS[option];
  }
}
