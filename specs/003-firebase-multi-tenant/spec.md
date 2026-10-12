# Feature Specification: molde Firebase multi-tenant GEMS

**Feature**: 003-firebase-multi-tenant · **Created**: 2026-10-07
**Status**: plano aprovado; implementação e homologação pendentes.
**Input**: plano completo revisto independentemente em PLAN_REV3_20261007 (PASS), seguido da instrução literal do usuário: "Implement the plan."
**Branch**: preservada; esta preparação documental não cria branch.

## User Scenarios & Testing

### US1 — Identidade e acesso explícitos (P1)

O operador central pré-cadastra uma pessoa; ela entra com Google ou e-mail/senha, conhece os vínculos permitidos e acessa somente ações vigentes. Tenant admin administra apenas memberships e grants do próprio tenant, dentro de sua delegação efetiva.

**Independent Test**: conta pré-cadastrada entra; desconhecida, suspensa ou sem vínculo é negada. Encerrar um grant impede o próximo request após o commit sem retirar outros vínculos legítimos.

**Acceptance Scenarios**:

1. Dado cadastro elegível pendente, Google verificado vincula exatamente uma identidade sem criar acesso por coincidência de e-mail.
2. Dado UID já conhecido historicamente, identidade encerrada nunca reabre por fallback de e-mail.
3. Dado cadastro por senha, criação usa UID planejado e envio nativo de definição/reset; nenhum segredo entra em armazenamento ou logs do produto.
4. Dado tenant admin, concessão direta ou indireta além de suas ações efetivas delegáveis é recusada; identidade e estado global permanecem centrais.

### US2 — Isolamento comprovado (P1)

Usuário alpha-only não acessa beta alterando o seletor, header ou payload. Usuário alpha+beta alterna com limpeza dos dados anteriores e espera o novo contexto antes de operar.

**Independent Test**: registros com a mesma PK nos dois tenants nunca se misturam; token atrasado, resposta antiga, logout ou falha na troca não reutilizam contexto anterior.

**Acceptance Scenarios**:

1. Dado acesso alpha-only, spoof beta falha sem abrir conexão para dados beta.
2. Dado usuário alpha+beta, troca durante await de token cancela Alpha; payload Alpha jamais chega a Beta.
3. Dada exceção/thread reutilizada, request seguinte não herda usuário, tenant ou actor.
4. Dado tenant ainda não pronto ou atrasado em migration, nenhum tráfego de negócio é liberado.

### US3 — Factory reutilizável (P2)

Equipe cria produto a partir de um molde com identidade GEMS AI, exemplos mínimos e limpeza explícita por CLI. SDKs publicados permitem consumo externo independente do checkout de desenvolvimento.

**Independent Test**: consumidor isolado instala artefatos publicados; cópia descartável renomeada e limpa compila, autentica e mantém fundamentos de segurança/tenancy.

**Acceptance Scenarios**:

1. Dado manifesto fechado, dry-run mostra somente exemplos; apply em árvore limpa remove-os integralmente sem tocar banco ou volumes.
2. Dado snapshot oficial, CLI comprova origem/integridade; nenhuma regra é editada à mão para passar gate.
3. Dado projeto real isolado de homologação, dois testers completam Google, senha, reset, revogação e disable; hosting permanece decisão posterior.

### US4 — Acesso com identidade visual G&Ms (P1)

Pessoa pré-cadastrada encontra uma tela própria de login, em português, reconhecível como G&Ms Soluções Tecnológicas e coerente com a referência pública https://gems.tec.br/. Entra com Google ou e-mail/senha e recupera a senha sem perder as fronteiras de elegibilidade e seleção de empresa.

**Independent Test**: nos tamanhos 320, 768 e 1440 px, concluir os fluxos por teclado, sem overflow horizontal; verificar feedback acessível, entrada elegível, negação genérica e contexto validado antes do retorno.

**Acceptance Scenarios**:

