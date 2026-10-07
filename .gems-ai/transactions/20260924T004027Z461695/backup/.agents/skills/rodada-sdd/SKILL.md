---
name: rodada-sdd
description: Conduz uma rodada de SDD deste projeto de ponta a ponta — descobre em que fase a rodada está (Start, Plan, SDD, Homologação, História), executa somente essa fase e para no portão humano seguinte. Use quando o Codex precisar retomar, avançar ou fechar uma rodada `specs/<NNN>-<slug>/`, inclusive depois de compactação de contexto ou de nova sessão. Não use para bug fix nem para edição exclusivamente documental fora de uma rodada.
---

# Rodada SDD — roteador de fase (adaptador do Codex)

Esta skill é o **mesmo roteador** de `.claude/skills/rodada-sdd/SKILL.md`, para o Codex. Ela **aponta,
nunca duplica**: o processo de cada fase vive nas `references/` daquela pasta, e é de lá que se lê.
Duas cópias do mesmo procedimento divergem e ninguém descobre qual está certa até o defeito aparecer.

## Passo 1 — Resolver qual rodada

O argumento vence tudo (`010`, `010-habilitacao-de-agentes` ou o caminho). Sem argumento, colete os
**três** sinais antes de decidir — não pare no primeiro:

| Sinal | Como ler | Confiabilidade |
| :--- | :--- | :--- |
| Branch | `git branch --show-current`, casada contra os diretórios de `specs/` | alta, quando é branch de feature |
| Ledger/plano de onda | primeira linha com ciclo em aberto em `specs/programa-estabilizacao/README.md` ou no `README.md` do plano de onda ativo | alta |
| `.specify/feature.json` | chave `feature_directory` | **baixa** — envelhece; só desempate, nunca contra a branch |

Sinais discordantes, ou nenhum: **pergunte** ao usuário. Guarde o caminho como `RODADA`.

## Passo 2 — Detectar a fase

Leia o `roteiro.md` da rodada e verifique **da fase 5 para a 1** (a mais adiantada vence). "Seção de
critérios" é a que tem o título **"Critérios de aprovação"** — localize pelo título, não pelo número
(é a §12 no template, §13 na `005`).

| # | Sinal | Fase |
| :-- | :--- | :--- |
| 5 | seção de critérios tem `✅ HOMOLOGADA` com data **e** `business/RF0NN-*.md` ainda descreve a feature como planejada | 5 — História |
| — | `✅ HOMOLOGADA` **e** `business/` já atualizado | **encerrada** — reporte e ofereça a próxima |
| 4 | `✅ PORTÃO APROVADO` **e** todas as tarefas do `tasks.md` estão `[x]` | 4 — Homologação |
| 3 | `✅ PORTÃO APROVADO` com data, e há tarefa `[ ]` no `tasks.md` | 3 — SDD |
| 2 | artefatos existem e o `/speckit-analyze` já rodou (§9, linha 5), mas a seção de critérios não está assinada | 2 — Plan |
| 1 | qualquer outro caso (rodada não existe, §9 com `⚠️ antecipado`/`⛔ pendente`, sem `roteiro.md`) | 1 — Start |

A **§9 e a seção de critérios do `roteiro.md` são o registro de progresso**. Não crie arquivo de
estado — leia e escreva nelas.

## Passo 3 — Anunciar e executar

Diga em três linhas: **qual rodada**, **qual fase**, **qual sinal**. Depois leia **apenas** a
referência da fase e siga-a:

| Fase | Referência (leia o arquivo; não há cópia aqui) |
| :--- | :--- |
| 1 — Start | `.claude/skills/rodada-sdd/references/fase-1-start.md` |
| 2 — Plan | `.claude/skills/rodada-sdd/references/fase-2-plan.md` |
| 3 — SDD | `.claude/skills/rodada-sdd/references/fase-3-sdd.md` |
| 4 — Homologação | `.claude/skills/rodada-sdd/references/fase-4-homologacao.md` |
| 5 — História | `.claude/skills/rodada-sdd/references/fase-5-historico.md` |

Formato do roteiro: `.specify/templates/roteiro-template.md`. Checklist A–F de fechamento:
`.claude/skills/rodada-sdd/references/checklist-de-fechamento.md`. Delegação a outro modelo: skill
`model-delegation` (adaptador de `.claude/skills/delegacao-de-modelos/`).

**Uma fase por invocação.** Na fase 3, quando houver ledger de programa, **um bloco por invocação**.
Terminada a fase, pare e diga o que o usuário precisa fazer para liberar a seguinte.

## Regras que valem em todas as fases

- **Portão não se pula nem se registra só no chat.** As fases 2 e 4 terminam em portão humano; o OK só
  está registrado quando gravado na seção de critérios do `roteiro.md`, com data, na forma dos blocos
  `> ✅` de `specs/001-parceiro-e-solicitacao/roteiro.md`. Nenhuma linha de código de feature antes do
  portão da fase 2 (Princípio VIII).
- **Gates são seus.** `cd backend; .\mvnw.cmd clean install`, `cd frontend; npm test; npm run build`
  — rode e **leia a saída**; não presuma. Reporte falha com a saída real.
- **Documentação diverge do código: o código vence, e a divergência é reportada** — nunca corrigida em
  silêncio nem contornada.
- **Não invente escopo.** Um ⛔ nos planos é decisão que falta ao usuário; pergunte.
- **Nada em duplicata**: todo artefato aponta para a fonte em vez de copiá-la.
- Ao fim das fases 2 e 4, recomende ao usuário compactar o contexto; não afirme que compactou.
