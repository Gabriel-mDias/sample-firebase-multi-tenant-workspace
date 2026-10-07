# Fase 2 — Plan

**Objetivo:** produzir o `roteiro.md` da rodada e iterá-lo até a aprovação do usuário.

**Termina quando:** a §12 do roteiro carrega o bloco de portão aprovado, com data.

> O `roteiro.md` é o **portão de aprovação humana antes do código** (Princípio VIII). Ele tem duas
> funções ao mesmo tempo, e as duas são obrigatórias: serve de **checklist de execução** para quem
> vai implementar (§3, §4, §6, §8) e de **direcionador de entendimento** para quem vai aprovar
> (§1, §2, §7, §10, §11).

---

## 1. Estrutura

Use `.specify/templates/roteiro-template.md`. As 12 seções, os comentários de preenchimento e as
duas regras de ouro estão lá — **não as repita aqui, siga-as**.

Modelo vivo de forma: `specs/001-parceiro-e-solicitacao/roteiro.md`, incluindo os blocos de
assinatura da §12 e a tabela de números medidos.

Remova por completo a seção que não se aplica. Não deixe "N/A".

---

## 2. Antes de escrever, apure

O roteiro é documento **compilado**, e a segunda regra de ouro do template é que todo número citado
precisa bater com a fonte. Apure **contra o código**, nunca contra outro documento:

| Número | Como apurar |
| :--- | :--- |
| Testes de backend, hoje | `.\mvnw.cmd test` e leia o total por módulo |
| Testes de frontend, hoje | `npm test` e leia arquivos e casos |
| Ações em `AcaoSistema` | conte no enum de `meduc-security` |
| Constantes em `acoes.ts` | conte no arquivo do frontend |
| Controllers e anotações `@PreAuthorize` | a varredura `CoberturaPreAuthorizeTest` já conta |
| Rotas guardadas | `routes-coverage.spec.ts` |
| Changelogs incluídos no master | conte os `<include>` |
| Tarefas da rodada | conte no `tasks.md`, depois do `/speckit-tasks` real |

Documento compilado que diverge da fonte é pior que ausência.

---

## 3. As seções que decidem a aprovação

Quatro merecem atenção desproporcional, porque são as que o usuário lê para decidir:

### §3 Banco de dados

Coluna a coluna, changesets em ordem, e **toda ordem obrigatória destacada** (backfill antes de
constraint, FK depois da tabela referenciada). Se a alteração toca dado existente, essa é a
informação de maior risco do documento inteiro — ela vem antes, não depois.

Registre também o que **não** entra no banco e por quê: regra que vive no service em vez de virar
constraint precisa estar escrita, ou reaparece como pergunta na revisão.

### §6 Testes esperados por bloco

Para cada teste, diga **o que ele prova** — não que ele existe. Marque o que é novo e o que é
existente ajustado.

Antes de fechar a §6, passe pelo §C do `references/checklist-de-fechamento.md`: ele lista os testes
que o projeto **exige por escrito**, e cada exigência que se aplica à rodada precisa ter linha aqui.

### §7 Interação e isolamento

Responde à pergunta direta: *esta feature é isolada o suficiente para não impactar as demais?* A
tabela classifica cada recurso como novo isolado, novo compartilhado, **alterado compartilhado** ou
reuso sem alteração — e diz o que impede a regressão em cada caso.

A subseção **"O que fica frouxo depois desta feature"** não é confissão, é o que permite decidir com
informação. Constraint que vira regra de service, invariante que passa a depender de código: tudo
entra.

### §8 Divisão de tasks e delegação por modelo

A decisão de delegação é **obrigatória e explícita** (Princípio VIII); `sem delegação` é válida. O
critério do projeto é *custo mínimo sujeito aos gates*, e a regra em uma linha está na skill
`delegacao-de-modelos`: modelo capaz onde a decisão propaga; executor barato onde há espelho e gate;
inline quando o onboarding não se amortiza.

**Justifique cada uso do modelo mais caro por risco material.** É o que torna a escolha revisável.

---

## 4. Iterar até a aprovação

Apresente o roteiro e **pare**. Não comece a fase 3, não escreva código, não rode
`/speckit-implement`.

O usuário vai comentar. Cada comentário é uma alteração no roteiro, não uma exceção verbal — se ele
mudar uma decisão, a §2 muda junto; se ele recusar um item da §11, ele sai de lá e entra no escopo.

---

## 5. Gravar o portão

Aprovado, marque os itens da lista "Para aprovar esta rodada (antes do código)" da §12 e acrescente
logo abaixo dela o bloco:

```markdown
> ✅ **PORTÃO APROVADO pelo product owner em <AAAA-MM-DD>.** <o que exatamente foi aceito: as
> decisões de entrada, o modelo de dados, o contrato REST, o raio de impacto.>
> **O `/speckit-implement` está liberado.**
```

Atualize também a linha 6 (`roteiro.md`) da §9.

Confira o §A do `references/checklist-de-fechamento.md` antes de considerar o portão fechado — ele
exige, entre outras coisas, que o `/speckit-clarify` tenha sido executado **ou dispensado por
escrito, com a razão**, e que os achados HIGH e MEDIUM do `/speckit-analyze` tenham sido
**incorporados aos artefatos, não só listados**.

> Se o ciclo spec-kit ainda não foi percorrido nesta rodada, o portão **não pode** fechar: o
> `/speckit-analyze` é pré-requisito da §12. Nesse caso, o roteiro é aprovado como **direção** e o
> portão fica aberto até a fase 3 rodar a varredura e devolver os achados a esta fase. Diga isso ao
> usuário com todas as letras — é a diferença entre "aprovei o plano" e "liberei o código".

---

## 6. Encerrar a fase

Reporte o que foi aprovado, o que mudou no roteiro durante a iteração, e **recomende `/compact`**:
este é o ponto de maior acúmulo de leitura do ciclo, e a fase 3 começa lendo bastante código.

Você não tem ferramenta para compactar. Recomende, e deixe a decisão com o usuário.
