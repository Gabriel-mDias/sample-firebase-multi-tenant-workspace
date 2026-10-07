<!-- gems-ai-rule: {"id":"java.repositories","version":"2.0.0","status":"candidate"} -->
---
stack: backend-java
module: gems-jpa
severity: mandatory
see-also: [services.md, architecture.md, utils.md]
origem: local
precedencia: local-vence
versao: 2.1.0
---

# Repositories & HQL Queries — Java / Spring Boot

> **TL;DR:**
> - **Sempre** estender `JpaRepository<E, UUID>` E `BaseCustomJpaRepository<E>` (nesta ordem), com `@Repository`.
> - Trinca obrigatória: `search()` default público + `countQuery()` privado + `searchQuery()` privado + `appendFilters()` privado.
> - HQL `WHERE 1=1` + filtros condicionais via `ObjectUtil.isNotNullAndNotEmpty()`. **Nunca** `!= null`.
> - ResponseDTO via constructor expression: `SELECT new br.com.gems.{app}.model.response.{dominio}.{DTO}(campos...)`.
> - Aliases curtos: `e`, `c`, `p`, `a`, `i`, `cg`. JOINs idênticos em `countQuery` e `searchQuery`.
> - `@EntityGraph` em `findById` quando há relacionamentos a carregar eagerly.

## 1. Declaração da Interface

```java
@Repository
public interface {Entidade}Repository extends JpaRepository<{Entidade}, UUID>, BaseCustomJpaRepository<{Entidade}> {
```

### Regras:
- `JpaRepository` **primeiro**, `BaseCustomJpaRepository` **segundo** (gems-jpa).
- `@Repository` obrigatória.
- UUID como tipo do ID.

---

## 2. Trinca de Métodos de Search (Obrigatória)

Todo módulo com listagem paginada DEVE ter exatamente esta estrutura:

### 2.1 `search` — método default público
```java
default Page<{Entidade}ResponseDTO> search( {Entidade}FilterParams filterParams, Pageable pageable ) {
    return new PageImpl<>( this.searchQuery( filterParams, pageable ), pageable, this.countQuery( filterParams ) );
}
```

### 2.2 `countQuery` — privado
```java
private Long countQuery( {Entidade}FilterParams filterParams ) {
    var hql = new StringBuilder();
    var params = new HashMap<String, Object>();

    hql.append( " SELECT count(e.id) " );
    hql.append( " FROM {Entidade} e " );
    // JOINs (se houver) — idênticos ao searchQuery
    appendFilters( filterParams, hql, params );

    return this.executeCountHql( hql, params );
}
```

### 2.3 `searchQuery` — privado
```java
private List<{Entidade}ResponseDTO> searchQuery( {Entidade}FilterParams filterParams, Pageable pageable ) {
    var hql = new StringBuilder();
    var params = new HashMap<String, Object>();

    hql.append( " SELECT new br.com.gems.{app}.model.response.{dominio}.{Entidade}ResponseDTO( " );
    hql.append( "       e.id, " );
    hql.append( "       e.campo " );
    hql.append( " ) " );
    hql.append( " FROM {Entidade} e " );
    // JOINs — idênticos ao countQuery
    appendFilters( filterParams, hql, params );

    return this.executeHql( hql, params, pageable, {Entidade}ResponseDTO.class );
}
```

### 2.4 `appendFilters` — privado
```java
private void appendFilters( {Entidade}FilterParams filterParams, StringBuilder hql, HashMap<String, Object> params ) {
    hql.append( " WHERE 1=1 " );

    if ( ObjectUtil.isNotNullAndNotEmpty( filterParams.getTextoLivre() ) ) {
        hql.append( " AND LOWER(e.campo) LIKE :textoLivre " );
        params.put( "textoLivre", "%" + filterParams.getTextoLivre().toLowerCase() + "%" );
    }
}
```

---

## 3. Variantes do Search

### Retorno de Entity (módulos simples, sem JOIN)
```java
default Page<{Entidade}> search( {Entidade}FilterParams filterParams, Pageable pageable ) {
    return new PageImpl<>( this.searchQuery( filterParams, pageable ), pageable, this.countQuery( filterParams ) );
}

private List<{Entidade}> searchQuery( {Entidade}FilterParams filterParams, Pageable pageable ) {
    var hql = new StringBuilder();
    var params = new HashMap<String, Object>();
    hql.append( " SELECT e FROM {Entidade} e " );
    appendFilters( filterParams, hql, params );
    return this.<{Entidade}>executeHql( hql, params, pageable, {Entidade}.class );
}
```

