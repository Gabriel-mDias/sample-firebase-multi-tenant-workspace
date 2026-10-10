<!-- gems-ai-rule: {"id":"java.openapi","version":"2.0.0","status":"candidate"} -->
---
stack: backend-java
module: gems-openapi
severity: recommended
see-also: [controllers.md, security.md]
origem: local
precedencia: local-vence
versao: 2.1.0
---

# OpenAPI / Swagger — gems-openapi

> **TL;DR:**
> - `gems-openapi` auto-configura springdoc-openapi + Swagger UI — sem configuração adicional além do `pom.xml`.
> - Acesse `/swagger-ui.html` ou `/v3/api-docs` após adicionar a dependência.
> - Use `@Operation` e `@Tag` nos Controllers; `@Schema` nos DTOs.
> - **Nunca** exponha Swagger UI em produção sem autenticação.

## 1. Setup

Adicione ao `pom.xml` do módulo `{app}-core`:

```xml
<dependency>
    <groupId>br.com.gems</groupId>
    <artifactId>gems-openapi</artifactId>
</dependency>
```

O Swagger UI estará disponível em `http://localhost:{porta}/swagger-ui.html` sem configuração adicional.

---

## 2. Personalização no application.yml

```yaml
gems:
  openapi:
    title: "{App} API"
    version: "1.0.0"
    description: "API REST do {app}"
    contact:
      name: "Time GEMS"
      email: "contato@gems.com.br"
    servers:
      - url: "http://localhost:8080"
        description: "Desenvolvimento"
      - url: "https://api.{app}.com.br"
        description: "Produção"
```

---

## 3. Enriquecendo Controllers com Anotações

```java
@Tag(name = "{Entidade}", description = "Gerenciamento de {entidades}")
@RestController
@RequestMapping("/api/{recurso}")
@RequiredArgsConstructor
public class {Entidade}Controller extends BaseController {

    @Operation(summary = "Busca {entidade} por ID", description = "Retorna os dados completos de uma {entidade} pelo seu UUID.")
    @GetMapping("/{id}")
    public ResponseEntity<{Entidade}DTO> findById( @PathVariable UUID id ) {
        return ResponseEntity.ok( service.findById( id ) );
    }

    @Operation(summary = "Pesquisa paginada de {entidades}")
    @PostMapping("/search")
    public ResponseEntity<Page<{Entidade}ResponseDTO>> search( ... ) { ... }

    @Operation(summary = "Cria nova {entidade}", responses = {
        @ApiResponse(responseCode = "201", description = "Criado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos (BusinessException)")
    })
    @PostMapping
    public ResponseEntity<{Entidade}DTO> insert( ... ) { ... }
}
```

---

## 4. Documentando DTOs

```java
public class {Entidade}DTO {

    @Schema(description = "ID único (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "Nome completo", example = "João da Silva", required = true)
    private String nome;
}
```

---

## 5. Regras

- Anotações `@Operation` e `@Tag` são **opcionais** em ambiente de desenvolvimento — úteis para APIs públicas ou compartilhadas com times de frontend.
- Não exponha endpoints de Swagger em produção sem autenticação: configure `springdoc.swagger-ui.enabled=false` em profiles de produção ou proteja via Security.
- Mantenha os `summary` curtos (5–10 palavras) e use `description` para detalhes.

---

## Anti-Patterns (NUNCA)

| Proibido | Correto |
| :--- | :--- |
| Swagger UI exposto em produção sem autenticação | `springdoc.swagger-ui.enabled=false` em profile de produção |
| `@Operation(summary = "Endpoint que recebe filtros e retorna uma lista paginada")` | `@Operation(summary = "Pesquisa paginada de {entidades}")` |
| Configurar manualmente `springdoc-openapi` sem usar `gems-openapi` | Adicionar `gems-openapi` no `pom.xml` — auto-configura tudo |
