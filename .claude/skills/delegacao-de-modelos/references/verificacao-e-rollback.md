# Isolamento, aceitação e rollback

## Isolamento — híbrido, por tipo de lote

| Lote | Onde roda | Por quê |
| :--- | :--- | :--- |
| **Backend** | `git worktree` próprio | Maven compartilha o `~/.m2`; o worktree custa segundos mesmo em repositório grande, e o rollback vira atômico |
| **Frontend** | branch de trabalho atual | um worktree novo pagaria `npm ci` inteiro **antes** de o portão `npm test` poder rodar |

⚠️ **Alguns CLIs só enxergam workspace sob uma raiz confiável declarada na própria configuração.** É o
caso do `agy`, cuja lista de `trustedWorkspaces` vive em `~/.gemini/antigravity-cli/settings.json`.
Fora dela o executor não enxerga o workspace e escreve no scratch dir **reportando sucesso** — um
worktree no diretório temporário do sistema falha; um sob o home do usuário funciona, com a árvore
principal intacta.

**O Codex não tem essa restrição**: em `codex exec` não há portão de confiança a atravessar — quem
decide o que ele pode escrever é o `--sandbox`, e só ele. Use a mesma raiz mesmo assim: um lugar só
para worktree de lote é mais fácil de limpar do que dois.

```bash
# 1. worktree sob raiz confiável (serve a qualquer executor)
git worktree add "<RAIZ_DE_WORKTREES>/lote-<X>" -b lote-<X>-<NNN>

# 2a. executor que RODA comando (ex.: Codex) — -C é o workspace, e o portão roda lá dentro
codex exec --json --sandbox workspace-write -C "<RAIZ_DE_WORKTREES>\lote-<X>" \
    -m <slug do roster> -c model_reasoning_effort=<esforço> \
    "<prompt de disparo + delta de quem roda comando>" < /dev/null

# 2b. executor que NÃO roda comando (ex.: agy) — a flag de diretório é obrigatória,
#     e o caminho é absoluto
agy --add-dir "<RAIZ_DE_WORKTREES>\lote-<X>" \
    --model "$(agy models | grep <tier> | head -1 | cut -f1)" \
    --mode accept-edits --output-format json --print-timeout 5m \
    -p "<prompt de disparo, com TODOS os caminhos absolutos>"

# subagente Claude — isolation: "worktree" no tool Agent

# frontend — sem worktree; a rede é a verificação da lista, abaixo
```

⚠️ **`< /dev/null` não é enfeite** num CLI de execução. Sem stdin fechado, o `exec` imprime
`Reading additional input from stdin...` e fica esperando — e num disparo em background isso é
indistinguível de um lote lento.

⚠️ **Nunca a flag de bypass total.** Todo CLI de agente tem uma; o nome muda, o efeito não — ela
derruba a fronteira do *"nenhum delegado comita"*. E nenhuma delas é necessária: o modo que libera
apenas a **árvore de trabalho** (`--mode accept-edits`, `--sandbox workspace-write` e equivalentes) já
dá ao executor tudo que ele precisa.

⚠️ **Diga ao executor que não roda comando para usar as ferramentas de arquivo.** Onde a execução de
shell é auto-negada em headless sem allowlist, o sintoma é `SUCCESS` com resposta vazia. **Não repita
essa instrução para quem roda comando** — ela jogaria fora a única vantagem que ele tem.

---

## Aceitação

### 0. O executor fez alguma coisa?

Antes de qualquer outra verificação, para executor externo — **e o sintoma é diferente em cada
família**:

| Família | "não fez nada" se parece com |
| :--- | :--- |
| que não roda comando | `status: SUCCESS` **com `response` vazio** = permissão auto-negada. **Não é sucesso.** Releia a invocação: quase sempre é a ferramenta de shell sem allowlist |
| que roda comando | o JSONL termina sem nenhum `item` do tipo `file_change`. O `turn.completed` vem normalmente, com `usage` — ele **conversou**, não trabalhou |

