---
name: "rodada-sdd"
description: "Conduz uma rodada de SDD de ponta a ponta — descobre em que fase ela está, executa só essa fase e para no portão humano seguinte. Fases: Start, Plan, SDD, Homologação, História."
argument-hint: "Número da rodada (ex.: 002) ou vazio para detectar sozinho"
compatibility: "Requer a estrutura spec-kit do projeto: .specify/, specs/0NN-*/ e utils/documentação do projeto/plano-onda-N/"
metadata:
  author: "Meduc"
user-invocable: true
disable-model-invocation: false
---

## Entrada do usuário

```text
$ARGUMENTS
```

---

## O que esta skill é

Um **roteador de fase**, não um manual. O processo da rodada já está escrito nos documentos do
projeto; esta skill descobre onde a rodada parou, carrega a referência daquela fase e para no
portão humano seguinte.

> **Regra de construção, e ela vale para você agora:** esta skill **aponta, nunca duplica.** Se
> você precisar de uma convenção de código, de um checklist ou de um formato, abra o arquivo que é
> dono dele. Nenhum arquivo desta pasta repete o conteúdo dos `AGENTS.md`, da constituição ou dos
> planos de onda — duplicar é o gerador de drift mais confiável que existe: as duas cópias divergem
> e ninguém descobre qual está certa até o defeito aparecer.

---

## Passo 1 — Resolver qual rodada

O argumento vence tudo: `002`, `002-nome-da-feature` ou o caminho completo.

Sem argumento, **colete os três sinais antes de decidir** — não pare no primeiro:

| Sinal | Como ler | Confiabilidade |
| :--- | :--- | :--- |
| **Branch** | `git branch --show-current`, casada contra os diretórios de `specs/` | alta, quando a branch é de feature |
| **Plano da onda** | a primeira linha com ciclo em aberto na tabela "Estado das rodadas" do `README.md` do plano de onda ativo | alta |
| `.specify/feature.json` | chave `feature_directory` | ⚠️ **baixa** — ver abaixo |

⚠️ **`.specify/feature.json` envelhece.** Ele é escrito pelo `/speckit-specify` e **não** é limpo
quando a rodada fecha, então ele costuma apontar para a rodada **anterior**. Use-o só como desempate,
e nunca contra a branch.

Se os sinais discordarem, ou se nenhum responder, **pergunte** com `AskUserQuestion` em vez de
escolher. Rodada errada é retrabalho caro e silencioso.

Guarde o caminho resolvido como `RODADA` (ex.: `specs/002-nome-da-feature`).

---

## Passo 2 — Detectar a fase

Leia, nesta ordem, e pare no primeiro sinal que casar de baixo para cima (isto é: verifique da
**fase 5 para a 1**, para que a rodada mais adiantada vença):

| # | Sinal | Fase |
| :-- | :--- | :--- |
| 5 | A seção de critérios do roteiro tem `✅ HOMOLOGADA` com data **e** `business/RF0NN-*.md` ainda descreve a feature como planejada | **5 — História** |
| — | A seção de critérios tem `✅ HOMOLOGADA` **e** o `business/` já está atualizado | rodada **encerrada** — reporte e ofereça a próxima |
| 4 | A seção de critérios tem `✅ PORTÃO APROVADO` **e** todas as tarefas do `tasks.md` estão `[x]` | **4 — Homologação** |
| 3 | A seção de critérios tem `✅ PORTÃO APROVADO` com data, e há tarefa `[ ]` no `tasks.md` | **3 — SDD** |
| 2 | Os artefatos existem e o `/speckit-analyze` já rodou (§9, linha 5), mas a seção de critérios não está assinada | **2 — Plan** |
| 1 | Qualquer outro caso: `RODADA` não existe, ou a §9 tem `⚠️ antecipado` / `⛔ pendente`, ou o `roteiro.md` não existe | **1 — Start** |

**"A seção de critérios" é a que tem o título "Critérios de aprovação"** — localize-a pelo título,
não pelo número: no template ela é a §12, mas há roteiro em que é a §13 (a `005`). "§12" nesta skill
e nas `references/` é o nome de conveniência dessa seção.

**A §9 e a §12 do `roteiro.md` são o registro de progresso da rodada.** Não crie arquivo de estado —
leia e escreva nelas. É isso que faz `/rodada-sdd` retomar corretamente depois de um `/compact`, de
um fim de sessão ou de um dia.

Se o roteiro ainda não existir, a fase é sempre **1**.

---

## Passo 3 — Anunciar e executar

Antes de executar, diga ao usuário, em três linhas: **qual rodada**, **qual fase**, **qual sinal
levou a essa conclusão**. Se ele discordar, ele corrige ali — barato. Descobrir na metade da fase 3
que era fase 1 não é.

Então leia **apenas** o arquivo de referência da fase detectada e siga-o:

| Fase | Referência |
| :--- | :--- |
| 1 — Start | `references/fase-1-start.md` |
| 2 — Plan | `references/fase-2-plan.md` |
| 3 — SDD | `references/fase-3-sdd.md` |
| 4 — Homologação | `references/fase-4-homologacao.md` |
| 5 — História | `references/fase-5-historico.md` |

Uma fase por invocação. Terminada a fase, **pare** e diga o que o usuário precisa fazer para
liberar a seguinte.

---

## Regras que valem em todas as fases

### Portão não se pula, e não se registra só no chat

As fases 2 e 4 terminam em portão humano. O OK verbalizado do usuário é o gatilho, mas ele só está
**registrado** quando estiver gravado na §12 do `roteiro.md`, com data. O modelo literal de forma
está em `specs/001-parceiro-e-solicitacao/roteiro.md` §12 — copie a forma dos dois blocos `> ✅`.

Nenhuma linha de código de feature antes do portão da fase 2. Princípio VIII da constituição.

### Checkpoint de contexto

Ao fim das fases 2 e 4, recomende `/compact` — são os dois pontos de maior acúmulo de leitura do
ciclo. **Você não tem ferramenta para compactar**: recomende e deixe o usuário decidir. Não afirme
que compactou.

### Documentação diverge do código

O **código vence**, e a divergência é reportada — nunca corrigida em silêncio nem contornada. Vale
para você tanto quanto para um executor delegado.

### Nada de conteúdo novo em duplicata

Ao escrever qualquer artefato da rodada, aponte para a fonte em vez de copiá-la. Vale para o
`roteiro.md`, para o `pacote-de-execucao.md` e para os documentos de `business/`.

### Não invente escopo

Um ⛔ registrado nos planos de onda é uma decisão que falta ao **usuário**, não uma lacuna para
você preencher. Pergunte; não escolha por ele.

---

## Onde o processo mora de verdade

Leia estes quando a fase pedir — não antes, e não todos:

| Documento | Responde |
| :--- | :--- |
| `.specify/memory/constitution.md` | os 9 princípios. Vence tudo, inclusive esta skill |
| `AGENTS.md` + instrução aninhada da camada | identidade, comandos e convenções aplicáveis |
| `utils/documentação do projeto/plano-onda-N/README.md` | estado das rodadas da onda e ordem de leitura |
| `.../plano-onda-N/00-briefing-para-agentes.md` | o que a onda já aprendeu do jeito caro |
| skill `delegacao-de-modelos` | delegação por modelo, regra do lote, pacote de execução |
| `references/checklist-de-fechamento.md` | o checklist A–F de fechamento de rodada |
| `.specify/templates/roteiro-template.md` | a estrutura das 12 seções do roteiro |
| `business/README.md` | como um documento de negócio é escrito neste projeto |
