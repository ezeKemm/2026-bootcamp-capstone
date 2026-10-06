import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { FAKE_CUSTOMERS } from '../customer.fake-data';
import { RecordInteractionForm } from '../../interactions/record-interaction-form/record-interaction-form';

@Component({
  selector: 'app-customer-profile',
  imports: [RecordInteractionForm],
  templateUrl: './customer-profile.html',
  styleUrl: './customer-profile.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CustomerProfile {
  /** Filled in automatically from the URL (/customers/:id). */
  readonly id = input.required<string>();

  protected readonly customer = computed(
    () => FAKE_CUSTOMERS.find((c) => c.customerId === this.id()) ?? null,
  );
}
