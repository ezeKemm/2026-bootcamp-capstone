// Channel values are a frontend placeholder; backend accepts any String. Confirm the list with the team.
export type InteractionChannel = 'PHONE' | 'EMAIL' | 'MEETING';

/** What the frontend sends (customerId goes in the URL, the rest in the body). */
export interface NewInteraction {
  customerId: string;
  channel: InteractionChannel;
  summary: string;
}

/** What the backend returns after recording (see openapi.yaml). */
export interface Interaction {
  interactionId: string;
  customerId: string;
  channel: InteractionChannel;
  summary: string;
  actor: string;
  occurredAt: string;
  correlationId: string;
}
