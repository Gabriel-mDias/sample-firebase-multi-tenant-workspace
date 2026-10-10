import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  provideRouter,
  Router,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';
import { Subject } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { AuthStore } from '../auth/auth.store';
import { AuthMe } from '../auth/auth.model';
import { IDENTITY_ADAPTER, IdentityUser } from '../auth/identity.adapter';
import { authGuard } from './auth.guard';

describe('authGuard / fachada real', () => {
  let auth: AuthService;
  let change: (user: IdentityUser | null) => void;
  let account: Subject<AuthMe>;
  const user = { uid: 'uid-1', getIdToken: async () => 'token' };
  const me: AuthMe = {
    user: { id: '1', name: 'Pessoa', email: 'pessoa@example.test' },
    tenants: [],
    globalActions: [],
  };
  const tick = async () => {
    for (let i = 0; i < 8; i++) await Promise.resolve();
  };
  const run = (url: string) =>
    TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot),
    );
  const serialize = (result: unknown) => TestBed.inject(Router).serializeUrl(result as UrlTree);
  beforeEach(() => {
    account = new Subject<AuthMe>();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthStore, useValue: { getMe: () => account } },
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
    expect(serialize(run('/demo-registro/list'))).toBe('/login?returnTo=%2Fdemo-registro%2Flist');
    expect(serialize(run('/tenants'))).toBe('/login?returnTo=%2Ftenants');
  });
  it('deveEncaminharAoLoginComRetornoInterno_quandoRotaTemUUID', () => {
    const view = '/demo-registro/view/123e4567-e89b-12d3-a456-426614174000';
    expect(serialize(run(view))).toBe('/login?returnTo=' + encodeURIComponent(view));
  });
  it('deveEncaminharAoLogin_quandoIdentidadeExisteMasMeAindaNaoChegou', async () => {
    change(user);
    await tick();
    expect(auth.authenticatedUser()).toBe(user);
    expect(serialize(run('/tenants'))).toBe('/login?returnTo=%2Ftenants');
  });
  it('deveLiberarRota_quandoMeValidado', async () => {
    change(user);
    await tick();
    account.next(me);
    await tick();
    expect(run('/tenants')).toBe(true);
  });
  it('deveEncaminharAoLogin_quandoMeFalhaParaIdentidadeExistente', async () => {
    change(user);
    await tick();
    account.error(new Error('forbidden'));
    await tick();
    expect(auth.authenticatedUser()).toBe(user);
    expect(serialize(run('/tenants'))).toBe('/login?returnTo=%2Ftenants');
  });
});