- **Confira no sistema de arquivos os arquivos que ele afirma ter criado.** O modo de falha observado
  é o executor responder *"o arquivo foi criado com sucesso"*, com link, tendo escrito no scratch dir.
  `git status --porcelain --untracked-files=all` vazio depois de um lote "entregue" é o sintoma.

> **Um executor que declara o que tocou entrega verificação de graça.** Onde cada edição sai no JSONL
> como um item `file_change` com `path` e `kind`, comparar essa lista com a §1 do pacote **antes** de
> rodar o verificador pega arquivo fora da lista em segundos. Não substitui o `verificar-lote.ps1` — o
> JSONL é o que ele **diz**, e o `git status` é o que **é** —, mas é o filtro mais barato que existe.

⚠️ **Um executor que para e reporta não é um executor que falhou.** Diante de um pacote com omissão
real, recusar-se a criar o arquivo e explicar o que falta é o contrato sendo cumprido, não uma
regressão de capacidade. Trate como pacote a corrigir, nunca como executor a trocar.

Um lote que não tocou nada não é lote reprovado — é lote **não executado**. Não role rollback: corrija
a invocação e redispare.

### 1. A lista de arquivos, primeiro

```powershell
.specify/scripts/powershell/verificar-lote.ps1 `
    -Pacote specs/0NN-<slug>/pacote-de-execucao-lote-<X>.md `
    -Estado specs/0NN-<slug>/estado-lote-<X>.md `
    -Ancora <arquivo da âncora> [-Worktree <caminho>]
```

Ele confronta **três** listas — o que a §1 do pacote pediu, o que a §5 do estado declara, e o que a
árvore mostra — descontando a âncora e o par pacote+estado. Cinco achados:

| Achado | Significa | Consequência |
| :--- | :--- | :--- |
| `FORA DA LISTA` | real ∖ esperado | **reprovado** — rollback, mesmo com portão verde |
| `NÃO DECLARADO` | real ∖ declarado | **reprovado** — o estado não bate com o diff, então não é confiável |
| `DECLARADO MAS AUSENTE` | declarado ∖ real | **reprovado** — sintoma do scratch dir; confira a invocação antes de culpar o lote |
| `NÃO ENTREGUE` | esperado ∖ real | lote **incompleto**, que não é lote falho — ver "Lote incompleto" |
| `CAMPOS VAZIOS` | seção em branco no estado | ressalva: sem elas ninguém assume o lote |

Exit **2** reprova, **1** é erro de leitura, **0** passa. Por baixo é
`git status --porcelain --untracked-files=all`, e você pode rodá-lo à mão.

⚠️ **O `--untracked-files=all` não é opcional.** Sem ele o git colapsa diretório novo numa linha só
(`?? .../parceiro/`), e você compararia um diretório contra uma lista de arquivos — o que esconde
exatamente o que a verificação existe para achar.

⚠️ **Exit 1 não é "nada fora da lista".** O script aborta nomeando arquivo e seção quando não
consegue ler a §1 do pacote ou a §5 do estado. Parse que devolve lista vazia passaria como aprovação
— é a rede que passa com e sem o defeito, e ela é proibida aqui.

**Sobrou arquivo fora da lista → o lote está reprovado.** Execute o rollback e reporte, mesmo que o
portão esteja verde e o código pareça bom. Um lote cuja extensão real não se conhece não pode ser
desfeito com segurança depois.

### 2. O portão, você mesmo

Rode o comando do pacote e **leia a saída**. Relatório afirmando verde não é verde — isso já valia
antes de existir executor externo.

### 3. As convenções

Os `AGENTS.md` aninhados da camada. O que mais escapa a executor delegado, em qualquer família:

- validação **acumulada** num `ArrayList<String>`, nunca fail-fast;
- `ObjectUtil.isNullOrEmpty()` no lugar de `!= null` / `.isEmpty()`;
- ordem fixa dos métodos do service, com `validate` sempre por último;
- `@NamedEntityGraph` pelo **sentido de navegação**, não por entidade;
- no frontend, `signal`/`computed`, `@if`/`@for`, `inject()`, e nada de hex no CSS.

### 4. Rede nova prova-se por mutação

Quebre o alvo, confirme que o teste falha, restaure. É comum uma rede passar com e sem a guarda que
ela existe para proteger — a mutação é o único jeito barato de descobrir isso.

### 5. Só então

Marque o `tasks.md`, atualize a §9 do roteiro, e **comite você**.

---

## Rollback exato

Sempre contra a **âncora** tirada antes do disparo (`git rev-parse HEAD` +
`git status --porcelain --untracked-files=all`, guardada em arquivo).

```bash
# lote em worktree
git worktree remove --force <caminho>
git branch -D <branch-do-lote>

