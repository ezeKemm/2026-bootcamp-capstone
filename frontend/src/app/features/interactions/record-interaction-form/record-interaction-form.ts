import { ChangeDetectionStrategy, Component, inject, input, output, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { INTERACTION_CHANNELS, InteractionChannel, Interaction } from '../interaction.model';
import { ApiError } from '../../../core/http/api-error';
import { CustomerApi } from '../../customers/customer-api';

@Component({
  selector: 'app-record-interaction-form',
  imports: [ReactiveFormsModule],
  templateUrl: './record-interaction-form.html',
  styleUrl: './record-interaction-form.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RecordInteractionForm {
  readonly customerId = input.required<string>();
  readonly recorded = output<Interaction>();

  // ADDED (record popup): lets a host screen show the result itself.
  /** false = the host shows the server error / success message, so this form stays quiet. */
  readonly showResult = input(true);
  /** Emits the server's error when saving fails (the form keeps the typed text for a retry). */
  readonly failed = output<ApiError>();

  protected readonly channels = INTERACTION_CHANNELS;
  protected readonly summaryMaxLength = 500;
  protected readonly lastRecorded = signal<Interaction | null>(null);
  protected readonly saving = signal(false);
  protected readonly error = signal<ApiError | null>(null);

  private readonly fb = inject(NonNullableFormBuilder);
  private readonly api = inject(CustomerApi);
  protected readonly form = this.fb.group({
    channel: this.fb.control<InteractionChannel | ''>('', Validators.required),
    summary: [
      '',
      [Validators.required, Validators.pattern(/\S/), Validators.maxLength(this.summaryMaxLength)],
    ],
  });

  protected showError(name: 'channel' | 'summary'): boolean {
    const control = this.form.controls[name];
    return control.invalid && control.touched;
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { channel, summary } = this.form.getRawValue();
    this.saving.set(true);
this.error.set(null);

this.api
  .recordInteraction({
    customerId: this.customerId(),
    channel: channel as InteractionChannel,
    summary: summary.trim(),
  })
  .subscribe({
    next: (saved) => {
      this.lastRecorded.set(saved);
      this.recorded.emit(saved);
      this.form.reset();
      this.saving.set(false);
    },
    error: (err: ApiError) => {
      this.error.set(err);
      this.failed.emit(err); // ADDED (record popup)
      this.saving.set(false);
    },
  });
}
}