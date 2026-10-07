# Fase 1 — Start

**Objetivo:** encontrar as definições da próxima rodada, decidir se elas se sustentam, e resolver
com o usuário toda ambiguidade antes que ela vire código.

**Termina quando:** as decisões de entrada estão fechadas e registradas. Nenhum artefato do ciclo
spec-kit é escrito aqui — isso é fase 2 e 3.

---

## 1. Procurar

Nesta ordem. Pare no primeiro nível que responda, mas leia o seguinte se ele acrescentar contexto:

| # | Onde | O que esperar |
| :-- | :--- | :--- |
| 1 | `utils/documentação do projeto/plano-onda-N/README.md` | a tabela "Estado das rodadas": o quê, quantas tarefas, ciclo, e a coluna **⛔ aberto** |
| 2 | `specs/0NN-<slug>/` | os artefatos já escritos — podem existir em **rascunho**, escritos antes do ciclo |
| 3 | `utils/documentação do projeto/especificacoes_spec_driven/onda-N/RF0NN-*.md` | a especificação preliminar do requisito de origem |
| 4 | `utils/documentação do projeto/especificacoes_spec_driven/000-*.md` | os pontos de drift catalogados e os déficits técnicos |
| 5 | `business/RF0NN-*.md` | o desenho em linguagem de negócio, escrito para o cliente |

⚠️ **A numeração de `specs/` não é a numeração do RF.** `specs/` numera ordem de entrega. Confirme o
par (specs, RF) antes de citar qualquer um dos dois.

### Se não encontrar

Reporte, e pare. O relatório precisa dizer:

- **o que procurou**, com os caminhos exatos;
- **o que existe e é insuficiente** — um requisito citado no plano da onda mas sem documento, por
  exemplo;
- **o que falta levantar**, em forma de perguntas de negócio que o usuário possa levar ao cliente.

Não invente escopo, não deduza requisito de código existente, e não abra `specs/` para a rodada.
Uma rodada aberta sem definição custa mais que uma rodada que não abriu.

---

## 2. Criticar antes de aceitar

Encontrando, o trabalho não é aceitar — é **conferir**. Cinco eixos, nesta ordem:

### 2.1 Cobertura do requisito de origem

Toda linha do RF preliminar (FRs, user stories, questões abertas `RF0NN-NN`) precisa ter destino no
que a rodada propõe: mantida, ampliada, corrigida ou explicitamente fora de escopo. A tabela
**Rastreabilidade** ao fim do `spec.md` é onde isso vive. Item do RF sem destino é lacuna, não
simplificação.

### 2.2 Coerência com o negócio

Todo domínio tem regras que não se inventam — prazos legais, ordens obrigatórias, quem pode o quê,
o que invalida o quê. Levante quais são as deste requisito e confira se o que a rodada propõe
conversa com o que o sistema **já calcula**, em vez de pedir que alguém digite de novo o que ele já
sabe derivar.

Confira também contra o formato de mercado para esse tipo de sistema: um funil comercial tem lead,
qualificação, proposta e fechamento; um processo tem estado, responsável e prazo. Se o desenho da
rodada diverge do padrão, ou há razão de negócio registrada, ou é lacuna.

### 2.3 Aderência ao que já foi entregue

Inspecione os domínios existentes no código e responda: **a rodada toca alguma feature
homologada?** Se sim, isso é o risco mais alto que ela carrega, e precisa aparecer no roteiro como
fase própria (§8) e como linha da §7.

Verifique no código, não só no documento — o código vence.

### 2.4 Os ⛔ registrados

A coluna "⛔ aberto" do `README.md` do plano da onda lista as decisões que faltam ao cliente. **Cada
uma é uma pergunta obrigatória ao usuário nesta fase.** Um ⛔ que atravessa a fase 1 sem resposta
reaparece na fase 3 como código escrito sobre uma suposição.

### 2.5 Dependências

A cadeia da onda costuma ser linear, e as arestas são dependências de dado. Confirme que tudo o que
a rodada consome já está entregue. Se não estiver, a fila está errada e isso é decisão do usuário.

---

## 3. Perguntar

Toda ambiguidade encontrada vira pergunta via `AskUserQuestion`. Quantas rodadas de perguntas forem
necessárias — **não há teto de 3 aqui**, ao contrário do `/speckit-specify`. Esta fase existe
justamente para gastar tempo barato agora.

Uma boa pergunta desta fase:

- é **de negócio**, não de implementação — quem decide é o cliente;
- traz as opções com a **consequência** de cada uma sobre o escopo;
- diz qual você recomenda, e por quê;
- não pergunta o que o repositório já responde.

Perguntas de implementação com resposta óbvia pela convenção do projeto **não sobem**. Você as
resolve e registra como decisão tomada por convenção.

---

## 4. Registrar

As respostas viram linhas da tabela **§2 Decisões de entrada** do `roteiro.md`, no formato do
`.specify/templates/roteiro-template.md`:

| # | Ponto | Decisão | O que isso fecha |

Se o `roteiro.md` ainda não existir, crie-o a partir do template e preencha **apenas** a §1 e a §2 —
o resto é fase 2. Se existir em rascunho, atualize a §2 e marque na §9 o que já foi percorrido.

Registre também as decisões tomadas **por convenção, sem pergunta**. O template pede isso
explicitamente, e são justamente essas que geram discussão depois, por não terem sido discutidas.

---

## 5. Encerrar a fase

Reporte ao usuário:

1. onde as definições foram encontradas;
2. o que você criticou, e o que passou;
3. as decisões fechadas nesta fase;
4. o que **continua aberto**, e o que ele precisa levantar para destravar.

Depois **pare**. A fase 2 começa quando ele disser para seguir.
