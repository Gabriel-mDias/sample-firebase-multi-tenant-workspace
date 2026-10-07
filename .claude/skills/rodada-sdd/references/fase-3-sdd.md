# Fase 3 — Rodada SDD

**Objetivo:** percorrer o ciclo spec-kit e entregar a implementação completa.

**Termina quando:** todas as tarefas do `tasks.md` estão `[x]`, os portões de build estão verdes, e
você exigiu do usuário a fase seguinte.

---

## 1. O ciclo, e o registro dele

`specify > clarify > plan > tasks > analyze > roteiro > implement`.

A **§9 do `roteiro.md`** é o registro de progresso — atualize a coluna Situação a cada comando, e
não no fim. É por ela que `/rodada-sdd` sabe retomar depois de um `/compact`.

| Ordem | Comando | Quando pular |
| :-- | :--- | :--- |
| 1 | `/speckit-specify` | se o `spec.md` já existe e a fase 1 o validou — registre "validado na fase 1", não "pulado" |
| 2 | `/speckit-clarify` | **nunca em silêncio.** Dispensar exige a razão escrita na §9 (exigência do §A do checklist de fechamento) |
| 3 | `/speckit-plan` | idem 1 |
| 4 | `/speckit-tasks` | idem 1 |
| 5 | `/speckit-analyze` | **nunca.** É pré-requisito da §12 |
| 6 | `roteiro.md` | é a fase 2 |
| 7 | `/speckit-implement` | 🔒 só depois do portão |

⚠️ **Artefato marcado "rascunho — revalidar" não conta como comando executado.** Quando os artefatos
de uma rodada foram escritos junto do planejamento da onda, antes do ciclo, rodar o comando sobre
eles é o trabalho, não uma formalidade: espere que o `/speckit-analyze` acrescente tarefas, e conte
com uma ordem de grandeza de dezenas por cento — trabalho que existia e não estava escrito.

### Os achados do analyze voltam para a fase 2

`/speckit-analyze` termina e você **não segue para o implement**. Os achados vão para:

- os artefatos, corrigidos de verdade — HIGH e MEDIUM **incorporados, não só listados**;
- a subseção "Achados do `/speckit-analyze` já incorporados" da §9, **incluindo os recusados, com a
  razão**. Achado sem decisão registrada volta como dúvida na revisão do PR;
- a §12, que só então pode ser fechada.

Achado **CRITICAL** pendente bloqueia o portão. Sem exceção.

---

## 2. Delegação — avaliar, propor, perguntar

O projeto pode delegar lotes a executores de outros modelos — subagente Claude ou CLI externo. A
decisão é **por rodada**, e é do usuário.

Havendo candidato que amortize o onboarding, **invoque a skill `delegacao-de-modelos`** e siga-a. Ela
cobre a elegibilidade pela regra do lote, a sondagem de quem está disponível, o roteamento por tier,
a proposta ao usuário, o disparo com isolamento e a verificação na volta. Não reproduza aqui o que
ela faz.

Três coisas continuam suas, delegando ou não:

- **Você é o executor mais barato para as fases de julgamento** — já carregou o contexto. Delegar a
  fase central da rodada é pagar de novo pelo que está na mesa.
- **Nenhum delegado comita.** Ele entrega e reporta; você verifica o portão e comita.
- **O que for delegado precisa bater** com a nota de delegação do `tasks.md` e a coluna `Modelo` do
  `plan.md` — Princípio VIII. Divergiu, corrija o artefato ou o recorte, e não os dois em silêncio.

---

## 3. Implementar

### Antes de qualquer coisa: a rodada é conduzida por blocos?

Se existir `specs/programa-estabilizacao/README.md`, **abra-o primeiro**. Ele é o ledger do programa e
responde "onde o programa parou": a tabela de blocos diz qual é o primeiro bloco não-verde, e a coluna
`Retomar por` nomeia o `estado-lote-<bloco>.md` que tem o ponto exato de parada. O protocolo de
retomada, o repositório onde os commits caem e as regras de execução estão lá — **não os reproduza
aqui nem os peça ao usuário.**

Com o ledger aberto, a retomada inteira é: primeiro bloco não-verde → o `estado-lote` dele → executar
**um bloco**, fechá-lo e parar. Nada mais precisa entrar em contexto, e nada mais precisa ser digitado
além de `/rodada-sdd`.

Sem esse arquivo, a rodada é conduzida pelo `tasks.md` direto, como abaixo.

Siga o `tasks.md` na ordem das fases, respeitando os checkpoints de cada uma.

**Test-first onde o `plan.md` marcou.** É exigência do Princípio VII, e nas fases marcadas o teste
vem antes da regra.

As convenções são as de `backend/AGENTS.md` e `frontend/AGENTS.md`. **Não as reproduza aqui** —
abra-as. Antes de começar, leia também o `00-briefing-para-agentes.md` do plano da onda ativa: ele é
curto e lista o que a onda já aprendeu do jeito caro, incluindo os itens que produziram defeito
silencioso.

### A cada lote entregue

O §B do `references/checklist-de-fechamento.md`. Em resumo, e sem substituir a leitura dele: reator
**completo** com `clean install` quando a mudança é transversal, `npm test` e `npm run build`,
`prettier` se tocou frontend, e a saída lida — não presumida.

### Rede nova prova-se por mutação

Todo teste escrito para proteger uma regra nova precisa ser provado: **quebre o alvo, confirme que
o teste falha, restaure**. É comum escrever uma rede que passa com e sem a guarda que ela existe
para proteger — e só a mutação revela isso.

---

## 4. Encerrar a fase

Marque as tarefas no `tasks.md`, atualize a §9, e reporte no formato do §F do checklist de
fechamento: o que foi feito, o que não foi e por quê, o que ficou frouxo, e as divergências entre
documentação e código.

**Se algo falhou, mostre a saída real.**

Então **exija a fase seguinte**: diga ao usuário, com todas as letras, que a implementação está
completa e que a rodada só avança pela **homologação** — e que ela começa com `/rodada-sdd`.
