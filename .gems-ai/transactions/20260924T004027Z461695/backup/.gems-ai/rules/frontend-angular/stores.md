---
stack: frontend-angular
module: gems-sdk (GemsBaseStore)
severity: mandatory
see-also: [signals-standards.md, lists.md, forms.md, http-interceptors.md]
origem: local
precedencia: local-vence
versao: 2.0.0
---

# Stores (HTTP Client Layer) — Angular / gems-sdk

> **TL;DR:**
> - Toda Store estende `GemsBaseStore` de `@gabriel-mdias/angular-gems-sdk` — **nunca** `HttpClient` no componente.
> - `@Injectable({ providedIn: 'root' })`. Arquivo: `features/{modulo}/services/{modulo}.store.ts`.
> - `super('api/{recurso-kebab-case}')` — **sem** barra inicial, **sem** host (quem dá a base é `provideGemsHttp`).
> - Ordem: `getAll` > `getById` > `search` > `create` > `update` > `delete{Entidade}` > ações de negócio.
> - `search()` devolve `Observable<Page<T>>` — a página do Spring Data inteira, não um par reduzido.
> - IDs sempre `string`. Generics explícitos em todo método. **Nunca** `object` ou `any` no lugar do tipo.
> - Uma linha em branco entre métodos.

> **Por que esta regra foi reescrita.** Os exemplos importavam de `'gems-sdk'` símbolos sem o prefixo
> `Gems` (`Pageable`, `BaseStore`) — nomes que não existem: o pacote é
> `@gabriel-mdias/angular-gems-sdk` e os símbolos são `GemsBaseStore`, `GemsPageable`. E `search()`
> era descrito devolvendo `{ content, totalElements }`, o que descarta `totalPages`, `number` e
> `size`, que a `gems-table` consome. Corrigida na rodada `008`, bloco M.

---

## 1. Declaração da Store

```typescript
import { Injectable } from '@angular/core';
import { GemsBaseStore, GemsPageable } from '@gabriel-mdias/angular-gems-sdk';
import { Observable } from 'rxjs';
import { {Entidade}, {Entidade}Detail, {Entidade}Filter, Page } from '../models/{modulo}.model';

@Injectable({ providedIn: 'root' })
export class {Entidade}Store extends GemsBaseStore {

  constructor() {
    super('api/{recurso-kebab-case}');   // sem barra inicial, sem host
  }
```

- **Estende** `GemsBaseStore`. Não existe `BaseStore`.
- `@Injectable({ providedIn: 'root' })` sempre — a Store não guarda estado de tela.
- O host vem de `provideGemsHttp(environment.apiBaseUrl)`; a Store só conhece o caminho.

---

## 2. `Page<T>` — a forma que o backend devolve

O backend pagina com Spring Data. A página inteira atravessa:

```typescript
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
```

Reduzi-la a `{ content, totalElements }` na Store obriga o componente a recalcular o que já veio
pronto, e desalinha a `gems-table` da resposta real assim que a paginação deixa de ser trivial.

---

## 3. Ordem Fixa dos Métodos

1. `getAll()` — quando existir listagem sem filtro
2. `getById(id)`
3. `search(filter, pageable)`
4. `create(dto)`
5. `update(id, dto)`
6. `delete{Entidade}(id)`
7. Ações de negócio (na ordem em que o fluxo as usa)

A ordem é a mesma do service do backend (`findById > search > delete > insert > save`) invertida para
a leitura de cliente: primeiro o que lê, depois o que escreve, por último o que é específico do
domínio. Um arquivo previsível se revisa mais rápido do que um arquivo ordenado por acaso.

---

## 4. Padrão de Cada Método

