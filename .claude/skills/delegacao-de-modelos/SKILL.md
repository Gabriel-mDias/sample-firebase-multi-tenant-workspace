---
name: "delegacao-de-modelos"
description: "Delega um lote de trabalho a um executor de outro modelo — subagente Claude ou um CLI externo — com sondagem de disponibilidade, isolamento, checkpoint de handoff e rollback exato. Acionada pela fase 3 da /rodada-sdd, e utilizável fora dela."
argument-hint: "O lote a delegar (ex.: 'lote C da 002, frontend') ou vazio para avaliar o que há"
compatibility: "Requer git. Os CLIs externos disponíveis variam por máquina e por período, e não são equivalentes entre si — a diferença que mais pesa é quem consegue rodar o portão. Sem nenhum, a skill roteia só para subagentes Claude."
metadata:
  author: "Meduc"
user-invocable: true
disable-model-invocation: false
---

## Entrada do usuário

```text
$ARGUMENTS
```

---

## O que esta skill é

O **procedimento do orquestrador** para entregar trabalho a um executor que não é você. O contrato do
lado de lá — checkpoint, rollback, lista fechada de arquivos — é do executor e mora em
`references/contrato-do-executor.md`, nomeado por caminho no pacote porque metade dos executores não
é Claude e não lê esta pasta por conta própria.

**O protocolo são dois arquivos, com um autor cada:**

| Arquivo | Escreve | Lê | É |
| :--- | :--- | :--- | :--- |
| `pacote-de-execucao-lote-<X>.md` | você | o executor | o pedido |
| `estado-lote-<X>.md` | o executor | você | a resposta |

Templates em `.specify/templates/`, verificação em `.specify/scripts/powershell/verificar-lote.ps1`.

> **Regra de construção:** esta skill **aponta, nunca duplica.** A regra do lote, o formato do pacote
> e o prompt de disparo estão em `references/regra-do-lote-e-disparo.md`. Nenhum deles é copiado aqui.

**Você continua responsável pelo resultado.** Delegar move a digitação, nunca o julgamento.

---

## Passo 1 — O lote é elegível?

Aplique a **regra do lote** (`references/regra-do-lote-e-disparo.md`, §1): portão barato, lista `READ`
fechada e trabalho suficiente para amortizar uma sessão nova. **Os três, não dois.**

Duas armadilhas fáceis de esquecer:

- **Você é o executor mais barato para julgamento** — já carregou o contexto. Delegar a fase central
  da rodada é pagar de novo pelo que está na mesa.
- **Confira a sobreposição de arquivos antes de paralelizar.** Dois lotes que tocam o mesmo arquivo
  não são paralelos, por mais que pareçam.

Não passou nos três? **Escolha sem delegação, registre o motivo e pare aqui.** Delegar um lote pequeno
é mais caro que fazê-lo.

---

## Passo 2 — Sondar quem está disponível

`references/sondagem-e-roteamento.md`, seção "Sondagem". Sonde **só os candidatos do tier deste
lote**, não o roster inteiro.

⚠️ A sonda prova disponibilidade **no instante**, não saldo — não existe leitura de cota em nenhuma
das famílias. Um executor aprovado pode morrer na metade, e é para isso que serve o checkpoint.

---

## Passo 3 — Rotear

Mesma referência, seção "Tiers e desempate". O critério é **custo mínimo sujeito aos gates**
(Princípio VIII): a capacidade de manter o padrão do projeto decide; a disponibilidade só desempata.

**Os executores não são intercambiáveis, e a diferença que mais pesa é quem consegue rodar o
portão.** Um lote cujo defeito só aparece em execução — HQL, grafo de entidade, migração — entregue
a quem não consegue rodá-lo volta com o defeito intacto e o relatório verde. A tabela da assimetria,
no fim daquela referência, é o resumo operacional.

Nenhum executor disponível → **faça inline e diga que fez.** Não espere cota voltar.

---

## Passo 4 — Propor ao usuário

`AskUserQuestion` com: quais lotes, o que cai em cada um, qual executor, qual portão, e **o que fica
com você**. Diga também o que você **não** vai delegar, e por quê — é a metade da proposta que revela
se o recorte está certo.

A decisão é do usuário. `Sem delegação` é uma decisão completa, não uma pendência. Numa rodada SDD ela
também precisa bater com a nota de delegação do `tasks.md` e a coluna `Modelo` do `plan.md`;
divergiu, corrija o artefato ou o recorte, não os dois em silêncio.

---

## Passo 5 — Escrever o pacote e disparar

1. **Escreva** `specs/0NN-<slug>/pacote-de-execucao-lote-<X>.md` a partir de
   `.specify/templates/pacote-de-execucao-template.md` — **teto de 150 linhas**, numeração de seções
   **fixa**, e ele nomeia o `contrato-do-executor.md` em vez de repetir o contrato. Um arquivo por
   lote: é o que evita que dois executores paralelos disputem a mesma linha.
