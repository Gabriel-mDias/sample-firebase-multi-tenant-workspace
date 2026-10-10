import { TestBed } from '@angular/core/testing';
import { FirebaseWebAdapter } from './firebase-web.adapter';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';
import { AuthStore } from './auth.store';
import { IDENTITY_ADAPTER, IdentityUser } from './identity.adapter';
import { of } from 'rxjs';

const native = vi.hoisted(() => ({
  getAuth: vi.fn(() => ({ name: 'native-auth' })),
  connectAuthEmulator: vi.fn(),
  signInWithEmailAndPassword: vi.fn(async () => ({
    user: { uid: 'native-uid', getIdToken: async () => 'native-token' },
  })),
  signInWithPopup: vi.fn(async () => ({
    user: { uid: 'google-uid', getIdToken: async () => 'native-token' },
  })),
  sendPasswordResetEmail: vi.fn(async () => {}),
  signOut: vi.fn(async () => {}),
  params: vi.fn(),
  observe: vi.fn(),
}));
vi.mock('firebase/app', () => ({ initializeApp: () => ({ name: 'sample' }), getApps: () => [] }));
vi.mock('firebase/auth', () => ({
  ...native,
  onAuthStateChanged: native.observe,
  GoogleAuthProvider: class {
    setCustomParameters = native.params;
  },
}));

describe('FirebaseWebAdapter / APIs nativas', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    native.observe.mockReturnValue(() => {});
  });
  it.each(['password', 'google'])(
    'deveSincronizarNovoUID_quandoObserverNativoEmiteAposLogin%s',
    async (method) => {
      let observe!: (user: IdentityUser | null) => void;
      native.observe.mockImplementation((_auth, callback) => {
        observe = callback;
        return () => {};
      });
      const getMe = vi.fn(() =>
        of({
          user: { id: '1', name: 'Pessoa', email: 'pessoa@example.test' },
          tenants: [],
          globalActions: [],
        }),
      );
      TestBed.configureTestingModule({
        providers: [
          { provide: IDENTITY_ADAPTER, useClass: FirebaseWebAdapter },
          { provide: AuthStore, useValue: { getMe } },
        ],
      });
      const auth = TestBed.inject(AuthService);
      if (method === 'password')
        await auth.signInWithEmailAndPassword('pessoa@example.test', 'existing');
      else await auth.signInWithGoogle();
      const snapshot = auth.capture();
      const next = { uid: 'cross-tab', getIdToken: async () => 'new-token' };
      observe(next);
      expect(auth.isCurrent(snapshot)).toBe(false);
      expect(auth.authenticatedUser()).toBe(next);
      expect(auth.me()).toBeNull();
      expect(auth.sessionReady()).toBe(false);
      await Promise.resolve();
      expect(getMe).toHaveBeenCalledTimes(2);
      expect(auth.sessionReady()).toBe(true);
    },
  );
  it('naoDeveRestaurarSessaoCancelada_quandoFirebaseNativoObservaAntesDeResolverSignIn', async () => {
    let observe!: (user: IdentityUser | null) => void;
    let complete!: (credential: { user: IdentityUser }) => void;
    native.observe.mockImplementation((_auth, callback) => {
      observe = callback;
      return () => {};
    });
    native.signInWithEmailAndPassword.mockReturnValueOnce(
      new Promise((resolve) => {
        complete = resolve;
      }),
    );
    const getMe = vi.fn(() =>
      of({
        user: { id: '1', name: 'Pessoa', email: 'pessoa@example.test' },
        tenants: [],
        globalActions: [],
      }),
    );
    TestBed.configureTestingModule({
      providers: [
        { provide: IDENTITY_ADAPTER, useClass: FirebaseWebAdapter },
        { provide: AuthStore, useValue: { getMe } },
      ],
    });
    const auth = TestBed.inject(AuthService);
    const user = { uid: 'native-uid', getIdToken: async () => 'native-token' };
    const signing = auth.signInWithEmailAndPassword('pessoa@example.test', 'existing');
    const rejected = expect(signing).rejects.toThrow('stale-session');
    await auth.signOut();
    const snapshot = auth.capture();
    observe(user);
    expect(getMe).not.toHaveBeenCalled();
    expect(auth.isCurrent(snapshot)).toBe(true);
    complete({ user });
    await rejected;
    expect(auth.authenticatedUser()).toBeNull();
    expect(getMe).not.toHaveBeenCalled();
  });
  it('deveUsarProviderENativeReset_quandoSolicitadoPelaFachada', async () => {
    const adapter = TestBed.inject(FirebaseWebAdapter);
    expect((await adapter.signInWithGoogle()).uid).toBe('google-uid');
    expect(native.signInWithPopup).toHaveBeenCalledTimes(1);
    expect(native.params).toHaveBeenCalledWith({ prompt: 'select_account' });
    await adapter.signInWithEmailAndPassword('pessoa@example.test', 'existing');
    expect(native.signInWithEmailAndPassword).toHaveBeenCalledWith(
      { name: 'native-auth' },
      'pessoa@example.test',
      'existing',
    );
    await adapter.resetPassword('pessoa@example.test');
    expect(native.sendPasswordResetEmail).toHaveBeenCalledWith(
      { name: 'native-auth' },
      'pessoa@example.test',
    );
    await adapter.signOut();
    expect(native.signOut).toHaveBeenCalledTimes(1);
  });
  it('naoDeveInicializarProvider_quandoConfiguracaoEmulatorInvalidaMesmoNoRetry', async () => {
    const original = environment.production;
    try {
      environment.production = true;
      const adapter = TestBed.inject(FirebaseWebAdapter);
      for (let i = 0; i < 2; i++)
        await expect(adapter.signInWithGoogle()).rejects.toThrow('invalid-emulator-config');
      expect(native.getAuth).not.toHaveBeenCalled();
      expect(native.signInWithPopup).not.toHaveBeenCalled();
    } finally {
      environment.production = original;
    }
  });
});
