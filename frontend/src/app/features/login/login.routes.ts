import { Routes } from '@angular/router';
export default [
  {
    path: '',
    loadComponent: () => import('./login.component').then((module) => module.LoginComponent),
  },
] satisfies Routes;
