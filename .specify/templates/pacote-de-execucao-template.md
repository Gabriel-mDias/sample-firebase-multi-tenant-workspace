# Pacote de Execução — Rodada <NNN>, **Lote <X>** (<fases do plan.md>)

<!--
  TEMPLATE DO PACOTE DE EXECUÇÃO — leia antes de instanciar, e apague este bloco.

  Quem escreve: o ORQUESTRADOR, depois do portão humano. O executor só LÊ este arquivo.
  A resposta do executor vai em `estado-lote-<X>.md`, criado do `estado-lote-template.md`.

  TETO DE 150 LINHAS. Ele não é decoração: o pacote existe para REDUZIR leitura, e o custo
  de onboarding de um executor cai quase pela metade por causa dele. Passou de 150, parou
  de cumprir a própria função — a essa altura sai mais barato mandar ler o repositório.

  A numeração das seções abaixo é FIXA. Não renumere, não insira seção no meio: o
  `verificar-lote.ps1` lê a §1, e o prompt de disparo nomeia arquivos, nunca números de
  seção de outro arquivo. Seção que não se aplica ao lote é REMOVIDA por inteiro — sem
  deixar "N/A", e sem puxar as de baixo para cima.

  O que NÃO entra aqui, nunca:
  - o contrato do executor (checkpoint, rollback, lista fechada) — ele tem dono próprio;
  - o `CLAUDE.md` inteiro — só as 8-10 regras que valem para ESTE lote;
  - "como fazer em detalhes" — a §3 MOSTRA o código de referência, que é mais barato e
    mais fiel do que descrever.
-->

> **A lista `READ` abaixo é fechada. Nada mais pode ser lido ou pesquisado.** Não leia o resto de
> `specs/`, nem a constituição, nem planos de onda, catálogos ou a camada oposta. Se este pacote
> estiver errado ou incompleto, **pare e reporte** — não amplie o contexto para adivinhar.
>
> **Você não comita.** <Rode o portão da §5 | Não rode o portão — ele é do orquestrador> e entregue o
> relatório da §6. <!-- a primeira opção para executores que rodam comando; a segunda para os que não rodam -->
>
> **Antes da primeira edição**, crie o seu `estado-lote-<X>.md` a partir de
> [`.specify/templates/estado-lote-template.md`](../../.specify/templates/estado-lote-template.md) e
> preencha a §1 dele. O seu contrato completo está em
> [`contrato-do-executor.md`](../../.claude/skills/delegacao-de-modelos/references/contrato-do-executor.md).

---

## 1. Fronteira fechada do lote

<Uma frase dizendo o que o lote é, e uma dizendo o que ele explicitamente NÃO é.>

### READ

<!-- Lista de leitura em bullets, nunca tabela: o verificador interpreta caminhos em tabelas desta
     seção como arquivos editáveis. Inclua pacote, contrato, arquivos editados, contratos consumidos
     e uma referência por tipo. -->

- `caminho/permitido.ext` — por que é necessário

**Nada fora de READ pode ser pesquisado ou lido.** Precisou de outro arquivo, pare e reporte.

### EDIT / CREATE

Você não cria, edita ou apaga nada fora da tabela — nem para "corrigir de passagem". Isto é
verificado na volta, e um arquivo fora da lista reprova o lote inteiro, mesmo com o portão verde. A
única exceção é o seu `estado-lote-<X>.md`.

| # | Ação | Arquivo |
| :-- | :--- | :--- |
| T0NN | EDIT | `caminho/completo/do/arquivo.ext` — o que muda nele |
| T0NN | CREATE | `outro/caminho.ext` |

<!--
  FORMATO OBRIGATÓRIO desta tabela — o `verificar-lote.ps1` a parseia:
  - três colunas: id da tarefa, ação EDIT/CREATE, depois o(s) arquivo(s);
  - todo caminho entre CRASES, relativo à raiz do repositório;
  - vários arquivos numa linha são permitidos, cada um em suas próprias crases;
  - texto explicativo depois do caminho é livre e ignorado pelo script.
-->

---

## 2. O que já existe e você consome

<!-- Seção REMOVÍVEL. Só existe quando o lote consome contrato pronto: assinatura de API,
     enum, DTO, evento. Serve para o executor não inventar campo. Apague-a se não se aplica. -->

**Já existe. Não invente campo.**

<Assinatura do endpoint, forma do DTO, valores do enum. Bloco de código, não prosa.>

---

## 3. Arquivos de referência — copie destes

**Um por tipo de artefato.** Copie a forma daqui em vez de deduzi-la do `CLAUDE.md`.

| Para escrever | Copie de |
| :--- | :--- |
| <o service> | `caminho/do/exemplo` — <o que observar nele> |
| <o teste> | `caminho/do/exemplo` |

---

## 4. As regras que valem para **este** lote

<!-- 8 a 10, numeradas, e SÓ as que este lote pode violar. Marque com ⚠️ as que já
     produziram defeito silencioso catalogado no projeto — são as que mais escapam. -->

1. <regra>
2. <regra>

---

## 5. O portão

<!-- A condição para remover esta seção é CAPACIDADE, não externalidade. Remova quando o
     executor não conseguir rodar comando — o `agy` em headless não roda sem allowlist, e
     aí o portão é do orquestrador; diga isso no cabeçalho. O Codex e o subagente Claude
     rodam, e para eles a seção FICA: o portão que ele roda encurta o ciclo de ida e volta.

     Rodar não é provar. O verde dele é pré-filtro — quem escreveu o código está atestando
     o código —, e o orquestrador roda o portão de novo na aceitação, sempre. -->

A partir de `<backend/ | frontend/>`:

```bash
<comando>
```

<A saída verde esperada, com o número atual de testes — "o número só pode subir".>

---

## 6. O relatório

1. **o que foi feito** — arquivos criados e alterados;
2. **o que não foi, e por quê**;
3. o comando, exit code e resumo do portão; cole log completo somente quando falhar;
4. **divergências** entre este pacote e o código. Quando divergirem, o **código vence** — e reporte.

Se algo falhou, mostre a saída. Relatório que omite o que não funcionou custa mais que a falha.

**O relatório não substitui o `estado-lote-<X>.md`.** O relatório é a sua narrativa e some com a
conversa; o estado é do lote, e é ele que o orquestrador confronta com a árvore.