2. **Crie o `estado-lote-<X>.md` vazio**, ao lado do pacote, a partir de
   `.specify/templates/estado-lote-template.md`. Um arquivo que já está lá é preenchido; um que
   precisa ser criado é esquecido — e as seções finais do estado nunca preenchidas são exatamente esse
   esquecimento.
3. **Ancore o estado antes de qualquer edição** — `git rev-parse HEAD` e
   `git status --porcelain --untracked-files=all`, guardados **em arquivo**, que é o que o
   `-Ancora` do verificador consome. Sem a âncora não existe rollback exato, porque a árvore de
   trabalho quase nunca está limpa.
4. **Isole** conforme `references/verificacao-e-rollback.md`, seção "Isolamento": worktree em lote de
   backend, branch atual em lote de frontend.
5. **Dispare** com o prompt da §4 de `references/regra-do-lote-e-disparo.md`, sem improvisar,
   acrescentando só os caminhos do pacote, do estado e do contrato.

**O pacote é seu, o estado é dele.** Você nunca escreve no `estado-lote-<X>.md` depois de criá-lo, e
ele nunca escreve no pacote. É o que faz a comparação na volta significar alguma coisa.

---

## Passo 6 — Receber

**Nesta ordem, e nenhum passo é dispensável.** `references/verificacao-e-rollback.md`, seção
"Aceitação".

1. **O executor fez alguma coisa?** Passo 0 da referência — `SUCCESS` com `response` vazio não é
   sucesso, e arquivo que ele afirma ter criado se confere no sistema de arquivos.
2. **Rode o verificador**, que confronta o que o pacote pediu, o que o executor declarou e o que a
   árvore mostra:

   ```powershell
   .specify/scripts/powershell/verificar-lote.ps1 `
       -Pacote specs/0NN-<slug>/pacote-de-execucao-lote-<X>.md `
       -Estado specs/0NN-<slug>/estado-lote-<X>.md `
       -Ancora <arquivo da âncora> [-Worktree <caminho>]
   ```

   `FORA DA LISTA`, `NÃO DECLARADO` ou `DECLARADO MAS AUSENTE` → **exit 2, lote reprovado**: desfaça,
   mesmo com portão verde. `NÃO ENTREGUE` e `CAMPOS VAZIOS` são ressalvas, não reprovação. Erro de
   leitura sai com **exit 1** e nomeia arquivo e seção — nunca o confunda com "nada fora da lista".
3. **Rode o portão você mesmo.** Não presuma o verde que o relatório afirma. **O verificador não é o
   portão**: ele mede a extensão do lote e a honestidade do relato, nunca a correção do código.

   ⚠️ **Isto não muda quando o executor roda o portão.** O verde dele é **pré-filtro**, não prova:
   ele rodou o comando que o pacote nomeou, no estado em que ele deixou a árvore, e é exatamente
   quem escreveu o código quem está atestando o código. A capacidade nova encurta o ciclo de ida e
   volta — ela não transfere o julgamento. Um passo 3 pulado porque "ele já rodou" é o passo 3 que
   não aconteceu.
4. **Revise contra as convenções** de `backend/AGENTS.md`, `frontend/AGENTS.md` ou
   `environments/AGENTS.md`, conforme os arquivos tocados.
5. **Rede nova prova-se por mutação** — quebre o alvo, veja o teste falhar, restaure.
6. Só então marque o `tasks.md`, atualize a §9 do roteiro e **comite**. O delegado nunca comita.

Lote incompleto não é lote falho: leia o `estado-lote-<X>.md`, decida entre **retomar** com outro
executor ou **assumir**, e diga qual. Critério na mesma referência, seção "Incompleto".

---

## Regras que valem sempre

- **Nenhum delegado comita.** Ele entrega e reporta; você verifica e comita.
- **Nunca a flag de bypass total, em CLI nenhum.** Todo CLI de agente tem uma — o nome muda, o efeito
  não: ela derruba a fronteira do *"nenhum delegado comita"*. A alternativa correta também existe em
  todos: o modo que libera a **árvore de trabalho** sem liberar o resto. Use esse.
- **Documentação diverge do código: o código vence**, e a divergência é reportada, nunca contornada.
- **Você não escreve id de modelo em lugar nenhum.** Tiers são o vocabulário; a sonda descobre quem
  os preenche. Id cravado apodrece na próxima versão e falha em silêncio.
- **Um arquivo, um autor.** Você nunca escreve no `estado-lote-<X>.md` depois de criá-lo; ele nunca
  escreve no pacote. Misturar os dois apaga a diferença entre o que foi pedido e o que foi entregue.
- **O estado é confrontado, não acreditado.** O valor dele não está em ser verdadeiro — está em ser
  comparável com a árvore. Um executor que mente no relatório mente no campo; o que ele não consegue
  é fazer o `git status` mentir junto.
