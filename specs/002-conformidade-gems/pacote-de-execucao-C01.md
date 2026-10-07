# Pacote de Execução — Rodada 002, checkpoint C01 (T009–T012)

| Controle | Valor |
| :--- | :--- |
| Checkpoint | `C01` |
| Pré-requisitos | `C00 verde; T004 aprovado em 2026-09-14` |
| Política de sessão | `nova` |
| SHA anterior | `b5d3410` |
| Estado inicial | `aberto` |
| Gate focado | `busca operacional sem flag + dependency:tree` |

> **Admissão:** contrato congelado: sim; caminhos conhecidos: sim; escritas disjuntas: sim; gate
> focado: sim; economia maior que autoria/revisão: sim. **Modelo: `gpt-5.6-sol`; fallback `gpt-6-astra` autorizado se o Sol estiver indisponível; sem Terra**, conforme
> a decisão 1 do roteiro. Quatro arquivos operacionais, uma varredura fechada e a árvore Maven tornam
> a execução e a revisão menores que redescobrir o alcance. O executor não comita, não faz push nem abre PR.

> **A lista `READ` é fechada.** Se faltar contexto, se surgir caminho fora de `EDIT / CREATE` ou se a
> árvore diferir da âncora, pare e registre no estado; não pesquise nem amplie o lote.

## 1. Fronteira fechada do lote

Concluir T009–T012: remover a flag operacional obsoleta de CI e documentação, provar que não resta
invocação operacional nos seis alvos e medir a árvore Maven final. T008 e o Docker build pertencem a
C10; não editar Dockerfile, manifests, regras, constituição nem código de produção.

### READ

- `specs/002-conformidade-gems/pacote-de-execucao-C01.md` — contrato deste checkpoint
- `specs/002-conformidade-gems/estado-lote-C01.md` — estado a criar e manter
- `.specify/templates/estado-lote-template.md` — formato obrigatório do estado
- `.specify/scripts/powershell/verificar-lote.ps1` — verificação da entrega
- `D:/repos/Meduc/meduc-workspace/.agents/skills/model-delegation/references/package-contract.md` — protocolo do lote
- `AGENTS.md` e `specs/AGENTS.md` — autoridade e regras da camada documental
- `specs/002-conformidade-gems/tasks.md` e `specs/002-conformidade-gems/roteiro.md` — escopo e registro
- `.github/workflows/ci.yml` e `.github/dependabot.yml` — pontos operacionais autorizados
- `README.md` — instrução de instalação autorizada
- `frontend/Dockerfile`, `frontend/package.json` e `frontend/package-lock.json` — alvos da varredura, somente leitura
- `backend/pom.xml` e `backend/app-core/pom.xml` — comparação de árvore Maven

### EDIT / CREATE

| # | Ação | Arquivo |
| :-- | :--- | :--- |
| T009 | EDIT | `.github/workflows/ci.yml` |
| T010 | EDIT | `README.md` |
| T010 | EDIT | `.github/dependabot.yml` |
| C01 | EDIT | `specs/002-conformidade-gems/tasks.md` |
| C01 | EDIT | `specs/002-conformidade-gems/roteiro.md` |

O estado `specs/002-conformidade-gems/estado-lote-C01.md` é a exceção protocolar permitida.

## 2. O que já existe e você consome

```text
frontend/package.json: @gabriel-mdias/angular-gems-sdk ^2.1.0
frontend/package.json: overrides para @angular/common, @angular/core e @angular/router
frontend/Dockerfile: npm ci (T008 ainda sem docker build)
T004: edição de ci.yml, dependabot.yml e README aprovada no portão de 2026-09-14
```

As citações históricas em `.specify/memory/constitution.md`, `.gems-ai/MIGRACAO-2.4.0.md` e
`.gems-ai/recipes/setup-frontend-sdk.md` são imutáveis neste lote: reporte-as separadamente, não as edite.

## 3. Arquivos de referência — copie destes

| Para escrever | Copie de |
| :--- | :--- |
| instalação limpa | `frontend/Dockerfile` — comando `npm ci` sem flag |
| dependabot npm | `.github/dependabot.yml` — comentário deve refletir Angular 22 e SDK 2.1.0 |
| registro SDD | `specs/002-conformidade-gems/roteiro.md` §9 — resumo factual e próxima retomada |

## 4. Regras deste lote

1. Use `npm ci`, nunca `npm install`, nas instruções operacionais.
2. A flag `--legacy-peer-deps` não pode ficar em comando, comentário operacional ou orientação desses seis alvos.
3. Não altere menções históricas/imutáveis nem use exceção GEMS.
4. Não toque em `frontend/Dockerfile`: o seu gate Docker é C10.
5. Registre no estado a saída literal de `git status`; não resuma arquivos tocados.
6. T009–T012 só ficam `[x]` após todos os gates abaixo verdes.
7. Não faça commit, push ou PR.

## 5. O portão

Da raiz do sample:

```powershell
$operational = @('frontend/Dockerfile','.github/workflows/ci.yml','.github/dependabot.yml','README.md','frontend/package.json','frontend/package-lock.json')
rg -n -- '--legacy-peer-deps' $operational
if ($LASTEXITCODE -eq 0) { throw 'A flag operacional ainda existe.' }
if ($LASTEXITCODE -ne 1) { exit $LASTEXITCODE }
Push-Location backend
.\mvnw.cmd dependency:tree
$exit = $LASTEXITCODE
Pop-Location
exit $exit
```

Verde: `rg` retorna 1 (zero ocorrências), Maven retorna 0/`BUILD SUCCESS`; a árvore continua resolvendo
GEMS 3.1.0 e ModelMapper 3.2.4 temporário, sem dependência inesperada. Execute também
`git diff --check`, com exit 0.

## 6. Relatório

1. Arquivos alterados e tarefas concluídas;
2. o que não foi feito e por quê (incluindo T008/C10);
3. gates, exit codes e resumo da árvore;
4. referências imutáveis encontradas, separadas das operacionais;
5. divergências entre pacote e código.

O relatório não substitui `estado-lote-C01.md`. Pare após C01 verde; o próximo checkpoint é definido
pelo orquestrador a partir do estado e do ledger.
