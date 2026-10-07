# Regra do lote, pacote de execução e prompt de disparo

O que o orquestrador precisa decidir **antes** de delegar, e o texto exato com que ele dispara.

---

## 1. A regra do lote

Só delegue trabalho que cumpra **os três**:

1. tem portão de verificação barato — `./mvnw clean install`, `npm test`;
2. cabe numa lista `READ` fechada e pequena;
3. economiza mais do que o onboarding de uma sessão nova, por volume ou paralelismo real.

Sem prova dos três, não delegue. **O orquestrador já carregado é o executor mais barato que existe**,
e delegar a fase de julgamento de uma rodada é pagar de novo pelo contexto que já está na mesa.

O critério que governa a escolha é o do Princípio VIII: **custo mínimo sujeito aos gates de
qualidade**. A decisão `sem delegação` é uma decisão completa, não uma pendência.

**A regra em uma linha:** *modelo capaz onde a decisão propaga; delegado onde o padrão já existe e há
teste que pega o erro.*

---

## 2. Como se monta um lote

Um lote é um recorte de fases do `tasks.md` que compartilha portão e camada. O que fica com o
orquestrador é o que decide: a máquina de estados, o mecanismo transversal, a varredura nova. O que
se delega é o que tem espelho literal no código e um portão que prova o erro.

Exemplo de recorte, para uma rodada com banco, mecanismo, autorização e frontend:

| Lote | Escopo | Quem | Tier | Portão |
| :--- | :--- | :--- | :--- | :--- |
| **A** | Banco + Modelo | delegado | executor-tabelado | `contextLoads` — paridade JPA ↔ Liquibase |
| — | **O mecanismo** | **orquestrador** | — | testes test-first verdes |
| — | **Varreduras** | **orquestrador** | — | mutação prova que a rede pega |
| **B** | Autorização + Consulta | delegado | executor-de-padrão | `./mvnw clean install` |
| **C** | Frontend | delegado | executor-de-padrão | `npm test` + `npm run build` |
| — | Fechamento | orquestrador | — | homologação |

**B e C são paralelos de verdade**: um é inteiramente backend, o outro inteiramente frontend, zero
arquivo em comum.

> ⚠️ **Verifique a sobreposição de arquivos antes de paralelizar.** Uma fase rotulada "autorização"
> costuma arrastar `acoes.ts` e a matriz de perfis — que são frontend. Dois lotes que tocam o mesmo
> arquivo não são paralelos, por mais que os rótulos sugiram que sim. Mova o arquivo para o lote da
> camada dele antes de disparar.

**Nenhum delegado comita.** Ele entrega e reporta; o orquestrador verifica o portão e comita. Um
delegado que comita convenção errada transforma um erro barato em um `revert`.

---

## 3. O pacote de execução

Um arquivo **por lote**, em `specs/0NN-.../pacote-de-execucao-lote-<X>.md`, escrito pelo orquestrador
**depois** do portão humano. **Teto de 150 linhas.** Um arquivo por lote — e não por rodada — é o que
impede dois executores paralelos de disputarem a mesma linha, e o que permite ao pacote acompanhar um
worktree.

Escreva-o a partir de
[`.specify/templates/pacote-de-execucao-template.md`](../../../../.specify/templates/pacote-de-execucao-template.md),
cuja numeração de seções é **fixa** — o `verificar-lote.ps1` lê a §1 dele. Conteúdo:

1. as listas fechadas `READ`, `EDIT` e `CREATE`, com caminho completo;
2. o contrato pronto que o lote consome, quando houver, para o executor não inventar campo;
3. **um** arquivo de referência por tipo de artefato;
4. as 8–10 regras de convenção que se aplicam **àquele lote** — não o `CLAUDE.md` inteiro;
5. o comando do portão e a saída verde esperada;
6. o formato do relatório.

**Checkpoint e rollback não moram aqui.** Eles são a resposta do executor, e vão no
`estado-lote-<X>.md` que ele cria a partir do
[`estado-lote-template.md`](../../../../.specify/templates/estado-lote-template.md). O pacote nomeia o
[`contrato-do-executor.md`](contrato-do-executor.md), que define formato e cadência de uma vez. É o
que mantém o teto de 150 linhas de pé enquanto o contrato cresce.

