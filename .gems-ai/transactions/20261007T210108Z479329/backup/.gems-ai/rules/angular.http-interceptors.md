<!-- gems-ai-rule: {"id":"angular.http-interceptors","version":"2.0.0","status":"candidate"} -->
---
stack: frontend-angular
module: gems-sdk (provideGemsHttp), @angular/common/http
severity: mandatory
see-also: [stores.md, auth.md]
origem: local
precedencia: local-vence
versao: 2.0.0
---

# HTTP e Interceptors — Angular / gems-sdk

> **TL;DR:**
> - `provideHttpClient(withFetch(), withInterceptorsFromDi())` + `provideGemsHttp(baseUrl)`.
> - `withFetch()` é obrigatório. `provideGemsHttp` fixa a base da API num lugar só.
> - Loading global, Bearer token e `X-Correlation-Id` vêm da SDK — **não** escreva os seus.
> - Interceptor próprio, quando houver, é funcional (`HttpInterceptorFn`) — nunca classe.
> - **NUNCA** `HttpClient` em componente: sempre via `GemsBaseStore`.

> **Por que esta regra foi reescrita.** Ela prescrevia `withInterceptors([loadingInterceptor,
> correlationIdInterceptor])` e mandava escrever o interceptor de correlação **no projeto**. A SDK
> passou a registrar os seus interceptors por DI e a expor `provideGemsHttp`, então o projeto usa
> `withInterceptorsFromDi()` e não tem — nem deve ter — um `correlationIdInterceptor` local.
> Corrigida na rodada `008`, bloco M.

---

## 1. Registro em `app.config.ts`

```typescript
import { provideHttpClient, withFetch, withInterceptorsFromDi } from '@angular/common/http';
import { provideGemsHttp } from '@gabriel-mdias/angular-gems-sdk';
import { environment } from '../environments/environment';

export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(withFetch(), withInterceptorsFromDi()),
    provideGemsHttp(environment.apiBaseUrl),
    // ...
  ],
};
```

| Peça | O que faz | Por que não é opcional |
| :--- | :--- | :--- |
| `withFetch()` | usa a Fetch API em vez de `XMLHttpRequest` | Angular 19+ assume Fetch; omitir cai no caminho legado, com comportamento diferente de cancelamento e streaming |
| `withInterceptorsFromDi()` | registra os interceptors que a SDK provê por DI | sem isto o loading global, o Bearer e o correlation-id **não rodam**, e a falha é silenciosa: as requisições funcionam, só que sem token |
| `provideGemsHttp(baseUrl)` | fixa a base da API para toda Store | a Store passa caminho relativo (`'api/organizacoes'`); a base muda por ambiente, num lugar só |

---

## 2. O que a SDK já entrega, e você não escreve

| Comportamento | Quem faz | O que **não** fazer |
| :--- | :--- | :--- |
| Loading global enquanto há requisição em voo | interceptor da SDK | criar `LoadingService`, `ngx-spinner` ou contador próprio |
| `Authorization: Bearer <token>` | integração `provideGemsKeycloak` | montar o header à mão a partir do storage |
| `X-Correlation-Id` por requisição | interceptor da SDK | escrever um `correlationIdInterceptor` local |

O backend (`gems-rest-common`) lê o `X-Correlation-Id`, grava no MDC e o devolve na resposta — ver
`rules/backend-java/rest-common.md` para o outro lado.

---

## 3. Interceptor próprio

Só quando o comportamento **não** existir na SDK. Funcional, nunca classe:

```typescript
// src/app/core/interceptors/{nome}.interceptor.ts
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

export const {nome}Interceptor: HttpInterceptorFn = (req, next) =>
  next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) inject(Router).navigate(['/acesso-negado']);
      return throwError(() => error);
    }),
  );
```

Registrado por DI, junto dos da SDK:

```typescript
{ provide: HTTP_INTERCEPTORS, useValue: {nome}Interceptor, multi: true }
```

Antes de escrever um, confirme que a SDK não cobre o caso — interceptor duplicado sobre o mesmo
comportamento produz efeito dobrado (dois contadores de loading, dois headers), e o sintoma aparece
longe da causa.

---

## 4. Regras

| Regra | Detalhe |
| :--- | :--- |
| `withFetch()` obrigatório | Explícito no `provideHttpClient` |
| `withInterceptorsFromDi()` obrigatório | É como os interceptors da SDK entram |
| `provideGemsHttp(baseUrl)` uma vez | A Store nunca repete o host |
| Interceptor funcional | `HttpInterceptorFn` — nunca `implements HttpInterceptor` |
| `HttpClient` nunca em componente | Sempre `GemsBaseStore.get/post/put/delete` |

---

## Anti-Patterns (NUNCA)

| Proibido | Correto |
| :--- | :--- |
| `withInterceptors([...])` com os interceptors da SDK listados à mão | `withInterceptorsFromDi()` |
| Escrever um `correlationIdInterceptor` no projeto | A SDK já envia o `X-Correlation-Id` |
| Criar interceptor ou serviço de loading próprio | O da SDK, por DI |
| `class LoadingInterceptor implements HttpInterceptor` | `export const x: HttpInterceptorFn = ...` |
| Omitir `withFetch()` | `provideHttpClient(withFetch(), withInterceptorsFromDi())` |
| `HttpClient` injetado no componente | `GemsBaseStore` |
| URL absoluta na Store (`https://api…/organizacoes`) | `super('api/organizacoes')` + `provideGemsHttp` |
