import { Routes } from '@angular/router';

// TODO: (customers, interactions, login) routes get added here later.
//added dummy customer route for now to test the lazy loading of the customer profile component
export const routes: Routes = [
  {
    path: 'customers/:id',
    loadComponent: () =>
      import('./features/customers/customer-profile/customer-profile').then(
        (m) => m.CustomerProfile,
      ),
  },
];
