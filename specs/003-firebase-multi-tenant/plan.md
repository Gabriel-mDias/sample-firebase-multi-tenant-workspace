# Implementation Plan: Firebase multi-tenant GEMS

**Date**: 2026-10-07 · **Spec**: [spec.md](spec.md) · **Approval**: [roteiro.md](roteiro.md#aprovação-existente)
**Estado**: contrato documental; gates de implementação NOT_RUN.

## Summary

Entregar mecanismos reutilizáveis nas SDKs antes de integrar o molde. Java SDK candidata 3.4.0 MINOR somente se aditiva; Angular core 3.0.0 MAJOR e adapter separado 1.0.0. Sample preserva convenções de cinco módulos e UI GEMS, troca identidade/autorização/tenancy com testes e remove domínios pesados em favor de DemoRegistro removível. Sequência: governança → Java/publicação/consumidor → Angular/publicação/consumidor → sample/local/CI → homologação real isolada.

## Technical Context

Java 21; Spring Boot 4.1 (sample POM observado 4.1.1); PostgreSQL 16; Liquibase; JDBC security opt-in; JPA multi-tenant empresarial. Angular 22/TypeScript 6/Vitest/Prettier, UI/core GEMS publicado. Pins: Firebase Admin9.11.0, Firebase JS12.19.0, Firebase CLI 15.32.1. Auth Emulator9099, UI 4000, project demo-gems, somente como opção demo/offline do Compose local; suíte automatizada e CI usam o projeto Firebase real de testes `sample-firebase-multi-tenant` via ADC (secret `FIREBASE_TEST_SERVICE_ACCOUNT_JSON` em arquivo temporário no runtime), com usuários sintéticos em namespace por execução (prefixo + run id), limpeza garantida e varredura de órfãos restrita ao namespace, PostgreSQL descartável (Testcontainers) e indisponibilidade do provedor por fault injection local determinística; Google OAuth real e entrega de reset por e-mail ficam em verificação humana (decisão 6B, 2026-10-09). O secret é cadastrado pelo usuário; em PR de fork o job E2E real é pulado com aviso explícito, e no repositório principal a ausência do secret falha o job. Código atual ainda depende de mecanismos anteriores; documentação alvo não comprova sua substituição.

GEMS AI 3.0.0 pin 51b94e3a5479da74e06444f4133e07db85ece3cc, release2026-09-26. Angular publicado2.3.4 pin8a571633e18fff2f537e1fa203b6ef7cae9a2a35 deve ser base do novo checkout root Angular-GEMS-SDK; clone legado 2.1.0 não será alterado. Baselines iniciais: Java HEAD63dbb188ca3751e2accbe31e7a15b1af14432be6 e sample HEAD00ca8f14673f1788f6bceca5a0fd86bfbbe9454d.

## Constitution Check

Constituição 3.1.0 mantém a redefinição de identidade e tenancy da 3.0.0, preserva grafo Maven, APIs GEMS e UI. Contratos e roteiro materializam o mesmo plano aprovado; publicação é pré-condição para substituição no sample. Config/migrations de aplicação ficam em app-deploy. Port/JDBC auth da SDK não depende de JPA. Nenhuma exceção/baseline novo é autorizado. Integridade CLI e verify-angular não substituem conformance completa de código novo e preservado.

## Project Structure e responsabilidade

| Área | Responsabilidade |
| :--- | :--- |
| Java-GEMS-SDK/gems-firebase-auth | ports, verifier/Admin adapter, Authentication/context, JDBC e changelog security explicitamente opt-in |
| Java-GEMS-SDK/gems-jpa-multi-tenant | helpers bootstrap/modelo e migration coordinator generalizados opt-in; sem quebra de contratos antigos |
| Java-GEMS-SDK/docs/plans/firebase-multi-tenant | contrato de SDK e gate de release, referenciando esta rodada |
| Angular-GEMS-SDK | core sem metadados/transitivas do adapter; adapter separado preserva implementação publicada |
| backend/app-model | entidades empresariais/DTOs; administração global declarada |
| backend/app-security | ações, filtro stateless, classificação/scanner/interceptor, ligação ao contrato publicado |
| backend/app-events | POJOs; comandos duráveis de segurança não são eventos efêmeros |
| backend/app-core | DemoRegistro, services/repositories/controllers/admin/provisionamento; listeners com regra |
| backend/app-deploy | aplicação, propriedades, Liquibase, recursos e configuração dos beans |
| frontend/src/app/core/auth | fachada Firebase local, guards de produto e interceptor com generation |
| frontend/src/app/features | login/tenant selector e DemoRegistro pequeno removível |
| environments/shared/firebase e environments/local | emulador como opção demo/offline e infraestrutura local versionada |
| scripts/sample.py e scripts/inicializar-projeto.ps1 | cleanup fechado/seguro e rename preservando snapshot/import pins |

## Fases, dependências e delegação

| Fase | Modelo solicitado | Gate de saída |
| :--- | :--- | :--- |
| Governança | GPT6.1Sol High, executor governance_impl; observado desconhecido | artefatos consistentes, CLI oficial íntegra, diff e review independente |
| Java SDK | root seleciona por risco de segurança/persistência | clean verify + gate oficial + workflow `Release` (`Java-GEMS-SDK/.github/workflows/release.yml`), pela tag imutável `v3.4.0`, com verify e Auth Emulator antes de deploy + consumidor Maven M2 isolado |
| Angular SDK | root seleciona por quebra de API pública | core/adapter publicados + smoke npm isolado |
| Sample | root seleciona com contrato fechado por bloco | backend/frontend/PostgreSQL descartável + projeto Firebase real de testes + conformance real baseline zero |
| Homologação | operador central e testers | projeto real isolado + matriz manual; sem hosting novo |

Execução serial por padrão. Este executor não despacha filhos e não escreve código de produção, POM, package.json, compose ou configurações runtime. Contrato fechado e ledger Astra regem sua escrita. Reviewer independente não implementa. Opus consultivo indisponível nesta sessão, registrado sem bloquear; PLAN_REV3_20261007 PASS é a revisão de planejamento recebida, não review final da implementação.

## Complexity Tracking

| Mecanismo | Por que necessário | Alternativa rejeitada |
| :--- | :--- | :--- |
| JDBC security separado de JPA | resolver custom funciona sem DataSource; segurança global não depende de seleção de schema empresarial | usar EM de tenant para consultar segurança antes de autenticar |
| comando/outbox durável com versão | SQL e Firebase não compartilham transação; latest-version CAS impede enable antigo | compensação efêmera em memória |
| modelo para bootstrap opt-in | ddlvalidate precisa schema técnico antes de EM; contexto ausente continua falhando fechado | alias default/public silencioso |
| generation frontend | operações cruzam await e responses tardias | confiar somente em tenant selecionado no envio |
| adapter Angular separado | core/UI precisa remover metadados/transitivas do provedor anterior | apenas omitir import sem separar publicação |

Não há violação dispensada: estas escolhas concretizam invariantes da constituição 3.1.0. Backend mantido e código novo entram nos mesmos gates, sem ampliar baseline nem manter ratchet como prova final.

## Extensão de login G&Ms — 2026-10-08

US4/FR-015/SC-005 acrescentam apresentação própria de acesso ao fluxo já contratado. Criar feature standalone lazy em `frontend/src/app/features/login`, registrada por `loadChildren` na raiz, com `.ts/.html/.css` separados; integração pela fachada `core/auth` e contexto T023/T024. Rota pública de login não torna `/api/auth/me` ou `/api/auth/context` públicos. Testes T033 precedem UI T034; T013/T016/T017 continuam bloqueando integração de artefatos.

A referência visual é [gems.tec.br](https://gems.tec.br/), observada em 2026-10-08: creme/preto/dourado, wordmark e títulos serifados. Aplicar marca e tema centralmente via `provideGemsTheme` em `app.config.ts`; CSS de componentes só usa tokens, sem hex fixo ou override `--gems-*`. Mapear tipografia apenas por mecanismo central realmente suportado na API publicada; não pressupor novos exports/parâmetros. Logo oficial e assets locais serão tratados em T034; fotos do site não são requisito. Layout de card/painel é sugestão de implementação, não mockup homologado.

Gate de saída da UI: SC-005, `gems-input-password` e `gems-field-error` conforme API publicada, sem impor política de criação de senha a credenciais já existentes. Google/senha/reset nativo pela fachada; loading, mensagem genérica, contexto válido, retorno interno permitido e races existentes. Abranger T029/T031/T032. Modelo solicitado desta extensão documental: gpt-6.1-sol Medium, executor doc_executor1, observado desconhecido, sem filhos. Não executar código/infra nesta extensão. Diagnóstico e ordem: [development-status-20261008.md](development-status-20261008.md).

## Correções contratuais P0 — 2026-10-08

Java SDK usa Maven externo sob JDK 21; o sample preserva `backend/mvnw.cmd`. Release candidata: workflow `Release` (`Java-GEMS-SDK/.github/workflows/release.yml`), pela tag imutável `v3.4.0`, com verify e Auth Emulator antes de deploy. `publish.yml` é somente Main Validation; não existe comando executável `publishPackage` nem workflow_dispatch para Release. Depois de revisão/gates atuais e consulta de colisão no registry, sequência operacional: commit/PR Validation/Main Validation → tag imutável → Release → consumidor Maven remoto em M2 novo. Nenhuma dessas ações foi executada por P0.

Angular core 3.0.0 e adapter 1.0.0 publicados no commit `49b5eb94d684661aac13a2efb4a8bba3db7e5bf0`. Publicação PASS reconfirmada em D2; consumers npm remotos isolados com compiler/runtime PASS histórico recuperado, não reexecutados em D2/P0. Fontes: receipt `C:/Users/gabri/AppData/Local/Temp/gems-firebase-20261007/angular-release-result.json`, review independente `angular-state.json` v17, `.tooling/astra-sample-development-20261008/remote-evidence.json` e `readiness-report.md`. CI 37704998108 e releases 37705125391/37705315746 PASS no receipt. Shasums core `0d78e574112e84bdc867f7a0a374f8f555855700`, adapter `5f67626896b8753afd49a04341e4720b3ac31a77`. T014–T016 continuam abertas para adjudicação root; adoção T017 pendente.

Autoridade atual recebida pelo root: **"Prossiga com o desenvolvimento"**, incluindo US4/FR-015/SC-005 e T033/T034. Esta instrução é distinta de PLAN_REV3_20261007; não constitui assinatura de mockup nem homologação.

T001 permanece aberta para revisão independente integral dos bytes finais. Nenhum gate funcional ou tarefa SDK é fechado por P0. Constituição 3.0.0, login G&Ms e invariantes Firebase/SQL/tenant/lifecycle preservados.
