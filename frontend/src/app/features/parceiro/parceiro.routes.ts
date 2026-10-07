import { Routes } from '@angular/router';
import { roleGuard } from '../../core/guards/role.guard';
import { Acoes } from '../../core/auth/acoes';

/**
 * Rotas da feature de parceiros, registradas no `app.routes.ts` raiz por `loadChildren`.
 *
 * Os três modos são atendidos pelo mesmo orquestrador — o modo chega por `data.mode`.
 */
export const PARCEIRO_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'list',
  },
  {
    path: 'list',
    canActivate: [roleGuard],
    data: { roles: [Acoes.CONSULTAR_PARCEIRO] },
    loadComponent: () =>
      import('./list/parceiro-list.component').then((c) => c.ParceiroListComponent),
  },
  {
    path: 'form',
    canActivate: [roleGuard],
    data: { roles: [Acoes.INSERIR_PARCEIRO], mode: 'create' },
    loadComponent: () =>
      import('./form/parceiro-form.component').then((c) => c.ParceiroFormComponent),
  },
  {
    path: 'form/:id',
    canActivate: [roleGuard],
    data: { roles: [Acoes.ALTERAR_PARCEIRO], mode: 'edit' },
    loadComponent: () =>
      import('./form/parceiro-form.component').then((c) => c.ParceiroFormComponent),
  },
  {
    path: 'view/:id',
    canActivate: [roleGuard],
    data: { roles: [Acoes.CONSULTAR_PARCEIRO], mode: 'view' },
    loadComponent: () =>
      import('./form/parceiro-form.component').then((c) => c.ParceiroFormComponent),
  },
];
