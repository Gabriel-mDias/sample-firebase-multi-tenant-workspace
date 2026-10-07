# Pacote de execução — rodada 002, C13

> Lista `READ` fechada. Sem delegação: o próprio Codex conduz o fechamento técnico. C11/C12
> permanecem intactos na árvore. Não iniciar quickstart, D-P04, analyze final, commit ou push.

Checkpoint T074–T076. Âncora: `evidencias/sample-c13-anchor-porcelain.txt` no ledger de controle.
Gate: mutações restauradas e gates completos.

## 1. Fronteira fechada do lote

C13 revalida as redes de segurança acumuladas e executa uma única vez os gates completos de backend
e frontend. As mutações são temporárias, uma por vez, e precisam ser restauradas por edição inversa
antes da próxima. Nenhuma alteração produtiva permanente é autorizada neste lote.

| # | Ação | Arquivo |
| :-- | :--- | :--- |
| C13 | CREATE | `specs/002-conformidade-gems/pacote-de-execucao-C13.md` |
| C13 | CREATE | `specs/002-conformidade-gems/estado-lote-C13.md` |
| T074 | TEMP/RESTORE | `backend/app-security/src/main/java/br/com/gems/app/security/AcaoSistema.java` |
| T074 | TEMP/RESTORE | `backend/app-core/src/main/java/br/com/gems/app/parceiro/controller/ParceiroController.java` |
| T074 | TEMP/RESTORE | `backend/app-deploy/src/main/resources/application.yml` |
| T074 | TEMP/RESTORE | `backend/app-core/src/main/java/br/com/gems/app/parceiro/service/ParceiroService.java` |
| T074 | EDIT | `backend/app-deploy/src/test/java/br/com/gems/app/parceiro/ParceiroRollbackIntegrationTest.java` |
| T074 | EDIT | `backend/app-deploy/src/test/java/br/com/gems/app/auditoria/AuditoriaSchemaMigrationIntegrationTest.java` |
| T074 | TEMP/RESTORE | `backend/app-core/src/main/java/br/com/gems/app/parceiro/repository/ParceiroRepository.java` |
| T074 | TEMP/RESTORE | `frontend/src/app/features/parceiro/parceiro.routes.ts` |
| T074 | TEMP/RESTORE | `frontend/src/app/core/footer/app-footer.component.ts` |
| T074 | EDIT | `.gems-ai/conformance-baseline.json` |
| T075 | EDIT | `backend/app-deploy/src/test/java/br/com/gems/app/solicitacao/SolicitacaoRollbackIntegrationTest.java` |
| T074–T076 | EDIT | `specs/002-conformidade-gems/tasks.md` |
| T074–T076 | EDIT | `specs/002-conformidade-gems/roteiro.md` |

### READ

- `AGENTS.md`, `backend/AGENTS.md`, `frontend/AGENTS.md`, `specs/AGENTS.md`
- `.specify/memory/constitution.md`
- `.gems-ai/rules/backend-java/{repositories.md,security.md,testing.md}`
- `.gems-ai/rules/frontend-angular/{organization.md,testing.md}`
- `specs/002-conformidade-gems/{tasks.md,roteiro.md,estado-lote-C12.md}`
- os sete alvos temporários da tabela e os testes/scripts diretamente chamados abaixo

## 2. Mutações T074

Executar separadamente e exigir vermelho antes da restauração:

1. catálogo: devolver escopo `TENANT` em `AcaoSistema`; rodar `AcaoSistemaCatalogTest`;
2. endpoint: remover uma classificação SDK de `ParceiroController`; rodar `CoberturaPreAuthorizeTest`;
3. auditoria: desativar colunas de contexto em `application.yml`; rodar os testes integrados de auditoria;
4. rollback: remover `rollbackFor = Exception.class` de `ParceiroService`; rodar `ParceiroRollbackIntegrationTest`;
5. zero Java: introduzir violação nominal mínima em `ParceiroRepository`; rodar `ConvencoesFonteTest`;
6. autorização/rotas: trocar temporariamente a rota `form` por `novo`; rodar a spec de rotas de parceiro;
7. residual frontend: reintroduzir `standalone: true` no footer; rodar `verify-conventions.mjs`.

Depois de cada vermelho, restaurar pelo patch inverso e repetir o gate focal verde. Não usar reset,
checkout amplo ou clean. Registrar comandos, exit codes e contagens no estado.

Expansão aprovada nominalmente em 2026-09-23: alinhar somente os quatro SQLs de
`ParceiroRollbackIntegrationTest` aos nomes e tipos do schema SDK implantado no C12 antes de repetir
a rede 4. A correção do teste é permanente; a remoção de `rollbackFor` continua temporária.

Segunda expansão aprovada nominalmente em 2026-09-23: renomear somente o método
`deveManterRollbackNominalNoChangesetDeMigracao` ao padrão `deveX_quandoY`, sem alterar seu corpo,
para remover a violação C12 revelada pelo gate da rede 5.

Terceira expansão aprovada nominalmente em 2026-09-23: alinhar somente as quatro operações SQL e
as duas extrações de identificador de `SolicitacaoRollbackIntegrationTest` ao schema de auditoria
C12. Nenhum arquivo de produção entra nessa expansão.

## 3. Gates completos T075/T076

```powershell
cd backend
.\mvnw.cmd clean install

cd ..\frontend
npm ci
npm test
npm run build
npx prettier --check .
```

T075 exige todos os módulos verdes e pelo menos 204 testes aplicáveis. T076 exige pelo menos 21
arquivos/118 testes, build e Prettier verdes. Fechar também com `git diff --check` e o verificador de
lote usando `estado-lote-C12.md` como âncora.

## 4. Rollback

As mutações são revertidas imediatamente por edição inversa. Se o lote parar entre mutação e
restauração, o estado deve nomear o arquivo e o trecho ainda mutado. Os únicos CREATE permanentes do
lote são os dois documentos C13; não tocar nos artefatos anteriores.