```typescript
  getById(id: string): Observable<{Entidade}Detail> {
    return this.get<{Entidade}Detail>(id);
  }

  search(filter: {Entidade}Filter, pageable: GemsPageable): Observable<Page<{Entidade}>> {
    return this.post<Page<{Entidade}>, {Entidade}Filter>(this.pageablePath('search', pageable), filter);
  }

  create(body: {Entidade}Create): Observable<{Entidade}Detail> {
    return this.post<{Entidade}Detail, {Entidade}Create>('', body);
  }

  update(id: string, body: {Entidade}Update): Observable<{Entidade}Detail> {
    return this.put<{Entidade}Detail, {Entidade}Update>(id, body);
  }

  delete{Entidade}(id: string): Observable<void> {
    return this.delete<void>(id);
  }
```

**Uma linha em branco entre métodos.** Métodos colados escondem onde um termina e o outro começa, e
é justamente na borda que o revisor procura o erro.

---

## 5. Ações de Negócio

Para o que vai além do CRUD, o nome é o da ação em camelCase e o caminho é kebab-case:

```typescript
  retentar(id: string, body: RetentativaInput): Observable<{Entidade}Detail> {
    return this.post<{Entidade}Detail, RetentativaInput>(`${id}/retentativa`, body);
  }

  gestores(id: string, filter: GestorFilter, pageable: GemsPageable): Observable<Page<Gestor>> {
    return this.post<Page<Gestor>, GestorFilter>(
      this.pageablePath(`${id}/gestores/search`, pageable),
      filter,
    );
  }

  removerGestor(id: string, userId: string): Observable<void> {
    return this.delete<void>(`${id}/gestores/${userId}`);
  }
```

Cada transição de fluxo é **um endpoint de ação** — situação nunca entra pelo corpo da requisição.
É a mesma regra do lado do backend.

---

## 6. Paginação na URL

A paginação do Spring Data vai em query string. Um helper **privado** monta o caminho, para que
nenhum método repita a serialização:

```typescript
  private pageablePath(path: string, pageable: GemsPageable): string {
    const params = new URLSearchParams({
      page: String(pageable.page ?? 0),
      size: String(pageable.size ?? 10),
    });
    pageable.sort?.forEach((sort) => params.append('sort', sort));
    return `${path}?${params.toString()}`;
  }
```

`sort` é `string[]` no formato `campo,direcao` — múltiplos critérios viram múltiplos parâmetros
`sort`, que é o que o Spring Data espera.

---

## 7. Tipagem

- Generics **sempre**: `this.get<T>()`, `this.post<TReturn, TBody>()`.
- IDs são `string` no frontend; a conversão é do backend.
- **`object` não é tipo.** `filter: object` e `body: object` passam pelo compilador sem proteger
  nada: qualquer campo entra, nenhum erro de digitação aparece. Declare a interface — mesmo que ela
  tenha três campos opcionais, ela é o que impede `{ nmoe: 'x' }` de virar filtro silenciosamente
  ignorado pelo backend.
- Evite `any`; quando o tipo for genuinamente desconhecido, `unknown`.

---

## 8. Formatação

- Aspas simples; template literals para caminho com variável.
- Uma linha em branco entre métodos; helper privado por último.
- Imports: Angular → gems-sdk → rxjs → models locais.

---

## Anti-Patterns (NUNCA)

| Proibido | Correto |
| :--- | :--- |
| `import { BaseStore, Pageable } from 'gems-sdk'` | `import { GemsBaseStore, GemsPageable } from '@gabriel-mdias/angular-gems-sdk'` |
| `HttpClient` injetado no componente ou na Store | `GemsBaseStore.get/post/put/delete` |
| `super('/api/recurso')` (barra inicial) ou URL absoluta | `super('api/recurso')` + `provideGemsHttp` |
| `search(filter: object, ...)` | `search(filter: {Entidade}Filter, ...)` — interface declarada |
| `Observable<{ content: T[]; totalElements: number }>` | `Observable<Page<T>>` — a página inteira |
| `id: number` no tipo | `id: string` |
| `this.post(...)` sem generics | `this.post<TReturn, TBody>(path, body)` |
| Métodos colados, sem linha em branco | Uma linha em branco entre cada método |
