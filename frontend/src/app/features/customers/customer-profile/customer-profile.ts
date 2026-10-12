import { DatePipe } from '@angular/common';
import {
  afterNextRender,
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  ElementRef,
  inject,
  Injector,
  input,
  signal,
  untracked,
  viewChild,
} from '@angular/core';
import { Auth } from '../../../core/auth/auth';
import { ApiError } from '../../../core/http/api-error';
import { RecordInteractionForm } from '../../interactions/record-interaction-form/record-interaction-form';
import { CustomerApi } from '../customer-api';
import { Customer } from '../customer.model';
import { Interaction } from '../../interactions/interaction.model';

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

  /** The server already limits this by role: admins get everything, agents get their own entries. */
protected readonly interactions = signal<Interaction[]>([]);
protected readonly timelineError = signal<ApiError | null>(null);

  // ADDED (record popup): the form lives in a <dialog>; its result shows above the timeline.
  private readonly injector = inject(Injector);
  private readonly recordDialog = viewChild<ElementRef<HTMLDialogElement>>('recordDialog');
  private readonly recordResult = viewChild<ElementRef<HTMLElement>>('recordResult');
  protected readonly recordError = signal<ApiError | null>(null);
  protected readonly lastRecorded = signal<Interaction | null>(null);

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

  // ADDED (record popup): opens the form as a modal. A new attempt clears the last result.
  protected openRecordDialog(): void {
    this.recordError.set(null);
    this.lastRecorded.set(null);
    this.recordDialog()?.nativeElement.showModal();
  }

  // ADDED (record popup)
  protected closeRecordDialog(): void {
    this.recordDialog()?.nativeElement.close();
  }

  protected onRecorded(saved: Interaction): void {
  this.interactions.update((list) => [saved, ...list]);
    // ADDED (record popup): close the popup and confirm above the timeline.
    this.recordError.set(null);
    this.lastRecorded.set(saved);
    this.showRecordResult();
}

  // ADDED (record popup): the server rejected the entry. Close the popup and say why above the timeline.
  protected onRecordFailed(err: ApiError): void {
    this.lastRecorded.set(null);
    this.recordError.set(err);
    this.showRecordResult();
  }

  // ADDED (record popup): closes the popup, then moves focus to the message once it is on the page.
  // Focusing it scrolls it into view on a long timeline and makes screen readers read it.
  private showRecordResult(): void {
    this.closeRecordDialog();
    afterNextRender(() => this.recordResult()?.nativeElement.focus(), { injector: this.injector });
  }

private load(id: string): void {
    this.loading.set(true);
    this.error.set(null);
    // ADDED (record popup): a different customer starts with no leftover record result.
    this.recordError.set(null);
    this.lastRecorded.set(null);
    this.loadTimeline(id);

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

  private loadTimeline(id: string): void {
    this.timelineError.set(null);
    this.api.listInteractions(id).subscribe({
      next: (list) => this.interactions.set(list),
      error: (err: ApiError) => {
        this.interactions.set([]);
        this.timelineError.set(err);
      },
    });
  }
}