# Estado — Lote S, rodada 002

| Controle | Valor |
| :--- | :--- |
| Checkpoint | `C07 — T062–T065, autenticação e providers frontend` |
| Pré-requisitos | `C06 verde em b807233` |
| Política de sessão | `nova` |
| SHA anterior | `b807233` |
| Estado inicial | `aberto` |
| Estado final | `verde` |
| Gate focado | `7 arquivos / 20 testes verdes; npm test 123/123; build 726,67 kB; 0 hasRole/hasAnyRole; mutação restaurada` |
| Ponto de recuperação | `C07 fechado; próxima retomada C08 por pacote próprio e sessão nova.` |

## 1. Rollback deste lote

Partir de `b807233` e restaurar somente o delta dos caminhos T062–T065 por edição inversa. Não usar
checkout sobre a árvore atual: os documentos SDD e os pacotes C07–C15 já estavam modificados antes
desta retomada e devem ser preservados.

## 2. Concluído

| Tarefa | O que provou |
| :-- | :--- |
| T062 | Seis specs de guard/menu/componentes exercitam `isAuthorized`; mutação renomeando o método produziu erros TS2339/TS2345/TS2353 e foi restaurada por patch inverso. |
| T063 | Fachada, requisito global explícito, guard fail-closed, menu e botão de histórico migrados para `isAuthorized`. |
| T064 | Consumidores de parceiro/solicitação migrados; busca nominal retornou zero `hasRole`/`hasAnyRole` nos 12 alvos. |
| T065 | `provideGemsKeycloak`, `withInterceptorsFromDi` e silent check-sso já estavam conformes em `b807233`; `app.spec.ts` passou a provar as opções preservadas. Nenhuma alteração gratuita em `app.config.ts`/`keycloak.init.ts`. |

## 3. Em curso

Nenhuma tarefa em curso. C07 está fechado verde.

## 4. Não iniciado

C08 e posteriores não foram iniciados nesta sessão.

## 5. Arquivos tocados

`git status --porcelain --untracked-files=all`

O delta C07 está restrito a `frontend/src/app/app.spec.ts`, aos cinco alvos de T063, aos cinco alvos
de T064, aos dois specs core de T062 e aos quatro specs de componente criados por T062. Os demais
arquivos modificados/untracked da árvore já existiam antes desta retomada e foram preservados.

## 6. Decisões fora do pacote

O product owner autorizou em 2026-09-20 antecipar exclusivamente a atualização do teste de
`field-error` de T067 e, depois, a formatação de `frontend/scripts/verify-conventions.mjs` de T071.
A segunda regravação não gerou diferença material. A conformidade preexistente de T065 foi provada
por teste em vez de gerar mudança sem efeito em configuração já correta.

## 7. O que está quebrado agora

Nada quebrado pelo C07. `npm test` passou com 123/123, `npm run build` passou com bundle inicial de
726,67 kB e apenas o aviso de budget conhecido, `npx prettier --check .` passou e o verificador
mediu 13 regras em 126 arquivos com zero violações.

## 8. Retomada

- **Executor:** Codex
- **Política:** sessão `nova`; o identificador legado é apenas diagnóstico e não foi reutilizado.
- **Próximo passo:** C08 em sessão nova, pelo pacote `pacote-de-execucao-C08.md`.
