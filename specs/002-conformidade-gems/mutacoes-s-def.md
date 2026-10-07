# Mutações — S-def, T057

2026-09-14. Todas retornaram exit 1 e foram restauradas por edição inversa em finally.

| Alvo real | Resultado observado |
| --- | --- |
| HTML referencia script ausente | silent check-sso rejeita referência |
| Baseline frontend cresce | catraca rejeita total acima de 174 |
| CSS recebe cor #abcdef | convenções rejeitam cor nova |
| Baseline Java recebe nova identidade | catraca rejeita 190 entradas (máximo 189) |
| Checkstyle recebe nova supressão | teste rejeita supressão fora do conjunto congelado |
| Teste Java renomeado invalidTestName | fonte rejeita nome fora de deveX_quandoY |
| Java recebe indentação incorreta | Spotless rejeita formatação |
| Import Java alterado para wildcard | Checkstyle rejeita AvoidStarImport |
| Exceção vencida com regra missing.md#1 | exceções rejeitam regra inexistente, vencimento, orfandade e inventário divergente |

[Resultados dos comandos](mutacoes-s-def.json). Nas mutações de fonte/Checkstyle/exceção,
Spotless foi desligado somente na linha de comando para isolar a garantia alvo; no gate final
todas as verificações rodam em modo reprovação. Nenhuma mutação sobrevive.
