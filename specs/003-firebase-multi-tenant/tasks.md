# Tasks: Firebase multi-tenant GEMS

**Input**: spec/plan/research/data-model/contracts/quickstart da rodada 003.
**Status**: 34 tarefas: 16 concluídas (T001–T013, T015, T016 e T027), 18 abertas. Estado reconciliado com o ledger Astra 2026-10-09 (R002); T026/T029 aceitas no desenho anterior ao 6B e reabertas para a migração ao Firebase real. As seções de estado de 2026-10-08 abaixo são histórico anterior a essa reconciliação.
**Organization**: dependências serializadas por ondas; testes antes da lógica. Sem [P] nesta lista, pois a alocação de ownership será feita por contratos futuros.

**Estado vigente DOC1 (2026-10-12)**: 34 tarefas, 23 concluídas e 11 abertas; após a homologação de 2026-10-12, T032 concluída: 24 e 10. O status acima é histórico. A [retificação DOC1](#doc1-retificacao) registra evidências recebidas, limitações e critérios NOT_RUN; não declara fechamento integral da feature.

## Fase1 — contrato e governança

- [x] T001 Materializar e revisar independentemente specs/003-firebase-multi-tenant e Java-GEMS-SDK/docs/plans/firebase-multi-tenant; atualizar .specify/memory/constitution.md3.0.0/SIR/agentes.
- [x] T002 Atualizar configuração e snapshot GEMS AI 3.0.0 pelo pin/CLI oficial; arquivar preview/unionbundles/evidence em specs/003-firebase-multi-tenant; strict integrity e análise de contratos.

## Fase2 — Java SDK bloqueante US1/US2

- [x] T003 [US1] Escrever primeiro tests de ports/verifier/Admin/custom resolver sem DS/beans próprios/Clock/error em Java-GEMS-SDK/gems-firebase-auth/src/test.
- [x] T004 [US1] Implementar módulo/ports/AdminSpring/properties independentes em Java-GEMS-SDK/gems-firebase-auth/src/main e reactor/BOM; versão3.4.0 somente se aditiva.
- [x] T005 [US1] Escrever tests constraints PostgreSQL/CTE de toda cadeia/emissor de identidade/rowlocks/elegibilidade Google em Java-GEMS-SDK/gems-firebase-auth/src/test antes de JDBC/changelog.
- [x] T006 [US1] Implementar changelogsecurity15 tabelas e repositories/services JDBC opt-in/manager qualificado em Java-GEMS-SDK/gems-firebase-auth/src/main.
- [x] T007 [US1] Tests duráveis disable/revoke/create/enable CAS da última versão/restart da outbox e tenantgrant escalation em Java-GEMS-SDK/gems-firebase-auth/src/test.
- [x] T008 [US1] Implementar comandos de lifecycle/EVENTO_SEGURANCA/outbox/services administrativos/sincronização de catálogo sem payload de segredo em Java-GEMS-SDK/gems-firebase-auth/src/main.
- [x] T009 [US2] Escrever RSA real/certificado stub/token de emulador/verificação de revogação/scanner/context/tests de thread em Java-GEMS-SDK/gems-firebase-auth/src/test.
- [x] T010 [US2] Implementar Authentication com AuthorizationContextAware/JwtAuthorizationContext e filtro stateless sem filter chain em Java-GEMS-SDK/gems-firebase-auth/src/main.
- [x] T011 [US2] Tests bootstrap modelo/sentinel ausente inválido/refuseSQL/restauração de getAnyConnection/settings legadas/compatibilidade do validator em Java-GEMS-SDK/gems-jpa-multi-tenant/src/test.
- [x] T012 [US2] Implementar helpers generalizados opt-in/configuração de validação/startup enabled/novo alias estrito em Java-GEMS-SDK/gems-jpa-multi-tenant/src/main; não quebrar sanitize/SchemaNaming antigos.
- [x] T013 [US3] Atualizar Java-GEMS-SDK/gems-bom/pom.xml/docs/release e executar full clean verify/gate oficial/workflow `Release` (`Java-GEMS-SDK/.github/workflows/release.yml`), pela tag imutável `v3.4.0`, com verify e Auth Emulator antes de deploy/consumidorM2remoto isolado antes de integração no sample.

## Fase3 — Angular SDK bloqueante US3

- [ ] T014 [US3] Criar checkout Angular-GEMS-SDK no pin oficial 2.3.4; tests exports/transitive/metadata/core e adapter antes da separação; clone legado 2.1.0 não alterado. **LIMITAÇÃO aceita pelo usuário (2026-10-10): cronologia histórica de teste antes não provada; D001-B/MUT1 prova a mutação atual, sem reconstrução retroativa.** Ver [limitações DOC1](evidence.md#limitações-aceitas-pelo-usuário-2026-10-10).
- [x] T015 [US3] Separar core 3.0.0 e adapter 1.0.0 em Angular-GEMS-SDK, mantendo implementação do adapter e UI/forms/exports core.
- [x] T016 [US3] Build/test/publicar core+adapter e consumidor npm remoto isolado; registrar pins/versões/hashes antes do consumo sample.

## Fase4 — sample US1/US2, depende T013+T016

- [x] T017 [US1] Testar dependency adoption/equivalence/mutations antes de trocar backend/pom.xml, app-security e frontend/package.json/locks para artefatos publicados. **DOC1:** BE2E `bfb8a50`: T017-G1, versão pelo JAR carregado e metadados Maven, consumidor Maven isolado baixando do registro e mutação de versão detectada. Ver [evidências DOC1](evidence.md#evidências-observadas-doc1).
- [x] T018 [US1] Configurar properties/filtro/scanner/interceptor/managerJDBC e endpoints me/context em backend/app-security e backend/app-core; recursos/env/Liquibase só app-deploy. **DOC1:** BE1 `54a9712` e checkpoint: T018-G1/G2 e GEMS-S1/S2. Ver [evidências DOC1](evidence.md#evidências-observadas-doc1).
- [ ] T019 [US1] Implementar mínimo admin central/tenant membership/grants e bootstrap CLI verificado, eventos de segurança/global em backend/app-core; DTOs app-model, POJOs app-events, config app-deploy. **LIMITAÇÃO aceita pelo usuário (2026-10-10): cronologia histórica B08/F05 não provada; BE2E fecha T019-G1/G2/G3 e MUT1 fornece o controle compensatório atual, sem provar a cronologia.** Ver [limitações DOC1](evidence.md#limitações-aceitas-pelo-usuário-2026-10-10).
- [x] T020 [US2] Testar schema derivation coincidente/READY e revisão mínima/locks de provisionamento/inventário de suspensos em backend/app-core/src/test; implementar TenantSchemaSource/CLI/job fora de HTTP e scope global deliberado. **DOC1:** BE3C `c8e8d53` e `80c88f4`: T020-G1/G2, inclusive falha de unlock contra SDK 3.4.1. Ver [evidências DOC1](evidence.md#evidências-observadas-doc1).
- [x] T021 [US2] Criar migrations modelo/administracao/auditoria/tenant em backend/app-deploy/src/main/resources/db/changelog; modelo ANTES do EM, OSIV false, sem troca de scope dentro de EM/tx. **DOC1:** BE3C `c8e8d53` e `80c88f4`: T021-G1, constraints explícitas verificadas no forward e no rollback. Ver [evidências DOC1](evidence.md#evidências-observadas-doc1).
- [ ] T022 [US2] Escrever tests deferred determinísticos em frontend/src/app/core/auth para AlphaBeta/await do token/response antiga/logout/outro UID/falha no switch/boundary de URL antes de fachada/interceptor. **LIMITAÇÃO aceita pelo usuário (2026-10-10): G13/G15/G17 de R005 fora do escopo enxuto; FE1D fecha G18, mas a cobertura integral das demais lacunas desta tarefa não está demonstrada no recorte fornecido.** Ver [limitações DOC1](evidence.md#limitações-aceitas-pelo-usuário-2026-10-10).
- [ ] T023 [US1] Implementar fachada Firebase/Google/email/password/native reset/signout/me e guard local data.roles+authorizationScope em frontend/src/app/core/auth; stores/actions/menu limpa geração. **LIMITAÇÃO aceita pelo usuário (2026-10-10): G13/G15/G17 de R005 fora do escopo enxuto; FE2B3 fecha G12/G14, mas a cobertura integral das demais lacunas desta tarefa não está demonstrada no recorte fornecido.** Ver [limitações DOC1](evidence.md#limitações-aceitas-pelo-usuário-2026-10-10).
- [ ] T024 [US2] Implementar seletor de tenant/loading/invalidação de contexto/headers de snapshot e cancel em frontend/src/app/core/auth e features; catálogo de actions só gerador Java oficial. **LIMITAÇÃO aceita pelo usuário (2026-10-10): G13/G15/G17 de R005 fora do escopo enxuto; não há evidência de fechamento integral de R005 nesta tarefa no recorte fornecido.** Ver [limitações DOC1](evidence.md#limitações-aceitas-pelo-usuário-2026-10-10).
- [x] T025 [US2] Implementar/tests DemoRegistro CRUD removível/public/global/tenant/read/write/campo restrito em backend/app-model,app-core,app-security e frontend/src/app/features/demo-registro. **DOC1:** BE1 `54a9712` e checkpoint: T025-G1/G2; FE2A2 `c61b091`: G06/G09/G11, review independente PASS. Ver [evidências DOC1](evidence.md#evidências-observadas-doc1).

## Fase5 — molde/ambiente e provas US3

- [x] T026 [US3] Remover domínio pesado antigo e infra operacional anterior; manter o emulador com PG16 e pins em environments/shared/firebase e environments/local apenas como opção demo/offline do Compose local; LocalStackprofile opcional/denyall; migrar a suíte e o CI ao projeto Firebase real de testes `sample-firebase-multi-tenant` via ADC (secret `FIREBASE_TEST_SERVICE_ACCOUNT_JSON` materializado em arquivo temporário no runtime), PostgreSQL descartável por Testcontainers, usuários sintéticos com namespace por execução (prefixo + run id), limpeza garantida e varredura de órfãos restrita ao namespace. O secret `FIREBASE_TEST_SERVICE_ACCOUNT_JSON` é cadastrado pelo usuário; em PR de fork o job E2E real é pulado com aviso explícito, e no repositório principal a ausência do secret falha o job. Reaberta pela decisão 6B de 2026-10-09; aceite anterior valia para o desenho com emulador no CI. **DOC1:** MIG2 `da59455` e MIG3B `8154aa3`: migração E2E/CI ao Firebase real; execução do CI remoto neste HEAD NOT_RUN. Ver [evidências DOC1](evidence.md#evidências-observadas-doc1).
- [x] T027 [US3] Escrever tests do manifesto fechado/árvore limpa/root absoluto/escape via junction em scripts; implementar scripts/sample.py clean-demo default dry-run/mutuamente exclusivos sem DB/volumes.
- [x] T028 [US3] Adaptar scripts/inicializar-projeto.ps1 preservando snapshot/pins de imports; cópia descartável: rename+cleanup/build/auth/fundamentos de tenants smoke. **DOC1:** `6c49201`: harness correction8 em duas cópias descartáveis, init/replay/clean-demo e gates PASS; unittest 26+24. Ver [evidências DOC1](evidence.md#evidências-observadas-doc1).
- [ ] T029 [US1/US2/US4] E2E PostgreSQL descartável (Testcontainers) + projeto Firebase real de testes com namespace por execução/constraints de toda cadeia/corridas de linking Google/restart da outbox/CAS da última versão/limpeza de thread/isolamento com mesma PK em backend/app-deploy/src/test e frontend/e2e; provar mutações reais scanner/guard/catalog e login por senha/reset (solicitação e retorno genérico; Google OAuth real e entrega de e-mail de reset ficam fora da automação, em verificação humana), retorno interno, acessibilidade, negação genérica e races de sessão/contexto; indisponibilidade do provedor (503) por fault injection local determinística, nunca derrubando o Firebase compartilhado. Reaberta pela decisão 6B de 2026-10-09; aceite anterior valia para o desenho anterior, que usava o emulador no E2E. **LIMITAÇÃO aceita pelo usuário (2026-10-10): G13/G15/G17 de R005 fora do escopo enxuto; MIG2 e T031 provam a suíte E2E real PASS, mas não demonstram o fechamento integral dos critérios frontend adjudicados desta tarefa.** Ver [limitações DOC1](evidence.md#limitações-aceitas-pelo-usuário-2026-10-10).
- [ ] T030 [US3] Medir inventário/provisionamento 100/1000 schemas em scripts/benchmark-tenants e registrar tempo/mem/SQL sem SLA. **LIMITAÇÃO aceita pelo usuário (2026-10-10): medição somente com 100 schemas; 1000 schemas = NOT_RUN.** Ver [limitações DOC1](evidence.md#limitações-aceitas-pelo-usuário-2026-10-10).
- [ ] T031 [US3/US4] Gates GMS strict+conformance de código NOVO E PRESERVADO com baseline zero/backend clean install/frontend npm ci/test/build/prettier check e termos operacionais zero; revisar specs/003-firebase-multi-tenant/evidence.md. **DOC1:** gates locais observados PASS em `98db709`; termos operacionais zero = NOT_RUN, pois a spec não define os termos. CI remoto neste HEAD = NOT_RUN. Ver [verificação T031](evidence.md#verificação-documental-t031).
- [x] T032 [US3/US4] ProjetoSpark Web homolog isolado/Google/email/password/authorized localhost/ADC e DB separado/bootstrap por UID do operador/dois testers/revoke/disable/reset/spoof; homologar também login G&Ms responsivo/acessível e retorno/contexto; evidência manual sanitizada em specs/003-firebase-multi-tenant. **LIMITAÇÃO aceita pelo usuário (2026-10-10): homologação manual do usuário = NOT_RUN até a execução dele.** Ver [limitações DOC1](evidence.md#limitações-aceitas-pelo-usuário-2026-10-10). **CONCLUÍDA em 2026-10-12:** homologação em tela pelo product owner, com Firebase real e banco dedicado, aprovou: login Google, isolamento com Tester só em Beta, campo restrito, DemoRegistro, auditoria, gestão de usuários pelo backend e login por senha após o e-mail de primeiro acesso e reset, além de disable e revoke com o token anterior (401) e spoof de `X-Tenant-Alias` (403), revogação de grant, encerramento de vínculo, suspensão com worker e reativação, retorno pós-login seguro e tela de login responsiva e acessível (Lighthouse Acessibilidade 100). A devolução automática da ação básica de conta na reativação, decidida pelo usuário, fica como mudança futura (achado 4). Ver [homologação 2026-10-12](evidence.md#homologacao-local-20261012).

## Fase4b — login próprio G&Ms US4 (pendente)

- [ ] T033 [US4] Escrever antes da UI testes em frontend/src/app/features/login/*.spec.ts e testes relacionados de core/auth: Google/senha/reset nativo por fachada, loading/submit duplicado/erro anunciado e genérico, 320/768/1440 sem overflow, teclado/foco/labels/contraste, retorno interno permitido, sessão ativa após me/context válidos, negação elegível, logout/novo UID/respostas antigas. Depende de T013/T016/T017 (publicação/adoção), T022/T023 (fachada) e T024 (contexto); gates UI/E2E previstos em T029/T031/T032. **LIMITAÇÃO aceita pelo usuário (2026-10-10): G13/G15/G17 de R005 fora do escopo enxuto; FE2B3 prova G12/G14, mas não demonstra o fechamento integral dos testes UI adjudicados desta tarefa.** Ver [limitações DOC1](evidence.md#limitações-aceitas-pelo-usuário-2026-10-10).
- [ ] T034 [US4] Implementar frontend/src/app/features/login/login.routes.ts e componentes .ts/.html/.css separados, standalone/lazy; integrar app.routes.ts, app.config.ts (provideGemsTheme central) e frontend/public/assets de marca pertinentes. Usar gems-input-password/gems-field-error publicados, fachada T023 e contexto T024; sem Firebase/HttpClient direto em componentes, autocadastro ou política de criação na senha de login. Aplicar identidade observada de gems.tec.br por tokens; cumprir SC-005. Depende de T033 aprovado e T013/T016/T017/T023/T024; permanece pendente. **LIMITAÇÃO aceita pelo usuário (2026-10-10): G13/G15/G17 de R005 fora do escopo enxuto; FE2B3 prova G12/G14, mas não demonstra o fechamento integral da UI adjudicada desta tarefa.** Ver [limitações DOC1](evidence.md#limitações-aceitas-pelo-usuário-2026-10-10).

## Dependências e execução

Ondas: contrato/revisão T001 (T002 preservada) → Java T003–T013 → Angular T014–T016 → adoção/backend/fachada/contexto T017–T024 → testes/UI login T033→T034 e demo T025 → cleanup/ambientes T026–T028 → E2E T029 → gates T031 → homologação T032 e benchmark T030. T029/T031/T032 dependem também de T034; T033/T034 não ficam fora da cadeia por terem IDs posteriores. T013/T016 são gate de publicação bloqueante: installlocal não habilita integração. Tarefas de teste precedem produção em cada bloco; gates de regressão abrangem código preservado. T032 não autoriza hosting; infraestrutura externa já prevista só executa com operador e mecanismos de segredo adequados.

Decisão de delegação explícita: root orchestrator solicitado GPT6.1Sol sob astra-orchestrator; executor documental governance_impl GPT6.1SolHigh, observado desconhecido, sem filhos/commits. Futuros contratos de implementação/review serão fechados e sequenciais; reviewer independente obrigatório para segurança/persistência. Não marcar build/test/publicação concluídos por estarem descritos aqui.

## Extensão documental atual

Pedido literal de login G&Ms em 2026-10-08 registrado no roteiro; pedido inicial para requisitos/documentação, seguido da autorização atual de desenvolvimento abaixo; P0 permanece documental. Executor doc_executor1, solicitado gpt-6.1-sol Medium, observado desconhecido; review independente posterior. T001 permanece aberta: revisão desta extensão não comprova todo contrato Java anteriormente materializado.

## Correções contratuais P0 — 2026-10-08

Java SDK usa Maven externo sob JDK 21; o sample preserva `backend/mvnw.cmd`. Release candidata: workflow `Release` (`Java-GEMS-SDK/.github/workflows/release.yml`), pela tag imutável `v3.4.0`, com verify e Auth Emulator antes de deploy. `publish.yml` é somente Main Validation; não existe comando executável `publishPackage` nem workflow_dispatch para Release. Depois de revisão/gates atuais e consulta de colisão no registry, sequência operacional: commit/PR Validation/Main Validation → tag imutável → Release → consumidor Maven remoto em M2 novo. Nenhuma dessas ações foi executada por P0.

Angular core 3.0.0 e adapter 1.0.0 publicados no commit `49b5eb94d684661aac13a2efb4a8bba3db7e5bf0`. Publicação PASS reconfirmada em D2; consumers npm remotos isolados com compiler/runtime PASS histórico recuperado, não reexecutados em D2/P0. Fontes: receipt `C:/Users/gabri/AppData/Local/Temp/gems-firebase-20261007/angular-release-result.json`, review independente `angular-state.json` v17, `.tooling/astra-sample-development-20261008/remote-evidence.json` e `readiness-report.md`. CI 37704998108 e releases 37705125391/37705315746 PASS no receipt. Shasums core `0d78e574112e84bdc867f7a0a374f8f555855700`, adapter `5f67626896b8753afd49a04341e4720b3ac31a77`. T015/T016 aceitas pelo root em angular-adjudication.json, 2026-10-08; T014 OPEN_PARTIAL pela cronologia individual teste-antes-edição não reconstruída. Adoção T017 pendente.

Autoridade atual recebida pelo root: **"Prossiga com o desenvolvimento"**, incluindo US4/FR-015/SC-005 e T033/T034. Esta instrução é distinta de PLAN_REV3_20261007; não constitui assinatura de mockup nem homologação.

T001 permanece aberta para revisão independente integral dos bytes finais. Somente o aceite root T015/T016 e critério Angular é projetado por P0; demais gates ficam abertos. Constituição 3.0.0, login G&Ms e invariantes Firebase/SQL/tenant/lifecycle preservados.

## Estado adjudicado P0 tentativa 2/3 — 2026-10-08

Fonte: [angular-adjudication.json](../../../.tooling/astra-sample-development-20261008/angular-adjudication.json), Astra root. Estado atual: **34 tarefas, 3 concluídas (T002/T015/T016), 31 abertas**; critério Angular do roteiro satisfeito e quatro critérios abertos. T014 **OPEN_PARTIAL**: baseline/cobertura/review favoráveis, cronologia individual teste-antes-edição não reconstruída. Angular publicação **PASS reconfirmado em D2**; consumers compiler/runtime **PASS histórico recuperado**, sem reexecução nesta tarefa.

Prova Java recebida: [p1-result.md](../../../.tooling/astra-sample-development-20261008/p1-result.md), SDK_JAVA_6: full verify **FAIL, 374 testes, 4 failures, 0 errors/skips**, 51 suites atuais. 14 testes históricos excluídos; 388 não é execução atual. Seis módulos downstream não executaram após parada em gems-firebase-auth. Auth Emulator **PASS, 5 testes, 0 failures/errors/skips**. Quatro casos J13 esperam USER_SUSPENDED na query e recebem zero; diagnóstico de prefixo BOOTSTRAP_ não corrigido/revalidado. As asserções finais pós-rollback/reuso de pool não foram alcançadas nesses casos.

Java orçamento **6/6, escaladas 0/1**, consulta de extensão pendente, sem reset. T003–T013/gates Java/publicação/consumer remoto abertos, sem aceite SDK. T001 **FAIL documental**, aguardando correções Java e review integral dos bytes finais. Sample/login T033/T034/conformance/E2E/homologação abertos. Diff e strict são gates documentais; strict comprova apenas snapshot_integrity, sem runtime sample. Autoridade **"Prossiga com o desenvolvimento"**, US4 e invariantes preservados.

## Reconciliação DOC1 — 2026-10-12

Fonte: evidências OBSERVADAS do coordenador no contrato DOC1, tentativa 1, ledger Astra; commits locais da branch `feature/003-firebase-multi-tenant`, ainda sem push. Estado documental atual: **34 tarefas, 23 concluídas, 11 abertas**. O status de 2026-10-09 no início e os estados anteriores permanecem como histórico; esta seção registra a atualização atual.

Novamente marcadas `[x]`: T017, T018, T020, T021, T025, T026 e T028. Mantidas `[x]` sem reexecução documental: T001–T013, T015, T016 e T027. Mantidas `[ ]`: T014, T019, T022, T023, T024, T029, T030, T031, T032, T033 e T034. A prova técnica atual não converte limitação aceita em tarefa concluída.

R003: BE1, BE2E e BE3C fecham as lacunas técnicas nomeadas de T017–T021/T025; B08/F05 conservam a limitação histórica em T019. R005: FE1D fecha G18, FE2A2 fecha G06/G09/G11 e FE2B3 fecha G12/G14. G13/G15/G17 ficam fora; o recorte autorizado não fornece a correspondência completa das demais lacunas R005 por tarefa. Por isso não se infere fechamento integral de T022/T023/T024/T029/T033/T034. Essa cautela documental não atribui novos IDs a essas tarefas nem declara falha das suítes observadas.

D001-B foi transcrita em [evidence.md](evidence.md#decisão-d001--transcrição). A assinatura humana na proposta de regularização continua pendente, sem edição ou assinatura por proxy. Publicação/consumidores SDK são provas das tarefas próprias T013/T016 e da release Java 3.4.1; os gates do sample não os substituem. CI remoto neste HEAD e homologação manual permanecem NOT_RUN. Nenhum build, rede, gate funcional ou commit foi executado por DOC1.

<a id="doc1-retificacao"></a>

## Retificação documental DOC1 — 2026-10-12

Os registros anteriores, inclusive os trechos com caracteres corrompidos, foram preservados. Esta seção e a [retificação de evidências](evidence.md#doc1-observadas) são o registro vigente em português do Brasil. Fonte: evidências OBSERVADAS pelo coordenador no ledger Astra, fornecidas no contrato DOC1, tentativa 1; não houve reexecução de builds ou consulta à rede pelo executor documental.

As caixas da lista principal já refletiam 23 tarefas concluídas no início deste contrato; nenhuma caixa foi alterada nesta execução. Permanecem marcadas `[x]`: **T001–T013, T015, T016, T017, T018, T020, T021, T025, T026, T027 e T028**. T017/T018/T020/T021/T025/T026/T028 têm a reconciliação sustentada pelas provas abaixo; as outras preservam o aceite anterior.

| Tarefa marcada `[x]` na reconciliação | Lacunas fechadas / prova recebida |
| :--- | :--- |
| T017 | BE2E `bfb8a50`, T017-G1: JAR carregado, metadados Maven, consumidor remoto em repositório local isolado e mutação de versão detectada |
| T018 | BE1 `54a9712` e checkpoint, T018-G1/G2 e GEMS-S1/S2 |
| T020 | BE3C `c8e8d53`/`80c88f4`, T020-G1/G2, falha de unlock contra SDK 3.4.1 |
| T021 | BE3C, T021-G1: constraints nomeadas com tipo, colunas e referência no forward e rollback |
| T025 | BE1, T025-G1/G2; FE2A2 `c61b091`, G06/G09/G11, review independente PASS |
| T026 | MIG2 `da59455` e MIG3B `8154aa3`, E2E/CI migrados ao Firebase real; execução remota neste HEAD NOT_RUN |
| T028 | `6c49201`, correction8 em duas cópias descartáveis, gates/invariantes PASS e unittest 26+24 |

As tarefas abaixo permanecem `[ ]`. Cada limitação remete à [seção de limitações de evidence.md](evidence.md#doc1-limitacoes); uma limitação aceita não equivale a tarefa concluída.

| Tarefa mantida `[ ]` | Anotação vigente / motivo |
| :--- | :--- |
| T014 | **LIMITAÇÃO aceita pelo usuário (2026-10-10): cronologia histórica de teste antes NÃO provada; D001-B/MUT1 é controle compensatório atual.** Ver [limitações](evidence.md#doc1-limitacoes). |
| T019 | **LIMITAÇÃO aceita pelo usuário (2026-10-10): cronologia histórica B08/F05 NÃO provada.** BE2E fecha T019-G1/G2/G3 e MUT1 prova mutação atual; não reconstitui cronologia. Ver [limitações](evidence.md#doc1-limitacoes). |
| T022 | **LIMITAÇÃO aceita pelo usuário (2026-10-10): G13/G15/G17 de R005 fora do escopo enxuto.** FE1D fecha G18; o recorte fornecido não demonstra fechamento integral das demais lacunas desta tarefa. Ver [limitações](evidence.md#doc1-limitacoes). |
| T023 | **LIMITAÇÃO aceita pelo usuário (2026-10-10): G13/G15/G17 de R005 fora do escopo enxuto.** FE2B3 fecha G12/G14; não se infere fechamento integral de R005 nesta tarefa. Ver [limitações](evidence.md#doc1-limitacoes). |
| T024 | **LIMITAÇÃO aceita pelo usuário (2026-10-10): G13/G15/G17 de R005 fora do escopo enxuto.** Não há fechamento integral demonstrado pelo recorte autorizado. Ver [limitações](evidence.md#doc1-limitacoes). |
| T029 | **LIMITAÇÃO aceita pelo usuário (2026-10-10): G13/G15/G17 de R005 fora do escopo enxuto.** MIG2 e T031 comprovam E2E real SUITE PASS; isso não encerra critérios frontend não demonstrados. Ver [limitações](evidence.md#doc1-limitacoes). |
| T030 | **LIMITAÇÃO aceita pelo usuário (2026-10-10): execução apenas com 100 schemas; 1000 = NOT_RUN.** Medidas observadas registradas, sem SLA. Ver [limitações](evidence.md#doc1-limitacoes). |
| T031 | Gates locais em `98db709` PASS; critério “termos operacionais zero” **NOT_RUN**, pois a spec não define os termos. CI remoto neste HEAD **NOT_RUN**. Ver [verificação T031](evidence.md#doc1-t031). Não há aceite atribuído ao usuário para dispensar esse critério. |
| T032 | **LIMITAÇÃO aceita pelo usuário (2026-10-10): homologação manual do usuário NOT_RUN até a execução dele.** Ver [limitações](evidence.md#doc1-limitacoes). **Superada em 2026-10-12:** homologação completa, ver [evidência](evidence.md#homologacao-local-20261012). |
| T033 | **LIMITAÇÃO aceita pelo usuário (2026-10-10): G13/G15/G17 de R005 fora do escopo enxuto.** FE2B3 fecha G12/G14; não se infere fechamento integral dos testes UI adjudicados. Ver [limitações](evidence.md#doc1-limitacoes). |
| T034 | **LIMITAÇÃO aceita pelo usuário (2026-10-10): G13/G15/G17 de R005 fora do escopo enxuto.** FE2B3 fecha G12/G14; não se infere cumprimento integral de SC-005 nem homologação humana. Ver [limitações](evidence.md#doc1-limitacoes). |

R003: os fechamentos técnicos nomeados vêm de BE1/BE2E/BE3C, conservando B08/F05 como limitação histórica em T019. R005: as provas nomeadas fecham G18, G06/G09/G11 e G12/G14; não foi inventada correspondência entre outras lacunas e tarefas. A decisão real D001 está [transcrita](evidence.md#doc1-d001); a assinatura da proposta continua **pendente**, reservada ao humano. Publicação/consumidores SDK pertencem a T013/T016 e à release 3.4.1, não ao gate do sample. Toda execução planejada e não observada permanece **NOT_RUN**.
