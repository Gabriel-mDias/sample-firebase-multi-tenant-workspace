# Roteiro da Rodada — [NNN] [NOME DA FEATURE]

**Feature**: `[NNN-nome-da-feature]` · **Branch**: `feature/[nome]` · **Requisito**: [RFxxx]
**Data**: [DATA] · **Status**: aguardando aprovação

> **O que é este documento.** A peça de aprovação da rodada: o que será feito, onde, com que testes,
> com que risco para o que já existe e em que ordem. Escrito para ser lido de ponta a ponta **sem
> abrir outro arquivo** — os demais artefatos ficam como referência de detalhe, não como
> pré-requisito de leitura. Também é a porta de entrada para quem for entender esta feature no
> futuro.

<!--
  COMO PREENCHER
  ==============
  Este é o último artefato do ciclo spec-kit, escrito DEPOIS do /speckit-analyze e ANTES do
  /speckit-implement (Princípio VIII). Ele é portão de aprovação humana: sem o "sim" registrado
  aqui, nenhuma linha de código da feature é escrita.

  Regra de ouro: CONSOLIDA a decisão e APONTA para o detalhe. Não duplique o conteúdo de spec.md,
  plan.md ou data-model.md — se o leitor precisar do detalhe, ele clica. O que não pode acontecer é
  o leitor precisar abrir outro arquivo para ENTENDER o que a rodada faz.

  Segunda regra: todo número citado aqui precisa bater com sua fonte (contagem de tarefas, de
  requisitos, de testes existentes). Documento compilado que diverge da fonte é pior que ausência.

  Remova por completo qualquer seção que não se aplique. Não deixe "N/A".
-->

