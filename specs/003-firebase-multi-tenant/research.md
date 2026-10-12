# Research — decisões e evidências da rodada 003

## Autoridades e fatos observados

Plano recebido aprovado e revisto em PLAN_REV3_20261007 PASS; autorização literal "Implement the plan.". Prompt anterior SHA7807071F34C5F784DA891DFD98F302037018C31B8733897C5EB373A1AF0B0955 preservado com emendas explícitas no próprio prompt. Constituição recebida2.0.0 era molde de outro modelo de identidade; agora3.0.0 é autoridade alvo.

Checkout oficial GEMS AI privado acessado via gh, pin 51b94e3a5479da74e06444f4133e07db85ece3cc. URL pública404 não prova ausência de repositório privado. Parser oficial usa arquivo `gems-ai.json`, `plan --project --task governance`, `apply --version 3.0.0 --dry-run`, `update --to 3.0.0 --dry-run`, `rules bundle --task --profile --format json`, `check --strict`, `verify-angular`. Sem flags multi-ID: a união de bundles é feita por ID, preservando cada regra/gate.

Snapshot original lock2.0.0 pin 394406d4da210d5c25f60a791199a4561a05a03a. Snapshot3.0.0 deve ser gerado exclusivamente pela CLI; preview oficial inclui ações/hashes, backups, adapters, lock e remoções. `check --strict` prova integridade e asserções reconhecidas; `verify-angular` só proíbe HttpClient em components, não autenticação, races ou toda conformance. Gate final precisa código novo E preservado com baseline zero.

SDK Java observada: `AuthorizationContextAware.authorizationContext()` retorna `JwtAuthorizationContext(globalProfiles,globalActions,tenantProfiles,tenantActions,Optional<String> tenantAlias)` real. `TenantScope.close()` restaura contexto anterior; limpar somente no close não protege thread já contaminada. `TenantIdentifierResolver` publicado lança sem contexto; bootstrap técnico deve ser opt-in aditivo, não fallback novo universal.

Regra canônica Angular auth3.0.0 descreve SDK publicada2.3.4 e data.roles/roleMode. Ela permite guard/fachada local de produto e contrato próprio de tenant. Não existe API Firebase nova na SDK publicada: fachada/authorizationScope são do sample. Segurança, constituição e API publicada prevalecem. Vocabulário genérico do provedor antigo no snapshot oficial é whitelist estreita; código/config/infra operacional do sample devem ter zero ocorrências, dependências e locks relacionados.

## Decisões congeladas

| Tema | Escolha e razão | Alternativa rejeitada |
| :--- | :--- | :--- |
| produto | molde eficiente IA, assinatura GEMS, exemplos mínimos removíveis | reconstruir domínio enterprise pesado |
| identidade | um projeto Firebase por produto/ambiente; Google+email/senha; projeto/emissor no banco | IdentityPlatform tenant por cliente |
| permissões | SQL vigente, dedup e interseção estrita catálogo/escopo, sem cache | Custom Claims roles/Firestore/cache |
| Google linking | UID primeiro, google.com+email_verified+cadastro pending único elegível, lock+uniques | e-mail como chave automática universal |
| password | Admin cria UID planejado; cliente usa envio nativo; nunca persistir segredo/action link | assumir conta remota por e-mail; SMTP/GmailAPI; Admin gera link e alegar envio |
| lifecycle | mutaçãoSQL+event/outbox atômicos, comandos idempotentes/versionados, bloqueio local primeiro | rollback local se remoto falhar |
| tenancy | schema tenant_ validado ASCII/LocaleROOT, modelo técnico, inventário READY e revisão mínima | header virar schema; fallback public |
| publicação | SDK publicada com consumidor externo isolado antes do sample | mvn install local como prova final |
| ambientes | local+CI+homolog real isolado agora | publicar hosting/VPS por premissa herdada |

## Referências oficiais para executores

