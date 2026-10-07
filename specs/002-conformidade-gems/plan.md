# Implementation Plan: Conformidade GEMS

**Branch**: `feature/conformidade-gems` | **Date**: 2026-09-14 | **Spec**: [spec.md](spec.md)

## Summary

Zerar a dívida Java, levar o frontend ao mínimo medido e adotar somente módulos SDK cujas garantias
publicadas cubram o comportamento atual. Auditoria e Keycloak ficam bloqueados até D-P01/D-P02; a
recomendação é preservar comportamento por evolução/publicação prévia das SDKs.

## Technical Context

**Language/Version**: Java 21; TypeScript 6.0; Angular 22.1
**Dependencies**: Spring Boot 4.1.1; GEMS Java 3.1.0 atual; Angular GEMS 2.1.0; Keycloak 26
**Storage**: PostgreSQL 16; Liquibase; schemas `public`, `security`, `auditoria`
**Testing**: JUnit 5, Spring/Testcontainers, Mockito para colaboração, ArchUnit, Vitest, scripts Node
**Platform**: JVM/Linux e browser; Docker Compose/Keycloak config CLI
**Constraints**: single tenant; zero dívida Java; frontend residual só com aceite humano; nenhuma API presumida
**Baseline**: 204 testes backend; 21 arquivos/118 testes frontend; 314/22, 189 e 174 ocorrências

**Release produtora**: Java GEMS SDK **3.2.0 MINOR**, decidida por SemVer nesta revisão porque o
desenho é estritamente aditivo sobre 3.1.0. Qualquer quebra pública descoberta invalida a decisão e
exige novo portão para MAJOR.

## Constitution Check

### Gate antes do desenho

| Princípio constitucional real | Estado | Avaliação |
| :--- | :--- | :--- |
| I. Fronteiras de Módulo Impostas pelo Reator Maven | PASS | SDK entra no módulo consumidor; grafo `security/model/events → core → deploy` não ganha ciclo. |
| II. Persistência Declarada, Nunca Inferida | **PASS DE PORTÃO; BLOQUEADO POR RELEASE** | D-P01 preserva identidade/correlação; o bloco espera SDK compatível publicada. |
| III. Changelog Liquibase Organizado e Rastreável | PASS DE DESENHO | Ordem, preconditions, backfill e teste de upgrade estão exigidos; execução virá na fase 3. |
| IV. Validação Acumulada e Ordem Canônica de Serviço | PASS DE DESENHO | Seis services, validação e ordem serão corrigidos; gates Java devem zerar. |
| V. Contrato REST e Autorização Espelhada na UI | **PASS DE PORTÃO; BLOQUEADO POR RELEASE** | D-P02 preserva ciclo/bootstrap; o bloco espera SDK compatível publicada. |
| VI. Frontend Modular e Padronizado pela GEMS SDK | **PASS DE DESENHO** | Correções estão definidas; D-P04 continua no portão posterior à medição. |
| VII. Testes como Gate (NÃO NEGOCIÁVEL) | PASS DE DESENHO | Testes primeiro, mutações e rollbacks integrados sem transação de teste estão especificados. |
| VIII. Especificação Antes de Código, Sem Sobras de Exemplo | PASS | Fase 2 documental concluída e portão assinado antes do código. |
| IX. Configuração de Ambiente é Código | PASS | D-P03 não se aplica; ambiente/realm preservados. T004 autoriza somente os três arquivos operacionais nominais. |

### Gate de adoção de módulos

| Módulo | Publicado | Alcance medido | Garantia equivalente | Estado |
| :--- | :--- | :--- | :--- | :--- |
| `gems-mapstruct` | sim, 3.1.0 | a medir antes/depois | sim | apto após teste local |
| `gems-security-authorization` | sim, 3.1.0 | a medir | sim para catálogo/scan | apto após teste local |
| `gems-auditing` | sim, 3.1.0 | fonte aferida | **não** para id/correlação | bloqueado D-P01 |
| `gems-keycloak-admin` | sim, 3.1.0 | fonte aferida | **não** para ciclo/roles | bloqueado D-P02 |
| Angular GEMS | sim, 2.1.0 | peers aferidos | a provar por componente | apto condicional |

Antes de C11/C12, uma rodada produtora única na Java GEMS SDK entrega dois blocos independentes:
SDK-AUD (`gems-auditing`) e SDK-KC (`gems-keycloak-admin`, com teste de composição junto de
`gems-security-authorization`). A publicação conjunta também altera `gems-bom`, POM pai, guias e
release notes. Nenhum arquivo de produção do sample é editado por esses checkpoints.

O portão não supera os bloqueios materiais: `gems-auditing` e `gems-keycloak-admin` só entram após
releases compatíveis publicadas e aferidas. D-P04 continua no portão posterior à medição.

## Project Structure

Mantém os cinco módulos Maven, `frontend/src/app/{core,shared,features}` e os ambientes atuais. Novos
testes/mappers entram no módulo nominal indicado em `tasks.md`; nenhuma nova camada ou SDK local.

## Sequência e atomicidade