# lote na branch de trabalho — caminhos NOMEADOS, vindos da §1 do pacote
git checkout -- <arquivo rastreado> [<arquivo rastreado> ...]
rm -f <arquivo novo> [<arquivo novo> ...]   # -f: lote parado na metade não criou tudo
```

⛔ **Proibido, sem exceção:** `git checkout .`, `git reset --hard`, `git clean -fd` sem caminho, e
`git stash` da árvore inteira. A árvore contém trabalho que não é do lote: alterações e arquivos
novos anteriores a qualquer delegação são a regra, não a exceção. Um rollback amplo transforma "lote
não entregue" em "perda de trabalho alheio", que é infinitamente pior.

**Confirme o rollback**: `git status --porcelain --untracked-files=all` precisa voltar a bater com a
âncora, com as entradas pré-existentes **intactas**.

---

## Lote incompleto

Não é falha — é o caso que o `estado-lote-<X>.md` existe para cobrir.

**Antes de escolher, veja se dá para simplesmente continuar.** Os CLIs externos costumam devolver um
token de retomada em toda invocação, e retomam **com o contexto preservado**:

```bash
# executor que roda comando — o thread_id vem no primeiro evento do JSONL, "thread.started"
codex exec resume <thread_id> --sandbox workspace-write -C "<workspace>" --json \
    "Continue o lote a partir do seu estado-lote-<X>.md, secao 3 'Em curso'." < /dev/null

# executor que não roda comando — o conversation_id vem no corpo da resposta
agy --conversation <id> --add-dir "<workspace>" --mode accept-edits --output-format json \
    -p "Continue o lote a partir do seu estado-lote-<X>.md, secao 3 'Em curso'."
```

Retomar é mais barato e mais fiel que qualquer handoff, e só não serve quando a interrupção foi cota
(a conversa volta, o limite não) ou quando o que falta virou julgamento.

O token vai na §8 do `estado-lote-<X>.md`, **rotulado com o executor que o emitiu** — os nomes diferem
entre famílias, e um id sem dono não retoma nada. **O checkpoint continua obrigatório**: a conversa é
do executor e some com ele; o estado é do lote e sobrevive à troca de modelo, de família e de máquina.

Não dando para retomar a conversa, leia o checkpoint e escolha:

| Decida | Quando |
| :--- | :--- |
| **Retomar** com outro executor | o campo "em curso" é específico o bastante para um estranho continuar, o que falta é do mesmo tier, e o "está quebrado" não passa de trabalho pela metade |
| **Assumir** você | o que falta virou julgamento, ou o checkpoint não passa no teste do estranho, ou houve decisão fora do pacote que precisa de revisão antes de qualquer continuação |
| **Desfazer e refazer** | a lista de arquivos foi violada, ou o checkpoint não bate com o diff real — aí o estado não é confiável, e continuar em cima dele propaga o erro |

**`NÃO DECLARADO` não vazio é a definição operacional da terceira linha.** É literalmente "o
checkpoint não bate com o diff real", medido em vez de julgado: desfaça e refaça, sem discutir se o
código parece bom.

Ao retomar, o pacote **não muda**: o novo executor recebe o mesmo arquivo, e continua o
`estado-lote-<X>.md` de onde o anterior parou. Reescrever o pacote no meio do lote é como se perde a
rastreabilidade do que foi pedido.
