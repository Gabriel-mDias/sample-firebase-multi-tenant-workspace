# Evidências documentais — 2026-10-07

Preparação documental concluída pelo executor governance_finish, GOVERNANCE_2,
work_key GOVERNANCE_FIREBASE, tentativa2/3, escaladas0/1. Modelo solicitado GPT6.1Sol High;
modelo/esforço observado desconhecido. Review independente no snapshot GOVERNANCE_2 de 2026-10-07: **NOT_RUN**; estado atual T001 FAIL documental abaixo.
Plano já autorizado por "Implement the plan." após PLAN_REV3_20261007 PASS;
nenhuma assinatura humana fabricada ou nova decisão de produto.

| Verificação | Resultado | Prova e limite |
| :--- | :--- | :--- |
| Configuração real gems-ai.json | PASS | versão3.0.0, commit51b94e3a5479da74e06444f4133e07db85ece3cc, waivers vazios |
| Pin do clone oficial | PASS | tool-head.stdout.txt no archive de instalação; checkout limpo em tool-status.stdout.txt |
| Preview oficial | PASS | update --to3.0.0 --dry-run, 105 destinos com ações/hashes/backups; destinos conferidos contra ownership |
| Aplicação oficial | PASS | transaction 20261007T210108Z479329; apply.json e backup transacional da CLI; nenhum managed file handedit |
| Strict integridade | PASS | status conformant, scope snapshot_integrity, 37 regras, strict true; não prova conformance do código |
| Cinco bundles/união por ID | PASS | security/workspace, persistence/java, multi-tenant/java, angular-auth/angular, angular-crud/angular; 20 IDs únicos; archive/cli-3.0.0-readonly |
| Adapters locais | PASS documental | .cursor/.github/.kilocode sincronizados com constituição/Firebase/multi-tenant; kilo.json preservado; fachada/guard locais com contratos/testes, sem API Firebase SDK inventada |
| sample_diff/sdk_diff exploratórios | PASS | argv/cwd/exit0 e hashes em archive/cli-3.0.0-install/commands.json; avisos CRLF/LF informativos |
| Provas terminais do controlador | ledger externo | state.py verify sample_diff/sdk_diff/toolkit_strict após congelar estes documentos; IDs/hashes/result no ledger, não em autodeclaração |
| Constituição/SIR/spec/plan/contratos/roteiro | preservados | provas documentais anteriores em observations.json.predecessor_attempt1; review atual NOT_RUN |
| T002 snapshot | concluída | configuração, preview, aplicação, strict e união observados; análise contratual documental existente preservada |
| T001 / T003+ | pendentes | review independente / implementação, builds, publicação, consumers, conformance, E2E e homologação NOT_RUN |

Comandos observados, na raiz do workspace:

- `powershell.exe -NoProfile -File .tooling/run-gems-ai.ps1 update --project sample-firebase-multi-tenant-workspace --to 3.0.0 --dry-run`
- Mesmo comando sem `--dry-run` aplica o snapshot oficial.
- `powershell.exe -NoProfile -File .tooling/run-gems-ai.ps1 check --project sample-firebase-multi-tenant-workspace --strict`
- `git diff --check` em sample e Java-GEMS-SDK; `git rev-parse HEAD` e `git status --short` no clone fixado.

Wrapper .tooling/run-gems-ai.ps1 aponta PYTHONPATH ao clone fixado, passa argumentos a
python -m gems_ai, restaura PYTHONPATH e propaga exit code; nenhuma credencial.
CLI3.0.0 parser/update --help lidos; nenhum update para HEAD remoto nem flags inventadas.
commands.json preserva saídas/hashes/exit; preview.json preserva destino/ação/hash antes da aplicação.
Observations.json contém SHA256 de configuração/lock/preview/apply/wrapper e provas históricas.

Ocorrências exploratórias: rg encontrou .kilocode opcional inexistente antes da criação autorizada.
Script inline de adapters parou ao encontrar front matter com BOM, após atualizar um adapter local;
script persistido UTF-8 usou utf-8-sig e corrigiu o conteúdo afetado pela codificação do console.
Não houve falha funcional de CLI/strict. Falhas do predecessor preservadas em archive/attempt1-evidence.md.
Avisos Git sobre CRLF/LF são informativos; arquivos gerenciados não foram normalizados manualmente.

Não houve código de produção, POM/package.json/compose/config runtime, conexão MCP/serviço,
instalação de serviço, credenciais, commits ou publicação nesta tarefa. PostgreSQL/Keycloak Docker
exige procurar enviroments (aqui environments) antes de conectar; nenhuma conexão foi feita.
Review do plano não substitui review independente destes bytes. Gates futuros continuam NOT_RUN.

Diagnóstico adicional: leitura manual inicial omitiu o sidecar state.json.data (FileNotFound); depois leitura JSON sem UTF-8 explícito produziu falso diff de nomes acentuados em utils. Coordenador conferiu API direta e git status -- utils: outside=[], pasta intacta. Comparação com UTF-8 explícito confirmou zero alterações fora do ownership; sem renames/rollback/amendamento.

## Extensão documental — 2026-10-08 / B1_DOCUMENTATION

