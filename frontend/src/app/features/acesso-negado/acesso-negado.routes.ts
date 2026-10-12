import { Routes } from '@angular/router';
export default [
  {
    path: '',
    loadComponent: () =>
      import('./acesso-negado.component').then((module) => module.AcessoNegadoComponent),
  },
] satisfies Routes;