> **Um arquivo, um autor.** O pacote é o pedido e é **somente leitura para o executor**; o
> `estado-lote-<X>.md` é a resposta e é somente leitura para o orquestrador. Manter o checkpoint
> dentro do pacote custa três coisas, todas já observadas: a numeração de seções colide entre pacotes
> e o prompt de disparo passa a apontar para a seção errada; o executor cola um `git status` dentro do
> próprio arquivo que aquele `git status` reporta como modificado; e a lista fechada precisa abrir
> exceção para o pacote.
>
> **Referência entre arquivos é por nome de arquivo, nunca por número de seção do outro.** É assim que
> a colisão nasce.

> ⚠️ **Não confunda o pacote com uma peça de aprovação humana.** O `roteiro.md` é escrito para um
> humano decidir e pode ser longo. O pacote é insumo de **executor**, e existe para *reduzir* leitura.
> Se ele passar de 150 linhas, ele parou de cumprir a própria função — a essa altura sai mais barato
> mandar ler o repositório.

---

## 4. Modelo de prompt de disparo

```
Você vai executar o LOTE <X> da rodada <NNN>.

1. Leia, e só isto:
   - specs/<NNN>-<slug>/pacote-de-execucao-lote-<X>.md
   - .claude/skills/delegacao-de-modelos/references/contrato-do-executor.md
   - .specify/templates/estado-lote-template.md
   - os arquivos de referência que o pacote nomeia

   NÃO leia o resto de specs/, nem a constituição, nem os planos de onda. O
   pacote já traz o que se aplica ao seu lote. Se ele estiver errado ou
   incompleto, PARE e reporte — não contorne, e não saia lendo o repositório
   para adivinhar.

2. As decisões marcadas ✅ são para executar, não para rediscutir.

3. Quando a documentação divergir do código, o CÓDIGO VENCE — e reporte.

4. ANTES de editar qualquer arquivo do lote, crie
   specs/<NNN>-<slug>/estado-lote-<X>.md a partir do template acima e preencha
   a §1 "Rollback deste lote". Você escreve esse arquivo; o pacote é somente
   leitura para você.

5. A CADA tarefa: ao ABRIR, escreva a §3 "Em curso" com o que vai fazer e onde;
   ao FECHAR, mova para a §2 e atualize a §5. Não deixe para o fim: se a sua
   cota acabar, esse arquivo é tudo que o orquestrador vai ter — e é no meio de
   uma tarefa que ela acaba, não entre duas.

6. Toque SOMENTE os arquivos da §1 do pacote, mais o seu estado-lote-<X>.md.
   Precisou de outro, PARE e reporte — isto é verificado na volta por comando,
   e um arquivo fora da lista reprova o lote inteiro, mesmo com o portão verde.

7. NÃO comite. Rode o portão do pacote, e entregue o relatório dizendo o que
   foi feito, o que não foi, e por quê. Com a saída real das falhas.
```

### Os deltas por família de executor

**Cada executor externo pede um delta sobre o prompt acima, e os deltas têm sinais opostos.** Meça
antes de assumir — a tabela de assimetria em `sondagem-e-roteamento.md` traz o estado corrente.

Para um executor que **não roda comando** em modo headless:

- **Todo caminho no prompt é absoluto.** O cwd do processo pode não ser o workspace; sem caminho
  absoluto e sem a flag de diretório adicional, ele escreve num scratch dir **e reporta sucesso**.
- **Use as ferramentas de arquivo, não a de shell.** Quando a execução de comando é auto-negada sem
  allowlist, o sintoma é `status: SUCCESS` com resposta vazia.
- **O item 7 vira "NÃO comite e NÃO rode o portão"** — ele entrega os arquivos, o checkpoint e o
  relatório; o portão é do orquestrador.

Para um executor que **roda comando** no próprio sandbox:

- **Caminho relativo serve**, quando a invocação define o worktree como cwd de verdade.
- **A instrução sobre não usar shell não se aplica** — e repeti-la é ativamente ruim: joga fora a
  única capacidade que ele tem a mais.
- **O item 7 permanece "NÃO comite, mas RODE o portão".** É o delta que importa: ele itera sobre o
  próprio portão antes de devolver, e os defeitos de runtime — o `LazyInitializationException`, o HQL
  que não resolve, o changeset que não valida — deixam de chegar ao orquestrador como relatório
  verde. Nenhum desses defeitos falha em compilação; um executor que executa é a rede que faltava.
- **Feche o stdin na invocação.** Sem isso, um CLI de execução fica esperando entrada, e num disparo
  em background isso é indistinguível de um lote lento.

> ⚠️ **Rodar o portão não transfere o julgamento.** O verde dele é pré-filtro: quem escreveu o código
> é quem está atestando o código. O orquestrador roda o portão de novo na aceitação, sempre — a
> capacidade nova encurta o ciclo, não o encerra.