**Índice**: [1 Objetivo](#1-objetivo-da-rodada) · [2 Decisões de entrada](#2-decisões-de-entrada) ·
[3 Banco](#3-banco-de-dados) · [4 Backend](#4-backend) · [5 Frontend](#5-frontend) ·
[6 Testes](#6-testes-esperados-por-bloco) · [7 Interação e isolamento](#7-interação-e-isolamento) ·
[8 Tasks e delegação](#8-divisão-de-tasks-e-delegação-por-modelo) · [9 Prompts SDD](#9-prompts-sdd-da-rodada) ·
[10 Riscos](#10-riscos-e-mitigações) · [11 Fora de escopo](#11-fora-de-escopo) ·
[12 Aprovação](#12-critérios-de-aprovação)

---

## 1. Objetivo da rodada

[O que a feature entrega, em um parágrafo.]

### Por que este requisito agora

[O critério de escolha. Se houver dependência entre requisitos, desenhe o grafo — é o argumento mais
forte e o mais rápido de ler.]

### O que a rodada carrega junto

[Dívida técnica, refatoração ou integração que vem de carona porque o requisito a exige. Se não
houver, remova a subseção. Se houver, diga **por que é obrigatória** — o leitor precisa saber que não
é escolha de quem planejou.]

---

## 2. Decisões de entrada

[Decisões tomadas com o product owner ANTES da escrita da especificação. São entrada do ciclo, não
questões em aberto — e é por elas que o spec.md não tem marcadores [NEEDS CLARIFICATION].]

| # | Ponto | Decisão | O que isso fecha |
| :-- | :--- | :--- | :--- |
| 1 | [tema] | **[decisão]** | [que porta se fecha, que retrabalho se evita] |

[Liste também as decisões tomadas por convenção, sem pergunta — elas costumam ser as que geram
discussão depois, justamente por não terem sido discutidas.]

---

## 3. Banco de dados

[Se a feature não toca o schema, remova a seção inteira.]

### 3.1 [TABELA] — alteração

[Tabela antes/depois, coluna a coluna. Se a alteração toca dado em produção, diga aqui — é a
informação de maior risco do documento.]

### 3.2 [TABELA] — nova

| Coluna | Tipo | Nulo | Observação |
| :--- | :--- | :--- | :--- |

[Liste os changesets em ordem, com o `id` de cada um, e destaque toda ordem OBRIGATÓRIA (backfill
antes de constraint, FK depois da tabela referenciada). Registre também o que **não** entra no banco
e por quê — regra que vive no service em vez de constraint, por exemplo.]

---

## 4. Backend

### 4.1 `app-model` · 4.2 `app-security` · 4.3 `app-events` · 4.4 `app-core` · 4.5 `app-deploy`

[Uma tabela por módulo Maven tocado, separando **novo** de **alterado**. Módulo não tocado: remova.]

| Arquivo | Pacote | Situação |
| :--- | :--- | :--- |

### 4.6 Onde cada regra vive

[A tabela mais útil do documento para quem vai revisar o PR: cada regra de negócio, onde ela mora, e
se é código novo ou reuso do que já existe.]

| Regra | Onde | Novo? |
| :--- | :--- | :--- |

### 4.7 Contrato REST

| Verbo | Rota | Autorização | Retorno |
| :--- | :--- | :--- | :--- |

[Destaque todo código de retorno não óbvio e a razão dele.]

---

## 5. Frontend

### 5.1 O que é novo

[Árvore do módulo de feature + as decisões de tela: listagem, wizard, modos de rota.]

### 5.2 O que é alterado fora da feature

| Arquivo | Alteração |
| :--- | :--- |

### 5.3 Comportamentos da SDK que o roteiro já contorna

[Armadilhas conhecidas da `angular-gems-sdk` que valem para esta feature. Não é detalhe de
implementação: é o que impede repetir um retrabalho já pago.]

---

## 6. Testes esperados por bloco

[Princípio VII. Para cada teste, diga **o que ele prova** — não apenas que ele existe.]

### 6.1 Banco de dados

| Teste | O que prova |
| :--- | :--- |

### 6.2 Backend

| Teste | Situação | O que prova |
| :--- | :--- | :--- |

[Marque quais são **novos** e quais são **existentes ajustados**. Testes existentes que precisam
continuar verdes são o gate de não-regressão — cite a contagem real.]

### 6.3 Frontend

| Teste | Situação | O que prova |
| :--- | :--- | :--- |

### 6.4 Validação manual

[Resumo dos gates do quickstart.md, em uma linha.]

---

## 7. Interação e isolamento

**A pergunta direta: esta feature é isolada o suficiente para não impactar as demais?**

**Resposta: [sim / não / parcialmente, e por quê].**

| Recurso | Natureza | Quem é afetado | O que garante |
| :--- | :--- | :--- | :--- |
| [recurso] | Novo e isolado / Novo compartilhado / **Alterado compartilhado** / Reuso sem alteração | [features atingidas] | [teste, gate ou propriedade que impede a regressão] |

### Fronteiras arquiteturais respeitadas

[Grafo Maven, uso ou não de eventos entre domínios, schemas tocados, permissões novas. Se a feature
diverge de um princípio, ela precisa estar no Complexity Tracking do plan.md — aponte para lá.]

### O que fica frouxo depois desta feature

[Toda garantia que a feature afrouxa — constraint que vira regra de service, invariante que passa a
depender de código. Registrar não é confessar: é o que permite decidir com informação.]

---

## 8. Divisão de tasks e delegação por modelo

**[N] tarefas** em [M] fases. Distribuição: [US1 n · US2 n · … · infraestrutura compartilhada n].

| Fase | Escopo | Tarefas | Modelo | Gate de saída |
| :-- | :--- | :--- | :--- | :--- |

### Critério de delegação

[**Custo mínimo sujeito aos gates.** `Sem delegação` é válida quando uma sessão nova não se amortiza.
Use modelo mais capaz apenas onde erro silencioso custa caro e justifique o risco material.]

### Ordem e paralelismo

[Diagrama das dependências entre fases, e a lista das ordens **obrigatórias** dentro de cada fase.]

**MVP = [fases].** [O que pode ser cortado se a rodada apertar, e o que não pode.]

---

## 9. Prompts SDD da rodada

| Ordem | Comando | Situação | Saída |
| :-- | :--- | :--- | :--- |
| 1 | `/speckit-specify` | | |
| 2 | `/speckit-clarify` | | [se dispensado, diga por quê] |
| 3 | `/speckit-plan` | | |
| 4 | `/speckit-tasks` | | |
| 5 | `/speckit-analyze` | | |
| 6 | **`roteiro.md`** | | este documento |
| 7 | `/speckit-implement` | 🔒 | após aprovação |

### Achados do `/speckit-analyze` já incorporados

| Achado | Sev. | Decisão |
| :--- | :--- | :--- |

[Inclua também os achados **recusados**, com a razão. Achado sem decisão registrada volta como dúvida
na revisão do PR.]

---

## 10. Riscos e mitigações

| # | Risco | Gravidade | Mitigação |
| :-- | :--- | :--- | :--- |

---

## 11. Fora de escopo

[Nada aqui pode ser esquecimento — cada item tem destino.]

| Item | Por quê | Volta em |
| :--- | :--- | :--- |

---

## 12. Critérios de aprovação

### Para aprovar esta rodada (antes do código)

- [ ] O escopo da §1 é o que se espera da próxima entrega
- [ ] As decisões da §2 refletem o que foi combinado
- [ ] O impacto da §7 é aceitável
- [ ] A cobertura de testes da §6 é suficiente para o risco assumido
- [ ] A decisão de delegação (inclusive `sem delegação`) da §8 está adequada
- [ ] Os itens fora de escopo (§11) podem mesmo esperar

### Para considerar a rodada concluída (depois do código)

- [ ] `.\mvnw.cmd clean install` verde no reator inteiro, com testes
- [ ] `npm test` e `npm run build` verdes
- [ ] Aplicação sobe com `ddl-auto: validate` sem reclamar de schema
- [ ] Os gates do quickstart.md marcados
- [ ] **Zero regressão** nas features já entregues
- [ ] `/speckit-analyze` final sem CRITICAL e sem violação de constituição
- [ ] Arquivos de agente atualizados

---

## Referência dos artefatos

| Arquivo | Para quê |
| :--- | :--- |
| [spec.md](spec.md) | Requisitos, user stories, critérios de sucesso, decisões de entrada |
| [plan.md](plan.md) | Decisões técnicas congeladas, Constitution Check, estrutura |
| [research.md](research.md) | Por que cada decisão técnica |
| [data-model.md](data-model.md) | Colunas, changesets, DTOs, consultas |
| [contracts/](contracts/) | Payloads, códigos de retorno, contrato do módulo de frontend |
| [tasks.md](tasks.md) | Tarefas, dependências e paralelismo |
| [quickstart.md](quickstart.md) | Roteiro de validação manual |
| [checklists/](checklists/) | Qualidade da especificação e cobertura do requisito |
