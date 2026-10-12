# Roteiro da Rodada — 004 Reativação devolve a ação padrão de conta

**Feature**: `004-reativacao-acao-padrao` · **Branch**: `feature/004-reativacao-acao-padrao` ·
**Requisito**: achados 4 e 5 da homologação da 003
([evidence.md](../003-firebase-multi-tenant/evidence.md#homologacao-local-20261012))
**Data**: 2026-10-12 · **Status**: aguardando aprovação

**Índice**: [1 Objetivo](#1-objetivo-da-rodada) · [2 Decisões de entrada](#2-decisões-de-entrada) ·
[3 Banco](#3-banco-de-dados) · [4 Backend](#4-backend) · [6 Testes](#6-testes-esperados-por-bloco) ·
[7 Interação e isolamento](#7-interação-e-isolamento) · [8 Tasks e delegação](#8-divisão-de-tasks-e-delegação-por-modelo) ·
[9 Prompts SDD](#9-prompts-sdd-da-rodada) · [10 Riscos](#10-riscos-e-mitigações) ·
[11 Fora de escopo](#11-fora-de-escopo) · [12 Aprovação](#12-critérios-de-aprovação)

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

---

## 3. Banco de dados

**Nenhuma alteração de schema nem de changelog.** O que muda são as transições sobre tabelas já
existentes do `security-3.4.0.sql` ([data-model.md](data-model.md)):

| Tabela | Antes (3.4.1) | Depois (3.5.0) |
| :--- | :--- | :--- |
| `security.usuario_role_global` | a suspensão encerra todas as linhas do usuário; nada volta | idem, e o ACK do `ENABLE` atual **insere uma linha nova** da ação padrão, se ela estiver no catálogo e não houver uma vigente |
| `security.entrega_evento` | depois do commit, a linha fica `PENDING` até o `publishNext`, mesmo já emitida | o `afterCommit` reivindica, emite e marca `ACKED`; só uma falha deixa `PENDING` |
| `security.evento_seguranca` | — | novo tipo `GLOBAL_SELF_ACTION_RESTORED` (texto livre, sem enum no banco) |

**Dado existente:** nenhuma migração. Linhas encerradas não são tocadas. O que **não** entra no banco:
a regra "só a ação padrão volta" vive no worker, não em constraint, porque depende do catálogo do
consumidor, que não está no SQL de forma confiável (research R2).

---

## 4. Backend

A correção mora no **Java-GEMS-SDK**, dono do mecanismo. O sample não ganha código de produção.

### 4.1 Java-GEMS-SDK `gems-firebase-auth`

| Arquivo | Situação |
| :--- | :--- |
| `jdbc/JdbcIdentityCommandWorker.java` | **alterado**: construtor novo com `AuthorizationCatalog`; o antigo vira `@Deprecated`; devolução no ramo `ENABLE` do `acknowledge` |
| `jdbc/SecurityJdbcSupport.java` | **alterado**: o `afterCommit` reivindica a própria entrega em `REQUIRES_NEW` |
| `config/FirebaseJdbcAutoConfiguration.java` | **alterado**: o bean do worker recebe o catálogo |
| `docs/FIREBASE-AUTH.md`, `RELEASE-NOTES-3.5.0.md`, `CLAUDE.md` | **alterado/novo**: documentação da 3.5.0 |
| `pom.xml` (todos, via `versions:set`) | **alterado**: 3.5.0 |

### 4.2 Sample `backend`

| Arquivo | Situação |
| :--- | :--- |
| `pom.xml` | **alterado**: `<gems.version>` 3.4.1 → 3.5.0 |
| `app-deploy/src/test/...` | **alterado**: testes (ver §6) |

A aplicação do sample usa o bean da autoconfiguração (`JobsConfiguration.java:83`), então o job
`identity-worker` recebe o comportamento novo só com a troca de versão.

### 4.6 Onde cada regra vive

| Regra | Onde | Novo? |
| :--- | :--- | :--- |
| Só a ação padrão volta, e só no ACK atual | `JdbcIdentityCommandWorker.acknowledge`, ramo `ENABLE` | novo |
| Ação precisa estar no catálogo | mesmo ponto, via `AuthorizationCatalog.globalActions()` | novo (mesma regra de `defaultSelfAction`) |
| ENABLE obsoleto não concede | `current(command)` antes do CAS | reuso |
| Sem duplicata | `INSERT ... WHERE NOT EXISTS` | novo |
| Entrega única no caminho normal | `SecurityJdbcSupport` `afterCommit` + CAS da outbox | novo |

### 4.7 Contrato REST

Nenhum endpoint muda. O efeito observável: depois de uma reativação confirmada, `GET /auth/me` volta
a responder **200** (antes, 403), e o contexto de tenant continua 403 até um vínculo novo. Mudança
decidida pelo product owner (§2, decisão 1).

---

## 6. Testes esperados por bloco

Linha de base: 290 testes no reator do sample (CI run 38295576662, merge `c111d8e`); 92 anotações
`@Test` em `gems-firebase-auth`, 48 delas em `JdbcSecurityPostgresTest` (contagem estática em
2026-10-12; a contagem executada entra no T001).

### 6.2 Backend — SDK (`JdbcSecurityPostgresTest`, Testcontainers)

| Teste | Situação | O que prova |
| :--- | :--- | :--- |
| T006 devolução | novo | Só a ação padrão volta, como linha nova; vínculos e outras globais continuam encerrados; evento emitido |
| T007 concessão já vigente | novo | Sem duplicata |
| T008 catálogo sem a ação | novo | Ação retirada pelo consumidor não volta |
| T009 construtor antigo | novo | Compatibilidade 3.4.1 |
| T009b reprocessamento e usuário vigente | novo | SC-004 e edge cases 3 e 4 |
| T012/T013 cancelado ou obsoleto | novo | FR-005: nenhuma devolução fora do ACK atual |
| T015/T016/T017 entrega de evento | novo | Entrega única, `ACKED` visível por outra conexão (`REQUIRES_NEW`), reentrega após falha, sem emissão se o dispatcher já reivindicou |
| T018b retry, poison, lease, restart | **existente ajustado** | A garantia original de cada um continua provada |
| Mutações T011, T014, T019 | prova | Cada rede nova falha quando o alvo é quebrado |

### 6.2b Backend — sample (`app-deploy`)

| Teste | Situação | O que prova |
| :--- | :--- | :--- |
| T028 E2E Firebase real | existente ajustado | Reativado: `/auth/me` 200, nenhuma empresa, 1 concessão vigente |
| T029 contrato de lifecycle | novo | Reativação cancelada não concede |
| T030 job `identity-worker` | novo + existente ajustado | Cada evento chega 1 vez ao publisher do sample; restart da outbox continua provado |
| T031 mutação no consumidor | prova | A garantia nomeada depende do SDK novo (Constituição, adoção de módulo, prova 2) |
| Demais 290 | não-regressão | Gate `mvnw.cmd clean install` |

### 6.3 Frontend

Sem mudança. `npm test` e `npm run build` rodam no gate (T033) só como não-regressão.

### 6.4 Validação manual

[quickstart §4](quickstart.md#4-verificação-humana): suspender, reativar e entrar com o Tester.
A tela abre com "Nenhuma empresa disponível".

---

## 7. Interação e isolamento

**A pergunta direta: esta feature é isolada o suficiente para não impactar as demais?**

**Resposta: parcialmente.** A devolução é isolada no ACK do `ENABLE`. A entrega de eventos é
**alterada e compartilhada** por todo evento de segurança do SDK.

| Recurso | Natureza | Quem é afetado | O que garante |
| :--- | :--- | :--- | :--- |
| ACK do `ENABLE` | **Alterado compartilhado** | reativação (003, homologada) | T006–T014, E2E T028 |
| `afterCommit` dos eventos | **Alterado compartilhado** | todos os eventos de segurança e o `SecurityEventBridge` do sample | T015–T018b, T030; o consumidor continua deduplicando por UUID |
| Construtor do worker | Novo compartilhado (antigo preservado) | consumidores do SDK | T009; compilação da 3.4.1 preservada |
| Suspensão, vínculos, grants manuais | Reuso sem alteração | 003 | 290 testes do sample |

### Fronteiras arquiteturais respeitadas

Nenhum módulo novo, nenhuma dependência nova, nenhum schema novo. O sample não ganha classe de
produção nem contorno local (Constituição, adoção de módulo).

### O que fica frouxo depois desta feature

- A regra "só a ação padrão volta" depende de código no worker, não de constraint.
- Consumidor que instancia o worker pelo construtor antigo continua com o defeito da 3.4.1 até migrar;
  o `@Deprecated` e as release notes avisam.
- A entrega continua "pelo menos uma vez" depois de falha: o consumidor ainda precisa deduplicar.

---

## 8. Divisão de tasks e delegação por modelo

**37 tarefas** em 8 fases. Distribuição: US1 9 (T006–T011, T009b, T027, T028) · US2 4 (T012–T014,
T029) · US3 7 (T015–T019, T018b, T030) · infraestrutura, release e fechamento 17.

| Fase | Escopo | Tarefas | Modelo | Gate de saída |
| :-- | :--- | :--- | :--- | :--- |
| 1–2 | Setup e construtor | T001–T005 | Sol (Codex) medium | reator do SDK compila; testes atuais verdes |
| 3–5 | US1, US2, US3 no SDK, testes antes | T006–T019, T009b, T018b | Sol (Codex) medium | testes novos verdes, mutações observadas pelo coordenador |
| 6 | Docs, review, gate, PR | T020–T024 | Sol (docs) · Sol read-only (review) · coordenador (gate) | `mvn clean verify` verde; review `accept` |
| 6 | Release `v3.5.0` | T025 | **Usuário** | `release.yml` verde |
| 7 | Adoção no sample | T026–T032 | Sol (Codex) medium · Sol read-only (review) | E2E real verde; mutação no consumidor |
| 8 | Fechamento | T033–T035 | Coordenador · **Usuário** (T034) | gate completo; verificação humana; CI verde |

### Critério de delegação

Custo mínimo sujeito aos gates. O desenvolvimento vai para o Sol, porque há espelho (Testcontainers e
E2E real) e teste antes; a preferência do usuário é poupar o Claude. Nenhum uso de Opus como executor:
o coordenador só contrata, observa as provas e aceita. Review independente é obrigatório, porque as
flags de segurança e persistência são verdadeiras.

### Ordem e paralelismo

```
T001→T005 → US1 → US2 → US3 → docs/review/gate → PR SDK → release (usuário) → adoção sample → gate → humano → PR
```

Ordens obrigatórias: a fase 3 desta rodada só começa depois que a
[006](../006-formatter-gems-ai/roteiro.md) formatar o Java-GEMS-SDK (decisão 5 da 006, de
2026-10-12), e a 3.5.0 sai no formato novo; teste antes da implementação em cada história; T018b depois de T018; nada do
sample antes da tag publicada (T025). Sem paralelismo de escritores: US1, US2 e US3 dividem o mesmo
arquivo de teste.

**MVP = US1 + US2** (o defeito e a garantia de segurança dele). US3 pode ser cortada se a rodada
apertar, mas aí volta a ser uma observação aberta.

---

## 9. Prompts SDD da rodada

| Ordem | Comando | Situação | Saída |
| :-- | :--- | :--- | :--- |
| 1 | `/speckit-specify` | ✅ 2026-10-12 | [spec.md](spec.md), [checklist](checklists/requirements.md) 100% |
| 2 | `/speckit-clarify` | ✅ dispensado | as decisões foram fechadas com o product owner na fase 1 (§2); a spec saiu sem `[NEEDS CLARIFICATION]` |
| 3 | `/speckit-plan` | ✅ 2026-10-12 | [plan.md](plan.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/sdk-gems-firebase-auth-3.5.0.md), [quickstart.md](quickstart.md). Hook `agent-context` não executado: o `CLAUDE.md` não tem os marcadores SpecKit e há job de paridade de contexto no CI |
| 4 | `/speckit-tasks` | ✅ 2026-10-12 | [tasks.md](tasks.md), 37 tarefas |
| 5 | `/speckit-analyze` | ✅ 2026-10-12 | 0 CRITICAL, 2 HIGH, 2 MEDIUM, 2 LOW, todos incorporados |
| 6 | **`roteiro.md`** | ✅ | este documento |
| 7 | `/speckit-implement` | 🔒 | após aprovação |

### Achados do `/speckit-analyze` já incorporados

| Achado | Sev. | Decisão |
| :--- | :--- | :--- |
| U1 — `afterCommit` com `REQUIRED` não confirma | HIGH | `REQUIRES_NEW` em research R5, data-model e T018; T015 lê `ACKED` por outra conexão |
| I1 — testes que esperam `PENDING` | HIGH | Inventário em research R5; tarefas T018b (SDK) e T030 (sample); impacto na §7 |
| C1 — edge cases sem teste | MEDIUM | T009b |
| D1 — gate de frontend ausente | MEDIUM | T033 inclui `npm ci/test/build` e `prettier` |
| A1 — dispensa do clarify | LOW | registrada nesta §9 |
| A2 — hook agent-context | LOW | registrado nesta §9 |

---

## 10. Riscos e mitigações

| # | Risco | Gravidade | Mitigação |
| :-- | :--- | :--- | :--- |
| 1 | A mudança no `afterCommit` altera todos os eventos e quebra testes ou consumidores | alta | T015–T018b, T030; dedup por UUID continua no contrato; review independente |
| 2 | Devolver acesso em reativação anulada | alta | `current(command)` antes do CAS; T012/T013 com mutação T014 |
| 3 | Release do SDK atrasa e bloqueia a adoção | média | O sample fica bloqueado de propósito (condição 1); a 005 não depende da 004 |
| 4 | Lentidão ou indisponibilidade do Firebase real no E2E | média | Padrão da 003 (namespace por execução, limpeza garantida) |
| 5 | Relógio do host atrasado afeta revogação no E2E local | baixa | Já tratado no teste na 003 |

---

## 11. Fora de escopo

| Item | Por quê | Volta em |
| :--- | :--- | :--- |
| Devolver vínculos, perfis, grupos ou outras ações globais | Decisão 1 da §2 | não volta; concessão manual |
| Migração de dados de usuários já reativados | Só há dados de teste (C6) | não volta |
| Remover o construtor antigo do worker | Exigiria MAJOR | próxima MAJOR do SDK |
| Mensagem de tela para "conta sem acesso" | Com a correção, o caso deixa de ocorrer no fluxo normal | rodada futura, se o product owner pedir |
| Frontend, nginx, documentação e scripts | Rodada própria | [005](../005-endurecimento-frontend-docs/roteiro.md) |

---

## 12. Critérios de aprovação

### Para aprovar esta rodada (antes do código)

- [ ] O escopo da §1 é o que se espera da próxima entrega
- [ ] As decisões da §2 refletem o que foi combinado
- [ ] O impacto da §7 é aceitável
- [ ] A cobertura de testes da §6 é suficiente para o risco assumido
- [ ] A decisão de delegação (inclusive `sem delegação`) da §8 está adequada
- [ ] Os itens fora de escopo (§11) podem mesmo esperar

### Para considerar a rodada concluída (depois do código)

- [ ] `mvn --batch-mode clean verify` verde no Java-GEMS-SDK e `v3.5.0` publicada
- [ ] `.\mvnw.cmd clean install` verde no reator inteiro do sample, com testes e Firebase real
- [ ] `npm test` e `npm run build` verdes
- [ ] Aplicação sobe com `ddl-auto: validate` sem reclamar de schema
- [ ] Os gates do [quickstart.md](quickstart.md) marcados
- [ ] **Zero regressão** nas features já entregues
- [ ] `/speckit-analyze` final sem CRITICAL e sem violação de constituição
- [ ] Arquivos de agente atualizados

---

## Referência dos artefatos

| Arquivo | Para quê |
| :--- | :--- |
| [spec.md](spec.md) | Requisitos, user stories, critérios de sucesso |
| [plan.md](plan.md) | Decisões técnicas, Constitution Check, estrutura, delegação |
| [research.md](research.md) | Por que cada decisão técnica, incluindo o diagnóstico do ACK duplicado |
| [data-model.md](data-model.md) | Transições do ACK e da outbox |
| [contracts/](contracts/sdk-gems-firebase-auth-3.5.0.md) | Delta público do SDK 3.5.0 |
| [tasks.md](tasks.md) | Tarefas, dependências e ordem |
| [quickstart.md](quickstart.md) | Validação |
| [checklists/](checklists/requirements.md) | Qualidade da especificação |