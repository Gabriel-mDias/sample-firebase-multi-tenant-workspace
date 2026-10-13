# Evidências — 004 Reativação devolve a ação padrão de conta

## SDK (Java-GEMS-SDK, branch `feature/3.5.0-reativacao-acao-padrao`)

A branch parte da `origin/main` `24a7809`, que é o merge da 006.

| Commit | Conteúdo |
| :--- | :--- |
| `2e01d9b` | `release: abre a 3.5.0` (T002, `versions:set`) |
| `e40fb62` | construtor com catálogo, factory e 10 testes novos (T003–T009b, T012, T013, T015–T017) |
| `8b9df15` | implementação (T010, T018), ajuste dos testes da outbox (T018b) e docs (T020, T021) |
| `2d61858` | correções do review: factory antiga preservada, diagnóstico do `afterCommit`, duração do backoff afirmada e limite do lease documentado |
| `b0cdf7e` | correções do re-review: a entrega usa o relógio vivo do dispatcher, e o T015 lê `ACKED` antes do cleanup (prova o `REQUIRES_NEW`) |

### T001 — Linha de base

`mvn -pl gems-firebase-auth -am test` na `24a7809`: `gems-firebase-auth` com **174 testes**, 0 falhas.
Distribuição: `JdbcSecurityPostgresTest` 103, `FirebaseConfigurationTest` 20, `FirebaseBootConsumerPostgresTest`
13, `FirebaseAuthenticationFilterTest` 11, `FirebaseSecurityDataSourceTest` 11, `JdbcTenantProvisionerFailurePostgresTest`
7, `FirebaseRsaVerificationTest` 6 e `FirebaseMvcIntegrationTest` 3. O reator inteiro (`mvn clean verify`, 006) tinha
402 testes.

### Testes primeiro (Constituição VII)

Com o `e40fb62`, que ainda não tinha a implementação, `JdbcSecurityPostgresTest` rodou 113 testes e teve **5 falhas**,
todas esperadas:
- `currentEnableAckRestoresOnlySelfActionAsNewGrantAndPreservesEndedHistory` (T006)
- `repeatedWorkerRunAndReactivationOfActiveUserKeepSingleRestoredGrant` (T009b)
- `successfulAfterCommitPersistsAckOnSeparateConnectionAndOutboxDoesNotEmitAgain` (T015)
- `failedAfterCommitSchedulesBackoffAndLaterOutboxPublishesExactlyOnce` (T016)
- `afterCommitDoesNotEmitOrOverwriteDeliveryAlreadyClaimedByDispatcher` (T017)

T007, T008, T009, T012 e T013 já passavam. Eles protegem casos em que nada deve ser devolvido, e esse
era o comportamento da 3.4.1.

Com o `8b9df15`, o módulo passou a ter 184 testes verdes.

### Mutações (T011, T014, T019)

Cada mutação foi aplicada por substituição literal, testada em `JdbcSecurityPostgresTest` e desfeita com
`git checkout` do arquivo. O script está no scratchpad (`tools/mut004.ps1`).

| Id | Mutação | Vermelhos |
| :--- | :--- | :--- |
| M1 (T011) | remove `restoreSelfAction(...)` do ACK do ENABLE | T006, T009b |
| M2 (T014) | devolve a ação antes de `current(command)` | T012 (`cancelledReactivationObsoletesEnableAndNeverRestoresSelfAction`), T013 (`enableClaimedAfterIdentityVersionAdvancesIsObsoleteWithoutSelfActionGrant`), T006 |
| M3 (T019) | o `afterCommit` emite direto, sem claim nem ACK | T015, T016, T017, `eventsAreAtomicAppendOnlyAndPublisherFailureReplays` e `outboxSelectivePermanentFailureAllowsOtherEventsAndRestartUsesBackoff` |

### T022 — Review independente

- **Primeiro review** (Sol, read-only, esforço high): **FAIL**.
  - HIGH: a factory pública `firebaseIdentityCommandWorker(...)` tinha perdido a assinatura da 3.4.1, o que
    quebrava a compatibilidade de uma versão MINOR.
  - MEDIUM: o `catch` silencioso do `afterCommit` passou a cobrir também o claim e o ACK.
  - MEDIUM: os testes da outbox deixaram de afirmar a duração do backoff.
  - LOW: a documentação prometia entrega única mesmo além do lease.
  - O Sol corrigiu tudo no `2d61858`. A primeira execução dessa correção foi encerrada pelo Claude Code
    por falta de memória; a segunda, autorizada pelo usuário, retomou o trabalho a partir das alterações
    parciais.
- **Re-review** do `2d61858`: **PASS**. Os 4 achados foram resolvidos. Restaram dois: um MEDIUM, porque o relógio da entrega ficava ancorado no instante do produtor e o lease podia nascer vencido depois de uma espera por lock; e um LOW, porque o T015 não provava o `REQUIRES_NEW`. Os dois foram corrigidos no `b0cdf7e`, a terceira e última tentativa do orçamento.
- **Mutações das correções:** M4 volta a ancorar o relógio, e `oldEventUsesLiveClockForLeaseBackoffAndDispatcherRetry` e mais 5 testes da outbox ficam vermelhos. M5 troca `REQUIRES_NEW` por `REQUIRED`, e o T015 e o teste do ACK com falha ficam vermelhos. As duas foram restauradas.
- **Assinaturas:** nenhum método nem construtor público ou protegido de `origin/main` foi perdido nos arquivos de main alterados.

### T023 — Gate do SDK

`mvn -B clean verify` no `b0cdf7e`: **BUILD SUCCESS, 415 testes**. São os 402 da linha de base, mais 10 da
implementação, 2 da primeira correção e 1 da segunda. No `2d61858`, eram 414. Checkstyle e Spotless sem violação, e `gems-ai check-java --changed` com
exit 0.

- **Review final** do `b0cdf7e`: **FAIL**, com 1 HIGH. A factory `firebaseJdbcResolver(...)` da 3.4.1 tinha perdido a assinatura. O coordenador corrigiu no `09f7e0f`, porque o orçamento de tentativas do Sol tinha acabado e a correção era mecânica, no mesmo padrão da factory do worker. A prova é o `javap -public` de todas as classes do jar 3.4.1 publicado contra as classes novas: nenhuma das 558 assinaturas foi perdida. O gate ficou em 415 testes.

### T024 — PR do SDK

[Java-GEMS-SDK #51](https://github.com/Gabriel-mDias/Java-GEMS-SDK/pull/51), com push autorizado pelo usuário. Build & Test (JDK 21) passou. Analisar (Java) (CodeQL) falhou com `Code scanning is not enabled for this repository`. O repositório passou a ser privado, e o code scanning em repositório privado exige o GitHub Code Security. O job `gems-ai` volta pelo PR #50.
