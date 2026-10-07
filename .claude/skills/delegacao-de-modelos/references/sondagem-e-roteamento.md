# Sondagem e roteamento

> **Tudo neste arquivo foi medido, e tudo envelhece.** Os CLIs mudam de flag, de roster e de
> capacidade entre versões. Trate as tabelas como o estado da última medição, não como fato
> permanente: **sonde antes de rotear**, sempre.

## Os executores externos não são equivalentes

| CLI | Família | Roda o portão? |
| :--- | :--- | :--- |
| `codex exec` | GPT | **sim** — sandbox próprio, aprovação `never`, nada a configurar |
| `agy` (Antigravity) | Gemini | **não** — `run_command` auto-negado em headless sem allowlist |

**A diferença acima é a que mais pesa no roteamento**, e não é preferência de qualidade: um lote
cujo defeito só aparece em execução — HQL, grafo de entidade, migração — entregue a quem não
consegue executá-lo volta com o defeito intacto e o relatório verde.

O `gemini` CLI avulso está **fora**: ele não autentica com conta pessoal (`IneligibleTierError`), e o
próprio erro manda migrar para o Antigravity.

Descubra os binários, nunca crave o caminho:

```bash
command -v codex
command -v agy || ls "$LOCALAPPDATA/agy/bin/agy.exe"
```

---

## Descoberta do roster — em runtime, sem id versionado

### Codex

**Não existe `codex models`.** A fonte é o cache que o próprio CLI mantém:

```bash
python -c "import json,io,os; \
  [print(m['slug'], [e['effort'] for e in m['supported_reasoning_levels']]) \
   for m in json.load(io.open(os.path.expanduser('~/.codex/models_cache.json'),encoding='utf-8'))['models']]"
```

⚠️ **É cache, não API** — ele traz `fetched_at` e `client_version`. Cache velho envelhece junto com
qualquer id que você copie dele.

⚠️ **No Codex o esforço NÃO faz parte do slug.** Ele é dimensão separada, passada na invocação como
`-c model_reasoning_effort=<low|medium|high|xhigh|max|ultra>` — e o roster diz quais níveis cada
modelo aceita, que não são os mesmos para todos. Isso é o oposto do `agy`, onde o sufixo
`-high/-medium/-low` é parte do id. Errar essa diferença é sondar um modelo e disparar outro.

✅ **Slug inválido falha ALTO no Codex**: `400 invalid_request_error`, dizendo o nome recusado. É a
diferença mais confortável entre as duas famílias — no `agy` o id errado falha em silêncio.

### Antigravity

```bash
agy models
```

Devolve `id<TAB>rótulo`, e **é a única fonte de id de modelo**. O sufixo `-high/-medium/-low` é o
esforço de raciocínio e faz parte do id — é uma dimensão de roteamento, não decoração.

⚠️ Modelo Claude que apareça no roster do `agy` **não** substitui o subagente in-process: lá dentro
ele lê o `AGENTS.md` e paga contexto de fora; aqui herda o `CLAUDE.md` mínimo. Para família Claude,
use o tool `Agent`.

## Sonda de disponibilidade

### Codex

```bash
codex exec --json --sandbox read-only -m <slug> -c model_reasoning_effort=low \
    "Responda apenas: OK" < /dev/null
```

Devolve JSONL, um evento por linha:

| Evento | Traz |
| :--- | :--- |
| `thread.started` | `thread_id` — **o token de retomada**, e ele só aparece aqui |
| `item.completed` com `type: agent_message` | a resposta |
| `item.completed` com `type: command_execution` | cada comando rodado, com `exit_code` |
| `item.completed` com `type: file_change` | **cada arquivo tocado, com `path` e `kind`** |
| `turn.completed` | `usage` da chamada |

| Resultado | Leitura |
| :--- | :--- |
| exit 0 **com `agent_message` não vazio** | disponível |
| `ERROR` com `status: 400` | slug inválido ou não liberado para a conta — **acionável**, corrija o id |
| exit ≠ 0 | indisponível |
| trava sem imprimir nada | **stdin aberto** — faltou `< /dev/null` |

> **`--sandbox read-only` na sonda, `workspace-write` no lote.** A sonda não precisa escrever, e o
> menor privilégio que responde à pergunta é o certo.

### Antigravity

```bash
agy --model <id> --mode plan --output-format json --print-timeout 2m -p "Responda apenas: OK"
```

Devolve `{conversation_id, status, response, duration_seconds, num_turns, usage{...}}`.

| Resultado | Leitura |
| :--- | :--- |
| `status: SUCCESS` **com `response` não vazio** | disponível |
| `status: SUCCESS` **com `response` vazio** | ⛔ **não é sucesso** — ver "SUCCESS vazio" abaixo |
| Erro de autenticação | indisponível, e **avise**: é acionável, diferente de cota |
| `429` / `RESOURCE_EXHAUSTED` | indisponível agora; roteie para o outro candidato |

