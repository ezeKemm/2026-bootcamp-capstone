import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  signal,
  untracked,
} from '@angular/core';
import { Auth } from '../../../core/auth/auth';
import { ApiError } from '../../../core/http/api-error';
import { InteractionStore } from '../../interactions/interaction-store';
import { NewInteraction } from '../../interactions/interaction.model';
import { RecordInteractionForm } from '../../interactions/record-interaction-form/record-interaction-form';
import { CustomerApi } from '../customer-api';
import { Customer } from '../customer.model';

@Component({
  selector: 'app-customer-profile',
  imports: [DatePipe, RecordInteractionForm],
  templateUrl: './customer-profile.html',
  styleUrl: './customer-profile.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CustomerProfile {
  /** Filled in automatically from the URL (/customers/:id). */
  readonly id = input.required<string>();

  private readonly auth = inject(Auth);
  private readonly api = inject(CustomerApi);
  private readonly interactionStore = inject(InteractionStore);

  // The customer from the backend, plus loading / error state.
  protected readonly customer = signal<Customer | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<ApiError | null>(null);

  // Activate button state.
  protected readonly activating = signal(false);
  protected readonly activateError = signal<ApiError | null>(null);

  /** 404 = no such customer; 400 = the id isn't a valid UUID (e.g. an old CUS-1001 link). */
  protected readonly notFound = computed(() => {
    const status = this.error()?.status;
    return status === 404 || status === 400;
  });

  protected readonly isAdmin = computed(() => this.auth.user()?.role === 'ADMIN');
  protected readonly isAgent = computed(() => this.auth.user()?.role === 'AGENT');

  /** Backlog: agents and admins can move a PROSPECT to ACTIVE. */
  protected readonly canActivate = computed(
    () => this.customer()?.status === 'PROSPECT' && (this.isAgent() || this.isAdmin()),
  );

  /** Backlog: only agents record, and only for ACTIVE customers. */
  protected readonly canRecord = computed(
    () => this.isAgent() && this.customer()?.status === 'ACTIVE',
  );

  /** Backlog: admins read every timeline; agents see only their own entries. (Still local data for now.) */
  protected readonly interactions = computed(() => {
    const all = this.interactionStore.forCustomer(this.id());
    if (this.isAdmin()) {
      return all;
    }
    const me = this.auth.user()?.username;
    return all.filter((i) => i.actor === me);
  });

  constructor() {
    // Load the customer now, and again whenever the :id in the URL changes.
    effect(() => {
      const id = this.id();
      untracked(() => this.load(id));
    });
  }

  protected retry(): void {
    this.load(this.id());
  }

  protected activate(): void {
    this.activating.set(true);
    this.activateError.set(null);

    this.api.activateCustomer(this.id()).subscribe({
      next: (updated) => {
        this.customer.set(updated);
        this.activating.set(false);
      },
      error: (err: ApiError) => {
        this.activateError.set(err);
        this.activating.set(false);
      },
    });
  }

  protected onRecorded(interaction: NewInteraction): void {
    const user = this.auth.user();
    if (user) {
      this.interactionStore.record(interaction, user.username);
    }
  }

  private load(id: string): void {
    this.loading.set(true);
    this.error.set(null);

    this.api.getCustomer(id).subscribe({
      next: (customer) => {
        this.customer.set(customer);
        this.loading.set(false);
      },
      error: (err: ApiError) => {
        this.customer.set(null);
        this.error.set(err);
        this.loading.set(false);
      },
    });
  }
}
