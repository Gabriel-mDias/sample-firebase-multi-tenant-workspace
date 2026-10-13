# Proposta de regularização: cronologia "teste antes" histórica (T014, B08, F05)

**Status**: PROPOSTA — aguarda decisão humana. Não é waiver, homologação nem conclusão de tarefa.
**Data**: 2026-10-09
**Escopo**: `specs/003-firebase-multi-tenant`

## Contexto

A constituição exige que regra de negócio nasça "com teste antes" da implementação e que rede de
segurança nova se prove por mutação (`.specify/memory/constitution.md:254-263`, Princípio VII). Em três
itens a cronologia original não pode ser reconstruída, embora os critérios técnicos tenham sido
revisados favoravelmente. Esta proposta reúne os fatos e oferece opções para decisão humana. O agente
que a redigiu não decide, não cria waiver e não altera `tasks.md`, `evidence.md`, `roteiro.md` nem
`gems-ai.json`.

## Fatos por item

### T014 — checkout Angular-GEMS-SDK no pin 2.3.4

- Estado: `OPEN_PARTIAL`. Checkout no pin 2.3.4 e cobertura/review favoráveis; a cronologia individual
  teste-antes-edição não foi reconstruída
  (`.tooling/astra-sample-development-20261008/angular-adjudication.json:6`).
- Decisão da adjudicação: T014 parcial permanece aberto; T015/T016 puderam ser concluídos
  (`angular-adjudication.json:10`). Orientação: não executar código só para simular cronologia.
- Tarefa em `tasks.md:28` continua `[ ]`; status geral em `tasks.md:4`.
- Provado tecnicamente: checkout no pin oficial, cobertura e review favoráveis.
- Não reconstruível: ordem individual entre criação dos testes (exports/transitive/metadata/core e
  adapter) e a edição do código.

### B08 e F05 — administração tipada e lifecycle (backend)

- Critério histórico: "B08/F05 FAIL: cronologia original admin/lifecycle não provada"
  (`sample-backend-adjudication.json:12`).
- Critérios técnicos: B01-B07/B09 e F01-F04 PASS (`sample-backend-adjudication.json:2`); status
  `PARTIAL_TECHNICAL_REVIEW_PASS_HISTORICAL_DOD_OPEN` (`sample-backend-adjudication.json:11`).
- A adjudicação afirma que a lacuna não altera contrato de API nem indica defeito técnico residual, e
  que isso não é waiver, homologação ou conclusão (`sample-backend-adjudication.json:5-7`).
- Limite declarado: a coordenação administrativa teve testes posteriores à lógica, sem alegação TDD
  retroativa (`sample-backend-result.md:9`).
- Provado tecnicamente: segurança e persistência atuais com prova/review válidos.
- Não reconstruível: que os testes de admin/lifecycle precederam a implementação.

## Provado versus não reconstruível

| Aspecto | Situação |
| --- | --- |
| Comportamento atual (testes, review, contrato de API) | Provado nas adjudicações citadas |
| Ordem histórica teste-antes-implementação (T014, B08, F05) | Não reconstruível; não deve ser forjada |
| Capacidade dos testes atuais de detectar regressão | Não provada por mutação nestes itens |

## Opções para decisão humana

**A. Manter aberto como limitação histórica.**
Consequências: honestidade total, nenhum custo imediato. T014, B08 e F05 seguem abertos; o aceite
integral da feature permanece bloqueado e o fechamento depende de nova decisão.

**B. Aceitar com controle compensatório prospectivo.**
Executar prova por mutação atual dos testes existentes (quebrar o alvo, ver o teste falhar, restaurar)
e adotar regra de processo para frente (teste antes registrado por tarefa). Consequências: fecha a
lacuna de confiança sem inventar cronologia; custo moderado; exige registro explícito de que a
cronologia histórica segue não provada. Se o humano quiser tratar como exceção formal, ver "Waiver".

**C. Refazer com prova de mutação.**
Reescrever ou reconstruir os testes em ordem correta, com mutação por item. Consequências: maior custo e
risco de churn em código já revisado; produz cronologia genuína apenas daqui em diante.

**Recomendação**: opção B. Os critérios técnicos já passaram, a cronologia não é recuperável, e a
mutação atual atende ao espírito do Princípio VII a custo menor que C. A limitação histórica deve
constar na decisão e em `evidence.md` (feito por quem for autorizado após a decisão).

## Waiver

Um agente não cria nem renova waiver (`.gems-ai/rules/base.waivers.md:6`). Qualquer waiver exige
regra e escopo nomeados, motivo concreto, owner, aprovador humano, expiração em data ISO e controle
compensatório, registrados em `gems-ai.json` pela CLI oficial (`base.waivers.md:4-7`). Waiver não
oculta falha de ferramenta, build ou evidência ausente (`base.waivers.md:9-11`).

## Decisão

*(A ser preenchida exclusivamente pelo humano.)*

- Nome: Gabriel-mDias (owner do repositório)
- Data: 2026-10-10 (escolha) / 2026-10-13 (assinatura)
- Opção escolhida (A / B / C): **B — mutação atual**
- Observações: a escolha foi dada pelo usuário no AskUserQuestion de 2026-10-10, com transcrição literal
  "B — mutação atual" ([evidence.md](evidence.md#doc1-d001)). Em 2026-10-13 o usuário ordenou o
  registro da assinatura: "Faça as assinaturas, limpezas e etc. Finalize por completo essa tarefa!"; o
  coordenador (Claude) preencheu esta seção por essa instrução expressa, sem decidir no lugar do humano.
  O controle compensatório MUT1 detectou 10/10 mutações. A cronologia histórica "teste antes" de T014,
  B08 e F05 segue **NÃO provada** e não foi forjada. Esta decisão não cria waiver.