### Retorno de ResponseDTO com JOINs
```java
hql.append( " FROM {Entidade} e " );
hql.append( " JOIN e.relacionamento r " );
hql.append( " JOIN r.categoria c " );
```

---

## 4. Tipos de Filtro no `appendFilters`

### String — LIKE case-insensitive
```java
if ( ObjectUtil.isNotNullAndNotEmpty( filterParams.getNome() ) ) {
    hql.append( " AND LOWER(e.nome) LIKE :nome " );
    params.put( "nome", "%" + filterParams.getNome().toLowerCase() + "%" );
}
```

### Enum — igualdade exata
```java
if ( ObjectUtil.isNotNullAndNotEmpty( filterParams.getSituacao() ) ) {
    hql.append( " AND e.situacao = :situacao " );
    params.put( "situacao", filterParams.getSituacao() );
}
```

### Range numérico
```java
if ( ObjectUtil.isNotNullAndNotEmpty( filterParams.getValorMin() ) ) {
    hql.append( " AND e.valor >= :valorMin " );
    params.put( "valorMin", filterParams.getValorMin() );
}
if ( ObjectUtil.isNotNullAndNotEmpty( filterParams.getValorMax() ) ) {
    hql.append( " AND e.valor <= :valorMax " );
    params.put( "valorMax", filterParams.getValorMax() );
}
```

### Range de datas
```java
if ( ObjectUtil.isNotNullAndNotEmpty( filterParams.getDataMin() ) ) {
    hql.append( " AND cast( e.dataCriacao as Date ) >= cast( :dataMin as Date ) " );
    params.put( "dataMin", filterParams.getDataMin() );
}
```

---

## 5. JOINs

```java
hql.append( " FROM {Entidade} e " );
hql.append( " JOIN e.organizacao o " );
hql.append( " JOIN o.responsavel r " );
hql.append( " JOIN e.categoria cg " );
```

### Regras de JOINs:
- Aliases curtos: `e`, `c`, `p`, `i`, `a`, `cg`.
- JOINs **idênticos** entre `countQuery` e `searchQuery`.
- `appendFilters` compartilhado entre os dois — deve funcionar para ambos.

---

## 6. EntityGraph para `findById` com Relacionamentos

```java
@Override
@EntityGraph(attributePaths = { "relacionamento", "relacionamento.categoria" })
Optional<{Entidade}> findById( UUID id );
```

Use quando o `findById` precisa carregar relacionamentos eagerly para evitar N+1.

---

## 7. Métodos JPA Derivados

Consultas simples por campo único:
```java
Optional<{Entidade}> findByCampoAAndCampoB( TipoA campoA, TipoB campoB );
boolean existsByCampo( String campo );
```

---

## 8. Formatação de Código

- **Espaço interno** em parênteses: `hql.append( " ... " )`, `params.put( "key", value )`.
- Cada `hql.append()` em sua própria linha.
- Espaço no **início e final** de cada string HQL: `" SELECT count(e.id) "`.
- `var` para `hql` e `params`.
- `WHERE 1=1` **sempre** — permite adição condicional de filtros sem `if/else` de concatenação.
- **SEMPRE** `ObjectUtil.isNotNullAndNotEmpty(...)` nos filtros, nunca `!= null`.

---

## Anti-Patterns (NUNCA)

| Proibido | Correto |
| :--- | :--- |
| `if (filterParams.getNome() != null)` em `appendFilters` | `ObjectUtil.isNotNullAndNotEmpty(filterParams.getNome())` |
| HQL sem `WHERE 1=1` inicial | `WHERE 1=1` sempre — permite filtros condicionais sem concatenação condicional |
| JOINs diferentes entre `countQuery` e `searchQuery` | JOINs **idênticos** — ambos compartilham `appendFilters` |
| Native SQL (`@Query(nativeQuery = true)`) | HQL sempre (portabilidade, type-safety) |
| `LIKE :nome` sem `LOWER()` | `AND LOWER(e.nome) LIKE :nome` com `:nome = "%" + valor.toLowerCase() + "%"` |
