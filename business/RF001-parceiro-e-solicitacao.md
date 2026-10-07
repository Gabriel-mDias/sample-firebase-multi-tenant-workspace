# RF001 — Parceiros e Solicitações

**Entregue e homologado em 5 de setembro de 2026** · Rodada [`001`](../specs/001-parceiro-e-solicitacao/)

---

## 1. A dor que resolve

Antes, três perguntas simples não tinham resposta rápida:

| A dor | O que ela custava |
| :--- | :--- |
| **"Quem é esse parceiro mesmo?"** | O cadastro vivia em planilha, e a mesma pessoa aparecia escrita de dois jeitos |
| **"Em que pé está esse pedido?"** | A resposta dependia de perguntar à pessoa certa, e ela podia estar de férias |
| **"Quem pode ver quanto a gente paga de comissão?"** | Todo mundo, porque a planilha era uma só |

E uma quarta, que só aparecia depois de dar errado: **"quem mudou isso?"** — sem registro, a resposta
era um palpite.

## 2. Resumo do requisito

O sistema passa a guardar **parceiros** e **solicitações** ligados entre si, com três garantias que
não dependem de disciplina de ninguém:

- o cadastro de um parceiro entra **inteiro ou não entra** — nunca metade;
- uma solicitação só anda pelos caminhos previstos, e cada passagem é uma permissão separada;
- toda alteração fica registrada automaticamente, sem que ninguém precise lembrar de registrar.

## 3. Quem usa

| Perfil | O que faz aqui |
| :--- | :--- |
| **Administrador** | Tudo, incluindo excluir parceiro e ver a comissão |
| **Operador** | Cadastra e altera parceiros; conduz solicitações de ponta a ponta. **Não** vê a comissão nem exclui parceiro |
| **Consulta** | Só lê. Nenhum botão de escrita aparece — e se alguém digitar o endereço direto, o sistema recusa |

## 4. O que ele controla

**Do parceiro**: nome, descrição, identidade civil (nome, CPF, RG, nascimento, sexo), um canal de
contato, um endereço — que pode ser fora do Brasil — e a comissão. Mais o número de solicitações
que já foram concluídas para ele, que o sistema mantém sozinho: **ninguém digita esse número**.

**Da solicitação**: título, descrição, o parceiro responsável, a situação atual, as etapas na ordem
em que devem acontecer e, quando for o caso, o motivo do cancelamento.

## 5. Como funciona

### Cadastrar um parceiro

O cadastro é um **assistente de quatro passos** — dados, contato, endereço e um resumo final. Nada é
gravado antes do resumo: o sistema envia tudo de uma vez, e ou entra tudo, ou não entra nada. Não
existe parceiro com endereço pela metade.

Se algum campo estiver errado, **todas** as mensagens aparecem de uma vez. O operador corrige tudo
num submit só, em vez de descobrir um erro por vez.

Duas regras que costumam surpreender, e são deliberadas:

- **Nomes repetidos são recusados** — mas só entre parceiros ativos. Se um parceiro foi excluído, o
  nome dele volta a ficar disponível.
- **Endereço fora do Brasil não pede UF nem CEP.** Cidade, bairro e logradouro continuam
  obrigatórios em qualquer país.

### Conduzir uma solicitação

Toda solicitação nasce em **rascunho** — visível só para quem a criou, e ainda editável. Dali ela
segue um caminho fixo:

```
Rascunho ──publicar──▶ Aberta ──iniciar──▶ Em andamento ──concluir──▶ Concluída
    │                     │                     │
    └─────────────────────┴──── cancelar ───────┴──────────────────▶ Cancelada
```

**Não há atalho.** Uma solicitação em rascunho não pode ser concluída direto: alguém precisa
publicá-la e alguém precisa assumi-la. E uma solicitação concluída ou cancelada não volta atrás.

Cada uma dessas passagens é uma **permissão separada**. Na prática: uma pessoa pode ter autorização
para corrigir o título de uma solicitação sem ter autorização para encerrá-la. Os botões que ela vê
na tela são exatamente os que ela pode usar.

Duas travas fecham o caminho mais comum de erro:

- **Concluir com etapa pendente é recusado.** Se ficou algo por fazer, o sistema diz.
- **Cancelar exige motivo.** Sem ele o cancelamento não acontece — porque, meses depois, "por que
  isso foi cancelado?" é a pergunta que sempre volta.

Ao concluir, o contador do parceiro sobe sozinho.

### O sigilo da comissão

Quem não tem a permissão específica **não recebe o valor**. Não é um campo escondido na tela: o
sistema não o entrega, ponto. Nem quando a pessoa abre o parceiro, nem quando ela lista todos, nem
quando ela consulta o sistema por fora da tela.

E há um detalhe que importa mais do que parece: **quando essa pessoa edita e salva o parceiro, o
valor da comissão continua lá.** Sem esse cuidado, salvar apagaria o número — e ninguém perceberia,
porque quem apagou não podia vê-lo.

A trilha de alterações segue a mesma regra: ela registra **que** a comissão mudou, e não para quanto.

### O registro de alterações

Toda mudança em um cadastro fica registrada: quem fez, quando, e qual campo mudou de quê para quê. O
registro é automático — nenhuma tela precisa lembrar de fazê-lo, e nenhuma tela pode deixar de fazer.

## 6. O que ele ainda não faz

| Item | Por quê |
| :--- | :--- |
| Tela para consultar a trilha de alterações | O registro acontece; a listagem entra numa rodada de conformidade |
| Anexar arquivo a um parceiro ou solicitação | Nenhum fluxo desta entrega precisa. A infraestrutura existe e está fechada |
| Cadastrar as etapas de uma solicitação pela tela | O sistema as trata; a tela desta entrega só as exibe |
| Campo de motivo na tela de cancelamento | A regra está fechada no sistema; falta o campo |
| Painéis e indicadores | Sem histórico suficiente para valer |

## 7. De onde vem

Origem: levantamento inicial da operação. Entregue pela rodada
[`001-parceiro-e-solicitacao`](../specs/001-parceiro-e-solicitacao/roteiro.md), homologada em tela
com dado real de negócio.
