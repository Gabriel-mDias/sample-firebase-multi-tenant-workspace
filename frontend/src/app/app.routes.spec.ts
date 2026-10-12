import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of, Subject } from 'rxjs';
import { GemsAlertService, GemsNavigationService } from '@gabriel-mdias/angular-gems-sdk';
import { routes } from './app.routes';
import { AuthService } from './core/auth/auth.service';
import { AuthStore } from './core/auth/auth.store';
import { AuthContext, AuthMe } from './core/auth/auth.model';
import { Acoes } from './core/auth/acoes';
import { IDENTITY_ADAPTER, IdentityAdapter, IdentityUser } from './core/auth/identity.adapter';
import { LoginComponent } from './features/login/login.component';
import { DemoRegistroStore } from './features/demo-registro/services/demo-registro.store';

describe('G14 / navegação pela configuração real', () => {
  let auth: AuthService;
  let router: Router;
  let harness: RouterTestingHarness;
  let observe: (user: IdentityUser | null) => void;
  let accountResponse: Subject<AuthMe>;
  let contextResponse: Subject<AuthContext>;
  let navigation: { navigate: ReturnType<typeof vi.fn> };
  let business: { search: ReturnType<typeof vi.fn>; getById: ReturnType<typeof vi.fn> };
  const user = { uid: 'restored', getIdToken: async () => 'token' };
  const tenant = { alias: 'alpha', name: 'Alpha' };
  const account: AuthMe = {
    user: { id: '1', name: 'Pessoa', email: 'pessoa@example.test' },
    tenants: [tenant],
    globalActions: [],
  };
  const context: AuthContext = {
    tenant,
    profiles: [],
    groups: [],
    actions: [Acoes.CONSULTAR_DEMO, Acoes.INSERIR_DEMO, Acoes.ALTERAR_DEMO],
  };
  const id = '123e4567-e89b-12d3-a456-426614174000';
  const tick = async () => {
    for (let i = 0; i < 8; i++) await Promise.resolve();
  };
  const settleNavigation = async () => {
    await tick();
    harness.detectChanges();
    const result = navigation.navigate.mock.results.at(-1);
    if (result) await result.value;
    harness.detectChanges();
  };
  const restore = async () => {
    observe(user);
    accountResponse.next(account);
    await tick();
  };
  const select = async (actions = context.actions) => {
    const selecting = auth.selectTenant('alpha');
    contextResponse.next({ ...context, actions });
    await selecting;
  };
  beforeEach(async () => {
    accountResponse = new Subject<AuthMe>();
    contextResponse = new Subject<AuthContext>();
    navigation = { navigate: vi.fn((commands: string[]) => router.navigate(commands)) };
    business = {
      search: vi.fn(() => of({ content: [], totalElements: 0, number: 0 })),
      getById: vi.fn(() => of({ id, nome: 'Registro', descricao: '' })),
    };
    const adapter: IdentityAdapter = {
      observe: (callback) => {
        observe = callback;
        return () => {};
      },
      signInWithEmailAndPassword: async () => user,
      signInWithGoogle: async () => user,
      resetPassword: async () => {},
      signOut: async () => {},
    };
    TestBed.configureTestingModule({
      providers: [
        provideRouter(routes),
        { provide: IDENTITY_ADAPTER, useValue: adapter },
        {
          provide: AuthStore,
          useValue: {
            getMe: () => accountResponse,
            getContext: () => contextResponse,
          },
        },
        { provide: DemoRegistroStore, useValue: business },
        { provide: GemsNavigationService, useValue: navigation },
        { provide: GemsAlertService, useValue: { errorFromApi: vi.fn() } },
      ],
    });
    auth = TestBed.inject(AuthService);
    router = TestBed.inject(Router);
    harness = await RouterTestingHarness.create();
  });
  afterEach(() => TestBed.resetTestingModule());

  it('deveAbrirLoginPublicoSemLoop_quandoAnonimoSolicitaNegocio', async () => {
    await harness.navigateByUrl('/demo-registro/list', LoginComponent);
    expect(router.url).toBe('/login?returnTo=%2Fdemo-registro%2Flist');
    expect(harness.routeNativeElement?.querySelector('form')).toBeTruthy();
    await harness.navigateByUrl('/login', LoginComponent);
    expect(router.url).toBe('/login');
    expect(navigation.navigate).not.toHaveBeenCalled();
    expect(business.search).not.toHaveBeenCalled();
  });

  it.each([
    '//evil.example',
    'https://evil.example',
    '/login?returnUrl=/login',
    'javascript:',
    '\\\\evil.example',
  ])('deveDescartarRetornoNaIntegracao_quandoDestino_%s', async (returnTo) => {
    await harness.navigateByUrl('/login?returnTo=' + encodeURIComponent(returnTo), LoginComponent);
    observe(user);
    accountResponse.next({ ...account, tenants: [] });
    await settleNavigation();
    expect(navigation.navigate).toHaveBeenCalledExactlyOnceWith(['/']);
    expect(router.url).toBe('/');
    expect(harness.routeNativeElement?.querySelector('form')).toBeNull();
    expect(business.search).not.toHaveBeenCalled();
  });

  it('deveEsperarMeEEncaminharAoSeletor_quandoSessaoRestauradaSemContexto', async () => {
    await harness.navigateByUrl('/login?returnTo=%2Fdemo-registro%2Flist', LoginComponent);
    observe(user);
    harness.detectChanges();
    expect(auth.sessionReady()).toBe(false);
    expect(harness.routeNativeElement?.querySelector('form')).toBeTruthy();
    expect(navigation.navigate).not.toHaveBeenCalled();
    accountResponse.next(account);
    await settleNavigation();
    expect(router.url).toBe('/tenants');
    expect(navigation.navigate).toHaveBeenCalledExactlyOnceWith(['/tenants']);
    expect(auth.currentTenant()).toBeNull();
    expect(auth.isAuthorized([Acoes.CONSULTAR_DEMO], 'tenant')).toBe(false);
    expect(business.search).not.toHaveBeenCalled();
    expect(harness.routeNativeElement?.querySelector('form')).toBeNull();
    const option = harness.routeNativeElement?.querySelector<HTMLButtonElement>('.tenant-option');
    expect(option?.textContent).toContain('Alpha');
    option!.click();
    harness.detectChanges();
    expect(option!.disabled).toBe(true);
    expect(router.url).toBe('/tenants');
    expect(auth.currentTenant()).toBeNull();
    expect(business.search).not.toHaveBeenCalled();
    expect(navigation.navigate).toHaveBeenCalledTimes(1);
    contextResponse.next(context);
    await settleNavigation();
    expect(navigation.navigate).toHaveBeenNthCalledWith(2, ['/demo-registro/list']);
    expect(router.url).toBe('/demo-registro/list');
    expect(auth.currentTenant()).toEqual(tenant);
    expect(auth.consumeReturnPath()).toBe('/');
    expect(business.search).toHaveBeenCalledOnce();
  });

  it('devePreservarTenantERetornoAutorizado_quandoContextoJaValidado', async () => {
    await restore();
    await select();
    const snapshot = auth.capture();
    await harness.navigateByUrl('/login?returnTo=%2Fdemo-registro%2Flist');
    await settleNavigation();
    expect(router.url).toBe('/demo-registro/list');
    expect(navigation.navigate).toHaveBeenCalledExactlyOnceWith(['/demo-registro/list']);
    expect(auth.currentTenant()).toEqual(tenant);
    expect(auth.isCurrent(snapshot)).toBe(true);
    expect(business.search).toHaveBeenCalledOnce();
    expect(harness.routeNativeElement?.querySelector('#login-email')).toBeNull();
  });

  it('deveNegarRetornoInterno_quandoTenantValidadoNaoTemAcaoDaRota', async () => {
    await restore();
    await select([Acoes.CONSULTAR_DEMO]);
    const snapshot = auth.capture();
    await harness.navigateByUrl('/login?returnTo=%2Fdemo-registro%2Fform');
    await settleNavigation();
    expect(navigation.navigate).toHaveBeenCalledExactlyOnceWith(['/demo-registro/form']);
    expect(router.url).toBe('/acesso-negado');
    expect(auth.currentTenant()).toEqual(tenant);
    expect(auth.isCurrent(snapshot)).toBe(true);
    expect(business.search).not.toHaveBeenCalled();
    expect(business.getById).not.toHaveBeenCalled();
    expect(harness.routeNativeElement?.querySelector('form')).toBeNull();
  });

  it('deveEsperarContexto_quandoLoginAbertoDuranteSelecaoPendente', async () => {
    await restore();
    const selecting = auth.selectTenant('alpha');
    await harness.navigateByUrl('/login?returnTo=%2Fdemo-registro%2Flist', LoginComponent);
    expect(harness.routeNativeElement?.querySelector('form')).toBeTruthy();
    expect(navigation.navigate).not.toHaveBeenCalled();
    expect(business.search).not.toHaveBeenCalled();
    contextResponse.next(context);
    await selecting;
    await settleNavigation();
    expect(router.url).toBe('/demo-registro/list');
    expect(auth.currentTenant()).toEqual(tenant);
    expect(business.search).toHaveBeenCalledOnce();
  });

  it('deveManterNegocioBloqueado_quandoContaInelegivel', async () => {
    await harness.navigateByUrl('/login', LoginComponent);
    observe(user);
    accountResponse.error(new Error('403: conta e vínculos privados'));
    await settleNavigation();
    expect(navigation.navigate).not.toHaveBeenCalled();
    expect(auth.sessionReady()).toBe(false);
    expect(harness.routeNativeElement?.querySelector('form')).toBeTruthy();
    expect(harness.routeNativeElement?.textContent).not.toContain('vínculos privados');
    await harness.navigateByUrl('/demo-registro/list', LoginComponent);
    expect(router.url).toBe('/login?returnTo=%2Fdemo-registro%2Flist');
    expect(business.search).not.toHaveBeenCalled();
  });

  it('deveManterNegocioBloqueado_quandoContextoFalha', async () => {
    await restore();
    const selecting = auth.selectTenant('alpha');
    const rejected = expect(selecting).rejects.toThrow('tenant-unavailable');
    contextResponse.error(new Error('503: contexto remoto privado'));
    await rejected;
    await harness.navigateByUrl('/demo-registro/list');
    expect(router.url).toBe('/acesso-negado');
    expect(auth.currentTenant()).toBeNull();
    expect(auth.effectiveActions()).toEqual([]);
    expect(business.search).not.toHaveBeenCalled();
    expect(business.getById).not.toHaveBeenCalled();
  });

  for (const [path, required] of [
    ['list', Acoes.CONSULTAR_DEMO],
    ['form', Acoes.INSERIR_DEMO],
    [`form/${id}`, Acoes.ALTERAR_DEMO],
    [`view/${id}`, Acoes.CONSULTAR_DEMO],
  ] as const) {
    it.each([false, true])(
      `deveRespeitarAcaoCanonica_quandoRota_${path}_autorizada_%s`,
      async (allowed) => {
        await restore();
        const actions = allowed
          ? [Acoes.CONSULTAR_DEMO, required]
          : required === Acoes.CONSULTAR_DEMO
            ? [Acoes.INSERIR_DEMO]
            : [Acoes.CONSULTAR_DEMO];
        await select(actions);
        await harness.navigateByUrl('/demo-registro/' + path);
        expect(router.url).toBe(allowed ? '/demo-registro/' + path : '/acesso-negado');
        if (!allowed) {
          expect(business.search).not.toHaveBeenCalled();
          expect(business.getById).not.toHaveBeenCalled();
        } else if (path === 'list') expect(business.search).toHaveBeenCalledOnce();
        else if (path.includes('/')) expect(business.getById).toHaveBeenCalledExactlyOnceWith(id);
      },
    );
  }
});
