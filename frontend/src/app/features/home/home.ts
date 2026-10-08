import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
type Status = 'Active' | 'Prospect';
type StatusFilter = 'All' | Status;

interface CustomerRow {
  customerId: string;
  name: string;
  status: Status;
}

@Component({
  selector: 'app-home',
  templateUrl: './home.html',
  styleUrl: './home.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Home {
  private readonly router = inject(Router);
  // The full list. Later this comes from the API instead of a hard-coded array.
  private readonly customers = signal<CustomerRow[]>([
    { customerId: 'CUS-1001', name: 'Amina Khan', status: 'Active' },
    { customerId: 'CUS-1002', name: 'Ravi Singh', status: 'Prospect' },
  ]);

    // UI state: the active filter, whether its menu is open, and the selected row.
  readonly filter = signal<StatusFilter>('All');
  readonly menuOpen = signal(false);
  readonly selectedName = signal<string | null>(null);
  readonly filterOptions: StatusFilter[] = ['All', 'Active', 'Prospect'];

   // Actions called by the template in response to user input.
    readonly visibleCustomers = computed(() => {
    const filter = this.filter();
    return this.customers().filter(
      (customer) => filter === 'All' || customer.status === filter,
    );
  });

  readonly countLabel = computed(
    () => `Showing ${this.visibleCustomers().length} of ${this.customers().length}`,
  );

  readonly filterLabel = computed(() =>
    this.filter() === 'All' ? 'Filter' : `Filter: ${this.filter()}`,
  );


  toggleMenu(): void {
    this.menuOpen.update((open) => !open);
  }

  chooseFilter(option: StatusFilter): void {
    this.filter.set(option);
    this.menuOpen.set(false);
  }

  select(customerId: string): void {
    void this.router.navigate(['/login'], {
      queryParams: { returnUrl: `/customers/${customerId}` },
    });
  }
}