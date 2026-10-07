import { inject } from '@angular/core';
import { CanActivateFn, Router, RouterStateSnapshot } from '@angular/router';
import { AuthorizationRequirement } from '../auth/acoes';
import { AuthService } from '../auth/auth.service';

/**
 * Guard de autorização por ação. Lê o requisito de `route.data.authorization`.
 *
 * Substitui o `gemsRoleGuard` do SDK porque precisamos **controlar o destino da negação**:
 *   - não logado                ⇒ login no Keycloak, volta à URL pedida;
 *   - logado sem a ação         ⇒ redireciona para `/acesso-negado` (página dedicada);
 *   - logado com a ação         ⇒ libera;
 *   - **rota sem ação declarada ⇒ NEGA.**
 *
 * ## Por que a última linha existe, e por que ela é nova
 *
 * Um guard que faça `if (required.length === 0) return true` **falha
 * aberto**. Uma rota que declarasse `canActivate: [roleGuard]` e esquecesse `data.authorization`, ou
 * escrevesse o nome da ação errado, liberava a tela a **qualquer pessoa autenticada**, em silêncio:
 * nada quebrava, nenhum teste reclamava, e a tela simplesmente ficava aberta.
 *
 * Isso passou de risco teórico a risco concreto quando a 011 reescreveu as **24** declarações de
 * `data.authorization` de uma vez. Falhar fechado transforma o mesmo erro em um sintoma imediato e
 * localizável — "acesso negado onde não deveria" — em vez de uma porta aberta que ninguém vê.
 *
 * A rede complementar é a varredura em `routes-coverage.spec.ts`, que falha se alguma rota guardada
 * não declarar ação. As duas juntas espelham, no frontend, o que o `CoberturaPreAuthorizeTest` faz
 * no backend.
 *
 * O requisito contém escopo e ações; sem ambos, a rota é negada.
 */
export const roleGuard: CanActivateFn = (route, state: RouterStateSnapshot) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.isLoggedIn()) {
    auth.login(window.location.origin + state.url);
    return false;
  }

  const required = route.data?.['authorization'];

  if (!isAuthorizationRequirement(required)) {
    console.error(
      `[roleGuard] A rota "${state.url}" está protegida mas não declara nenhuma ação em ` +
        `data.authorization. Acesso negado por omissão. Declare a ação exigida em <feature>.routes.ts.`,
    );
    return router.createUrlTree(['/acesso-negado']);
  }

  return auth.isAuthorized(required) ? true : router.createUrlTree(['/acesso-negado']);
};

function isAuthorizationRequirement(value: unknown): value is AuthorizationRequirement {
  if (typeof value !== 'object' || value === null) return false;
  const requirement = value as { scope?: unknown; actions?: unknown };
  return (
    requirement.scope === 'global' &&
    Array.isArray(requirement.actions) &&
    requirement.actions.length > 0 &&
    requirement.actions.every((action) => typeof action === 'string')
  );
}
