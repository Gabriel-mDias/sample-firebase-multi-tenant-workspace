# Research — 006 Formatter Java do GEMS AI SDK 3.1.0

Ensaio de 2026-10-12, feito em clones descartáveis no scratchpad da sessão (`ensaio-006-*`, `e6sdk*`),
sem tocar nos checkouts reais. A CLI veio da release `v3.1.0`, instalada num venv isolado depois de
conferir o hash do wheel contra o `SHA256SUMS`
(`1a7027d1…cedba887  gems_ai-3.1.0-py3-none-any.whl`). O formatter rodou com JDK 21 e com JDK 26.

## R1 — Caminho da migração do sample (3.0.0 → 3.1.0)

- **Decisão:** `gems-ai update --project . --to 3.1.0`, depois de conferir o `--dry-run`.
- **Medido:** no `--dry-run`, 35 regras ficam iguais e 17 destinos mudam:

  | Tipo | Ação | Destino |
  | :--- | :--- | :--- |
  | rule | create | `.gems-ai/rules/java.formatting.md` |
  | rule | update | `.gems-ai/rules/java.controllers.md`, `.gems-ai/rules/signature.invariants.md` |
  | catalog, version | update | `.gems-ai/catalog.json`, `.gems-ai/.version` |
  | schema | update | `.gems-ai/schemas/gems-ai-lock.schema.json`, `.gems-ai/schemas/gems-ai.schema.json` |
  | formatter | create | `.gems-ai/java/engine.json`, `.gems-ai/java/profile.properties` |
  | adapter | update | `AGENTS.md`, `CLAUDE.md`, `GEMINI.md`, `.github/copilot-instructions.md` (bloco gerenciado) |
  | adapter | create | `.cursor/rules/gems.mdc` |
  | config, lock | update | `gems-ai.json` (só `version`), `gems-ai.lock.json` |
  | transaction | create | `.gems-ai/transactions/<id>/` (12 backups) |

  Depois da aplicação, `check --strict` dá exit 0 (38 regras, `conformant`), e o
  `scripts/verificar-contexto-agentes.ps1` passa: os arquivos raiz ficam com 5.333 bytes e 72 linhas.
- **Convenção mantida:** o diretório `.gems-ai/transactions/` é versionado no sample (178 arquivos
  em `.gems-ai/`, com as transações da 3.0.0). A transação nova entra no mesmo commit.
- **README 3.1.0:** `apply` e `update` exigem "aprovação humana explícita do plano completo". A tabela
  acima é o plano. A aprovação do portão desta rodada cobre exatamente esse plano. Se o `--dry-run` da
  fase 3 divergir dele, a execução para e volta ao usuário.

## R2 — Campo `commit` do manifesto

- **Fato:** o `update` muda só o `version` e mantém `commit: 51b94e3a…` (o pin da 3.0.0). O README
  define `commit` como o SHA de 40 caracteres do SDK candidato, e a tag `v3.1.0` aponta para
  `909c69aa846ea97b3aea06a08fdfed8c6657d391`. O `scripts/gems-ai.ps1` compara esse campo com o
  `HEAD` da fonte local (`../.tooling/gems-ai-sdk`, hoje em `51b94e3`).
- **Decisão:** atualizar o `commit` para `909c69aa…` no mesmo commit da migração. O `gems-ai.json` é
  a configuração do consumidor, fora do `.gems-ai/`. Também é preciso fazer checkout de `909c69aa`
  em `../.tooling/gems-ai-sdk`, ferramenta local do workspace e fora dos repositórios, para o gate
  local e o hook Stop resolverem a 3.1.0.
- **Alternativa descartada:** manter `51b94e3a`. O wrapper rodaria o código da 3.0.0, que não conhece
  a 3.1.0, e o pin ficaria contraditório com a versão.

## R3 — Fim de linha do snapshot do formatter

- **Medido:** num clone limpo com `core.autocrlf=false`, que reproduz o que o CI recebe, o
  `check --strict` falha com `drift detected: .gems-ai\java\engine.json, .gems-ai\java\profile.properties`.
  A causa é a mesma da 003: o lock guarda o hash dos bytes em CRLF, e o `.gitattributes` só força CRLF
  nos caminhos da 3.0.0. Além disso, `*.properties text eol=lf` pega o `profile.properties`.
- **Decisão:** acrescentar `.gems-ai/java/** text eol=crlf` ao bloco GEMS do `.gitattributes`, depois
  da linha do `*.properties`, porque no git o último atributo que casa vence. Com isso, o clone limpo
  passa no `check --strict` e no `check-java --all` (exit 0 nos dois).
- **Java-GEMS-SDK:** o SDK não tem `.gitattributes`. Um `.gitattributes` com
  `.gems-ai/** text eol=crlf` e `.agents/skills/** text eol=crlf` faz o clone limpo passar no
  `check --strict` (exit 0).

## R4 — Separador de caminho no lock e runner da CI

