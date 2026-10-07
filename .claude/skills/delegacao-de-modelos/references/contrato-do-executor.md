# Contrato do Executor Delegado

**Vale para qualquer executor de lote** — subagente Claude, CLI externo ou outro. Não é específico de
ferramenta: o pacote de execução nomeia o caminho deste arquivo explicitamente, para que um executor
que não lê `.claude/` ainda o receba.

Este documento **não repete** o que já tem dono:

| Você precisa de | Leia |
| :--- | :--- |
| Como se portar como delegado, e o que entregar sempre | `00-briefing-para-agentes.md` do plano da onda ativa |
| O que o **seu** lote faz, as convenções dele, o portão | o `pacote-de-execucao-lote-<X>.md` que você recebeu |
| A forma exata do que você devolve | `.specify/templates/estado-lote-template.md` |
| A regra do lote e o formato do pacote | `regra-do-lote-e-disparo.md`, ao lado deste arquivo |

**São dois arquivos, e cada um tem um autor só.** O pacote é o pedido: o orquestrador escreve, você
lê. O `estado-lote-<X>.md` é a resposta: você escreve, ele lê. Nunca o contrário — é isso que faz o
protocolo funcionar sem ninguém pisar na linha do outro.

O que ele acrescenta é uma coisa só: **o que acontece quando você não termina.** Um executor pode
esgotar cota, travar ou errar no meio do lote. As três seções abaixo existem para que isso custe um
comando ao orquestrador, e não uma investigação.

---

## 1. Leitura e escrita são fechadas

A **§1 do seu pacote** tem uma lista `READ` e uma tabela `EDIT / CREATE`. Você não pesquisa nem lê
fora de `READ`, e não cria, edita ou apaga fora da tabela — nem para "corrigir de passagem".

Encontrou algo que exige ler ou tocar fora das listas? **Pare e reporte.** É decisão do orquestrador,
e quase sempre significa que o pacote está errado — que é informação valiosa, não um obstáculo.

**Isto é verificado, não confiado.** Ao receber o lote, o orquestrador roda

```bash
git status --porcelain --untracked-files=all      # modificados + não rastreados, de uma vez
```

e compara com a lista da §1. **Um único arquivo fora dela reprova o lote inteiro e dispara o
rollback — mesmo com o portão verde.** Não é punição: é o que torna o rollback exato possível. Um
lote cuja extensão real não se conhece não pode ser desfeito com segurança.

Duas exceções de escrita, e só elas: o seu **`estado-lote-<X>.md`** (a §2 abaixo) e os arquivos que o
portão gera sem serem versionados (`target/`, `node_modules/`, `dist/`) — todos já cobertos pelo
`.gitignore`.

⚠️ **O pacote não está entre as exceções: ele é somente leitura para você.** Quem o escreve é o
orquestrador, e é o registro do que foi pedido. Editá-lo no meio do lote — para anotar progresso,
para corrigir o que parece errado — apaga a diferença entre o que foi pedido e o que foi entregue,
que é justamente o que a verificação na volta compara.

Sobrou artefato de ferramenta que o `.gitignore` não cobre — cache de linter, metadado de IDE, algo
que o ambiente gerou sem você pedir? **Registre no relatório em vez de apagar por conta própria.** O
orquestrador distingue artefato de ambiente de arquivo que você tocou; você, de dentro do lote, não
tem como provar a diferença — e apagar o que não é seu é o começo do problema que o rollback existe
para evitar.

---

## 2. Checkpoint — a cada tarefa, nunca no fim

**Antes da primeira edição, crie o seu `estado-lote-<X>.md`**, ao lado do pacote, a partir de
[`.specify/templates/estado-lote-template.md`](../../../../.specify/templates/estado-lote-template.md).
O esqueleto já traz as oito seções na ordem em que se preenche — siga-a e não apague seção.

É **arquivo seu**: você escreve, o orquestrador só lê. Dois lotes paralelos nunca disputam a mesma
linha, e ele acompanha o worktree quando há um.

