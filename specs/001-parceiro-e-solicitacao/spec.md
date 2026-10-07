# Feature Specification: Parceiro e Solicitação

**Feature Branch**: `feature/001-parceiro-e-solicitacao`

**Created**: 2026-09-05

**Status**: ✅ Entregue e homologada

**Input**: User description: "Cadastro de parceiros e um fluxo de solicitações atribuídas a eles."

> **Por que esta rodada existe no molde.** Ela é a **referência viva** do padrão de qualidade
> esperado de uma spec neste projeto — e é também a origem do código que está no repositório, para
> que o Princípio VIII (nada de código sem spec) valha desde o primeiro commit. Um projeto derivado
> a substitui pela primeira rodada real; até lá, ela é o exemplo que a IA imita.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Cadastrar um parceiro (Priority: P1)

Quem opera precisa registrar um parceiro com a identidade civil dele, um canal de contato e um
endereço, tudo numa passagem só. A gravação é atômica: ou entra tudo, ou não entra nada.

**Why this priority**: nenhuma outra história existe sem esta — a solicitação é sempre de um
parceiro. É também o cadastro de referência que os demais domínios copiam.

**Independent Test**: cadastrar um parceiro pela tela, encontrá-lo na busca e reabri-lo em leitura.
Entrega valor sozinha: o cadastro de parceiros já é útil sem nenhuma solicitação.

**Acceptance Scenarios**:

1. **Given** um operador com `INSERIR_PARCEIRO`, **When** ele preenche os quatro passos do wizard e
   confirma, **Then** o parceiro, a pessoa, o contato e o endereço são gravados na mesma transação.
2. **Given** um formulário com nome vazio e comissão negativa, **When** o operador confirma,
   **Then** as duas mensagens aparecem **de uma vez**, não uma por submit.
3. **Given** um nome já usado por um parceiro ativo, **When** o operador tenta gravar, **Then** a
   gravação é recusada com mensagem de negócio.
4. **Given** um parceiro excluído cujo nome era "Alfa", **When** alguém cadastra outro "Alfa",
   **Then** a gravação é aceita — a unicidade vale entre os ativos.

---

### User Story 2 - Conduzir uma solicitação pelo fluxo (Priority: P1)

A solicitação nasce em rascunho, é publicada, alguém a assume, e ela termina concluída ou cancelada.
Cada passagem é um ato próprio, com permissão própria.

**Why this priority**: é a razão de o sistema existir. Sem ela, o cadastro de parceiros é uma agenda.

**Independent Test**: abrir uma solicitação e levá-la de rascunho a concluída, conferindo que cada
botão só aparece para quem pode executá-lo.

**Acceptance Scenarios**:

1. **Given** uma solicitação em rascunho, **When** o operador a publica, **Then** ela passa a
   `ABERTA`.
2. **Given** uma solicitação em rascunho, **When** alguém tenta concluí-la direto, **Then** a
   transição é recusada — pular a execução não é caminho.
3. **Given** uma solicitação com etapa pendente, **When** o operador tenta concluí-la, **Then** a
   conclusão é recusada e nenhum evento é publicado.
4. **Given** uma solicitação concluída, **When** alguém tenta reabri-la, **Then** a transição é
   recusada: concluída é terminal.
5. **Given** um usuário que pode **editar** mas não pode **concluir**, **When** ele envia um `PUT`
   declarando a situação `CONCLUIDA`, **Then** a situação **não muda**.

---

### User Story 3 - Contar as solicitações concluídas de cada parceiro (Priority: P2)

Ao concluir uma solicitação, o contador do parceiro sobe. Ninguém digita esse número.

**Why this priority**: é informação de gestão, não de operação — o sistema funciona sem ela, mas ela
é o que torna a listagem de parceiros útil para decidir.

**Independent Test**: concluir uma solicitação e conferir que o contador do parceiro subiu em um.

**Acceptance Scenarios**:

1. **Given** um parceiro com 2 solicitações concluídas, **When** uma terceira é concluída, **Then**
   o contador passa a 3.
2. **Given** um parceiro excluído logicamente, **When** uma solicitação dele é concluída, **Then** a
   conclusão acontece normalmente e o contador não é atualizado.

---

### User Story 4 - Preservar o sigilo da comissão (Priority: P2)

O valor da comissão é informação de gestão. Quem não tem a permissão específica enxerga o parceiro
inteiro, menos esse número — na tela, na API e na trilha de auditoria.

**Why this priority**: é a única regra da rodada cuja violação não gera erro nenhum. Ela só aparece
quando alguém lê o que não devia.

**Independent Test**: abrir o mesmo parceiro com dois perfis e comparar o que cada um recebe.

**Acceptance Scenarios**:

1. **Given** um usuário sem `VISUALIZAR_COMISSAO`, **When** ele consulta um parceiro pela API,
   **Then** o campo vem nulo.
2. **Given** o mesmo usuário, **When** ele lista parceiros, **Then** o campo vem nulo em **todas** as
   linhas.
