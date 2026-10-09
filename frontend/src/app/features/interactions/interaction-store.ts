import { Injectable, signal } from '@angular/core';
import { Interaction, NewInteraction } from './interaction.model';

// TEMPORARY in-memory data until the backend interactions endpoints exist. Resets on refresh.
const SEED: Interaction[] = [
  {
    interactionId: 'INT-1',
    customerId: 'CUS-1001',
    channel: 'EMAIL',
    summary: 'Sent welcome pack.',
    actor: 'agent1',
    occurredAt: '2026-10-01T14:00:00Z',
    correlationId: 'lab-request-001',
  },
  {
    interactionId: 'INT-2',
    customerId: 'CUS-1001',
    channel: 'PHONE',
    summary: 'Called about renewal options.',
    actor: 'other.agent',
    occurredAt: '2026-10-03T15:30:00Z',
    correlationId: 'lab-request-001',
  },
];

@Injectable({ providedIn: 'root' })
export class InteractionStore {
  private readonly all = signal<Interaction[]>(SEED);
  private nextId = SEED.length + 1;

  /** A customer's interactions, newest first. */
  forCustomer(customerId: string): Interaction[] {
    return this.all()
      .filter((i) => i.customerId === customerId)
      .sort((a, b) => b.occurredAt.localeCompare(a.occurredAt));
  }

  /** Saves an interaction stamped with who recorded it (the server will do this later). */
  record(interaction: NewInteraction, actor: string): Interaction {
    const saved: Interaction = {
      ...interaction,
      interactionId: `INT-${this.nextId++}`,
      actor,
      occurredAt: new Date().toISOString(),
      correlationId: 'lab-request-001',
    };
    this.all.update((list) => [...list, saved]);
    return saved;
  }
}
