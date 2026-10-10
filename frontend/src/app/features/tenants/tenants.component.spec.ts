import { TestBed, ComponentFixture } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { GemsNavigationService, provideGemsHttp } from '@gabriel-mdias/angular-gems-sdk';
import { TenantsComponent } from './tenants.component';
import { AuthService } from '../../core/auth/auth.service';
import { IDENTITY_ADAPTER, IdentityUser } from '../../core/auth/identity.adapter';
import { firebaseInterceptor, gemsApiRoot } from '../../core/auth/firebase.interceptor';
import { Acoes } from '../../core/auth/acoes';
import { environment } from '../../../environments/environment';

describe('TenantsComponent / retorno autorizado', () => {
  it('deveRetomarPathInterno_quandoContextoDestinoValidado', async () => {
    const navigation = { navigate: vi.fn() };
    const path = '/demo-registro/view/123e4567-e89b-12d3-a456-426614174000';
    const auth = {
      currentTenant: () => ({ alias: 'beta' }),
      loading: () => false,
      tenants: () => [],
      error: () => '',
      selectTenant: vi.fn(async () => {}),
      capture: () => ({}),
      isCurrent: () => true,
      isAuthorized: () => true,
      consumeReturnPath: () => path,
    };
    TestBed.configureTestingModule({
      imports: [TenantsComponent],
      providers: [
        { provide: AuthService, useValue: auth },
        { provide: GemsNavigationService, useValue: navigation },
      ],
    });
    const fixture = TestBed.createComponent(TenantsComponent);
    await (fixture.componentInstance as unknown as { select(alias: string): Promise<void> }).select(
      'beta',
    );
    expect(navigation.navigate).toHaveBeenCalledWith([path]);
  });
});

