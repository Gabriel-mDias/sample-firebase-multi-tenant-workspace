# Feature Specification: Conformidade GEMS

**Feature Branch**: `feature/conformidade-gems`
**Created**: 2026-09-14
**Status**: Em revisão — aguardando decisões humanas de clarificação
**Input**: Conformar o sample às definições GEMS atuais e às SDKs publicadas, preservando o comportamento funcional e eliminando a dívida Java congelada.

## User Scenarios & Testing

### User Story 1 — Molde sem dívida Java (Priority: P1)

Como mantenedor do molde, quero que todo exemplo backend já cumpra as regras vigentes para que um projeto novo não copie dívida técnica nem dependa de listas de tolerância.

**Why this priority**: O sample existe para ser copiado; uma tolerância no backend vira tolerância em cada projeto derivado.

**Independent Test**: Uma verificação completa encontra zero desvio de formato, arquitetura, nomes de teste ou supressão de dívida no backend.

**Acceptance Scenarios**:

1. **Given** o código backend completo, **When** os gates executam, **Then** não existe desvio Java tolerado nem lista de dívida residual.
2. **Given** um novo desvio proposital, **When** o gate executa, **Then** o build reprova e nomeia arquivo e regra.
3. **Given** um campo novo entre entrada, domínio e resposta, **When** ele não recebe decisão explícita de conversão, **Then** a compilação reprova.

### User Story 2 — Auditoria íntegra e consultável (Priority: P1)

Como responsável por conformidade, quero continuar identificando de forma estável quem fez cada mudança, em qual operação correlacionada e quais campos mudaram, sem trilha parcial ou forjável.

**Why this priority**: Perder identidade, correlação ou atomicidade é perda de comportamento, mesmo que a tela continue abrindo.

**Independent Test**: Operações confirmadas, revertidas, sigilosas, técnicas e correlacionadas são verificadas no banco e pelos contratos de consulta antes e depois da migração.

**Acceptance Scenarios**:

1. **Given** uma alteração confirmada, **When** a trilha é consultada, **Then** ela contém operação, registro, identidade estável, nome/tipo do autor, correlação e campos alterados.
2. **Given** uma alteração revertida, **When** a transação termina, **Then** dado, efeitos e trilha estão ausentes.
3. **Given** um campo sigiloso alterado, **When** a trilha é lida, **Then** a mudança existe e os valores não foram armazenados.
4. **Given** o autor muda de login, é removido ou um login é reutilizado, **When** o histórico é filtrado pela identidade original, **Then** pessoas diferentes não são misturadas nem perdidas.
5. **Given** uma consulta de cadastro agregado ou período, **When** o usuário consulta, **Then** agregados aparecem e a data final é inclusiva.

### User Story 3 — Autorização e identidade preservadas (Priority: P1)

Como operador, quero que permissões e ciclo de vida de usuários continuem convergindo de forma idempotente e que nenhum endpoint fique sem classificação explícita.

**Why this priority**: Uma perda pode abrir recurso indevido, retirar acesso legítimo ou deixar conta externa divergente do cadastro local.

**Independent Test**: O catálogo tem uma origem, todos os endpoints são classificados, e criação, alteração, suspensão, grupos, compensações e indisponibilidade do provedor mantêm os resultados atuais.

**Acceptance Scenarios**:

1. **Given** um endpoint novo, **When** a cobertura executa, **Then** omissão, ambiguidade, perfil genérico ou ação inexistente reprovam.
2. **Given** uma ação ou perfil novo, **When** o provisionamento converge, **Then** ação, composição e grupo resultam da mesma fonte e ambientes existentes são atualizados.
3. **Given** um usuário criado, alterado, suspenso ou movido de perfil, **When** o fluxo termina, **Then** cadastro local e conta externa permanecem equivalentes.
4. **Given** falha após alteração externa compensável, **When** o fluxo aborta, **Then** o estado anterior é restaurado ou a falha prevista fica explícita.

### User Story 4 — Jornadas web conformes (Priority: P2)

Como usuário autenticado, quero continuar listando, criando, editando e visualizando parceiros e solicitações com autorização por ação, mensagens acessíveis e navegação previsível.

**Why this priority**: A conformidade só vale se preservar o comportamento visível das features de referência.

**Independent Test**: As oito rotas canônicas, menus, comandos, formulários, carga/erro e componentes compartilhados são exercitados com permissão concedida e negada.

**Acceptance Scenarios**:

1. **Given** uma ação ausente, **When** o usuário abre rota ou comando protegido, **Then** o acesso é negado consistentemente.
2. **Given** uma ação presente, **When** percorre lista, criação, edição e visualização, **Then** cada modo funciona e a visualização não grava.
3. **Given** um campo inválido tocado ou alterado, **When** o erro aparece, **Then** a primeira mensagem é anunciada de forma acessível.
4. **Given** navegação de formulário, **When** o usuário retorna ou avança, **Then** contexto efêmero é preservado sem estado de domínio obsoleto.

### User Story 5 — Gates finais verificáveis (Priority: P2)