> **A cadência é a regra inteira.** Fim de cota não é anunciado — ele simplesmente acontece na
> próxima chamada. Checkpoint escrito ao final do lote é inútil exatamente para a falha que ele
> existe para cobrir.
>
> São **dois** momentos, e o segundo é o que costuma faltar:
>
> 1. **Ao abrir uma tarefa**, escreva o campo "Em curso" com o que você vai fazer e onde.
> 2. **Ao fechá-la**, mova-a para "Concluído" e abra a seguinte.
>
> Só escrever entre tarefas deixa "Em curso" sempre vazio — e é exatamente no meio de uma tarefa que
> a cota acaba. O campo tem que estar preenchido *antes* de ser necessário.

Seis campos, as §2 a §7 do arquivo. O teste que eles precisam passar: **um estranho, sem o seu
histórico de conversa, consegue assumir daqui?**

1. **§2 Concluído** — as tarefas fechadas, e o comando que provou cada uma. "Compilou" não é prova se
   o portão do pacote é um teste.
2. **§3 Em curso** — a tarefa aberta e **em que ponto exatamente**: qual arquivo, o que já está nele,
   o que falta. Este é o campo que salva ou perde o handoff; os outros cinco são inventário.
3. **§4 Não iniciado** — as tarefas que sobraram.
4. **§5 Arquivos tocados** — a saída **literal** de `git status --porcelain --untracked-files=all`,
   colada no bloco ` ```text `, **menos a linha do próprio `estado-lote-<X>.md`** — que é a única
   entrada previsível, porque é o arquivo que você está escrevendo. Não resuma e não reescreva de
   memória: é ela que o orquestrador confronta com a §1 do pacote, e **a divergência entre o que você
   declara e o que a árvore mostra é o sinal mais barato de que o estado não é confiável**.
5. **§6 Decisões fora do pacote** — o que você resolveu que o pacote não previa, e por quê. Sem isto,
   o sucessor refaz a escolha ao contrário.
6. **§7 O que está quebrado agora** — parar no meio costuma deixar o build vermelho. Diga. Omitir
   isto é o pior modo de falha do contrato inteiro, porque manda o sucessor procurar um defeito que é
   só trabalho pela metade.

A §8 fica para o identificador de conversa, quando o executor for externo. Ela é a única opcional: a
conversa some com o executor, e as §1 a §7 são o que sobrevive à troca de modelo, de família e de
máquina.

> **Isto é conferido por comando, não por leitura.** Na volta, o orquestrador roda
> `.specify/scripts/powershell/verificar-lote.ps1`, que confronta o que o pacote pediu, o que você
> declarou na §5 e o que a árvore mostra. Seção em branco aparece no relatório dele — o preenchimento
> deixou de ser uma boa intenção.

---

## 3. Rollback — declare o seu antes de começar

Na **§1 do seu `estado-lote-<X>.md`** — que é a primeira seção do arquivo justamente porque é a
primeira coisa a escrever, **antes de editar qualquer arquivo do lote**. Duas linhas bastam, e o
formato depende de onde você está rodando:

- **Em worktree próprio** (lotes de backend): `git worktree remove --force <caminho>` e apagar a
  branch. É atômico e não toca a árvore principal.
- **Na branch de trabalho** (lotes de frontend): `git checkout -- <os arquivos rastreados da §1>` e
  `rm -f` nos arquivos novos que você criou, **nomeados um a um**. O `-f` não é preguiça: se o lote
  parou na metade, parte dos arquivos previstos não chegou a existir, e um `rm` sem ele aborta o
  rollback no primeiro que faltar — deixando o resto por desfazer.

⚠️ **Nunca escreva um rollback que use `git checkout .`, `git reset --hard` ou `git clean -fd` sem
caminho.** A árvore contém trabalho que não é seu: alterações e arquivos novos anteriores ao lote são
a regra, não a exceção. Um rollback amplo destrói trabalho alheio, e o seu lote passa de "não
entregue" a "causou perda".

O orquestrador executa este rollback **sem depender de você** — é por isso que ele é escrito antes,
e não depois. Um plano de rollback redigido por um executor que acabou de esgotar cota é o artefato
menos confiável do processo; um redigido antes da primeira edição é apenas verdade.
