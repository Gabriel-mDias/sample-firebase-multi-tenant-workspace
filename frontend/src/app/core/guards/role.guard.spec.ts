import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  provideRouter,
  Router,
  RouterStateSnapshot,
} from '@angular/router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { Acoes, globalAuthorization } from '../auth/acoes';
import { AuthService } from '../auth/auth.service';
import { roleGuard } from './role.guard';

function configure(auth: Partial<AuthService>): void {
  TestBed.configureTestingModule({
    providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
  });
}

function runGuard(route: Partial<ActivatedRouteSnapshot>) {
  return TestBed.runInInjectionContext(() =>
    roleGuard(route as ActivatedRouteSnapshot, { url: '/parceiros' } as RouterStateSnapshot),
  );
}

describe('roleGuard', () => {
  beforeEach(() => {
    vi.stubGlobal('location', { origin: 'http://localhost:4200' });
  });

  it('deveDispararLoginENegar_quandoNaoLogado', () => {
    const login = vi.fn();
    configure({ isLoggedIn: () => false, login });

    const result = runGuard({
      data: { authorization: globalAuthorization(Acoes.CONSULTAR_PARCEIRO) },
    });

    expect(result).toBe(false);
    expect(login).toHaveBeenCalledWith('http://localhost:4200/parceiros');
  });

  it('deveNegar_quandoRequisitoDeAutorizacaoAusente', () => {
    configure({ isLoggedIn: () => true, isAuthorized: vi.fn(() => true) });
    const router = TestBed.inject(Router);

    const result = runGuard({ data: {} });

    expect(result).toEqual(router.createUrlTree(['/acesso-negado']));
  });

  it('deveLiberar_quandoAcaoExigidaAutorizada', () => {
    configure({
      isLoggedIn: () => true,
      isAuthorized: (requirement) => requirement.actions.includes(Acoes.CONSULTAR_PARCEIRO),
    });

    const result = runGuard({
      data: { authorization: globalAuthorization(Acoes.CONSULTAR_PARCEIRO) },
    });

    expect(result).toBe(true);
  });

  it('deveRedirecionarParaAcessoNegado_quandoAcaoExigidaNaoAutorizada', () => {
    configure({ isLoggedIn: () => true, isAuthorized: () => false });
    const router = TestBed.inject(Router);

    const result = runGuard({
      data: { authorization: globalAuthorization(Acoes.EXCLUIR_PARCEIRO) },
    });

    expect(result).toEqual(router.createUrlTree(['/acesso-negado']));
  });
});
