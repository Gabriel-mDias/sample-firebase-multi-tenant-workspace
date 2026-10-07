# Estado — Lote C01, rodada 002

## 1. Rollback deste lote

Estado inicial conferido em 2026-09-15: HEAD `b5d3410`; árvore idêntica à âncora
`C:/Users/gabri/AppData/Local/Temp/sample-c01-pre-dispatch-20260915.porcelain`.
O único arquivo preexistente não rastreado é o pacote C01, que será preservado.
Se necessário, o orquestrador pode restaurar somente `.github/workflows/ci.yml`,
`.github/dependabot.yml`, `README.md`, `specs/002-conformidade-gems/tasks.md` e
`specs/002-conformidade-gems/roteiro.md` a partir de `b5d3410`, após conferir que não receberam
edições posteriores de terceiros, e remover somente este estado criado pelo executor.
Nenhum rollback foi executado.

## 2. Concluído

| Tarefa | O que provou |
| :-- | :--- |
| T009 | YAML analisado por `yaml.safe_load`; step afirmado como `npm ci` no diretório `frontend`, exit 0. Comando/comentário corrigidos. |
| T010 | README usa `npm ci`; comentários CI/Dependabot refletem Angular 22/SDK 2.1.0/overrides. `rg -n -- '--legacy-peer-deps' README.md .github/dependabot.yml`: exit 1, zero ocorrências. |
| T011 | `rg -n -- '--legacy-peer-deps' frontend/Dockerfile .github/workflows/ci.yml .github/dependabot.yml README.md frontend/package.json frontend/package-lock.json`: exit 1, zero ocorrências. Referências imutáveis separadas na §6. |
| T012 | Em `backend`, `.\mvnw.cmd dependency:tree`: exit 0, BUILD SUCCESS nos seis módulos. GEMS 3.1.0, ModelMapper 3.2.4 temporário, MapStruct/processador 1.6.3. |

Comparação com POMs e o registro C00/T005–T006 em `tasks.md`: versões aprovadas mantidas,
nenhuma alteração de dependência introduzida pelo lote. `git diff --exit-code -- backend/pom.xml
backend/app-core/pom.xml frontend/Dockerfile frontend/package.json frontend/package-lock.json`
retornou 0. `git diff --check` retornou 0. T009–T012 marcadas somente após os gates verdes.
O resumo da árvore contém GEMS utils, exception, rest-common, jpa, model-mapper, openapi,
mapstruct, observability, aws e aws-web, todos em 3.1.0; App, Security, Model, Events, Core e Deploy
registraram SUCCESS. Não foi aberta uma árvore histórica fora de READ: a comparação usa o registro
C00 autorizado e os POMs, sem alegar comparação integral de logs históricos.
`verificar-lote.ps1 -Pacote specs/002-conformidade-gems/pacote-de-execucao-C01.md
-Estado specs/002-conformidade-gems/estado-lote-C01.md
-Ancora C:/Users/gabri/AppData/Local/Temp/sample-c01-pre-dispatch-20260915.porcelain`
retornou 0: cinco arquivos esperados, declarados e reais; extensão do lote confere.

## 3. Em curso

- **Tarefa:** nenhuma; C01 verde.
- **Arquivo:** estado, tasks e roteiro atualizados.
- **O que já está feito nele:** T009–T012 concluídas e gates registrados.
- **O que falta:** nenhuma tarefa deste lote; próximo checkpoint definido pelo orquestrador.

## 4. Não iniciado

Nenhuma tarefa C01 não iniciada. T008/Docker pertence a C10 e está fora deste lote.

## 5. Arquivos tocados

Saída literal de `git status --porcelain --untracked-files=all`, exceto este estado:

```text
 M .github/dependabot.yml
 M .github/workflows/ci.yml
 M README.md
 M specs/002-conformidade-gems/roteiro.md
 M specs/002-conformidade-gems/tasks.md
?? specs/002-conformidade-gems/pacote-de-execucao-C01.md
```

## 6. Decisões fora do pacote

Nenhuma ampliação. O pacote informa referências imutáveis em
`.specify/memory/constitution.md`, `.gems-ai/MIGRACAO-2.4.0.md` e
`.gems-ai/recipes/setup-frontend-sdk.md`; estão fora de READ e não serão abertas nem editadas.
São referências reportadas pelo pacote, não ocorrências novamente aferidas pelo executor.
Nenhuma divergência de árvore inicial ou dos contratos consumidos. A descrição menciona quatro
arquivos operacionais, mas a lista EDIT autoriza três e exclui Dockerfile expressamente; seguiu-se
a lista nominal. T008/Docker build não executado porque pertence a C10. Não houve commit, push ou PR.
As tentativas auxiliares de validação YAML via Node falharam por passagem de aspas e módulo `yaml`
ausente; a validação foi concluída com PyYAML já disponível, sem instalação ou criação de arquivo.
A primeira seleção de step por nome sofreu codificação no pipe; a asserção final usa o comando e
o diretório exatos. Nenhuma dessas tentativas alterou o workflow além da edição autorizada.

## 7. O que está quebrado agora

Nada no escopo C01: busca, YAML, dependency:tree e diff check verdes. O gate Docker permanece
pendente em C10. Gates completos de backend/frontend não foram executados neste checkpoint focado.

## 8. Retomada

- **Executor:** Codex / gpt-6-astra, fallback autorizado pelo pacote.
- **Token de retomada:** não disponibilizado; identificação da tarefa: `/root/execute_c01_astra`.
- **Ponto exato:** C01 verde; conferir pacote/estado/âncora e aguardar checkpoint do orquestrador.
