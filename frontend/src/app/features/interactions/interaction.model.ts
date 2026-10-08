/** Must match the backend InteractionChannel enum and openapi.yaml. */
export const INTERACTION_CHANNELS = ['PHONE', 'EMAIL', 'CHAT', 'BRANCH'] as const;
export type InteractionChannel = (typeof INTERACTION_CHANNELS)[number];
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