1. Decisões humanas e releases prévias exigidas.
2. Dependências e árvores antes/depois.
3. Autorização/catalogação e Keycloak somente quando D-P02 liberar.
4. Auditoria em um bloco único somente quando D-P01 liberar.
5. Mappers/transações e conformidade Java até zero.
6. Plataforma e features Angular; medir residual e submeter D-P04.
7. Remover supressões/baselines/ratchet; gates completos e analyze final.

### Fila produtora e consumidora

| Checkpoint | Repositório/arquivos permitidos | Saída objetiva |
| :--- | :--- | :--- |
| SDK-P00 | `Java-GEMS-SDK/specs/<rodada>/**` e metadados da branch | ciclo SDD até analyze, 28 tarefas internas, SemVer 3.2.0 e portão humano; zero código |
| SDK-AUD | `gems-auditing/**` e documentação nominal do módulo | identidade estável/correlação, compatibilidade 3.1.0, mutações e gate focado |
| SDK-KC | `gems-keycloak-admin/**` e testes de integração em `gems-security-authorization/**` | ciclo completo/compensações/roles, mutações e gate focado |
| SDK-REL | POM pai, `gems-bom/**`, README/guia/llms/release notes e workflows somente se necessários | reator, revisão, BOM, PR, merge, publish e confirmação no registry |
| C11 | somente arquivos já listados em T013–T024 no sample | depende de SDK-KC publicado; ciclo Keycloak preservado |
| C12 | somente arquivos já listados em T025–T036 no sample | depende de C11 e SDK-AUD publicado; auditoria atômica preservada |
| C13 | sample, T074–T076 | depende de C11 e C12 verdes, além dos gates já declarados |

SDK-AUD e SDK-KC podem avançar em paralelo entre si e com C06–C10 porque os conjuntos de escrita são
disjuntos. “Paralelo” é no nível do programa: cada invocação continua executando exatamente um
checkpoint e fechando seu próprio estado. C05 permanece integralmente inalterado e tem prioridade de
recuperação. O caminho crítico passa a ser `SDK-P00 → max(SDK-AUD, SDK-KC) → SDK-REL → C11 → C12 → C13`;
C06–C10 só entram no caminho crítico se terminarem depois de SDK-REL.

### Contratos, compatibilidade e rollback da produtora

- Auditoria preserva nome/tipo, transação do chamador, sigilo e destino; acrescenta id estável e
  correlação por providers opcionais. Default ausente mantém o comportamento 3.1.0, sem fabricar id.
- Keycloak preserva o gateway 3.1.0 e acrescenta subinterfaces/tipos. A compensação de criação
  remove o usuário se a senha falhar; snapshots permitem ao listener consumidor restaurar usuário,
  enable e grupos após rollback.
- Mutações mínimas: ignorar/trocar id por nome; zerar correlação; gerar correlação diferente por
  entidade; omitir delete após falha de senha; tornar enable/delete no-op; omitir snapshot/grupos;
  não criar role; não compor filhos; confundir role direta com efetiva.
- Ponto seguro após cada checkpoint: branch/árvore limpa, estado persistido e gate focado verde.
  Antes do merge, rollback é abandonar/reverter somente a branch produtora. Depois da publicação,
  artefato não é sobrescrito: consumidor continua em 3.1.0, corrige-se numa nova versão SemVer e C11/
  C12 permanecem bloqueados.
- A release é consumível somente com CI da PR e de `main` verdes, merge identificado, publish verde,
  quatro coordenadas resolvendo em cache Maven limpo e um consumer smoke usando apenas o BOM.

### Contagem sem misturar granularidades

- Rodada 010: **72 tarefas (T001–T072)**. As 28 tarefas internas da rodada produtora da Java GEMS
  SDK pertencem à SDK e não aumentam esse denominador.
- Sample 002: **79 tarefas internas**, sem aumento; apenas dependências/estados de T013–T036 mudam.
- Java GEMS SDK: **28 tarefas internas novas** — SDK-AUD 10, SDK-KC 12, SDK-REL 6 — materializadas
  em SDK-P00. Elas não são somadas às 79 do sample nem contadas duas vezes como implementação da 010.

Auditoria não possui checkpoint intermediário publicável. O commit do orquestrador só pode ocorrer
depois de migration + reader + provider + writer SDK + remoções locais + testes verdes.

## Retomada compacta — 2026-09-16

Os C00–C06 permanecem como evidência histórica. A partir de C07, o baseline canônico é
`BL-S-CODEX-20260916@b807233`; cada pacote delta fecha READ/EDIT/CREATE/TEST, rollback, SHA inicial e
gate focalizado. Hash divergente, alteração do baseline ou diff fora da fronteira invalida o pacote.
C09 incorpora o build Docker de T008. C10-SDK, no repositório Java GEMS SDK, produz 3.2.0 aditiva e
tem os únicos estados `aberto`, `em-curso`, `verde`, `bloqueado-release`, `aguardando-decisão-major`
e `falhou-gate`; só `verde` libera C11.

## Complexity Tracking

Nenhuma violação foi justificada. D-P01/D-P02 preservam garantias, D-P03 não se aplica e D-P04 é uma
decisão posterior prevista, não exceção.
