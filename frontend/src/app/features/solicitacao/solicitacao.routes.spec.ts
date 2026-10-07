import { describe, expect, it } from 'vitest';
import { Acoes } from '../../core/auth/acoes';
import { roleGuard } from '../../core/guards/role.guard';
import { SOLICITACAO_ROUTES } from './solicitacao.routes';

describe('SOLICITACAO_ROUTES', () => {
  it('deveExporQuatroRotasCanonicas_quandoConfigurarFeature', () => {
    expect(SOLICITACAO_ROUTES[0]).toMatchObject({
      path: '',
      pathMatch: 'full',
      redirectTo: 'list',
    });
    expect(SOLICITACAO_ROUTES.slice(1).map((route) => route.path)).toEqual([
      'list',
      'form',
      'form/:id',
      'view/:id',
    ]);
  });

  it('deveDeclararAcaoEModo_quandoRotaForGuardada', () => {
    expect(SOLICITACAO_ROUTES.slice(1).map((route) => route.canActivate)).toEqual([
      [roleGuard],
      [roleGuard],
      [roleGuard],
      [roleGuard],
    ]);
    expect(SOLICITACAO_ROUTES.slice(1).map((route) => route.data)).toEqual([
      { roles: [Acoes.CONSULTAR_SOLICITACAO] },
      { roles: [Acoes.INSERIR_SOLICITACAO], mode: 'create' },
      { roles: [Acoes.ALTERAR_SOLICITACAO], mode: 'edit' },
      { roles: [Acoes.CONSULTAR_SOLICITACAO], mode: 'view' },
    ]);
  });
});
