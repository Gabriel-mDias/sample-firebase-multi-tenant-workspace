---
origem: local
precedencia: local-vence
versao: 2.1.0
---

# Recipe: CRUD Ponta-a-Ponta

**Objetivo:** Implementar um CRUD completo desde a entidade JPA até o formulário Angular, cobrindo todas as camadas da GEMS SDK.

**Pré-requisitos:**
- `gems-jpa` (BaseCustomJpaRepository, soft delete)
- `gems-mapstruct` (`GemsMappingConfig`, `unmappedTargetPolicy = ERROR`)
- `gems-exception` (`BaseController`, envelope de resposta e `BusinessException`)
- `gems-sdk` Angular (GemsBaseStore, FormCardComponent, TableComponent)

**Rules relacionadas:**
- [architecture.md](../rules/backend-java/architecture.md)
- [controllers.md](../rules/backend-java/controllers.md)
- [services.md](../rules/backend-java/services.md)
- [repositories.md](../rules/backend-java/repositories.md)
- [persistence-db.md](../rules/backend-java/persistence-db.md)
- [mapping.md](../rules/backend-java/mapping.md)
- [stores.md](../rules/frontend-angular/stores.md)
- [lists.md](../rules/frontend-angular/lists.md)
- [forms.md](../rules/frontend-angular/forms.md)

## Resultado Esperado

- Entidade JPA persistida com soft delete, prefixos GEMS nas colunas e UUID como PK.
- Service com o **mapper do domínio** como primeiro campo, `@Transactional` nos métodos de escrita e `BusinessException` em falhas de negócio.
- Listagem Angular com `gems-table` paginada via `POST /search` e formulário com `gems-form-card` distinguindo criação de edição.

---

## Camadas 1 a 5: backend

Cada camada tem um **template** que já é a forma da regra — copie o template, não este documento:

| Camada | Template | Regra | O que muda por domínio |
| :--- | :--- | :--- | :--- |
| 1. Entidade JPA | [`EntityTemplate.java`](../templates/backend/EntityTemplate.java) | `architecture.md` §2, `persistence-db.md` §6 | tabela, schema, colunas com prefixo, enum |
| 1b. Changeset Liquibase | `persistence-db.md` §4 (SQL puro em `<sql><![CDATA[ ]]>`) | `persistence-db.md` | `changelog-<dominio>.xml` novo, incluído no `changelog-master.xml` |
| 2. DTOs (3) | [`DtoTemplate.java`](../templates/backend/DtoTemplate.java) | `architecture.md` §3 | espelho em `model.dto`, grid em `model.response`, filtro em `model.params` — **id é `UUID`** |
| 3. Mapper | `mapping.md` §2 | `mapping.md` | `toDTO`, `toEntity`, `toResponse`, `atualizar(@MappingTarget)` |
| 4. Repository | [`RepositoryTemplate.java`](../templates/backend/RepositoryTemplate.java) | `repositories.md` | `JpaRepository` + `BaseCustomJpaRepository`; tétrade `search`/`countQuery`/`searchQuery`/`appendFilters`; HQL, nunca `@Query` nativa |
| 5. Service | [`ServiceTemplate.java`](../templates/backend/ServiceTemplate.java) | `services.md` | validação acumulada; ordem canônica dos métodos |
| 6. Controller | [`ControllerTemplate.java`](../templates/backend/ControllerTemplate.java) | `controllers.md`, `security.md` | ação concreta de `AcaoSistema` em cada endpoint; `setBodyToExceptionLog( dto, request )` |

Fatos que os templates não carregam e a regra também não:

- **Soft delete é por `DT_EXCLUSAO`** (`@SQLDelete` faz `SET DT_EXCLUSAO = NOW()`), nunca por coluna
  booleana `ST_ATIVO`.
- **`dataCriacao` é atribuída na Service** (`dto.setDataCriacao( LocalDateTime.now() )` no `insert`),
  não por `@CreationTimestamp`.
- O Controller devolve o DTO **direto** — o `gems-exception` envelopa a resposta; não existe
  `ApiResponseDTO.of(...)` no código do projeto.
- Toda ação nova entra no enum `AcaoSistema` antes de aparecer em `@PreAuthorize`; o JSON do frontend
  é gerado dele no build.

## Camada 7: Store Angular

```typescript
@Injectable({ providedIn: 'root' })
export class {Entidade}Store extends GemsBaseStore {
  constructor() { super('api/{recurso}'); }

  getById(id: string): Observable<{Entidade}Model> {
    return this.get<{Entidade}Model>(`/${id}`);
  }

  search(filter: {Entidade}FilterModel, pageable: Pageable): Observable<{ content: {Entidade}Model[]; totalElements: number }> {
    return this.post<{ content: {Entidade}Model[]; totalElements: number }, {Entidade}FilterModel>('/search', filter, { pageable });
  }

  create(dto: {Entidade}Model): Observable<{Entidade}Model> {
    return this.post<{Entidade}Model, {Entidade}Model>('', dto);
  }

  update(id: string, dto: {Entidade}Model): Observable<{Entidade}Model> {
    return this.put<{Entidade}Model, {Entidade}Model>(`/${id}`, dto);
  }

  delete{Entidade}(id: string): Observable<void> {
    return this.delete<void>(`/${id}`);
  }
}
```

## Camada 8: Models TypeScript

```typescript
// {entidade}.model.ts
export interface {Entidade}Model {
  id?: string;
  nome?: string;
  dataCriacao?: string;
}

// {entidade}-filter.model.ts
export interface {Entidade}FilterModel {
  nome?: string;
}
```

## Camada 9: Listagem e Formulário Angular

Ver receitas:
- [search-paginated-end-to-end.md](./search-paginated-end-to-end.md) — listagem com filtros e paginação.
- Componente de formulário: [forms.md](../rules/frontend-angular/forms.md).

---

## Checklist de Conformidade

- [ ] Entidade com `@SQLDelete` + `@SQLRestriction` (soft delete).
- [ ] 3 DTOs: espelho (create/update) + ResponseDTO (listagem) + FilterParams (search).
- [ ] Service com ordem: `findById(String)` > `findById(UUID)` > `search` > `delete` > `insert` > `save` > `validate`.
- [ ] `ObjectUtil.isNullOrEmpty()` em toda validação — nunca `== null`.
- [ ] `validate()` acumula erros em `ArrayList<String>` + lança `BusinessException(errors)`.
- [ ] Controller estende `BaseController`, chama `super.setBodyToExceptionLog( dto, request )` nos mutantes e nomeia uma ação de `AcaoSistema` em cada endpoint.
- [ ] `POST /search` para listagem paginada (não `GET`).
- [ ] `POST` retorna `201 CREATED`; `PUT` e `DELETE` retornam `200` e `204`.
- [ ] Store com `super('api/{recurso}')` sem barra inicial.
- [ ] IDs sempre `string` no frontend.
