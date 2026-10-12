# Quickstart — validação da 006

Pré-requisitos: CLI `gems-ai` 3.1.0, com o hash do wheel conferido contra o `SHA256SUMS` da release; JDK 21+; Docker para o
build do sample; ADC do Firebase de testes por `GOOGLE_APPLICATION_CREDENTIALS`, só pelo caminho.

## Java-GEMS-SDK

| # | Gate | Comando | Esperado |
| :-- | :--- | :--- | :--- |
| S1 | Conformidade | `gems-ai check --project . --strict` | exit 0 |
| S2 | Formato completo | `gems-ai check-java --project . --all` | exit 0, `selected` 220, `changed` vazio |
| S3 | Build | `mvn --batch-mode clean verify -DgenerateBackupPoms=false` | verde, com a mesma contagem de testes medida em T002 |
| S4 | Bytecode | `.class` compilados com `-g:none` antes e depois do commit de estilo | idênticos (R7) |
| S5 | Clone limpo | `git -c core.autocrlf=false clone` e S1 | exit 0 |
| S6 | Mutação | Inserir um espaço em um `.java` e rodar S2 | exit 10, aponta o arquivo |
| S7 | Autoria | `git blame` com `blame.ignoreRevsFile=.git-blame-ignore-revs` numa linha reformatada | commit anterior ao de estilo |
| S8 | CI | PR com o job `gems-ai` | verde; com a mutação S6, vermelho |

## Sample

| # | Gate | Comando | Esperado |
| :-- | :--- | :--- | :--- |
| A1 | Gate GEMS | `scripts/gems-ai-gate.ps1` | exit 0, com `check --strict`, `verify-angular` e `check-java --all` |
| A2 | Contexto | `scripts/verificar-contexto-agentes.ps1` | "Contexto de agentes aprovado." |
| A3 | Build | `backend\mvnw.cmd clean install` | verde, 290 testes, sem plugin Spotless no log |
| A4 | Bytecode | igual a S4, no reator do backend | idênticos |
| A5 | Clone limpo | igual a S5, com o gate A1 | exit 0 |
| A6 | Mutação | igual a S6 | o gate A1 falha |
| A7 | Autoria | igual a S7 | commit anterior |
| A8 | CI | PR com os 4 jobs | verdes; o log do `gems-ai` mostra "gems-ai 3.1.0 instalado do ref 'v3.1.0'" |
| A9 | Frontend | `npm ci`, `npm test`, `npm run build` e `npx prettier --check .` | verdes. A rodada não toca no frontend; o gate só confirma que nada vazou |

## IDE (usuário)

`gems-ai ide artifacts` lista os caminhos e checksums da extensão do VS Code e do plugin do IntelliJ. A
instalação é feita por você. Depois de instalados, ambos formatam ao salvar nos projetos GEMS Java.
