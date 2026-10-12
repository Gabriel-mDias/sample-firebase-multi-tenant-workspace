# Roteiro da Rodada — 006 Formatter Java do GEMS AI SDK 3.1.0

**Feature**: `006-formatter-gems-ai` · **Branch**: `feature/006-formatter-gems-ai` (sample) e uma
branch própria no Java-GEMS-SDK · **Requisito**: pedido do product owner em 2026-10-12 ("aplique esse
formatador no projeto backend, para deixá-lo no formato que eu desejo de escrita de código")
**Data**: 2026-10-12 · **Status**: fase 3 executada localmente; PRs aguardando o ok do product owner

**Índice**: [1 Objetivo](#1-objetivo-da-rodada) · [2 Decisões de entrada](#2-decisões-de-entrada) ·
[4 Backend e build](#4-backend-e-build) · [6 Testes](#6-testes-esperados-por-bloco) ·
[7 Isolamento](#7-interação-e-isolamento) · [8 Tasks](#8-divisão-de-tasks-e-delegação-por-modelo) ·
[9 SDD](#9-prompts-sdd-da-rodada) · [10 Riscos](#10-riscos-e-mitigações) · [11 Fora de escopo](#11-fora-de-escopo) ·
[12 Aprovação](#12-critérios-de-aprovação)

Sem §3 (banco) e §5 (frontend): a rodada não toca em nenhum dos dois.

---

## 1. Objetivo da rodada

Adotar o GEMS AI SDK 3.1.0 e o formatter Java dele como a **única fonte de verdade do formato Java**
no backend do sample e no Java-GEMS-SDK. Os fontes passam a seguir o formato do product owner, com
gate no CI e formatação ao salvar nas IDEs.

### O que foi verificado em 2026-10-12 (o código vence)

| Fato | Fonte |
| :--- | :--- |
| A 3.1.0 está publicada: release `v3.1.0` (21:34Z), com wheel, sdist e `SHA256SUMS`, no commit `909c69aa` (PR #14) | `gh release view v3.1.0 -R Gabriel-mDias/gems-ai-sdk` |
| Formatter: motor Eclipse JDT 3.46.0 com adapter GEMS, embutido no wheel. Exige Python 3.11+ e JDK 21+. Comandos `check-java` e `format java` com `--changed`, `--all` ou lista de arquivos | `docs/java-formatter.md` na tag `v3.1.0` |
| `check` e `apply` da 3.1.0 recusam manifesto 3.0.0; a migração é `gems-ai update --to 3.1.0` (com `--dry-run`), com rollback exato | idem |
| **CI quebrado:** o job `gems-ai` usava `gems-ai-sdk@main`. Como `@main` passou a instalar a 3.1.0 e o `gems-ai.json` declara 3.0.0, a action falha. Corrigido à parte pelo hotfix [PR #11](https://github.com/Gabriel-mDias/sample-firebase-multi-tenant-workspace/pull/11), que fixa o commit `baba0f5e` (CLI 3.0.0) | `action.yml` da `main` do SDK; `.github/workflows/ci.yml:74` |
| **Conflito:** o backend roda Spotless (`config/spotless/eclipse-formatter.xml`) na fase `validate` e Checkstyle com `failOnViolation` | `backend/pom.xml:39-40`, `:146-223` |
| O backend tem 78 arquivos `.java` versionados | `git ls-files backend` |
| O Java-GEMS-SDK tem 220 arquivos `.java`, Spotless 3.10.2 e Checkstyle 13.9.0, e **não tem `gems-ai.json`** | `Java-GEMS-SDK/pom.xml:32-34`, `:161-183` |

---

## 2. Decisões de entrada

Decisões tomadas com o product owner em 2026-10-12.

| # | Ponto | Decisão | O que isso fecha |
| :-- | :--- | :--- | :--- |
| 1 | CI quebrado | **Hotfix imediato** (PR #11: action fixada no commit da 3.0.0) e migração para a 3.1.0 nesta rodada | Os PRs da 004 e da 005 não esperam a 006 |
| 2 | Fonte de verdade do formato Java | **O formatter do gems-ai substitui o Spotless.** As regras do Checkstyle que só tratam de layout e conflitarem são desligadas; as de qualidade continuam | Uma fonte de verdade só, sem oscilação entre formatters |
| 3 | Escopo da reformatação | **Em massa, num commit isolado só de estilo**, registrado em `.git-blame-ignore-revs`. O CI passa a exigir `check-java --all` | O código inteiro fica no formato desejado; o blame continua legível |
| 4 | Java-GEMS-SDK | **Entra na rodada**, com **manifesto Java completo** (perfil java): regras gerenciadas, blocos de agente, snapshot e gate no CI. *Correção da fase 2:* a 3.1.0 não tem `gems-ai init`; a adoção é escrever o `gems-ai.json` e rodar `plan`, `apply --dry-run` e `apply` (research R6) | O SDK segue o mesmo formato que o sample |
| 5 | Ordem com a 004 | **A 006 vem primeiro no SDK.** A 004 é implementada sobre o código já formatado, e a 3.5.0 sai no formato novo | A 004 pode ter o portão aprovado agora, mas a fase 3 dela espera a 006 no SDK |
| 6 | Spotless e Checkstyle **do SDK** | **Manter os dois** (proposta da fase 2, aceita no portão). No SDK, o Spotless não formata nada, só aplica `trimTrailingWhitespace` e `endWithNewline`. O Checkstyle só tem `FileTabCharacter`, `NewlineAtEndOfFile` e `RegexpSinglelineJava`. Com o código formatado, os dois passaram nos 18 módulos (research R6). A decisão 2 foi tomada sobre o Spotless do sample, que é um formatter completo | Nada oscila e nenhuma checagem de higiene se perde. Se você preferir tirar também o do SDK, vira uma tarefa a mais em T012 |
| 7 | `commit` do `gems-ai.json` do sample | **`909c69aa…`**, o commit da tag `v3.1.0` (proposta da fase 2, aceita no portão). O `update` mantém o `51b94e3a` da 3.0.0, e o wrapper local compara esse campo com a fonte (research R2) | O pin fica coerente com a versão |
| 8 | Inicializador, clean-demo e testes Python | **Entram na 006** (2026-10-12, depois do portão, resposta à pergunta de escopo: "Tudo na 006"). O `scripts/sample.py` passa a formatar com o gems-ai em vez do `mvnw spotless:apply`; o `scripts/demo-manifest.json` é regenerado sobre o código formatado; os testes Python ficam verdes (research R11) | O PR da 006 não deixa o inicializador quebrado. Sai da [005](../005-endurecimento-frontend-docs/roteiro.md) (decisão 4 de lá) |

### Decisões tomadas por convenção, sem pergunta

| # | Ponto | Decisão | Fonte da convenção |
| :-- | :--- | :--- | :--- |
| C1 | Caminho da migração | `gems-ai update --project . --to 3.1.0 --dry-run`, revisão do plano e só então a aplicação. Nada em `.gems-ai/` é editado à mão | `AGENTS.md` e `CLAUDE.md` do sample; release notes 3.1.0 |
| C2 | Ref da action | `Gabriel-mDias/gems-ai-sdk@v3.1.0` depois da migração, nunca `@main` | Release notes 3.1.0 ("Prefira `@v3.1.0`") e o incidente do item 1 |
| C3 | Commit de estilo | Só formatação, sem nenhuma mudança semântica, e com a prova do gate completo verde antes e depois (mesma contagem de testes) | Constituição VII |
| C4 | Frontend | Fora: Prettier continua sendo o formatter do TypeScript | O formatter da 3.1.0 é só para Java |
| C5 | Angular-GEMS-SDK | Fora: não tem Java | idem |
| C6 | Imports | O motor não reordena imports; a ordem atual é preservada | `docs/java-formatter.md` ("Imports permanecem na ordem/grupos existentes") |
| C7 | IDE | `gems-ai ide artifacts` lista a extensão do VS Code e o plugin do IntelliJ; a instalação na máquina é do usuário | Release notes 3.1.0 |

---

## 4. Backend e build

Nenhum código Java muda de comportamento. O que muda é o build e a ferramenta. Os números vêm do ensaio
de 2026-10-12 ([research](research.md)).

### 4.1 Java-GEMS-SDK (branch própria a partir de `origin/main` `e830533`)

| Ordem | Commit | Conteúdo | Prova |
| :-- | :--- | :--- | :--- |
| 1 | `chore(gems-ai): adota GEMS AI 3.1.0 com perfil java` | `gems-ai.json` (3.1.0, `909c69aa…`, `java`), saída do `apply`: 24 regras, catálogo, 4 schemas, 5 arquivos de skill, snapshot do formatter, adapters `AGENTS.md`/`GEMINI.md`/`.cursor`/`copilot-instructions` e bloco no `CLAUDE.md`, lock. `.gitattributes` com CRLF para o snapshot (R3) | `check --strict` exit 0, inclusive em clone limpo |
| 2 | `style: formata o Java com o formatter do GEMS AI 3.1.0` | **só `.java`**: 208 de 220 arquivos, +6.598/−4.860 | `.class` idênticos com `-g:none` (R7); `mvn clean verify` verde com a mesma contagem |
| 3 | `chore: ignora o commit de estilo no blame` | `.git-blame-ignore-revs` | `git blame` aponta o commit anterior |
| 4 | `ci: gate GEMS AI e formato Java` | job `gems-ai` em `windows-latest` no `pr-validation.yml`: JDK 21, action `@v3.1.0`, `check --strict`, `check-java --all` | PR com o job verde; com a mutação, vermelho |

O Spotless e o Checkstyle do SDK ficam (decisão 6). Sem release: não há mudança de API nem de
comportamento, e a 3.5.0 da 004 já sai formatada.

### 4.2 Sample (`feature/006-formatter-gems-ai`)

| Ordem | Commit | Conteúdo | Prova |
| :-- | :--- | :--- | :--- |
| 1 | `chore(gems-ai): migra para o GEMS AI 3.1.0 e troca o Spotless pelo formatter` | saída do `update --to 3.1.0`: os 17 destinos de R1. Também entram: `commit` → `909c69aa…` (decisão 7), `.gems-ai/java/** text eol=crlf` no `.gitattributes` (R3), a parte local dos 5 arquivos de agente e o `README.md:103` com 3.1.0 em paridade, a saída do Spotless do `backend/pom.xml` com `config/spotless/`, o `check-java --all` no `scripts/gems-ai-gate.ps1` e o `ci.yml` com `@v3.1.0`, JDK 21 no job `gems-ai` e o comentário do `fetch-depth` corrigido | `check --strict` e `check-java` em clone limpo; contexto de agentes aprovado |
| 2 | `style: formata o Java com o formatter do GEMS AI 3.1.0` | **só `.java`**: 77 de 78 arquivos, +998/−1.173 | `.class` idênticos: **no ensaio, 130 de 130** (R7); `mvnw clean install` com 290 testes |
| 3 | `chore: ignora o commit de estilo no blame` | `.git-blame-ignore-revs` | `git blame` aponta o commit anterior |
| 4 | `fix(scripts): regenera o manifesto do clean-demo sobre o código formatado` | `scripts/demo-manifest.json` (hashes, textos antes/depois das 28 transformações, metadados), o pin `MANIFEST_SHA256` do `sample.py` e o ajuste dos testes Python (decisão 8, R11) | `unittest` verde; `clean-demo --dry-run` aceito no checkout real; `clean-demo` e `initialize` aplicados em clone descartável, seguidos de `check-java --all` e `mvnw clean install` |

A troca do formatter no `sample.py` e no `inicializar-projeto.ps1` entra no commit 1, junto com a saída
do Spotless. O manifesto só pode ser regenerado depois do commit 2, porque guarda o texto dos `.java`.

O Checkstyle do sample fica inteiro: `ParenPad`, `AvoidStarImport` e `MethodName` deram 0 violações
no código formatado, então nenhuma regra de layout conflita (decisão 2).

### 4.3 Onde cada regra vive

| Regra | Onde |
| :--- | :--- |
| Formato Java | `.gems-ai/java/profile.properties` + engine, só pela CLI. O perfil é do gems-ai-sdk, não deste repositório |
| Verificação local | `scripts/gems-ai-gate.ps1` (sample), executado pelo hook Stop |
| Verificação de PR | job `gems-ai` (sample: `ci.yml`; SDK: `pr-validation.yml`) |
| Higiene e qualidade | Checkstyle (os dois repositórios); Spotless de higiene só no SDK |

---

## 6. Testes esperados por bloco

A rodada não tem lógica nova, então não há teste unitário novo. Cada linha diz o que prova.

| # | Prova | O que ela prova | Novo/existente |
| :-- | :--- | :--- | :--- |
| 1 | `.class` com SHA-256 idêntico, compilados com `-g:none` antes e depois do commit de estilo, nos dois repositórios | **A reformatação não muda comportamento**, por qualquer caminho, inclusive os sem teste | novo (R7) |
| 2 | Build completo verde com a mesma contagem: sample com 290 testes e Firebase real; SDK com o total medido em T002 | O build continua íntegro sem o Spotless e com o código formatado | existente |
| 3 | `check --strict` e `check-java --all` num clone com `core.autocrlf=false` | O snapshot e o formato sobrevivem ao checkout que o CI faz (o defeito que o ensaio achou) | novo |
| 4 | Mutação: um espaço a mais num `.java` faz o `check-java` e o gate local falharem, e apontarem o arquivo | O gate de formato pega o que deveria pegar | novo |
| 5 | Mutação na CI: o mesmo arquivo num PR deixa o job `gems-ai` vermelho | A CI aplica o gate, e não só instala a CLI | novo |
| 6 | `git blame` com `.git-blame-ignore-revs` | A autoria sobrevive à reformatação | novo |
| 7 | `scripts/verificar-contexto-agentes.ps1` | O bloco gerenciado novo cabe no orçamento e mantém a paridade | existente |
| 8 | Frontend: `npm ci`, `npm test`, `npm run build` e `prettier --check` | Nada vazou para o frontend | existente |

Do §C do checklist de fechamento, o que se aplica é o gate completo de backend e frontend (linhas 2 e
8). Isolamento por tenant, autorização e mudanças assíncronas de estado não mudam: a linha 1 prova que
o comportamento é o mesmo.

---

## 7. Interação e isolamento

| Recurso | Classificação | O que impede regressão |
| :--- | :--- | :--- |
| 298 arquivos `.java` (78 + 220) | alterado compartilhado | Prova 1 (bytecode) e prova 2 (build) |
| `backend/pom.xml` (sem Spotless) | alterado compartilhado | Prova 2 e Checkstyle mantido |
| Job `gems-ai` e gate local | alterado compartilhado | Provas 3, 4 e 5 |
| `.gitattributes` | alterado compartilhado | Prova 3 |
| Arquivos de agente | alterado compartilhado | Prova 7. O bloco gerenciado é da CLI |
| Frontend e Angular-GEMS-SDK | reuso sem alteração | Prova 8 |

**Branches abertas:** a 004 e a 005 só têm documentação. Não há conflito de rebase, e o Java da 004
nasce formatado (research R10).

### O que fica frouxo depois desta feature

- O **layout passa a depender de uma ferramenta externa ao Maven**. O `mvnw clean install` sozinho deixa
  de reprovar Java fora do formato. Quem reprova é o gate GEMS, localmente pelo hook Stop e no CI pelo
  job `gems-ai`. Um build feito só com Maven, como o `backend/Dockerfile`, aceita código desformatado.
  Isso é aceitável, porque o merge exige o CI.
- **O perfil de formato vive no gems-ai-sdk.** Uma versão nova pode reformatar o código de novo. A
  action fixada em `@v3.1.0` faz isso acontecer só numa atualização deliberada, que é uma rodada.
- **Linhas longas:** o formatter mantém numa linha só construções como a lista de constantes de
  enum. O máximo medido é 1.213 caracteres, e 374 linhas passam de 120 no backend (R8).
- **O job `gems-ai` continua preso ao `windows-latest`**, porque o lock ainda grava `\` (R4).

---

## 8. Divisão de tasks e delegação por modelo

**32 tarefas** em 6 fases ([tasks.md](tasks.md)). Distribuição: US1 9 (T004–T012), US2 14 (T013–T024, T031, T032),
US3 1 (T025) e infraestrutura, review e fechamento 8.

| Fase | Escopo | Tarefas | Executor | Gate de saída |
| :-- | :--- | :--- | :--- | :--- |
| 1 | Setup, CLI, linha de base do SDK | T001–T003 | Coordenador | CLI 3.1.0 com hash conferido; contagem do SDK registrada |
| 2–3 | SDK: adoção, estilo, blame, CI | T004–T012 | Coordenador (CLI) · Sol low (`pr-validation.yml`) | S1, S2, S4, S5 |
| 4 | Sample: migração, Spotless, gate, estilo, blame | T013–T024 | Coordenador (CLI) · Sol low (T016–T019) | A1, A2, A4, A5 |
| 5 | Mutação e autoria | T025 | Coordenador | S6, A6 |
| 6 | Review, gates, PRs, evidência | T026–T030 | Sol read-only (review) · coordenador (gates) · **usuário** (ok de PR e merges) | CI verde nos dois repositórios |

### Critério de delegação

Custo mínimo sujeito aos gates. A maior parte é execução determinística da CLI com plano já aprovado
(§4), e o coordenador faz isso inline, porque o onboarding de um worker custaria mais que o comando. As
edições de YAML, de pom e do gate vão para o Sol low, com gate de saída. Review independente é
obrigatório, porque a rodada mexe no gate de CI. Os diffs de estilo são grandes demais para revisão
humana linha a linha e têm prova mecânica (§6, linha 1). Nenhum uso de Opus como executor.

### Ordem e paralelismo

```
T001 → SDK (T002→T012) → PR SDK → merge (usuário) ──→ libera a fase 3 da 004
T001 → sample (T013→T024) → T025 → review → PR sample → merge (usuário)
```

O sample pode correr depois de T012, sem esperar o merge do SDK. T016 e T017 são o único paralelismo.

---

## 9. Prompts SDD da rodada

| Ordem | Comando | Situação | Saída |
| :-- | :--- | :--- | :--- |
| 1 | `/speckit-specify` | ✅ 2026-10-12 | [spec.md](spec.md), [checklist](checklists/requirements.md) 100% |
| 2 | `/speckit-clarify` | ✅ dispensado | As decisões 1–5 foram fechadas com o product owner na fase 1. As duas que a fase 2 abriu (6 e 7) estão na §2 para o portão, e a spec saiu sem `[NEEDS CLARIFICATION]` |
| 3 | `/speckit-plan` | ✅ 2026-10-12 | [plan.md](plan.md), [research.md](research.md) (R1–R10, com ensaio medido), [quickstart.md](quickstart.md). Sem `data-model.md` (não há dado) e sem `contracts/` (a API não muda). Hook `agent-context` não executado: o `CLAUDE.md` não tem os marcadores SpecKit, e há job de paridade de contexto no CI |
| 4 | `/speckit-tasks` | ✅ 2026-10-12 | [tasks.md](tasks.md), 30 tarefas |
| 5 | `/speckit-analyze` | ✅ 2026-10-12 | 0 CRITICAL, 0 HIGH, 3 MEDIUM e 2 LOW, todos incorporados |
| 6 | **`roteiro.md`** | ✅ | este documento |
| 7 | `/speckit-implement` | 🔒 | após aprovação |

### Achados do `/speckit-analyze` já incorporados

| Achado | Sev. | Decisão |
| :--- | :--- | :--- |
| I1: a decisão 4 cita `gems-ai init`, que não existe na 3.1.0 | MEDIUM | Decisão 4 corrigida; fluxo em R6 e T004 |
| I2: a decisão 2 ("substitui o Spotless") não diz se vale para o SDK | MEDIUM | Decisão 6 proposta para o portão |
| U1: a prova de bytecode não dizia como compilar sem depuração; `debug=false` não basta | MEDIUM | Parâmetros exatos em R7, T008 e T021; ensaio com 0 diferenças |
| C1: a contagem de testes do SDK não está medida | LOW | Medida em T002, antes de qualquer mudança |
| A1: o `apply`/`update` exige aprovação do plano completo (README 3.1.0) | LOW | Os planos medidos estão na §4 e em R1/R6. O portão os cobre; se divergirem, T004/T013 param |

---

## 10. Riscos e mitigações

| # | Risco | Gravidade | Mitigação |
| :-- | :--- | :--- | :--- |
| 1 | A reformatação muda comportamento sem ninguém perceber | alta | Prova 1 (bytecode idêntico) antes do build; commit de estilo isolado |
| 2 | Snapshot com drift no CI por fim de linha | alta | Achado no ensaio; `.gitattributes` (R3) e prova 3 |
| 3 | O `apply` ou o `update` da fase 3 planeja algo diferente do que foi aprovado | média | T004/T013 comparam com R1/R6 e param se divergir |
| 4 | Gate local mais lento ou sem JDK | baixa | `check-java --all` leva cerca de 6 s; JDK 26 da máquina passou no ensaio |
| 5 | Futuro `@main` ou versão nova reformata tudo | baixa | Action fixada em `@v3.1.0` (C2) |
| 6 | O build do SDK no CI do PR quebra por causa do job novo | baixa | Job separado; o `mvn verify` existente não muda |

---

## 11. Fora de escopo

| Item | Por quê | Volta em |
| :--- | :--- | :--- |
| Mudar o perfil de formato, por exemplo para quebrar enums longos | O perfil é do gems-ai-sdk (R8) | rodada no gems-ai-sdk, se o product owner pedir |
| Frontend e Angular-GEMS-SDK | O formatter é só para Java (C4, C5) | não volta |
| Release nova do Java-GEMS-SDK só para a formatação | Sem mudança de API nem de comportamento | a 3.5.0 da [004](../004-reativacao-acao-padrao/roteiro.md) já sai formatada |
| Job `gems-ai` em Ubuntu | O lock ainda grava `\` (R4) | quando a CLI normalizar |
| Instalar os plugins de IDE | É na máquina do usuário (C7) | T030 entrega os caminhos |
| Formatar o `Dockerfile` ou fazer o build Docker reprovar formato | O gate de merge é o CI (§7) | não volta |

---

## 12. Critérios de aprovação

### Para aprovar esta rodada (antes do código)

- [x] O escopo da §1 é o que se espera da próxima entrega
- [x] As decisões da §2 refletem o que foi combinado, incluindo as propostas 6 e 7
- [x] Os planos de mudança da §4 (o que a CLI vai escrever) estão aprovados como o "plano completo" do README 3.1.0
- [x] O impacto da §7 é aceitável, inclusive o que fica frouxo
- [x] A cobertura de provas da §6 é suficiente para o risco assumido
- [x] A decisão de delegação da §8 está adequada
- [x] Os itens fora de escopo (§11) podem mesmo esperar

> ✅ **PORTÃO APROVADO pelo product owner em 2026-10-12.** Falas literais: "Ok para o merge e a
> sequência sugerida das tarefas" e, depois da entrega da fase 2, "eu dei o ok para o
> desenvolvimento. Minha espectativa é que você está seguindo com as demais rodadas que levantamos".
> Aceitos como propostos: D6 (o Spotless e o Checkstyle do Java-GEMS-SDK ficam como higiene), D7
> (`commit` do `gems-ai.json` em `909c69aa…`) e os planos de escrita da CLI da §4 como o "plano
> completo". Se o `--dry-run` da execução divergir desses planos, a execução para e volta ao product
> owner. Push, PR e merge continuam exigindo ok próprio. **O `/speckit-implement` está liberado.**

### Para considerar a rodada concluída (depois do código)

- [x] `.class` idênticos com `-g:none` antes e depois do commit de estilo, nos dois repositórios
- [x] `mvn --batch-mode clean verify` verde no Java-GEMS-SDK, com a mesma contagem de T002
- [x] `.\mvnw.cmd clean install` verde no reator inteiro do sample, com 290 testes e Firebase real
- [x] `npm test`, `npm run build` e `prettier --check` verdes
- [x] Gate GEMS e contexto de agentes verdes; clone limpo conforme
- [ ] Mutação de formato observada localmente e na CI
- [ ] CI verde nos dois PRs, com o log mostrando a CLI 3.1.0 instalada do ref `v3.1.0`
- [ ] Os gates do [quickstart.md](quickstart.md) marcados
- [ ] **Zero regressão** nas features já entregues

---

## Referência dos artefatos

| Arquivo | Para quê |
| :--- | :--- |
| [spec.md](spec.md) | Requisitos, user stories e critérios de sucesso |
| [plan.md](plan.md) | Contexto técnico, Constitution Check, estrutura e delegação |
| [research.md](research.md) | Ensaio medido: migração, adoção, impacto, fim de linha, bytecode |
| [tasks.md](tasks.md) | Tarefas, dependências e ordem |
| [quickstart.md](quickstart.md) | Validação |
| [checklists/](checklists/requirements.md) | Qualidade da especificação |