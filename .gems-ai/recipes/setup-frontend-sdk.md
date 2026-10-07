---
origem: local
precedencia: local-vence
versao: 2.1.0
---

# Recipe: Setup Frontend — Angular GEMS SDK

**Objetivo:** Configurar um novo projeto Angular com a GEMS SDK via GitHub Packages npm, design tokens e interceptors obrigatórios.

> **Por que esta recipe foi reescrita (rodada `009`, T059).** Os passos 3, 5 e 6 ainda prescreviam
> `angular-oauth2-oidc`/`provideOAuthClient`, um `correlationIdInterceptor` local, `session.service.ts`
> e o `gems-theme.css` — tudo o que `auth.md` e `http-interceptors.md` (rodada `008`) já tinham
> retirado. A recipe agora espelha o `app.config.ts` do Meduc; as regras são a autoridade, a recipe
> só encadeia os passos.

**Rules relacionadas:**
- [signals-standards.md](../rules/frontend-angular/signals-standards.md)
- [visual-standards.md](../rules/frontend-angular/visual-standards.md)
- [http-interceptors.md](../rules/frontend-angular/http-interceptors.md)
- [auth.md](../rules/frontend-angular/auth.md)
- [organization.md](../rules/frontend-angular/organization.md)

---

## Passo 1: Configurar GitHub Packages npm

```bash
# .npmrc na raiz do projeto
@gabriel-mdias:registry=https://npm.pkg.github.com
//npm.pkg.github.com/:_authToken=${GITHUB_TOKEN}
```

`GITHUB_TOKEN` = Personal Access Token GitHub com permissão `read:packages`.

## Passo 2: Instalar a SDK

```bash
npm install @gabriel-mdias/angular-gems-sdk
npm install keycloak-angular
npm install animate.css
```

**Sem `--legacy-peer-deps`.** A SDK 2.0.0 declara peer Angular 22. Se uma dependência declarar peer
de Angular anterior (hoje só `keycloak-angular@16`, que pede `@angular ^18`), a saída é o bloco
`overrides` do `package.json`, que aponta o peer para a versão instalada — e não a flag, que
esconde todo conflito futuro:

```json
"overrides": {
  "keycloak-angular": {
    "@angular/common": "$@angular/common",
    "@angular/core": "$@angular/core",
    "@angular/router": "$@angular/router"
  }
}
```

## Passo 3: app.config.ts Completo

```typescript
// src/app/app.config.ts
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideHttpClient, withFetch, withInterceptorsFromDi } from '@angular/common/http';
import { provideGemsHttp, provideGemsTheme } from '@gabriel-mdias/angular-gems-sdk';
import { provideGemsKeycloak } from '@gabriel-mdias/angular-gems-sdk/auth';
import { routes } from './app.routes';
import { environment } from '../environments/environment';
import { keycloakOptions } from './core/auth/keycloak.init';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    // A SDK ainda usa a API de animações clássica (ex.: gems-summary-card).
    provideAnimationsAsync(),
    // Os interceptors (loading, Bearer, X-Correlation-Id) são da SDK, registrados por DI.
    provideHttpClient(withFetch(), withInterceptorsFromDi()),
    // Authorization Code + PKCE pelo Keycloak; keycloakOptions() lê environment.oidc.
    provideGemsKeycloak(keycloakOptions()),
    // O único lugar de cor do projeto (Princípio VI).
    provideGemsTheme({
      primary: '#1e3a5f',
      secondary: '#3b82f6',
      tertiary: '#6366f1',
      background: '#f8fafc',
    }),
    provideGemsHttp(environment.apiBaseUrl),
  ],
};
```

Não existe interceptor local nem `angular-oauth2-oidc`: `http-interceptors.md` §2 lista o que a SDK
entrega, e `auth.md` §1 descreve o `provideGemsKeycloak`. O `keycloak.init.ts` deriva `url` e `realm`
do `issuer` de `environment.oidc` e usa `onLoad: 'check-sso'` com `pkceMethod: 'S256'`.

## Passo 4: app.component.html

```html
<!-- src/app/app.component.html -->
<gems-loading></gems-loading>
<router-outlet></router-outlet>
```

`gems-loading` exibe o loading global gerenciado pelo `loadingInterceptor`.

## Passo 5: Estrutura de Pastas

```
src/app/
├── core/
│   ├── auth/
│   │   ├── keycloak.init.ts
│   │   └── auth.service.ts
│   └── guards/
│       └── role.guard.ts
├── features/
│   └── {modulo}/
│       ├── {modulo}.routes.ts
│       ├── models/
│       │   ├── {entidade}.model.ts
│       │   └── {entidade}-filter.model.ts
│       ├── services/
│       │   └── {entidade}-state.service.ts
│       ├── list/
│       │   ├── {entidade}-list.component.ts
│       │   └── {entidade}-list.component.html
│       └── form/
│           ├── {entidade}-form.component.ts
│           └── {entidade}-form.component.html
├── app.component.ts
├── app.component.html
├── app.config.ts
└── app.routes.ts
```

## Passo 6: Configurar angular.json (styles)

```json
// angular.json → projects.{app}.architect.build.options
{
  "styles": [
    "node_modules/animate.css/animate.min.css",
    "src/styles.css"
  ]
}
```

Não há folha de tema para incluir: os `--gems-*` são gerados em runtime pelo `provideGemsTheme`
(`visual-standards.md` §2). Fontes (`@fontsource/inter`) e ícones (Font Awesome) entram no mesmo
array quando o projeto os usa.

## Passo 7: environments/

```typescript
// src/environments/environment.ts
export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080',
  oidc: {
    issuer:      'https://your-idp.example.com/realms/{realm}',
    clientId:    '{client-id}',
    redirectUri: window.location.origin + '/callback',
    scope:       'openid profile email'
  }
};
```

```typescript
// src/environments/environment.prod.ts
export const environment = {
  production: true,
  apiBaseUrl: 'https://api.{app}.com.br',
  oidc: {
    issuer:      'https://your-idp.example.com/realms/{realm}',
    clientId:    '{client-id}',
    redirectUri: 'https://{app}.com.br/callback',
    scope:       'openid profile email'
  }
};
```

---

## Checklist de Conformidade

- [ ] `.npmrc` com registry e token do GitHub Packages.
- [ ] `provideGemsTheme()` configurado com as cores do projeto.
- [ ] `provideHttpClient(withFetch(), withInterceptorsFromDi())` + `provideGemsHttp(environment.apiBaseUrl)`;
      nenhum interceptor local.
- [ ] `provideGemsKeycloak(keycloakOptions())`; nenhum `angular-oauth2-oidc`.
- [ ] `<gems-loading>` no `app.component.html`.
- [ ] `animate.css` nos estilos do `angular.json`; nenhum `gems-theme.css`.
- [ ] Estrutura de pastas: `core/` (auth + guards) + `features/{modulo}/`.
- [ ] `environment.ts` com `apiBaseUrl` e `oidc` (`issuer`, `clientId`, `redirectUri`, `scope`).
- [ ] Nenhum componente usa `HttpClient` diretamente — sempre via `GemsBaseStore`.
