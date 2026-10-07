# Estado — Lote <X>, rodada <NNN>

> **Este arquivo é seu.** Você, o executor, escreve; o orquestrador só lê. Ele não some com a sua
> conversa, e é por isso que ele existe: **um estranho, sem o seu histórico, precisa conseguir
> assumir o lote daqui.** Esse é o teste que cada seção abaixo tem que passar.
>
> **A cadência é a regra inteira.** Fim de cota não é anunciado — ele simplesmente acontece na
> próxima chamada. Estado escrito no fim do lote é inútil exatamente para a falha que ele existe
> para cobrir.
>
> - a **§1**, ANTES da primeira edição de qualquer arquivo;
> - a **§3**, ao **abrir** cada tarefa — não ao fechá-la;
> - a **§2** e a **§5**, ao **fechar** cada tarefa.
>
> Contrato completo: [`contrato-do-executor.md`](../../.claude/skills/delegacao-de-modelos/references/contrato-do-executor.md).
> **Não apague seção**, e não deixe nenhuma em branco: uma seção vazia é conferida por comando na
> volta, e seção sumida é indistinguível de seção esquecida. Se ela não se aplica, escreva por quê.

---

## 1. Rollback deste lote

<!--
  ANTES DE EDITAR QUALQUER COISA. Um plano de rollback redigido por um executor que acabou
  de esgotar cota é o artefato menos confiável do processo; um redigido antes da primeira
  edição é apenas verdade. O orquestrador executa isto SEM depender de você.

  Duas linhas bastam, e o formato depende de onde você esta rodando:
    - em worktree proprio: `git worktree remove --force <caminho>` e apagar a branch;
    - na branch de trabalho: `git checkout --` nos arquivos rastreados da §1 do pacote, e
      `rm -f` nos arquivos novos, NOMEADOS UM A UM. O `-f` nao e preguica: um lote parado
      na metade nao criou todos, e um `rm` sem ele aborta no primeiro que faltar.
-->

> ⛔ **Nunca** `git checkout .`, `git reset --hard` ou `git clean -fd` sem caminho. A árvore contém
> trabalho que não é seu. Um rollback amplo transforma "lote não entregue" em "causou perda".

---

## 2. Concluído

<!-- A tarefa fechada E o comando que a provou. "Compilou" nao e prova se o portao e um teste. -->

| Tarefa | O que provou |
| :-- | :--- |

---

## 3. Em curso

<!--
  ESTE É O CAMPO QUE SALVA OU PERDE O HANDOFF; os outros são inventário. Preenchido ao
  ABRIR a tarefa: é no meio dela que a cota acaba, e o campo tem que estar escrito antes
  de ser necessário. Se nao ha tarefa aberta, diga "nenhuma" — nao deixe os rotulos secos.
-->

- **Tarefa:**
- **Arquivo:**
- **O que já está feito nele:**
- **O que falta:**

---

## 4. Não iniciado

<!-- As tarefas do pacote que sobraram, pelos ids. "Nenhuma" e resposta valida. -->

---

## 5. Arquivos tocados

<!--
  A saída LITERAL do comando abaixo, colada sem editar, MENOS a linha deste próprio
  arquivo — que é a única entrada previsível, porque é o arquivo que você está escrevendo.

  Não resuma e não reescreva de memória: é ela que o orquestrador confronta com a §1 do
  pacote, e a divergência entre o que você declara e o que a árvore mostra é o sinal mais
  barato de que o estado não é confiável.

  O bloco precisa ser ```text — é o que o `verificar-lote.ps1` lê.
-->

`git status --porcelain --untracked-files=all`

```text
```

---

## 6. Decisões fora do pacote

<!--
  O que você resolveu que o pacote não previa, e POR QUÊ. Sem isto, o sucessor refaz a
  escolha ao contrário. Vale também para o que você encontrou e NÃO tocou por estar fora
  da lista fechada — isso é informação valiosa, não obstáculo. "Nenhuma" é resposta
  válida, e é diferente de deixar vazio.
-->

---

## 7. O que está quebrado agora

<!--
  Parar no meio costuma deixar o build vermelho. DIGA. Omitir isto é o pior modo de falha
  do contrato inteiro, porque manda o sucessor procurar um defeito que é só trabalho pela
  metade. "Nada — o portão estava verde na última execução" é resposta válida.
-->

---

## 8. Retomada

<!--
  Só para executor externo, e a única seção opcional. O token de retomada permite continuar
  com o contexto preservado, o que é mais barato e mais fiel que qualquer handoff. Mas ele
  some com o executor: as §1 a §7 continuam obrigatórias, porque elas sobrevivem à troca de
  modelo, de família e de máquina.

  CADA FAMÍLIA CHAMA O SEU DE UM JEITO, e id sem dono não retoma nada. Preencha a linha do
  executor que você é, e apague a outra:
    - Codex  → `thread_id`, primeiro evento do JSONL (`thread.started`)
    - agy    → `conversation_id`, no corpo da resposta
-->

- **Executor:** <Codex | agy>
- **Token de retomada:** <`thread_id` ou `conversation_id`, com o valor>