- **Medido:** o lock da 3.1.0 continua gravando caminhos com `\`: são 37 ocorrências no lock do SDK. O
  defeito de `project.py` que prende o job `gems-ai` do sample ao `windows-latest` continua.
- **Decisão:** o job `gems-ai` novo do SDK roda em `windows-latest`, como o do sample. A volta para Ubuntu
  continua dependendo de uma correção na CLI, fora desta rodada.

## R5 — Impacto do formatter no backend do sample

- **Medido:** o `check-java --all` seleciona 78 arquivos e marca 77 para mudar. O `format java --all
  --apply` muda 77 arquivos, com 998 inserções e 1.173 remoções; sem considerar espaço, são 821 e
  996. A nova verificação dá exit 0 e leva cerca de 6 s.
- **Spotless:** com o código formatado, `mvnw validate` falha no `spotless-check` (`app-security`,
  `AcaoSistema.java`, entre outros), porque o perfil `config/spotless/eclipse-formatter.xml` quebra as
  linhas de outro jeito. O conflito é real, e manter os dois faria o formato oscilar.
- **Checkstyle:** com o Spotless desligado por propriedade, o `validate` dá 0 violações em todos os
  módulos. As regras do sample são `ParenPad`, `AvoidStarImport` e `MethodName`. Como nenhuma conflita,
  **nenhuma regra de layout precisa ser desligada** (decisão 2).
- **Decisão:** remover do `backend/pom.xml` o plugin Spotless e a propriedade `spotless.check.phase`,
  e apagar `backend/config/spotless/eclipse-formatter.xml`. Sem o `ratchetFrom`, o comentário do
  `fetch-depth: 0` do job backend no `ci.yml` perde a razão. O `fetch-depth` fica, porque é inócuo, e o
  comentário é corrigido.

## R6 — Impacto do formatter no Java-GEMS-SDK

- **Medido:** a adoção com `profile: java` (`apply --version 3.1.0`, depois do `--dry-run`) cria:
  - 24 regras;
  - catálogo, versão e 4 schemas;
  - 5 arquivos de skill em `.agents/skills/`;
  - o snapshot do formatter;
  - os adapters `AGENTS.md`, `GEMINI.md`, `.cursor/rules/gems.mdc` e `.github/copilot-instructions.md`,
    mais a atualização do `CLAUDE.md` existente com o bloco gerenciado;
  - o lock.

  O `check --strict` dá exit 0. O `check-java --all` seleciona 220 arquivos e marca 208. O `format
  java --all --apply` muda 208 arquivos, com 6.598 inserções e 4.860 remoções.
- **Spotless e Checkstyle do SDK:** com o código formatado, `spotless:check` dá exit 0 nos 18 módulos e
  `checkstyle:check` dá 0 violações nos 18. O Spotless do SDK não é um formatter: ele só aplica
  `trimTrailingWhitespace` e `endWithNewline`. O Checkstyle só tem `FileTabCharacter`,
  `NewlineAtEndOfFile` e `RegexpSinglelineJava`.
- **Decisão proposta (D6 do roteiro):** **manter** o Spotless e o Checkstyle do SDK como checagens
  de higiene. Eles não decidem layout e não conflitam com o formatter, então não há oscilação. A
  decisão 2 ("o formatter substitui o Spotless") foi tomada sobre o backend do sample, onde o Spotless
  é um formatter completo.
- **Não existe `gems-ai init`:** a 3.1.0 só tem `inspect`, `plan`, `apply` e `update`. Pelo README, a
  adoção é escrever o `gems-ai.json` (`version`, `commit`, `profile`), rodar `plan`, `apply --dry-run`
  e então `apply`. A decisão 4 do roteiro foi corrigida para esse fluxo.

## R7 — Prova de que a reformatação não muda comportamento

- **Decisão:** além do build completo verde com a mesma contagem de testes (C3), comparar os `.class`
  compilados com `-g:none` antes e depois do commit de estilo. Sem informação de depuração, a única
  diferença possível entre os dois bytecodes seria semântica, porque número de linha e nome de variável
  local não entram. Isso prova FR-005 de forma mecânica e barata, sem depender só da suíte.
- **Medido no ensaio do sample:** `mvnw -o clean test-compile -DskipTests -Dmaven.compiler.debug=true
  -Dmaven.compiler.debuglevel=none` no commit da migração e no da formatação gerou 130 `.class` (main e
  test) em cada um, com **0 diferenças de SHA-256**.
- **Armadilha:** `-Dmaven.compiler.debug=false` **não** gera `-g:none`, porque o javac mantém
  `LineNumberTable` e `SourceFile`. No ensaio, com essa opção, 111 classes diferiram só no número de
  linha (conferido com `javap -v`). O par certo é `debug=true` + `debuglevel=none`.
- **Alternativa descartada:** confiar só na suíte. Ela não cobre todo caminho de código.

## R8 — Saída do formatter que chama atenção

- O formatter junta numa linha só a lista de constantes de enum que o Spotless quebrava. O máximo medido
  é 1.213 caracteres, em `AcaoSistema.java`, e 374 linhas passam de 120 caracteres no backend formatado.
  Essa é a saída do perfil do product owner (`profile.properties` do SDK GEMS AI). Esta rodada não muda
  o perfil. Se o product owner quiser outro comportamento, a mudança é no gems-ai-sdk, e depois uma
  atualização de versão aqui.
- Linhas em branco passam a separar campos e métodos (por exemplo, `DemoAccessController.java`).

## R9 — Gate e CI depois da migração

- **Action:** `Gabriel-mDias/gems-ai-sdk@v3.1.0`. Sem `args`, ela só instala a CLI e confere a versão
  contra o `gems-ai.json`. O gate continua no `scripts/gems-ai-gate.ps1`.
- **JDK no job `gems-ai`:** o `check-java` exige JDK 21+. O job ganha `actions/setup-java@v6`
  (`temurin`, `21`) explícito, para não depender do JDK padrão da imagem `windows-latest`.
- **Gate local:** o `scripts/gems-ai-gate.ps1` ganha `check-java --project . --all` como terceiro
  comando. O hook Stop e o CI rodam o mesmo script, então a falha aparece antes do push. O custo é de
  cerca de 6 s.

## R10 — Conflito com as branches abertas

- A 004 (`feature/004-*`) e a 005 (`feature/005-*`) só têm commits de documentação. A 005 não toca em
  Java. O Java da 004, no SDK e no sample, nasce depois da 006, que vem antes pela decisão 5. Não há
  rebase com conflito de formatação.
