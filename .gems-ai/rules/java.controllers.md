<!-- gems-ai-rule: {"id":"java.controllers","version":"2.0.0","status":"candidate"} -->
---
stack: backend-java
module: gems-exception
severity: mandatory
see-also: [services.md, rest-common.md, openapi.md, validation.md]
origem: local
precedencia: local-vence
versao: 2.1.0
---

# Controllers — Java / Spring Boot

> **TL;DR:**
> - `@RestController + @RequestMapping("/api/{recurso}")` + `@RequiredArgsConstructor`. Estende `BaseController`.
> - Apenas 1 Service injetada. Zero lógica de negócio no Controller.
> - Status HTTP: GET=200, POST(insert)=201, PUT=200, DELETE=204.
> - **Obrigatório:** `super.setBodyToExceptionLog(dto, request)` em TODO POST/PUT com `@RequestBody`.
> - Ordem dos métodos: `findById > findAll > search > insert > save > delete`.
> - Espaço interno em parênteses de **método e de controle** (`search( params, pageable )`, `if ( ... )`); **sem** espaço em anotação (`@GetMapping("/{id}")`), cast e generics.

## 1. Declaração da Classe

```java
@RestController
@RequestMapping("/api/{recurso-kebab-case}")
@RequiredArgsConstructor
public class {Entidade}Controller extends BaseController {

    private final {Entidade}Service service;
```

### Regras obrigatórias:
- **Estende** `BaseController` (de `br.com.gems.exception.base.BaseController`).
- Injeção exclusivamente via `@RequiredArgsConstructor` + `private final`.
- **UMA única** dependência de Service — ZERO lógica de negócio no controller.
- Annotations na ordem exata acima.

---

## 2. Métodos Padrão e Ordem Fixa

Escreva os métodos nesta ordem, omitindo os que não se aplicam:

### 2.1 `findById` — GET /{id}
```java
@GetMapping("/{id}")
public ResponseEntity<{Entidade}DTO> findById( @PathVariable UUID id ) {
    return ResponseEntity.ok( service.findById( id ) );
}
```

### 2.2 `findAll` — GET / (quando necessário)
```java
@GetMapping
public ResponseEntity<List<{Entidade}DTO>> findAll() {
    return ResponseEntity.ok( service.findAll() );
}
```

### 2.3 `search` — POST /search
```java
@PostMapping("/search")
public ResponseEntity<Page<{Entidade}ResponseDTO>> search(
        @RequestBody {Entidade}FilterParams filterParams,
        @PageableDefault(page = 0, size = 10, sort = "{campoPadrao}", direction = Sort.Direction.ASC)
        Pageable pageable ) {
    return ResponseEntity.ok( service.search( filterParams, pageable ) );
}
```

### 2.4 `insert` — POST /
```java
@PostMapping
public ResponseEntity<{Entidade}DTO> insert( @RequestBody {Entidade}DTO dto, HttpServletRequest request ) {
    super.setBodyToExceptionLog( dto, request );
    return ResponseEntity.status( HttpStatus.CREATED ).body( service.insert( dto ) );
}
```

### 2.5 `save` — PUT /{id}
```java
@PutMapping("/{id}")
public ResponseEntity<{Entidade}DTO> save( @PathVariable UUID id, @RequestBody {Entidade}DTO dto,
        HttpServletRequest request ) {
    super.setBodyToExceptionLog( dto, request );
    return ResponseEntity.ok( service.save( id, dto ) );
}
```

### 2.6 `delete` — DELETE /{id}
```java
@DeleteMapping("/{id}")
public ResponseEntity<Void> delete( @PathVariable UUID id ) {
    service.delete( id );
    return ResponseEntity.noContent().build();
}
```

---

## 3. HTTP Status Codes

| Operação | Status | Como retornar |
| :--- | :--- | :--- |
| GET (findById, findAll, search) | `200 OK` | `ResponseEntity.ok(...)` |
| POST insert/create | `201 CREATED` | `ResponseEntity.status(HttpStatus.CREATED).body(...)` |
| PUT update/save | `200 OK` | `ResponseEntity.ok(...)` |
| DELETE | `204 NO CONTENT` | `ResponseEntity.noContent().build()` |

---