**O `usage` é consumo real, não saldo.** Ele traz `input_tokens`, `output_tokens`, `thinking_tokens`,
`cache_read_tokens` e `total_tokens` **da chamada**. Não existe leitura de cota disponível em nenhuma
das famílias — o que existe é medição do que cada lote custou, insumo para a **próxima** decisão de
roteamento, não para esta.

> **Um executor externo tem piso de contexto menor que um subagente Claude**, porque não recebe o
> `CLAUDE.md`. Meça o seu em lotes reais; o piso é o `input_tokens` de uma chamada trivial.

---

## As armadilhas do Codex

**As do `agy`, logo abaixo, NÃO se aplicam a ele** — e saber disso vale tanto quanto a lista própria.
Ele não tem scratch dir, não precisa de flag de diretório, não tem parede de execução de comando, e
não inventou entrega nos lotes medidos.

### 1. Sem `< /dev/null`, ele espera para sempre

O `codex exec` lê stdin quando ele está aberto (para anexar um bloco `<stdin>` ao prompt). Num
disparo em background, isso é indistinguível de um lote lento. **Feche stdin sempre.**

### 2. `exec` não aceita `-a/--ask-for-approval`

A flag existe no modo interativo, não no `exec` — que já roda com `approval: never`. Passá-la aborta
a invocação com `unexpected argument '-a' found`, antes de qualquer trabalho.

### 3. O `--sandbox` padrão não escreve

Sem `--sandbox workspace-write` ele não edita arquivo nenhum. E o oposto também importa: `--sandbox`
é a **única** coisa que decide o que ele pode tocar. Não há portão de confiança de diretório a
atravessar — um worktree novo, ausente da tabela `[projects]` do `~/.codex/config.toml`, é escrito
normalmente.

### 4. ⚠️ No Windows, ele executa via `powershell.exe` — e `./mvnw` é script `sh`

**O portão que você escreve no pacote precisa ser o comando que ELE consegue rodar.** Medido: o
`./mvnw` do pacote falha na invocação (`icm : Não é possível indexar em uma matriz nula`), e o
executor reporta a falha honestamente sem tocar em arquivo fora da lista para contorná-la — com o
código dele **compilando limpo**. O portão é que não rodava. No Windows, escreva `cmd /c mvnw.cmd
...`, como `backend/AGENTS.md` manda. Portão que o executor não consegue rodar é portão que não
rodou, e o lote volta sem a rede que você achou que tinha posto.

---

## As quatro armadilhas do `agy`

### 1. `SUCCESS` vazio significa permissão negada

Em headless, uma ferramenta que precise da permissão `command` é **auto-negada** — não há como
perguntar. O resultado é `status: SUCCESS` com `response: ""` e uma linha `jetski: no output
produced` no stderr. **Trate `response` vazio como falha**, sempre.

### 2. `run_command` tem parede; ferramenta de arquivo não

`view_file`, `write_to_file`, `replace_file_content`, `sed_file`, `grep_search`, `list_dir` e
`find_by_name` rodam sem pedir nada. Só `run_command` bate na allowlist de
`~/.gemini/antigravity-cli/settings.json` — e o casamento é por segmento: `command(Get-ChildItem)`
autoriza o comando simples e **não** autoriza `Get-ChildItem ... | Select-Object`.

**Consequência de desenho, e ela é boa:** o executor externo **edita**; o **portão é seu**. Isso já
era a regra ("rode o portão você mesmo"), e agora é também o caminho de menor configuração — zero
alteração no `settings.json` do usuário. Se um dia quiser que ele itere sozinho sobre o próprio
portão, aí sim allowliste os comandos do portão — mas é escolha do usuário, não sua.

### 3. cwd **não** é o workspace

Rodar o `agy` de dentro de um diretório não faz dele o workspace. Sem `--add-dir`, o agente escreve
no **scratch dir dele** (`~/.gemini/antigravity-cli/scratch`) e **reporta sucesso**. Sempre:

- `--add-dir "<caminho absoluto>"`;
- caminhos **absolutos** dentro do prompt, nunca relativos;
- o caminho precisa estar sob `trustedWorkspaces` do `settings.json`.

### 4. Ele afirma ter criado arquivo que não criou

Medido: resposta dizendo *"o arquivo foi criado com sucesso"*, com link clicável, enquanto o arquivo
estava no scratch dir e **não** no workspace. **Nunca aceite a alegação — confira no sistema de
arquivos.** É a mesma disciplina de "não presuma o verde do portão", um nível abaixo.

---

## O que a calibração ensinou

O método que produziu isto, e que vale repetir a cada mudança de roster: **reimplementação cega
contra gabarito já commitado**. Escolha arquivos que já existem no repositório, monte o pacote como
se eles não existissem, e compare a entrega com o commit. É o único jeito de medir um executor sem
depender de opinião.

