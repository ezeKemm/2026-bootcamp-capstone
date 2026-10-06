// Channel values are a frontend placeholder; backend accepts any String. Confirm the list with the team.
export type InteractionChannel = 'PHONE' | 'EMAIL' | 'MEETING';

export interface NewInteraction {
  customerId: string;
  channel: InteractionChannel;
  summary: string;
}
