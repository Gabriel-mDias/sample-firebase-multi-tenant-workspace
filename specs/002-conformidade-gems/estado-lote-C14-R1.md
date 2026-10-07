# Estado — C14-R1, rodada 002

Data: 2026-09-23. Estado: **concluído**.

## Decisão e correção

D-P04 foi nominalmente **devolvido para correção**. As 63 ocorrências foram eliminadas: 57
títulos de teste foram renomeados sem alterar corpos; duas cores usam o token global existente; o
acoplamento entre features foi substituído por `ParceiroLookupStore`; o state service passou para
`form/parceiro-state.service.ts`; e `ParceiroResponseModel` foi incorporado ao model canônico.

O baseline terminou com `entradas: []`. Nenhuma exceção GEMS foi criada.

## Provas

- Convenções: 13 regras, 126 arquivos, 0 violações, 0 baseline.
- `npm ci`: exit 0, 392 pacotes, 0 vulnerabilidades.
- `npm test`: 27 arquivos/125 testes, zero falhas.
- `npm run build`: exit 0; apenas o aviso conhecido de budget em 735,79 kB.
- `npx prettier --check .` e `git diff --check`: verdes.
- Backend não repetido; C13 permanece válido com 213 testes.

## Fechamento

T078 está concluída e C14 fecha 2/2. Nenhum commit ou push foi criado. O próximo lote é C15,
restrito ao analyze final e à identificação do próximo portão humano.
