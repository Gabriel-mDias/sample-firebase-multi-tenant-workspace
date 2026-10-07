# Pacote de execução — rodada 002, C09

> Lista READ fechada. Sem delegação: T008, tokenização e gate de ações compartilham o mesmo
> fechamento frontend/Docker. Não iniciar C10-SDK, gates humanos ou D-P04.

Checkpoint T008 e T070–T073. Âncora: `aedaf47cdcf52f1ab7b920fd1afec1a29cfb3066`.

## 1. Fronteira fechada do lote

C09 conclui o gate de ações, a tokenização visual, os testes focados e o build Docker do frontend.
Não altera `index.html`, não instala com flag de peers e não concede aprovação humana.

### READ

- `AGENTS.md`, `frontend/AGENTS.md`, `environments/AGENTS.md`
- `.specify/memory/constitution.md`
- `.gems-ai/rules/frontend-angular/{visual-standards.md,organization.md}`
- `specs/002-conformidade-gems/{tasks.md,roteiro.md,ancora-porcelain-C08.txt,pacote-de-execucao-C08.md,estado-lote-C08.md}`
- `.github/workflows/build-images.yml`
- `frontend/{Dockerfile,package.json,scripts/verify-authorization-actions.mjs,scripts/verify-conventions.mjs,src/styles.css}`
- `frontend/src/app/{app.config.ts,core/auth/acoes.ts,core/footer/app-footer.component.ts,core/footer/app-footer.component.css}`
- `backend/app-security/src/main/java/br/com/gems/app/security/AcaoSistema.java`

### EDIT / CREATE

| # | Ação | Arquivo |
| :-- | :--- | :--- |
| T008 | EDIT/TEST | `frontend/Dockerfile` — manter `npm ci`, montar a credencial como secret BuildKit e executar o build do stage `build` |
| T008 | EDIT | `.github/workflows/build-images.yml` — fornecer o mesmo secret BuildKit nos builds backend/frontend, sem build arg de credencial |
| T070 | EDIT | `frontend/src/styles.css` — trocar usos literais por tokens GEMS ou `--meduc-*` no `:root` |
| T070 | EDIT | `frontend/src/app/core/footer/app-footer.component.ts` `frontend/src/app/core/footer/app-footer.component.css` — remover residual standalone e tokenizar contraste |
| T071 | EDIT | `frontend/scripts/verify-authorization-actions.mjs` — comparar `Acoes` com `AcaoSistema` real |
| T071 | EDIT | `frontend/package.json` — executar o gate de ações em `test` e `build` |
| C09 | CREATE | `specs/002-conformidade-gems/pacote-de-execucao-C09.md` — contrato fechado do checkpoint |
| C09 | CREATE | `specs/002-conformidade-gems/estado-lote-C09.md` — resultado factual do checkpoint |
| C09 | EDIT | `specs/002-conformidade-gems/tasks.md` — somente após todas as provas verdes |
| C09 | EDIT | `specs/002-conformidade-gems/roteiro.md` — somente após todas as provas verdes |

## 2. O que já existe e você consome

```text
frontend: export const Acoes = { CHAVE: 'CHAVE', ... } as const
backend:  public enum AcaoSistema { CHAVE("descrição"), ... }
```

O sample é single-tenant; não há JSON gerado, escopo tenant nem pacote `meduc` neste contrato.

## 3. Arquivos de referência

| Para escrever | Copie de |
| :--- | :--- |
| Token de cor | `.gems-ai/rules/frontend-angular/visual-standards.md` §2/§2.1 |
| Ação espelhada | `frontend/src/app/core/auth/acoes.ts` e `backend/app-security/src/main/java/br/com/gems/app/security/AcaoSistema.java` |
| Gate de convenções | `frontend/scripts/verify-conventions.mjs` — 13 regras e modo de relatório |

## 4. Regras deste lote

1. `Dockerfile` mantém `npm ci`; só os build args previstos podem fornecer credenciais.
2. Sem credenciais no registry, registrar bloqueio e parar: sem contorno e sem expor segredo.
3. Toda cor literal em uso de `styles.css` vira token GEMS de mesmo papel ou `--meduc-*` declarado no `:root`.
4. Literais que sejam dado, metadado ou comentário e `index.html` não entram no escopo.
5. O footer usa token de contraste; `standalone: true` residual sai.
6. O gate compara conjuntos idênticos, chaves/valores iguais, únicos e no padrão de ação.
7. O gate de ações roda em `npm test` e `npm run build`; convenções permanece com 13 regras.
8. A mutação divergente em `Acoes` deve ficar vermelha e ser restaurada por edição inversa.
9. D-P04 permanece desmarcado; relatório de residual não é aprovação humana.

A fronteira de T008 foi corrigida antes do fechamento: a migração de build arg para secret BuildKit
exige que o workflow forneça o secret nomeado. O arquivo já estava no diff factual do checkpoint;
mantê-lo fora de `EDIT` deixaria pacote e implementação divergentes.

## 5. O portão

Em `frontend/`:

```powershell
node scripts/verify-authorization-actions.mjs
npx ng test --watch=false --include=src/app/app.spec.ts --include=src/app/core/guards/role.guard.spec.ts --include=src/app/core/guards/routes-coverage.spec.ts --include=src/app/core/menu/menu.builder.spec.ts --include=src/app/shared/forms/dados-pessoais/dados-pessoais-fields.component.spec.ts --include=src/app/shared/forms/contato/contato-fields.component.spec.ts
npx ng test --watch=false --include=src/app/features/parceiro/parceiro.routes.spec.ts --include=src/app/features/parceiro/list/parceiro-list.component.spec.ts --include=src/app/features/parceiro/form/parceiro-form.component.spec.ts --include=src/app/features/parceiro/services/parceiro-form-state.service.spec.ts --include=src/app/features/solicitacao/solicitacao.routes.spec.ts --include=src/app/features/solicitacao/list/solicitacao-list.component.spec.ts --include=src/app/features/solicitacao/form/solicitacao-form.component.spec.ts
node scripts/verify-conventions.mjs
npx prettier --check .
docker build --target build .
```

Também executar `npm test`, `npm run build` e `git diff --check` no fechamento. O relatório de
convenções é usado somente para inventariar residual nominal.

## 6. Rollback

Reverter por edição inversa exclusivamente os arquivos da tabela contra `aedaf47`; remover, de modo
nomeado, apenas os dois documentos C09 se necessário. Preservar integralmente os artefatos já
descontados em `ancora-porcelain-C08.txt`; nunca usar reset, clean ou checkout amplo.
