# Estado — C14, rodada 002

Data: 2026-09-23. Estado inicial: **aberto**. Estado atual: **concluído 2/2 após C14-R1**.
Âncora: `evidencias/sample-c14-anchor-porcelain.txt` no ledger de controle.

## 1. Entrada

- C13 concluído 3/3, sem mutação ativa, commit ou push.
- Backend completo verde com 213 testes; frontend completo verde com 26 arquivos/123 testes, build
  e Prettier.
- Branch `feature/conformidade-gems`, HEAD `098a9f5`, upstream `0/0`.

## 2. Concluído

T077 foi executado e registrado em `quickstart.md`: upgrade 32→33 e reinício idempotente; três
compose files válidos; one-shot 2/2; bootstrap real 21 ações, 21→0 filhos e 3/3 grupos; gate de
oito rotas 9/9. Os contêineres/rede criados pelo ensaio foram removidos sem `-v`; volumes preservados.

## 3. Inventário T078 / D-P04

A desativação temporária do baseline revelou 63 ocorrências; a restauração voltou a 13 regras,
124 arquivos e zero violações. Inventário mínimo:

| Regra | Ocorrências | Arquivos | Alternativa de correção |
| :--- | ---: | :--- | :--- |
| `testing.md#7` | 57 | 13 specs: rotas (1), localidade (2), estado de parceiro (8), histórico (8), tooltip (2), contato (2), dados pessoais (8), CEP (7), Enter (5), perfil (4), RG (10) | Renomear somente os títulos `it(...)` ao padrão `deveX_quandoY`, sem alterar os corpos. |
| `visual-standards.md#2` | 2 | `features/acesso-negado/acesso-negado.component.css`, mesma linha de `box-shadow` | Substituir os dois `rgba(...)` por token `--gems-*` equivalente ou um único `--app-*` em `styles.css`. |
| `organization.md#1` | 1 | `features/solicitacao/form/solicitacao-form.component.ts` | Mover o contrato compartilhado hoje importado de `features/parceiro` para `shared/` ou `core/`. |
| `organization.md#6` | 3 | `parceiro-form-state.service.ts`, sua spec e `parceiro-response.model.ts` | Mover o state service para `form/` conforme a regra e unificar o model sob o prefixo `parceiro`. |

Decisão recebida: **devolver as 63 ocorrências para correção**. O sublote C14-R1 eliminou 63/63,
zerou o baseline e passou os gates completos do frontend. D-P04 está resolvida com residual zero.

## 4. O que está quebrado agora

Nada funcional conhecido; os gates estão verdes sem baseline frontend.

## 5. Retomada

- **Executor:** Codex
- **Próxima ação:** C15/analyze final.
- **Política:** não commitar, fazer push ou editar o Meduc.
