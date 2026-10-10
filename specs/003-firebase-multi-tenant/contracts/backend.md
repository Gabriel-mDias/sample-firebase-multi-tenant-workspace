# Contrato backend e SDK Java

## Superfície independente

Artefato `br.com.gems:gems-firebase-auth`, candidato3.4.0 se mudança aditiva. Não depende de adapter do provedor anterior, JpaMulti, GemsObs ou AWS; gems-security-authorization é permitido. Ports, token verifier e Admin/Spring podem funcionar sem DataSource; JDBC services/schema security são opt-in separado. No hiddenDDL, nenhuma SecurityFilterChain da SDK. Manager JDBC nomeado/qualificado separado do manager JPA; serviços externos não participam da transação SQL.

Propriedades base `gems.firebase.auth.enabled`, `gems.firebase.auth.project-id`, `gems.firebase.auth.check-revoked`, `gems.firebase.auth.tenant-header` (X-Tenant-Alias), todas sob o prefixo `gems.firebase.auth`. Sample checkrevoked=true; verifyIdToken default não basta. FirebaseApp nomeada: ConditionalOnMissingBean e cleanup somente se criada/owned pela SDK. Clock injetado, eventos sanitizados, sem token/header secreto/cause remota bruto.

O emulator é apenas opção demo/offline opt-in do Compose local com demo-id, fora da suíte automatizada e do CI; proibição real/prod. Unsigned tokens do emulator precisam consulta extra de revogação/disabled mesmo check-revoked=false. A suíte automatizada e o CI usam o projeto Firebase real de testes `sample-firebase-multi-tenant` via ADC (secret `FIREBASE_TEST_SERVICE_ACCOUNT_JSON` em arquivo temporário no runtime), com usuários sintéticos em namespace por execução e limpeza garantida; RSA+cert/http lookup stub permanece como prova determinística local, e a indisponibilidade do provider é simulada por fault injection local, nunca no Firebase compartilhado (decisão 6B de 2026-10-09). Ausência/invalidade/revogação/disabled=401; negativa local=403; indisponibilidade do provider=503 e falha fechada.

## Autenticação e contexto

Authentication implementsAuthorizationContextAware; retorna JwtAuthorizationContext real(globalProfiles,globalActions,tenantProfiles,tenantActions,Optional alias). Authorities ROLE_ação derivam catálogo e scope efetivo. `EndpointAuthorizationScan.assertProtected` registrado e `TenantAuthorizationInterceptor` obrigatório. Uma única Spring registration do filtro stateless. O scanner não é contornado com PublicEndpoint em auth/context.

Fluxo: limpar contexto no início → verificar token/projeto → resolver usuário/provider/identidade SQL → comprovar membership+tenant+READY+revisão → instalar alias em Authentication → TenantScope.forTenant → abrir EM/transaction de negócio → fechar scope → limpar security/tenant/actor em finally. OSIV=false; nenhum EntityManager antes da autorização. Global JPA usa TenantScope.global deliberado; ausência de alias nunca default. Não switchscope dentro de EM/tx. Async quando usado captura security+tenant+actor e restaura/limpa na thread.

## REST

| Verbo/rota | Classificação/ação | Contrato |
| :--- | :--- | :--- |
| GET /api/auth/me | GlobalEndpoint; hasRole CONSULTAR_PROPRIA_CONTA | user{id,name,email}, tenants[{alias,name}],globalActions[]; sem tenant obrigatório; grant atômico no pré-provisionamento |
| GET /api/auth/context | TenantEndpoint; hasRole CONSULTAR_PROPRIO_CONTEXTO_TENANT | header X-Tenant-Alias; tenant{alias,name},profiles[],groups[],actions[]; grant atômico na criação membership |
| GET /api/demo/public | PublicEndpoint | demonstra classificação pública deliberada |
| GET /api/demo/global | GlobalEndpoint; CONSULTAR_TENANT | snapshot global sem header obrigatório |
| GET /api/demo/tenant/read | TenantEndpoint; CONSULTAR_DEMO | leitura isolada |
| POST /api/demo/tenant/write | TenantEndpoint; ALTERAR_DEMO | validação/tx isoladas |
| POST /api/demo-registros/search | TenantEndpoint; CONSULTAR_DEMO | FilterParams+Pageable; projeção/campo restrito no service |
| GET /api/demo-registros/{id} | TenantEndpoint; CONSULTAR_DEMO | UUID; mesmo PK de outro tenant não vaza |
| POST /api/demo-registros | TenantEndpoint; INSERIR_DEMO | 201, DTO, validação acumulada |
| PUT /api/demo-registros/{id} | TenantEndpoint; ALTERAR_DEMO | 200; payload não troca tenant |
| DELETE /api/demo-registros/{id} | TenantEndpoint; EXCLUIR_DEMO | 204, softdelete |

Campo `VISUALIZAR_VALOR_RESTRITO` prova leitura/busca/gravação de service. Catálogo deriva AcaoSistema via gerador oficial Java, não JSON manual. Nenhuma ação ADMIN genérica autoriza tudo. Endpoints admin são pequenos e orientados a ações (consultar/insert/alterar/suspender/membership/grants), todos classificados, auditados e testados; tenant admin não recebe mutação de identidade/global. Central usa açãoGLOBAL distinta para bootstrap do primeiro admin de tenant; bootstrap CLI exige project+UID verificado Admin, idempotência e auditoria, sem firstlogin automático.

## JPA opt-in aditivo

Generalizar helpers técnicos modelo a partir do contrato Meduc nomeado, sem copiar fora READ. `gems.tenant.validation-schema=modelo`; `gems.tenant.liquibase.startup-enabled` defaulttrue SDK/defaultfalse sample. Configs antigas sem propriedades continuam iguais. Missing sentinel é identificador inválido como alias e falha antes de obter conexão/SQL; getAnyConnection restaura schema original; migrations modelo precedem EMddlvalidate. Prefix tenant_ com NOVO validator opt-in ASCII/LocaleROOT max56bytes/reservados/immutabilidade, preservando sanitize e SchemaNaming legados na MINOR. Ports auth sem JpaDep e contrato sample prova derivação coincidente.

## Gate release

BOM/docs/release notes+full clean verify/gate oficial/workflow `Release` (`Java-GEMS-SDK/.github/workflows/release.yml`), pela tag imutável `v3.4.0`, com verify e Auth Emulator antes de deploy, depois consumidor Maven com repo M2 isolado/clean para provar artefato remoto. Instalação local não é aceite. Published authorization public API e full reactor atual precisam compatibilidade testada antes de declarar3.4.0 MINOR.

## Correções contratuais P0 — 2026-10-08

Java SDK usa Maven externo sob JDK 21; o sample preserva `backend/mvnw.cmd`. Release candidata: workflow `Release` (`Java-GEMS-SDK/.github/workflows/release.yml`), pela tag imutável `v3.4.0`, com verify e Auth Emulator antes de deploy. `publish.yml` é somente Main Validation; não existe comando executável `publishPackage` nem workflow_dispatch para Release. Depois de revisão/gates atuais e consulta de colisão no registry, sequência operacional: commit/PR Validation/Main Validation → tag imutável → Release → consumidor Maven remoto em M2 novo. Nenhuma dessas ações foi executada por P0.

T001 permanece aberta para revisão independente integral dos bytes finais. Nenhum gate funcional ou tarefa SDK é fechado por P0. Constituição 3.0.0, login G&Ms e invariantes Firebase/SQL/tenant/lifecycle preservados.
