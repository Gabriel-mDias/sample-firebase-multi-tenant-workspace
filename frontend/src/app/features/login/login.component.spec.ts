import { TestBed } from '@angular/core/testing';
import { provideRouter, ActivatedRoute } from '@angular/router';
import { signal } from '@angular/core';
import { Subject } from 'rxjs';
import { GemsNavigationService, provideGemsHttp } from '@gabriel-mdias/angular-gems-sdk';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { LoginComponent } from './login.component';
import { AuthService } from '../../core/auth/auth.service';
import { IDENTITY_ADAPTER, IdentityAdapter, IdentityUser } from '../../core/auth/identity.adapter';
import { firebaseInterceptor, gemsApiRoot } from '../../core/auth/firebase.interceptor';
import { environment } from '../../../environments/environment';
import { buildMenu } from '../../core/menu/menu.builder';
import { Acoes } from '../../core/auth/acoes';

describe('LoginComponent', () => {
  let resolve: () => void;
  const auth = {
    invalidated: new Subject<void>(),
    invalidationReason: signal('logout'),
    authenticatedUser: signal<{ uid: string } | null>(null),
    sessionReady: signal(false),
    me: signal(null),
    currentTenant: signal(null),
    tenants: signal([]),
    generation: signal(0),
    signInWithEmailAndPassword: vi.fn(),
    signInWithGoogle: vi.fn(),
    resetPassword: vi.fn(),
    rememberReturnPath: vi.fn(),
    capture: () => ({ generation: 0 }),
    isCurrent: () => true,
  };
  let navigation: { navigate: ReturnType<typeof vi.fn> };
  beforeEach(async () => {
    vi.clearAllMocks();
    navigation = { navigate: vi.fn() };
    auth.sessionReady.set(false);
    auth.signInWithEmailAndPassword.mockImplementation(
      () => new Promise<void>((yes) => (resolve = yes)),
    );
    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: auth },
        { provide: GemsNavigationService, useValue: navigation },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: { get: () => '//evil.test' } } },
        },
      ],
    }).compileComponents();
  });
  it('deveCancelarEstadoPendente_quandoLogoutInvalidaLoginAntesDaResposta', async () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
    fixture.componentInstance.form.setValue({ email: 'pessoa@example.test', password: 'existing' });
    const signing = fixture.componentInstance.submit();
    fixture.detectChanges();
    auth.invalidated.next();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('button[type="submit"]').disabled).toBe(false);
    resolve();
    await signing;
    expect(navigation.navigate).not.toHaveBeenCalled();
    expect(fixture.componentInstance.form.controls.email.value).toBe('');
  });
  it('naoDeveNavegar_quandoRespostaChegaDepoisDeDestruirLogin', async () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
    fixture.componentInstance.form.setValue({ email: 'pessoa@example.test', password: 'existing' });
    const signing = fixture.componentInstance.submit();
    fixture.destroy();
    auth.sessionReady.set(true);
    resolve();
    await signing;
    expect(navigation.navigate).not.toHaveBeenCalled();
  });
  it('deveAceitarSenhaExistente_quandoNaoCumprePoliticaCadastro', async () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
    const form = fixture.componentInstance.form;
    form.setValue({ email: 'pessoa@example.test', password: 'short' });
    fixture.detectChanges();
    expect(form.valid).toBe(true);
    const submit = fixture.nativeElement.querySelector('button[type="submit"]');
    submit.click();
    submit.click();
    fixture.detectChanges();
    expect(auth.signInWithEmailAndPassword).toHaveBeenCalledTimes(1);
    expect(submit.disabled).toBe(true);
    resolve();
    await fixture.whenStable();
    expect(navigation.navigate).not.toHaveBeenCalled();
  });
  it('deveAssociarLabels_quandoFormularioRenderiza', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('label[for="login-email"]')).toBeTruthy();
    const ids = [...el.querySelectorAll('[id]')].map((element) => element.id);
    expect(new Set(ids).size).toBe(ids.length);
    expect((el.querySelector('#login-password') as HTMLInputElement).labels?.length).toBe(1);
    expect(el.querySelector('gems-input-password button')?.getAttribute('tabindex')).toBe('0');
    expect(el.querySelector('input[id="login-password"]')?.getAttribute('autocomplete')).toBe(
      'current-password',
    );
    expect(el.querySelector('[role="status"]')).toBeTruthy();
    expect(el.textContent).not.toContain('Criar conta');
  });
  it('deveConfirmarResetGenericamente_quandoProviderRejeitaConta', async () => {
    auth.resetPassword.mockRejectedValue(new Error('user-not-found'));
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
    fixture.componentInstance.form.controls.email.setValue('pessoa@example.test');
    await fixture.componentInstance.resetPassword();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Se a conta estiver habilitada');
    expect(fixture.nativeElement.textContent).not.toContain('user-not-found');
  });
});

