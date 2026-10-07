import { describe, expect, it } from 'vitest';
import { Acoes } from '../../core/auth/acoes';
import { roleGuard } from '../../core/guards/role.guard';
import { PARCEIRO_ROUTES } from './parceiro.routes';

describe('PARCEIRO_ROUTES', () => {
  it('deveExporQuatroRotasCanonicas_quandoConfigurarFeature', () => {
    expect(PARCEIRO_ROUTES[0]).toMatchObject({
      path: '',
      pathMatch: 'full',
      redirectTo: 'list',
    });
    expect(PARCEIRO_ROUTES.slice(1).map((route) => route.path)).toEqual([
      'list',
      'form',
      'form/:id',
      'view/:id',
    ]);
  });

  it('deveDeclararAcaoEModo_quandoRotaForGuardada', () => {
    expect(PARCEIRO_ROUTES.slice(1).map((route) => route.canActivate)).toEqual([
      [roleGuard],
      [roleGuard],
      [roleGuard],
      [roleGuard],
    ]);
    expect(PARCEIRO_ROUTES.slice(1).map((route) => route.data)).toEqual([
      { roles: [Acoes.CONSULTAR_PARCEIRO] },
      { roles: [Acoes.INSERIR_PARCEIRO], mode: 'create' },
      { roles: [Acoes.ALTERAR_PARCEIRO], mode: 'edit' },
      { roles: [Acoes.CONSULTAR_PARCEIRO], mode: 'view' },
    ]);
  });
});
