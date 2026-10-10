<!-- gems-ai-rule: {"id":"java.services","version":"2.0.0","status":"candidate"} -->
---
stack: backend-java
module: gems-exception, gems-utils, gems-model-mapper
severity: mandatory
see-also: [repositories.md, validation.md, mapping.md, events.md]
origem: local
precedencia: local-vence
versao: 2.1.0
---

# Services — Java / Spring Boot

> **TL;DR:**
> - `@Service + @RequiredArgsConstructor`. Injeção: **o mapper do domínio** (1º, quando existir), `{Entidade}Repository` (2º), auxiliares depois.
> - Ordem: `findById(String)` > `findById(UUID)` > `findAll` > `search` > `delete` > `insert` > `save` > `validate` (private).
> - Validação: **SEMPRE** `ObjectUtil.isNullOrEmpty` / `isNotNullAndNotEmpty`. **NUNCA** `!= null`, `.isEmpty()` ou `StringUtils`.
> - Acumule erros em `ArrayList<String>` e lance UMA `BusinessException(errors)` no final.
> - `@Transactional(rollbackFor = Exception.class)` em `insert`, `save` e `delete`.
> - Métodos com >6 linhas de bloco coeso → extraia para método privado descritivo.
> - Subfluxos complexos → Spring Events (`events.md`). **NUNCA** `@Async` dentro da transação principal.

## 1. Declaração da Classe

```java
@Service
@RequiredArgsConstructor
public class {Entidade}Service {

    private final {Entidade}Mapper mapper;
    private final {Entidade}Repository repository;
    // Repositories auxiliares (se necessário)
    // Services auxiliares (se necessário)
```

### Regras:
- **O mapper do domínio como primeiro campo** — a interface `@Mapper(config = GemsMappingConfig.class)`
  de `mapping.md`. A âncora é **condicional**: service **sem** mapper do domínio não tem primeiro campo
  prescrito, e a regra não inventa um. Projeto que usa `ModelMapper` põe o bean dele nessa posição.
- Repository da própria entidade como **segundo** campo.
- Sem `@Autowired` — apenas `@RequiredArgsConstructor` + `private final`.

---

## 2. Ordem Fixa dos Métodos

1. `findById(String id)` — overload de conveniência
2. `findById(UUID id)` — método principal
3. `findAll()` — quando necessário
4. `search(FilterParams, Pageable)` — busca paginada
5. `delete(String id)` — overload de conveniência
6. `delete(UUID id)` — método principal
7. `insert(DTO)` — criação (POST → 201)
8. `save(DTO)` ou `save(UUID id, DTO)` — atualização (PUT → 200)
9. Métodos privados: `validate()`, helpers

---

## 3. `findById`

```java
// Overload de conveniência
public {Entidade}DTO findById( String id ) {
    return this.findById( ObjectUtil.isNullOrEmpty( id ) ? null : UUID.fromString( id ) );
}

// Método principal
public {Entidade}DTO findById( UUID id ) {
    if ( ObjectUtil.isNullOrEmpty( id ) ) {
        throw new BusinessException( "O id não foi informado!" );
    }
    return repository.findById( id )
            .map( mapper::toDTO )
            .orElseThrow( () -> new BusinessException( "{Entidade} não encontrado(a)!" ) );
}
```

---

## 4. `search`

```java
// Retornando ResponseDTO (módulos com JOIN)
public Page<{Entidade}ResponseDTO> search( {Entidade}FilterParams filterParams, Pageable pageable ) {
    return repository.search( filterParams, pageable );
}

// Retornando Entity mapeada (módulos simples)
public Page<{Entidade}DTO> search( {Entidade}FilterParams filterParams, Pageable pageable ) {
    return repository.search( filterParams, pageable )
            .map( mapper::toDTO );
}
```

---

## 5. `delete`

```java
@Transactional(rollbackFor = Exception.class)
public void delete( UUID id ) {
    if ( ObjectUtil.isNullOrEmpty( id ) ) {
        throw new BusinessException( "O id não foi informado!" );
    }
    if ( !repository.existsById( id ) ) {
        throw new BusinessException( "{Entidade} não encontrado(a)!" );
    }
    repository.deleteById( id );  // Soft delete automático via @SQLDelete na Entity
}
```

---

## 6. `insert` e `save`

```java
@Transactional(rollbackFor = Exception.class)
public {Entidade}DTO insert( {Entidade}DTO dto ) {
    if ( ObjectUtil.isNullOrEmpty( dto ) ) {
        throw new BusinessException( "Os dados não foram informados!" );
    }
    validate( dto );
    dto.setDataCriacao( LocalDateTime.now() );
    var entity = repository.save( mapper.toEntity( dto ) );
    return mapper.toDTO( entity );
}

@Transactional(rollbackFor = Exception.class)
public {Entidade}DTO save( UUID id, {Entidade}DTO dto ) {
    validate( dto );
    var entity = repository.findById( id )
            .orElseThrow( () -> new BusinessException( "{Entidade} não encontrado(a)!" ) );
    mapper.atualizar( dto, entity );  // @MappingTarget: id, dataCriacao e dataExclusao ficam como estão
    return mapper.toDTO( repository.save( entity ) );
}
```

---

