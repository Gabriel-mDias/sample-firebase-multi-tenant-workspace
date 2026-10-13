# Tasks: Reativação devolve a ação padrão de conta

**Input**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md),
[contracts/](contracts/sdk-gems-firebase-auth-3.5.0.md), [quickstart.md](quickstart.md)

**Tests**: obrigatórios e escritos antes (Constituição VII). Toda rede nova prova-se por mutação.

Caminhos: `SDK/` = `Java-GEMS-SDK/`; `SAMPLE/` = `sample-firebase-multi-tenant-workspace/`.

## Phase 1: Setup

- [x] T001 Criar a branch `feature/3.5.0-reativacao-acao-padrao` a partir de `origin/main` em `SDK/` e registrar a contagem atual de testes de `gems-firebase-auth` (`mvn -pl gems-firebase-auth -am test`) em `SAMPLE/specs/004-reativacao-acao-padrao/evidence.md`
- [x] T002 Rodar `mvn versions:set -DnewVersion=3.5.0 -DgenerateBackupPoms=false` na raiz de `SDK/`, sem editar `<version>` à mão

## Phase 2: Foundational

- [x] T003 Acrescentar o construtor de `JdbcIdentityCommandWorker` com `AuthorizationCatalog` e marcar o atual `@Deprecated(since = "3.5.0")`, sem mudar comportamento ainda, em `SDK/gems-firebase-auth/src/main/java/br/com/gems/firebase/auth/jdbc/JdbcIdentityCommandWorker.java`
- [x] T004 Fazer o bean do worker usar o construtor novo em `SDK/gems-firebase-auth/src/main/java/br/com/gems/firebase/auth/config/FirebaseJdbcAutoConfiguration.java:146-149`
- [x] T005 Ajustar o helper `worker(...)` do teste para o construtor novo em `SDK/gems-firebase-auth/src/test/java/br/com/gems/firebase/auth/jdbc/JdbcSecurityPostgresTest.java:220`

**Checkpoint**: o reator do SDK compila, e os testes existentes continuam verdes.

## Phase 3: User Story 1 — Usuário reativado volta a ver a própria conta (P1) 🎯 MVP

