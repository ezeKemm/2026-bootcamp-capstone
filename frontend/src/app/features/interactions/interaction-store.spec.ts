import { TestBed } from '@angular/core/testing';
import { InteractionStore } from './interaction-store';

describe('InteractionStore', () => {
  it("lists a customer's interactions newest first", () => {
    const list = TestBed.inject(InteractionStore).forCustomer('CUS-1001');
    expect(list.map((i) => i.interactionId)).toEqual(['INT-2', 'INT-1']);
  });

  it('records an interaction stamped with the actor', () => {
    const store = TestBed.inject(InteractionStore);
    const saved = store.record(
      { customerId: 'CUS-1002', channel: 'EMAIL', summary: 'Intro email.' },
      'agent',
    );
    expect(saved).toMatchObject({
      customerId: 'CUS-1002',
      actor: 'agent',
      summary: 'Intro email.',
    });
    expect(store.forCustomer('CUS-1002')).toEqual([saved]);
  });
});