Como product owner, quero evidência reproduzível de que a rodada zerou a dívida Java e reduziu o frontend ao mínimo medido para decidir cada residual, sem aceite concedido pelo agente.

**Why this priority**: Uma contagem menor ainda pode esconder dívida; o encerramento deve provar o estado nominal.

**Independent Test**: O fechamento compara inventários completos, remove tolerâncias Java e apresenta cada residual frontend com arquivo, regra, motivo e alternativa.

**Acceptance Scenarios**:

1. **Given** o fechamento backend, **When** os gates executam, **Then** formato, fonte, arquitetura, nomes, supressões de dívida e ratchet estão zerados/removidos.
2. **Given** o fechamento frontend, **When** a medição executa, **Then** toda ocorrência corrigível saiu e cada residual está submetido ao product owner.
3. **Given** residual não aceito, **When** o gate humano é avaliado, **Then** a rodada permanece aberta.

### Edge Cases

- Atualização sem mudança efetiva não cria operação vazia; exclusão lógica aparece como exclusão de negócio.
- Operação sem usuário registra origem técnica sem impedir trabalho legítimo.
- Autores homônimos ou logins reutilizados não compartilham histórico.
- Migração para diante de dado incompatível; não trunca nem descarta silenciosamente.
- Falha do provedor durante compensação não vira sucesso.
- Realm preserva clients, mappers, usuários, grupos e defaults não derivados.
- Residual frontend só permanece depois de medido e aceito por humano.

## Requirements

### Functional Requirements

- **FR-001**: O sistema MUST preservar parceiro, solicitação, localidade, normalização, usuário, consulta de auditoria e upload S3.
- **FR-002**: O backend MUST consumir versão publicada da Java GEMS SDK declarada uma vez, com alcance medido antes/depois.
- **FR-003**: O frontend MUST consumir Angular GEMS SDK 2.1.0 com overrides; toda invocação operacional e instrução obsoleta de `--legacy-peer-deps` no escopo aprovado MUST sair.
- **FR-004**: Os seis serviços medidos MUST usar conversão compilada estrita; campo não tratado MUST reprovar.
- **FR-005**: Os processadores de conversão MUST coexistir com Lombok.
- **FR-006**: Parceiro e solicitação MUST usar transação Spring com `rollbackFor` e prova integrada por proxy, banco real e exceção checked após escrita.
- **FR-007**: O teste de rollback MUST afirmar ausência de dado, efeitos e trilha e reprovar sem `rollbackFor`, sem rollback do próprio teste.
- **FR-008**: `CampoAlteradoRepository`, `EstadoRepository`, `PaisRepository` e `EtapaSolicitacaoRepository` MUST adotar o fragmento GEMS.
- **FR-009**: `LocalidadeController` e `CepController` MUST estender `BaseController`.
- **FR-010**: `ProvedorExternoExceptionHandler`, `AccessDeniedExceptionHandler`, `IdentityProviderExceptionHandler` e `RequisicaoInvalidaExceptionHandler` MUST sair com imports, testes e tipos órfãos na mesma alteração.
- **FR-011**: Os 177 nomes medidos MUST assumir `deveX_quandoY` e o inventário MUST chegar a zero.
- **FR-012**: Checkstyle, fonte e arquitetura Java MUST chegar a zero; 22 supressões de dívida e `ratchetFrom` MUST sair.
- **FR-013**: Escrita local de auditoria só pode sair na mesma alteração atômica que schema, read adapter, ator, configuração e writer substituto verdes.
- **FR-014**: Auditoria MUST preservar identidade estável, nome/tipo, correlação, commit/rollback, sigilo, operação, agregados, busca e data inclusiva.
- **FR-015**: Sem suporte publicado a FR-014, a implementação MUST parar até release compatível ou emenda humana explícita; equivalência não será presumida.
- **FR-016**: O banco MUST preservar dados históricos e parar diante de valor incompatível.
- **FR-017**: O backend MUST derivar catálogo do enum e cobrir todos os endpoints publicados.
- **FR-018**: Toda rota de negócio MUST ter classificação única e ação concreta existente.
- **FR-019**: O catálogo frontend MUST ser derivado/verificado da fonte backend.
- **FR-020**: A adoção Keycloak MUST preservar criar, ler, alterar, habilitar, suspender, remover/compensar, consultar grupos, associar membros, criar roles e compor perfis.
- **FR-021**: Sem suporte publicado a FR-020, a implementação MUST parar até release compatível ou emenda humana; nenhum cliente duplicado será recriado.
- **FR-022**: Passagem ao realm declarativo MUST ter expansão aprovada e tocar só seções derivadas, preservando restante.
- **FR-023**: Local, staging e produção MUST manter realm único por one-shot saudável, cache desligado e dependências atuais.
- **FR-024**: O frontend MUST usar `isAuthorized` por ação e falhar fechado.
- **FR-025**: Parceiro e solicitação MUST expor `list`, `form`, `form/:id`, `view/:id`, com oito rotas testadas.
- **FR-026**: Compartilhados MUST residir em `shared/`; componente local só sai após equivalência provada.
- **FR-027**: Formulários MUST manter validação, acessibilidade e modos; stores/navegação MUST respeitar limites de estado.
- **FR-028**: Frontend MUST chegar ao mínimo medido; cada residual MUST ter arquivo, regra, motivo, alternativa e aceite humano.
- **FR-029**: O agente MUST manter decisões residuais pendentes; não assina exceção, baseline ou aceite.
- **FR-030**: O fechamento MUST manter verdes 204 testes backend e 21 arquivos/118 testes frontend aplicáveis, mais novas provas.
- **FR-031**: D-P01 e D-P02 MUST ser atendidas por uma release publicada da Java GEMS SDK; branch,
  snapshot, clone local e adapter no sample não liberam a adoção.
