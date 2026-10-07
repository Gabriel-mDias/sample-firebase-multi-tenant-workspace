# Documentação do projeto — o funil de entrada dos requisitos

Aqui vive tudo o que existe **antes** de `specs/`. O spec-kit começa numa feature já recortada; esta
pasta é o caminho de uma conversa com o cliente até esse recorte.

> **Esta hierarquia está vazia de propósito.** Cada nível traz um `README.md` explicando o papel dele
> e um template do documento que ele guarda. Preencha na ordem — pular um nível é o que produz uma
> rodada aberta sem definição, que custa mais que a rodada que não abriu.

## Os quatro níveis

```
1. levantamento previo de requisitos/     o que o cliente disse, cru
        │                                  entrevistas, desenhos, listas de entidades
        ▼
2. especificacoes_spec_driven/            o que isso vira, organizado por requisito
        │                                  RF001, RF002, … agrupados em ondas
        ▼
3. plano-onda-N/                          a ordem de execução de uma onda
        │                                  sequenciamento, dependências, portões humanos
        ▼
4. specs/0NN-<slug>/                      a rodada, já sob o spec-kit
                                           spec → clarify → plan → tasks → analyze → roteiro
```

| Nível | Pergunta que ele responde | Quem escreve |
| :--- | :--- | :--- |
| **1. Levantamento** | O que o cliente faz hoje, e o que dói? | analista, com o cliente |
| **2. Especificações** | Que requisitos numerados saem disso, e em que ordem de valor? | analista |
| **3. Plano de onda** | Quais requisitos entram na próxima leva, e o que bloqueia o quê? | product owner + orquestrador |
| **4. `specs/`** | Como esta rodada é construída, testada e aprovada? | a skill `/rodada-sdd` |

## Por que os níveis 1 e 2 não são `specs/`

Porque a numeração é diferente e o público é diferente.

- **`RF001` é o requisito**, e a numeração dele é a ordem em que os requisitos foram levantados.
- **`specs/001-*` é a rodada**, e a numeração dela é a ordem de **entrega**.

Os dois números divergem já na segunda onda, e é normal que divirjam: uma rodada pode entregar dois
requisitos, e um requisito pode se partir em duas rodadas. Confundi-los é o erro mais comum de quem
chega — `specs/AGENTS.md` diz isso em uma linha, e este parágrafo é o porquê.

## O que NÃO vive aqui

- **Documento de negócio para o cliente** — vive em `business/`, escrito em linguagem de negócio, com
  as telas reais depois da entrega.
- **Decisão técnica** — vive no `research.md` da rodada que a tomou.
- **Convenção de código** — vive nos `AGENTS.md` e em `.gems-ai/rules/`.

## Quando o levantamento não responde

A fase 1 da `/rodada-sdd` procura aqui em cinco níveis antes de abrir uma rodada. Se não encontrar, a
instrução dela é **reportar e parar**, com a lista de perguntas de negócio que faltam. Uma rodada
aberta sobre suposição vira código escrito sobre suposição, e a suposição só aparece na homologação.
