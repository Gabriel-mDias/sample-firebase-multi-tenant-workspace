# Tasks: Formatter Java do GEMS AI SDK 3.1.0

**Input**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [quickstart.md](quickstart.md)
**Tests**: a rodada não tem lógica nova. A prova é por gate, equivalência de bytecode (R7) e mutação de
formato (SC-004).

Formato: `- [ ] Tnnn [P] [USn] descrição com caminho`. `SDK/` = `Java-GEMS-SDK/`; sem prefixo = sample.

## Phase 1: Setup

- [x] T001 Conferir de novo o hash do wheel `gems_ai-3.1.0` contra o `SHA256SUMS` da release `v3.1.0` e instalar a CLI num venv isolado fora dos repositórios; `gems-ai --version` = `3.1.0`
- [x] T002 Criar a branch `feature/006-formatter-gems-ai` em `SDK/` a partir de `origin/main` (`e830533`) e medir a linha de base: `mvn --batch-mode clean verify -DgenerateBackupPoms=false`, com o total de testes por módulo registrado em `specs/006-formatter-gems-ai/evidence.md`
- [x] T003 Fazer checkout de `909c69aa846ea97b3aea06a08fdfed8c6657d391` em `../.tooling/gems-ai-sdk` (ferramenta local do workspace, R2) e confirmar `scripts/gems-ai.ps1 --version` = `3.1.0` depois de T013

## Phase 2: User Story 1 — SDK formatado (P1) 🎯 MVP

**Goal**: o SDK adota a 3.1.0 com perfil `java` e todo o Java fica no formato.
**Independent Test**: quickstart S1, S2, S5 e S6.

- [x] T004 [US1] Escrever `SDK/gems-ai.json` (`version` 3.1.0, `commit` `909c69aa…`, `profile` `java`, `waivers` `[]`); rodar `gems-ai plan --project . --task governance` e `gems-ai apply --project . --version 3.1.0 --dry-run` e comparar com a lista de R6. Se divergir, parar e voltar ao usuário
- [x] T005 [US1] `gems-ai apply --project . --version 3.1.0` em `SDK/` e depois `gems-ai check --project . --strict` (exit 0)
- [x] T006 [US1] Criar `SDK/.gitattributes` com `.gems-ai/** text eol=crlf` e `.agents/skills/** text eol=crlf` e comentário apontando para R3; `git add --renormalize .`; provar S5
- [x] T007 [US1] Commit `chore(gems-ai): adota GEMS AI 3.1.0 com perfil java` em `SDK/` (manifesto, lock, snapshot, adapters e `.gitattributes`; nenhum `.java`)
- [x] T008 [US1] Compilar o reator `SDK/` em espelho com `mvn -B -q clean test-compile -DskipTests -Dmaven.compiler.debug=true -Dmaven.compiler.debuglevel=none` (nunca `debug=false`, R7) e guardar o SHA-256 de cada `.class` de `target/classes` e `target/test-classes` (linha de base de R7)
- [x] T009 [US1] `gems-ai format java --project . --all --apply` em `SDK/` (208 arquivos esperados) e commit isolado `style: formata o Java com o formatter do GEMS AI 3.1.0`, só com `.java`
- [x] T010 [US1] Recompilar com `-g:none` e comparar com T008: os `.class` devem ser idênticos (S4); registrar em `evidence.md`
- [x] T011 [US1] Criar `SDK/.git-blame-ignore-revs` com o SHA de T009 e comentário; commit `chore: ignora o commit de estilo no blame`; provar S7

## Phase 3: User Story 1 — gate de CI do SDK

- [x] T012 [US1] Em `SDK/.github/workflows/pr-validation.yml`, adicionar o job `gems-ai` em `windows-latest` (R4) com `actions/checkout@v7`, `actions/setup-java@v6` (temurin 21), `Gabriel-mDias/gems-ai-sdk@v3.1.0` e os passos `gems-ai check --project . --strict` e `gems-ai check-java --project . --all`. Manter o Spotless e o Checkstyle do SDK (D6, R6)

## Phase 4: User Story 2 — sample migrado e formatado (P1)

**Goal**: o sample migra para a 3.1.0 e o formatter do GEMS AI passa a ser a única fonte do layout Java.
**Independent Test**: quickstart A1, A2, A3, A5 e A6.

