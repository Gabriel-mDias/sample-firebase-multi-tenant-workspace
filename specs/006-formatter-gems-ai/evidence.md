# Evidências — 006 Formatter Java do GEMS AI SDK 3.1.0

Registro do que foi observado na execução. O que ainda não rodou não aparece aqui.

## T001 — CLI

Em 2026-10-12, o SHA-256 do `gems_ai-3.1.0-py3-none-any.whl` deu
`1a7027d10cb1041c6fff52367d7dac15f365fb650f3abe965afddee1cedba887`, igual à linha do `SHA256SUMS` da
release `v3.1.0`. A CLI está num venv isolado fora dos repositórios, e `gems-ai --version` = `3.1.0`.

## T002 — Linha de base do Java-GEMS-SDK

A branch `feature/006-formatter-gems-ai` foi criada a partir de `origin/main` `e830533`. Com JDK 21,
`mvn --batch-mode clean verify -DgenerateBackupPoms=false` terminou em `BUILD SUCCESS`, nos 18 módulos
do reator, com **402 testes**, 0 falhas, 0 erros e 0 ignorados.

| Módulo | Testes |
| :--- | --: |
| GEMS SDK Utils | 38 |
| GEMS SDK Model Mapper | 5 |
| GEMS SDK MapStruct | 4 |
| GEMS SDK Exception | 29 |
| GEMS SDK JPA | 4 |
| GEMS SDK JPA Multi-Tenant | 41 |
| GEMS SDK Auditing | 35 |
| GEMS SDK Keycloak Admin | 33 |
| GEMS SDK Security Authorization | 25 |
| GEMS Firebase Auth | 174 |
| GEMS SDK AWS | 11 |
| GEMS SDK Validation | 3 |

Os outros 6 módulos (raiz, BOM, AWS Web, REST Common, OpenAPI e Observability) não têm testes.

## Java-GEMS-SDK — T004 a T012 (branch `feature/006-formatter-gems-ai`, local, sem push)

| Commit | Conteúdo |
| :--- | :--- |
| `72a7234` | `chore(gems-ai): adota GEMS AI 3.1.0 com perfil java` |
| `3d3fb2b` | `style: formata o Java com o formatter do GEMS AI 3.1.0` (só `.java`) |
| `9d75efb` | `chore: ignora o commit de estilo no blame` |
| `4f85c3e` | `ci: gate GEMS AI e formato Java` |

- **T004:** `plan --task governance` exit 0. O `apply --dry-run` bateu com R6: 24 regras, catálogo,
  versão, 4 schemas, 5 arquivos de skill, 2 arquivos do snapshot do formatter, 4 adapters criados, o
  `CLAUDE.md` atualizado e o lock, além da transação com o backup do `CLAUDE.md`. A transação segue a
  convenção do sample e é versionada.
- **T005 e S1:** `apply` exit 0; `check --strict` exit 0 (`conformant`, 24 regras).
- **T006 e S5:** clone com `core.autocrlf=false` e `check --strict` exit 0.
- **T009 e S2:** `format java --all --apply` mudou 208 arquivos, com 6.598 inserções e 4.860
  remoções, igual ao ensaio. Depois, `check-java --all` exit 0 e `changed` vazio. O `selected` deu
  221: são os 220 `.java` versionados mais um gerado em `target/`, que a CLI exclui (`excluded`).
- **T008, T010 e S4:** a compilação com `-Dmaven.compiler.debug=true -Dmaven.compiler.debuglevel=none`
  foi feita num clone com `core.longpaths=true` gravado, nos commits `72a7234` e `3d3fb2b`. Deu 334
  `.class` nos dois e **0 diferenças** de SHA-256. Uma primeira tentativa foi descartada: num clone
  sem `core.longpaths` gravado, 5 arquivos de caminho longo não foram extraídos.
- **S3:** `mvn --batch-mode clean verify` deu `BUILD SUCCESS` com **402 testes**, igual à T002 módulo
  a módulo. O Spotless (higiene) e o Checkstyle continuaram passando (decisão 6).
- **T011 e S7:** sem o arquivo de ignore, o blame da linha 22 de `AuditActor.java` aponta `3d3fb2b`;
  com `blame.ignoreRevsFile`, aponta `364c129`.
- **T012:** job `gems-ai` no `pr-validation.yml` (`windows-latest`, JDK 21, `@v3.1.0`, `check --strict`
  e `check-java --all`). A S8, que é a CI do PR, ainda não rodou.
- **T025 e S6:** um espaço extra em `AuditActor.java` faz o `check-java --all` sair com exit 10 e
  apontar o arquivo. Revertido, exit 0.

## Sample — T013 a T024, T031 e T032 (branch `feature/006-formatter-gems-ai`, local, sem push)

