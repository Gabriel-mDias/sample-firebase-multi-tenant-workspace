# Pacote de execução — rodada 002, C08

> Lista READ fechada. Sessão `nova`. Sem delegação: rotas, imports, componentes e testes formam
> um grafo acoplado cujo particionamento custa mais que a execução serial.

Checkpoint T066–T069. Pré-requisito: C07 verde em
`030a65ee313c3254941aab822f92cfc65c661ac7`.

## 1. Fronteira fechada do lote

C08 entrega rotas, `shared/`, field-error SDK e navegação. Não executa cores, residual, Docker,
auditoria nem Keycloak.

### READ

- `AGENTS.md`, `frontend/AGENTS.md`
- `.gems-ai/rules/frontend-angular/organization.md`
- `.gems-ai/rules/frontend-angular/forms.md`
- `.gems-ai/rules/frontend-angular/gems-components.md` §5
- `.gems-ai/rules/frontend-angular/testing.md` §7
- `specs/002-conformidade-gems/{spec.md,plan.md,tasks.md,roteiro.md}` somente FR-025–FR-027,
  SC-008 e T066–T069
- arquivos da tabela abaixo; `frontend/src/app/app.ts` somente para confirmar que seu `Router`
  observa `NavigationEnd`

### EDIT / CREATE / DELETE

| # | Ação | Arquivo |
| :-- | :--- | :--- |
| T066 | EDIT | `frontend/src/app/app.routes.ts` `frontend/src/app/app.spec.ts` `frontend/src/app/core/guards/routes-coverage.spec.ts` `frontend/src/app/core/menu/menu.builder.ts` `frontend/src/app/core/menu/menu.builder.spec.ts` |
| T066 | EDIT | `frontend/src/app/features/parceiro/parceiro.routes.ts` `frontend/src/app/features/solicitacao/solicitacao.routes.ts` |
| T066 | CREATE | `frontend/src/app/features/parceiro/parceiro.routes.spec.ts` `frontend/src/app/features/solicitacao/solicitacao.routes.spec.ts` |
| T067 | EDIT | `frontend/src/app/shared/forms/dados-pessoais/dados-pessoais-fields.component.ts` `frontend/src/app/shared/forms/dados-pessoais/dados-pessoais-fields.component.html` `frontend/src/app/shared/forms/contato/contato-fields.component.ts` `frontend/src/app/shared/forms/contato/contato-fields.component.html` |
| T067 | EDIT | `frontend/src/app/features/solicitacao/form/solicitacao-form.component.ts` `frontend/src/app/features/solicitacao/form/solicitacao-form.component.html` `frontend/src/app/features/parceiro/form/steps/step-dados/step-dados.component.ts` `frontend/src/app/features/parceiro/form/steps/step-dados/step-dados.component.html` `frontend/src/app/features/parceiro/form/steps/step-endereco/step-endereco.component.ts` `frontend/src/app/features/parceiro/form/steps/step-endereco/step-endereco.component.html` |
| T067 | DELETE | `frontend/src/app/shared/forms/field-error/field-error.component.ts` `frontend/src/app/shared/forms/field-error/field-error.component.html` `frontend/src/app/shared/forms/field-error/field-error.component.css` `frontend/src/app/shared/forms/field-error/field-error.component.spec.ts` |
| T067 | EDIT | `frontend/AGENTS.md` |
| T068 | DELETE | `frontend/src/app/commons/data-br.ts` `frontend/src/app/commons/data-br.spec.ts` `frontend/src/app/commons/moeda-br.ts` `frontend/src/app/commons/moeda-br.spec.ts` `frontend/src/app/commons/page-response.ts` `frontend/src/app/commons/password.validator.ts` `frontend/src/app/commons/password.validator.spec.ts` `frontend/src/app/commons/table-sort.ts` `frontend/src/app/commons/table-sort.spec.ts` `frontend/src/app/commons/gems-table-labels.ts` |
| T068 | CREATE | `frontend/src/app/shared/utils/data-br.ts` `frontend/src/app/shared/utils/data-br.spec.ts` `frontend/src/app/shared/utils/moeda-br.ts` `frontend/src/app/shared/utils/moeda-br.spec.ts` `frontend/src/app/shared/utils/table-sort.ts` `frontend/src/app/shared/utils/table-sort.spec.ts` |
| T068 | CREATE | `frontend/src/app/shared/models/page-response.ts` `frontend/src/app/shared/models/gems-table-labels.ts` `frontend/src/app/shared/validators/password.validator.ts` `frontend/src/app/shared/validators/password.validator.spec.ts` |
| T068 | EDIT | `frontend/src/app/shared/auditoria/historico-registro/historico-registro.component.ts` `frontend/src/app/shared/auditoria/historico-registro/historico-registro.component.spec.ts` `frontend/src/app/shared/auditoria/auditoria.store.ts` `frontend/src/app/features/parceiro/services/parceiro.store.ts` `frontend/src/app/features/solicitacao/services/solicitacao.store.ts` |
| T068 | EDIT | `frontend/src/app/features/parceiro/list/parceiro-list.component.ts` `frontend/src/app/features/solicitacao/list/solicitacao-list.component.ts` |
| T069 | EDIT | `frontend/src/app/features/acesso-negado/acesso-negado.component.ts` `frontend/src/app/features/acesso-negado/acesso-negado.component.html` |
| T069 | EDIT | `frontend/src/app/features/parceiro/form/parceiro-form.component.ts` `frontend/src/app/features/parceiro/list/parceiro-list.component.ts` `frontend/src/app/features/parceiro/list/parceiro-list.component.spec.ts` `frontend/src/app/features/parceiro/form/steps/step-dados/step-dados.component.ts` |
| T069 | EDIT | `frontend/src/app/features/solicitacao/form/solicitacao-form.component.ts` `frontend/src/app/features/solicitacao/list/solicitacao-list.component.ts` `frontend/src/app/features/solicitacao/list/solicitacao-list.component.spec.ts` |
| C08 | CREATE | `specs/002-conformidade-gems/ancora-porcelain-C08.txt` |
| C08 | EDIT | `specs/002-conformidade-gems/roteiro.md` — registro factual do fechamento |

