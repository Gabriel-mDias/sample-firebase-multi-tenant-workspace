# Roteiro da Rodada — 006 Formatter Java do GEMS AI SDK 3.1.0

**Feature**: `006-formatter-gems-ai` · **Branch**: `feature/006-formatter-gems-ai` (sample) e uma
branch própria no Java-GEMS-SDK · **Requisito**: pedido do product owner em 2026-10-12 ("aplique esse
formatador no projeto backend, para deixá-lo no formato que eu desejo de escrita de código")
**Data**: 2026-10-12 · **Status**: fase 1 concluída, aguardando o ciclo spec-kit (fase 2)

**Índice**: [1 Objetivo](#1-objetivo-da-rodada) · [2 Decisões de entrada](#2-decisões-de-entrada)

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
| 4 | Java-GEMS-SDK | **Entra na rodada**, com **manifesto Java completo** (`gems-ai init`, perfil java): regras gerenciadas, blocos de agente, snapshot e gate no CI | O SDK segue o mesmo formato que o sample |
| 5 | Ordem com a 004 | **A 006 vem primeiro no SDK.** A 004 é implementada sobre o código já formatado, e a 3.5.0 sai no formato novo | A 004 pode ter o portão aprovado agora, mas a fase 3 dela espera a 006 no SDK |

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
