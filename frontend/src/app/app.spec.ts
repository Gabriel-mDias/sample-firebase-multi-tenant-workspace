import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { GemsNavigationService } from '@gabriel-mdias/angular-gems-sdk';
import { signal } from '@angular/core';
import { Subject } from 'rxjs';
import { App } from './app';
import { AuthService } from './core/auth/auth.service';
describe('App / casca', () => {
  function invalidationAt(url: string, authenticated = false) {
    const invalidated = new Subject<void>();
    const navigate = vi.fn();
    TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        { provide: GemsNavigationService, useValue: { navigate } },
        {
          provide: AuthService,
          useValue: {
            me: signal(null),
            invalidated,
            authenticatedUser: signal(authenticated ? { uid: 'current' } : null),
            isAuthorized: () => false,
          },
        },
      ],
    });
    vi.spyOn(TestBed.inject(Router), 'url', 'get').mockReturnValue(url);
    const fixture = TestBed.createComponent(App);
    invalidated.next();
    return { fixture, navigate };
  }

  it.each([
    '/login',
    '/login?returnTo=%2Fdemo-registro%2Flist',
    '/login#form',
    '/login?returnTo=%2Fdemo-registro%2Fview%2F123#form',
  ])('devePreservarRetorno_quandoLoginEmAndamentoEm %s', (url) => {
    const { navigate } = invalidationAt(url);
    expect(navigate).not.toHaveBeenCalled();
  });

  it.each(['/demo-registro/list', '/tenants', '/loginOutra', '/login/outro'])(
    'deveRedirecionar_quandoSessaoInvalidadaForaDoLoginEm %s',
    (url) => {
      const { navigate } = invalidationAt(url);
      expect(navigate).toHaveBeenCalledExactlyOnceWith(['/login']);
    },
  );

  it('naoDeveRedirecionar_quandoIdentidadeContinuaAutenticada', () => {
    const { navigate } = invalidationAt('/tenants', true);
    expect(navigate).not.toHaveBeenCalled();
  });

  it('naoDeveRenderizarMenu_quandoSessaoAindaNaoValidada', () => {
    TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: {
            me: signal(null),
            invalidated: new Subject<void>(),
            authenticatedUser: signal(null),
            isAuthorized: () => false,
          },
        },
      ],
    });
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('gems-side-menu')).toBeNull();
  });
});
