# Implementation Plan: Formatter Java do GEMS AI SDK 3.1.0

**Branch**: `feature/006-formatter-gems-ai` (sample) · `feature/006-formatter-gems-ai` (Java-GEMS-SDK, a
partir de `origin/main` `e830533`) | **Date**: 2026-10-12 | **Spec**: [spec.md](spec.md)

## Summary

O GEMS AI 3.1.0 vira a fonte única do formato Java nos dois repositórios Java. No SDK: adoção com o perfil
`java`, `.gitattributes` para o snapshot, reformatação em massa num commit isolado e job `gems-ai` na CI.
No sample: migração 3.0.0 → 3.1.0, `commit` do manifesto em `909c69aa`, fim de linha do snapshot novo,
saída do Spotless, reformatação em massa isolada, `check-java --all` no gate local e na CI, e a action
fixada em `@v3.1.0`. Fundamentos e números em [research.md](research.md).

## Technical Context

**Language/Version**: Java 21 (código formatado); Python 3.11+ (CLI `gems-ai` 3.1.0); JDK 21+ para o formatter
**Primary Dependencies**: `gems-ai` 3.1.0 (Eclipse JDT 3.46.0 + adapter GEMS, embutido no wheel); Maven
(Checkstyle 3.6.0/13.9.0 no SDK, `maven-checkstyle-plugin` 3.6.0 no sample)
**Storage**: N/A, sem banco
**Testing**: build completo (`mvn clean verify` no SDK, `mvnw.cmd clean install` no sample, com Firebase
real); `check --strict`; `check-java --all`; comparação de bytecode com `-g:none` (R7); mutação de formato
**Target Platform**: CI GitHub Actions (`windows-latest` no job `gems-ai`, R4); Windows local
**Project Type**: ferramenta e build. Não muda comportamento de runtime
**Performance Goals**: `check-java --all` em cerca de 6 s no sample (medido)
**Constraints**: `.gems-ai/` só pela CLI; sem waiver; commit de estilo sem mudança semântica (C3)
**Scale/Scope**: 78 arquivos Java no sample (77 mudam) e 220 no SDK (208 mudam)

## Constitution Check

| Princípio | Situação |
| :--- | :--- |
| I. Fronteiras de módulo | ✅ Nenhuma dependência entre módulos muda |
| II, III. Persistência e Liquibase | ✅ Não tocados |
| IV, V, VI. Serviço, REST e frontend | ✅ Não tocados. O frontend fica fora (C4) |
| VII. Testes como gate | ✅ Build completo verde antes e depois, com a mesma contagem, e equivalência de bytecode (R7). O gate ganha a verificação de formato |
| VIII. Especificação antes de código | ✅ Este ciclo spec-kit e o portão da §12 do roteiro |
| IX. Configuração é código | ✅ CI, `.gitattributes`, gate e manifesto versionados |
| Governance: waiver | ✅ Nenhum waiver nem baseline. O drift de fim de linha é resolvido na causa (R3), não suprimido |

Reavaliação pós-design: sem violação.

## Project Structure

### Documentation (this feature)

```text
specs/006-formatter-gems-ai/
├── roteiro.md, spec.md, plan.md, research.md, quickstart.md, tasks.md
└── checklists/requirements.md
```

Sem `data-model.md` (não há dado) e sem `contracts/` (não há interface pública nova; a API do SDK não muda).

### Source Code (arquivos tocados)

```text
Java-GEMS-SDK/
├── gems-ai.json, gems-ai.lock.json, .gitattributes, .git-blame-ignore-revs   (novos)
├── .gems-ai/**, .agents/skills/**, AGENTS.md, GEMINI.md, .cursor/rules/gems.mdc,
│   .github/copilot-instructions.md                                         (gerados pela CLI)
├── CLAUDE.md                                                               (bloco gerenciado anexado pela CLI)
├── .github/workflows/pr-validation.yml                                     (job gems-ai)
└── **/*.java                                                               (commit de estilo)

sample-firebase-multi-tenant-workspace/
├── gems-ai.json, gems-ai.lock.json, .gems-ai/**, .cursor/rules/gems.mdc     (CLI; commit no manifesto à mão, R2)
├── AGENTS.md, CLAUDE.md, CODEX.md, GEMINI.md, .github/copilot-instructions.md (bloco pela CLI; parte local à mão)
├── .gitattributes, .git-blame-ignore-revs
├── backend/pom.xml, backend/config/spotless/                               (saída do Spotless)
├── scripts/gems-ai-gate.ps1, .github/workflows/ci.yml
└── backend/**/*.java                                                       (commit de estilo)
```

**Structure Decision**: cada repositório recebe a sequência de três commits: adoção ou migração, estilo e
`.git-blame-ignore-revs`. O commit de estilo só contém `.java`.

## Delegação

| Trabalho | Executor | Razão |
| :--- | :--- | :--- |
| Execução da CLI (`update`, `apply`, `format`) e gates | Coordenador | São comandos determinísticos com plano já aprovado, e o onboarding de um worker não se amortiza |
| Edição de `pom.xml`, `ci.yml`, `pr-validation.yml`, gate e docs de agente | Sol (Codex) low | São mecânicas, com gate de saída |
| Review independente dos dois PRs | Sol read-only | Muda o gate de CI. Os dois diffs de estilo são grandes, e a prova principal é mecânica (R7) |

Nenhum uso de Opus como executor.

## Complexity Tracking

Sem violação de constituição a justificar.
