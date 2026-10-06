export type CustomerStatus = 'ACTIVE' | 'PROSPECT';

export interface Customer {
  customerId: string;
  fullName: string;
  email: string;
  status: CustomerStatus;
}