describe('TenantsComponent / seleção com a fachada real', () => {
  let auth: AuthService;
  let http: HttpTestingController;
  let client: HttpClient;
  let fixture: ComponentFixture<TenantsComponent>;
  let change: (user: IdentityUser | null) => void;
  let loginAs: IdentityUser;
  let navigation: { navigate: ReturnType<typeof vi.fn> };
  let tenants: { alias: string; name: string }[];
  const first = { uid: 'uid-1', getIdToken: async () => 'token-one' };
  const second = { uid: 'uid-2', getIdToken: async () => 'token-two' };
  const apiUrl = (path: string) => environment.apiBaseUrl + path;
  const account = () => ({
    user: { id: '1', name: 'Pessoa', email: 'pessoa@example.test' },
    tenants,
    globalActions: [],
  });
  const context = (alias: string) => ({
    tenant: { alias, name: alias },
    profiles: [],
    groups: [],
    actions: [Acoes.CONSULTAR_DEMO],
  });
  const settle = () => new Promise<void>((resolve) => setTimeout(resolve));
  const buttons = () =>
    Array.from<HTMLButtonElement>(fixture.nativeElement.querySelectorAll('button.tenant-option'));
  const choose = async (name: string) => {
    fixture.detectChanges();
    buttons()
      .find((button) => button.textContent?.trim() === name)
      ?.click();
    await settle();
    fixture.detectChanges();
  };
  const alert = () => fixture.nativeElement.querySelector('[role="alert"]').textContent.trim();
  const status = () => fixture.nativeElement.querySelector('[role="status"]').textContent.trim();
  async function enter(user: IdentityUser = first) {
    change(user);
    await settle();
    http.expectOne(apiUrl('/auth/me')).flush(account());
    await settle();
    fixture.detectChanges();
  }
  beforeEach(() => {
    tenants = [
      { alias: 'alpha', name: 'Alpha' },
      { alias: 'beta', name: 'Beta' },
    ];
    loginAs = first;
    navigation = { navigate: vi.fn() };
    TestBed.configureTestingModule({
      imports: [TenantsComponent],
      providers: [
        provideHttpClient(withInterceptors([firebaseInterceptor])),
        provideHttpClientTesting(),
        provideGemsHttp(gemsApiRoot()),
        {
          provide: IDENTITY_ADAPTER,
          useValue: {
            observe: (callback: (user: IdentityUser | null) => void) => {
              change = callback;
              return () => {};
            },
            signInWithGoogle: async () => loginAs,
            signOut: async () => {},
          },
        },
        { provide: GemsNavigationService, useValue: navigation },
      ],
    });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
    client = TestBed.inject(HttpClient);
    fixture = TestBed.createComponent(TenantsComponent);
  });
  afterEach(() => http.verify());

  it('deveListarSomenteEmpresasDeMe_quandoContaTemVinculos', async () => {
    await enter();
    expect(buttons().map((button) => button.textContent?.trim())).toEqual(['Alpha', 'Beta']);
    expect(buttons().every((button) => !button.disabled)).toBe(true);
  });
  it('naoDeveOferecerEmpresaAlheiaAMe_quandoContaTemUmVinculo', async () => {
    tenants = [{ alias: 'beta', name: 'Beta' }];
    await enter();
    expect(buttons().map((button) => button.textContent?.trim())).toEqual(['Beta']);
  });
  it('deveInformarSemEmpresas_quandoMeNaoTemVinculos', async () => {
    tenants = [];
    await enter();
    expect(buttons()).toEqual([]);
    expect(fixture.nativeElement.textContent).toContain('Nenhuma empresa disponível');
  });
  it('deveDesabilitarSeletorEAnunciarStatus_quandoContextoPendente', async () => {
    await enter();
    await choose('Alpha');
    expect(buttons().every((button) => button.disabled)).toBe(true);
    expect(status()).toBe('Validando acesso à empresa…');
    http.expectOne(apiUrl('/auth/context')).flush(context('alpha'));
    await settle();
    fixture.detectChanges();
    expect(buttons().every((button) => !button.disabled)).toBe(true);
    expect(status()).toBe('');
  });
  it('naoDeveEnviarChamadaEmpresarial_quandoContextoPendenteEDeveLiberarAposSucesso', async () => {
    await enter();
    await choose('Alpha');
    let failure: Error | undefined;
    client
      .get(apiUrl('/demo-registros/1'))
      .subscribe({ error: (error: Error) => (failure = error) });
    await settle();
    expect(failure?.message).toBe('context-required');
    http.expectNone(apiUrl('/demo-registros/1'));
    expect(navigation.navigate).not.toHaveBeenCalled();
    http.expectOne(apiUrl('/auth/context')).flush(context('alpha'));
    await settle();
    client.get(apiUrl('/demo-registros/1')).subscribe();
    await settle();
    const released = http.expectOne(apiUrl('/demo-registros/1'));
    expect(released.request.headers.get('X-Tenant-Alias')).toBe('alpha');
    released.flush({});
    expect(navigation.navigate).toHaveBeenCalledWith(['/demo-registro/list']);
  });
  it('deveNavegarParaRetornoEConsumirOCaminho_quandoContextoDestinoValidado', async () => {
    await enter();
    const path = '/demo-registro/view/123e4567-e89b-12d3-a456-426614174000';
    auth.rememberReturnPath(path);
    await choose('Beta');
    http.expectOne(apiUrl('/auth/context')).flush(context('beta'));
    await settle();
    expect(navigation.navigate).toHaveBeenCalledTimes(1);
    expect(navigation.navigate).toHaveBeenCalledWith([path]);
    expect(auth.consumeReturnPath()).toBe('/');
  });
  it.each(['403', '503', 'mismatch'])(
    'naoDeveNavegarNemManterTenant_quandoContextoFalha%sERecuperaDepois',
    async (failure) => {
      await enter();
      await choose('Alpha');
      http.expectOne(apiUrl('/auth/context')).flush(context('alpha'));
      await settle();
      navigation.navigate.mockClear();
      await choose('Beta');
      const request = http.expectOne(apiUrl('/auth/context'));
      expect(request.request.headers.get('X-Tenant-Alias')).toBe('beta');
      if (failure === 'mismatch') request.flush(context('alpha'));
      else request.flush({}, { status: Number(failure), statusText: 'Falha' });
      await settle();
      fixture.detectChanges();
      expect(navigation.navigate).not.toHaveBeenCalled();
      expect(auth.currentTenant()).toBeNull();
      expect(auth.effectiveActions()).toEqual([]);
      expect(alert()).toBe('Não foi possível acessar esta empresa. Escolha novamente.');
      expect(buttons().every((button) => !button.disabled)).toBe(true);
      client.get(apiUrl('/demo-registros/1')).subscribe({ error: () => {} });
      await settle();
      http.expectNone(apiUrl('/demo-registros/1'));
      await choose('Beta');
      http.expectOne(apiUrl('/auth/context')).flush(context('beta'));
      await settle();
      fixture.detectChanges();
      expect(auth.currentTenant()?.alias).toBe('beta');
      expect(alert()).toBe('');
      expect(navigation.navigate).toHaveBeenCalledWith(['/demo-registro/list']);
    },
  );
  it('deveAnunciarFalhaSemNavegar_quandoAliasNaoAutorizado', async () => {
    await enter();
    await expect(auth.selectTenant('gamma')).rejects.toThrow();
    fixture.detectChanges();
    expect(alert()).toBe('Não foi possível acessar esta empresa. Escolha novamente.');
    expect(auth.currentTenant()).toBeNull();
    expect(auth.effectiveActions()).toEqual([]);
    expect(buttons().every((button) => !button.disabled)).toBe(true);
    expect(navigation.navigate).not.toHaveBeenCalled();
    http.expectNone(apiUrl('/auth/context'));
  });
  it('deveRecuperarSeletorSemNavegar_quandoSelecaoNegadaChegaComSelecaoPendente', async () => {
    await enter();
    await choose('Alpha');
    const pendingContext = http.expectOne(apiUrl('/auth/context'));
    expect(buttons().every((button) => button.disabled)).toBe(true);
    await expect(auth.selectTenant('gamma')).rejects.toThrow();
    await settle();
    fixture.detectChanges();
    expect(pendingContext.cancelled).toBe(true);
    expect(buttons().every((button) => !button.disabled)).toBe(true);
    expect(status()).toBe('');
    expect(alert()).toBe('Não foi possível acessar esta empresa. Escolha novamente.');
    expect(auth.currentTenant()).toBeNull();
    expect(auth.effectiveActions()).toEqual([]);
    expect(navigation.navigate).not.toHaveBeenCalled();
    await choose('Beta');
    http.expectOne(apiUrl('/auth/context')).flush(context('beta'));
    await settle();
    expect(navigation.navigate).toHaveBeenCalledWith(['/demo-registro/list']);
  });
  it.each(['logout', 'uid'])(
    'naoDeveNavegarNemConsumirRetorno_quandoSelecaoAntigaConcluiAposContextoNovoComMesmoAlias_%s',
    async (reason) => {
      await enter();
      const original = auth.selectTenant.bind(auth);
      let release!: () => void;
      const gate = new Promise<void>((resolve) => (release = resolve));
      let delayed = false;
      vi.spyOn(auth, 'selectTenant').mockImplementation((alias) => {
        const real = original(alias);
        if (delayed) return real;
        delayed = true;
        return gate.then(() => real);
      });
      await choose('Alpha');
      const oldContext = http.expectOne(apiUrl('/auth/context'));
      if (reason === 'logout') {
        await auth.signOut();
        loginAs = first;
        const login = auth.signInWithGoogle();
        await settle();
        http.expectOne(apiUrl('/auth/me')).flush(account());
        await login;
      } else {
        change(second);
        await settle();
        http.expectOne(apiUrl('/auth/me')).flush(account());
        await settle();
      }
      expect(oldContext.cancelled).toBe(true);
      const path = '/demo-registro/view/123e4567-e89b-12d3-a456-426614174000';
      auth.rememberReturnPath(path);
      const current = auth.selectTenant('alpha');
      await settle();
      http.expectOne(apiUrl('/auth/context')).flush(context('alpha'));
      await current;
      expect(auth.currentTenant()?.alias).toBe('alpha');
      release();
      await settle();
      expect(navigation.navigate).not.toHaveBeenCalled();
      expect(auth.consumeReturnPath()).toBe(path);
    },
  );
});
