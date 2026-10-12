# Roteiro da rodada 003 — Firebase multi-tenant

**Data**: 2026-10-07 · **Status**: execução do MESMO plano já autorizada; provas de implementação pendentes.

## Objetivo e decisões

Molde GEMS para SaaS de um produto e referência de 100–1000 empresas, com segurança/isolation e exemplos mínimos removíveis. Identidade Google+email/password em um projeto Firebase por produto/ambiente; autorização SQL vigente/catalogscope. Administração central cria usuários/tenants/primeiro admin; tenant admin apenas membership/grants próprios com subconjunto efetivo delegável. Sem domínio pesado, sem migração Meduc/Apoia/ADACI, sem hosting novo/MFA/cache/Firestore/CustomClaimsroles/SMTP/GmailAPI.

## Banco/backend/frontend e raio de impacto

[data-model.md](data-model.md) materializa 15 tabelas de autorização + suporte de lifecycle/event/outbox, identidade com issuer/UID permanente e constraints de intervalos/tenant/scope. [backend](contracts/backend.md) divide ports/JDBC security opt-in de JPA tenant, descreve revogação de token/contexto/scanner/endpoints me/context/provisionjob. [frontend](contracts/frontend.md) fixa core Angular MAJOR/adapter separado e fachada LOCAL Firebase/guard com roles tipadas/authorizationScope/generation antes de await.

Raio de impacto é compartilhado: toda autenticação, autorização, tenancy, auditoria, catálogo, menu, HTTP e configuração local/CI do sample. Cinco módulos Maven e forma de CRUD/UI preservam convenções técnicas. Auditoria empresarial fica por tenant; global/security separadas. SDK Java deve ser publicado e provado externo antes sample; Angular core/adapter também publicados antes do consumo. Header só seleção validada, não confirmação de autoridade.

## Testes e ordem

[tasks.md](tasks.md) tem 34 tarefas: 16 concluídas (T001–T013, T015, T016, T027) e 18 abertas conforme o ledger Astra reconciliado em 2026-10-09 (R002); T026/T029 reabertas pela decisão 6B. Ordem serial governança→Java→Angular→adoção/backend/fachada/contexto→T033 testes/T034 login e demo→cleanup/CI→E2E/gates→homologação isolada e benchmark. Tests antes da lógica. Banco com toda cadeia/compositeFK/CHECK/dedup; token401/local403/provider503; UID Google elegível/issuer; outbox de lifecycle/restart/CAS da última versão; tenant READY/revision/locks de provisionamento; mesmaPK/requests/limpeza de thread; frontend deferred switch/logout/UID/response antiga/failure; manifesto de cleanup/junction/árvore limpa/rename com fundamentos preservados. Gates cobrem código novo E preservado, baseline zero.

Cada bloco tem gate publicado no [plan](plan.md) e [quickstart](quickstart.md). Instalação local não é consumer/publicação. Homolog manual dois testers Google/senha/reset nativo/revoke/disable/spoof. Bench 100/1000 mede tempo/mem/SQL sem SLA. Execução documental não despacha filhos; revisão independente final é obrigatória. Opus consultivo indisponível não bloqueia.

## Aprovação existente

Registro literal da autoridade recebida nesta conversa: **"Implement the plan."**, após proposed_plan completo aprovado e revisão independente **PLAN_REV3_20261007 — PASS**. O presente roteiro consolida o MESMO escopo e as mesmas decisões; não constitui nova proposta nem nova autorização. Origem: instrução do usuário nesta sessão, reproduzida pelo contrato de delegação do root. Não há assinatura fabricada, identidade autenticada pelo ledger ou aprovação humana por proxy.

Essa autorização satisfaz o gate de roteiro antes de código para este plano. Mudança material de produto/escopo futura exige decisão real do usuário; inexistência de hospedagem nesta fase permanece vinculante. Preview/mutação CLI gerenciada se limitam à atualização canônica já aprovada, sem waiver ou mudança de regras base.

## Critérios de conclusão

- [ ] Revisão independente atual aprova contratos/documentação e evidências desta materialização.
- [x] Java SDK full clean verify/gate oficial/publicação/consumer remoto isolado e compatibilidade aditiva — satisfeito por T013 aceita (java-release-adjudication), conforme tasks.md. Nota histórica: em 2026-10-08 este gate constava aberto.
- [x] Angular core+adapter publicados e consumer remoto isolado — aceite root 2026-10-08; consumers PASS histórico recuperado.
- [ ] Sample baseline zero/gates backend/frontend/E2E constraints/races/outbox/thread/isolation/cleanup pass.
- [ ] Homolog real isolada passa e bench 100/1000 registrado sem promessa de capacidade. *(2026-10-12: homologação em tela parcial aprovada, ver bloco abaixo; bench só com 100.)*

