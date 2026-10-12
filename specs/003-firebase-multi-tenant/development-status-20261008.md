# Estado do desenvolvimento — 2026-10-08

O sample permanece na base Keycloak e single-tenant observada; Firebase multi-tenant é o alvo documental, ainda sem integração comprovada. Java segue candidata local sem publicação/consumer remoto demonstrados; Angular tem publicação e consumers remotos recuperados abaixo. Adoção no sample permanece pendente. US4/FR-015/SC-005 acrescentam a necessidade de login próprio G&Ms, sem implementação nesta entrega.

## Escopo e método

Auditoria A1 estática aceita pelo coordenador, conferência documental e referência pública de marca observada em 2026-10-08. Não foram executados builds, testes, Docker, conexão a banco, fluxos de login ou exploração de vulnerabilidades. "Não identificado" refere-se ao escopo inspecionado, não a prova runtime de inexistência. Fontes de código abaixo são relativas à raiz do sample; SDKs são checkouts irmãos. Constituição 3.0.0 e spec representam alvo; código representa estado observado.

## Requisito × estado × fonte

| Requisito/objetivo | Estado observado | Fonte concreta e lacuna |
| :--- | :--- | :--- |
| FR-001/002 identidade Firebase, Google/senha e pré-provisionamento | Sample ainda usa Keycloak | [auth.service.ts](../../frontend/src/app/core/auth/auth.service.ts):2,14,38–39,50,55; [app.config.ts](../../frontend/src/app/app.config.ts):8,29; [auth.guard.ts](../../frontend/src/app/core/guards/auth.guard.ts):18. Fachada Firebase T023 pendente |
| FR-003/004/005 SQL vigente/catalog/scope e delegação | Contrato novo não identificado no sample | [SecurityConfig.java](../../backend/app-security/src/main/java/br/com/gems/app/security/config/SecurityConfig.java):86–87,99,132,141–151 libera /api/auth/** e usa claims; [Usuario.java](../../backend/app-model/src/main/java/br/com/gems/app/model/entity/seguranca/Usuario.java):64,67 contém CD_KEYCLOAK_ID/FL_ATIVO. T018/T019 pendentes |
| FR-004/009 guard espelha ações/scopes | Divergência de contrato estática | [role.guard.ts](../../frontend/src/app/core/guards/role.guard.ts):41,58 lê data.authorization e scope global; parceiro.routes.ts:19,26,33,40 e solicitacao.routes.ts:21,28,35,42 fornecem data.roles; routes-coverage.spec.ts:70 também usa roles. Provável negação de rotas; falha aberta/exploit não observados. T022/T023/T029 precisam provar comportamento/mutações |
| FR-007 lifecycle durável | Compensação remota em memória na base | UsuarioKeycloakListener.java:33,36,65–69,91; outbox/versão/CAS alvo não identificado. T007/T008/T019/T029 pendentes |
| FR-008/010 isolamento e auditoria empresarial | Schema public e auditoria fixa | application.yml:18 default_schema public; RegistroAuditoria.java:61 e CampoAlterado.java:46 schema auditoria. TenantScope/membership/scopes/selector alvo não identificados no sample; T020–T024/T029 pendentes |
| FR-011 factory/demo/cleanup | Domínios anteriores permanecem | [app.routes.ts](../../frontend/src/app/app.routes.ts):22,27 registra parceiros/solicitações; DemoRegistro e scripts/sample.py ausentes no escopo observado. T025/T027/T028 pendentes |
| FR-012 SDK publicada antes de consumo | Java candidata; Angular publicada; sample em versões anteriores | Java POM/BOM candidatos 3.4.0; Firebase untracked e POMs JPA modificados; release-gates.md:3–6,16–18 publicação/consumer NOT_RUN. Angular publicada no commit 49b5eb9 separa core 3.0.0 (package:3) e adapter 1.0.0 (package:17,26–27). Sample backend/pom.xml:34 ainda 3.3.0; frontend/package-lock.json:1626–1628 ainda Angular SDK 2.3.0. T013/T017 abertos; T016 aceita pelo root |
| FR-013/014 ambientes/homologação/100–1000 | Alvo emulador/benchmark não identificado | environments/shared/firebase e scripts/benchmark-tenants ausentes no escopo observado. T026/T030/T032 pendentes; 100–1000 sem SLA |
| FR-015/SC-005 login próprio G&Ms | Necessidade nova, sem tela Angular/Firebase observada | app.config.ts:34–36 usa paleta neutra; app.html:3 sem logo. Tema Keycloak existe em environments/keycloak-theme/app/login: login.ftl:23 url.loginAction,108 sem social; template.ftl:89–102 sem marca; realm:40,44 seleciona tema. Tema técnico não satisfaz US4; T033/T034 pendentes |

Fontes auxiliares: [listener](../../backend/app-core/src/main/java/br/com/gems/app/usuario/listener/UsuarioKeycloakListener.java), [RegistroAuditoria](../../backend/app-model/src/main/java/br/com/gems/app/model/entity/auditoria/RegistroAuditoria.java), [CampoAlterado](../../backend/app-model/src/main/java/br/com/gems/app/model/entity/auditoria/CampoAlterado.java), [rotas parceiro](../../frontend/src/app/features/parceiro/parceiro.routes.ts), [rotas solicitação](../../frontend/src/app/features/solicitacao/solicitacao.routes.ts), [teste de cobertura](../../frontend/src/app/core/guards/routes-coverage.spec.ts), [POM sample](../../backend/pom.xml), [lock sample](../../frontend/package-lock.json), [POM Java SDK](../../../Java-GEMS-SDK/pom.xml), [BOM Java SDK](../../../Java-GEMS-SDK/gems-bom/pom.xml), [release gates](../../../Java-GEMS-SDK/docs/plans/firebase-multi-tenant/release-gates.md), [manifesto core Angular](../../../Angular-GEMS-SDK/projects/gems-sdk/package.json) e [manifesto adapter](../../../Angular-GEMS-SDK/projects/gems-keycloak-auth/package.json). Números referem-se ao snapshot estático A1, não a execução runtime.

## Leitura do progresso

[tasks.md](tasks.md) contém 34 tarefas: 3 concluídas (T002/T015/T016) e 31 abertas. Antes desta extensão havia 32 tarefas, 1 concluída e 31 abertas; corrigida a afirmação anterior do roteiro de que todas estavam abertas. O critério Angular foi aceito pelo root; quatro critérios permanecem abertos. Checkbox não mede percentual de produto: T002 comprova snapshot_integrity histórico (37 regras), sem demonstrar código Firebase, conformance ou baseline zero. T001 permanece aberta, pois esta revisão não valida todo o contrato Java materializado anteriormente.

Java candidata 3.4.0 segue sem release/consumer remoto comprovados; Angular tem publicação e consumers remotos recuperados abaixo. Adoção no sample permanece pendente. Não fechar tarefas SDK por arquivo presente/commit. Compatibilidade, gates oficiais, publicação remota e consumidor isolado continuam necessários. A avaliação estática inicial não executou compilação/testes; agora as provas SDK recebidas estão discriminadas abaixo.

## Necessidade de login e identidade visual

Pedido literal do usuário: "Acrescente entre as necessidades, uma tela de login customizada seguindo os padrões da G&Ms Soluções tecnológicas em gems.tec.br". Pedido inicial de acréscimo documental, seguido da autorização atual de desenvolvimento abaixo, separado do "Implement the plan." histórico após PLAN_REV3_20261007; não registra assinatura de mockup nem execução da UI.

Referência [gems.tec.br](https://gems.tec.br/), [CSS oficial](https://gems.tec.br/assets/css/styles.css) e [wordmark oficial](https://gems.tec.br/assets/logos/gems-wordmark.svg) observados por Playwright/DOM/CSS/screenshot. Creme/preto/dourado e títulos serifados são fatos da marca; layout de card/painel é apenas sugestão. Tokens/tipografia/contrastes medidos em [research.md](research.md). Gold-bright sobre surface tem 2,41:1, insuficiente para texto normal; SC-005 exige pares acessíveis. Tema central provideGemsTheme, tokens em componentes e mecanismo de tipografia realmente suportado; nenhum export SDK inventado ou hex/override local. Não copiar fotos.

Login terá Google, e-mail/senha existente e reset nativo por fachada; português, loading/submit único, mensagens genéricas/erros anunciados, teclado/foco/labels, 320/768/1440 sem overflow. Sem autocadastro/auto-admin/claims de autoridade ou Firebase/HTTP direto em componentes. Retorno só interno permitido, me/context válidos e seleção de tenant após autenticação; negar não elegíveis sem vazamento. T033 testa antes de T034; contexto/races/logout continuam sujeitos a FR-009. [Contrato frontend](contracts/frontend.md).

## Próxima ordem de execução

1. Concluir review integral T001 e gates/release/consumer remoto Java T013; preservar T014 OPEN_PARTIAL e aceite T015/T016; não integrar artefatos locais por atalho.
2. Provar adoção T017, implementar backend SQL/scopes/identidade/lifecycle/migrations/provisionamento T018–T021; remover duplicatas na mesma alteração.
3. Testar e implementar fachada/generation/contexto T022–T024, resolver drift roles/scopes; executar T033 antes da UI T034, com marca central e acesso seguro.
4. Entregar demo removível T025, cleanup/rename e ambientes emulador/CI T026–T028.
5. Executar E2E/mutações de scanner/guard/catalog/constraints/races/outbox/thread/mesma PK e login T029; gates completos/conformance baseline zero T031.
6. Homologar projeto real isolado com dois testers T032 e medir benchmark 100/1000 T030, sem hosting ou promessa de capacidade.

## Gates e limites

No snapshot histórico B1_DOCUMENTATION, doc_executor1 em 2026-10-08, builds/testes/releases/consumers não foram executados nesta avaliação. Estado SDK recebido atualizado abaixo; gates sample/conformance/E2E/homologação/benchmark permanecem NOT_RUN. Checks documentais atuais ficam no ledger externo B1_DOCUMENTATION; diff-whitespace não cobre untracked e strict só prova integridade do snapshot. Review independente dos bytes finais é obrigatório. Observations.json e hashes anteriores permanecem evidência histórica. Gerador consumer gerar-estado-rodada.ps1: N/A, ausente em .specify/scripts/powershell; nenhuma nova rodada ou projeção concorrente criada. Nenhuma mudança runtime, infraestrutura, SDK, managed files ou publicação autorizada nesta tarefa.

## Correções contratuais P0 — 2026-10-08

Angular core 3.0.0 e adapter 1.0.0 publicados no commit `49b5eb94d684661aac13a2efb4a8bba3db7e5bf0`. Publicação PASS reconfirmada em D2; consumers npm remotos isolados com compiler/runtime PASS histórico recuperado, não reexecutados em D2/P0. Fontes: receipt `C:/Users/gabri/AppData/Local/Temp/gems-firebase-20261007/angular-release-result.json`, review independente `angular-state.json` v17, `.tooling/astra-sample-development-20261008/remote-evidence.json` e `readiness-report.md`. CI 37704998108 e releases 37705125391/37705315746 PASS no receipt. Shasums core `0d78e574112e84bdc867f7a0a374f8f555855700`, adapter `5f67626896b8753afd49a04341e4720b3ac31a77`. T015/T016 aceitas pelo root em angular-adjudication.json, 2026-10-08; T014 OPEN_PARTIAL pela cronologia individual teste-antes-edição não reconstruída. Adoção T017 pendente.

Autoridade atual recebida pelo root: **"Prossiga com o desenvolvimento"**, incluindo US4/FR-015/SC-005 e T033/T034. Esta instrução é distinta de PLAN_REV3_20261007; não constitui assinatura de mockup nem homologação.

T001 permanece aberta para revisão independente integral dos bytes finais. Somente o aceite root T015/T016 e critério Angular é projetado por P0; demais gates ficam abertos. Constituição 3.0.0, login G&Ms e invariantes Firebase/SQL/tenant/lifecycle preservados.

## Estado adjudicado P0 tentativa 2/3 — 2026-10-08

Fonte: [angular-adjudication.json](../../../.tooling/astra-sample-development-20261008/angular-adjudication.json), Astra root. Estado atual: **34 tarefas, 3 concluídas (T002/T015/T016), 31 abertas**; critério Angular do roteiro satisfeito e quatro critérios abertos. T014 **OPEN_PARTIAL**: baseline/cobertura/review favoráveis, cronologia individual teste-antes-edição não reconstruída. Angular publicação **PASS reconfirmado em D2**; consumers compiler/runtime **PASS histórico recuperado**, sem reexecução nesta tarefa.

Prova Java recebida: [p1-result.md](../../../.tooling/astra-sample-development-20261008/p1-result.md), SDK_JAVA_6: full verify **FAIL, 374 testes, 4 failures, 0 errors/skips**, 51 suites atuais. 14 testes históricos excluídos; 388 não é execução atual. Seis módulos downstream não executaram após parada em gems-firebase-auth. Auth Emulator **PASS, 5 testes, 0 failures/errors/skips**. Quatro casos J13 esperam USER_SUSPENDED na query e recebem zero; diagnóstico de prefixo BOOTSTRAP_ não corrigido/revalidado. As asserções finais pós-rollback/reuso de pool não foram alcançadas nesses casos.

Java orçamento **6/6, escaladas 0/1**, consulta de extensão pendente, sem reset. T003–T013/gates Java/publicação/consumer remoto abertos, sem aceite SDK. T001 **FAIL documental**, aguardando correções Java e review integral dos bytes finais. Sample/login T033/T034/conformance/E2E/homologação abertos. Diff e strict são gates documentais; strict comprova apenas snapshot_integrity, sem runtime sample. Autoridade **"Prossiga com o desenvolvimento"**, US4 e invariantes preservados.
