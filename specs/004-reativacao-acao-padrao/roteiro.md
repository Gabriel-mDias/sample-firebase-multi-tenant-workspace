# Roteiro da Rodada — 004 Reativação devolve a ação padrão de conta

**Feature**: `004-reativacao-acao-padrao` · **Branch**: `feature/004-reativacao-acao-padrao` ·
**Requisito**: achados 4 e 5 da homologação da 003
([evidence.md](../003-firebase-multi-tenant/evidence.md#homologacao-local-20261012))
**Data**: 2026-10-12 · **Status**: fase 1 concluída, aguardando o ciclo spec-kit (fase 2)

**Índice**: [1 Objetivo](#1-objetivo-da-rodada) · [2 Decisões de entrada](#2-decisões-de-entrada)

---

## 1. Objetivo da rodada

Depois de uma reativação, o usuário volta a conseguir entrar no sistema e ver a própria conta. Hoje
a suspensão encerra todas as concessões, inclusive a ação global `CONSULTAR_PROPRIA_CONTA`, que o
cadastro concede automaticamente. A reativação não devolve essa ação. O resultado é uma conta
vigente no Firebase e no SQL em que `/auth/me` responde 403 e o login não se completa, sem mensagem
que explique o motivo. Isso foi observado na homologação da 003, em 2026-10-12.

### Por que este requisito agora

É a única decisão de comportamento que a homologação da 003 deixou aberta. Fala literal do product
owner: "Sobre a reativação, a claim precisa automaticamente ser devolvida em uma reativação".

```
Java-GEMS-SDK gems-firebase-auth (correção + release)  ──►  sample (adoção + E2E real)
```

A seta é dependência de dado: o sample só consome o comportamento novo depois da publicação do SDK
(`specs/AGENTS.md`: adoção exige publicação).

### Onde o defeito mora

O código vence o documento. Verificado em 2026-10-12, no Java-GEMS-SDK `release/3.4.1`:

- `JdbcUserService.suspend` chama `endUserGrants`, que encerra também `usuario_role_global`
  (`gems-firebase-auth/.../jdbc/JdbcUserService.java:146`).
- A ação padrão vem de `defaultSelfAction()` e só é concedida em `create`
  (`JdbcUserService.java:34`, `:38`, `:41`).
- O ACK do `ENABLE` reabre só o usuário (`dt_fim=NULL`) e não toca as concessões
  (`JdbcIdentityCommandWorker.java:118`).
- O contrato da 003 proíbe a reabertura
  ([data-model.md:39](../003-firebase-multi-tenant/data-model.md): "sem reabrir
  identidades/memberships/grants encerrados"). A 004 abre uma exceção nomeada a essa regra.

### O que a rodada carrega junto

O achado 5: no log do `identity-worker`, cada `IDENTITY_COMMAND_ACKED` sai duas vezes com o mesmo
id. O componente é o mesmo (worker e outbox do `gems-firebase-auth`), e o diagnóstico custa pouco
enquanto o código está aberto. A decisão é investigar primeiro e só corrigir se houver duplicidade
real.

---

## 2. Decisões de entrada

Decisões tomadas com o product owner em 2026-10-12, antes da escrita da especificação.

| # | Ponto | Decisão | O que isso fecha |
| :-- | :--- | :--- | :--- |
| 1 | O que volta na reativação | **Só a ação padrão de conta** (`CONSULTAR_PROPRIA_CONTA`), a mesma que o cadastro concede. Vínculos com empresas, perfis, roles de tenant, grupos e as demais ações globais continuam encerrados e precisam ser concedidos de novo, de propósito | Impede que a reativação devolva privilégios que ninguém revisou. A frase do data-model da 003 ganha uma exceção, com nome |
| 2 | Divisão em rodadas | **Duas rodadas.** A 004 cuida da reativação (SDK, release e adoção). A [005](../005-endurecimento-frontend-docs/roteiro.md) cuida do frontend, da documentação e dos scripts | Cada rodada tem o próprio portão e o próprio PR. O release do SDK não trava o endurecimento do frontend |
| 3 | ACK duplicado no log (achado 5) | **Investigar na 004.** Se a causa for log ou evento emitido duas vezes, corrige. Se for entrega "pelo menos uma vez" com efeito idempotente, registra a explicação e segue | Fecha a observação aberta sem comprometer escopo antes do diagnóstico |

### Decisões tomadas por convenção, sem pergunta

| # | Ponto | Decisão | Fonte da convenção |
| :-- | :--- | :--- | :--- |
| C1 | Momento da devolução | No ACK do `ENABLE` da versão atual, na mesma transação do CAS que torna o usuário vigente. Nunca na solicitação da reativação | `JdbcUserService.java:94`: "vigência local só muda depois do ACK atual". ENABLE obsoleto não devolve nada |
| C2 | Catálogo | A ação volta só se ainda existir em `catalog.globalActions()`, a mesma regra de `defaultSelfAction()` | Evita conceder ação que o consumidor tirou do catálogo |
| C3 | Idempotência | Uma concessão nova, sem duplicar se já houver uma vigente. A linha encerrada continua encerrada, porque o histórico não se reescreve | O padrão de vigência (`dt_inicio`/`dt_fim`) da 003 |
| C4 | Versão do SDK | Release **MINOR** do Java-GEMS-SDK (candidata 3.5.0), porque muda um comportamento documentado do `gems-firebase-auth`. Publicação só por tag imutável aprovada pelo usuário | `Java-GEMS-SDK/CLAUDE.md`, "Versioning & releasing" |
| C5 | Adoção no sample | Dependência atualizada no sample, com prova no consumidor: E2E com o Firebase real (suspender, rodar o worker, reativar e conferir `/auth/me` 200) | `specs/AGENTS.md` e a memória "Firebase real em testes e CI" (decisão 6B) |
| C6 | Dados existentes | Sem migração de dados. No sample só há dados de teste, e o Tester já foi restaurado manualmente na homologação | Escopo enxuto, decisão do usuário na 003 |
| C7 | Contrato | A regra nova vive no `spec.md` da 004 e aponta a exceção ao data-model da 003. A 003 é história e não é reescrita | `specs/AGENTS.md`: specs concluídas são história |