O padrão que atravessa todos os executores externos medidos:

> **Eles acertam o que o pacote *diz* e erram o que o pacote *omite*** — e preenchem a omissão
> **adivinhando em silêncio**, em vez de parar e reportar como o contrato do executor manda. Uma
> entidade não listada na §3 do pacote vira um campo inventado que compila; um passo de wizard não
> descrito vira um passo a mais. **Nenhum desses erros falha em compilação.**

Consequências para o roteamento:

1. **A qualidade do pacote é o teto da entrega.** Um arquivo de referência a menos vira um defeito,
   não uma pergunta. Liste na §3 do pacote **toda** entidade que o HQL ou o texto atravessa.
2. **Portão que só compila não pega o defeito deles.** Tier tabelado exige portão de **paridade** —
   diff contra um gabarito ou uma asserção que execute a consulta.
3. **Auditoria delegada não substitui a sua.** Um falso negativo em relatório de conferência é
   indistinguível de um arquivo correto — confira os que ele absolveu, não só os que ele acusou.
4. **Um executor que para diante de um pacote defeituoso está cumprindo o contrato**, não falhando.
   A recusa dele é revisão do seu pacote. Entrega menos por disparo, e vale mais.

---

## Tiers

| Tier | Serve para | Quem preenche |
| :--- | :--- | :--- |
| **Orquestrador** | onde a decisão propaga: mecanismo, sigilo, máquina de estados, escopo. **Nunca delegado** | quem conduz a sessão |
| **Executor-de-padrão** | lote com espelho literal no código **e** portão que prova o erro | subagente Claude Sonnet · executor externo que **roda o portão** |
| **Executor-tabelado** | lote mecânico — changelog, enum, DTO — com portão de paridade | qualquer executor externo, esforço conforme o lote |

**Confirme o roster antes de usar** — ele muda, e id cravado falha em silêncio no `agy` (no Codex,
falha alto). É por isso que este arquivo nomeia tiers e capacidades, nunca ids.

Modelos pequenos (a classe Haiku e equivalentes) **não entram em nenhum tier aqui**. Não há trabalho
puramente mecânico neste projeto: as convenções são muitas e o modo de falha é **silencioso**, que é
o pior tipo para um modelo pequeno.

## Desempate, nesta ordem

1. **O defeito deste lote só aparece em EXECUÇÃO?** HQL, grafo de entidade, migração, ordenação —
   qualquer coisa que compile igual estando certa ou errada → **executor que roda o portão**. Quem
   não executa está fora daqui, e não por qualidade: ele não consegue rodar o que provaria o erro.
2. **O lote toca convenção que já produziu defeito silencioso catalogado?** (o
   `00-briefing-para-agentes.md` do plano da onda) → **subagente Claude**, que carrega o contexto
   mínimo sem novo briefing.
3. **Trabalho tabelado, com portão de paridade que você roda?** → o executor externo mais barato
   serve, e é onde ele continua sendo a escolha certa.
4. **Frontend puro?** → qualquer executor externo; todos preservam cota Claude.
5. **Candidato indisponível** → o próximo desta mesma ordem, nunca "o outro" — são três famílias, e
   pular direto para a terceira porque a primeira caiu ignora a segunda. **Nenhum disponível** →
   faça inline e diga que fez.

## A assimetria

| | Subagente Claude | Executor que roda o portão | Executor que não roda |
| :--- | :--- | :--- | :--- |
| Contexto de projeto | `CLAUDE.md` mínimo + regra por caminho | `AGENTS.md` raiz + aninhado | `GEMINI.md` raiz + camada |
| Permissões | herda a sessão | `--sandbox workspace-write`; nada a configurar | `--mode accept-edits`; execução na allowlist |
| **Rodar o portão** | **sim** | **sim** | **não**, sem allowlist — o portão é seu |
| Isolamento | `isolation: "worktree"` | `-C <worktree>`; sem portão de confiança | flag de diretório; **precisa** de `trustedWorkspaces` |
| Declara o que tocou | — | **sim**, eventos `file_change` no JSONL | não |
| Relatório | volta direto | JSONL no stdout, com `usage` | JSON no stdout, com `usage` |
| Retomar | nova invocação, contexto frio | `codex exec resume <thread_id>` | `--conversation <id>` |
| Id inválido | — | falha **alta** (400) | falha em **silêncio** |

O `usage` é cumulativo por turno, portanto a **quantidade de chamadas** importa tanto quanto o
tamanho de cada uma. Recalibre com lotes reais; benchmark sintético mede a ferramenta, não o
trabalho.

**`AGENTS.md` é load-bearing**. O executor externo lê a raiz e a instrução da camada, sem encadear
`CODEX.md` automaticamente. Divergiu delas, avise e pare.
