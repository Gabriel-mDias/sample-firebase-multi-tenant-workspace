---
stack: backend-java
module: gems-mapstruct
severity: mandatory
see-also: [services.md, architecture.md, repositories.md]
origem: local
precedencia: local-vence
versao: 2.1.0
---

# Mapeamento Entity ↔ DTO — gems-mapstruct

> **TL;DR:**
> - Interface anotada com `@Mapper(config = GemsMappingConfig.class)`, injetada na Service.
> - `GemsMappingConfig` traz `unmappedTargetPolicy = ERROR`: **alvo sem origem não compila**.
> - O mapper do domínio é o **primeiro campo** injetado na Service.
> - Renomeação se declara com `@Mapping(target = ..., source = ...)`, uma por campo.
> - `@Mapping(ignore = true)` é último recurso, e carrega a razão escrita ao lado.
> - `ModelMapper` continua **suportado** — ver §6. O que muda é o recomendado, não o permitido.

---

## 1. Por que MapStruct, e o que ele pega que o anterior não pegava

O mapeamento manual e o mapeamento por reflexão compartilham o mesmo defeito, e ele é **silencioso**:
campo novo na entidade **não quebra nada**. Quem acrescenta uma coluna e esquece de passá-la ao DTO
vê a aplicação compilar, os testes passarem, e o campo chegar nulo na tela.

Com `GemsMappingConfig`, um alvo sem origem **reprova a compilação**. Cada exceção precisa ser
declarada, e cada declaração é uma decisão de campo com a razão ao lado — nunca um `ignore` para
calar o compilador.

---

## 2. Declaração da Interface

```java
@Mapper(config = GemsMappingConfig.class)
public interface {Entidade}Mapper {

    {Entidade}DTO toDTO( {Entidade}Entity entity );

    {Entidade}Entity toEntity( {Entidade}DTO dto );

    {Entidade}ResponseDTO toResponse( {Entidade}Entity entity );

    // Atualização em lugar: identidade e auditoria pertencem ao registro persistido.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dataCriacao", ignore = true)
    @Mapping(target = "dataExclusao", ignore = true)
    void atualizar( {Entidade}DTO dto, @MappingTarget {Entidade}Entity entity );
}
```

### Regras:
- `config = GemsMappingConfig.class` **sempre** — é ele que traz `unmappedTargetPolicy = ERROR` e o
  `componentModel` do Spring. Sem ele, o mapper volta a ser silencioso.
- A interface vive em `{dominio}/mapper/`, junto do resto do domínio.
- Um método por par (entidade, DTO). Nome no padrão `to{Alvo}`; a atualização em lugar usa
  `@MappingTarget` e chama-se `atualizar`. A Service chama esses métodos tipados — nunca
  `mapper.map( origem, Alvo.class )`, que é a API do `ModelMapper`.
- **Não** escreva a implementação: ela é gerada em tempo de compilação.

---

## 3. Injeção na Service

```java
@Service
@RequiredArgsConstructor
public class {Entidade}Service {
    private final {Entidade}Mapper mapper;       // Primeiro campo — sempre
    private final {Entidade}Repository repository;
```

O mapper do domínio ocupa a **primeira posição** entre os campos injetados. A âncora é
**condicional**: service que não tem mapper do domínio não tem primeiro campo prescrito — o que a
regra exige é que, **havendo** mapper, ele venha primeiro. É o que torna a ordem de leitura do
arquivo previsível e a verificação barata.

---

## 4. Renomeação de campo

Quando origem e destino não têm o mesmo nome, cada par se declara:

```java
/**
 * O {@code User} do provedor de identidade não usa os nomes do domínio. São quatro renomeações
 * declaradas — e é por causa delas que o mapeamento se paga: o construtor manual aceitava qualquer
 * ordem de {@code String}, e trocar login por e-mail compilava sem reclamar.
 */
@Mapping(target = "userId", source = "id")
@Mapping(target = "nomeCompleto", source = "name")
@Mapping(target = "login", source = "username")
@Mapping(target = "habilitado", source = "enabled")
GestorResponseDTO toGestor( IdentityGateway.User user );
```

---

## 5. A armadilha do `annotationProcessorPaths`

Ela é de configuração de build, e custa caro quando aparece: o
`<annotationProcessorPaths>` **substitui** o classpath de processadores em vez de complementá-lo.
O processador que chega por dependência transitiva é simplesmente **ignorado**, e o mapper não é
gerado.

A ordem também é contrato: Lombok, depois `lombok-mapstruct-binding`, depois `mapstruct-processor`.
O MapStruct precisa enxergar os getters que o Lombok gera; com a ordem trocada, o mapper compila
lendo uma classe ainda sem acessores — e o campo chega nulo em silêncio, que é exatamente o defeito
que `unmappedTargetPolicy = ERROR` existe para impedir.

A configuração canônica está no POM pai (`backend/pom.xml`), nas duas execuções do compilador.

---

## 6. `ModelMapper` — suportado, não recomendado

`gems-model-mapper` **continua de primeira classe na SDK**: nenhum `@Deprecated`, nenhuma remoção.
Projetos que o usam hoje — o repositório pai e a ADACI, com 37 usos medidos — estão **corretos**, e
esta regra não declara o código deles errado.

| Situação | O que usar |
| :--- | :--- |
| Código novo | **`gems-mapstruct`** |
| Domínio que já usa `ModelMapper` e não está sendo reescrito | `ModelMapper`, sem pressa de migrar |
| Domínio em reescrita | migra para MapStruct na mesma passada |

Quando `ModelMapper` for o mecanismo do projeto, ele é o bean injetado — **nunca** `new
ModelMapper()` — e ocupa a posição de primeiro campo que o mapper do domínio ocuparia.

---

## Anti-Patterns (NUNCA)

| Proibido | Correto |
| :--- | :--- |
| `@Mapper` sem `config = GemsMappingConfig.class` | com o config — sem ele não há `unmappedTargetPolicy = ERROR` |
| `@Mapping(ignore = true)` para calar o compilador | declarar a origem, ou escrever a razão do `ignore` ao lado |
| Escrever a classe de implementação do mapper | ela é gerada; escrever à mão desfaz a verificação |
| Construtor de DTO montado à mão na Service | método no mapper do domínio |
| Repetir o `componentModel` em cada `@Mapper` | ele vem do `GemsMappingConfig` |
| `new ModelMapper()` em projeto que usa ModelMapper | injetar o bean |
