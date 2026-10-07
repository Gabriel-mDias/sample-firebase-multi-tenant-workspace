# Estado — C08, rodada 002

Data: 2026-09-20. Estado: **verde**. Âncora inicial:
`030a65ee313c3254941aab822f92cfc65c661ac7`.

## 1. Rollback deste lote

Reverter por edição inversa somente os caminhos da tabela da §1 do pacote C08 contra a âncora.
Preservar integralmente os artefatos descontados por `ancora-porcelain-C08.txt`.

## 2. Concluído

| Tarefa | O que provou |
| :-- | :--- |
| T066 | Oito rotas guardadas, redirects/menu alinhados; mutação de path vermelha/restaurada. |
| T067 | Equivalência local/SDK 6/6 antes da remoção; consumidores na SDK. |
| T068 | Dez arquivos em `shared/`, zero `commons/`; 4 specs/32 testes verdes. |
| T069 | Seis componentes na navegação SDK; mutação de URL vermelha/restaurada. |

## 3. Em curso

- **Tarefa:** nenhuma.
- **Arquivo:** nenhum.
- **O que já está feito nele:** checkpoint concluído.
- **O que falta:** somente registrar SHA depois do commit do checkpoint.

## 4. Não iniciado

Nenhuma tarefa do pacote C08.

## 5. Arquivos tocados

Saída literal de `git status --porcelain --untracked-files=all`, menos este arquivo:

```text
 M .specify/scripts/powershell/verificar-lote.ps1
 M frontend/AGENTS.md
 M frontend/src/app/app.routes.ts
 M frontend/src/app/app.spec.ts
 D frontend/src/app/commons/data-br.spec.ts
 D frontend/src/app/commons/data-br.ts
 D frontend/src/app/commons/gems-table-labels.ts
 D frontend/src/app/commons/moeda-br.spec.ts
 D frontend/src/app/commons/moeda-br.ts
 D frontend/src/app/commons/page-response.ts
 D frontend/src/app/commons/password.validator.spec.ts
 D frontend/src/app/commons/password.validator.ts
 D frontend/src/app/commons/table-sort.spec.ts
 D frontend/src/app/commons/table-sort.ts
 M frontend/src/app/core/guards/routes-coverage.spec.ts
 M frontend/src/app/core/menu/menu.builder.spec.ts
 M frontend/src/app/core/menu/menu.builder.ts
 M frontend/src/app/features/acesso-negado/acesso-negado.component.html
 M frontend/src/app/features/acesso-negado/acesso-negado.component.ts
 M frontend/src/app/features/parceiro/form/parceiro-form.component.ts
 M frontend/src/app/features/parceiro/form/steps/step-dados/step-dados.component.html
 M frontend/src/app/features/parceiro/form/steps/step-dados/step-dados.component.ts
 M frontend/src/app/features/parceiro/form/steps/step-endereco/step-endereco.component.html
 M frontend/src/app/features/parceiro/form/steps/step-endereco/step-endereco.component.ts
 M frontend/src/app/features/parceiro/list/parceiro-list.component.spec.ts
 M frontend/src/app/features/parceiro/list/parceiro-list.component.ts
 M frontend/src/app/features/parceiro/parceiro.routes.ts
 M frontend/src/app/features/parceiro/services/parceiro.store.ts
 M frontend/src/app/features/solicitacao/form/solicitacao-form.component.html
 M frontend/src/app/features/solicitacao/form/solicitacao-form.component.ts
 M frontend/src/app/features/solicitacao/list/solicitacao-list.component.spec.ts
 M frontend/src/app/features/solicitacao/list/solicitacao-list.component.ts
 M frontend/src/app/features/solicitacao/services/solicitacao.store.ts
 M frontend/src/app/features/solicitacao/solicitacao.routes.ts
 M frontend/src/app/shared/auditoria/auditoria.store.ts
 M frontend/src/app/shared/auditoria/historico-registro/historico-registro.component.spec.ts
 M frontend/src/app/shared/auditoria/historico-registro/historico-registro.component.ts
 M frontend/src/app/shared/forms/contato/contato-fields.component.html
 M frontend/src/app/shared/forms/contato/contato-fields.component.ts
 M frontend/src/app/shared/forms/dados-pessoais/dados-pessoais-fields.component.html
 M frontend/src/app/shared/forms/dados-pessoais/dados-pessoais-fields.component.ts
 D frontend/src/app/shared/forms/field-error/field-error.component.css
 D frontend/src/app/shared/forms/field-error/field-error.component.html
 D frontend/src/app/shared/forms/field-error/field-error.component.spec.ts
 D frontend/src/app/shared/forms/field-error/field-error.component.ts
 M specs/002-conformidade-gems/plan.md
 M specs/002-conformidade-gems/research.md
 M specs/002-conformidade-gems/roteiro.md
 M specs/002-conformidade-gems/spec.md
 M specs/002-conformidade-gems/tasks.md
?? frontend/src/app/features/parceiro/parceiro.routes.spec.ts
?? frontend/src/app/features/solicitacao/solicitacao.routes.spec.ts
?? frontend/src/app/shared/models/gems-table-labels.ts
?? frontend/src/app/shared/models/page-response.ts
?? frontend/src/app/shared/utils/data-br.spec.ts
?? frontend/src/app/shared/utils/data-br.ts
?? frontend/src/app/shared/utils/moeda-br.spec.ts
?? frontend/src/app/shared/utils/moeda-br.ts
?? frontend/src/app/shared/utils/table-sort.spec.ts
?? frontend/src/app/shared/utils/table-sort.ts
?? frontend/src/app/shared/validators/password.validator.spec.ts
?? frontend/src/app/shared/validators/password.validator.ts
?? specs/002-conformidade-gems/ancora-porcelain-C08.txt
?? specs/002-conformidade-gems/baseline-codex-20260916.md
?? specs/002-conformidade-gems/pacote-de-execucao-C07.md
?? specs/002-conformidade-gems/pacote-de-execucao-C08.md
```

## 6. Decisões fora do pacote

O movimento para `shared/` colocou quatro specs antes excluídos sob `testing.md#7`; 32 identidades
foram corrigidas sem alterar corpos. D-P04 permanece intocada.

## 7. O que está quebrado agora

Nada. Últimos gates: T072 6/30; T073 7/18; movidos 4/32; convenções 13/124/0.

## 8. Retomada

- **Executor:** Codex
- **Token:** não aplicável; C08 fechado. C09 exige pacote novo.
