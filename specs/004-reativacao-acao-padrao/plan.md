# Implementation Plan: Reativação devolve a ação padrão de conta

**Branch**: `feature/004-reativacao-acao-padrao` (sample) e `feature/3.5.0-reativacao-acao-padrao`
(Java-GEMS-SDK) | **Date**: 2026-10-12 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/004-reativacao-acao-padrao/spec.md`

## Summary

No ACK do `ENABLE` da versão atual, o `gems-firebase-auth` passa a conceder de novo a ação global
padrão de conta, se ela estiver no catálogo do consumidor e não houver concessão vigente, e emite
`GLOBAL_SELF_ACTION_RESTORED`. Na mesma release, a emissão `afterCommit` dos eventos reivindica a
própria linha da outbox, o que elimina a entrega dupla sistemática do achado 5 da 003. O SDK sai
como 3.5.0 (MINOR, aditivo), e o sample adota a versão com prova no consumidor pelo E2E com o
Firebase real. Decisões em [research.md](research.md).

## Technical Context

**Language/Version**: Java 21 (SDK e sample backend). O frontend não muda.

**Primary Dependencies**: Spring Boot 4.1.0, Spring JDBC (`JdbcClient`, `TransactionTemplate`),
Firebase Admin SDK, `gems-security-authorization` (`AuthorizationCatalog`).

**Storage**: PostgreSQL 16, schema `security` do changelog `security-3.4.0.sql`, sem mudança de schema.

**Testing**: JUnit 5 com Testcontainers PostgreSQL no SDK (`JdbcSecurityPostgresTest`); no sample,
contratos e E2E em `app-deploy` com o Firebase real de testes via ADC.

**Target Platform**: biblioteca Maven publicada no GitHub Packages; sample com backend Spring Boot.

**Project Type**: library (SDK) + consumidor web-service (sample).

**Performance Goals**: sem meta nova. O ACK ganha um `INSERT ... WHERE NOT EXISTS`, e o
`afterCommit`, um `UPDATE` por evento.

**Constraints**: versão MINOR sem quebra de fonte; nenhuma linha encerrada reaberta; ENABLE obsoleto
não concede; segredo nunca versionado.

**Scale/Scope**: 2 classes de produção do SDK (`JdbcIdentityCommandWorker`, `SecurityJdbcSupport`),
a autoconfiguração, a documentação e as release notes; no sample, `backend/pom.xml` e os testes de
`app-deploy`.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio | Situação |
| :--- | :--- |
| I. Fronteiras do reator | PASS. O sample só troca a versão e mexe em testes de `app-deploy` |
| II/III. Persistência e Liquibase | PASS. Sem schema novo; o changelog do SDK não muda |
| V. Contrato REST | PASS. Nenhum endpoint muda; `/auth/me` volta a responder 200 depois da reativação, o que é o comportamento pretendido e foi decidido pelo product owner (roteiro §2) |
| VII. Testes como gate | PASS planejado. Testes antes no SDK, mutação no SDK e no consumidor, `mvn clean verify` e `mvnw.cmd clean install` |
| VIII. Spec antes de código | PASS. O ciclo está em curso, e o portão é a §12 do roteiro. Clarify dispensado: as decisões foram fechadas na fase 1 (roteiro §2) |
| IX. Ambiente | PASS. E2E com o Firebase real de testes via ADC/secret; emulador fora |
| Adoção de módulo da SDK | PASS planejado. Condição 1: o bloco do sample fica **bloqueado** até a tag `v3.5.0` publicada. Condição 2: `<gems.version>` único. Condição 3: `dependency:tree` antes e depois. Condição 4: garantia nomeada, "usuário reativado consulta a própria conta". Condição 6: mudança observável decidida no roteiro |

Re-check pós-design: sem violações. Complexity Tracking não se aplica.

## Project Structure

### Documentation (this feature)

```text
specs/004-reativacao-acao-padrao/
├── roteiro.md
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/sdk-gems-firebase-auth-3.5.0.md
├── checklists/requirements.md
└── tasks.md             # /speckit-tasks
```

### Source Code

```text
Java-GEMS-SDK/
├── gems-firebase-auth/src/main/java/br/com/gems/firebase/auth/
│   ├── jdbc/JdbcIdentityCommandWorker.java     # construtor com catálogo + devolução no ACK ENABLE
│   ├── jdbc/SecurityJdbcSupport.java           # afterCommit reivindica a própria entrega
│   └── config/FirebaseJdbcAutoConfiguration.java  # bean do worker com catálogo
├── gems-firebase-auth/src/test/java/br/com/gems/firebase/auth/jdbc/JdbcSecurityPostgresTest.java
├── docs/FIREBASE-AUTH.md
└── RELEASE-NOTES-3.5.0.md

sample-firebase-multi-tenant-workspace/backend/
├── pom.xml                                     # <gems.version> 3.4.1 → 3.5.0
└── app-deploy/src/test/java/br/com/gems/app/
    ├── EndToEndSupport.java                    # worker com catálogo; asserções pós-reativação
    ├── IdentityLifecycleContractIntegrationTest.java
    └── support/FirebaseContractSupport.java    # worker com catálogo
```

**Structure Decision**: a correção mora no SDK, dono do mecanismo; o sample não ganha código de
produção. Constituição, "Adaptar o módulo ao projeto por dentro" proíbe contorno local.

## Fases e delegação

| # | Fase | Repositório | Modelo | Por quê |
| :-- | :--- | :--- | :--- | :--- |
| 1 | Testes vermelhos do SDK (devolução, idempotência, catálogo, obsoleto, cancelamento, entrega única, reentrega) | Java-GEMS-SDK | Sol (Codex) medium | Há espelho e gate forte (Testcontainers); a memória do usuário prefere o Sol para desenvolvimento |
| 2 | Implementação no SDK + docs + release notes | Java-GEMS-SDK | Sol (Codex) medium | Mudança local em duas classes, coberta pela fase 1 |
| 3 | Review independente do SDK | Java-GEMS-SDK | Sol (Codex) read-only, outra instância | Flags de segurança e persistência são verdadeiras: review obrigatório |
| 4 | Gate do SDK, mutação e PR | Java-GEMS-SDK | Coordenador (Opus low) | Prova observada pelo controlador |
| 5 | Release `v3.5.0` | Java-GEMS-SDK | **Usuário** | Tag imutável aprovada por humano |
| 6 | Adoção no sample (versão, testes, mutação no consumidor) | sample | Sol (Codex) medium | Mudança de teste com E2E real como gate |
| 7 | Gate completo, verificação humana e PR | sample | Coordenador + usuário | Princípio VII e homologação |

Nenhum uso do Opus como executor; o coordenador só contrata, verifica e aceita.