As assinaturas e comportamentos precisam ser confrontados pelo executor de código com documentação oficial e tests do pin antes de adaptar contratos: [Admin setup](https://firebase.google.com/docs/admin/setup), [verify ID tokens](https://firebase.google.com/docs/auth/admin/verify-id-tokens), [manage sessions](https://firebase.google.com/docs/auth/admin/manage-sessions), [manage users](https://firebase.google.com/docs/auth/admin/manage-users), [emulator](https://firebase.google.com/docs/emulator-suite/connect_auth), [web Google](https://firebase.google.com/docs/auth/web/google-signin), [email actions](https://firebase.google.com/docs/auth/admin/email-action-links). Links são referências do plano, não alegação de consulta nesta tarefa documental.

`check-revoked=true` exige verificação real de revogação/disabled; verify default não prova isso. Modo real será testado com RSA+cert/http lookup stub, sem projeto real no CI. Emulator precisa opt-in local/test/ci e demo-id; unsigned emulator tokens recebem verificação extra de revogação mesmo quando propriedade false.

Referência Meduc nomeada pelo plano: `D:/repos/legacy/Meduc/meduc-workspace/backend/meduc-deploy/src/main/java/br/com/gems/meduc/persistence/tenant`, classes BootstrapTenantIdentifierResolver/BootstrapSchemaConnectionProvider/TenantHibernateConfiguration. Fora do READ deste executor: não lida/copied; executor futuro precisa contrato READ explícito para generalização.

## Riscos/pendências

Na preparação histórica de 2026-10-07, publicações Maven/npm/consumers eram NOT_RUN. Atualmente Angular publicação PASS reconfirmado e consumers compiler/runtime PASS histórico recuperado, não reexecutado; publicação/consumer remoto Java, migrations e provas de segurança/races do sample e homologação permanecem NOT_RUN. VersãoJava3.4.0 só é MINOR se compatibilidade aditiva for demonstrada; breaking exige decisão de release. Revisão final de contratos é independente e obrigatória. Hospedagem será decidida em rodada posterior; Opus review opcional não disponível. [evidence.md](evidence.md) registra apenas comandos realmente observados.

## Referência de marca e decisão de acesso — 2026-10-08

Consulta pública observada pelo coordenador via Playwright (DOM, CSS computado e screenshot) em [gems.tec.br](https://gems.tec.br/); título "G&Ms — Tecnologia à altura do seu negócio". Fonte dos tokens: [styles.css](https://gems.tec.br/assets/css/styles.css); [wordmark oficial](https://gems.tec.br/assets/logos/gems-wordmark.svg), alt G&Ms Soluções Tecnológicas. Dados medidos: bg `#f6f4ef`, surface `#fffefa`, ink `#24231f`, gold `#8a6425`, gold-bright `#c6a15b`, dark `#0a0a0a`; sans Plus Jakarta Sans/Arial/sans-serif e serif Cormorant Garamond/Georgia/serif. Estes são fatos da referência, não valores autorizados no CSS de componente nem layout aprovado.

Contrastes calculados WCAG: gold/surface 5,29:1; ink/bg 14,31:1; gold-bright/surface 2,41:1 (insuficiente para texto normal); gold-bright/dark 8,16:1. Selecionar pares que satisfaçam SC-005 no tema central. Dourado claro sobre creme pode ser decorativo, não texto normal. Não copiar fotografia do site. Tipografia deve usar configuração central comprovada, sem inventar API SDK.

Decisão requerida pelo usuário: US4/FR-015/SC-005, login próprio G&Ms para Google e senha/reset nativo, por fachada local e sem autocadastro. T033/T034 permanecem pendentes. A tela técnica Keycloak existente não satisfaz este requisito Angular/Firebase. A avaliação estática inicial de 2026-10-08 não executou publicação/consumers/runtime/homologação. Atualmente Angular publicação PASS reconfirmado e consumers PASS histórico recuperado; Java publicação/consumer remoto e sample runtime/homologação continuam NOT_RUN; [diagnóstico](development-status-20261008.md) distingue os SDKs avançados do sample ainda anterior.

## Estado adjudicado P0 tentativa 2/3 — 2026-10-08

Fonte: [angular-adjudication.json](../../../.tooling/astra-sample-development-20261008/angular-adjudication.json), Astra root. Estado atual: **34 tarefas, 3 concluídas (T002/T015/T016), 31 abertas**; critério Angular do roteiro satisfeito e quatro critérios abertos. T014 **OPEN_PARTIAL**: baseline/cobertura/review favoráveis, cronologia individual teste-antes-edição não reconstruída. Angular publicação **PASS reconfirmado em D2**; consumers compiler/runtime **PASS histórico recuperado**, sem reexecução nesta tarefa.

Prova Java recebida: [p1-result.md](../../../.tooling/astra-sample-development-20261008/p1-result.md), SDK_JAVA_6: full verify **FAIL, 374 testes, 4 failures, 0 errors/skips**, 51 suites atuais. 14 testes históricos excluídos; 388 não é execução atual. Seis módulos downstream não executaram após parada em gems-firebase-auth. Auth Emulator **PASS, 5 testes, 0 failures/errors/skips**. Quatro casos J13 esperam USER_SUSPENDED na query e recebem zero; diagnóstico de prefixo BOOTSTRAP_ não corrigido/revalidado. As asserções finais pós-rollback/reuso de pool não foram alcançadas nesses casos.

Java orçamento **6/6, escaladas 0/1**, consulta de extensão pendente, sem reset. T003–T013/gates Java/publicação/consumer remoto abertos, sem aceite SDK. T001 **FAIL documental**, aguardando correções Java e review integral dos bytes finais. Sample/login T033/T034/conformance/E2E/homologação abertos. Diff e strict são gates documentais; strict comprova apenas snapshot_integrity, sem runtime sample. Autoridade **"Prossiga com o desenvolvimento"**, US4 e invariantes preservados.