## 7. Validação Acumulada (obrigatória)

```java
private void validate( {Entidade}DTO dto ) {
    var errors = new ArrayList<String>();

    if ( ObjectUtil.isNullOrEmpty( dto.getCampoObrigatorio() ) ) {
        errors.add( "O campo obrigatório é obrigatório." );
    }

    if ( ObjectUtil.isNullOrEmpty( dto.getRelacionamento() ) || ObjectUtil.isNullOrEmpty( dto.getRelacionamento().getId() ) ) {
        errors.add( "O relacionamento é obrigatório." );
    }

    // Validação condicional (não interrompe o acúmulo)
    if ( ObjectUtil.isNotNullAndNotEmpty( dto.getEmail() ) && !EmailUtil.isValid( dto.getEmail() ) ) {
        errors.add( "O e-mail informado é inválido!" );
    }

    if ( ObjectUtil.isNotNullAndNotEmpty( errors ) ) {
        throw new BusinessException( errors );
    }
}
```

### Regras de validação:
- **SEMPRE** use `ObjectUtil.isNullOrEmpty` / `ObjectUtil.isNotNullAndNotEmpty` (gems-utils).
- **NUNCA** use `== null`, `.isEmpty()`, `StringUtils`, `!= null`.
- Acumule **todos** os erros antes de lançar — uma única `BusinessException(errors)` ao final.
- Mensagens em **português**, finalizadas com `.` ou `!`.
- `validate` é sempre `private` (ou `private static`).

---

## 8. Delegação para Services Auxiliares

Em entidades com relacionamentos complexos, delegue persistência de sub-entidades para suas respectivas Services:

```java
@Transactional(rollbackFor = Exception.class)
public {Entidade}DTO insert( {Entidade}DTO dto ) {
    validate( dto );

    // Delega sub-entidades para suas respectivas Services
    if ( ObjectUtil.isNotNullAndNotEmpty( dto.getSubEntidade() ) && ObjectUtil.isNullOrEmpty( dto.getSubEntidade().getId() ) ) {
        var saved = subEntidadeService.insert( dto.getSubEntidade() );
        dto.getSubEntidade().setId( saved.getId() );
    }

    var entity = mapper.toEntity( dto );

    // Referência JPA sem carregar a entidade inteira (evita N+1)
    if ( ObjectUtil.isNotNullAndNotEmpty( dto.getRelacionadoId() ) ) {
        entity.setRelacionado( relacionadoRepository.getReferenceById( dto.getRelacionadoId() ) );
    }

    return mapper.toDTO( repository.save( entity ) );
}
```

---

## 9. Clean Code — Extração de Métodos Privados

Blocos com **mais de 6 linhas** fazendo uma única tarefa coesa devem ser extraídos para método privado descritivo:

```java
// PREFIRA:
horariosDTO.stream().forEach( dto -> mapToEntityAndSave( dto, entidadeRelacionada ) );

private void mapToEntityAndSave( HorarioDTO dto, EntidadeRelacionada entidadeRelacionada ) {
    var entity = new Horario();
    entity.setRelacionado( entidadeRelacionada );
    entity.setDiaSemana( dto.getDiaSemana() );
    entity.setHoraInicio( dto.getHoraInicio() );
    entity.setHoraTermino( dto.getHoraTermino() );
    entity.setDataCriacao( LocalDateTime.now() );
    repository.save( entity );
}
```

---

## 10. Formatação de Código

- `var` para variáveis locais cujo tipo é óbvio.
- Uma linha em branco entre blocos lógicos.
- Sem Javadoc em métodos CRUD padrão.
- `@Transactional(rollbackFor = Exception.class)` em **todas** as operações de escrita, e a
  anotação é a de **`org.springframework.transaction.annotation`**.

  > **Corrigido em 2026-09-11 (rodada 008).** Esta regra prescrevia `rollbackOn`, que é o atributo da
  > anotação de `jakarta.transaction`. Aqui a **regra estava errada, não o código**: os projetos da
  > família usam a do Spring, e um gate sobre a letra antiga reprovaria 100% dos métodos
  > transacionais existentes. O Princípio IV da constituição foi corrigido na mesma alteração — sem
  > isso, ele revogaria esta correção no dia seguinte.
- Para fluxos complexos pós-insert/save → Spring Events (veja `events.md`), nunca `@Async`.

---

## Anti-Patterns (NUNCA)

| Proibido | Correto |
| :--- | :--- |
| `dto.getNome() == null \|\| dto.getNome().isEmpty()` | `ObjectUtil.isNullOrEmpty(dto.getNome())` |
| `throw new BusinessException("erro A"); // depois: throw new BusinessException("erro B")` | Acumular em `ArrayList<String>` + lançar UMA `BusinessException(errors)` |
| Mapper do domínio como segundo ou último campo injetado | havendo mapper, ele é **sempre** o primeiro campo na Service |
| `@Transactional` de `jakarta.transaction` | o de `org.springframework.transaction.annotation`, com `rollbackFor` |
| `@Async` dentro de método `@Transactional` | Spring Events com `@TransactionalEventListener(AFTER_COMMIT)` |
| Bloco inline de 10+ linhas fazendo uma única coisa | Extrair para método `private` com nome descritivo |
