# Estado — Lote C06, rodada 002

## 1. Rollback deste lote

Restaurar somente os quatro arquivos de convenções e os três artefatos SDD enumerados no pacote C06, depois de compará-los à âncora `C:/Users/gabri/AppData/Local/Temp/sample-c06-pre-dispatch-20260916.porcelain`. Nunca usar rollback amplo; preservar as quatro alterações SDD anteriores que a âncora desconta.

## 2. Concluído

| Tarefa | O que provou |
| :-- | :--- |
| T060 | Baseline de arquitetura com uma entrada, supressão Checkstyle e regra de nomes mutada falharam nos respectivos gates; as três mutações foram restauradas por edição inversa. A tolerância final é zero. |
| T061 | `backend: .\mvnw.cmd clean install` verde nos seis módulos; 216 testes em 30 relatórios, zero falhas/erros/skips, Checkstyle zero e Spotless verde. |

## 3. Em curso

- **Tarefa:** nenhuma; C06 concluído.
- **Arquivo:** nenhum com mutação ativa.
- **O que já está feito nele:** T060 e T061 concluídos.
- **O que falta:** parar; C07 é a próxima seleção do ledger.

## 4. Não iniciado

Nenhuma neste pacote.

## 5. Arquivos tocados

`git status --porcelain --untracked-files=all`

```text
 M backend/app-deploy/src/test/java/br/com/gems/app/convencoes/ConvencoesArquiteturaTest.java
 M backend/app-deploy/src/test/java/br/com/gems/app/convencoes/ConvencoesFonteTest.java
 M backend/app-deploy/src/test/java/br/com/gems/app/convencoes/RegrasDeFonte.java
 M backend/app-deploy/src/test/java/br/com/gems/app/convencoes/RelatorioDeConformidade.java
 M specs/002-conformidade-gems/roteiro.md
 M specs/002-conformidade-gems/tasks.md
?? specs/002-conformidade-gems/estado-lote-C06.md
?? specs/002-conformidade-gems/pacote-de-execucao-C06.md
```

## 6. Decisões fora do pacote

Nenhuma. Sem delegação porque a economia do lote não supera a revisão da rede e a repetição do reator.

## 7. O que está quebrado agora

Nada quebrado; as mutações foram restauradas e o reator completo terminou verde.

## 8. Retomada

- **Executor:** Codex
- **Token de retomada:** não exposto por este ambiente; a política é `nova`.