describe('LoginComponent / fachada real e ordem nativa do observer', () => {
  let auth: AuthService;
  let http: HttpTestingController;
  let observe: (user: IdentityUser | null) => void;
  let completeOldLogin: (user: IdentityUser) => void;
  let adapter: IdentityAdapter;
  let navigation: { navigate: ReturnType<typeof vi.fn> };
  const oldUser = { uid: 'old-uid', getIdToken: async () => 'old-token' };
  const account = {
    user: { id: '1', name: 'Pessoa', email: 'pessoa@example.test' },
    tenants: [{ alias: 'alpha', name: 'Alpha' }],
    globalActions: [],
  };
  const tick = async () => {
    for (let i = 0; i < 8; i++) await Promise.resolve();
  };
  beforeEach(() => {
    navigation = { navigate: vi.fn() };
    adapter = {
      observe: (callback) => {
        observe = callback;
        return () => {};
      },
      signInWithEmailAndPassword: () =>
        new Promise((resolve) => {
          completeOldLogin = resolve;
        }),
      signInWithGoogle: async () => oldUser,
      signOut: async () => {},
      resetPassword: async () => {},
    };
    TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([firebaseInterceptor])),
        provideHttpClientTesting(),
        provideGemsHttp(gemsApiRoot()),
        { provide: IDENTITY_ADAPTER, useValue: adapter },
        { provide: GemsNavigationService, useValue: navigation },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: { get: () => '/' },
              paramMap: { get: () => null },
              url: [{ path: 'form' }],
            },
          },
        },
      ],
    });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => {
    try {
      http.verify();
    } finally {
      TestBed.resetTestingModule();
    }
  });
  it.each([
    ['password', 'auth/invalid-credential'],
    ['google', 'auth/popup-closed-by-user'],
    ['password', 'auth/network-request-failed'],
    ['google', 'auth/network-request-failed'],
    ['password', 'me-403'],
    ['google', 'me-403'],
  ])('G12 deveNegarGenericamente_quandoFalha_%s_%s', async (method, failure) => {
    const remote = 'conta-remota@example.test; vínculo Alpha; UID privado';
    const result = async () => {
      if (failure === 'me-403') return oldUser;
      throw new Error(`${failure}: ${remote}`);
    };
    adapter.signInWithEmailAndPassword = result;
    adapter.signInWithGoogle = result;
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
    fixture.componentInstance.form.setValue({ email: 'pessoa@example.test', password: 'existing' });
    const signing =
      method === 'password'
        ? fixture.componentInstance.submit()
        : fixture.componentInstance.google();
    await tick();
    if (failure === 'me-403') {
      http
        .expectOne(environment.apiBaseUrl + '/auth/me')
        .flush({ message: remote }, { status: 403, statusText: 'Forbidden' });
    }
    await signing;
    fixture.detectChanges();
    const element: HTMLElement = fixture.nativeElement;
    expect(element.querySelector('[role="alert"]')?.textContent?.trim()).toBe(
      'Não foi possível entrar. Confira seu acesso ou tente novamente.',
    );
    expect(element.textContent).not.toContain(remote);
    for (const detail of ['conta-remota@example.test', 'vínculo Alpha', 'UID privado'])
      expect(element.textContent).not.toContain(detail);
    expect(element.textContent).not.toContain(failure);
    expect(element.querySelector('[role="status"]')?.textContent?.trim()).toBe('');
    expect((element.querySelector('button[type="submit"]') as HTMLButtonElement).disabled).toBe(
      false,
    );
    expect(fixture.componentInstance.form.enabled).toBe(true);
    expect(auth.loading()).toBe(false);
    expect(auth.me()).toBeNull();
    expect(auth.currentTenant()).toBeNull();
    expect(auth.tenants()).toEqual([]);
    expect(auth.effectiveActions()).toEqual([]);
    expect(auth.sessionReady()).toBe(false);
    expect(navigation.navigate).not.toHaveBeenCalled();
    http.expectNone(environment.apiBaseUrl + '/auth/context');
    fixture.destroy();
  });
  it.each(['password', 'google', 'reset'])(
    'G12 deveBloquearTodasAsOperacoes_quandoPendente_%s',
    async (method) => {
      let complete!: () => void;
      const pending = new Promise<void>((resolve) => (complete = resolve));
      const password = vi.fn(async () => {
        await pending;
        throw new Error('auth/invalid-credential');
      });
      const google = vi.fn(async () => {
        await pending;
        throw new Error('auth/popup-closed-by-user');
      });
      const reset = vi.fn(() => pending);
      adapter.signInWithEmailAndPassword = password;
      adapter.signInWithGoogle = google;
      adapter.resetPassword = reset;
      const fixture = TestBed.createComponent(LoginComponent);
      fixture.detectChanges();
      const component = fixture.componentInstance;
      component.form.setValue({ email: 'pessoa@example.test', password: 'existing' });
      const running =
        method === 'password'
          ? component.submit()
          : method === 'google'
            ? component.google()
            : component.resetPassword();
      fixture.detectChanges();
      await fixture.whenStable();
      const element: HTMLElement = fixture.nativeElement;
      expect(component.form.disabled).toBe(true);
      expect(element.querySelector('form')?.getAttribute('aria-busy')).toBe('true');
      expect(element.querySelector('[role="status"]')?.textContent?.trim()).toBe(
        'Aguarde, estamos validando seu acesso…',
      );
      for (const input of element.querySelectorAll<HTMLInputElement>('input'))
        expect(input.disabled).toBe(true);
      for (const button of element.querySelectorAll<HTMLButtonElement>('button'))
        expect(button.disabled).toBe(true);
      expect(element.querySelector('gems-input-password button')?.getAttribute('tabindex')).toBe(
        '-1',
      );
      await component.submit();
      await component.google();
      await component.resetPassword();
      expect(password).toHaveBeenCalledTimes(method === 'password' ? 1 : 0);
      expect(google).toHaveBeenCalledTimes(method === 'google' ? 1 : 0);
      expect(reset).toHaveBeenCalledTimes(method === 'reset' ? 1 : 0);
      complete();
      await running;
      fixture.detectChanges();
      await fixture.whenStable();
      expect(component.form.enabled).toBe(true);
      expect(element.querySelector('form')?.getAttribute('aria-busy')).toBe('false');
      expect(
        (element.querySelector('gems-input-password button') as HTMLButtonElement).disabled,
      ).toBe(false);
      expect(navigation.navigate).not.toHaveBeenCalled();
      fixture.destroy();
    },
  );
  it.each(['success', 'rejection'])(
    'G12 deveEnviarEmailEConfirmarResetGenericamente_quandoResultado_%s',
    async (outcome) => {
      const reset = vi.fn(async () => {
        if (outcome === 'rejection') throw new Error('conta inexistente; vínculo privado');
      });
      adapter.resetPassword = reset;
      const fixture = TestBed.createComponent(LoginComponent);
      fixture.detectChanges();
      fixture.componentInstance.form.controls.email.setValue('pessoa@example.test');
      await fixture.componentInstance.resetPassword();
      fixture.detectChanges();
      expect(reset).toHaveBeenCalledExactlyOnceWith('pessoa@example.test');
      expect(fixture.nativeElement.querySelector('[role="status"]').textContent.trim()).toBe(
        'Se a conta estiver habilitada, você receberá um e-mail com as instruções para definir sua senha.',
      );
      expect(fixture.nativeElement.querySelector('[role="alert"]').textContent.trim()).toBe('');
      expect(fixture.nativeElement.textContent).not.toContain('vínculo privado');
      expect(fixture.componentInstance.form.enabled).toBe(true);
      expect(navigation.navigate).not.toHaveBeenCalled();
      http.expectNone(environment.apiBaseUrl + '/auth/me');
      fixture.destroy();
    },
  );
  for (const method of ['password', 'google', 'reset']) {
    it.each(['resolve', 'reject'])(
      `G12 deveIgnorarResultadoAntigo_quandoLogoutDurante_${method}_%s`,
      async (settlement) => {
        let resolve!: () => void;
        let reject!: (reason: Error) => void;
        const pending = new Promise<void>((yes, no) => {
          resolve = yes;
          reject = no;
        });
        adapter.signInWithEmailAndPassword = async () => {
          await pending;
          return oldUser;
        };
        adapter.signInWithGoogle = async () => {
          await pending;
          return oldUser;
        };
        adapter.resetPassword = () => pending;
        const fixture = TestBed.createComponent(LoginComponent);
        fixture.detectChanges();
        const component = fixture.componentInstance;
        component.form.setValue({ email: 'pessoa@example.test', password: 'existing' });
        const running =
          method === 'password'
            ? component.submit()
            : method === 'google'
              ? component.google()
              : component.resetPassword();
        await auth.signOut();
        if (settlement === 'resolve') resolve();
        else reject(new Error('resposta antiga privada'));
        await running;
        fixture.detectChanges();
        expect(component.form.getRawValue()).toEqual({ email: '', password: '' });
        expect(component.form.enabled).toBe(true);
        expect(fixture.nativeElement.querySelector('[role="alert"]').textContent.trim()).toBe('');
        expect(fixture.nativeElement.querySelector('[role="status"]').textContent.trim()).toBe('');
        expect(navigation.navigate).not.toHaveBeenCalled();
        http.expectNone(environment.apiBaseUrl + '/auth/me');
        fixture.destroy();
      },
    );
  }
  for (const method of ['password', 'google']) {
    it.each(['C', 'B/C'])(
      `FRI04 naoDeveNavegarB_quandoRejeicaoAntigaPrecedeResultadoAtual_${method}_%s`,
      async (observations) => {
        let rejectOld!: (reason: Error) => void;
        adapter.signInWithEmailAndPassword = () => new Promise((_, no) => (rejectOld = no));
        const oldOutcome = auth
          .signInWithEmailAndPassword('a@example.test', 'existing')
          .catch((error: Error) => error.message);
        let resolveCurrent!: (user: IdentityUser) => void;
        const result = new Promise<IdentityUser>((yes) => (resolveCurrent = yes));
        adapter.signInWithEmailAndPassword = () => result;
        adapter.signInWithGoogle = () => result;
        const fixture = TestBed.createComponent(LoginComponent);
        fixture.detectChanges();
        fixture.componentInstance.form.setValue({ email: 'b@example.test', password: 'existing' });
        const signing =
          method === 'password'
            ? fixture.componentInstance.submit()
            : fixture.componentInstance.google();
        const b = { uid: 'b', getIdToken: vi.fn(async () => 'b-token') };
        const c = { uid: 'c', getIdToken: vi.fn(async () => 'c-token') };
        if (observations === 'B/C') observe(b);
        observe(c);
        rejectOld(new Error('denied'));
        expect(await oldOutcome).toBe('stale-session');
        resolveCurrent(b);
        await tick();
        // Drain any erroneous bootstrap so the red reports a security assertion,
        // rather than an unfinished test or an unhandled Promise rejection.
        for (const request of http.match(environment.apiBaseUrl + '/auth/me'))
          request.flush(account);
        await signing;
        fixture.detectChanges();
        expect(b.getIdToken).not.toHaveBeenCalled();
        expect(auth.authenticatedUser()).toBeNull();
        expect(auth.sessionReady()).toBe(false);
        expect(buildMenu(auth)).toEqual([]);
        expect(navigation.navigate).not.toHaveBeenCalled();
        http.expectNone(environment.apiBaseUrl + '/auth/context');
        fixture.destroy();
      },
    );
  }
  it.each(['password', 'google'])(
    'FRI01 deveLimparLoginEMenu_quandoUIDCanceladoRetornaAposNovaSessao_%s',
    async (method) => {
      const fixture = TestBed.createComponent(LoginComponent);
      fixture.detectChanges();
      fixture.componentInstance.form.setValue({ email: 'a@example.test', password: 'existing' });
      const cancelled = fixture.componentInstance.submit();
      await auth.signOut();
      completeOldLogin(oldUser);
      await cancelled;
      let token = Promise.resolve('b-token');
      const b = { uid: 'confirmed-b', getIdToken: () => token };
      adapter.signInWithEmailAndPassword = async () => b;
      adapter.signInWithGoogle = async () => b;
      fixture.componentInstance.form.setValue({ email: 'b@example.test', password: 'existing' });
      const signing =
        method === 'password'
          ? fixture.componentInstance.submit()
          : fixture.componentInstance.google();
      await tick();
      http.expectOne(environment.apiBaseUrl + '/auth/me').flush(account);
      await signing;
      fixture.detectChanges();
      const switching = auth.selectTenant('alpha');
      await tick();
      http.expectOne(environment.apiBaseUrl + '/auth/context').flush({
        tenant: account.tenants[0],
        profiles: [],
        groups: [],
        actions: [Acoes.CONSULTAR_DEMO],
      });
      await switching;
      const client = TestBed.inject(HttpClient);
      let response = false;
      client.post(environment.apiBaseUrl + '/demo-registros', { nome: 'Alpha' }).subscribe({
        next: () => (response = true),
      });
      await tick();
      const oldSave = http.expectOne(environment.apiBaseUrl + '/demo-registros');
      let resolveToken!: (value: string) => void;
      token = new Promise((resolve) => (resolveToken = resolve));
      client
        .post(environment.apiBaseUrl + '/demo-registros', { nome: 'Alpha deferred' })
        .subscribe();
      fixture.componentInstance.form.setValue({ email: 'b@example.test', password: 'existing' });
      navigation.navigate.mockClear();
      observe(oldUser);
      fixture.detectChanges();
      expect(oldSave.cancelled).toBe(true);
      expect(auth.authenticatedUser()).toBeNull();
      expect(buildMenu(auth)).toEqual([]);
      expect(fixture.componentInstance.form.getRawValue()).toEqual({ email: '', password: '' });
      resolveToken('old-b-token');
      await tick();
      fixture.detectChanges();
      http.expectNone(environment.apiBaseUrl + '/demo-registros');
      http.expectNone(environment.apiBaseUrl + '/auth/me');
      expect(response).toBe(false);
      expect(navigation.navigate).not.toHaveBeenCalled();
      fixture.destroy();
    },
  );
  it.each(['password', 'google'])(
    'FRI02 naoDeveNavegarB_quandoCObservadoAntesDePromiseB_%s',
    async (method) => {
      let resolve!: (user: IdentityUser) => void;
      const result = new Promise<IdentityUser>((yes) => (resolve = yes));
      adapter.signInWithEmailAndPassword = () => result;
      adapter.signInWithGoogle = () => result;
      const fixture = TestBed.createComponent(LoginComponent);
      fixture.detectChanges();
      fixture.componentInstance.form.setValue({ email: 'b@example.test', password: 'existing' });
      const signing =
        method === 'password'
          ? fixture.componentInstance.submit()
          : fixture.componentInstance.google();
      const b = { uid: 'b', getIdToken: vi.fn(async () => 'b-token') };
      const c = { uid: 'c', getIdToken: async () => 'c-token' };
      observe(b);
      observe(c);
      resolve(b);
      await signing;
      await tick();
      fixture.detectChanges();
      expect(b.getIdToken).not.toHaveBeenCalled();
      expect(navigation.navigate).not.toHaveBeenCalled();
      const request = http.expectOne(environment.apiBaseUrl + '/auth/me');
      expect(request.request.headers.get('Authorization')).toBe('Bearer c-token');
      request.flush({ ...account, user: { ...account.user, name: 'Pessoa C' } });
      await tick();
      fixture.detectChanges();
      expect(auth.authenticatedUser()).toBe(c);
      expect(auth.username()).toBe('Pessoa C');
      expect(auth.currentTenant()).toBeNull();
      expect(navigation.navigate).toHaveBeenCalledOnce();
      expect(navigation.navigate).toHaveBeenCalledWith(['/tenants']);
      fixture.destroy();
    },
  );
  it.each(['password', 'google'])(
    'FRI03 deveNavegarAposContaValidada_quandoPrimeiroNuloChegaDuranteLogin_%s',
    async (method) => {
      let resolve!: (user: IdentityUser) => void;
      const result = new Promise<IdentityUser>((yes) => (resolve = yes));
      adapter.signInWithEmailAndPassword = () => result;
      adapter.signInWithGoogle = () => result;
      const fixture = TestBed.createComponent(LoginComponent);
      fixture.detectChanges();
      fixture.componentInstance.form.setValue({ email: 'b@example.test', password: 'existing' });
      const signing =
        method === 'password'
          ? fixture.componentInstance.submit()
          : fixture.componentInstance.google();
      observe(null);
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('button[type="submit"]').disabled).toBe(true);
      expect(navigation.navigate).not.toHaveBeenCalled();
      resolve({ uid: 'b', getIdToken: async () => 'b-token' });
      await tick();
      http.expectOne(environment.apiBaseUrl + '/auth/me').flush(account);
      await signing;
      fixture.detectChanges();
      expect(auth.sessionReady()).toBe(true);
      expect(navigation.navigate).toHaveBeenCalledOnce();
      expect(navigation.navigate).toHaveBeenCalledWith(['/tenants']);
      fixture.destroy();
    },
  );
  it('deveLimparFormEMenuSemNavegacaoAntiga_quandoOutraAbaTrocaIdentidadeComResponseDeferred', async () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
    const signing = fixture.componentInstance.google();
    await tick();
    http.expectOne(environment.apiBaseUrl + '/auth/me').flush(account);
    await signing;
    fixture.detectChanges();
    const switching = auth.selectTenant('alpha');
    await tick();
    http.expectOne(environment.apiBaseUrl + '/auth/context').flush({
      tenant: { alias: 'alpha', name: 'Alpha' },
      profiles: [],
      groups: [],
      actions: [Acoes.CONSULTAR_DEMO, Acoes.INSERIR_DEMO],
    });
    await switching;
    let response = false;
    TestBed.inject(HttpClient)
      .post(environment.apiBaseUrl + '/demo-registros', { nome: 'Alpha' })
      .subscribe({
        next: () => {
          response = true;
        },
      });
    await tick();
    const oldSave = http.expectOne(environment.apiBaseUrl + '/demo-registros');
    fixture.componentInstance.form.setValue({ email: 'old@example.test', password: 'existing' });
    let completeToken!: (token: string) => void;
    const nextToken = new Promise<string>((resolve) => {
      completeToken = resolve;
    });
    navigation.navigate.mockClear();
    expect(buildMenu(auth)).toHaveLength(1);
    observe({ uid: 'cross-tab-new', getIdToken: () => nextToken });
    expect(oldSave.cancelled).toBe(true);
    expect(response).toBe(false);
    expect(fixture.componentInstance.form.getRawValue()).toEqual({ email: '', password: '' });
    expect(buildMenu(auth)).toEqual([]);
    expect(auth.me()).toBeNull();
    expect(auth.currentTenant()).toBeNull();
    fixture.detectChanges();
    expect(navigation.navigate).not.toHaveBeenCalled();
    completeToken('new-token');
    await tick();
    const newMe = http.expectOne(environment.apiBaseUrl + '/auth/me');
    expect(newMe.request.headers.get('Authorization')).toBe('Bearer new-token');
    newMe.flush({ ...account, user: { ...account.user, name: 'Nova pessoa' } });
    await tick();
    fixture.detectChanges();
    expect(navigation.navigate).toHaveBeenCalledOnce();
    expect(navigation.navigate).toHaveBeenCalledWith(['/tenants']);
    expect(auth.username()).toBe('Nova pessoa');
    expect(response).toBe(false);
    expect(auth.currentTenant()).toBeNull();
    fixture.destroy();
  });
  it('naoDeveNavegarOuValidarConta_quandoLogoutPrecedeObserverEResultadoAntigos', async () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
    fixture.componentInstance.form.setValue({ email: 'old@example.test', password: 'existing' });
    const signing = fixture.componentInstance.submit();
    await auth.signOut();
    const snapshot = auth.capture();
    observe(oldUser);
    await tick();
    fixture.detectChanges();
    expect(auth.isCurrent(snapshot)).toBe(true);
    expect(auth.sessionReady()).toBe(false);
    http.expectNone(environment.apiBaseUrl + '/auth/me');
    expect(navigation.navigate).not.toHaveBeenCalled();
    completeOldLogin(oldUser);
    await signing;
    fixture.detectChanges();
    expect(auth.isCurrent(snapshot)).toBe(true);
    expect(navigation.navigate).not.toHaveBeenCalled();
    expect(fixture.componentInstance.form.controls.email.value).toBe('');
  });
  it.each(['new-uid', 'old-uid'])(
    'naoDeveRepetirNavegacao_quandoObserverELoginAntigosChegamAposIntencaoNovaComUID%s',
    async (uid) => {
      const fixture = TestBed.createComponent(LoginComponent);
      fixture.detectChanges();
      fixture.componentInstance.form.setValue({ email: 'old@example.test', password: 'existing' });
      const old = fixture.componentInstance.submit();
      const next = { uid, getIdToken: async () => 'next-token' };
      adapter.signInWithGoogle = async () => next;
      const current = auth.signInWithGoogle();
      await tick();
      http.expectOne(environment.apiBaseUrl + '/auth/me').flush(account);
      await current;
      fixture.detectChanges();
      expect(navigation.navigate).toHaveBeenCalledOnce();
      expect(navigation.navigate).toHaveBeenCalledWith(['/tenants']);
      const snapshot = auth.capture();
      observe(oldUser);
      await tick();
      fixture.detectChanges();
      expect(auth.isCurrent(snapshot)).toBe(uid === oldUser.uid);
      expect(auth.authenticatedUser()).toBe(uid === oldUser.uid ? next : null);
      completeOldLogin(oldUser);
      await old;
      fixture.detectChanges();
      expect(auth.isCurrent(snapshot)).toBe(uid === oldUser.uid);
      expect(auth.sessionReady()).toBe(uid === oldUser.uid);
      expect(navigation.navigate).toHaveBeenCalledOnce();
      http.expectNone(environment.apiBaseUrl + '/auth/me');
    },
  );
});