Executor doc_executor1, tentativa1/3, escalada0/1; solicitado gpt-6.1-sol Medium, observado desconhecido. Entrada A1 aceita como auditoria estática pelo coordenador (audit-accepted.json no controle externo); marca observada via Playwright, CSS computado e screenshot (brand-reference.json). [Diagnóstico](development-status-20261008.md) registra fontes, limites e ordem. US4/FR-015/SC-005 e T033/T034 são requisitos/tarefas pendentes, sem código ou aprovação de mockup. Pedido literal atual separado da autorização histórica em roteiro.

Após congelar os 13 documentos, o controlador externo executará `diff-whitespace` e `gems-snapshot-strict` no contrato B1_DOCUMENTATION; IDs, exit codes, hashes e fingerprint atuais ficam no ledger/resultado externo, sem editar estes bytes depois da primeira verificação. Esta seção não antecipa PASS. `git diff --check` não cobre documentos untracked; leitura/revisão dos bytes e links é necessária. `check --strict` tem escopo snapshot_integrity; não comprova conformance do código nem baseline zero.

Snapshot histórico B1_DOCUMENTATION, doc_executor1 em 2026-10-08: 1/34 (somente T002 histórica), 33 abertas; estado atual 3/34 (T002/T015/T016), 31 abertas; observações e source hashes de observations.json preservados como história. Builds/tests/runtime/publicação/consumers/conformance/E2E/homologação/benchmark: NOT_RUN nesta tarefa. Review independente atual: pendente. Gerador consumer gerar-estado-rodada.ps1 N/A: ausente na lista .specify/scripts/powershell. Nenhum managed file, runtime, SDK, ambiente, segredo ou commit alterado.

## Correções contratuais P0 — 2026-10-08

Angular core 3.0.0 e adapter 1.0.0 publicados no commit `49b5eb94d684661aac13a2efb4a8bba3db7e5bf0`. Publicação PASS reconfirmada em D2; consumers npm remotos isolados com compiler/runtime PASS histórico recuperado, não reexecutados em D2/P0. Fontes: receipt `C:/Users/gabri/AppData/Local/Temp/gems-firebase-20261007/angular-release-result.json`, review independente `angular-state.json` v17, `.tooling/astra-sample-development-20261008/remote-evidence.json` e `readiness-report.md`. CI 37704998108 e releases 37705125391/37705315746 PASS no receipt. Shasums core `0d78e574112e84bdc867f7a0a374f8f555855700`, adapter `5f67626896b8753afd49a04341e4720b3ac31a77`. T015/T016 aceitas pelo root em angular-adjudication.json, 2026-10-08; T014 OPEN_PARTIAL pela cronologia individual teste-antes-edição não reconstruída. Adoção T017 pendente.

Autoridade atual recebida pelo root: **"Prossiga com o desenvolvimento"**, incluindo US4/FR-015/SC-005 e T033/T034. Esta instrução é distinta de PLAN_REV3_20261007; não constitui assinatura de mockup nem homologação.

T001 permanece aberta para revisão independente integral dos bytes finais. Somente o aceite root T015/T016 e critério Angular é projetado por P0; demais gates ficam abertos. Constituição 3.0.0, login G&Ms e invariantes Firebase/SQL/tenant/lifecycle preservados.

## Estado adjudicado P0 tentativa 2/3 — 2026-10-08

Fonte: [angular-adjudication.json](../../../.tooling/astra-sample-development-20261008/angular-adjudication.json), Astra root. Estado atual: **34 tarefas, 3 concluídas (T002/T015/T016), 31 abertas**; critério Angular do roteiro satisfeito e quatro critérios abertos. T014 **OPEN_PARTIAL**: baseline/cobertura/review favoráveis, cronologia individual teste-antes-edição não reconstruída. Angular publicação **PASS reconfirmado em D2**; consumers compiler/runtime **PASS histórico recuperado**, sem reexecução nesta tarefa.

Prova Java recebida: [p1-result.md](../../../.tooling/astra-sample-development-20261008/p1-result.md), SDK_JAVA_6: full verify **FAIL, 374 testes, 4 failures, 0 errors/skips**, 51 suites atuais. 14 testes históricos excluídos; 388 não é execução atual. Seis módulos downstream não executaram após parada em gems-firebase-auth. Auth Emulator **PASS, 5 testes, 0 failures/errors/skips**. Quatro casos J13 esperam USER_SUSPENDED na query e recebem zero; diagnóstico de prefixo BOOTSTRAP_ não corrigido/revalidado. As asserções finais pós-rollback/reuso de pool não foram alcançadas nesses casos.

Java orçamento **6/6, escaladas 0/1**, consulta de extensão pendente, sem reset. T003–T013/gates Java/publicação/consumer remoto abertos, sem aceite SDK. T001 **FAIL documental**, aguardando correções Java e review integral dos bytes finais. Sample/login T033/T034/conformance/E2E/homologação abertos. Diff e strict são gates documentais; strict comprova apenas snapshot_integrity, sem runtime sample. Autoridade **"Prossiga com o desenvolvimento"**, US4 e invariantes preservados.
