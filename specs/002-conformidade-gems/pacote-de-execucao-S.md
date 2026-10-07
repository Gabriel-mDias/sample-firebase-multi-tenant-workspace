# Pacote de Execução — Rodada 002, checkpoint C00 (recuperação de T005–T008)

| Controle | Valor |
| :--- | :--- |
| Checkpoint | `C00` |
| Pré-requisitos | `nenhum` |
| Política de sessão | `nova` |
| SHA anterior | `c1a466b` |
| Estado inicial | `em recuperação` |
| Gate focado | `backend: dependency:tree + app-core -am test; frontend: npm ci; git diff --check` |

> **Admissão**: contrato congelado: sim; caminhos conhecidos: sim; escritas disjuntas: sim; gate
> focado: sim; economia maior que onboarding/revisão: sim. **Modelo: `gpt-5.6-sol`.** O trabalho já
> existe, mas precisa de uma sessão nova que confira cinco manifests, refaça três provas e deixe um
> estado confiável; o pacote é menor que repetir a descoberta interrompida.
>
> **A lista `READ` é fechada. Nada fora dela pode ser lido ou pesquisado.** Se faltar contexto, pare
> e reporte. Você não comita, não faz push e não abre PR. Migre o estado legado para o cabeçalho do
> novo template antes de qualquer outra edição. O identificador antigo é somente diagnóstico.

## 1. Fronteira fechada do lote

Recuperar e validar somente o trabalho já presente de T005–T008. T005–T007 podem ser marcadas `[x]`
se as provas abaixo passarem. T008 permanece `[ ]`: a alteração do Dockerfile é preservada, mas o
`docker build --target build .` continua obrigatório e pertence ao checkpoint C10. Não iniciar T009.

### READ

- `specs/002-conformidade-gems/pacote-de-execucao-S.md`
- `specs/002-conformidade-gems/estado-lote-S.md`
- `.specify/templates/estado-lote-template.md`
- `.specify/scripts/powershell/verificar-lote.ps1`
- `D:/repos/Meduc/meduc-workspace/.agents/skills/model-delegation/references/package-contract.md`
- `.claude/skills/delegacao-de-modelos/references/contrato-do-executor.md`
- `AGENTS.md`
- `backend/AGENTS.md`
- `frontend/AGENTS.md`
- `specs/AGENTS.md`
- `.specify/memory/constitution.md`
- `specs/002-conformidade-gems/tasks.md`
- `specs/002-conformidade-gems/roteiro.md`
- `specs/002-conformidade-gems/medicao-gates.md`
- `backend/pom.xml`
- `backend/app-core/pom.xml`
- `frontend/package.json`
- `frontend/package-lock.json`
- `frontend/Dockerfile`

### EDIT / CREATE

| # | Ação | Arquivo |
| :-- | :--- | :--- |
| C00 | EDIT | `specs/002-conformidade-gems/tasks.md` |
| C00 | EDIT | `specs/002-conformidade-gems/roteiro.md` |

O estado `specs/002-conformidade-gems/estado-lote-S.md` é a exceção protocolar e deve ser atualizado.
Os cinco manifests já modificados pertencem à âncora anterior ao despacho: confira-os, mas não os
altere. Se encontrar defeito, registre o arquivo e pare para revisão do pacote.

## 2. Estado recebido

- `HEAD` e upstream: `c1a466b`, branch `feature/conformidade-gems`.
- A árvore contém alterações em `backend/pom.xml`, `backend/app-core/pom.xml`,
  `frontend/package.json`, `frontend/package-lock.json` e `frontend/Dockerfile`.
- O estado legado registra T005, T006 e T007 concluídas e T008 interrompida no build Docker.
- O build autenticou no registry e chegou ao `npm run build`, mas o contexto Docker sem `.git`
  tornou o gate de convenções incapaz de reconhecer o baseline. Não mude o gate para fazê-lo passar.

## 3. Conferência nominal

1. Revise o diff dos cinco manifests: GEMS Java 3.1.0 em um único lugar, `gems-mapstruct` somente no
   consumidor, pino ModelMapper 3.2.4 temporário, Angular SDK `^2.1.0` com overrides e `npm ci` no
   Dockerfile.
2. Não leia `.npmrc`, `.env` ou qualquer credencial. Não imprima variáveis de ambiente.
3. Converta o estado ao novo cabeçalho: C00, pré-requisito nenhum, sessão nova, SHA anterior
   `c1a466b`, inicial `em recuperação`, final `verde` somente depois dos gates, e ponto de recuperação.
4. Atualize `tasks.md`: T005–T007 `[x]` com evidência curta; T008 fica `[ ]` com implementação
   preservada e gate Docker remetido explicitamente a C10. Não altere T009–T012.
5. Atualize somente a linha de implementação/§9 do `roteiro.md` com o resultado de C00 e a retomada
   por C01; não reescreva planejamento.

## 4. Não objetivos e paradas

- Não executar Docker neste checkpoint; não afrouxar, remover nem contornar `verify-conventions`.
- Não editar os cinco manifests recebidos, código Java/TypeScript, CI, README, regras ou ambiente.
- Não iniciar T009–T012 nem qualquer fase posterior.
- Pare se o diff divergir do estado, se uma prova falhar, se for necessário ler fora de `READ`, ou
  se qualquer arquivo novo aparecer fora do estado/pacote.

## 5. Gate focado

Da raiz do sample, execute nesta ordem e registre exit/contagens no estado:

```powershell
Push-Location backend
.\mvnw.cmd dependency:tree
if ($LASTEXITCODE) { exit $LASTEXITCODE }
.\mvnw.cmd -pl app-core -am test
if ($LASTEXITCODE) { exit $LASTEXITCODE }
Pop-Location
Push-Location frontend
npm ci
if ($LASTEXITCODE) { exit $LASTEXITCODE }
Pop-Location
git diff --check
```

Verde: dependency tree e testes Maven com exit 0; `npm ci` sem `--legacy-peer-deps` com exit 0; diff
sem erro. O Docker continua pendente para C10 e não reduz o resultado deste gate focado.

## 6. Relatório

Atualize o estado com o cabeçalho novo, a saída literal do status sem a linha do próprio estado,
gates reais e nenhuma tarefa em curso. Declare C00 `verde`, T008/C10 pendente e C01 como próximo.
Não comite. Pare após o relatório.