- [x] T013 [US2] `gems-ai update --project . --to 3.1.0 --dry-run` no sample e comparar com a tabela de R1. Se divergir, parar e voltar ao usuário. Depois, `gems-ai update --project . --to 3.1.0`
- [x] T014 [US2] Atualizar `gems-ai.json` `commit` para `909c69aa846ea97b3aea06a08fdfed8c6657d391` (R2) e rodar `gems-ai check --project . --strict` (exit 0)
- [x] T015 [US2] Acrescentar `.gems-ai/java/** text eol=crlf` ao bloco GEMS de `.gitattributes`, depois do `*.properties`; `git add --renormalize .`; provar A5
- [x] T016 [P] [US2] Atualizar a parte local, fora do bloco gerenciado, de `AGENTS.md`, `CLAUDE.md`, `CODEX.md`, `GEMINI.md` e `.github/copilot-instructions.md`: de "3.0.0"/"pin 51b94e3a…" para 3.1.0/`909c69aa…`, com paridade entre os arquivos; `README.md:103`; rodar A2
- [x] T017 [P] [US2] Remover de `backend/pom.xml` o plugin `spotless-maven-plugin` e a propriedade `spotless.check.phase`, e apagar `backend/config/spotless/eclipse-formatter.xml`; manter o Checkstyle (R5); procurar menções a Spotless em `backend/`, `.claude/`, `specs/AGENTS.md` e `D:/repos/build a new sample multi tenant/AGENTS.md` e atualizar para o formatter do GEMS AI (FR-011)
- [x] T031 [US2] Trocar o formatter de `scripts/sample.py` (`initializer_formatter`, `format_initialized_backend`, relatório do dry-run, mensagem de erro, ajuda do `--skip-format`) de `mvnw spotless:apply` para `gems-ai format java --project <raiz> --all --apply` (R11); ajustar `scripts/inicializar-projeto.ps1`, `scripts/benchmark-tenants/README.md` e os formatters falsos de `scripts/test_initializer.py` e `scripts/test_sample.py`
- [x] T018 [US2] Em `scripts/gems-ai-gate.ps1`, acrescentar `check-java --project . --all` como terceiro comando (R9)
- [x] T019 [US2] Em `.github/workflows/ci.yml`: action `Gabriel-mDias/gems-ai-sdk@v3.1.0` (C2), `actions/setup-java@v6` temurin 21 no job `gems-ai` antes do gate, comentário do job atualizado e comentário do `fetch-depth: 0` do job backend corrigido (R5)
- [x] T020 [US2] Commit `chore(gems-ai): migra para o GEMS AI 3.1.0 e troca o Spotless pelo formatter` (T013–T019 e T031; nenhum `.java`)
- [x] T021 [US2] Compilar o reator `backend/` em espelho com os mesmos parâmetros de T008 (`-Dcheckstyle.skip=true` permitido; ensaio: 130 `.class`) e guardar os SHA-256 (linha de base de R7)
- [x] T022 [US2] `gems-ai format java --project . --all --apply` (77 arquivos esperados) e commit isolado `style: formata o Java com o formatter do GEMS AI 3.1.0`, só com `.java`
- [x] T023 [US2] Recompilar com `-g:none` e comparar com T021: idênticos (A4); registrar em `evidence.md`
- [x] T024 [US2] Criar `.git-blame-ignore-revs` com o SHA de T022; commit `chore: ignora o commit de estilo no blame`; provar A7
- [x] T032 [US2] Regenerar `scripts/demo-manifest.json` sobre o código formatado (hashes, `before`/`after` das 28 transformações formatados pelo gems-ai, metadados) e o pin `MANIFEST_SHA256` de `scripts/sample.py`; diagnosticar e corrigir os testes Python restantes (remoção só com prova de obsolescência em `evidence.md`); commit `fix(scripts): regenera o manifesto do clean-demo sobre o código formatado`; provar A10 e A11

## Phase 5: User Story 3 — mutação e autoria (P2)

- [x] T025 [US3] Mutação de formato nos dois repositórios: um espaço extra num `.java`; `check-java --all` e o gate do sample falham (exit ≠ 0) e apontam o arquivo; reverter (S6 e A6)

## Phase 6: Review, gates e PRs

- [x] T026 Review Sol read-only dos dois diffs, sem o commit de estilo, que já tem a prova mecânica: escopo, ausência de waiver, `.gems-ai/` só pela CLI, CI fixada em `@v3.1.0`
- [x] T027 Gate do SDK: S1–S3; PR no `Gabriel-mDias/Java-GEMS-SDK` (só com o ok do usuário); CI verde (S8); merge pelo usuário. Sem release: a próxima versão publicada (3.5.0, 004) já sai formatada
- [x] T028 Gate do sample: A1, A2, A3 (290 testes com Firebase real), A9, A10 e A11; PR no sample (só com o ok do usuário); CI verde nos 4 jobs (A8); merge pelo usuário
- [x] T029 Registrar `evidence.md` (números, bytecode, mutação, CI) e fechar a §12 do roteiro; atualizar o `.specify/feature.json` para a próxima rodada ativa
- [x] T030 Entregar ao usuário a saída de `gems-ai ide artifacts` para a instalação nas IDEs (C7)

## Dependencies

```
T001 → T002 → T004 → T005 → T006 → T007 → T008 → T009 → T010 → T011 → T012 → (PR SDK, T027)
T001 → T003
T001 → T013 → T014 → T015 → {T016, T017, T031} → T018 → T019 → T020 → T021 → T022 → T023 → T024 → T032
T011, T032 → T025 → T026 → T027, T028 → T029 → T030
```

O SDK vem primeiro (decisão 5). O sample pode começar depois de T012, sem esperar o merge do SDK, porque
não depende de artefato publicado do SDK. A fase 3 da 004 só começa depois do merge do PR do SDK.

## Parallel

- T016 e T017 tocam arquivos disjuntos.
- Nenhum outro paralelismo: o `format --all` e os gates leem o repositório inteiro.

## Implementation Strategy

MVP = US1, o SDK formatado com o gate. Isso libera a 004. Em seguida vêm a US2, o sample, e a US3, a
prova de mutação e de autoria, que fecha a rodada.