1. Dado visitante sem sessão, a rota pública de login apresenta marca, Google, e-mail/senha e recuperação nativa; não oferece autocadastro nem auto-admin.
2. Dado submit pendente, exibir loading e impedir envio duplicado; erro é anunciado, tem mensagem em português e não revela existência/elegibilidade da conta.
3. Dada autenticação válida, aguardar dados da conta e seleção/contexto de tenant válido antes de liberar negócio; conta não elegível é negada sem vazamento.
4. Dado retorno solicitado, aceitar somente caminho interno permitido; sessão ativa evita novo login após conta/contexto válidos. Logout e respostas atrasadas respeitam invalidação da sessão.
5. Dado teclado/leitor de tela, campos têm labels, foco visível e erros anunciados; contraste mínimo é 4,5:1 para texto normal e 3:1 para texto grande.

### Edge Cases

UID histórico; e-mail ambíguo; provider encerrado; projeto/emissor divergente; concessão no mesmo instante do encerramento; perfil/grupo de outro tenant; catálogo com drift; outbox interrompida; enable remoto antigo após disable novo; provisionamentos concorrentes; falha de troca de contexto; symlink/junction na limpeza.

## Requirements

### Functional Requirements

- **FR-001**: Acesso nasce de pré-provisionamento central e identidade comprovada, com Google e e-mail/senha, sem auto-admin no primeiro login.
- **FR-002**: Identidade externa e proprietário são imutáveis; vínculo histórico não permite apropriação nem reativação implícita.
- **FR-003**: Usuário, provider, identidade, tenant, membership, grupo, perfil e todos os grants precisam estar vigentes para autorizar.
- **FR-004**: Ações efetivas são a união deduplicada de caminhos vigentes, limitada ao catálogo e ao escopo; frontend não é autoridade.
- **FR-005**: Tenant admin só modifica o próprio membership/grants e concede subconjunto da própria autorização delegável, inclusive por perfil/grupo; ação global central distinta cria o primeiro administrador da empresa.
- **FR-006**: Encerramento torna-se efetivo no próximo request após commit; request já autorizado pode concluir com seu snapshot.
- **FR-007**: Desativação global bloqueia localmente antes da chamada externa e persiste comandos recuperáveis; membership encerrado não desabilita a conta externa.
- **FR-008**: Tráfego empresarial exige identidade/membership/tenant vigentes, schema pronto e revisão mínima aplicada, sem fallback de tenant.
- **FR-009**: Seleção de tenant não autoriza acesso; troca/logout/novo UID invalidam operações pendentes e dados da UI.
- **FR-010**: Auditoria empresarial é isolada por tenant; segurança/global possuem trilha separada sem senhas, tokens, chaves ou links de ação.
- **FR-011**: Exemplo CRUD mínimo e demonstrações de acesso público/global/tenant/leitura/escrita/campo são integralmente removíveis por CLI segura.
- **FR-012**: SDKs são publicados e testados por consumidores isolados antes da integração; molde mantém assinatura GEMS AI canônica.
- **FR-013**: A suíte automatizada, inclusive o CI, usa o projeto Firebase real de testes `sample-firebase-multi-tenant` via ADC, com o secret `FIREBASE_TEST_SERVICE_ACCOUNT_JSON` materializado em arquivo temporário no runtime; usuários sintéticos usam namespace por execução (prefixo + run id no e-mail/UID), com limpeza garantida e varredura de órfãos restrita ao namespace; o PostgreSQL permanece descartável (Testcontainers); a indisponibilidade do provedor é testada por fault injection local determinística, nunca derrubando o Firebase compartilhado; o emulador é mantido apenas como opção demo/offline do Compose local; Google OAuth real e entrega de reset por e-mail ficam fora da automação, cobertos por verificação humana na homologação isolada (decisão 6B de 2026-10-09).
- **FR-014**: Registrar custo medido de 100/1000 empresas e migrations; escala de referência não promete capacidade ou SLA.

