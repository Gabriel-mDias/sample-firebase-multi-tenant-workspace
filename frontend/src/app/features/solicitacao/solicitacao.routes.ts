import { Routes } from '@angular/router';
import { Acoes } from '../../core/auth/acoes';
import { roleGuard } from '../../core/guards/role.guard';

/**
 * Rotas da feature de solicitações, registradas no `app.routes.ts` raiz por `loadChildren`.
 *
 * Toda rota guardada declara `data.roles`. O `routes-coverage.spec.ts` percorre esta lista e falha
 * se alguma esquecer — e o `roleGuard` nega por padrão, então o esquecimento fecharia a tela em vez
 * de abri-la.
 */
export const SOLICITACAO_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'list',
  },
  {
    path: 'list',
    canActivate: [roleGuard],
    data: { roles: [Acoes.CONSULTAR_SOLICITACAO] },
    loadComponent: () =>
      import('./list/solicitacao-list.component').then((c) => c.SolicitacaoListComponent),
  },
  {
    path: 'form',
    canActivate: [roleGuard],
    data: { roles: [Acoes.INSERIR_SOLICITACAO], mode: 'create' },
    loadComponent: () =>
      import('./form/solicitacao-form.component').then((c) => c.SolicitacaoFormComponent),
  },
  {
    path: 'form/:id',
    canActivate: [roleGuard],
    data: { roles: [Acoes.ALTERAR_SOLICITACAO], mode: 'edit' },
    loadComponent: () =>
      import('./form/solicitacao-form.component').then((c) => c.SolicitacaoFormComponent),
  },
  {
    path: 'view/:id',
    canActivate: [roleGuard],
    data: { roles: [Acoes.CONSULTAR_SOLICITACAO], mode: 'view' },
    loadComponent: () =>
      import('./form/solicitacao-form.component').then((c) => c.SolicitacaoFormComponent),
  },
];
