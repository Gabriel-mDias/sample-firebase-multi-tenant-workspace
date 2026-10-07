# 3. Plano da onda 1

A ordem de execução de uma leva de requisitos: o que entra, o que bloqueia o quê, e onde estão os
portões humanos.

Este arquivo é lido pela **fase 1 da `/rodada-sdd`**, que procura aqui a próxima rodada a abrir. A
tabela "Estado das rodadas" abaixo é o que ela lê — mantenha-a viva.

## O grafo

```
[desenhe as dependências entre as rodadas desta onda]
```

Cada seta é uma dependência de **dado**, não de conveniência. Descreva o porquê de cada uma: é o que
permite discutir a fila em vez de aceitá-la.

## Estado das rodadas

| Rodada | O que entrega | Tarefas | Ciclo | ⛔ aberto |
| :--- | :--- | ---: | :--- | :--- |
| [`0NN`](../../../specs/0NN-slug/) | | | `rascunho` \| `em curso` \| `✅ CONCLUÍDA e homologada em <data>` | |

## Os outros arquivos desta pasta

| Arquivo | Para quê |
| :--- | :--- |
| `00-briefing-para-agentes.md` | O que esta onda já aprendeu do jeito caro. Curto, e lido antes de implementar |
| `01-sequenciamento.md` | O detalhe do grafo, e o recorte de lotes de cada rodada |
| `09-pendencias-abertas.md` | O que a onda deixou frouxo, e em que rodada fecha |

## Portões humanos da onda

Nenhum é dispensável — todos vêm do Princípio VIII.

| # | Portão | Quando | Estado |
| :-- | :--- | :--- | :--- |
| H1 | §12 do `roteiro.md` da rodada X aprovada | antes do primeiro commit de código | |
| H2 | Homologação da rodada X por uso real | ao fim dela | |