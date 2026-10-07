import { Routes } from '@angular/router';

/**
 * Rotas de topo (a casca/side-menu vive no AppComponent, por fora).
 *
 * - `''` : home vazia de boas-vindas (`HOME_ROUTES`), qualquer usuário autenticado.
 * - `parceiros` : módulo com `list`, `form`, `form/:id` e `view/:id`.
 * - `solicitacoes` : módulo com as mesmas quatro rotas canônicas.
 * - `acesso-negado` : sem guard (destino da negação de ação; casca oculta nesta rota).
 * - `**` : qualquer rota desconhecida volta para a home.
 *
 * Cada domínio de negócio entra aqui como um único `loadChildren` — a raiz não conhece os
 * componentes internos da feature. O `routes-coverage.spec.ts` percorre esta lista, resolve cada
 * `loadChildren` e falha se uma rota guardada não declarar `data.roles`.
 */
export const routes: Routes = [
  {
    path: '',
    loadChildren: () => import('./features/home/home.routes').then((m) => m.HOME_ROUTES),
  },
  {
    path: 'parceiros',
    loadChildren: () =>
      import('./features/parceiro/parceiro.routes').then((m) => m.PARCEIRO_ROUTES),
  },
  {
    path: 'solicitacoes',
    loadChildren: () =>
      import('./features/solicitacao/solicitacao.routes').then((m) => m.SOLICITACAO_ROUTES),
  },
  {
    path: 'acesso-negado',
    loadComponent: () =>
      import('./features/acesso-negado/acesso-negado.component').then(
        (c) => c.AcessoNegadoComponent,
      ),
  },
  { path: '**', redirectTo: '' },
];
