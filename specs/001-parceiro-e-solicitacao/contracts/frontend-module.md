# Contrato do módulo de frontend — rodada 001

## Rotas registradas na raiz

| Caminho | Feature | Registro |
| :--- | :--- | :--- |
| `` | `home` | `loadChildren` |
| `parceiros` | `parceiro` | `loadChildren` |
| `solicitacoes` | `solicitacao` | `loadChildren` |
| `acesso-negado` | — | `loadComponent`, **sem guard** |
| `**` | — | redireciona para a home |

A raiz não conhece nenhum componente interno de feature.

## Rotas de cada feature

| Caminho | Ação exigida | `data.mode` |
| :--- | :--- | :--- |
| `/parceiros` | `CONSULTAR_PARCEIRO` | — |
| `/parceiros/novo` | `INSERIR_PARCEIRO` | `create` |
| `/parceiros/:id/editar` | `ALTERAR_PARCEIRO` | `edit` |
| `/parceiros/:id` | `CONSULTAR_PARCEIRO` | `view` |
| `/solicitacoes` | `CONSULTAR_SOLICITACAO` | — |
| `/solicitacoes/novo` | `INSERIR_SOLICITACAO` | `create` |
| `/solicitacoes/:id/editar` | `ALTERAR_SOLICITACAO` | `edit` |
| `/solicitacoes/:id` | `CONSULTAR_SOLICITACAO` | `view` |

**A ordem importa**: `novo` e `:id/editar` vêm antes de `:id`, ou o parâmetro engole as rotas
literais.

## Stores

| Store | Path base | Observação |
| :--- | :--- | :--- |
| `ParceiroStore` | `api/parceiros` | Sem barra inicial — o `GemsBaseStore` concatena |
| `SolicitacaoStore` | `api/solicitacoes` | Uma chamada por transição; **não existe** método que grave a situação |
| `LocalidadeStore` | `api/localidades` | `listarEstados` exige `paisId` |

Componentes nunca usam `HttpClient` direto.

## Itens de menu

| Item | Ação | Rota |
| :--- | :--- | :--- |
| Parceiros | `CONSULTAR_PARCEIRO` | `/parceiros` |
| Solicitações | `CONSULTAR_SOLICITACAO` | `/solicitacoes` |
| Auditoria | `CONSULTAR_AUDITORIA` | `/auditoria` |

Cada item depende da ação de **consulta**. A home não tem entrada. Sem nenhuma dessas ações, o menu
fica vazio — e isso é o comportamento correto.

## Estado do wizard

`ParceiroFormStateService` é fornecido **no orquestrador**, não em `root`: cada abertura do
formulário começa limpa. Os passos leem e escrevem só nele; nenhum passo chama a store.

**O campo `podeVerComissao` mora no estado**, e não é recalculado a cada leitura: o passo de resumo e
o `toPayload` precisam decidir pelo mesmo valor, ou o payload sai diferente do que a tela mostrou.

## Espelho de autorização

`core/auth/acoes.ts` é espelho **exato** de `AcaoSistema.java`. Não há ferramenta que ligue os dois:
a sincronia é disciplina, e a divergência quebra de forma barulhenta — a tela some ou o acesso é
negado.