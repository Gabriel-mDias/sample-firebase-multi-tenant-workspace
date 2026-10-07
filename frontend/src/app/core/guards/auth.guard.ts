import { inject } from '@angular/core';
import { CanActivateFn, RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../auth/auth.service';

/**
 * Guard de autenticação. Exige apenas sessão válida — não checa role.
 *
 * Logado ⇒ libera. Não logado ⇒ dispara o login do Keycloak (Authorization Code + PKCE) apontando
 * o `redirectUri` para a URL solicitada, de modo que o usuário volte exatamente onde tentou entrar.
 */
export const authGuard: CanActivateFn = (_route, state: RouterStateSnapshot) => {
  const auth = inject(AuthService);

  if (auth.isLoggedIn()) {
    return true;
  }

  auth.login(window.location.origin + state.url);
  return false;
};
