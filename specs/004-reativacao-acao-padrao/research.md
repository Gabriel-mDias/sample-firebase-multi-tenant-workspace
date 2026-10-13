# Research — 004 Reativação devolve a ação padrão de conta

Apurado no código em 2026-10-12. Java-GEMS-SDK na branch `release/3.4.1` (tag `v3.4.1` publicada) e
sample na `main` `2e9fc75`.

## R1 — Onde a ação padrão volta

- **Decision**: no ramo `ENABLE` de `JdbcIdentityCommandWorker.acknowledge`, logo depois do CAS que
  limpa `dt_fim` (`JdbcIdentityCommandWorker.java:118-123`), na mesma transação e sob o mesmo
  `FOR UPDATE` do usuário.
- **Rationale**: é o único ponto em que a reativação passa a valer (`JdbcUserService.java:94`:
  "vigência local só muda depois do ACK atual"). O `current(command)` já descarta ENABLE obsoleto,
  superado ou com REVOKE anterior pendente (`:89-99`), então FR-005 vem de graça. Se o CAS falha, a
  transação inteira faz rollback e nenhuma concessão sobra.
- **Alternatives considered**: (a) conceder em `requestReactivation`: rejeitado, porque concederia
  antes da confirmação e sobreviveria a um cancelamento; (b) não encerrar a ação padrão em `suspend`:
  rejeitado, porque o usuário suspenso ficaria com uma concessão vigente, contrariando "bloqueio
  local antes da chamada remota" (backend/AGENTS.md).

## R2 — Como o worker sabe se a ação está no catálogo

- **Decision**: novo construtor de `JdbcIdentityCommandWorker` que recebe o `AuthorizationCatalog`
  do consumidor, e `FirebaseJdbcAutoConfiguration.java:149` passa a usá-lo. A regra é a mesma de
  `JdbcUserService.defaultSelfAction()` (`:41`). O construtor atual continua compilando, fica
  `@Deprecated` e não devolve nada (comportamento da 3.4.1), o que mantém a versão MINOR aditiva.
- **Rationale**: o `role_seguranca` não basta. `JdbcSecurityCatalog` não encerra a role de uma ação
  retirada do catálogo, só emite `AUTHORIZATION_CATALOG_DRIFT` (`JdbcSecurityCatalog.java:53`).
  Consultar só o SQL concederia uma ação que o consumidor retirou.
- **Alternatives considered**: ler só o `role_seguranca` vigente (rejeitado pelo motivo acima);
  quebrar o construtor (rejeitado, porque exigiria MAJOR).

## R3 — Idempotência e histórico

- **Decision**: `INSERT ... SELECT ... WHERE NOT EXISTS` (concessão vigente da mesma role para o
  usuário), com `dt_inicio = now` do ACK. A linha encerrada pela suspensão não muda.
- **Rationale**: FR-003 e FR-006. O ACK roda uma vez por comando por causa do CAS `cd_claim`, mas a
  guarda cobre concessão manual concorrente e o reprocessamento.

## R4 — Evento da devolução

- **Decision**: novo evento `GLOBAL_SELF_ACTION_RESTORED`, com `TargetType.GLOBAL_ACTION`, alvo na
  concessão nova, origem no usuário, relacionado na role e ação `CONSULTAR_PROPRIA_CONTA`. É o mesmo
  formato de `GLOBAL_SELF_ACTION_GRANTED` (`JdbcUserService.java:67`).
- **Rationale**: FR-007, distinguível da concessão do cadastro e da manual (`GLOBAL_ACTION_GRANTED`).
  O ator é técnico (worker), então `id_ator` fica nulo, como nos demais eventos do worker.

## R5 — `IDENTITY_COMMAND_ACKED` duplicado (achado 5 da 003)