- **FR-015**: Oferecer login próprio e responsivo com a identidade visual G&Ms observada em gems.tec.br, conteúdo em português, Google, e-mail/senha e recuperação nativa. Garantir acessibilidade, estados de carregamento/erro, prevenção de envio duplicado, retorno interno seguro e continuidade da seleção/contexto de tenant, sem autocadastro, auto-admin ou autoridade por claims; componentes usam a fachada local, sem chamadas diretas ao provedor/API.

### Key Entities

Usuário; identidade externa; provedor; empresa; membership; ação de escopo global/tenant; perfil; grupo; concessão com histórico; comando de ciclo de vida; evento de segurança; inventário de schema/revisão.

## Success Criteria

- **SC-001**: matriz negativa de autenticação/autorização/constraints/concorrência não permite nenhum acesso indevido.
- **SC-002**: cenários determinísticos de mesma PK, troca atrasada, resposta antiga e thread reutilizada preservam isolamento em todos os casos.
- **SC-003**: dois consumidores externos e uma réplica limpa comprovam reutilização sem mecanismos locais duplicados.
- **SC-004**: gates de código novo e preservado têm baseline zero; provas de publicação e homologação são registradas, nunca inferidas de build local.

- **SC-005**: Login aprovado nos tamanhos 320/768/1440 px sem overflow horizontal, operável por teclado com foco/labels/erros anunciados e contraste 4,5:1 normal/3:1 grande; Google, senha e reset passam cenários elegível/negado/loading/logout/resposta antiga/retorno interno, sem liberar negócio antes de contexto válido.

## Assumptions e limites

Um produto atende como referência 100–1000 empresas; um projeto de identidade por produto/ambiente e um projeto por banco. Exemplos servem à forma de código, não ao domínio de produto. Meduc é referência de tenancy, sem migração de Meduc/Apoia/ADACI. Hosting, MFA, Firestore, cache, roles em custom claims, SMTP/Gmail API e tenant de Identity Platform por empresa ficam fora deste escopo.

## Extensão documental — 2026-10-08

Origem literal: usuário solicitou "Acrescente entre as necessidades, uma tela de login customizada seguindo os padrões da G&Ms Soluções tecnológicas em gems.tec.br". US4/FR-015/SC-005 registram a necessidade e a execução autorizada pela instrução atual abaixo; não declaram mockup aprovado, implementação concluída ou publicação do sample. A autorização histórica do plano anterior permanece separada em [roteiro.md](roteiro.md). Estado atual e lacunas: [development-status-20261008.md](development-status-20261008.md).

## Correções contratuais P0 — 2026-10-08

Angular core 3.0.0 e adapter 1.0.0 publicados no commit `49b5eb94d684661aac13a2efb4a8bba3db7e5bf0`. Publicação PASS reconfirmada em D2; consumers npm remotos isolados com compiler/runtime PASS histórico recuperado, não reexecutados em D2/P0. Fontes: receipt `C:/Users/gabri/AppData/Local/Temp/gems-firebase-20261007/angular-release-result.json`, review independente `angular-state.json` v17, `.tooling/astra-sample-development-20261008/remote-evidence.json` e `readiness-report.md`. CI 37704998108 e releases 37705125391/37705315746 PASS no receipt. Shasums core `0d78e574112e84bdc867f7a0a374f8f555855700`, adapter `5f67626896b8753afd49a04341e4720b3ac31a77`. T014–T016 continuam abertas para adjudicação root; adoção T017 pendente.

Autoridade atual recebida pelo root: **"Prossiga com o desenvolvimento"**, incluindo US4/FR-015/SC-005 e T033/T034. Esta instrução é distinta de PLAN_REV3_20261007; não constitui assinatura de mockup nem homologação.

T001 permanece aberta para revisão independente integral dos bytes finais. Nenhum gate funcional ou tarefa SDK é fechado por P0. Constituição 3.0.0, login G&Ms e invariantes Firebase/SQL/tenant/lifecycle preservados.
