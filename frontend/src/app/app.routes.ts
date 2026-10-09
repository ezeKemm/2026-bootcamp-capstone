import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/home/home').then((m) => m.Home),
  },
  {
    path: 'login',
    loadComponent: () => import('./features/login/login').then((m) => m.Login),
  },
  {
    path: 'customers',
    canActivate: [authGuard],
    loadComponent: () => import('./features/browseCustomers/browseCustomers').then(
      (m)=> m.BrowseCustomers
    ),
  },
  {
    path: 'customers/:id',
    canActivate: [authGuard],
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