- **Diagnóstico**: é emissão dupla sistemática, não reentrega rara.
  `SecurityJdbcSupport.event` grava o evento, cria a linha `entrega_evento` `PENDING` e registra um
  `afterCommit` que chama `events.emit` (`SecurityJdbcSupport.java:44-53`). Esse caminho não marca a
  entrega. Depois, o job `identity-worker` do sample roda `publishNext` (`JobsConfiguration.java:90`),
  que encontra a mesma linha `PENDING` e emite de novo (`JdbcSecurityEventOutbox.java:19-42`). Todo
  evento produzido num processo que também drena a outbox sai duas vezes. A documentação do SDK
  admite repetição, mas só como exceção ("após send antes de ACK", `docs/FIREBASE-AUTH.md:169-174`),
  não no caminho normal.
- **Decision**: corrigir no SDK (FR-008). O `afterCommit` passa a reivindicar a própria linha
  `entrega_evento` com o mesmo CAS de `publishNext` (`PENDING` → `CLAIMED` com lease), emite e marca
  `ACKED`. Se a reivindicação falhar, porque o dispatcher já pegou a linha, não emite. Uma falha da
  emissão devolve a linha a `PENDING` com backoff, como hoje. O resultado é entrega única no caminho
  normal e "pelo menos uma vez" só depois de falha ou restart, como a documentação já diz.
- **Restrição de transação (achado U1 do analyze)**: dentro de `afterCommit`, os recursos da
  transação original ainda estão ligados à thread, e um `TransactionTemplate` com `REQUIRED` entra
  nela sem confirmar nada. A reivindicação e a marcação usam **`PROPAGATION_REQUIRES_NEW`**, num
  template próprio, e o teste confere o estado `ACKED` lido por outra conexão.
- **Impacto em testes existentes (achado I1)**: depois do commit, a linha deixa de ficar `PENDING`
  quando o sink do teste aceita o evento. Os testes que drenam a outbox esperando encontrar eventos
  recém-criados precisam emitir com um sink que falha ou passar a afirmar entrega única. Os
  candidatos, inventariados em 2026-10-12, são `JdbcSecurityPostgresTest.java:273-278`, `:469-493`
  e `:787-797` no SDK, e `FirebaseBackendIntegrationTest.java:586-604` no sample. O ajuste mantém a
  garantia original de cada teste (retry, poison, lease concorrente, restart) e não reduz a
  cobertura.
- **Alternatives considered**: (a) só documentar: rejeitado, porque a decisão 3 do roteiro manda
  corrigir quando a duplicidade é real; (b) retirar a emissão `afterCommit` e deixar só a outbox:
  rejeitado, porque muda a latência dos consumidores web, que hoje recebem o evento na hora;
  (c) deduplicar no `SecurityEventBridge` do sample: rejeitado, porque esconde o defeito no
  consumidor (Constituição, "Adaptar o módulo ao projeto por dentro").

## R6 — Versão e publicação

- **Decision**: Java-GEMS-SDK **3.5.0**, a partir da `main` do SDK com a 3.4.1 incorporada. A
  publicação sai por tag `v3.5.0` aprovada pelo usuário, e o sample troca `<gems.version>` de `3.4.1`
  para `3.5.0` (`backend/pom.xml:34`).
- **Rationale**: muda comportamento documentado e acrescenta API (construtor e evento) sem quebrar a
  existente (`Java-GEMS-SDK/CLAUDE.md`, "Versioning & releasing").

## R7 — Prova no consumidor

- **Decision**: o E2E com o Firebase real do sample (`EndToEndSupport.java:379` já reativa) passa a
  afirmar `/auth/me` 200, lista de empresas vazia e exatamente uma concessão vigente da ação padrão
  após o ACK. O teste de contrato `IdentityLifecycleContractIntegrationTest` afirma o caso obsoleto, e
  um teste do sample conta uma única entrega por evento no job `identity-worker`. A prova por
  mutação no consumidor desliga a devolução (construtor antigo) e espera o vermelho.
- **Rationale**: Constituição, "Adoção de Módulo da SDK GEMS", provas 1 e 2.