| Commit | Conteúdo |
| :--- | :--- |
| `715036b` | `chore(gems-ai): migra para o GEMS AI 3.1.0 e troca o Spotless pelo formatter` |
| `083ed4d` | `style: formata o Java com o formatter do GEMS AI 3.1.0` (só `.java`) |
| `509cf7f` | `chore: ignora o commit de estilo no blame` |
| `3b3c008` | `docs(backend): o javadoc das exceções escritas deixa de citar o Spotless` |
| `c95dcb3` | `fix(scripts): regenera o manifesto do clean-demo sobre o código formatado` |

- **T013:** o `update --dry-run` bateu com a tabela R1: 35 regras iguais, 17 destinos e 12 backups.
- **T014 (decisão 7):** com o `commit` trocado à mão para `909c69aa…`, o `check --strict` acusou
  `manifest-lock-mismatch`, porque o lock também guarda o pin. Um segundo `update --to 3.1.0
  --dry-run` listou só `lock update`, já previsto no plano aprovado, e a CLI regravou o lock. Depois,
  `check --strict` exit 0 com 38 regras. Ficaram duas transações da 3.1.0 (`20261012T224650Z542008`
  e `20261012T224749Z841066`). O ensaio da fase 2 não tinha medido isso.
- **T003:** `../.tooling/gems-ai-sdk` recebeu o fetch da tag `v3.1.0` e o checkout em `909c69a`. O
  `scripts/gems-ai.ps1 --version` = `3.1.0`.
- **T016:** a versão e o pin foram atualizados fora dos blocos gerenciados. Os arquivos foram `AGENTS.md`,
  `CLAUDE.md`, `CODEX.md`, `GEMINI.md`, `.github/copilot-instructions.md`, as 5 regras de
  `.cursor/rules`, as 4 de `.github/instructions`, `.kilocode/rules/firebase-multi-tenant.md`,
  `scripts/hooks/gems-ai-session.ps1` e `README.md`. A lista é maior que a da T016 porque o
  verificador de contexto exige paridade entre as camadas. A2: "Contexto de agentes aprovado.".
- **Pendência de governança, sem alteração:** a tabela de stack da constituição (`.specify/memory/constitution.md`,
  linha "Factory") ainda diz `GEMS AI 3.0.0 pin 51b94e3a…`. Corrigir exige emenda com incremento de
  versão, que é decisão do product owner. A divergência fica registrada.
- **T022:** `format java --all --apply` mudou 77 arquivos, com 998 inserções e 1.173 remoções, igual ao
  ensaio. Depois, `check-java --all` exit 0.
- **T021, T023 e A4:** num clone limpo, nos commits `715036b` e `083ed4d`, a compilação deu 130 `.class` nos dois e
  **0 diferenças**.
- **T024 e A7:** em `BootstrapOperator.java`, a linha reformatada aponta `54a9712` com o arquivo de ignore.
  As duas linhas em branco criadas pelo formatter continuam no commit de estilo, porque não têm origem
  anterior.
- **T031:** o `sample.py` resolve o `gems-ai` do PATH e confere `gems-ai --version` contra o
  `gems-ai.json` (fail-closed, como o wrapper). Mantém a checagem de Java e roda
  `format java --project <raiz> --all --apply`. Os testes do inicializador usam o runner injetado,
  como já faziam.
- **T032 e A10:** a causa da falha na `main` foi medida. O manifesto da T028 tinha sido gerado num checkout
  em CRLF: `app-security/pom.xml` e `changelog-demo-registro.xml` só batem com o hash convertidos para
  CRLF. Além disso, o `.gitignore` mudou depois (`a90444e`). Na regeneração, 24 hashes mudaram e os 9 textos `after` em Java
  passaram pelo formatter do GEMS AI num clone descartável. Resultado: `python -B -m unittest discover -s scripts
  -p "test_*.py"` passa nos **60 testes**, contra 50 falhas e 7 erros antes. Nenhum teste foi
  removido. O `clean-demo --dry-run` no checkout real foi aceito (exit 0).
- **A1:** `scripts/gems-ai-gate.ps1` exit 0 no checkout real (`check --strict`, `verify-angular` e
  `check-java --all`, com 78 selecionados).
- **A5:** num clone com `core.autocrlf=false`, o gate deu exit 0.
- **T025 e A6:** com um espaço extra em `BootstrapOperator.java`, o gate sai com exit 1
  (`falhou: gems-ai check-java --project . --all (exit 10)`). Revertido, exit 0.

## Linha de base dos testes Python do sample (R11)

Em 2026-10-12, na branch da 005 rebaseada sobre `origin/main` `5b849a3`,
`python -m unittest discover -s scripts -p "test_*.py"` rodou 60 testes, com 50 falhas e 7 erros.
Em 48 deles a causa é `Protected metadata integrity conflict: backend/app-security/pom.xml`. O
`clean-demo --root <checkout> --dry-run` real recusa pelo mesmo motivo.
