# Estado — C13, rodada 002

Data: 2026-09-23. Estado inicial: **aberto**. Estado atual: **concluído, T074–T076 3/3**.
Âncora: `evidencias/sample-c13-anchor-porcelain.txt` no ledger de controle. Sessão: retomada.

## 1. Rollback deste lote

Restaurar imediatamente por edição inversa qualquer mutação temporária nomeada na seção 3. Os
artefatos C11/C12 são a base preservada. Para cancelar C13, remover nomeadamente apenas
`pacote-de-execucao-C13.md` e `estado-lote-C13.md`; nunca usar reset, clean ou checkout amplo.

## 2. Concluído

| Tarefa | O que provou |
| :-- | :--- |
| Preparação | Branch/upstream em `098a9f5`, ahead/behind `0/0`; estado real coincide com a seção 5 do estado C12. C13 não depende de nova decisão humana. |
| T074 — catálogo | Mutação `GLOBAL` → `TENANT` em `AcaoSistema.scope()` executou 1 teste e ficou vermelha: catálogo global esperado 21, obtido 0. Patch inverso aplicado; repetição executou 1/1 teste verde, exit 0. |
| T074 — endpoint | `@GlobalEndpoint` e seu import foram removidos temporariamente de `ParceiroController`. O gate focal via `cmd /c` executou 2 testes e ficou vermelho com 1 erro: os cinco endpoints do controller foram nomeados como “sem classificação”. Patch inverso aplicado; repetição executou 2/2 testes verdes, zero falhas/erros/skips, exit 0. A primeira tentativa via PowerShell não chegou aos testes porque reinterpretou `-Dsurefire.failIfNoSpecifiedTests=false`; ela não foi contabilizada como prova. |
| T074 — contexto de auditoria | O default de produção `context-columns-enabled` foi alterado temporariamente para `false`. O primeiro gate executou 13/13 verdes porque o YAML de teste fixa `true` e mascara o default; a repetição com a propriedade efetiva `-Dgems.auditing.context-columns-enabled=false` ficou vermelha com 3 falhas, zero erros/skips: correlação nula, `actorId` nulo e filtro por identidade vazio. Patch inverso aplicado; sem override, o gate repetiu 13/13 verde, exit 0. Nenhum arquivo de teste fora do pacote foi editado. |
| T074 — rollback transacional | Expansão dos quatro SQLs de `ParceiroRollbackIntegrationTest` aprovada nominalmente em 2026-09-23. O teste passou 2/2 com a produção restaurada; sem `rollbackFor = Exception.class`, ficou vermelho com 1 falha porque uma linha `PARCEIRO` persistiu. Patch inverso aplicado; repetição 2/2 verde, zero falhas/erros/skips, exit 0. |
| T074 — convenção Java | A renomeação nominal do teste C12 foi aprovada e aplicada sem mudar o corpo. A inversão temporária da herança de `ParceiroRepository` foi nomeada pelo gate; patch inverso aplicado e `ConvencoesFonteTest` repetiu 9/9 verde, exit 0. |
| T074 — autorização/rotas | A rota de criação `form` foi alterada temporariamente para `novo`; a spec dedicada executou 2 testes e ficou vermelha com 1 falha, mostrando a lista recebida com `novo`. Patch inverso aplicado; repetição 2/2 verde, exit 0. |
| T074 — residual frontend | Após autorização nominal, a entrada obsoleta de `AppFooterComponent` foi removida do baseline. O verificador restaurado passou com 13 regras, 124 arquivos e zero violações; `standalone: true` então reprovou nominalmente com uma violação em `signals-standards.md#1`. Patch inverso aplicado e repetição verde, com 63 ocorrências ainda no baseline e 110 entradas já corrigidas. As sete redes de T074 estão fechadas. |
| T075 — ajuste focal | Expansão nominalmente autorizada para somente `SolicitacaoRollbackIntegrationTest`. Quatro operações SQL e duas extrações de identificador foram alinhadas ao schema C12, sem mudança de produção; gate focal executou 4/4 verde, zero falhas/erros/skips, exit 0. |
| T075 — backend completo | `.\mvnw.cmd clean install` terminou com exit 0/BUILD SUCCESS: `app-security` 16/16, `app-core` 124/124 e `app-deploy` 73/73; total 213, acima do mínimo 204, zero falhas/erros/skips. |
| T076 — frontend completo | `npm ci`, `npm test`, `npm run build` e `npx prettier --check .` terminaram com exit 0. Vitest passou 26/26 arquivos e 123/123 testes; build concluiu com apenas o aviso não bloqueante de budget; Prettier passou integralmente. SSO, paridade das 21 ações e 13 regras sobre 124 arquivos ficaram verdes, com zero violações. |
| Fechamento | `git diff --check` passou. A comparação literal com `sample-c13-anchor-porcelain.txt` encontrou zero caminhos ausentes e somente os cinco deltas autorizados: baseline frontend, dois testes de rollback e os dois documentos C13. |

## 3. Bloqueio resolvido

- **Tarefa:** T075, gate integral do backend.
- **Comando:** `backend/.\mvnw.cmd clean install` terminou com exit 1; `app-security` passou 16/16, `app-core` passou 124/124 e `app-deploy` executou 73 testes, com quatro erros em `SolicitacaoRollbackIntegrationTest`.
- **Causa:** o teste ainda usa o schema anterior (`REGISTRO_AUDITORIA`, `CAMPO_ALTERADO`, `ID_REGISTRO_AUDITORIA`, `CD_CORRELACAO`, `ID_CAMPO_ALTERADO` e `id_registro_afetado`), embora a migration C12 tenha renomeado essas estruturas. As quatro operações SQL falham antes de avaliar a lógica de rollback.
- **Expansão mínima proposta:** incluir apenas `backend/app-deploy/src/test/java/br/com/gems/app/solicitacao/SolicitacaoRollbackIntegrationTest.java` como EDIT e alinhar suas quatro operações SQL e duas extrações de identificador a `AUDIT_OPERATION`, `AUDIT_CHANGE`, `ID_AUDIT_OPERATION`, `CD_CORRELATION`, `ID_AUDIT_CHANGE` e `DS_ENTITY_ID`, comparando UUIDs como texto.
- **Decisão:** expansão nominalmente autorizada pelo responsável em 2026-09-23; somente o teste
  indicado entra na fronteira EDIT e nenhum arquivo de produção será alterado.
- **Resultado:** gate focal e gate integral verdes; o bloqueio não permanece ativo.

## 4. Não iniciado

T077–T079 pertencem a C14/C15 e permanecem fora deste lote.

## 5. Arquivos tocados

`git status --porcelain --untracked-files=all`, relativo à âncora C12; os dois documentos do próprio
protocolo são descontados pelo verificador.

```text
 M backend/app-deploy/src/test/java/br/com/gems/app/parceiro/ParceiroRollbackIntegrationTest.java
 M backend/app-deploy/src/test/java/br/com/gems/app/solicitacao/SolicitacaoRollbackIntegrationTest.java
?? specs/002-conformidade-gems/estado-lote-C13.md
?? specs/002-conformidade-gems/pacote-de-execucao-C13.md
```

## 6. Decisões fora do pacote

Nenhuma. D-P04 continua deliberadamente aberta para C14; ela não bloqueia as mutações nem os gates
completos de C13.

## 7. O que está quebrado agora

Nada está quebrado. Nenhuma mutação temporária permanece ativa; T074–T076 estão verdes.

## 8. Retomada

- **Executor:** Codex
- **Política:** C13 encerrado sem commit nem push; próximo lote C14.
