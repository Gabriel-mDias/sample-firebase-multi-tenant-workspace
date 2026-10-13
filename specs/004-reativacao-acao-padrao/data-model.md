# Data model — 004 Reativação devolve a ação padrão de conta

**Nenhuma alteração de schema.** As tabelas são as do changelog `security-3.4.0.sql` do
`gems-firebase-auth`. A rodada muda só as transições abaixo.

## Exceção à regra da 003

A [data-model da 003](../003-firebase-multi-tenant/data-model.md) diz que a reativação acontece
"sem reabrir identidades/memberships/grants encerrados". Isso continua valendo, com **uma exceção
nomeada**: no ACK do `ENABLE` atual, a ação global padrão de conta é concedida **de novo**, como
linha nova. Nenhuma linha encerrada é reaberta.

## Transição: ACK do `ENABLE` (versão atual)

Na mesma transação, sob `SELECT ... FOR UPDATE` de `security.usuario`:

| Passo | Tabela | Efeito | Condição |
| :-- | :--- | :--- | :--- |
| 1 | `security.usuario` | `dt_inicio=now`, `dt_fim=NULL` (já existe) | `nr_versao_identidade` = versão do comando, `cd_elegibilidade='BOUND'`, `dt_fim IS NOT NULL`; senão rollback (`ENABLE_CAS_FAILED`) |
| 2 | `security.usuario_role_global` | **novo**: insere `(id novo, id_usuario, id_role, dt_inicio=now)` | a ação padrão está no catálogo do consumidor **e** existe `role_seguranca` vigente `GLOBAL` para ela **e** não há concessão vigente da mesma role para o usuário |
| 3 | `security.evento_seguranca` + `entrega_evento` | **novo**: `GLOBAL_SELF_ACTION_RESTORED` | só se o passo 2 inseriu |
| 4 | `security.comando_identidade` | `ACKED`/`APPLIED` (já existe) | — |
| 5 | eventos | `IDENTITY_COMMAND_ACKED` (já existe) | — |

O que **não** muda: `usuario_tenant`, `usuario_tenant_perfil`, `usuario_tenant_role`, `grupo_usuario`
e as demais linhas de `usuario_role_global` continuam com o `dt_fim` da suspensão.

ENABLE obsoleto, superado ou bloqueado por REVOKE anterior não chega ao passo 1: `current(command)`
o encerra como `OBSOLETE`/`SUPERSEDED`, sem concessão.

## Transição: entrega de evento (`security.entrega_evento`)

| De | Para | Quem | Condição |
| :--- | :--- | :--- | :--- |
| `PENDING` | `CLAIMED` | `afterCommit` do evento (**novo**) ou `publishNext` | CAS: `cd_estado='PENDING'` e linha não travada (`SKIP LOCKED`) |
| `CLAIMED` | `ACKED` | quem reivindicou | emissão sem exceção, `cd_claim` igual e lease vigente |
| `CLAIMED` | `PENDING` + backoff | quem reivindicou | emissão com exceção |
| `CLAIMED` expirado | `CLAIMED` | `publishNext` | `dt_lease<=now` (recuperação após restart) |

Invariante novo: no caminho sem falha, cada `id_evento` é emitido **uma vez**. As transições feitas
pelo `afterCommit` rodam em transação nova (`PROPAGATION_REQUIRES_NEW`), porque a transação original
já foi confirmada.
