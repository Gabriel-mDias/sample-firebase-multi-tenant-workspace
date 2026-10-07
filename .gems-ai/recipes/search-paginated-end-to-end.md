---
origem: local
precedencia: local-vence
versao: 2.1.0
---

# Recipe: Search Paginado Ponta-a-Ponta

**Objetivo:** Implementar uma consulta paginada completa, da HQL no repositório até a `gems-table` com filtros, paginação, ordenação e skeleton de loading.

**Pré-requisitos:**
- `gems-jpa` (BaseCustomJpaRepository)
- `gems-rest-common` (PageResponseDTO, Pageable)
- `gems-sdk` Angular (TableComponent, FormCardComponent, GemsBaseStore)

**Rules relacionadas:**
- [repositories.md](../rules/backend-java/repositories.md)
- [services.md](../rules/backend-java/services.md)
- [controllers.md](../rules/backend-java/controllers.md)
- [rest-common.md](../rules/backend-java/rest-common.md)
- [stores.md](../rules/frontend-angular/stores.md)
- [lists.md](../rules/frontend-angular/lists.md)
- [gems-components.md](../rules/frontend-angular/gems-components.md)

## Resultado Esperado

- Endpoint `POST /api/{recurso}/search` recebendo `FilterParams` + `Pageable` e retornando `Page<ResponseDTO>`.
- Repository com a tétrade HQL (`search` default + `countQuery` + `searchQuery` + `appendFilters`), sem hardcoded de filtros opcionais.
- Listagem Angular com `gems-table` paginada, ordenável, com filtros via `gems-form-card` e skeleton de loading via `@if (isLoading())`.

---

## Passos 1 a 5: backend

A forma está nos templates e na regra — este documento não a repete:

| Passo | Onde | O que fixa |
| :--- | :--- | :--- |
| 1. `{Entidade}ResponseDTO` | [`DtoTemplate.java`](../templates/backend/DtoTemplate.java), `architecture.md` §3 | `model.response.{dominio}`; plano, com os campos de JOIN achatados; `id` é `UUID`; enum tipado |
| 2. `{Entidade}FilterParams` | mesmo template, `architecture.md` §3 | `model.params.{dominio}`; só primitivos, String, enum e datas; range com sufixo `Min`/`Max` |
| 3. Repository | [`RepositoryTemplate.java`](../templates/backend/RepositoryTemplate.java), `repositories.md` §2–§4 | tétrade `search`/`countQuery`/`searchQuery`/`appendFilters` em HQL com `StringBuilder` + `HashMap`; **nunca** `@Query` com `countQuery` nem parâmetros soltos — o filtro entra inteiro, e `appendFilters` decide o que vai para o `WHERE 1=1` |
| 4. Service | [`ServiceTemplate.java`](../templates/backend/ServiceTemplate.java), `services.md` §4 | `search( filterParams, pageable )` delega ao repository, sem desmontar o filtro |
| 5. Controller | [`ControllerTemplate.java`](../templates/backend/ControllerTemplate.java), `controllers.md` §2.3 | `POST /search` com `@RequestBody FilterParams` + `@PageableDefault`; devolve `Page<ResponseDTO>` direto — o `gems-exception` envelopa |

Os tipos de filtro (LIKE case-insensitive, enum, range numérico, range de data) estão em
`repositories.md` §4, cada um com o `ObjectUtil` que o guarda.

## Passo 6: Store Angular

```typescript
// {entidade}.store.ts
search(filter: {Entidade}FilterModel, pageable: Pageable): Observable<{ content: {Entidade}ResponseModel[]; totalElements: number }> {
  return this.post<{ content: {Entidade}ResponseModel[]; totalElements: number }, {Entidade}FilterModel>(
    '/search',
    filter,
    { pageable }
  );
}
```

## Passo 7: Componente de Listagem

Ver template completo em [lists.md](../rules/frontend-angular/lists.md).

Pontos críticos:
1. `isLoading = signal(false)` — controla o skeleton.
2. `@if (isLoading()) { spinner } @else { gems-table }` — **nunca** `*ngIf`.
3. `onPageChange(newPageable)` — atualiza `this.pageable` e chama `loadEntidades()`.
4. `search()` — reseta `pageable.page = 0` antes de pesquisar.

```html
<!-- Skeleton de loading obrigatório antes da tabela -->
@if (isLoading()) {
  <div class="loading-state">
    <i class="fa-solid fa-circle-notch fa-spin"></i> Carregando {entidades}...
  </div>
} @else {
  <gems-table
    [columns]="columns"
    [data]="entidades()"
    [actions]="actions"
    [totalRecords]="totalRecords()"
    [page]="pageable.page ?? 0"
    [size]="pageable.size ?? 10"
    [sortField]="currentSortField"
    [sortDirection]="currentSortDirection"
    emptyMessage="Nenhum(a) {entidade} encontrado(a)."
    (actionClick)="handleAction($event)"
    (pageChange)="onPageChange($event)">
  </gems-table>
}
```

---

## Checklist de Conformidade

- [ ] `FilterParams` sem campos desnecessários (sem entidade completa).
- [ ] `ResponseDTO` com apenas os campos exibidos na tabela.
- [ ] Repository com trinca: `search()` + `countQuery` + `appendFilters()`.
- [ ] `WHERE 1=1` como âncora dos filtros dinâmicos.
- [ ] `ObjectUtil.isNotNullAndNotEmpty()` em cada filtro — nunca `!= null`.
- [ ] Controller usa `POST /search` (não `GET`) com `@RequestBody`.
- [ ] Store tipada com generics (`Observable<{ content: T[]; totalElements: number }>`).
- [ ] `isLoading` é um `signal<boolean>`.
- [ ] Template usa `@if` (não `*ngIf`).
- [ ] `onPageChange` atualiza `pageable` e rechama `loadEntidades()`.
- [ ] `search()` reseta `pageable.page = 0`.
