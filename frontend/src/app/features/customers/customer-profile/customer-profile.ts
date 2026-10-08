import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { Auth } from '../../../core/auth/auth';
import { InteractionStore } from '../../interactions/interaction-store';
import { NewInteraction } from '../../interactions/interaction.model';
import { RecordInteractionForm } from '../../interactions/record-interaction-form/record-interaction-form';
import { CustomerStore } from '../customer-store';

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
  private readonly customers = inject(CustomerStore);
  private readonly interactionStore = inject(InteractionStore);

  protected readonly customer = computed(() => this.customers.find(this.id()));
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

  /** Backlog: admins read every timeline; agents see only their own entries. */
  protected readonly interactions = computed(() => {
    const all = this.interactionStore.forCustomer(this.id());
    if (this.isAdmin()) {
      return all;
    }
    const me = this.auth.user()?.username;
    return all.filter((i) => i.actor === me);
  });

  protected activate(): void {
    this.customers.activate(this.id());
  }

  protected onRecorded(interaction: NewInteraction): void {
    const user = this.auth.user();
    if (user) {
      this.interactionStore.record(interaction, user.username);
    }
  }
}
