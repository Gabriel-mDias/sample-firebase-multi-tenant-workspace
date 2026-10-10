import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { provideGemsHttp } from '@gabriel-mdias/angular-gems-sdk';
import { AuthService } from './auth.service';
import { AuthStore } from './auth.store';
import { IDENTITY_ADAPTER, IdentityAdapter, IdentityUser } from './identity.adapter';
import { firebaseInterceptor, gemsApiRoot, isApiDestination } from './firebase.interceptor';
import { Acoes } from './acoes';
import { environment } from '../../../environments/environment';
import { buildMenu } from '../menu/menu.builder';

function deferred<T>() {
  let resolve!: (value: T) => void;
  let reject!: (reason?: unknown) => void;
  const promise = new Promise<T>((yes, no) => {
    resolve = yes;
    reject = no;
  });
  return { promise, resolve, reject };
}

describe('AuthService / isolamento determinístico', () => {
  let auth: AuthService;
  let http: HttpTestingController;
  let client: HttpClient;
  let change: (user: IdentityUser | null) => void;
  let token: ReturnType<typeof deferred<string>>;
  let adapter: IdentityAdapter;
  const user = { uid: 'uid-1', getIdToken: () => token.promise };
  const me = {
    user: { id: '1', name: 'Pessoa', email: 'pessoa@example.test' },
    tenants: [
      { alias: 'alpha', name: 'Alpha' },
      { alias: 'beta', name: 'Beta' },
    ],
    globalActions: [Acoes.CONSULTAR_PROPRIA_CONTA],
  };
  const context = (alias: string) => ({
    tenant: { alias, name: alias },
    profiles: [],
    groups: [],
    actions: [Acoes.CONSULTAR_DEMO, Acoes.INSERIR_DEMO],
  });
  const tick = async () => {
    for (let i = 0; i < 8; i++) await Promise.resolve();
  };
  async function ready() {
    change(user);
    token.resolve('token-one');
    await tick();
    http.expectOne(environment.apiBaseUrl + '/auth/me').flush(me);
    await tick();
    const switching = auth.selectTenant('alpha');
    await tick();
    http.expectOne(environment.apiBaseUrl + '/auth/context').flush(context('alpha'));
    await switching;
  }
  async function readyWithoutTenant() {
    change(user);
    token.resolve('token-one');
    await tick();
    http.expectOne(environment.apiBaseUrl + '/auth/me').flush(me);
    await tick();
  }
  beforeEach(() => {
    token = deferred<string>();
    adapter = {
      observe: (callback) => {
        change = callback;
        return () => {};
      },
      signInWithEmailAndPassword: async () => user,
      signInWithGoogle: async () => user,
      resetPassword: async () => {},
      signOut: async () => {},
    };
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([firebaseInterceptor])),
        provideHttpClientTesting(),
        provideGemsHttp(gemsApiRoot()),
        { provide: IDENTITY_ADAPTER, useValue: adapter },
      ],
    });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
    client = TestBed.inject(HttpClient);
  });
  afterEach(() => {
    try {
      http.verify();
    } finally {
      TestBed.resetTestingModule();
    }
  });
  for (const method of ['password', 'google']) {
    for (const oldSettlement of ['reject', 'resolve-B', 'resolve-A', 'resolve-C']) {
      it.each(['old/B/C', 'B/old/C', 'C/old/B'])(
        `FRI04 deveRespeitarCallbacksIntercalados_${method}_${oldSettlement}_%s`,
        async (events) => {
          const old = deferred<IdentityUser>();
          const current = deferred<IdentityUser>();
          adapter.signInWithEmailAndPassword = () => old.promise;
          const oldOutcome = auth
            .signInWithEmailAndPassword('a@example.test', 'existing')
            .catch((error: Error) => error.message);
          adapter.signInWithEmailAndPassword = () => current.promise;
          adapter.signInWithGoogle = () => current.promise;
          const currentOutcome = (
            method === 'password'
              ? auth.signInWithEmailAndPassword('b@example.test', 'existing')
              : auth.signInWithGoogle()
          ).then(
            () => 'accepted',
            (error: Error) => error.message,
          );
          const identities = {
            A: { uid: 'a', getIdToken: vi.fn(async () => 'a-token') },
            B: { uid: 'b', getIdToken: vi.fn(async () => 'b-token') },
            C: { uid: 'c', getIdToken: vi.fn(async () => 'c-token') },
          };
          for (const event of events.split('/')) {
            if (event === 'old') {
              if (oldSettlement === 'reject') old.reject(new Error('denied'));
              else old.resolve(identities[oldSettlement.slice(-1) as 'A' | 'B' | 'C']);
              expect(await oldOutcome).toBe('stale-session');
            } else change(identities[event as 'B' | 'C']);
          }
          const coherent = events.endsWith('B');
          current.resolve(identities.B);
          await tick();
          for (const request of http.match(environment.apiBaseUrl + '/auth/me')) {
            expect(request.request.headers.get('Authorization')).toBe(
              coherent ? 'Bearer b-token' : 'Bearer c-token',
            );
            request.flush(me);
          }
          expect(await currentOutcome).toBe(coherent ? 'accepted' : 'stale-session');
          if (coherent) {
            expect(auth.authenticatedUser()?.uid).toBe('b');
            expect(auth.sessionReady()).toBe(true);
          } else {
            expect(identities.B.getIdToken).not.toHaveBeenCalled();
            expect(auth.authenticatedUser()?.uid).not.toBe('b');
            expect(auth.currentTenant()).toBeNull();
            expect(buildMenu(auth)).toEqual([]);
          }
        },
      );
    }
    it(`FRI04 deveCancelarBootstrapB_quandoCPrecedeRejeicaoAntiga_${method}`, async () => {
      const old = deferred<IdentityUser>();
      const current = deferred<IdentityUser>();
      adapter.signInWithEmailAndPassword = () => old.promise;
      const oldOutcome = auth
        .signInWithEmailAndPassword('a@example.test', 'existing')
        .catch((error: Error) => error.message);
      adapter.signInWithEmailAndPassword = () => current.promise;
      adapter.signInWithGoogle = () => current.promise;
      const currentOutcome = (
        method === 'password'
          ? auth.signInWithEmailAndPassword('b@example.test', 'existing')
          : auth.signInWithGoogle()
      ).catch((error: Error) => error.message);
      const b = { uid: 'b', getIdToken: vi.fn(async () => 'b-token') };
      change(b);
      current.resolve(b);
      await tick();
      const bootstrapB = http.expectOne(environment.apiBaseUrl + '/auth/me');
      const c = { uid: 'c', getIdToken: vi.fn(async () => 'c-token') };
      change(c);
      expect(bootstrapB.cancelled).toBe(true);
      expect(auth.authenticatedUser()).toBeNull();
      expect(buildMenu(auth)).toEqual([]);
      old.reject(new Error('denied'));
      expect(await oldOutcome).toBe('stale-session');
      expect(await currentOutcome).toBe('stale-session');
      await tick();
      expect(b.getIdToken).toHaveBeenCalledTimes(1);
      expect(c.getIdToken).not.toHaveBeenCalled();
      http.expectNone(environment.apiBaseUrl + '/auth/me');
      http.expectNone(environment.apiBaseUrl + '/auth/context');
      expect(auth.me()).toBeNull();
      expect(auth.sessionReady()).toBe(false);
    });
    for (const oldSettlement of ['reject', 'resolve-B', 'resolve-A', 'resolve-C']) {
      for (const order of ['old-first', 'current-first']) {
        it.each(['C', 'B/C', 'B/C/B'])(
          `FRI04 deveRespeitarUltimoUID_${method}_${oldSettlement}_${order}_%s`,
          async (observations) => {
            const old = deferred<IdentityUser>();
            const current = deferred<IdentityUser>();
            adapter.signInWithEmailAndPassword = () => old.promise;
            const oldOutcome = auth.signInWithEmailAndPassword('a@example.test', 'existing').then(
              () => 'accepted',
              (error: Error) => error.message,
            );
            adapter.signInWithEmailAndPassword = () => current.promise;
            adapter.signInWithGoogle = () => current.promise;
            const currentOutcome = (
              method === 'password'
                ? auth.signInWithEmailAndPassword('b@example.test', 'existing')
                : auth.signInWithGoogle()
            ).then(
              () => 'accepted',
              (error: Error) => error.message,
            );
            const identities = {
              A: { uid: 'a', getIdToken: vi.fn(async () => 'a-token') },
              B: { uid: 'b', getIdToken: vi.fn(async () => 'b-token') },
              C: { uid: 'c', getIdToken: vi.fn(async () => 'c-token') },
            };
            const coherent = observations.endsWith('B');
            for (const uid of observations.split('/')) change(identities[uid as 'B' | 'C']);
            const flushAccount = async () => {
              await tick();
              for (const request of http.match(environment.apiBaseUrl + '/auth/me')) {
                expect(request.request.headers.get('Authorization')).toBe(
                  coherent ? 'Bearer b-token' : 'Bearer c-token',
                );
                expect(request.request.headers.has('X-Tenant-Alias')).toBe(false);
                request.flush(me);
              }
              await tick();
            };
            const finishOld = async () => {
              if (oldSettlement === 'reject') old.reject(new Error('denied'));
              else old.resolve(identities[oldSettlement.slice(-1) as 'A' | 'B' | 'C']);
              expect(await oldOutcome).toBe('stale-session');
              await flushAccount();
            };
            if (order === 'old-first') await finishOld();
            current.resolve(identities.B);
            await flushAccount();
            expect(await currentOutcome).toBe(coherent ? 'accepted' : 'stale-session');
            if (order === 'current-first') await finishOld();
            expect(identities.A.getIdToken).not.toHaveBeenCalled();
            if (coherent) {
              expect(auth.authenticatedUser()?.uid).toBe('b');
              expect(auth.sessionReady()).toBe(true);
              expect(identities.B.getIdToken).toHaveBeenCalledTimes(1);
            } else {
              expect(identities.B.getIdToken).not.toHaveBeenCalled();
              expect(auth.authenticatedUser()?.uid).not.toBe('b');
              expect(auth.currentTenant()).toBeNull();
              expect(auth.effectiveActions()).toEqual([]);
              expect(buildMenu(auth)).toEqual([]);
              client
                .post(environment.apiBaseUrl + '/demo-registros', { nome: 'B/Alpha' })
                .subscribe({ error: () => {} });
              await tick();
              http.expectNone(environment.apiBaseUrl + '/demo-registros');
              http.expectNone(environment.apiBaseUrl + '/auth/context');
            }
          },
        );
      }
    }
  }
  for (const method of ['password', 'google']) {
    it.each(['first-null', 'second-null', 'identity', 'logout', 'expiry'])(
      `FRI04 deveRespeitarRevogacaoComFinalizacaoAntiga_${method}_%s`,
      async (prior) => {
        if (prior === 'second-null') change(null);
        if (prior === 'identity') {
          token.resolve('previous-token');
          change(user);
          await tick();
          http.expectOne(environment.apiBaseUrl + '/auth/me').flush(me);
          await tick();
        }
        if (prior === 'logout') await auth.signOut();
        if (prior === 'expiry') auth.expire(auth.capture());
        const old = deferred<IdentityUser>();
        const current = deferred<IdentityUser>();
        adapter.signInWithEmailAndPassword = () => old.promise;
        const oldOutcome = auth
          .signInWithEmailAndPassword('a@example.test', 'existing')
          .catch((error: Error) => error.message);
        adapter.signInWithEmailAndPassword = () => current.promise;
        adapter.signInWithGoogle = () => current.promise;
        const currentOutcome = (
          method === 'password'
            ? auth.signInWithEmailAndPassword('b@example.test', 'existing')
            : auth.signInWithGoogle()
        ).then(
          () => 'accepted',
          (error: Error) => error.message,
        );
        change(null);
        old.reject(new Error('denied'));
        expect(await oldOutcome).toBe('stale-session');
        const b = { uid: 'b', getIdToken: vi.fn(async () => 'b-token') };
        current.resolve(b);
        await tick();
        if (prior === 'first-null') {
          http.expectOne(environment.apiBaseUrl + '/auth/me').flush(me);
          expect(await currentOutcome).toBe('accepted');
          expect(auth.authenticatedUser()).toBe(b);
        } else {
          expect(await currentOutcome).toBe('stale-session');
          expect(b.getIdToken).not.toHaveBeenCalled();
          expect(auth.authenticatedUser()).toBeNull();
          http.expectNone(environment.apiBaseUrl + '/auth/me');
        }
      },
    );
  }
  it.each([
    ['password', false],
    ['google', false],
    ['password', true],
    ['google', true],
  ])(
    'FRI01 deveRevogarAlphaAntesDeAvaliarUIDCancelado_%s_pendente%s',
    async (method, pendingAnother) => {
      const cancelled = deferred<IdentityUser>();
      adapter.signInWithEmailAndPassword = () => cancelled.promise;
      const staleLogin = auth.signInWithEmailAndPassword('a@example.test', 'existing');
      const rejected = expect(staleLogin).rejects.toThrow('stale-session');
      await auth.signOut();
      const cancelledUser = { uid: 'cancelled-a', getIdToken: async () => 'cancelled-token' };
      cancelled.resolve(cancelledUser);
      await rejected;
      const activeToken = deferred<string>();
      const active = { uid: 'confirmed-b', getIdToken: () => activeToken.promise };
      const another = deferred<IdentityUser>();
      let rejectedAnother: Promise<void> | undefined;
      if (pendingAnother) {
        adapter.signInWithGoogle = () => another.promise;
        rejectedAnother = expect(auth.signInWithGoogle()).rejects.toThrow('stale-session');
      }
      adapter.signInWithEmailAndPassword = async () => active;
      adapter.signInWithGoogle = async () => active;
      const signing =
        method === 'password'
          ? auth.signInWithEmailAndPassword('b@example.test', 'existing')
          : auth.signInWithGoogle();
      activeToken.resolve('confirmed-token');
      await tick();
      http.expectOne(environment.apiBaseUrl + '/auth/me').flush(me);
      await signing;
      const switching = auth.selectTenant('alpha');
      await tick();
      http.expectOne(environment.apiBaseUrl + '/auth/context').flush(context('alpha'));
      await switching;
      let oldResponse = false;
      client.post(environment.apiBaseUrl + '/demo-registros', { nome: 'Alpha save' }).subscribe({
        next: () => (oldResponse = true),
      });
      client.post(environment.apiBaseUrl + '/demo-registros/search', { nome: 'Alpha' }).subscribe({
        next: () => (oldResponse = true),
      });
      await tick();
      const oldSave = http.expectOne(environment.apiBaseUrl + '/demo-registros');
      const oldSearch = http.expectOne(environment.apiBaseUrl + '/demo-registros/search');
      const pendingToken = deferred<string>();
      active.getIdToken = () => pendingToken.promise;
      let cancelledRequest = false;
      client
        .post(environment.apiBaseUrl + '/demo-registros', { nome: 'Alpha deferred' })
        .subscribe({
          complete: () => (cancelledRequest = true),
        });
      const before = auth.capture();
      change(cancelledUser);
      expect(auth.isCurrent(before)).toBe(false);
      expect(auth.authenticatedUser()).toBeNull();
      expect(auth.sessionReady()).toBe(false);
      expect(auth.me()).toBeNull();
      expect(auth.context()).toBeNull();
      expect(auth.currentTenant()).toBeNull();
      expect(auth.effectiveActions()).toEqual([]);
      expect(buildMenu(auth)).toEqual([]);
      expect(oldSave.cancelled).toBe(true);
      expect(oldSearch.cancelled).toBe(true);
      expect(cancelledRequest).toBe(true);
      pendingToken.resolve('old-b-token');
      if (pendingAnother) {
        another.resolve({ uid: 'cancelled-d', getIdToken: async () => 'd-token' });
        await rejectedAnother;
      }
      await tick();
      http.expectNone(environment.apiBaseUrl + '/demo-registros');
      http.expectNone(environment.apiBaseUrl + '/auth/me');
      expect(oldResponse).toBe(false);
    },
  );
  it.each(['password', 'google'])(
    'FRI02 naoDeveValidarResultadoB_quandoUltimaObservacaoNativaEC_%s',
    async (method) => {
      const result = deferred<IdentityUser>();
      adapter.signInWithEmailAndPassword = () => result.promise;
      adapter.signInWithGoogle = () => result.promise;
      const signing =
        method === 'password'
          ? auth.signInWithEmailAndPassword('b@example.test', 'existing')
          : auth.signInWithGoogle();
      const rejected = expect(signing).rejects.toThrow('stale-session');
      const b = { uid: 'promise-b', getIdToken: vi.fn(async () => 'b-token') };
      const c = { uid: 'native-c', getIdToken: async () => 'c-token' };
      change(b);
      change(c);
      result.resolve(b);
      await tick();
      expect(b.getIdToken).not.toHaveBeenCalled();
      expect(auth.sessionReady()).toBe(false);
      const request = http.expectOne(environment.apiBaseUrl + '/auth/me');
      expect(request.request.headers.get('Authorization')).toBe('Bearer c-token');
      expect(request.request.headers.has('X-Tenant-Alias')).toBe(false);
      request.flush(me);
      await rejected;
      await tick();
      expect(auth.authenticatedUser()).toBe(c);
      expect(auth.currentTenant()).toBeNull();
    },
  );
  it.each(['password', 'google'])(
    'FRI03 deveConcluirLoginValido_quandoPrimeiroCallbackAnonimoAtrasa_%s',
    async (method) => {
      const result = deferred<IdentityUser>();
      adapter.signInWithEmailAndPassword = () => result.promise;
      adapter.signInWithGoogle = () => result.promise;
      const signing =
        method === 'password'
          ? auth.signInWithEmailAndPassword('b@example.test', 'existing')
          : auth.signInWithGoogle();
      const outcome = signing.then(
        () => 'success',
        (error: Error) => error.message,
      );
      change(null);
      const b = { uid: 'valid-b', getIdToken: async () => 'b-token' };
      result.resolve(b);
      await tick();
      const request = http.expectOne(environment.apiBaseUrl + '/auth/me');
      expect(request.request.headers.get('Authorization')).toBe('Bearer b-token');
      request.flush(me);
      expect(await outcome).toBe('success');
      expect(auth.authenticatedUser()).toBe(b);
      expect(auth.sessionReady()).toBe(true);
    },
  );
  it('deveAceitarResultadoCoerente_quandoUltimaObservacaoRetornaAoUIDDaIntencao', async () => {
    const result = deferred<IdentityUser>();
    adapter.signInWithGoogle = () => result.promise;
    const signing = auth.signInWithGoogle();
    const b = { uid: 'b', getIdToken: async () => 'b-token' };
    change(b);
    change({ uid: 'c', getIdToken: async () => 'c-token' });
    change(b);
    result.resolve(b);
    await tick();
    const request = http.expectOne(environment.apiBaseUrl + '/auth/me');
    expect(request.request.headers.get('Authorization')).toBe('Bearer b-token');
    request.flush(me);
    await signing;
    expect(auth.authenticatedUser()).toBe(b);
  });
  it('naoDeveBootstraparUIDDivergente_quandoOperacaoAntigaAindaPrecisaClassificacao', async () => {
    const oldResult = deferred<IdentityUser>();
    const currentResult = deferred<IdentityUser>();
    adapter.signInWithEmailAndPassword = () => oldResult.promise;
    const oldOutcome = auth
      .signInWithEmailAndPassword('a@example.test', 'existing')
      .catch((error: Error) => error.message);
    adapter.signInWithGoogle = () => currentResult.promise;
    const currentOutcome = auth.signInWithGoogle().catch((error: Error) => error.message);
    change({ uid: 'c', getIdToken: async () => 'c-token' });
    currentResult.resolve({ uid: 'b', getIdToken: vi.fn(async () => 'b-token') });
    await tick();
    expect(await currentOutcome).toBe('stale-session');
    expect(auth.authenticatedUser()).toBeNull();
    http.expectNone(environment.apiBaseUrl + '/auth/me');
    oldResult.reject(new Error('denied'));
    expect(await oldOutcome).toBe('stale-session');
    await tick();
    expect(auth.sessionReady()).toBe(false);
    http.expectNone(environment.apiBaseUrl + '/auth/me');
    const c = { uid: 'c', getIdToken: async () => 'confirmed-c' };
    adapter.signInWithGoogle = async () => c;
    const confirmed = auth.signInWithGoogle();
    change(c);
    await tick();
    const request = http.expectOne(environment.apiBaseUrl + '/auth/me');
    expect(request.request.headers.get('Authorization')).toBe('Bearer confirmed-c');
    request.flush(me);
    await confirmed;
    expect(auth.authenticatedUser()).toBe(c);
  });
  it('devePreservarSomenteUltimaIntencao_quandoNuloInicialChegaComDoisLoginsPendentes', async () => {
    const first = deferred<IdentityUser>();
    const latest = deferred<IdentityUser>();
    adapter.signInWithEmailAndPassword = () => first.promise;
    adapter.signInWithGoogle = () => latest.promise;
    const oldOutcome = auth
      .signInWithEmailAndPassword('a@example.test', 'existing')
      .catch((error: Error) => error.message);
    const current = auth.signInWithGoogle();
    change(null);
    const b = { uid: 'b', getIdToken: async () => 'b-token' };
    change(b);
    latest.resolve(b);
    await tick();
    http.expectOne(environment.apiBaseUrl + '/auth/me').flush(me);
    await current;
    const snapshot = auth.capture();
    first.reject(new Error('old-denied'));
    expect(await oldOutcome).toBe('stale-session');
    expect(auth.isCurrent(snapshot)).toBe(true);
    expect(auth.authenticatedUser()).toBe(b);
  });
  it.each(['identity', 'logout', 'expiry', 'second-null'])(
    'deveRevogarIntencao_quandoNuloDepoisDe%s',
    async (prior) => {
      if (prior === 'identity') change(user);
      if (prior === 'logout') await auth.signOut();
      if (prior === 'expiry') auth.expire(auth.capture());
      if (prior === 'second-null') change(null);
      const result = deferred<IdentityUser>();
      adapter.signInWithGoogle = () => result.promise;
      const signing = auth.signInWithGoogle();
      const rejected = expect(signing).rejects.toThrow('stale-session');
      change(null);
      result.resolve({ uid: 'b', getIdToken: async () => 'b-token' });
      await rejected;
      await tick();
      expect(auth.authenticatedUser()).toBeNull();
      http.expectNone(environment.apiBaseUrl + '/auth/me');
    },
  );
  it.each(['password', 'google'])(
    'deveInvalidarAlphaSincronamente_quandoOutraAbaTrocaUIDAposLoginExplicito%s',
    async (method) => {
      token.resolve('login-token');
      const signing =
        method === 'password'
          ? auth.signInWithEmailAndPassword('pessoa@example.test', 'existing')
          : auth.signInWithGoogle();
      await tick();
      http.expectOne(environment.apiBaseUrl + '/auth/me').flush(me);
      await signing;
      const switching = auth.selectTenant('alpha');
      await tick();
      http.expectOne(environment.apiBaseUrl + '/auth/context').flush(context('alpha'));
      await switching;
      const snapshot = auth.capture();
      expect(buildMenu(auth)).toHaveLength(1);
      token = deferred<string>();
      let completed = false;
      client
        .post(environment.apiBaseUrl + '/demo-registros', { nome: 'Alpha' })
        .subscribe({ complete: () => (completed = true) });
      const nextToken = deferred<string>();
      const next = { uid: 'cross-tab-uid', getIdToken: () => nextToken.promise };
      change(next);
      expect(auth.isCurrent(snapshot)).toBe(false);
      expect(completed).toBe(true);
      expect(auth.authenticatedUser()).toBe(next);
      expect(auth.me()).toBeNull();
      expect(auth.currentTenant()).toBeNull();
      expect(auth.context()).toBeNull();
      expect(auth.effectiveActions()).toEqual([]);
      expect(buildMenu(auth)).toEqual([]);
      expect(auth.sessionReady()).toBe(false);
      token.resolve('old-alpha-token');
      await tick();
      http.expectNone(environment.apiBaseUrl + '/demo-registros');
      http.expectNone(environment.apiBaseUrl + '/auth/me');
      nextToken.resolve('new-identity-token');
      await tick();
      const request = http.expectOne(environment.apiBaseUrl + '/auth/me');
      expect(request.request.headers.get('Authorization')).toBe('Bearer new-identity-token');
      expect(request.request.headers.has('X-Tenant-Alias')).toBe(false);
      request.flush({ ...me, user: { ...me.user, id: 'new', name: 'Nova pessoa' } });
      await tick();
      expect(auth.username()).toBe('Nova pessoa');
      expect(auth.currentTenant()).toBeNull();
      expect(buildMenu(auth)).toEqual([]);
    },
  );
  it('deveSuspenderAlphaEReconciliarOutraAba_quandoLoginCanceladoAindaNaoRevelouSeuUID', async () => {
    const oldLogin = deferred<IdentityUser>();
    adapter.signInWithEmailAndPassword = () => oldLogin.promise;
    const old = auth.signInWithEmailAndPassword('old@example.test', 'existing');
    const rejected = expect(old).rejects.toThrow('stale-session');
    const active = { uid: 'active-uid', getIdToken: async () => 'active-token' };
    adapter.signInWithGoogle = async () => active;
    const signing = auth.signInWithGoogle();
    await tick();
    http.expectOne(environment.apiBaseUrl + '/auth/me').flush(me);
    await signing;
    const switching = auth.selectTenant('alpha');
    await tick();
    http.expectOne(environment.apiBaseUrl + '/auth/context').flush(context('alpha'));
    await switching;
    const snapshot = auth.capture();
    const requestToken = deferred<string>();
    active.getIdToken = () => requestToken.promise;
    client.post(environment.apiBaseUrl + '/demo-registros', { nome: 'Alpha' }).subscribe();
    const next = { uid: 'cross-tab-uid', getIdToken: async () => 'cross-tab-token' };
    change(next);
    expect(auth.isCurrent(snapshot)).toBe(false);
    expect(auth.authenticatedUser()).toBeNull();
    expect(auth.me()).toBeNull();
    expect(auth.currentTenant()).toBeNull();
    expect(buildMenu(auth)).toEqual([]);
    requestToken.resolve('old-active-token');
    await tick();
    http.expectNone(environment.apiBaseUrl + '/demo-registros');
    http.expectNone(environment.apiBaseUrl + '/auth/me');
    oldLogin.resolve(user);
    await rejected;
    await tick();
    const request = http.expectOne(environment.apiBaseUrl + '/auth/me');
    expect(request.request.headers.get('Authorization')).toBe('Bearer cross-tab-token');
    request.flush(me);
    await tick();
    expect(auth.authenticatedUser()).toBe(next);
    expect(auth.currentTenant()).toBeNull();
  });
  it('naoDeveNavegarOuAceitarContaAntiga_quandoOutraAbaTrocaUIDDuranteBootstrapDoLogin', async () => {
    token.resolve('old-token');
    const signing = auth.signInWithGoogle();
    const rejected = expect(signing).rejects.toThrow('stale-session');
    await tick();
    const oldMe = http.expectOne(environment.apiBaseUrl + '/auth/me');
    const next = { uid: 'new-tab', getIdToken: async () => 'next-token' };
    change(next);
    expect(oldMe.cancelled).toBe(true);
    expect(auth.me()).toBeNull();
    await rejected;
    await tick();
    http.expectOne(environment.apiBaseUrl + '/auth/me').flush(me);
    await tick();
    expect(auth.authenticatedUser()).toBe(next);
  });
  it('devePreservarAlpha_quandoObserverDoMesmoUIDChegaAposLoginExplicito', async () => {
    token.resolve('token');
    const signing = auth.signInWithGoogle();
    await tick();
    http.expectOne(environment.apiBaseUrl + '/auth/me').flush(me);
    await signing;
    const switching = auth.selectTenant('alpha');
    await tick();
    http.expectOne(environment.apiBaseUrl + '/auth/context').flush(context('alpha'));
    await switching;
    const snapshot = auth.capture();
    change({ uid: user.uid, getIdToken: async () => 'same-uid' });
    expect(auth.isCurrent(snapshot)).toBe(true);
    expect(auth.currentTenant()?.alias).toBe('alpha');
    await tick();
    http.expectNone(environment.apiBaseUrl + '/auth/me');
  });
  it('naoDeveReabrirLogin_quandoObserverNuloCancelaIntencaoAnonima', async () => {
    const login = deferred<IdentityUser>();
    adapter.signInWithGoogle = () => login.promise;
    const signing = auth.signInWithGoogle();
    const rejected = expect(signing).rejects.toThrow('stale-session');
    change(user);
    change(null);
    login.resolve(user);
    await rejected;
    change(user);
    await tick();
    expect(auth.authenticatedUser()).toBeNull();
    http.expectNone(environment.apiBaseUrl + '/auth/me');
  });
  it('naoDeveReabrirSessaoExpirada_quandoObserverRepeteUIDAposLoginExplicito', async () => {
    token.resolve('token');
    const signing = auth.signInWithGoogle();
    await tick();
    http.expectOne(environment.apiBaseUrl + '/auth/me').flush(me);
    await signing;
    auth.expire(auth.capture());
    change(user);
    await tick();
    expect(auth.sessionReady()).toBe(false);
    expect(auth.authenticatedUser()).toBeNull();
    http.expectNone(environment.apiBaseUrl + '/auth/me');
  });
  it('deveConfirmarUIDCanceladoSomentePorNovaIntencao_quandoObserverTardioRepeteUIDAntigo', async () => {
    const login = deferred<IdentityUser>();
    adapter.signInWithGoogle = () => login.promise;
    const signing = auth.signInWithGoogle();
    const rejected = expect(signing).rejects.toThrow('stale-session');
    await auth.signOut();
    login.resolve(user);
    await rejected;
    const next = { uid: 'active', getIdToken: async () => 'active-token' };
    adapter.signInWithGoogle = async () => next;
    const current = auth.signInWithGoogle();
    await tick();
    http.expectOne(environment.apiBaseUrl + '/auth/me').flush(me);
    await current;
    const snapshot = auth.capture();
    change(user);
    expect(auth.isCurrent(snapshot)).toBe(false);
    expect(auth.authenticatedUser()).toBeNull();
    expect(auth.sessionReady()).toBe(false);
    expect(auth.me()).toBeNull();
    token.resolve('confirmed');
    const confirmed = auth.signInWithEmailAndPassword('pessoa@example.test', 'existing');
    await tick();
    http.expectOne(environment.apiBaseUrl + '/auth/me').flush(me);
    await confirmed;
    change(user);
    await tick();
    expect(auth.authenticatedUser()).toBe(user);
    expect(auth.sessionReady()).toBe(true);
    http.expectNone(environment.apiBaseUrl + '/auth/me');
  });
  it.each(['current', 'cancelled'])(
    'naoDeveReabrirCallbackAmbiguo_quandoLogin%sRejeita',
    async (which) => {
      const login = deferred<IdentityUser>();
      adapter.signInWithEmailAndPassword = () => login.promise;
      const signing = auth.signInWithEmailAndPassword('old@example.test', 'existing');
      const rejected = expect(signing).rejects.toThrow(
        which === 'current' ? 'denied' : 'stale-session',
      );
      if (which === 'cancelled') {
        const current = auth.signInWithGoogle();
        token.resolve('valid');
        await tick();
        http.expectOne(environment.apiBaseUrl + '/auth/me').flush(me);
        await current;
      }
      const ambiguous = { uid: 'ambiguous', getIdToken: async () => 'ambiguous-token' };
      change(ambiguous);
      login.reject(new Error('denied'));
      await rejected;
      change(ambiguous);
      await tick();
      expect(auth.authenticatedUser()).toBeNull();
      expect(auth.sessionReady()).toBe(false);
      http.expectNone(environment.apiBaseUrl + '/auth/me');
    },
  );
  it('naoDeveReconciliarUsuarioBufferizado_quandoNuloChegaAntesDeLoginCanceladoResolver', async () => {
    const login = deferred<IdentityUser>();
    adapter.signInWithEmailAndPassword = () => login.promise;
    const signing = auth.signInWithEmailAndPassword('old@example.test', 'existing');
    const rejected = expect(signing).rejects.toThrow('stale-session');
    const current = auth.signInWithGoogle();
    token.resolve('valid');
    await tick();
    http.expectOne(environment.apiBaseUrl + '/auth/me').flush(me);
    await current;
    change({ uid: 'cross-tab', getIdToken: async () => 'cross-tab' });
    change(null);
    login.resolve({ uid: 'cancelled', getIdToken: async () => 'old' });
    await rejected;
    await tick();
    expect(auth.authenticatedUser()).toBeNull();
    http.expectNone(environment.apiBaseUrl + '/auth/me');
  });
  it.each(['uid-1', 'old-other'])(
    'naoDeveAceitarDuasIntencoesAntigas_quandoTerceiraConfirmaAntesComUID%s',
    async (uid) => {
      const first = deferred<IdentityUser>();
      const second = deferred<IdentityUser>();
      adapter.signInWithEmailAndPassword = () => first.promise;
      const oldFirst = auth.signInWithEmailAndPassword('old@example.test', 'existing');
      const rejectedFirst = expect(oldFirst).rejects.toThrow('stale-session');
      adapter.signInWithGoogle = () => second.promise;
      const oldSecond = auth.signInWithGoogle();
      const rejectedSecond = expect(oldSecond).rejects.toThrow('stale-session');
      token.resolve('current-token');
      adapter.signInWithGoogle = async () => user;
      const latest = auth.signInWithGoogle();
      await tick();
      http.expectOne(environment.apiBaseUrl + '/auth/me').flush(me);
      await latest;
      const stale = { uid, getIdToken: async () => 'old-token' };
      change(stale);
      second.resolve(stale);
      await rejectedSecond;
      change(stale);
      first.resolve(stale);
      await rejectedFirst;
      change(stale);
      await tick();
      expect(auth.authenticatedUser()).toBe(uid === user.uid ? user : null);
      expect(auth.sessionReady()).toBe(uid === user.uid);
      http.expectNone(environment.apiBaseUrl + '/auth/me');
      const next = { uid: 'legitimate-new', getIdToken: async () => 'new-token' };
      change(next);
      await tick();
      http.expectOne(environment.apiBaseUrl + '/auth/me').flush(me);
      await tick();
      expect(auth.authenticatedUser()).toBe(next);
      expect(auth.currentTenant()).toBeNull();
    },
  );
  it('naoDeveReabrirSessao_quandoObserverNativoChegaAntesDeLoginAntigoResolverAposLogout', async () => {
    const login = deferred<IdentityUser>();
    adapter.signInWithEmailAndPassword = () => login.promise;
    const signing = auth.signInWithEmailAndPassword('old@example.test', 'existing');
    const rejected = expect(signing).rejects.toThrow('stale-session');
    await auth.signOut();
    const snapshot = auth.capture();
    token.resolve('old');
    change(user);
    await tick();
    expect(auth.isCurrent(snapshot)).toBe(true);
    expect(auth.authenticatedUser()).toBeNull();
    expect(auth.me()).toBeNull();
    expect(auth.context()).toBeNull();
    expect(auth.sessionReady()).toBe(false);
    http.expectNone(environment.apiBaseUrl + '/auth/me');
    login.resolve(user);
    await rejected;
    change(user);
    await tick();
    expect(auth.isCurrent(snapshot)).toBe(true);
    http.expectNone(environment.apiBaseUrl + '/auth/me');
  });
  it.each(['uid-2', 'uid-1'])(
    'naoDeveTrocarSessaoNova_quandoLoginAntigoObservaEResolveDepoisDeNovaIntencaoComUID%s',
    async (uid) => {
      const oldLogin = deferred<IdentityUser>();
      adapter.signInWithEmailAndPassword = () => oldLogin.promise;
      const old = auth.signInWithEmailAndPassword('old@example.test', 'existing');
      const rejected = expect(old).rejects.toThrow('stale-session');
      const next = { uid, getIdToken: async () => 'next-token' };
      adapter.signInWithGoogle = async () => next;
      const current = auth.signInWithGoogle();
      await tick();
      http.expectOne(environment.apiBaseUrl + '/auth/me').flush(me);
      await current;
      const switching = auth.selectTenant('beta');
      await tick();
      http.expectOne(environment.apiBaseUrl + '/auth/context').flush(context('beta'));
      await switching;
      const snapshot = auth.capture();
      token.resolve('old-token');
      change(user);
      await tick();
      expect(auth.isCurrent(snapshot)).toBe(uid === user.uid);
      expect(auth.authenticatedUser()).toBe(uid === user.uid ? next : null);
      expect(auth.currentTenant()?.alias).toBe(uid === user.uid ? 'beta' : undefined);
      http.expectNone(environment.apiBaseUrl + '/auth/me');
      oldLogin.resolve(user);
      await rejected;
      change(user);
      await tick();
      expect(auth.isCurrent(snapshot)).toBe(uid === user.uid);
      expect(auth.currentTenant()?.alias).toBe(uid === user.uid ? 'beta' : undefined);
      expect(auth.sessionReady()).toBe(uid === user.uid);
      http.expectNone(environment.apiBaseUrl + '/auth/me');
    },
  );
  it('deveEsperarResultadoDaIntencao_quandoObserverNativoChegaAntesDeLoginValidoResolver', async () => {
    const login = deferred<IdentityUser>();
    adapter.signInWithGoogle = () => login.promise;
    const signing = auth.signInWithGoogle();
    token.resolve('valid-token');
    change(user);
    await tick();
    expect(auth.sessionReady()).toBe(false);
    http.expectNone(environment.apiBaseUrl + '/auth/me');
    login.resolve(user);
    await tick();
    http.expectOne(environment.apiBaseUrl + '/auth/me').flush(me);
    await signing;
    expect(auth.authenticatedUser()).toBe(user);
    expect(auth.sessionReady()).toBe(true);
  });
  it('deveRestaurarSessaoInicial_quandoObserverNativoTemUsuarioValidoSemIntencaoLocal', async () => {
    await ready();
    expect(auth.authenticatedUser()).toBe(user);
    expect(auth.currentTenant()?.alias).toBe('alpha');
    expect(auth.sessionReady()).toBe(true);
  });
  it('deveCancelarValidacaoAnterior_quandoNovaIntencaoComecaDuranteRestauracaoInicial', async () => {
    change(user);
    token.resolve('restored-token');
    await tick();
    const old = http.expectOne(environment.apiBaseUrl + '/auth/me');
    const login = deferred<IdentityUser>();
    adapter.signInWithGoogle = () => login.promise;
    const signing = auth.signInWithGoogle();
    expect(old.cancelled).toBe(true);
    expect(auth.me()).toBeNull();
    const next = { uid: 'next-uid', getIdToken: async () => 'next-token' };
    login.resolve(next);
    await tick();
    http.expectOne(environment.apiBaseUrl + '/auth/me').flush(me);
    await signing;
    expect(auth.authenticatedUser()).toBe(next);
  });
  it('deveReconhecerSnapshotAnonimo_quandoRecuperacaoOcorreAntesDeLogin', async () => {
    const snapshot = auth.capture();
    expect(auth.isCurrent(snapshot)).toBe(true);
    await auth.resetPassword('pessoa@example.test');
    expect(auth.isCurrent(snapshot)).toBe(true);
    http.expectNone(environment.apiBaseUrl + '/auth/me');
  });
  it('naoDeveEnviarPayloadAlpha_quandoTokenResolveDepoisDeBeta', async () => {
    await ready();
    token = deferred<string>();
    let completed = false;
    client
      .post(environment.apiBaseUrl + '/demo-registros', { id: 'same-pk', nome: 'Alpha' })
      .subscribe({ complete: () => (completed = true) });
    const switching = auth.selectTenant('beta');
    token.resolve('token-two');
    await tick();
    http.expectNone((req) => req.url.endsWith('/demo-registros'));
    const req = http.expectOne(environment.apiBaseUrl + '/auth/context');
    expect(req.request.headers.get('X-Tenant-Alias')).toBe('beta');
    req.flush(context('beta'));
    await switching;
    expect(completed).toBe(true);
    expect(auth.currentTenant()?.alias).toBe('beta');
  });
  it('naoDeveAceitarResponseAlpha_quandoBetaUsaMesmaChave', async () => {
    await ready();
    let value = 'empty';
    client
      .get(environment.apiBaseUrl + '/demo-registros/same-pk')
      .subscribe((v) => (value = String(v)));
    await tick();
    const old = http.expectOne(environment.apiBaseUrl + '/demo-registros/same-pk');
    const switching = auth.selectTenant('beta');
    await tick();
    expect(old.cancelled).toBe(true);
    http.expectOne(environment.apiBaseUrl + '/auth/context').flush(context('beta'));
    await switching;
    expect(value).toBe('empty');
  });
  it('naoDeveEnviarToken_quandoLogoutDuranteAwait', async () => {
    await ready();
    token = deferred<string>();
    client.get(environment.apiBaseUrl + '/demo-registros/1').subscribe();
    await auth.signOut();
    token.resolve('old');
    await tick();
    http.expectNone(environment.apiBaseUrl + '/demo-registros/1');
    expect(auth.effectiveActions()).toEqual([]);
    expect(auth.me()).toBeNull();
  });
  it('naoDeveManterAcoes_quandoSwitchFalha', async () => {
    await ready();
    const switching = auth.selectTenant('beta');
    await tick();
    http
      .expectOne(environment.apiBaseUrl + '/auth/context')
      .flush({}, { status: 403, statusText: 'Forbidden' });
    await expect(switching).rejects.toThrow();
    expect(auth.currentTenant()).toBeNull();
    expect(auth.effectiveActions()).toEqual([]);
  });
  it('naoDeveReusarToken_quandoOutroUIDEntra', async () => {
    await ready();
    token = deferred<string>();
    client.post(environment.apiBaseUrl + '/demo-registros', { nome: 'Alpha' }).subscribe();
    const nextToken = deferred<string>();
    change({ uid: 'uid-2', getIdToken: () => nextToken.promise });
    token.resolve('old');
    nextToken.resolve('new');
    await tick();
    http.expectNone(environment.apiBaseUrl + '/demo-registros');
    const request = http.expectOne(environment.apiBaseUrl + '/auth/me');
    expect(request.request.headers.get('Authorization')).toBe('Bearer new');
    request.flush(me);
    await tick();
    expect(auth.currentTenant()).toBeNull();
  });
  it('naoDeveVazarBearer_quandoOrigemOuBoundaryNaoCoincidem', async () => {
    await ready();
    for (const url of [
      'https://outside.test/api/demo-registros',
      environment.apiBaseUrl.replace('/api', '/api-evil') + '/demo-registros',
      environment.apiBaseUrl.replace('localhost', 'localhost.evil') + '/demo-registros',
    ]) {
      client.get(url).subscribe();
      const req = http.expectOne(url);
      expect(req.request.headers.has('Authorization')).toBe(false);
      expect(req.request.headers.has('X-Tenant-Alias')).toBe(false);
      req.flush({});
    }
  });
  it('naoDeveLiberarEscopoTenant_quandoApenasAcaoGlobal', async () => {
    await ready();
    expect(auth.isAuthorized([Acoes.CONSULTAR_PROPRIA_CONTA], 'tenant')).toBe(false);
    expect(auth.isAuthorized([], 'global')).toBe(false);
    expect(auth.isAuthorized([Acoes.CONSULTAR_DEMO, Acoes.EXCLUIR_DEMO], 'tenant', 'all')).toBe(
      false,
    );
    expect(auth.isAuthorized([Acoes.CONSULTAR_DEMO, Acoes.EXCLUIR_DEMO], 'tenant', 'any')).toBe(
      true,
    );
  });
  it('naoDeveAlterarPayloadCapturado_quandoCallerMutaDuranteTokenAwait', async () => {
    await ready();
    token = deferred<string>();
    const body = { nome: 'Alpha' };
    client.post(environment.apiBaseUrl + '/demo-registros', body).subscribe();
    body.nome = 'Outra empresa';
    token.resolve('fresh');
    await tick();
    const request = http.expectOne(environment.apiBaseUrl + '/demo-registros');
    expect(request.request.body).toEqual({ nome: 'Alpha' });
    expect(request.request.headers.get('X-Tenant-Alias')).toBe('alpha');
    request.flush({});
  });
  it('naoDeveManterSessao_quandoTokenExpira', async () => {
    await ready();
    token = deferred<string>();
    client.get(environment.apiBaseUrl + '/demo-registros/1').subscribe({ error: () => {} });
    token.reject(new Error('expired'));
    await tick();
    expect(auth.me()).toBeNull();
    expect(auth.currentTenant()).toBeNull();
    http.expectNone(environment.apiBaseUrl + '/demo-registros/1');
  });
  it('deveMostrarSomenteBeta_quandoMesmaPKEResponseAlphaAntigaChegaOuECancelada', async () => {
    await ready();
    const url = environment.apiBaseUrl + '/demo-registros/same-pk';
    const visible: string[] = [];
    client.get<{ nome: string }>(url).subscribe((item) => visible.push(item.nome));
    client.put(url, { id: 'same-pk', nome: 'Alpha editado' }).subscribe();
    await tick();
    const alphaRead = http.expectOne((req) => req.method === 'GET' && req.url === url);
    const alphaWrite = http.expectOne((req) => req.method === 'PUT' && req.url === url);
    expect(alphaRead.request.headers.get('Authorization')).toBe('Bearer token-one');
    expect(alphaRead.request.headers.get('X-Tenant-Alias')).toBe('alpha');
    expect(alphaWrite.request.body).toEqual({ id: 'same-pk', nome: 'Alpha editado' });
    const switching = auth.selectTenant('beta');
    await tick();
    expect(alphaRead.cancelled).toBe(true);
    expect(alphaWrite.cancelled).toBe(true);
    http.expectOne(environment.apiBaseUrl + '/auth/context').flush(context('beta'));
    await switching;
    client.get<{ nome: string }>(url).subscribe((item) => visible.push(item.nome));
    await tick();
    const betaRead = http.expectOne((req) => req.method === 'GET' && req.url === url);
    expect(betaRead.request.headers.get('Authorization')).toBe('Bearer token-one');
    expect(betaRead.request.headers.get('X-Tenant-Alias')).toBe('beta');
    betaRead.flush({ id: 'same-pk', nome: 'Beta' });
    expect(() => alphaRead.flush({ id: 'same-pk', nome: 'Alpha' })).toThrow();
    expect(visible).toEqual(['Beta']);
    client.put(url, { id: 'same-pk', nome: 'Beta editado' }).subscribe();
    await tick();
    const betaWrite = http.expectOne((req) => req.method === 'PUT' && req.url === url);
    expect(betaWrite.request.body).toEqual({ id: 'same-pk', nome: 'Beta editado' });
    expect(betaWrite.request.headers.get('X-Tenant-Alias')).toBe('beta');
    betaWrite.flush({});
    expect(visible).toEqual(['Beta']);
  });
  it.each([
    'https://outside.test/api/demo-registros',
    environment.apiBaseUrl.replace('/api', '/api-evil') + '/demo-registros',
    environment.apiBaseUrl.replace('localhost', 'localhost.evil') + '/demo-registros',
  ])(
    'naoDeveInjetarSessaoNemAlias_quandoDestinoNaoControladoRecebeHeadersDoChamador',
    async (url) => {
      await ready();
      client
        .get(url, { headers: { Authorization: 'Bearer terceiro', 'X-Tenant-Alias': 'terceiro' } })
        .subscribe();
      await tick();
      const request = http.expectOne(url);
      expect(request.request.headers.get('Authorization')).not.toBe('Bearer token-one');
      expect(request.request.headers.get('X-Tenant-Alias')).not.toBe('alpha');
      expect(request.request.headers.has('Authorization')).toBe(false);
      expect(request.request.headers.has('X-Tenant-Alias')).toBe(false);
      request.flush({});
      expect(auth.sessionReady()).toBe(true);
    },
  );
  it('naoDeveAceitarHeadersDoChamador_quandoDestinoDaApiUsaSnapshotDaSessao', async () => {
    await ready();
    const forged = { Authorization: 'Bearer forjado', 'X-Tenant-Alias': 'beta' };
    client.get(environment.apiBaseUrl + '/demo-registros/1', { headers: forged }).subscribe();
    client.get(environment.apiBaseUrl + '/auth/me', { headers: forged }).subscribe();
    await tick();
    const business = http.expectOne(environment.apiBaseUrl + '/demo-registros/1');
    expect(business.request.headers.get('Authorization')).toBe('Bearer token-one');
    expect(business.request.headers.get('X-Tenant-Alias')).toBe('alpha');
    const account = http.expectOne(environment.apiBaseUrl + '/auth/me');
    expect(account.request.headers.get('Authorization')).toBe('Bearer token-one');
    expect(account.request.headers.has('X-Tenant-Alias')).toBe(false);
    business.flush({});
    account.flush(me);
  });
  it('deveLimparSessao_quando401ChegaNaGeracaoAtual', async () => {
    await ready();
    let delivered = false;
    client.get(environment.apiBaseUrl + '/demo-registros/1').subscribe({
      next: () => (delivered = true),
      error: () => {},
    });
    await tick();
    http
      .expectOne(environment.apiBaseUrl + '/demo-registros/1')
      .flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(delivered).toBe(false);
    expect(auth.authenticatedUser()).toBeNull();
    expect(auth.me()).toBeNull();
    expect(auth.currentTenant()).toBeNull();
    expect(auth.effectiveActions()).toEqual([]);
    expect(auth.sessionReady()).toBe(false);
  });
  it.each([403, 500])(
    'naoDeveExpirarSessao_quandoErroHttp%sChegaNaGeracaoAtual',
    async (status) => {
      await ready();
      let failure: HttpErrorResponse | undefined;
      client
        .get(environment.apiBaseUrl + '/demo-registros/1')
        .subscribe({ error: (error: HttpErrorResponse) => (failure = error) });
      await tick();
      http
        .expectOne(environment.apiBaseUrl + '/demo-registros/1')
        .flush({}, { status, statusText: 'Erro' });
      expect(failure?.status).toBe(status);
      expect(auth.authenticatedUser()).toBe(user);
      expect(auth.currentTenant()?.alias).toBe('alpha');
      expect(auth.sessionReady()).toBe(true);
    },
  );
  it.each(['token', 'http-401'])(
    'naoDeveExpirarSessaoNova_quandoRejeicaoAntigaChegaDepoisDaSessaoPronta_%s',
    async (origin) => {
      await ready();
      if (origin === 'token') token = deferred<string>();
      let oldError: unknown;
      client
        .post(environment.apiBaseUrl + '/demo-registros', { nome: 'Alpha' })
        .subscribe({ error: (error: unknown) => (oldError = error) });
      await tick();
      const oldRequest =
        origin === 'http-401'
          ? http.expectOne(environment.apiBaseUrl + '/demo-registros')
          : undefined;
      const next = { uid: 'uid-2', getIdToken: async () => 'token-two' };
      change(next);
      await tick();
      http.expectOne(environment.apiBaseUrl + '/auth/me').flush(me);
      await tick();
      const switching = auth.selectTenant('beta');
      await tick();
      http.expectOne(environment.apiBaseUrl + '/auth/context').flush(context('beta'));
      await switching;
      const account = auth.me();
      expect(auth.sessionReady()).toBe(true);
      if (oldRequest) {
        expect(oldRequest.cancelled).toBe(true);
        expect(() => oldRequest.flush({}, { status: 401, statusText: 'Unauthorized' })).toThrow();
      } else token.reject(new Error('expired'));
      await tick();
      expect(oldError).toBeUndefined();
      expect(auth.authenticatedUser()).toBe(next);
      expect(auth.sessionReady()).toBe(true);
      expect(auth.me()).toBe(account);
      expect(auth.currentTenant()?.alias).toBe('beta');
      expect(auth.effectiveActions()).toEqual(context('beta').actions);
      http.expectNone(environment.apiBaseUrl + '/demo-registros');
    },
  );
  it.each(['anonimo', 'tenant-pronto'])(
    'deveChamarDemoPublicoSemHeadersSensiveis_quando%s',
    async (state) => {
      if (state === 'tenant-pronto') await ready();
      let body: unknown;
      client
        .get(environment.apiBaseUrl + '/demo/public', {
          headers: { Authorization: 'Bearer chamador', 'X-Tenant-Alias': 'chamador' },
        })
        .subscribe((value) => (body = value));
      await tick();
      const request = http.expectOne(environment.apiBaseUrl + '/demo/public');
      expect(request.request.headers.has('Authorization')).toBe(false);
      expect(request.request.headers.has('X-Tenant-Alias')).toBe(false);
      request.flush({ classificacao: 'publico' });
      expect(body).toEqual({ classificacao: 'publico' });
    },
  );
  it.each(['sem-tenant', 'com-tenant'])(
    'deveChamarDemoGlobalSemAlias_quandoSessaoPronta%s',
    async (state) => {
      if (state === 'com-tenant') await ready();
      else await readyWithoutTenant();
      let body: unknown;
      client
        .get(environment.apiBaseUrl + '/demo/global', { headers: { 'X-Tenant-Alias': 'chamador' } })
        .subscribe((value) => (body = value));
      await tick();
      const request = http.expectOne(environment.apiBaseUrl + '/demo/global');
      expect(request.request.headers.get('Authorization')).toBe('Bearer token-one');
      expect(request.request.headers.has('X-Tenant-Alias')).toBe(false);
      request.flush({ escopo: 'global' });
      expect(body).toEqual({ escopo: 'global' });
    },
  );
  it('naoDeveEnviarDemoGlobal_quandoAnonimo', async () => {
    let failure: Error | undefined;
    client
      .get(environment.apiBaseUrl + '/demo/global')
      .subscribe({ error: (error: Error) => (failure = error) });
    await tick();
    expect(failure?.message).toBe('context-required');
    http.expectNone(environment.apiBaseUrl + '/demo/global');
  });
  it.each([
    ['GET', '/demo/tenant/read'],
    ['POST', '/demo/tenant/write'],
  ])('naoDeveEnviarDemoTenant_quandoSemContexto%s', async (method, path) => {
    await readyWithoutTenant();
    let failure: Error | undefined;
    client
      .request(method, environment.apiBaseUrl + path, { body: method === 'POST' ? {} : undefined })
      .subscribe({ error: (error: Error) => (failure = error) });
    await tick();
    expect(failure?.message).toBe('context-required');
    http.expectNone(environment.apiBaseUrl + path);
  });
  it.each([
    ['GET', '/demo/tenant/read'],
    ['POST', '/demo/tenant/write'],
  ])('deveUsarSnapshotDoTenant_quandoDemoTenant%sTemContexto', async (method, path) => {
    await ready();
    client
      .request(method, environment.apiBaseUrl + path, {
        body: method === 'POST' ? { nome: 'Alpha' } : undefined,
        headers: { 'X-Tenant-Alias': 'beta' },
      })
      .subscribe();
    await tick();
    const request = http.expectOne(environment.apiBaseUrl + path);
    expect(request.request.headers.get('Authorization')).toBe('Bearer token-one');
    expect(request.request.headers.get('X-Tenant-Alias')).toBe('alpha');
    if (method === 'POST') expect(request.request.body).toEqual({ nome: 'Alpha' });
    request.flush({});
  });
  it('naoDeveEnviarEscritaAlphaParaBeta_quandoTokenDoDemoTenantResolveDepoisDaTroca', async () => {
    await ready();
    token = deferred<string>();
    client
      .post(environment.apiBaseUrl + '/demo/tenant/write', { nome: 'Alpha' })
      .subscribe({ error: () => {} });
    const switching = auth.selectTenant('beta');
    token.resolve('token-two');
    await tick();
    http.expectNone(environment.apiBaseUrl + '/demo/tenant/write');
    http.expectOne(environment.apiBaseUrl + '/auth/context').flush(context('beta'));
    await switching;
    client.post(environment.apiBaseUrl + '/demo/tenant/write', { nome: 'Beta' }).subscribe();
    await tick();
    const request = http.expectOne(environment.apiBaseUrl + '/demo/tenant/write');
    expect(request.request.body).toEqual({ nome: 'Beta' });
    expect(request.request.headers.get('X-Tenant-Alias')).toBe('beta');
    request.flush({});
  });
  it('deveAnunciarFalhaESemTenant_quandoAliasNaoAutorizado', async () => {
    await ready();
    await expect(auth.selectTenant('gamma')).rejects.toThrow();
    expect(auth.error()).toBe('Não foi possível acessar esta empresa. Escolha novamente.');
    expect(auth.currentTenant()).toBeNull();
    expect(auth.effectiveActions()).toEqual([]);
    expect(auth.loading()).toBe(false);
    http.expectNone(environment.apiBaseUrl + '/auth/context');
  });
  it.each(['negacao-antes', 'pendente-antes'])(
    'deveRecuperarLoadingEAnunciarErro_quandoSelecaoNegadaChegaComSelecaoPendente_%s',
    async (order) => {
      await ready();
      const pending = auth.selectTenant('beta');
      await tick();
      const pendingContext = http.expectOne(environment.apiBaseUrl + '/auth/context');
      expect(auth.loading()).toBe(true);
      const denied = auth.selectTenant('gamma').then(
        () => 'aceita',
        (error: Error) => error.message,
      );
      expect(auth.loading()).toBe(false);
      expect(pendingContext.cancelled).toBe(true);
      if (order === 'pendente-antes') await pending;
      expect(await denied).toBe('tenant-denied');
      await pending;
      await tick();
      expect(auth.loading()).toBe(false);
      expect(auth.error()).toBe('Não foi possível acessar esta empresa. Escolha novamente.');
      expect(auth.currentTenant()).toBeNull();
      expect(auth.context()).toBeNull();
      expect(auth.effectiveActions()).toEqual([]);
      http.expectNone(environment.apiBaseUrl + '/auth/context');
      const retry = auth.selectTenant('alpha');
      await tick();
      http.expectOne(environment.apiBaseUrl + '/auth/context').flush(context('alpha'));
      await retry;
      expect(auth.currentTenant()?.alias).toBe('alpha');
      expect(auth.loading()).toBe(false);
      expect(auth.error()).toBe('');
    },
  );
});