3. **Given** o mesmo usuário, **When** ele salva o parceiro com o campo nulo, **Then** o valor
   gravado **permanece** — a leitura negada não vira escrita.
4. **Given** uma alteração de comissão, **When** alguém consulta a trilha de auditoria, **Then** a
   trilha registra **que** o campo mudou, e não para quanto.

---

### Edge Cases

- Um `PUT` que traga a situação é aceito, mas a situação é ignorada — não é erro, é um campo somente
  leitura.
- Cancelar sem motivo é recusado: o motivo é o que permite responder depois por que a solicitação
  morreu.
- Excluir solicitação em andamento ou concluída é recusado. A concluída já publicou um evento, e não
  há como desfazê-lo.
- Endereço fora do Brasil dispensa UF e CEP, e exige cidade, bairro e logradouro como qualquer outro.
- Se a aplicação cair entre a conclusão e o listener, o contador fica defasado. É aceitável porque o
  número é recalculável a partir das solicitações — e está declarado.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema MUST gravar parceiro, pessoa, contato e endereço numa **única transação**.
- **FR-002**: O sistema MUST acumular todas as violações de validação e devolvê-las de uma vez.
- **FR-003**: O nome do parceiro MUST ser único **entre os registros ativos**; um nome liberado por
  exclusão lógica pode ser reutilizado.
- **FR-004**: A exclusão de parceiro MUST ser lógica e MUST arrastar pessoa, contato e endereço.
- **FR-005**: A busca de parceiros MUST ser paginada, com filtros combináveis de nome e CPF.
- **FR-006**: Toda solicitação MUST nascer em `RASCUNHO`, sem exceção e sem depender do que o
  chamador enviar.
- **FR-007**: A situação MUST avançar **apenas** pelos endpoints de transição, cada um com a sua
  própria ação de autorização. Um `PUT` de cadastro MUST ignorar a situação.
- **FR-008**: As transições válidas MUST ser: `RASCUNHO → ABERTA`, `ABERTA → EM_ANDAMENTO`,
  `EM_ANDAMENTO → CONCLUIDA`, e `→ CANCELADA` a partir de qualquer não terminal.
- **FR-009**: A conclusão MUST ser recusada enquanto houver etapa pendente.
- **FR-010**: O cancelamento MUST exigir motivo.
- **FR-011**: A conclusão MUST publicar um evento de domínio, consumido pelo domínio `parceiro`. O
  domínio `solicitacao` MUST NOT escrever no parceiro diretamente.
- **FR-012**: A exclusão MUST ser recusada para solicitação em andamento ou concluída.
- **FR-013**: O valor da comissão MUST ser omitido para quem não tem `VISUALIZAR_COMISSAO` — na
  leitura, na busca **e** na gravação.
- **FR-014**: A trilha de auditoria MUST registrar a alteração da comissão **sem** gravar os valores.
- **FR-015**: Toda alteração de entidade de negócio MUST ser registrada automaticamente pela camada
  de persistência, nunca por chamada do serviço de domínio.

### Key Entities

- **Parceiro**: cadastro de referência. Nome, descrição, comissão (sigilosa) e o contador de
  solicitações concluídas. Referencia uma Pessoa, um Contato e um Endereço.
- **Solicitação**: título, descrição, situação, motivo de cancelamento e data de conclusão.
  Pertence a um Parceiro e tem etapas ordenadas.
- **Etapa da solicitação**: nome, ordem e marca de concluída. A ordem é dado, não posição na lista.
- **Pessoa / Contato / Endereço**: dados normalizados compartilhados, com dono próprio.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Um parceiro completo é cadastrado em menos de 2 minutos, num único envio ao servidor.
- **SC-002**: Um formulário com N erros devolve as N mensagens no primeiro submit — nunca uma por
  vez.
- **SC-003**: Nenhuma transição inválida é aceita: os 100% dos pares (origem, destino) fora da
  máquina de estados são recusados, e há teste para cada um.
- **SC-004**: Um perfil sem `VISUALIZAR_COMISSAO` não obtém o valor por nenhum dos três caminhos
  (leitura, busca, trilha), verificado por teste em cada um.
- **SC-005**: Nenhum endpoint mapeado fica sem declaração de autorização — verificado por varredura
  que falha, não por revisão.
- **SC-006**: Nenhuma rota guardada do frontend fica sem ação declarada — mesma disciplina, mesma
  varredura.

## Assumptions

- O provedor de identidade é o Keycloak, e os perfis são grupos dele. O sistema nunca atribui role a
  usuário: coloca-o no grupo.
- Três perfis bastam para o molde — administrador, operador e consulta. Um projeto derivado troca a
  lista e a matriz de permissões juntas.
- A entrega de eventos é em memória. Aceito porque a única consequência é um contador recalculável.
- Não há upload de arquivo nesta rodada. A infraestrutura de S3 existe e está fechada.
- O motivo do cancelamento chega por um campo simples; um fluxo de aprovação de cancelamento está
  fora de escopo.