`tasks.md`, `roteiro.md`, este pacote e `estado-lote-C08.md` são registros do checkpoint; já
estavam na fronteira documental/porcelain inicial ou são a exceção de estado do verificador.

## 2. O que já existe e você consome

- SDK Angular 2.1.0: `GemsFieldErrorComponent` e `GemsNavigationService`.
- `roleGuard` falha fechado e `routes-coverage.spec.ts` resolve `loadChildren` reais.
- `ParceiroFormStateService` conserva domínio em signals no provider do formulário.

## 3. Arquivos de referência

| Para escrever | Copie de |
| :--- | :--- |
| Rotas | `.gems-ai/rules/frontend-angular/organization.md` §1/§3 |
| Campo de erro | `.gems-ai/rules/frontend-angular/gems-components.md` §5 |
| Teste | `.gems-ai/rules/frontend-angular/testing.md` §6/§7 |

## 4. Regras deste lote

1. Rotas: `list`, `form`, `form/:id`, `view/:id`; base redireciona para `list`.
2. Menu e toda navegação programática mudam junto; componentes usam `GemsNavigationService`.
3. `app.ts` pode manter `Router` apenas para observar eventos, não para navegar.
4. Compartilhado vive em `shared/`; `commons/` termina vazio.
5. A cópia de field-error sai somente depois da equivalência local/SDK verde.
6. Nenhum domínio via session/local storage ou navigation state.
7. Testes seguem `deveX_quandoY`; mover arquivo para alcance do gate torna a regra aplicável.
8. Rede nova é mutada, observada vermelha e restaurada por edição inversa.

## 5. O portão

Em `frontend/`: equivalência pré-remoção; T072; T073; quatro specs movidos; verificadores
silent-check/convenções; buscas nominais; Prettier; `git diff --check`; verificador do lote.

Esperado: 6/6 de equivalência; T072 6 arquivos/30; T073 7/18; movidos 4/32; convenções zero;
duas mutações vermelhas/restauradas.

## 6. Rollback

Edição inversa somente dos caminhos da tabela contra
`030a65ee313c3254941aab822f92cfc65c661ac7`; preservar todo artefato anterior da âncora porcelain.