- **FR-032**: A release produtora MUST preservar a API pública 3.1.0 por evolução aditiva. O plano
  classifica a entrega como **3.2.0 MINOR**; qualquer quebra descoberta reabre o portão para decisão
  MAJOR antes de código adicional.
- **FR-033**: `gems-auditing` MUST aceitar identidade estável do autor e correlação por pontos de
  extensão públicos, mantendo nome/tipo e o comportamento técnico atual quando os novos providers
  não existirem.
- **FR-034**: `gems-keycloak-admin` MUST expor, sem remover o gateway atual, leitura/snapshot,
  atualização, habilitação, suspensão, exclusão, compensação de falha na criação, consulta e
  restauração de grupos, criação idempotente e composição de realm roles.
- **FR-035**: C11 só pode iniciar após a publicação compatível de FR-034; C12 depende de C11 verde e
  da publicação compatível de FR-033; C13 continua dependendo de C11 e C12 verdes.
- **FR-036**: A release só é consumível quando PR/CI/merge/publicação estiverem verdes, o BOM e todos
  os POMs apontarem para 3.2.0, e `gems-bom`, `gems-auditing`, `gems-keycloak-admin` e
  `gems-security-authorization` resolverem do GitHub Packages em consumidor limpo.

### Key Entities

- **Registro de auditoria**: operação imutável, registro afetado, autor estável, origem, correlação e instante.
- **Mudança auditada**: campo, valores permitidos e sigilo.
- **Ação do sistema**: unidade de autorização com nome, descrição e escopo.
- **Perfil de atuação**: composição de ações concedida a grupo; não protege endpoint.
- **Catálogo derivado**: representação gerada para consumidores.
- **Residual frontend**: ocorrência não removida com evidência e decisão humana individual.

## Success Criteria

- **SC-001**: Zero endpoint sem classificação única e ação concreta válida.
- **SC-002**: As 21 ações atuais produzem catálogos consistentes sem segunda lista manual.
- **SC-003**: 100% dos cenários auditados preservam identidade/correlação no commit; zero dado, efeito ou trilha sobrevive ao rollback.
- **SC-004**: 100% dos campos sigilosos exercitados armazenam valores anterior/novo nulos.
- **SC-005**: Seis serviços com zero conversão reflexiva e zero campo ignorado.
- **SC-006**: Checkstyle, fonte, arquitetura e nomes Java em zero; 22 supressões de dívida e ratchet removidos.
- **SC-007**: Frontend no mínimo medido; 100% dos residuais nominalmente justificados e aceitos ou pendentes de humano.
- **SC-008**: Oito rotas passam nos cenários permitido, negado e modo correto.
- **SC-009**: Gates mantêm pelo menos 204 testes backend e 21/118 frontend aplicáveis, mais novas provas, verdes.
- **SC-010**: A Java GEMS SDK 3.2.0 publicada preserva compilação do contrato 3.1.0, passa no reator
  inteiro e reprova as mutações de identidade/correlação, ciclo de usuários, compensação e roles.
- **SC-011**: O sample não contém adapter de auditoria ou Keycloak criado para cobrir lacuna da SDK;
  as classes locais só saem nos commits consumidores atômicos de C11 e C12.

## Clarifications decididas no portão de 2026-09-14

- **D-P01 — Auditoria**: preservar identidade estável e correlação; adoção bloqueada até release SDK compatível publicada.
- **D-P02 — Keycloak**: preservar ciclo atual completo e roles/compostas; substituição bloqueada até release SDK compatível publicada.
- **D-P03 — Ambiente**: não aplicável; o realm e os ambientes atuais permanecem inalterados.
- **T004 — Operação**: autorizada a correção nominal de `.github/workflows/ci.yml`, `.github/dependabot.yml` e `README.md`.
- **D-P04 — Residual frontend**: decisão mantida para depois da medição; o agente não antecipa aceite.

## Assumptions

- Java SDK 3.1.0 (`137f18f`) e Angular SDK 2.1.0 (`e452ba0`) são os releases atuais aferidos.
- A aplicação permanece single tenant.
- Estado atual: 314 Checkstyle/22 supressões; 189 identidades (4 repositories, 2 controllers, 6 ordens, 177 nomes); 174 frontend; 204 testes backend; 21/118 frontend.
- D-P01 e D-P02 bloqueiam apenas a implementação dependente até as releases compatíveis; D-P04
  permanece para o portão posterior à medição.
