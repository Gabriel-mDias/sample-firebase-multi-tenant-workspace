import { Routes } from '@angular/router';
import { authGuard } from '../../core/guards/auth.guard';

/**
 * Rotas da feature home. Só exige sessão válida (`authGuard`) — a home é a tela
 * de boas-vindas exibida a qualquer usuário autenticado, sem checagem de role.
 */
export default [
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./home.component').then((c) => c.HomeComponent),
  },
] satisfies Routes;
