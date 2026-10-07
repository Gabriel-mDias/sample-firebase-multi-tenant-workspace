---
stack: frontend-angular
module: keycloak-angular, gems-sdk (provideGemsKeycloak)
severity: mandatory
see-also: [http-interceptors.md, stores.md, organization.md]
origem: local
precedencia: local-vence
versao: 2.0.0
---

# Autenticação e Autorização — Angular / gems-sdk

> **TL;DR:**
> - OIDC **Authorization Code + PKCE** pelo Keycloak, via `provideGemsKeycloak()` da SDK.
> - Uma fachada local (`AuthService`) é o **único** ponto que lê o token. Nenhum componente lê `tokenParsed`.
> - Autorização é por **ação concreta**, nunca por nível/role: `isAuthorized({ scope, actions })`.
> - Escopo é `'global'` ou `'tenant'`, e **ação global nunca satisfaz requisito tenant**.
> - Guards funcionais (`CanActivateFn`). Rota protegida **sem** ação declarada ⇒ **nega** (fail-closed).
> - Toda rota guardada declara `data.authorization`; uma varredura de teste falha se alguma não declarar.

> **Por que esta regra foi reescrita.** Ela prescrevia `angular-oauth2-oidc`, um `SessionService` com
> `hasRole(role)` e guards do tipo `roleGuard('ADMIN')`. Duas coisas estavam erradas: a biblioteca
> (o projeto usa `keycloak-angular` com `provideGemsKeycloak`) e, mais grave, **o modelo de
> autorização** — autorizar por nível é exatamente o que a constituição proíbe (RF002: autorização
> por ações concretas, nunca `hasAnyRole('ADMIN')`). Corrigida na rodada `008`, bloco M.

---

## 1. Configuração em `app.config.ts`

```typescript
import { provideGemsKeycloak } from '@gabriel-mdias/angular-gems-sdk/auth';
import { provideGemsHttp } from '@gabriel-mdias/angular-gems-sdk';
import { keycloakOptions } from './core/auth/keycloak.init';
import { environment } from '../environments/environment';

export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(withFetch(), withInterceptorsFromDi()),
    provideGemsKeycloak(keycloakOptions()),
    provideGemsHttp(environment.apiBaseUrl),
    // ...
  ],
};
```

O `provideGemsKeycloak` cuida da inicialização, do `check-sso` silencioso e da injeção do Bearer
token nas chamadas à API. **Não** monte `AuthConfig` à mão nem chame `loadDiscoveryDocument`.

---

## 2. A fachada `AuthService`

Concentra num único ponto a leitura do token. Isto não é preferência de estilo: espalhar
`tokenParsed` pelos componentes é o caminho pelo qual um claim malformado vira fallback local
silencioso.

```typescript
import { Injectable, inject, signal } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';
import { AUTHORIZATION_ACTIONS } from '../authorization/authorization.actions';
import { AuthorizationRequirement, isAuthorized } from '../authorization/authorization-requirement';
import { AuthContext } from './auth-context.model';
import { parseAuthContext } from './auth-context.parser';

@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly keycloak = inject(KeycloakService);

  /** Revisão do token. O KeycloakService não é reativo; o contador reavalia os derivados. */
  private readonly rev = signal(0);

  private get token(): Record<string, unknown> | undefined {
    return this.keycloak.getKeycloakInstance()?.tokenParsed as Record<string, unknown> | undefined;
  }

  isLoggedIn(): boolean {
    return this.keycloak.isLoggedIn();
  }

  login(redirectUri?: string, scope?: string): Promise<void> {
    return this.keycloak.login(redirectUri || scope ? { redirectUri, scope } : undefined);
  }

  logout(): Promise<void> {
    return this.keycloak.logout(window.location.origin);
  }

  context(requestedAlias?: string): AuthContext {
    this.rev();
    return parseAuthContext(this.token, AUTHORIZATION_ACTIONS, requestedAlias);
  }

  isAuthorized(requirement: AuthorizationRequirement): boolean {
    return isAuthorized(this.context(), requirement);
  }
}
```

---

## 3. Autorização por ação, não por nível

O requisito é um par **escopo + ações**, e as duas metades importam:

```typescript
export interface AuthorizationRequirement {
  readonly scope: 'global' | 'tenant';
  readonly actions: readonly string[];
}

export function isAuthorized(context: AuthContext, requirement: AuthorizationRequirement): boolean {
  const owned = requirement.scope === 'global' ? context.globalActions : context.tenant?.actions;
  return owned !== undefined && requirement.actions.every((action) => owned.has(action));
}
```

**O escopo não é decoração.** Sem ele, uma ação global homônima satisfaria uma rota tenant — e
`ADMIN` global passaria a enxergar dado escolar de qualquer organização. A separação dos dois
conjuntos é a garantia, e ela tem prova dedicada nos testes do parser.

### O catálogo de ações tem uma fonte

As ações válidas vêm de um catálogo **gerado do enum do backend**, nunca digitadas à mão no
frontend. Um gate de build compara os dois e reprova divergência: ação escrita errada não compila.

```typescript
import { AUTHORIZATION_ACTIONS } from '../authorization/authorization.actions';
```

---

## 4. Guards funcionais

Os dois são `CanActivateFn`. Nunca classe com `implements CanActivate`.

