# Fase 5 — Escrita do histórico

**Objetivo:** deixar o repositório contando a verdade sobre o que a rodada entregou — em linguagem
de negócio na pasta `business/`, e em linguagem de agente nos arquivos de contexto.

**Termina quando:** o §D do checklist de fechamento está integralmente marcado.

Esta é a **última etapa da rodada**. Ela só começa com a homologação registrada — o bloco
`✅ HOMOLOGADA` com data na seção **"Critérios de aprovação"** do roteiro (localizada pelo título; é a
§12 no template, §13 na `005`). Ao fim desta fase, o mesmo bloco mais o `business/` atualizado é o que
faz a `/rodada-sdd` ler a rodada como **encerrada** — sem os dois, ela volta a ser lida como fase 5.

---

## 1. `business/RF0NN-*.md` — o documento de negócio

O arquivo normalmente já existe, escrito como **desenho proposto** quando o requisito foi levantado.
Esta fase o converte para **entregue**.

A estrutura é fixa, e está declarada na seção "Como ler esta pasta" do `business/README.md` — sete
seções, na ordem, para que os documentos sejam comparáveis entre si. Siga aquela lista; não invente
seções.

O que muda de um documento planejado para um entregue:

| Seção | O que fazer |
| :--- | :--- |
| A dor que resolve | preservar — ela não mudou |
| Resumo do requisito | reescrever para o que **foi** construído, não o que se pretendia |
| Quem usa | a matriz real de perfis, conferida contra `MapaPerfilAcoes` e `AcaoSistema` |
| O que ele controla | as informações que a feature efetivamente gere |
| **Como funciona** | o passo a passo **ilustrado com as telas reais**, capturadas na fase 4 |
| O que ele ainda não faz | a fronteira real, saída da §11 do roteiro — para não gerar expectativa errada |
| De onde vem | a origem no levantamento, mais a rodada que entregou |

**As imagens vão para `business/imagens/rf0NN/`**, com nomes que descrevem a tela
(`listagem.png`, `wizard-1-dados.png`, `visualizacao.png`).

### O tom

Olhe um documento de negócio já entregue antes de escrever. O leitor é o cliente e o analista de
requisitos, não o desenvolvedor: sem nome de classe, sem endpoint, sem nome de tabela. Uma decisão
técnica só aparece quando ela **muda o que o cliente pode fazer**.

---

## 2. `business/README.md`

Quatro pontos, e todos envelhecem em silêncio se você esquecer:

- a tabela "Como o sistema está estruturado hoje" — mover a área nova para as entregues;
- a lista **"O que ainda não existe"** — remover o que a rodada passou a existir;
- a tabela de perfis, se a rodada deu alcance novo a algum;
- o diagrama das ondas, se a rodada fechou uma.

---

## 3. Os arquivos de agente

O `AGENTS.md` raiz é a regra transversal; instruções específicas ficam nos arquivos aninhados.
`CLAUDE.md`, `GEMINI.md`, `.claude/rules/`, `.cursor/rules/` e `.github/instructions/` são adaptadores
paralelos e **se atualizam juntos** quando a regra afetar suas plataformas.

Nos arquivos de agente, a rodada normalmente move:

- o `AGENTS.md` da camada, se nasceu módulo, schema ou convenção recorrente;
- a regra path-scoped equivalente nas plataformas usadas;
- uma skill, se nasceu procedimento com múltiplas etapas;
- os números citados no texto, se mudaram (ações, controllers, changelogs, rotas).

`CODEX.md` é um roteador sob demanda — atualize-o **só** se a rodada mudou hierarquia
documental, fluxo spec-kit ou um override local de alto risco. Sem duplicar a regra completa nele.

⚠️ Mantenha os adaptadores curtos; detalhes pertencem ao `AGENTS.md` aninhado ou à skill.

Rode `scripts/verificar-contexto-agentes.ps1` ao fim: ele confere que os adaptadores continuam
consistentes entre si e dentro dos limites de tamanho.

---

## 4. Os planos de onda

- `README.md` do plano da onda — a linha da rodada na tabela de estado passa de `rascunho` para
  `✅ CONCLUÍDA e homologada em <data>`, e a coluna ⛔ é zerada ou atualizada;
- a tabela de **portões humanos** do plano, se houver — marque os portões daquela rodada;
- o arquivo de pendências abertas da onda anterior, se a rodada fechou alguma pendência herdada.

Se a rodada acrescentou uma lição do tipo que o §E do checklist de fechamento coleciona
("o que este projeto aprendeu do jeito caro"), acrescente a linha lá. É uma tabela pequena de
propósito: só entra o que custou.

---

## 5. A constituição

**Só** se a rodada mudou um princípio. Nesse caso é `/speckit-constitution`, com bump de versão e a
propagação para os templates — não uma edição manual.

Se a constituição e um arquivo de agente divergirem, a constituição vence e o arquivo é corrigido.

---

## 6. Encerrar a rodada

Percorra o **§D do `references/checklist-de-fechamento.md`** item a item e mostre o resultado.
Depois reporte:

1. o que foi documentado, e onde;
2. as divergências entre documentação e código que a fase encontrou — corrigidas se pequenas e
   óbvias, reportadas se não;
3. o que a rodada deixou **frouxo**, e em qual rodada fecha;
4. qual é a próxima rodada da fila, e o que ela precisa para abrir.

A rodada está encerrada. A próxima começa com `/rodada-sdd`.
