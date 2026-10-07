import { KeycloakOptions } from 'keycloak-angular';
import { environment } from '../../../environments/environment';

/**
 * Opções do Keycloak (Authorization Code + PKCE S256, hospedado no próprio Keycloak),
 * consumidas por `provideGemsKeycloak(...)` no `app.config.ts`.
 *
 * `onLoad: 'check-sso'` não força login no bootstrap: a aplicação sobe e o usuário é redirecionado
 * ao login apenas ao acessar uma rota protegida (via `roleGuard`/`authGuard`).
 *
 * `provideGemsKeycloak` já roda o `init` com fail-open (try/catch): falha ao contatar o Keycloak não
 * trava o bootstrap — a app sobe não-autenticada e os guards redirecionam ao login quando necessário.
 */
export function keycloakOptions(): KeycloakOptions {
  const { issuer, clientId } = environment.oidc;
  // issuer = http://host/realms/{realm} → deriva url base e realm.
  const match = issuer.match(/^(.*)\/realms\/([^/]+)$/);
  const url = match ? match[1] : issuer;
  const realm = match ? match[2] : 'app-realm';

  return {
    config: { url, realm, clientId },
    initOptions: {
      onLoad: 'check-sso',
      pkceMethod: 'S256',
      checkLoginIframe: false,
      silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
    },
    enableBearerInterceptor: true,
    bearerPrefix: 'Bearer',
    // Domínio da API que deve receber o token; evita vazar o Bearer para terceiros.
    shouldAddToken: (request) => request.url.startsWith(environment.apiBaseUrl),
  };
}