**Goal**: o ACK do `ENABLE` atual concede de novo só a ação padrão ([data-model](data-model.md#transição-ack-do-enable-versão-atual)).

**Independent Test**: suspender, rodar o worker, reativar, rodar o worker e conferir uma concessão vigente da ação padrão, nenhum vínculo vigente e `/auth/me` 200.

### Tests (escrever primeiro, ver falhar)

- [x] T006 [US1] Teste: suspender e reativar um usuário com a ação padrão, outra ação global e um vínculo; depois do ACK, há exatamente 1 concessão vigente da ação padrão, nenhuma das demais, nenhum `usuario_tenant` vigente, a linha antiga continua encerrada e há 1 evento `GLOBAL_SELF_ACTION_RESTORED`, em `SDK/.../jdbc/JdbcSecurityPostgresTest.java`
- [x] T007 [US1] Teste: concessão manual vigente da ação padrão antes do ACK → nenhuma duplicata e nenhum evento de devolução, em `SDK/.../jdbc/JdbcSecurityPostgresTest.java`
- [x] T008 [US1] Teste: catálogo sem `CONSULTAR_PROPRIA_CONTA` → a reativação conclui sem concessão, em `SDK/.../jdbc/JdbcSecurityPostgresTest.java`
- [x] T009 [US1] Teste: worker com o construtor antigo → comportamento da 3.4.1 (nenhuma devolução), em `SDK/.../jdbc/JdbcSecurityPostgresTest.java`
- [x] T009b [US1] Teste: depois do ACK, um segundo `runOnce` não faz nada (continua 1 concessão), e `requestReactivation` de usuário vigente não altera concessões, em `SDK/.../jdbc/JdbcSecurityPostgresTest.java`

### Implementation

- [x] T010 [US1] Implementar a devolução no ramo `ENABLE` de `acknowledge`, depois do CAS, com `INSERT ... SELECT ... WHERE NOT EXISTS` e o evento `GLOBAL_SELF_ACTION_RESTORED`, em `SDK/.../jdbc/JdbcIdentityCommandWorker.java:118-123`
- [x] T011 [US1] Mutação: remover o `INSERT` de T010, confirmar T006 vermelho e restaurar por edição inversa; registrar em `SAMPLE/specs/004-reativacao-acao-padrao/evidence.md`

**Checkpoint**: US1 verde no SDK.

## Phase 4: User Story 2 — Reativação obsoleta ou anulada não devolve acesso (P1)

**Independent Test**: reativar, suspender de novo antes do worker, rodar o worker → usuário suspenso e sem concessão vigente.

- [x] T012 [US2] Teste: `requestReactivation` seguido de `cancelReactivation` (ou `suspend`) antes do worker → o ENABLE fica `OBSOLETE`, o usuário continua com `dt_fim` e não há concessão vigente nem evento de devolução, em `SDK/.../jdbc/JdbcSecurityPostgresTest.java`
- [x] T013 [US2] Teste: ENABLE de versão antiga reivindicado depois de nova versão → nenhuma concessão, em `SDK/.../jdbc/JdbcSecurityPostgresTest.java`
- [x] T014 [US2] Mutação: fazer a devolução de T010 rodar antes de `current(command)`, confirmar T012/T013 vermelhos e restaurar; registrar em `evidence.md`

## Phase 5: User Story 3 — Confirmação de comando registrada uma vez (P3)

**Goal**: entrega única no caminho sem falha ([research R5](research.md#r5--identity_command_acked-duplicado-achado-5-da-003)).

**Independent Test**: produzir um evento num processo que também drena a outbox e contar as emissões por `id_evento`.

- [x] T015 [US3] Teste: um evento com `afterCommit` bem-sucedido seguido de `publishNext` → 1 emissão e `entrega_evento` `ACKED` **lido por outra conexão** (prova de `REQUIRES_NEW`), em `SDK/.../jdbc/JdbcSecurityPostgresTest.java`
- [x] T016 [US3] Teste: `afterCommit` cujo publisher lança exceção → linha `PENDING` com backoff, e `publishNext` posterior emite 1 vez, em `SDK/.../jdbc/JdbcSecurityPostgresTest.java`
- [x] T017 [US3] Teste: linha já `CLAIMED` pelo dispatcher quando o `afterCommit` roda → o `afterCommit` não emite, em `SDK/.../jdbc/JdbcSecurityPostgresTest.java`
- [x] T018 [US3] Implementar no `afterCommit` a reivindicação CAS da própria linha (`PENDING`→`CLAIMED` com lease), a emissão e a marcação `ACKED`/`PENDING`+backoff, em transação `PROPAGATION_REQUIRES_NEW`, em `SDK/.../jdbc/SecurityJdbcSupport.java:47-53`
- [x] T018b [US3] Ajustar os testes do SDK que drenam a outbox esperando eventos recém-criados (`JdbcSecurityPostgresTest.java:273-278`, `:469-493`, `:787-797` e outros que o gate apontar), preservando a garantia de cada um (retry, poison, lease concorrente, restart), em `SDK/.../jdbc/JdbcSecurityPostgresTest.java`
- [x] T019 [US3] Mutação: retirar a reivindicação de T018, confirmar T015 vermelho e restaurar; registrar em `evidence.md`

## Phase 6: SDK — documentação, review e gate

- [x] T020 [P] Atualizar o lifecycle (devolução e exceção) e a entrega de eventos em `SDK/docs/FIREBASE-AUTH.md:158-174`
- [x] T021 [P] Criar `SDK/RELEASE-NOTES-3.5.0.md` com os itens do [contrato](contracts/sdk-gems-firebase-auth-3.5.0.md) e atualizar a seção de candidato em `SDK/CLAUDE.md`
- [x] T022 Review independente (Sol read-only) do diff do SDK contra spec, data-model e contrato
- [x] T023 Gate `mvn --batch-mode clean verify` em `SDK/` observado pelo coordenador; registrar a contagem de testes antes e depois em `evidence.md`
- [ ] T024 Commit, push e PR do SDK para `main` (com ok do usuário); CI verde
- [ ] T025 **Usuário**: merge, tag `v3.5.0` e workflow `release.yml` verde (publicação no GitHub Packages)

## Phase 7: Sample — adoção (bloqueada até T025)

- [ ] T026 `dependency:tree` de `app-deploy` antes da troca; trocar `<gems.version>` para `3.5.0` em `SAMPLE/backend/pom.xml:34`; `dependency:tree` depois; registrar o alcance em `evidence.md`
- [ ] T027 [P] [US1] Passar o worker de teste para o construtor com catálogo em `SAMPLE/backend/app-deploy/src/test/java/br/com/gems/app/support/FirebaseContractSupport.java:180` e `SAMPLE/backend/app-deploy/src/test/java/br/com/gems/app/EndToEndSupport.java:568,964`
- [ ] T028 [US1] E2E com o Firebase real: depois do ACK do `ENABLE`, `/auth/me` 200, nenhuma empresa e 1 concessão vigente da ação padrão, em `SAMPLE/backend/app-deploy/src/test/java/br/com/gems/app/EndToEndSupport.java` (fluxo de `:379`)
- [ ] T029 [US2] Contrato: reativação cancelada antes do worker não concede, em `SAMPLE/backend/app-deploy/src/test/java/br/com/gems/app/IdentityLifecycleContractIntegrationTest.java`
- [ ] T030 [US3] Teste: o fluxo do job `identity-worker` emite cada evento 1 vez para o `SecurityEventPublisher` do sample; ajustar o teste de restart da outbox (`:586-604`) preservando a garantia dele, em `SAMPLE/backend/app-deploy/src/test/java/br/com/gems/app/FirebaseBackendIntegrationTest.java`
- [ ] T031 Mutação no consumidor: voltar o worker de T027 ao construtor antigo, confirmar T028 vermelho e restaurar; registrar em `evidence.md`
- [ ] T032 Review independente (Sol read-only) do diff do sample

## Phase 8: Fechamento

- [ ] T033 Gate completo do sample uma vez: `.\mvnw.cmd clean install` com ADC, `npm ci`, `npm test -- --watch=false`, `npm run build` e `npx prettier --check .` em `frontend/` (sem mudança, mas exigidos pelo gate de merge da Constituição VII) e `scripts/gems-ai-gate.ps1`; registrar em `evidence.md`
- [ ] T034 **Usuário**: verificação humana do [quickstart §4](quickstart.md#4-verificação-humana) (suspender, reativar e entrar com o Tester)
- [ ] T035 Commit, push e PR do sample (com ok do usuário); CI verde nos 4 jobs

## Dependencies & Execution Order

- T001 → T002 → T003 → T004/T005 → US1 (T006–T011, com T009b) → US2 (T012–T014) → US3 (T015–T019, com T018b depois de T018) → T020/T021 → T022 → T023 → T024 → T025 → T026 → T027–T030 → T031 → T032 → T033 → T034 → T035.
- US1 e US2 tocam o mesmo trecho do worker, por isso ficam em série. US3 toca outra classe (`SecurityJdbcSupport`) e poderia correr em paralelo a US1/US2, mas compartilha o arquivo de teste; fica em série para não ter dois escritores no mesmo arquivo.
- [P] reais: T020 ∥ T021; T027 ∥ T028–T030 só se forem pacotes distintos (mesmo executor, em série, é o padrão).

## Implementation Strategy

MVP = US1 no SDK (T001–T011). Ele já corrige o defeito observado, mas só chega ao sample depois da release (T025). US2 e US3 entram na mesma release para que a publicação seja única.

## Delegação

Conforme a tabela "Fases e delegação" do [plan.md](plan.md#fases-e-delegação): desenvolvimento e review pelo Sol (Codex), em instâncias distintas; gates, mutações observadas e aceite pelo coordenador; release, verificação humana e autorizações de push pelo usuário.