A preparação histórica de 2026-10-07 não marcou critérios satisfeitos; agora Java (T013) e Angular estão satisfeitos por aceite. [evidence.md](evidence.md) distingue PASS/FAIL/NOT_RUN; autoridade de aceite pertence ao root+reviewer, não ao executor.

## Necessidade acrescentada — 2026-10-08

Origem literal atual: "Acrescente entre as necessidades, uma tela de login customizada seguindo os padrões da G&Ms Soluções tecnológicas em gems.tec.br". Este pedido inicial acrescentou requisito e diagnóstico documental; a autorização atual de desenvolvimento incluindo US4 está registrada abaixo, sem assinatura de mockup ou execução da UI por P0. Não atribuir US4/FR-015/SC-005 a PLAN_REV3_20261007. A aprovação existente acima permanece histórica para o mesmo plano anterior.

[Spec](spec.md#us4--acesso-com-identidade-visual-gms-p1), [research](research.md), [contrato frontend](contracts/frontend.md) e [tasks](tasks.md) definem login próprio, Google/senha/reset nativo, acessibilidade e marca por tema central, sem autocadastro/auto-admin. T033 precede T034, depende de publicação/adoção T013/T016/T017 e fachada/contexto T023/T024, integra T029/T031/T032. Não altera fronteiras Firebase/SQL/tenant ou concede autoridade por UI/claims. Os critérios Java (T013) e Angular foram aceitos; três critérios permanecem abertos; review desta extensão não encerra T001. Diagnóstico: [development-status-20261008.md](development-status-20261008.md).

Estado consumer da rodada: geração por `gerar-estado-rodada.ps1` N/A, arquivo não existe em `.specify/scripts/powershell` (nomes inspecionados). Nenhuma rodada concorrente/inicialização/advance ou managed file foi alterado.

## Correções contratuais P0 — 2026-10-08

Angular core 3.0.0 e adapter 1.0.0 publicados no commit `49b5eb94d684661aac13a2efb4a8bba3db7e5bf0`. Publicação PASS reconfirmada em D2; consumers npm remotos isolados com compiler/runtime PASS histórico recuperado, não reexecutados em D2/P0. Fontes: receipt `C:/Users/gabri/AppData/Local/Temp/gems-firebase-20261007/angular-release-result.json`, review independente `angular-state.json` v17, `.tooling/astra-sample-development-20261008/remote-evidence.json` e `readiness-report.md`. CI 37704998108 e releases 37705125391/37705315746 PASS no receipt. Shasums core `0d78e574112e84bdc867f7a0a374f8f555855700`, adapter `5f67626896b8753afd49a04341e4720b3ac31a77`. T015/T016 aceitas pelo root em angular-adjudication.json, 2026-10-08; T014 OPEN_PARTIAL pela cronologia individual teste-antes-edição não reconstruída. Adoção T017 pendente.

Autoridade atual recebida pelo root: **"Prossiga com o desenvolvimento"**, incluindo US4/FR-015/SC-005 e T033/T034. Esta instrução é distinta de PLAN_REV3_20261007; não constitui assinatura de mockup nem homologação.

T001 permanece aberta para revisão independente integral dos bytes finais. Somente o aceite root T015/T016 e critério Angular é projetado por P0; demais gates ficam abertos. Constituição 3.0.0, login G&Ms e invariantes Firebase/SQL/tenant/lifecycle preservados.

## Estado adjudicado P0 tentativa 2/3 — 2026-10-08

Fonte: [angular-adjudication.json](../../../.tooling/astra-sample-development-20261008/angular-adjudication.json), Astra root. Estado atual: **34 tarefas, 3 concluídas (T002/T015/T016), 31 abertas**; critério Angular do roteiro satisfeito e quatro critérios abertos. T014 **OPEN_PARTIAL**: baseline/cobertura/review favoráveis, cronologia individual teste-antes-edição não reconstruída. Angular publicação **PASS reconfirmado em D2**; consumers compiler/runtime **PASS histórico recuperado**, sem reexecução nesta tarefa.

Prova Java recebida: [p1-result.md](../../../.tooling/astra-sample-development-20261008/p1-result.md), SDK_JAVA_6: full verify **FAIL, 374 testes, 4 failures, 0 errors/skips**, 51 suites atuais. 14 testes históricos excluídos; 388 não é execução atual. Seis módulos downstream não executaram após parada em gems-firebase-auth. Auth Emulator **PASS, 5 testes, 0 failures/errors/skips**. Quatro casos J13 esperam USER_SUSPENDED na query e recebem zero; diagnóstico de prefixo BOOTSTRAP_ não corrigido/revalidado. As asserções finais pós-rollback/reuso de pool não foram alcançadas nesses casos.

Java orçamento **6/6, escaladas 0/1**, consulta de extensão pendente, sem reset. T003–T013/gates Java/publicação/consumer remoto abertos, sem aceite SDK. T001 **FAIL documental**, aguardando correções Java e review integral dos bytes finais. Sample/login T033/T034/conformance/E2E/homologação abertos. Diff e strict são gates documentais; strict comprova apenas snapshot_integrity, sem runtime sample. Autoridade **"Prossiga com o desenvolvimento"**, US4 e invariantes preservados.

## Emenda 6B — 2026-10-09

Decisão do usuário, registrada literalmente: **"6 - opcao b"**, com o contexto "Esse projeto em si é um TESTE" e a instrução de usar o ambiente Firebase entregue. Efeito: a suíte automatizada, inclusive o CI, passa a usar o projeto Firebase real de testes `sample-firebase-multi-tenant` via ADC, com o secret `FIREBASE_TEST_SERVICE_ACCOUNT_JSON` materializado em arquivo temporário no runtime; usuários sintéticos usam namespace por execução (prefixo + run id no e-mail/UID), com limpeza garantida e varredura de órfãos restrita ao namespace; o PostgreSQL continua descartável (Testcontainers); o emulador fica apenas como opção demo/offline do Compose local; a indisponibilidade do provedor é testada por fault injection local determinística, nunca derrubando o Firebase compartilhado; Google OAuth real e entrega de reset por e-mail saem da automação e passam a verificação humana. Afeta FR-013, T026, T029, plan, quickstart, contracts/operations e contracts/backend; a constituição 3.1.0 (MINOR) registra a emenda. A ratificação vem desta decisão do usuário na data; nenhuma assinatura humana adicional foi criada.

## Critérios de aprovação — homologação

> ✅ **HOMOLOGADA pelo product owner em 2026-10-12**, em tela, com dado real de negócio, no escopo
> percorrido. O ambiente foi local, com o projeto Firebase real de testes e um PostgreSQL dedicado.
> Foram percorridos: login Google real de dois usuários (Operador central e Tester só em Beta), o
> isolamento Alpha/Beta com registros cadastrados pelo usuário, o campo restrito oculto para quem
> não tem a ação, o CRUD do DemoRegistro, a auditoria por tenant no banco e a gestão de usuários,
> vínculos e concessões pelo backend. Fala do usuário: "Aqui, está perfeito até o momento.
> Acredito que podemos prosseguir para a próxima fase". A rodada libera a fase de História.
> Depois, no mesmo dia, o usuário aprovou o fluxo por senha: "login e senha após o reset/primeiro
> acesso: OK!". Disable e revogação com o token anterior do Tester retornaram 401, e a reabilitação
> retornou 200. O spoof de `X-Tenant-Alias` com o token real do Tester retornou 403 para Alpha e
> para alias inexistente, e 200 para Beta. Revogação de grant, encerramento de vínculo, suspensão
> com worker e reativação se comportaram como o contrato, sem logout. A decisão do usuário de devolver
> a ação básica de conta na reativação ficou registrada como mudança futura (achado 4 da evidência).
> **Fora do percorrido, NOT_RUN:** retorno externo e responsividade e acessibilidade do login. Esses itens mantêm a
> T032 `[ ]`. Detalhe e achados da preparação estão em
> [evidence.md](evidence.md#homologacao-local-20261012).

#### Números medidos em 2026-10-12

As medições vêm do CI run 38295576662, no merge `c111d8e`, e do T030 no HEAD `7080bc7`. A coluna
"Antes" fica sem valor porque a rodada não mediu a linha de base.

| Métrica | Antes | Depois |
| :--- | ---: | ---: |
| Testes de backend (reator inteiro) | — | **290** |
| — `app-security` | — | 1 |
| — `app-core` | — | 87 |
| — `app-deploy` (integração, Testcontainers e E2E com Firebase real) | — | 202 |
| Testes de frontend (Vitest) | — | **419** em 24 arquivos |
| Ações do catálogo: globais / tenant | — | 14 / 12 |
| Provisionamento N=100 (T030): p50 / p95 / máx por tenant | — | 1,43 s / 1,56 s / 1,69 s |
| Prova por mutação (MUT1, D001-B) | — | 10/10 detectadas |