import { Routes } from '@angular/router';

// TODO: interactions and login routes get added here later.
export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/home/home').then((m) => m.Home),
  },
  {
    path: 'customers/:id',
    loadComponent: () =>
      import('./features/customers/customer-profile/customer-profile').then(
        (m) => m.CustomerProfile,
      ),
  },
  {
    // Catch-all: must stay last.
    path: '**',
    loadComponent: () => import('./features/not-found/not-found').then((m) => m.NotFound),
  },
];