### `authGuard` — só exige sessão

```typescript
export const authGuard: CanActivateFn = (_route, state: RouterStateSnapshot) => {
  const auth = inject(AuthService);

  if (auth.isLoggedIn()) return true;

  auth.login(window.location.origin + state.url);   // volta exatamente onde o usuário tentou entrar
  return false;
};
```

### `roleGuard` — exige a ação declarada na rota, e **falha fechado**

```typescript
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
        `data.authorization. Acesso negado por omissão ou formato inválido.`,
    );
    return router.createUrlTree(['/acesso-negado']);
  }

  return auth.isAuthorized(required) ? true : router.createUrlTree(['/acesso-negado']);
};
```

**A terceira ramificação é a que importa.** Rota protegida sem ação declarada é omissão de quem
escreveu a rota; liberar nesse caso faria a omissão passar despercebida até virar incidente. Negar
transforma o erro em sintoma imediato.

> `console.error` aqui é diagnóstico para o desenvolvedor, não feedback ao usuário — a proibição de
> `console.log` em `feedback-alerts.md` continua valendo para tudo que o usuário deveria ver.

---

## 5. Rotas

Toda rota guardada declara escopo e ações em `data.authorization`:

```typescript
export const {MODULO}_ROUTES: Routes = [
  {
    path: '',
    canActivate: [roleGuard],
    data: { authorization: { scope: 'global', actions: ['CONSULTAR_{ENTIDADE}'] } },
    loadComponent: () => import('./list/{modulo}-list.component').then((c) => c.{Entidade}ListComponent),
  },
  {
    path: 'novo',
    canActivate: [roleGuard],
    data: { authorization: { scope: 'global', actions: ['INSERIR_{ENTIDADE}'] } },
    loadComponent: () => import('./form/{modulo}-form.component').then((c) => c.{Entidade}FormComponent),
  },
];
```

A raiz conhece a feature **apenas** por `loadChildren` — nunca os componentes internos dela:

```typescript
// app.routes.ts
{
  path: '{modulo}',
  loadChildren: () => import('./features/{modulo}/{modulo}.routes').then((m) => m.{MODULO}_ROUTES),
}
```

### A rede que fecha a lacuna

Um `routes-coverage.spec.ts` percorre as rotas, resolve cada `loadChildren` e **falha** se uma rota
guardada não declarar `data.authorization`. O guard protege em execução; a varredura protege contra
a rota que ninguém protegeu.

---

## 6. Uso no Template

Autorização em tela espelha a mesma ação concreta da rota — nunca uma checagem diferente:

```typescript
protected readonly canCreate = signal(
  this.auth.isAuthorized({ scope: 'global', actions: ['INSERIR_{ENTIDADE}'] }),
);
```

```html
@if (canCreate()) {
  <button type="button" class="btn-novo" (click)="novo()">
    <i class="fa-solid fa-plus"></i>
    Nova {Entidade}
  </button>
}
```

Numa `gems-table`, a ação condicional usa `visible`:

```typescript
{ actionName: 'edit', icon: 'fa-solid fa-pen', tooltip: 'Editar',
  colorClass: 'btn-warning', visible: () => this.canEdit() }
```

**Esconder o botão não é a proteção** — é cortesia. A proteção é o guard na rota e o `@PreAuthorize`
no endpoint. Os três citam a mesma ação.

---

## 7. Regras

| Regra | Detalhe |
| :--- | :--- |
| PKCE obrigatório | Authorization Code + PKCE via `provideGemsKeycloak` — nunca implicit flow |
| Token só pela fachada | Nenhum componente lê `tokenParsed`, `localStorage` ou `getToken()` |
| Autorização por ação | `isAuthorized({ scope, actions })` — nunca `hasRole` / `hasAnyRole` |
| Escopo explícito | `'global'` ou `'tenant'`; ação global não satisfaz requisito tenant |
| Catálogo gerado | `AUTHORIZATION_ACTIONS` vem do enum do backend, verificado no build |
| Guards funcionais | `CanActivateFn` — nunca `implements CanActivate` |
| Fail-closed | Claim ausente, ambíguo ou inválido nega; **nunca** produz fallback local |
| Negação tem destino | `/acesso-negado`, página dedicada — não um 404 nem a home |

---

## Anti-Patterns (NUNCA)

| Proibido | Correto |
| :--- | :--- |
| `angular-oauth2-oidc`, `OAuthService`, `authConfig` à mão | `provideGemsKeycloak(keycloakOptions())` |
| `session.hasRole('ADMIN')` | `auth.isAuthorized({ scope: 'global', actions: ['INSERIR_X'] })` |
| `roleGuard('ADMIN')` (guard parametrizado por nível) | `roleGuard` lendo `data.authorization` da rota |
| Rota protegida sem `data.authorization` | Declarar escopo e ações; o guard nega quem não declara |
| Requisito sem `scope` | `{ scope: 'tenant', actions: [...] }` — escopo sempre explícito |
| `localStorage.getItem('access_token')` | A fachada `AuthService`; nada lê o storage direto |
| `class AuthGuard implements CanActivate` | `export const authGuard: CanActivateFn = ...` |
| Ação digitada à mão no frontend | `AUTHORIZATION_ACTIONS`, gerado do enum e verificado no build |
