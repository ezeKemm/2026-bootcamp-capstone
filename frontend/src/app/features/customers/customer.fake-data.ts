import { Customer } from './customer.model';

// Temporary until the API contract is agreed. Replaced by CustomerApiService in step 3.
export const FAKE_CUSTOMERS: Customer[] = [
  {
    customerId: 'CUS-1001',
    fullName: 'Amina Example',
    email: 'amina@example.com',
    status: 'ACTIVE',
  },
  {
    customerId: 'CUS-1002',
    fullName: 'Ravi Example',
    email: 'ravi@example.com',
    status: 'PROSPECT',
  },
];
