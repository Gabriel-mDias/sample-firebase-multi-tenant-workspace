<!--
SYNC IMPACT REPORT — v3.1.1
Emenda 2026-10-13 (3.1.0 → 3.1.1, PATCH: correção factual da tabela de stack). A linha Factory passa de GEMS AI 3.0.0, pin 51b94e3a, para 3.1.0, pin 909c69aa (tag v3.1.0, igual à main do gems-ai-sdk). Essa versão foi adotada pela rodada 006 (PR #12) e traz o formatter Java oficial no lugar do Spotless. Nenhum princípio muda. Decisão do usuário: "Revise para utilizar a versão mais recente, da main". As referências à "constituição 3.1.0" nos arquivos de agente foram atualizadas para 3.1.1 na mesma alteração. Nenhuma assinatura humana adicional foi criada.

Histórico anterior — v3.1.0
Emenda 2026-10-09 (3.0.0 → 3.1.0, MINOR: expansão material da orientação do Princípio IX e da tabela de stack; nenhum princípio removido nem redefinido de forma incompatível). Decisão do usuário 6B ("6 - opcao b"; contexto: "Esse projeto em si é um TESTE"; usar o ambiente Firebase entregue): suíte automatizada e CI usam o projeto Firebase real de testes sample-firebase-multi-tenant via ADC; emulador só opção demo/offline do Compose local; Google OAuth real e entrega de reset por e-mail são verificação humana. A ratificação vem da decisão do usuário nesta data; nenhuma assinatura humana adicional foi criada.

Histórico anterior — v3.0.0
Version change: 2.0.0 → 3.0.0 (MAJOR: identidade, tenancy, autorização e operação redefinidas).
Autorização existente: usuário "Implement the plan." após plano aprovado/revisão PLAN_REV3_20261007 PASS.
Registro literal em specs/003-firebase-multi-tenant/roteiro.md; nenhuma assinatura fabricada.
Princípios preservados: cinco módulos, direção Maven, Liquibase/ddlvalidate, UUID, validação
acumulada, REST/serviços GEMS, frontend modular/UI, testes/mutações e SDD antes de código.
Princípios redefinidos: identidade Firebase+SQL, vigência completa, tenant schemas/contexto,
auditoria empresarial, lifecycle durável, envio nativo, exemplos mínimos/cleanup, local+CI+homolog.
Histórico2.0.0 arquivado .specify/history/constitution-2.0.0.md (não executável).
Sincronização local: AGENTS raiz/backend/frontend/environments/specs; README/CODEX/CLAUDE/GEMINI;
skill local spec-driven/templates; rodada 003 e contrato Java SDK. Adapters de outras ferramentas
+pendentes de propagação em contrato sucessor; seus fatos locais precisam ser sincronizados.
Snapshot/adapters GEMS AI 3.0.0: exclusivamente CLI oficial; pendência administrativa de contrato
para gems-ai.json não habilita edição manual nem altera a autoridade3.0.0.
Gates produção/publicação/consumers/homolog: NOT_RUN nesta preparação documental.
-->

# Constituição do projeto

Este repositório é um **molde Firebase multi-tenant** para uma software factory eficiente com IA,
preservando a assinatura GEMS. O produto SaaS de referência atende 100–1000 empresas, sem promessa
de capacidade ou SLA. Um projeto Firebase por produto/ambiente; tenant empresarial é local ao banco.
Exemplos mínimos removíveis demonstram contratos, sem carregar regras de negócio pesadas.
O plano autorizado está em `specs/003-firebase-multi-tenant/roteiro.md`.

## Core Principles

### I. Fronteiras de Módulo Impostas pelo Reator Maven

Maven fatia o backend por **responsabilidade técnica** em cinco módulos, organizados num reator com
grafo de dependência estritamente unidirecional.

| Módulo Maven | Depende de | Conteúdo | Organização de pacotes |
| :--- | :--- | :--- | :--- |
| `app-model` | — | entidades JPA, DTOs, params de filtro, responses, enums | **camada-primeiro**: `br.com.gems.app.model.entity.<agrupamento>`, `model.dto`, `model.params`, `model.response` |
| `app-security` | — | Firebase auth adapter, ações, contexto e classificações de endpoint | `br.com.gems.app.security.*` |
| `app-events` | model | **somente POJOs de evento** (ver regra abaixo) | `br.com.gems.app.<dominio>.event` |
| `app-core` | model, security, events | regra de negócio | **domínio-primeiro**: `br.com.gems.app.<dominio>.{controller,service,repository}` |
| `app-deploy` | core | `AppApplication`, `application.yml`, changelogs Liquibase, fat jar | raiz |

Regras não negociáveis:

- A direção das dependências é imposta pelo **grafo do reator Maven em tempo de compilação**:
  `app-model` e `app-security` não dependem de ninguém, `app-events` depende só de `model`,
  `app-core` depende de `model`/`security`/`events`, e `app-deploy` depende de `core`. Um import na
  direção errada (por exemplo, `app-model` importando um tipo de `app-core`) **não compila**. Essa é
  a garantia de fronteira do projeto — e é mais forte do que qualquer verificação em tempo de teste,
  que só reprova depois de o código existir.
- Tudo que é **ambiente** — configuração, migração, empacotamento — mora exclusivamente em
  `app-deploy`. Um `application.yml` ou changeset que vaze para `core`/`model` inviabiliza
  reaproveitar o `app-core` no segundo executável. Esta é a razão de o módulo existir.
- **`app-events` contém somente os POJOs de evento.** Um listener que precise chamar service de
  negócio **não** pode morar ali: ele dependeria de `app-core`, e o reator recusaria o ciclo
  `events → core`. Esses listeners nascem em `app-core`, junto do domínio que reage
  (`br.com.gems.app.<dominio>.listener`).
- Comunicação entre domínios de negócio dentro de `app-core` distingue **acionar** de **consultar**:
  - **Acionar regra de negócio de outro domínio é por evento** (`ApplicationEventPublisher` +
    `@EventListener`/`@TransactionalEventListener` do próprio Spring), nunca chamada direta de
    service a service. O exemplo mínimo deve demonstrar comunicação sem dependência circular entre domínios.
  - **Consultar dado de outro domínio pelo repository dele é permitido.** São leituras síncronas, e
    um evento não responde pergunta — ele avisa que algo aconteceu. Forçá-las a passar por evento
    tornaria a resposta assíncrona sem ganho de isolamento nenhum.
  - O que continua proibido é **escrever** no dado de outro domínio, direta ou indiretamente:
    chamar o service dele, ou salvar pelo repository dele.
- Esta regra é **convenção revisada em code review** — não há gate automatizado que a verifique.
- Eventos de domínio recalculáveis podem usar Spring em memória. Comandos de ciclo de vida de
  identidade e eventos de segurança com consequência não descartável usam outbox persistida
  atomicamente com a mutação SQL, idempotência por UID e versão monotônica. SQL e Firebase não
  compartilham transação. Nunca persistir senha, token, chave ou action link.
- `spring-boot-maven-plugin` só pode estar ativo em `app-deploy`; nos demais módulos fica em
  `<pluginManagement>`. Ativo no POM pai, o goal `repackage` transformaria os jars de biblioteca em
  fat jars com `BOOT-INF/`, inutilizáveis como dependência.

### II. Persistência Declarada, Nunca Inferida

`ddl-auto: validate`. O schema muda **exclusivamente** por changeset Liquibase. Hibernate nunca cria
nem altera tabela.

- **Nomenclatura física:** tabelas e colunas em MAIÚSCULO, sem prefixo de tabela. Prefixos de coluna
  obrigatórios: `ID_` (chave primária e estrangeira), `NM_` (nome, sigla), `DS_` (descrição e textos
  livres), `CD_` (código, e-mail, CEP), `NR_` (número, telefone), `VL_` (valor monetário), `DT_`
  (data e timestamp), `FL_` (booleano).
- Chave primária UUID com `@GeneratedValue(strategy = GenerationType.UUID)`.
- Entidades de negócio MUST ter **soft delete**:
  `@SQLDelete(sql = "UPDATE TABELA SET DT_EXCLUSAO = NOW() WHERE ID_X = ?")` +
  `@SQLRestriction("DT_EXCLUSAO IS NULL")`, herdando `DT_CRIACAO` e `DT_EXCLUSAO` de
  `EntidadeAuditavel` (`br.com.gems.app.model.entity.base`). Tabelas puramente referenciais,
  carregadas por seed e nunca editadas pelo usuário, são isentas e não herdam `EntidadeAuditavel`.
- Toda entidade com `@SQLDelete` MUST estender `EntidadeAuditavel`. `@SQLDelete` apontando para uma
  coluna não mapeada é **defeito**, não estilo: o delete roda, não encontra a coluna e falha em
  runtime, não no build.
- Relacionamentos `ManyToOne` são sempre `LAZY`. Com `open-in-view: false`, todo relacionamento
  consumido fora da transação MUST estar coberto por `@NamedEntityGraph` ou carregado dentro do
  service — caso contrário estoura `LazyInitializationException` em produção, não no teste.
- **Schemas:** `security` é JDBC compartilhado opt-in; `administracao` guarda metadados globais;
  `auditoria` guarda trilha global separada; `modelo` é técnico para bootstrap/validação;
  `tenant_<alias>` contém negócio e auditoria empresarial sem schema fixo em anotação.
  Sem fallback para `public`: global JPA exige `TenantScope.global()` deliberado; tenant JPA só
  após autenticação, membership vigente, READY e revisão mínima aplicada. OSIV=false, sem EM antes
  de autenticar e sem troca de scope dentro de EM/transação. Limpar contexto no início e finally,
  pois `TenantScope.close()` restaura o anterior. Async usado captura security/tenant/actor.
- Segurança usa UUID/Instant e Clock injetado; DT_INICIO/DT_FIM em toda cadeia, sem FL_ATIVO.
  CHECK fim >= início permite intervalo vazio. Grants imediatos, encerramento de vínculo não reabre;
  novas concessões geram nova linha. Composite FKs/CHECK provam tenant e escopo no banco.
- Lombok padrão nas entidades: `@Builder @AllArgsConstructor @NoArgsConstructor @Getter @Setter
  @ToString`, com `@ToString.Exclude` nos `ManyToOne` para evitar recursão.

### III. Changelog Liquibase Organizado e Rastreável

- `changelog-master.xml` é apenas um **índice**: contém a property `uuid_function`, os changesets de
  infraestrutura e uma sequência de `<include>` apontando para um arquivo por domínio, em
  `backend/app-deploy/src/main/resources/db/changelog/`.
- Um arquivo por domínio ou agrupamento (`changelog-localidades.xml`, `changelog-normalizacao.xml`,
  `changelog-demo-registro.xml`). É **proibido** continuar inflando o master com `createTable`.
- `id` de changeset descritivo e estável (`create-table-demo-registro`, `seed-pais`,
  `seed-estado-brasil`); `author` é o responsável pela alteração. Changeset já aplicado **nunca** é
  editado — corrige-se com um changeset novo. Editar um changeset aplicado muda o checksum e quebra
  o boot de todo ambiente que já o rodou.
- Dados de referência (países, unidades federativas) entram por changeset de **seed idempotente**,
  não por script manual nem por código de aplicação.
- Toda entidade JPA MUST ter changeset correspondente; toda coluna mapeada MUST existir no
  changeset. A divergência é detectada no boot pelo `ddl-auto: validate` e conta como build
  quebrado.

### IV. Validação Acumulada e Ordem Canônica de Serviço

- **Ordem fixa dos métodos no Service:** `findById > search > delete > insert > save > validate`.
  `validate(dto, isInsert)` é privado e sempre o último método da classe.
- Validação **NUNCA** é fail-fast: acumule as mensagens em `ArrayList<String> errors` e lance
  `BusinessException(errors)` uma única vez ao final. O usuário corrige tudo de uma vez, em vez de
  descobrir um erro por submit.
- Proibido `!= null` e `.isEmpty()`: use `ObjectUtil.isNullOrEmpty` e
  `ObjectUtil.isNotNullAndNotEmpty`.
- `@Transactional(rollbackFor = Exception.class)` de `org.springframework.transaction.annotation` em toda mutação. Cadastros compostos (entidade +
  agregados) gravam em **uma única transação**, a partir de um único payload.
- Sobrecargas que recebem id `String` delegam para a sobrecarga `UUID`.
- Repository estende `JpaRepository<E, UUID>` **e** `BaseCustomJpaRepository<E>`. A busca vive como
  métodos `default`/`private` na própria interface, na ordem
  `search > countQuery > searchQuery > appendFilters`: HQL montado em `StringBuilder`, parâmetros
  nomeados em `HashMap`, `appendFilters` abrindo sempre com `WHERE 1=1`, execução por
  `executeHql`/`executeCountHql`, retorno em `new PageImpl<>(...)` e projeção por construtor em
  `XxxResponseDTO`. Os aliases do `FROM`/`JOIN` MUST ser os mesmos usados no `SELECT` e no
  `appendFilters` — divergência de alias só falha em runtime.

### V. Contrato REST e Autorização Espelhada na UI

- Controller estende `BaseController`, chama `super.setBodyToExceptionLog(dto, request)` em **todo**
  POST e PUT, documenta com `@Tag` na classe e `@Operation`/`@ApiResponse` nos métodos, e retorna
  **201** em insert, **204** em delete e **200** no restante.
- A extensão é incondicional, inclusive em controller somente GET. Não estender
  exige motivo específico no Javadoc da classe; ausência de corpo não é exceção.
- O catálogo é derivado do enum de ações do projeto, implementando `AuthorizationAction`,
  via `AuthorizationCatalog` da SDK. Ações escritas incorretamente devem falhar na
  compilação; o JSON gerado deve ser verificado no build, nunca mantido à mão.
- Busca paginada é **`POST /search`** com `@RequestBody FilterParams` e `@PageableDefault` — nunca
  GET com query params.
- **Autorização é por ação, não por nível.** Endpoint protegido usa uma ação concreta do catálogo
  e classificação GlobalEndpoint/TenantEndpoint. União SQL de caminhos vigentes diretos/perfil/grupo
  deduplica e intersecciona catálogo+scope; todos os elos e provider precisam vigência. Sem cache,
  claims de roles ou autoridade por drift. Mudança de scope no catálogo falha startup.
- Tenant admin administra somente memberships/grants próprios e concede subconjunto das ações
  efetivas do ator intersectadas com catálogo delegável, inclusive por grupo/perfil. Cadastro
  central e ação GLOBAL distinta criam o primeiro administrador da empresa. Nenhum bypass ADMIN.
- UID/provider/issuer é identidade permanente; ownership imutável. Google vincula por e-mail
  somente UID jamais ligado, google.com, e-mail verificado e exatamente um pending elegível sob
  lock/uniques. Identidade histórica encerrada nunca faz fallback. Senha usa UID planejado/Admin,
  envio nativo Firebase pelo cliente e nenhum segredo persistido. Bootstrap central é CLI explícita
  com project+UID verificado Admin, idempotente/auditado; sem primeiro login virar administrador.
- Desativação global bloqueia SQL antes de disable/revoke remoto; falha externa não desfaz bloqueio.
  Reativação exige enable ACK da última versão+CAS e nunca reabre grants encerrados. Encerrar
  membership não desabilita a conta Firebase. Revogação vale no próximo request após commit;
  request já autorizado pode concluir com snapshot.
- Dado referencial sem regra de negócio (países, estados, CEP) usa `isAuthenticated()`.
- **Autorização de campo** é o segundo mecanismo, e vive no **service**, não na anotação: quando a
  regra é "este perfil vê o registro mas não vê *este campo*", o service consulta
  `UsuarioAutenticadoService.possuiAcao()` e omite o valor na projeção — em `toDTO`, na busca **e**
  na gravação, para que um payload malicioso não sobrescreva o que o chamador não podia ler. O
  campo restrito de DemoRegistro é a referência mínima planejada. Aplicá-lo só na UI é defeito, não
  estilo: a API continua devolvendo o valor a quem chamar direto.
- **Estado de fluxo nunca entra pelo corpo da requisição.** Situação, etapa e marco avançam por
  endpoints de ação dedicados (endpoint de transição dedicado), cada um com a sua própria
  ação de `AcaoSistema`. Aceitar a situação num `PUT` genérico transforma a máquina de estados em
  sugestão e a autorização por transição em ficção.
- A UI **espelha a ação concreta**: `isAuthorized(Acoes.EXCLUIR_DEMO)` no sinal que controla o botão,
  `data: { roles: [Acoes.INSERIR_DEMO] }` na rota. O JSON frontend é gerado e verificado a partir de
  `AcaoSistema.java`, verificado por diff dos dois conjuntos.
- Scanner `EndpointAuthorizationScan.assertProtected` registrado e
  `TenantAuthorizationInterceptor` obrigatório. Authentication implementa AuthorizationContextAware
  com JwtAuthorizationContext publicado real e authorities ROLE_ação. Uma única registration do
  filtro stateless. Cobertura de rotas resolve loadChildren e reprova metadata `data.roles` ausente.
- O `roleGuard` do frontend falha **fechado**: rota guardada sem `data.roles` é negada, nunca
  liberada.
- Segurança stateless, CSRF desabilitado, EnableMethodSecurity. Público apenas OPTIONS, health e
  endpoint declarado PublicEndpoint. `/api/auth/me` é GlobalEndpoint+CONSULTAR_PROPRIA_CONTA;
  `/api/auth/context` é TenantEndpoint+CONSULTAR_PROPRIO_CONTEXTO_TENANT, com grants atômicos no
  pré-provisionamento/membership. Auth não vira público para contornar scanner. Token ausente,
  inválido, revogado ou disabled=401; negado local=403; provider indisponível=503 fail closed.
  Verificação real de revogação é obrigatória no sample, não presumida de verify default.
  OpenAPI público somente quando SPRINGDOC_ENABLED permite; desligado em ambiente exposto.
- Rota de terceiro que o projeto publica sem controlar (o `S3Controller` do `gems-aws-web`, cuja
  classe está num jar e não pode receber `@PreAuthorize`) MUST ser negada por regra de caminho no
  `SecurityConfig`, antes de qualquer registro de endpoint público.

### VI. Frontend Modular e Padronizado pela GEMS SDK

- Toda funcionalidade nasce como **módulo de feature próprio** em `src/app/features/<dominio>/`,
  contendo seu arquivo de rotas (`<dominio>.routes.ts`), `models/`, `services/` (store), `list/`,
  `form/` e `view/`. O `app.routes.ts` raiz apenas registra o módulo por `loadChildren`, e o item de
  menu correspondente é adicionado em `core/menu/menu.builder.ts` **com teste**. Nada de componente
  de feature declarado direto na raiz. *(Angular standalone não tem `NgModule`: "módulo" aqui é o
  arquivo de rotas do feature registrado por `loadChildren`.)*
- Componentes standalone; arquivos `.ts`, `.html` e `.css` **sempre separados** (template e style
  inline são proibidos); `inject()` em vez de parâmetros de construtor; `input()`/`output()` em vez
  de `@Input`/`@Output`; `signal()`/`computed()` para estado; `@if`/`@for`/`@switch` em vez de
  `*ngIf`/`*ngFor`/`*ngSwitch`.
- Componentes **nunca** usam `HttpClient` direto: a store estende `GemsBaseStore` com
  `super('api/{recurso}')` (sem barra inicial). `alertService.errorFromApi(err)` em **todo** bloco
  `error:` de chamada HTTP. `alertService.confirm(...)` devolve `SweetAlertResult` — cheque
  `.isConfirmed`.
- Tema definido **só** em `provideGemsTheme()` no `app.config.ts`. Proibido hex hardcoded em CSS de
  componente e proibido sobrescrever variáveis `--gems-*` fora do provider.
- Ícones exclusivamente **Font Awesome 6 Solid**. Botões pelas classes globais definidas em
  `styles.css` (`btn-save`, `btn-cancel`, `btn-novo`, `btn-back`, `btn-secondary`, `btn-danger`).
  Classe de botão que não exista ali renderiza **sem estilo nenhum**, em silêncio.
- Listagem segue o padrão **cabeçalho global + card de filtros + tabela solta**: a `gems-table` é
  elemento próprio da tela, sem card em volta, apenas espaçada do card de filtros. Coluna final de
  ações Visualizar, Editar e Excluir, cada uma visível conforme a ação concreta do usuário.
- Cadastros compostos usam o **wizard** da SDK (`GemsWizardComponent` + `GemsBaseStepComponent`) com
  StateService baseado em signals fornecido no `providers` do orquestrador, steps isolados que só
  conversam pelo StateService, um passo final de Resumo e submit único ao backend. O `gems-wizard` é
  só a barra de progresso: quem renderiza o passo ativo é o orquestrador, com `@switch`.
- **Mensagem de erro de campo usa `gems-field-error` da SDK (≥ 2.0.0)**, e **senha usa
  `gems-input-password`** com `enforcePolicy`, `showCriteria` e `matchControl` — não se recria
  política de senha nem `passwordMismatch` no projeto. A versão core publicada e provada fornece os componentes e a política; adapter de autenticação é separado. Cópia local de componente que a SDK entrega é o
  defeito que a seção "Adoção de Módulo da SDK GEMS" existe para remover.
- **Estrutura duplicada em duas features tem um dono só, e ele fica em `shared/`** — nunca dentro
  de uma delas: formulários, componentes, modelos, constantes e funções puras que atravessam
  features. Uma feature importar de outra feature é defeito estrutural; `core/` ou `shared/`
  importar de `features/` é o mesmo defeito, invertido.
- A mesma rota base atende os três modos, com os nomes de
  `.gems-ai/rules/angular.organization.md`: `/<recurso>/list`, `/<recurso>/form` (novo),
  `/<recurso>/form/:id` (edição) e `/<recurso>/view/:id` (leitura); a base `/<recurso>` redireciona
  para `list`. Menu, redirects e `navigate([...])` mudam juntos, com teste.


- A fachada de autenticação Firebase e guard são locais ao produto, sem inventar export da SDK.
  `data.roles`/roleMode e authorizationScope global|tenant tipados; lista ausente/vazia nega.
  Interceptor captura generation/UID/alias/sessão/payload ANTES de await getIdToken, revalida antes
  de enviar e usa headers do snapshot. Bearer somente origem API e boundary do path. Switch/logout/
  UID novo cancela pendências e limpa stores/menus/forms/actions; loading bloqueia negócio até
  contexto destino válido. Falha deixa ações tenant vazias; response antiga não muda UI/navegação.

### VII. Testes como Gate (NÃO NEGOCIÁVEL)

- Regra de negócio nasce **com teste antes** da implementação. Teste de service com JUnit 5 +
  Mockito (o agente do Mockito é anexado pelo Surefire no POM pai).
- Frontend testa com **Vitest** (builder `@angular/build:unit-test`). Funções puras de
  infraestrutura de UI — o construtor de menu é o caso canônico — MUST ter teste.
- **Rede de segurança nova prova-se por mutação**: quebre deliberadamente o alvo, confirme que o
  teste falha, restaure. Teste que passa sem nunca ter falhado não é prova de nada. **Rede que passa a
  depender de módulo da SDK entra nesta regra** — a garantia mudou de dono, e o teste precisa provar
  que o dono novo a cumpre.
- Nenhum teste pode referenciar classe ou componente inexistente. Teste órfão de código removido é
  removido junto, no mesmo commit.
- **Gate de merge:** `.\mvnw.cmd clean install` (reator inteiro, com testes) verde, `npm test`
  verde, `npm run build` verde e aplicação subindo com `ddl-auto: validate`.

### VIII. Especificação Antes de Código, Sem Sobras de Exemplo

- Toda funcionalidade nasce de um ciclo spec-kit:
  `specify > clarify > plan > tasks > analyze > roteiro > implement`. Marcadores
  `[NEEDS CLARIFICATION]` MUST ser resolvidos antes do `plan`.
- A documentação é **proporcional ao risco**. Nova feature, mudança de comportamento, arquitetura ou
  ambiente usa o ciclo completo. Bug que restaura contrato usa teste de regressão e gate. Refatoração
  ou ajuste técnico sem mudança de comportamento usa registro curto (`change-template.md`), tarefas e
  gates; artefato vazio não é criado por cerimônia.
- O `roteiro.md` é o **portão de aprovação humana antes do código**, escrito depois da análise de planejamento e
  antes de produção. Ele consolida numa peça única o que será desenvolvido em banco, backend e frontend,
  os testes esperados de cada bloco, o **raio de impacto sobre o que já foi entregue**, a divisão de
  tarefas com a delegação por modelo e os critérios de aprovação. Nenhuma linha de código de uma
  feature MUST ser escrita antes de o roteiro dela existir e ter sido aprovado. Autorização prévia
  explícita da conversa para o MESMO plano pode ser registrada literalmente, sem assinatura por
  proxy nem pedido repetido; alterações materiais exigem decisão real. O template está em
  `.specify/templates/roteiro-template.md`.
- O roteiro **consolida e aponta**, nunca duplica: todo número citado nele MUST bater com a sua fonte
  nos demais artefatos. Documento compilado que diverge da fonte é pior que a ausência dele, porque
  passa a ser lido como verdade sem sê-lo.
- A decisão de delegação MUST ser explícita — coluna `Modelo` na tabela de fases do `plan.md` e nota
  ao fim do `tasks.md`. **Sem delegação é uma decisão válida e preferencial** quando uma sessão nova
  não se amortiza. O critério é custo mínimo sujeito aos gates do Princípio VII: modelo mais capaz
  somente onde erro silencioso custa caro; modelos menores onde há espelho e teste forte. Toda
  escolha do modelo mais caro MUST ser justificada por risco material.
- Executor delegado recebe listas fechadas `READ`, `EDIT`, `CREATE` e `TEST`. Se o pacote for
  insuficiente, MUST parar e reportar; explorar o repositório para completar contexto é violação de
  escopo. Sessão existente SHOULD ser retomada em vez de recriada.
- Durante a iteração, execute o gate focalizado da área. O gate completo continua obrigatório no
  fechamento, mas SHOULD rodar uma vez pelo orquestrador para evitar repetição de contexto e logs.
- Requisito aprovado é o comportamento pretendido; código/testes são o estado real. Divergência
  exige investigação e correção ou registro de drift, não substituição silenciosa de autoridade.
- Exemplos mínimos ativos podem permanecer no molde até adoção; devem ser removíveis integralmente
  via `scripts/sample.py clean-demo` por manifesto fechado. Dry-run default; apply exige árvore
  limpa/root absoluto e rejeita symlink/junction/escape. Não toca DB/volumes. Rename/cleanup em
  cópia descartável prova build e preserva auth/tenants/security; catálogo regenerado oficialmente.

### IX. Configuração de Ambiente é Código

O schema do banco já obedecia a isso desde o Princípio III. Este princípio estende a mesma exigência
ao resto do ambiente.

- **Todo ambiente local/CI nasce de arquivo versionado** em `environments/`: PostgreSQL 16 e,
  como opção demo/offline do Compose local, Auth Emulator fixado15.32.1, projeto demo-gems, 9099/UI 4000.
  Emulador só opt-in local, fora da suíte automatizada e do CI, nunca real/prod; host usa localhost e
  container usa hostname de serviço. Tokens unsigned do emulador precisam verificação extra de
  revogação mesmo check-revoked=false.
- **Suíte automatizada e CI usam o projeto Firebase real de testes** `sample-firebase-multi-tenant`,
  via ADC, com o secret `FIREBASE_TEST_SERVICE_ACCOUNT_JSON` materializado em arquivo temporário no
  runtime. Usuários sintéticos usam namespace por execução (prefixo + run id no e-mail/UID), com limpeza
  garantida e varredura de órfãos restrita ao namespace; o PostgreSQL permanece descartável
  (Testcontainers). Indisponibilidade do provedor é testada por fault injection local determinística,
  nunca derrubando o Firebase compartilhado. Google OAuth real e entrega de reset por e-mail ficam fora
  da automação, em verificação humana.
- Primeiro procurar `enviroments` e usar pasta real `environments` para identificar serviços antes
  de conectar MCP/PostgreSQL. Não copiar credenciais para config.toml nem respostas.
- Homologação real isolada usa configuração manual documentada no console (Google/email/password,
  authorized localhost), ADC/segredo externo, banco separado, bootstrap UID verificado e dois
  testers. O CI usa apenas a credencial do projeto de testes, como secret externo. Hospedagem será decidida depois; não é autorizada
  por runbook herdado. LocalStack/S3 é profile opcional útil com denyall preservado.
- **Segredo nunca é versionado.** Senha, client secret e credencial de nuvem chegam por variável de
  ambiente, com um `.env.example` versionado servindo de contrato e o `.env` real fora do Git.
  Nenhum valor real de produção pode existir no repositório, em nenhum arquivo.
- **Dado de produção MUST ter backup verificado.** Backup nunca restaurado é hipótese, não backup: o
  ensaio de restore é parte do gate de entrega, não da documentação. Onde a integridade depende de
  dois armazenamentos distintos, o backup MUST ser tratado como unidade e restaurado em conjunto.
- **Exposição de rede é mínima e explícita.** Em ambiente publicado, apenas o proxy reverso expõe
  porta ao host; banco, provedor de identidade e aplicação conversam pela rede interna.
- **Superfície administrativa não é publicada.** O console do provedor de identidade e qualquer
  endpoint de diagnóstico que enumere a aplicação MUST ser fechados no proxy e alcançados por túnel,
  não por allowlist de IP — endereço residencial muda, e a regra que depende dele apodrece em
  silêncio. O ambiente publicado MUST responder com os cabeçalhos de segurança (HSTS, `nosniff`,
  `X-Frame-Options`, `Referrer-Policy`, CSP) e MUST ser verificado **de fora da máquina** antes de
  ser considerado entregue.

## Adoção de Módulo da SDK GEMS

A SDK GEMS é a origem dos mecanismos transversais deste projeto. Quando o backend implementa à mão
algo que um módulo da SDK já entrega, a duplicata é **defeito de fundação**, não escolha de estilo: as
duas cópias divergem, e a divergência aparece como comportamento — não como erro de compilação.

Esta seção autoriza a substituição e fixa o preço dela. Ela **não** autoriza adotar módulo por
conveniência: substituir mecanismo em funcionamento é mudança de fundação, e mudança de fundação
passa pelo Princípio VIII.

**Condições de entrada — todas MUST estar satisfeitas antes da primeira linha de substituição:**

1. **O módulo está publicado no registry**, na versão que o projeto vai consumir. Branch, snapshot ou
   clone local não habilitam substituição: o bloco que depende de artefato não publicado fica
   **bloqueado**, nunca contornado.
2. **A versão é declarada num único lugar** (`<gems.version>` no POM pai). Módulo filho declara
   dependência, nunca versão.
3. **O alcance real da versão é medido, não presumido** — `dependency:tree` antes e depois. Um MAJOR
   da SDK é atravessável quando nenhuma das quebras alcança o grafo do projeto, e isso é uma medição,
   não uma expectativa.
4. **A garantia que o mecanismo local oferecia está nomeada** antes de ele sair. Mecanismo que existe
   sem que ninguém saiba dizer o que ele protege não é substituído: é estudado primeiro.
5. **O módulo é de escopo aprovado.** Adotar um módulo que não consta do escopo da rodada é escopo
   novo, e escopo é decisão do product owner — registrada no `roteiro.md`, não no commit.
6. **Mudança de comportamento observável** (resposta de API, código de status, falha de boot,
   contrato de configuração) é **decisão do product owner**, tomada antes e registrada no roteiro.
   Nunca efeito colateral descoberto depois.

**O que a substituição MUST provar, e prova é execução, não inspeção:**

1. **Equivalência com teste no projeto.** A garantia nomeada na condição 4 continua tendo, **neste
   repositório**, um teste que falha se o módulo deixar de cumpri-la. Teste que vive só na SDK prova a
   SDK, não a adoção dela aqui.
2. **Prova por mutação** (Princípio VII) sobre toda rede que passa a depender do módulo: quebrar o
   alvo real, confirmar o vermelho, restaurar por edição inversa.
3. **Gate onde a falha é silenciosa.** Garantia cuja violação não gera erro —
   paridade de catálogo, cobertura de anotação — MUST continuar tendo varredura que **quebra o
   build**, e a varredura é código deste projeto se o módulo não a traz.
4. **Gate do Princípio VII verde**, com o reator inteiro, e aplicação subindo com `ddl-auto: validate`.

**O que a substituição não autoriza:**

- **Coexistência.** A classe local sai **na mesma alteração** em que o módulo entra; teste órfão sai
  com ela. Duas fontes de verdade em trânsito é exatamente o defeito que a substituição vem corrigir.
- **Adaptar o módulo ao projeto por dentro.** Se o módulo não serve, o caminho é a rodada da SDK — não
  uma camada de contorno aqui, que reintroduz a duplicata com outro nome.
- **Reduzir cobertura.** Substituição que diminui o número de provas do projeto é regressão, ainda que
  verde: o que o teste local cobria MUST continuar coberto.


## Stack e Restrições Técnicas

| Camada | Tecnologia/contrato |
| :--- | :--- |
| Backend | Java 21/Spring Boot 4.1, cinco módulos Maven, GEMS publicada, Liquibase |
| Banco | PostgreSQL 16; JDBC security opt-in separado de JPA empresarial |
| Frontend | Angular 22/TypeScript 6, core Angular GEMS 3.0.0 alvo, Vitest/Prettier |
| Identidade | Firebase Admin 9.11.0, Web JS 12.19.0; Google+email/senha |
| Local/CI | DockerCompose local (AuthEmulator/CLI 15.32.1, demo-gems, só demo/offline); suíte e CI no projeto Firebase real de testes via ADC, PostgreSQL por Testcontainers |
| Factory | GEMS AI 3.1.0 pin 909c69aa846ea97b3aea06a08fdfed8c6657d391 (formatter Java oficial no lugar do Spotless) |

- No Windows use mvnw.cmd; iterar -pl/-am, gate final reator inteiro. npm ci segue lockfile sem
  legacy-peer-deps improvisado. Versões SDK só entram após publicação e consumidor externo isolado.
- `docker compose down -v` é proibido: volumes são dados, não artefatos descartáveis de cleanup.
- Provisionamento de schema é CLI/job fora HTTP, advisory lock por alias com sessão pinada,
  unlock seguro/evict se falhar, Liquibase verificada antes READY+revision; suspensos também migram.
  Inventário expectedSchemas fornece nomes de schema READY, não aliases.
- Bootstrap técnico `modelo` é opt-in aditivo na SDK; migration antes de EMddlvalidate; missing
  sentinel inválido como alias falha antes conexão/SQL; getAnyConnection restaura original.
  Properties antigas ausentes preservam comportamento. Novo strict validator não muda sanitize legado.

## Fluxo de Trabalho e Portões de Qualidade

- Branch a partir de `main` no padrão `feature/<descricao>` ou `bugfix/<descricao>`.
- PR para `main` com título curto e corpo contendo: problema resolvido, recursos criados, recursos
  removidos ou depreciados, e pendências.
- Antes do PR: `.\mvnw.cmd clean install` na raiz do backend, `npm ci`, `npm test`, `npm run build` e `npx prettier --check .`
  no frontend.
- Nenhum PR é aprovado com o gate do Princípio VII vermelho.
- Alteração de infraestrutura ou de configuração de ambiente segue o **mesmo** ciclo spec-kit do
  Princípio VIII. Ela muda como o sistema é publicado e operado, e merece a mesma rastreabilidade de
  uma mudança de comportamento.

## Governance

Esta constituição **prevalece sobre qualquer outra prática documentada**, inclusive `CLAUDE.md`,
`AGENTS.md`, `CODEX.md`, `GEMINI.md`, `.cursor/rules/`, `.github/copilot-instructions.md` e os
arquivos em `.gems-ai/`. Quando um desses arquivos divergir da constituição, **ele** é corrigido —
não a constituição.

### A regra de escrita vence o código consolidado

**As regras de `.gems-ai/rules/` prevalecem sobre o código já consolidado.** Quando o código do
repositório divergir de uma regra de escrita, é **o código** que se corrige — e enquanto não se
corrige, o desvio MUST carregar uma **exceção escrita** no próprio ponto do código.

A autoridade de regras de escrita é preservada da constituição 2.0.0. Referências de outros projetos não importam requisitos de produto. Constituição, segurança e contrato público publicado prevalecem sobre orientação genérica de adapter.

**"Código vence documentação" permanece inteiro para o que ele sempre protegeu**: documentos preliminares. Código e testes mostram o estado real; a spec aprovada
continua sendo contrato do comportamento pretendido e a divergência é investigada — é a proteção contra documento alucinado, e não é o que
esta seção troca. O que muda é **apenas** a relação entre código e **regra de escrita**.

| Quando divergem | Quem vence | Onde |
| :--- | :--- | :--- |
| Constituição × qualquer outro documento | **a constituição** | acima |
| Regra de `.gems-ai/rules/` × código consolidado | **a regra** | esta seção |
| Spec aprovada × código | **spec como contrato; código como estado observado** | investigar bug/drift |
| Documento preliminar × código | **código como evidência** | sem inventar estado implementado |

**A regra também pode estar errada, e então é ela que se corrige.** A inversão não transforma a
regra em verdade revelada: quando obedecê-la literalmente produziria código que não compila ou
quebra comportamento provado, a saída é emendar a regra — com a mesma cerimônia desta seção —, nunca
ignorá-la em silêncio. A anotação transacional do Princípio IV é alinhada a
`services.md`.

### Exceção escrita de conformidade

O desvio autorizado existe, e tem forma única:

```text
GEMS-EXCEPTION(<arquivo-de-regra>#<seção>): <motivo>; expira AAAA-MM-DD
```

O formato completo, os portadores por linguagem, o ciclo de vida e as três formas de reprovar —
vencida, órfã e regra inexistente — vivem em **`.gems-ai/rules/base.waivers.md`**, que
esta seção autoriza e não duplica.

Três exigências são constitucionais:

1. **Prazo obrigatório, que reprova o build quando vence.** Desvio autorizado sem data é permissão
   permanente, e permissão permanente é a hierarquia antiga de volta com outro nome.
2. **Ela é decisão humana.** Agente de codificação **não** escreve exceção para fazer gate vermelho
   ficar verde. Gate vermelho é informação: ou o código está errado, ou a regra está — e nos dois
   casos a saída é corrigir o que está errado.
3. **Exceção não é baseline.** Baseline é dívida que uma rodada não alcançou, sem justificativa
   individual e sem prazo; ele **só encolhe**. Exceção é desvio decidido, com motivo e data.
   Confundir os dois transforma o baseline no caminho pelo qual o desvio volta com aparência de
   autorização.


Emendas exigem:

1. Registro no **Sync Impact Report** no topo deste arquivo.
2. Incremento **semântico** de versão: MAJOR para remoção ou redefinição incompatível de princípio,
   MINOR para novo princípio ou expansão material de orientação, PATCH para clarificação, correção
   de redação ou refinamento não semântico.
3. Atualização dos arquivos de agente afetados **na mesma alteração**.

Toda revisão de PR MUST verificar conformidade com estes princípios. Complexidade adicional MUST ser
justificada por escrito no `plan.md` da feature — ausência de justificativa é motivo suficiente para
recusar o PR.

**Version**: 3.1.1 | **Ratified**: 2026-09-05 | **Last Amended**: 2026-10-13
