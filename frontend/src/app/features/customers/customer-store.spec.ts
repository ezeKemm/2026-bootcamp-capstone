import { TestBed } from '@angular/core/testing';
import { CustomerStore } from './customer-store';

describe('CustomerStore', () => {
  it('finds a customer and returns null for an unknown id', () => {
    const store = TestBed.inject(CustomerStore);
    expect(store.find('CUS-1001')?.status).toBe('ACTIVE');
    expect(store.find('CUS-9999')).toBeNull();
  });

  it('activates a prospect', () => {
    const store = TestBed.inject(CustomerStore);
    store.activate('CUS-1002');
    expect(store.find('CUS-1002')?.status).toBe('ACTIVE');
  });
});