describe('base da API / normalização única', () => {
  const original = environment.apiBaseUrl;
  afterEach(() => {
    environment.apiBaseUrl = original;
  });
  it.each([
    ['http://localhost:8080/api', 'http://localhost:8080'],
    ['https://api.example.test/api/', 'https://api.example.test'],
    ['https://api.example.test/prefixo/api', 'https://api.example.test/prefixo'],
    ['/api', ''],
    ['/api/', ''],
    ['/api//', ''],
    ['/prefixo/api', '/prefixo'],
  ])('deveNormalizarBarrasFinais_quandoBase%s', (base, root) => {
    expect(gemsApiRoot(base)).toBe(root);
  });
  it.each([
    '/gateway',
    'https://api.example.test/gateway',
    'https://api.example.test',
    '/api-evil',
    '/apix/api/x',
    '',
    'https://api.example.test/api?x=1',
    'https://api.example.test/api#h',
    '//api',
  ])('deveRejeitarBase_quandoFormatoNaoTerminaEmApi%s', (base) => {
    expect(() => gemsApiRoot(base)).toThrow('API_BASE_URL inválida');
  });
  it('deveReconhecerSomenteOrigemExataEBoundary_quandoOrigemParecida', () => {
    const base = 'https://api.example.test/prefixo/api/';
    expect(isApiDestination('https://api.example.test/prefixo/api/auth/me', base)).toBe(true);
    expect(isApiDestination('https://api.example.test/prefixo/api', base)).toBe(true);
    for (const outside of [
      'https://api.example.test.evil/prefixo/api/auth/me',
      'https://api.example.test:8443/prefixo/api/auth/me',
      'http://api.example.test/prefixo/api/auth/me',
      'https://api.example.test/prefixo/api-evil/auth/me',
      'https://api.example.test/api/auth/me',
    ])
      expect(isApiDestination(outside, base)).toBe(false);
    expect(isApiDestination('https://api.example.test/prefixo/api/x', '/gateway')).toBe(false);
  });
  it.each([
    'https://api.example.test/api',
    'https://api.example.test/api/',
    'https://api.example.test/api//',
    'https://api.example.test/prefixo/api/',
  ])('deveClassificarMeComoStoreMonta_quandoBase%s', async (base) => {
    environment.apiBaseUrl = base;
    const normalized = base.replace(/\/+$/, '');
    let change!: (user: IdentityUser | null) => void;
    TestBed.configureTestingModule({
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
          },
        },
      ],
    });
    TestBed.inject(AuthService);
    const http = TestBed.inject(HttpTestingController);
    change({ uid: 'uid-1', getIdToken: async () => 'token-one' });
    for (let i = 0; i < 8; i++) await Promise.resolve();
    const request = http.expectOne(normalized + '/auth/me');
    expect(request.request.headers.get('Authorization')).toBe('Bearer token-one');
    expect(request.request.headers.has('X-Tenant-Alias')).toBe(false);
    request.flush({
      user: { id: '1', name: 'Pessoa', email: 'pessoa@example.test' },
      tenants: [],
      globalActions: [],
    });
    http.verify();
  });
  it('deveManterUrlsDaApiSemDuplicarApi_quandoStoresUsamApiComoPrefixo', () => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideGemsHttp(gemsApiRoot())],
    });
    TestBed.inject(AuthStore).getMe().subscribe();
    TestBed.inject(AuthStore).getContext().subscribe();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne(environment.apiBaseUrl + '/auth/me').flush({});
    http.expectOne(environment.apiBaseUrl + '/auth/context').flush({});
    http.verify();
  });
});
