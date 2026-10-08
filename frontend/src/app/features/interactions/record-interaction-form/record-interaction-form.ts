import { ChangeDetectionStrategy, Component, inject, input, output, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { INTERACTION_CHANNELS, InteractionChannel, NewInteraction } from '../interaction.model';

@Component({
  selector: 'app-record-interaction-form',
  imports: [ReactiveFormsModule],
  templateUrl: './record-interaction-form.html',
  styleUrl: './record-interaction-form.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RecordInteractionForm {
  readonly customerId = input.required<string>();
  readonly recorded = output<NewInteraction>();

  protected readonly channels = INTERACTION_CHANNELS;
  protected readonly summaryMaxLength = 500;
  protected readonly lastRecorded = signal<NewInteraction | null>(null);

  private readonly fb = inject(NonNullableFormBuilder);
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
    const interaction: NewInteraction = {
      customerId: this.customerId(),
      channel: channel as InteractionChannel,
      summary: summary.trim(),
    };
    this.lastRecorded.set(interaction);
    this.recorded.emit(interaction);
    this.form.reset();
  }
}
