---
stack: backend-java
module: gems-bom, gems-jpa, gems-mapstruct
severity: mandatory
see-also: [controllers.md, services.md, repositories.md, persistence-db.md]
origem: local
precedencia: local-vence
versao: 2.1.0
---

# Backend Architecture — Java / Spring Boot

> **TL;DR:**
> - Camadas: `{app}-model` (Entities + DTOs) e `{app}-core` (Controllers + Services + Repositories).
> - Entidades: `@Builder @AllArgsConstructor @NoArgsConstructor @Getter @Setter @ToString @Entity @SQLDelete @SQLRestriction`.
> - DTOs: espelho (tráfego), FilterParams (busca — campos primitivos, nunca nested), ResponseDTO (flat para grid).
> - Imports: SEMPRE `import` no topo do arquivo. Única exceção: constructor expression em HQL.
> - Observabilidade: ative `gems-observability` para `@Observed`/Micrometer. Documentação via `gems-openapi`.

## 1. Organização de Módulos Maven

No Meduc, a constituição prevalece sobre o template de dois módulos. O reator tem cinco módulos:

```
backend/
├── meduc-model/      # Entidades, DTOs, filtros, responses e enums
├── meduc-security/   # Resource server, CORS, handlers e @PublicEndpoint
├── meduc-events/     # Somente POJOs de evento
├── meduc-core/       # Controllers, services, repositories e listeners
└── meduc-deploy/     # Executável, configuração e Liquibase
```

- Pacote raiz: `br.com.gems.meduc`.
- Dependências seguem `model/security → events/core → deploy`, sem ciclo.
- Somente `meduc-deploy` contém ambiente, migrations e empacotamento.
- Listener que chama regra de negócio fica em `meduc-core`, nunca em `meduc-events`.

---

## 2. Entidades JPA

### Anotações obrigatórias (nesta ordem)
```java
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "NOME_TABELA")
@SQLDelete(sql = "UPDATE NOME_TABELA SET DT_EXCLUSAO = NOW() WHERE ID_ENTIDADE = ?")
@SQLRestriction("DT_EXCLUSAO IS NULL")
public class {Entidade} {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID_ENTIDADE")
    private UUID id;

    @Column(name = "DS_CAMPO", nullable = false)
    private String campo;

    // Propriedade virtual calculada pelo banco (não persistida, evita N+1 em junções simples)
    @Formula(" CONCAT(DS_PRIMEIRO_NOME, ' ', DS_SOBRENOME) ")
    private String nomeCompleto;

    @Column(name = "DT_CRIACAO")
    private LocalDateTime dataCriacao;

    @Column(name = "DT_EXCLUSAO")
    private LocalDateTime dataExclusao;
}
```

### Regras de entidade:
- Soft delete automático via `@SQLDelete` + `@SQLRestriction` — NUNCA implemente delete físico manual.
- Toda tabela inclui `DT_CRIACAO` e `DT_EXCLUSAO`.
- Use `@Formula` para propriedades calculadas simples (evita overhead de JOIN ou processamento no Java).
- Relacionamentos: use `@ManyToOne(fetch = FetchType.LAZY)` por padrão.

---

## 3. DTOs (três tipos obrigatórios)

### DTO Espelho (tráfego de dados — módulo model, pacote `model.dto.{dominio}`)
Reflete a entidade, usado para transport entre camadas. Anotações `@Data @Builder @AllArgsConstructor @NoArgsConstructor`.

### FilterParams (busca — módulo model, pacote `model.params.{dominio}`)
DTO de filtro: **campos primitivos/String APENAS**, nunca objetos aninhados. Campos de range usam sufixo `Min`/`Max`.

```java
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class {Entidade}FilterParams {
    private String textoLivre;
    private TipoEnum situacao;
    private BigDecimal valorMin;
    private BigDecimal valorMax;
    private LocalDate dataMin;
    private LocalDate dataMax;
}
```

### ResponseDTO (flat para grid — módulo model, pacote `model.response.{dominio}`)
Resultado plano do `search`, com campos de JOINs achatados. Construído via constructor expression no HQL.

```java
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class {Entidade}ResponseDTO {
    private UUID id;
    private String campo;
    // Campos de JOIN achatados:
    private String relacionadoNome;
    private String relacionadoTipo;
}
```

---

## 4. Importações e Nomes Qualificados

- **SEMPRE** declare `import` no topo do arquivo e use apenas o nome simples da classe.
- **Única exceção:** constructor expression em HQL — `SELECT new br.com.gems.{app}.model.response.{dominio}.{ResponseDTO}(...)` — onde o Hibernate exige o caminho qualificado completo.

---

## 5. Módulos do Java-GEMS-SDK consumidos nessa camada

Versão corrente da SDK Java: **`3.0.0`**, publicada no registry em 2026-09-11.

| Módulo | Função |
| :--- | :--- |
| `gems-bom` | Alinha versões de todos os módulos via BOM |
| `gems-mapstruct` | `GemsMappingConfig` com `unmappedTargetPolicy = ERROR` — o mapeamento Entity↔DTO recomendado |
| `gems-model-mapper` | ModelMapper pré-configurado. **Suportado, não recomendado para código novo** — ver `mapping.md` §6 |
| `gems-jpa` | `BaseCustomJpaRepository` para HQL customizado |
| `gems-jpa-multi-tenant` | Multi-tenancy schema-based (quando aplicável) |
| `gems-auditing` | Trilha declarativa por `@Auditable` / `@SensitiveField` |
| `gems-security-authorization` | Autorização por ação, com catálogo derivado de enum |
| `gems-keycloak-admin` | Cliente administrativo do provedor de identidade |
| `gems-exception` | Envelope uniforme de erro |
| `gems-openapi` | Swagger UI auto-configurado (springdoc-openapi) |
| `gems-observability` | `@Observed` / Micrometer via `ObservedAspect` |

Para setup de consumo, veja `recipes/setup-backend-sdk.md`.

**Mecanismo que um destes módulos entrega não é reimplementado no projeto.** Quando a implementação
local já existe, a substituição é autorizada pela seção **Adoção de Módulo da SDK GEMS** da
constituição e tem preço: módulo publicado no registry, versão declarada só no POM pai, alcance
medido com `dependency:tree` antes e depois, a garantia do mecanismo antigo nomeada e provada por
teste **neste** repositório, varredura que quebra o build onde a falha é silenciosa, e a classe local
removida na mesma alteração — nunca as duas convivendo.

---

## Anti-Patterns (NUNCA)

| Proibido | Correto |
| :--- | :--- |
| Lógica de negócio em `{app}-model` | `{app}-model` contém apenas Entities, DTOs e Enums |
| `FilterParams` com objetos aninhados (`RelacionadoDTO`) | Apenas primitivos: `String`, `Enum`, `LocalDate`, `BigDecimal` |
| Nomes qualificados fora do HQL (`br.com.gems.{app}.model.Entidade`) | `import` no topo, usar apenas nome simples |
| DELETE físico sem `@SQLDelete` | `@SQLDelete` + `@SQLRestriction` em toda entidade |
