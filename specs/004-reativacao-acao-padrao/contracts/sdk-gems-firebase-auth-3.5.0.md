# Contrato — `gems-firebase-auth` 3.5.0 (delta sobre 3.4.1)

Superfície pública que o sample consome. Tudo aditivo; a 3.4.1 continua compilando.

## API

| Elemento | Mudança |
| :--- | :--- |
| `JdbcIdentityCommandWorker(JdbcClient, TransactionTemplate, SecurityEventSink, DataSource, FirebaseAdminGateway, Clock, Duration, AuthorizationCatalog)` | **novo** construtor. Com ele, o ACK do `ENABLE` devolve a ação padrão de conta ([data-model](../data-model.md#transição-ack-do-enable-versão-atual)) |
| construtor atual, sem catálogo | `@Deprecated(since = "3.5.0")`; comportamento da 3.4.1 (não devolve) |
| `FirebaseJdbcAutoConfiguration` | o bean do worker usa o construtor novo com o `AuthorizationCatalog` já exigido no opt-in |

## Eventos

| Evento | Mudança |
| :--- | :--- |
| `GLOBAL_SELF_ACTION_RESTORED` | **novo**. `targetType=GLOBAL_ACTION`, `target`=concessão nova, `source`=usuário, `related`=role, `action=CONSULTAR_PROPRIA_CONTA` |
| todos | entrega única no caminho sem falha: a emissão `afterCommit` reivindica e confirma a própria linha da outbox. Repetição só depois de falha ou restart; o consumidor continua deduplicando por UUID |

## Documentação

`docs/FIREBASE-AUTH.md`: a seção de lifecycle (linha ~158) descreve a devolução e a exceção, e a seção
de eventos (linhas 168-174) descreve a reivindicação do `afterCommit`. `RELEASE-NOTES-3.5.0.md`
lista os três itens acima.
