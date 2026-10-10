import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  provideRouter,
  Router,
  RouterStateSnapshot,
} from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { AuthStore } from '../auth/auth.store';
import { Acao, Acoes } from '../auth/acoes';
import { IDENTITY_ADAPTER, IdentityUser } from '../auth/identity.adapter';
import { roleGuard } from './role.guard';
describe('roleGuard', () => {
  const auth = { sessionReady: () => true, isAuthorized: vi.fn(() => true) };
  beforeEach(() =>
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
    }),
  );
  it('naoDeveLiberarRota_quandoRolesAusentesOuVazias', () => {
    for (const data of [
      {},
      { roles: [] },
      { roles: [], authorizationScope: 'tenant' },
      { roles: ['CONSULTAR_DEMO'] },
      { roles: ['CONSULTAR_DEMO'], authorizationScope: 'tenant', roleMode: 'invalid' },
    ]) {
      const result = TestBed.runInInjectionContext(() =>
        roleGuard(
          { data } as ActivatedRouteSnapshot,
          { url: '/demo-registro/list' } as RouterStateSnapshot,
        ),
      );
      expect(
        TestBed.inject(Router).serializeUrl(result as ReturnType<Router['createUrlTree']>),
      ).toBe('/acesso-negado');
    }
  });
  it('deveEncaminharEscopoModo_quandoMetadataValida', () => {
    const data = { roles: ['CONSULTAR_DEMO'], authorizationScope: 'tenant', roleMode: 'all' };
    expect(
      TestBed.runInInjectionContext(() =>
        roleGuard(
          { data } as unknown as ActivatedRouteSnapshot,
          { url: '/demo-registro/list' } as RouterStateSnapshot,
        ),
      ),
    ).toBe(true);
    expect(auth.isAuthorized).toHaveBeenCalledWith(data.roles, 'tenant', 'all');
  });
});

