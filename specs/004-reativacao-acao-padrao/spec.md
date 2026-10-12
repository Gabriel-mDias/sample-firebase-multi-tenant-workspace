# Feature Specification: Reativação devolve a ação padrão de conta

**Feature Branch**: `feature/004-reativacao-acao-padrao`

**Created**: 2026-10-12

**Status**: Draft

**Input**: User description: "Ao concluir a reativação de um usuário suspenso, o sistema devolve automaticamente apenas a ação global padrão de conta (CONSULTAR_PROPRIA_CONTA), se ela existir no catálogo e não houver concessão vigente; vínculos e demais concessões continuam encerrados. Reativação obsoleta não devolve nada. Investigar o evento de confirmação de comando entregue duas vezes. Mudança no SDK de autenticação, com release e adoção no sample." Decisões de entrada em [roteiro.md §2](roteiro.md#2-decisões-de-entrada).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Usuário reativado volta a entrar e ver a própria conta (Priority: P1)

Um operador central suspende um usuário e depois o reativa. Quando a reativação se conclui, o usuário
entra no sistema, Google ou senha, e vê a própria conta, sem que nenhum operador precise conceder
de novo a ação básica de conta. Ele ainda não vê nenhuma empresa: os vínculos encerrados pela
suspensão continuam encerrados.

**Why this priority**: É o defeito observado na homologação da 003. Sem isso, a conta reativada fica
vigente, mas inutilizável, e o login falha sem mensagem que explique o motivo.

**Independent Test**: Suspender um usuário de teste, processar os comandos de identidade, reativá-lo,
processar de novo e entrar com ele. A consulta da própria conta precisa responder com sucesso, e a
lista de empresas precisa vir vazia.

**Acceptance Scenarios**:

1. **Given** um usuário vigente com a ação padrão de conta e um vínculo com uma empresa, **When** ele é
   suspenso e depois reativado, e a reativação é confirmada pelo provedor de identidade, **Then** ele
   tem exatamente uma concessão vigente da ação padrão de conta e nenhum vínculo vigente com empresa.
2. **Given** o mesmo usuário reativado, **When** ele faz login, **Then** a consulta da própria conta
   responde com sucesso e o seletor de empresas informa que não há empresa disponível.
3. **Given** um usuário com outras ações globais além da padrão, **When** ele é suspenso e reativado,
   **Then** só a ação padrão volta; as demais continuam encerradas.

---

### User Story 2 - Reativação obsoleta ou anulada não devolve acesso (Priority: P1)

Se a reativação for superada por uma nova suspensão antes de ser confirmada, ou se a confirmação
chegar para uma versão que já não é a atual, nada é devolvido.

**Why this priority**: Devolver acesso numa reativação anulada reabriria uma conta que o operador
decidiu bloquear. É uma garantia de segurança do mesmo nível da história 1.

**Independent Test**: Solicitar a reativação, suspender de novo antes de processar e então processar
os comandos. O usuário continua suspenso e sem nenhuma concessão vigente.

**Acceptance Scenarios**:

1. **Given** uma reativação pendente, **When** o usuário é suspenso de novo antes da confirmação,
   **Then** a confirmação tardia não torna o usuário vigente e não cria concessão.
2. **Given** uma confirmação de reativação de versão antiga, **When** ela é processada, **Then** é
   tratada como obsoleta e nenhuma concessão é criada.

---

### User Story 3 - Confirmação de comando registrada uma vez (Priority: P3)

Quem acompanha o log e a auditoria do processamento de identidade vê cada confirmação de comando uma
única vez, ou entende, por documentação, por que ela aparece repetida e que o efeito não se repete.

**Why this priority**: É uma observação da homologação, sem falha funcional observada. Fecha a dúvida
operacional.

**Independent Test**: Processar um comando de identidade e contar os eventos de confirmação com o
mesmo identificador no log e na trilha de eventos.

**Acceptance Scenarios**:

1. **Given** um comando processado com sucesso, **When** se conta o evento de confirmação daquele
   comando, **Then** há um único registro, ou a duplicidade está explicada na documentação do SDK com
   a prova de que o efeito não se repetiu.

### Edge Cases

- A ação padrão foi retirada do catálogo do consumidor entre a suspensão e a reativação: a
  reativação conclui e nenhuma concessão é criada.
- O usuário já tem uma concessão vigente da ação padrão no momento da confirmação, por exemplo
  concedida à mão: nenhuma concessão duplicada é criada.
- A confirmação da reativação é processada duas vezes, por reentrega ou nova tentativa: o resultado
  é uma única concessão.
- A reativação de um usuário que nunca foi suspenso, ou que já está vigente, não altera concessões.
- As concessões encerradas pela suspensão não são reabertas nem reescritas; o histórico é preservado.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema MUST conceder automaticamente a ação global padrão de conta ao usuário no
  mesmo passo atômico em que a confirmação da reativação da versão atual o torna vigente.
- **FR-002**: O sistema MUST conceder a ação padrão somente se ela fizer parte do catálogo de ações
  globais do consumidor naquele momento.
- **FR-003**: O sistema MUST NOT criar uma segunda concessão vigente da ação padrão se já houver uma.
- **FR-004**: O sistema MUST NOT reabrir nem devolver vínculos com empresas, perfis, papéis de
  empresa, grupos ou outras ações globais encerrados pela suspensão.
- **FR-005**: O sistema MUST NOT conceder nada quando a confirmação de reativação for obsoleta,
  superada ou anulada por nova suspensão.
- **FR-006**: O sistema MUST preservar as concessões encerradas como histórico; a devolução é uma
  concessão nova, com início no momento da reativação.
- **FR-007**: O sistema MUST registrar na trilha de eventos que a ação padrão foi concedida pela
  reativação, distinguível de uma concessão manual.
- **FR-008**: A causa do evento de confirmação de comando repetido MUST ser diagnosticada. Se for
  emissão duplicada, ela MUST ser corrigida; se for entrega repetida de efeito idempotente, ela MUST
  ser documentada com a prova.
- **FR-009**: A mudança MUST ser publicada como nova versão menor do SDK de autenticação, com a
  documentação do SDK atualizada, e adotada pelo sample com prova ponta a ponta contra o provedor de
  identidade real de testes.

### Key Entities

- **Usuário**: identidade central com vigência e versão de identidade; a reativação o torna vigente
  de novo.
- **Concessão global**: ação central concedida a um usuário, com início e fim de vigência. A ação
  padrão de conta é uma delas.
- **Comando de identidade**: pedido versionado ao provedor (desabilitar, revogar, habilitar), cuja
  confirmação conclui a suspensão ou a reativação.
- **Evento de segurança**: registro auditável das mudanças de identidade e de concessão.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% dos usuários reativados conseguem entrar e ver a própria conta na primeira
  tentativa após a confirmação, sem ação manual de operador.
- **SC-002**: Nenhum usuário reativado recebe de volta vínculo com empresa ou ação além da padrão
  (0 ocorrências nos cenários de teste).
- **SC-003**: Nenhuma reativação obsoleta ou anulada resulta em acesso (0 ocorrências nos cenários de
  teste).
- **SC-004**: Cada usuário reativado tem exatamente 1 concessão vigente da ação padrão, mesmo com a
  confirmação processada mais de uma vez.
- **SC-005**: A dúvida do evento de confirmação repetido fica fechada: corrigida ou documentada com
  prova.

## Assumptions

- A ação padrão de conta é a mesma que o cadastro concede hoje (`CONSULTAR_PROPRIA_CONTA`, quando
  presente no catálogo); nenhuma ação nova é criada.
- O fluxo de suspensão e de reativação, a fila de comandos e o processador de identidade da 003
  continuam como estão; a rodada só acrescenta a devolução e o diagnóstico.
- Não há migração de dados: no sample só existem dados de teste, e o usuário afetado na homologação
  já foi restaurado à mão.
- A publicação do SDK depende de tag aprovada pelo usuário; o sample só adota depois da publicação.
- A exceção vale apenas para a ação padrão; a regra da 003 de não reabrir vínculos e concessões
  encerrados continua valendo para todo o resto.
