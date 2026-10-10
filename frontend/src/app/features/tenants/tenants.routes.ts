import { Routes } from '@angular/router';
export default [
  {
    path: '',
    loadComponent: () => import('./tenants.component').then((module) => module.TenantsComponent),
  },
] satisfies Routes;