describe('roleGuard / fachada real', () => {
  let auth: AuthService;
  let change: (user: IdentityUser | null) => void;
  let globalActions: Acao[] = [];
  let tenantActions: Acao[] = [];
  const user = { uid: 'uid-1', getIdToken: async () => 'token' };
  const tick = async () => {
    for (let i = 0; i < 8; i++) await Promise.resolve();
  };
  const store = {
    getMe: () =>
      of({
        user: { id: '1', name: 'Pessoa', email: 'pessoa@example.test' },
        tenants: [{ alias: 'alpha', name: 'Alpha' }],
        globalActions,
      }),
    getContext: () =>
      of({
        tenant: { alias: 'alpha', name: 'Alpha' },
        profiles: [],
        groups: [],
        actions: tenantActions,
      }),
  };
  async function enter(withTenant: boolean) {
    change(user);
    await tick();
    if (withTenant) await auth.selectTenant('alpha');
  }
  function outcome(data: Record<string, unknown>) {
    const result = TestBed.runInInjectionContext(() =>
      roleGuard(
        { data } as unknown as ActivatedRouteSnapshot,
        { url: '/demo-registro/list' } as RouterStateSnapshot,
      ),
    );
    return result === true
      ? 'liberado'
      : TestBed.inject(Router).serializeUrl(result as ReturnType<Router['createUrlTree']>);
  }
  beforeEach(() => {
    globalActions = [];
    tenantActions = [];
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthStore, useValue: store },
        {
          provide: IDENTITY_ADAPTER,
          useValue: {
            observe: (callback: (user: IdentityUser | null) => void) => {
              change = callback;
              return () => {};
            },
          },
        },
      ],
    });
    auth = TestBed.inject(AuthService);
  });
  it('deveEncaminharAoLoginComRetornoInterno_quandoUsuarioAnonimo', () => {
    expect(outcome({ roles: [Acoes.CONSULTAR_DEMO], authorizationScope: 'tenant' })).toBe(
      '/login?returnTo=%2Fdemo-registro%2Flist',
    );
  });
  it.each([
    ['global', Acoes.CONSULTAR_TENANT],
    ['tenant', Acoes.CONSULTAR_DEMO],
  ])('deveEncaminharAcessoNegado_quandoAutenticadoSemAcao%s', async (scope, action) => {
    globalActions = [Acoes.CONSULTAR_PROPRIA_CONTA];
    tenantActions = [Acoes.INSERIR_DEMO];
    await enter(true);
    expect(outcome({ roles: [action], authorizationScope: scope })).toBe('/acesso-negado');
  });
  it('deveLiberarRota_quandoAcaoGlobalAutorizadaSemTenant', async () => {
    globalActions = [Acoes.CONSULTAR_TENANT];
    await enter(false);
    expect(auth.currentTenant()).toBeNull();
    expect(outcome({ roles: [Acoes.CONSULTAR_TENANT], authorizationScope: 'global' })).toBe(
      'liberado',
    );
  });
  it('naoDeveLiberarRota_quandoAcaoDeTenantSemContexto', async () => {
    globalActions = [Acoes.CONSULTAR_TENANT];
    tenantActions = [Acoes.CONSULTAR_DEMO];
    await enter(false);
    expect(outcome({ roles: [Acoes.CONSULTAR_DEMO], authorizationScope: 'tenant' })).toBe(
      '/acesso-negado',
    );
  });
  it.each([
    ['tenant', [Acoes.CONSULTAR_TENANT]],
    ['global', [Acoes.CONSULTAR_DEMO]],
    ['global', [Acoes.CONSULTAR_TENANT, Acoes.CONSULTAR_DEMO]],
    ['tenant', [Acoes.CONSULTAR_DEMO, Acoes.CONSULTAR_TENANT]],
  ])('naoDeveSubstituirEscopo_quandoRotaExige%sComAcoes%j', async (scope, roles) => {
    globalActions = [Acoes.CONSULTAR_TENANT];
    tenantActions = [Acoes.CONSULTAR_DEMO];
    await enter(true);
    expect(outcome({ roles, authorizationScope: scope })).toBe('/acesso-negado');
    expect(outcome({ roles, authorizationScope: scope, roleMode: 'all' })).toBe('/acesso-negado');
  });
  it.each([
    ['tenant', [Acoes.CONSULTAR_DEMO, Acoes.EXCLUIR_DEMO], undefined, 'liberado'],
    ['tenant', [Acoes.CONSULTAR_DEMO, Acoes.EXCLUIR_DEMO], 'any', 'liberado'],
    ['tenant', [Acoes.CONSULTAR_DEMO, Acoes.EXCLUIR_DEMO], 'all', '/acesso-negado'],
    ['tenant', [Acoes.CONSULTAR_DEMO, Acoes.INSERIR_DEMO], 'all', 'liberado'],
    ['tenant', [Acoes.EXCLUIR_DEMO, Acoes.ALTERAR_DEMO], 'any', '/acesso-negado'],
    ['global', [Acoes.CONSULTAR_TENANT, Acoes.INSERIR_TENANT], undefined, 'liberado'],
    ['global', [Acoes.CONSULTAR_TENANT, Acoes.INSERIR_TENANT], 'all', '/acesso-negado'],
    ['global', [Acoes.CONSULTAR_TENANT, Acoes.ALTERAR_TENANT], 'all', 'liberado'],
    ['global', [Acoes.INSERIR_TENANT, Acoes.SUSPENDER_TENANT], 'any', '/acesso-negado'],
  ])(
    'deveAplicarModoDeAcoes_quandoEscopo%sComAcoes%jEModo%s',
    async (scope, roles, roleMode, expected) => {
      globalActions = [Acoes.CONSULTAR_TENANT, Acoes.ALTERAR_TENANT];
      tenantActions = [Acoes.CONSULTAR_DEMO, Acoes.INSERIR_DEMO];
      await enter(true);
      expect(outcome({ roles, authorizationScope: scope, roleMode })).toBe(expected);
    },
  );
});