## 4. Endpoints de Ação de Negócio

Para operações de negócio específicas além do CRUD:

```java
@PutMapping("/acao-especifica/{id}")
public ResponseEntity<Void> nomeAcao( @PathVariable UUID id ) {
    service.nomeAcao( id );
    return ResponseEntity.ok().build();
}
```

- Path em **kebab-case** descritivo: `/abertura-contrato`, `/suspensao-acesso`.
- Nome do método em camelCase refletindo a ação: `aberturaContrato`.

---

## 5. Formatação de Código

- **Espaço interno** em parênteses de invocação, declaração e estrutura de controle:
  `service.findById( id )`, `public ResponseEntity<Void> delete( @PathVariable UUID id )`, `if ( ... )`.
- **Sem espaço interno** em parênteses de annotation, cast e expressão entre parênteses:
  `@RequestMapping("/api/recurso")`, `(UUID) id`. É o que o `eclipse-formatter.xml` do projeto impõe
  no Spotless — exemplo que diverge dele é exemplo a corrigir.
- **Sem espaço interno** em generics: `ResponseEntity<Page<{Entidade}ResponseDTO>>`.
- Estilo universal em `java.formatting`: sem teto de colunas; assinaturas e instruções comuns em linha única, indentação de 4 espaços e chaves na mesma linha.
- Imports: preservar ordem e grupos existentes; o executor n?o ordena imports.
- Uma linha em branco entre cada método.
- Sem Javadoc ou comentários em métodos CRUD padrão.

---

## Anti-Patterns (NUNCA)

| Proibido | Correto |
| :--- | :--- |
| Lógica de negócio no Controller (cálculos, condicionais de domínio) | Delegar 100% para a Service |
| Omitir `setBodyToExceptionLog(dto, request)` em POST/PUT | Chamar em **todo** endpoint mutante com `@RequestBody` |
| `GET /api/{recurso}?nome=x&status=y` para listagens | `POST /api/{recurso}/search` com `@RequestBody FilterParams` |
| Mais de uma Service injetada no Controller | Uma única dependência de Service por Controller |
| `return ResponseEntity.ok(service.insert(dto))` sem status 201 | `ResponseEntity.status(HttpStatus.CREATED).body(...)` |

---

## 6. Template Completo

```java
package br.com.gems.{app}.{dominio}.controller;

import br.com.gems.exception.base.BaseController;
import br.com.gems.{app}.model.params.{dominio}.{Entidade}FilterParams;
import br.com.gems.{app}.model.response.{dominio}.{Entidade}ResponseDTO;
import br.com.gems.{app}.{dominio}.service.{Entidade}Service;
import br.com.gems.{app}.model.dto.{dominio}.{Entidade}DTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/{recurso}")
@RequiredArgsConstructor
public class {Entidade}Controller extends BaseController {

    private final {Entidade}Service service;

    @GetMapping("/{id}")
    public ResponseEntity<{Entidade}DTO> findById( @PathVariable UUID id ) {
        return ResponseEntity.ok( service.findById( id ) );
    }

    @PostMapping("/search")
    public ResponseEntity<Page<{Entidade}ResponseDTO>> search(
            @RequestBody {Entidade}FilterParams filterParams,
            @PageableDefault(page = 0, size = 10, sort = "dataCriacao", direction = Sort.Direction.DESC)
            Pageable pageable ) {
        return ResponseEntity.ok( service.search( filterParams, pageable ) );
    }

    @PostMapping
    public ResponseEntity<{Entidade}DTO> insert( @RequestBody {Entidade}DTO dto, HttpServletRequest request ) {
        super.setBodyToExceptionLog( dto, request );
        return ResponseEntity.status( HttpStatus.CREATED ).body( service.insert( dto ) );
    }

    @PutMapping("/{id}")
    public ResponseEntity<{Entidade}DTO> save( @PathVariable UUID id, @RequestBody {Entidade}DTO dto,
            HttpServletRequest request ) {
        super.setBodyToExceptionLog( dto, request );
        dto.setId( id );
        return ResponseEntity.ok( service.save( dto ) );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete( @PathVariable UUID id ) {
        service.delete( id );
        return ResponseEntity.noContent().build();
    }
}
```
