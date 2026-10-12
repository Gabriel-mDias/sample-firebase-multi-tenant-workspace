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

## Evidências observadas DOC1

Registro documental em 2026-10-12, contrato DOC1, tentativa 1, executor codex-sol-17. Fonte: relato de evidências OBSERVADAS do coordenador, ledger Astra, fornecido no pedido DOC1. Os commits do sample estão na branch `feature/003-firebase-multi-tenant`, ainda sem push; HEAD de referência dos gates: `98db709`. As provas abaixo foram recebidas, não reexecutadas por este executor. As seções anteriores são histórico, inclusive falhas e estados abertos anteriores; não foram apagadas.

| Identificador / tarefa | Evidência observada e resultado | Limite |
| :--- | :--- | :--- |
| FE1D | `9135b24`: G18, base da API e `parseApiBase` recusando base relativa; review independente PASS | Não demonstra todas as lacunas R005 |
| FE2A2 / T025 | `c61b091`: G06/G09/G11 em DemoRegistro; review independente PASS | Fechamento frontend nomeado para o demo |
| FE2B3 | `a2d7eff`: G12/G14, login, retorno seguro e rotas canônicas; 419 testes e 8/8 mutações detectadas; review independente PASS | G13/G15/G17 fora do escopo aceito |
| MIG2 / T029 | `da59455`: E2E com Playwright, PostgreSQL descartável e Firebase real de testes; revogação real com tratamento do desvio de relógio e recuperação da outbox | PASS técnico não comprova todos os critérios R005; homologação manual separada |
| MIG3B / T026 | `8154aa3`: CI com E2E real, secret materializado somente no runtime e PR de fork pulando com aviso | Configuração implementada; CI remoto neste HEAD NOT_RUN |
| BE1 / T018, T025 | `54a9712` e checkpoint: T018-G1/G2, T025-G1/G2 e GEMS-S1/S2 | Evidência recebida do coordenador |
| SDK Java 3.4.1 / T013 | Cinco defeitos reais corrigidos: primeiro administrador duplicado, bootstrap não idempotente, READY antes do unlock, sessão com lock herdado e log com mensagem da exceção. PR #47 com merge; tag `v3.4.1`; workflow Release verde; pacote publicado no GitHub Packages; clean verify com 18 módulos SUCCESS; mutações detectadas no SDK | Prova da release SDK, separada do gate do sample |
| BE2E / T017, T019 | `bfb8a50`: adoção SDK 3.4.1; T017-G1 com versão pelo JAR carregado e metadados Maven e consumidor com repositório Maven local isolado baixando do registro; T019-G1/G2/G3: Administration 41, Identity 13, Lifecycle 4, Adoption 1+7; mutação `gems.version=3.4.0` detectada | Cronologia B08/F05 não provada |
| BE3C / T020, T021 | `c8e8d53` e `80c88f4`: T020-G1/G2, incluindo falha de unlock contra 3.4.1; T021-G1 com nomes explícitos de constraints, validação de tipo, colunas e referência no forward e rollback; 3 mutações detectadas | Evidência recebida do coordenador |
| MUT1 / D001-B | 10/10 mutações detectadas, controles verdes. T014 no Angular-GEMS-SDK HEAD `49b5eb9`: export renomeado, versão divergente, keycloak no core e every→some. B08: rota do first-administrator, bootstrap sem ações, grant MEMBER→PROFILE e validação de e-mail. F05: PASSWORD pela porta Google e modo identity-worker | CAS, ACK e eleição vivem no SDK Java e foram provados por mutação no próprio SDK; cronologia histórica não provada |
| T028 | `6c49201`: initializer com Spotless pós-rename; clean-demo com Prettier e transformações corrigidas; regressão G16 cobre referências sobreviventes, fundamentos enfraquecidos, helpers/ações órfãos, contagens fixas e retornos rejeitados. Harness correction8 em duas cópias descartáveis (default `br.com.gems` e custom `com.acme`): init, replay idempotente, clean-demo, backend clean install com smoke E2E real, frontend test/build/prettier e invariantes, todos PASS; unittest 26+24 | Prova observada nas cópias descartáveis |
| MEM1 | `b58169b`: `spring.test.context.cache.maxSize=2`; Surefire do app-deploy com `-Xmx1536m`, SerialGC e um fork | Não limita a JVM extra do E2E |
| T030 | `7080bc7` e `98db709`, N=100: provisionamento p50 1,43 s, p95 1,56 s, máximo 1,69 s por tenant, total 143,5 s; inventário 10,4 ms; heap em torno de 60 MB; 12.203 statements SQL; PostgreSQL 16.15; SDK 3.4.1 | N=1000 NOT_RUN; sem SLA |
| T031 | HEAD `98db709`: backend `mvnw.cmd clean install`, 290 testes, 0 falhas e T029 SUITE PASS no Firebase real; frontend `npm ci`, test, build e `prettier --check`; `gems-ai-gate` strict mais conformance; `verificar-contexto-agentes`; checker do CI. Todos com exit 0; review independente PASS | Gates locais observados; termos operacionais e CI remoto tratados abaixo |
| T013 / T016 | Publicação e consumidores isolados SDK comprovados nas tarefas próprias e na release Java 3.4.1; evidências Angular históricas acima preservadas | Não são inferidos do gate do sample e não foram reexecutados em DOC1 |

### Decisão D001 — transcrição

- Data da decisão real: **2026-10-10**.
- Fonte: resposta humana no **AskUserQuestion**, transcrita pelo coordenador no contrato DOC1 (ledger Astra).
- Transcrição literal: **"B — mutação atual"**.
- Efeito autorizado: mutação atual como controle compensatório; a cronologia histórica de T014/B08/F05 continua **NÃO provada**, sem forjar teste antes.
- A seção **Decisão** de `proposta-regularizacao-tdd-historico.md` continua reservada ao humano: **assinatura pendente**. DOC1 não editou esse arquivo, não preencheu nomes/datas de assinatura e não assinou por proxy. Esta transcrição não cria waiver.

### Limitações aceitas pelo usuário (2026-10-10)

1. **R005 G13/G15/G17:** ficam fora do escopo enxuto. As evidências recebidas fecham G18, G06/G09/G11 e G12/G14, sem demonstrar a correspondência integral das demais lacunas R005 por tarefa. T022/T023/T024/T029/T033/T034 conservam `[ ]`; não se presume que uma suíte verde encerre lacunas não nomeadas. T025 tem fechamento nomeado no backend e no frontend.
2. **T030:** somente 100 schemas executados; 1000 = **NOT_RUN**. As medidas não constituem SLA.
3. **D001-B / T014, B08 e F05:** cronologia histórica não provada; MUT1 é controle compensatório atual, com 10/10 mutações detectadas. T014 e T019 conservam `[ ]` pela limitação histórica aceita, embora os controles atuais tenham passado. CAS, ACK e eleição foram provados no SDK Java, onde vivem os algoritmos.
4. **T032:** homologação manual do usuário = **NOT_RUN** até a execução dele, incluindo Google OAuth real e entrega de reset. Não é inferida do E2E automatizado.
5. **Publicação e consumidores SDK:** provados em T013/T016 e na release Java 3.4.1, não no gate do sample. Não se exige nem se alega nova publicação por T031.

### Verificação documental T031

T031 exige “termos operacionais zero”, mas a leitura de `spec.md` não fornece uma lista ou definição dos termos a verificar. Os nomes citados como referências na spec não foram convertidos em uma lista proibida pelo executor.

Busca de leitura executada por DOC1 na raiz do repositório:

```powershell
rg -n -i -- 'termos operacionais|termo operacional' specs/003-firebase-multi-tenant/spec.md
```

Resultado observado: **exit 1, nenhuma correspondência, saída vazia**. Isso comprova somente que essas expressões não aparecem na spec; não comprova ausência de termos no código. A busca de termos operacionais no repositório = **NOT_RUN**, causa: a spec não define quais termos compõem esse critério. T031 conserva `[ ]` apesar dos gates locais PASS recebidos.

**CI remoto neste HEAD: NOT_RUN** até o push e execução observada; o checker local e a configuração MIG3B não equivalem à execução remota. **Homologação manual T032: NOT_RUN**. Qualquer execução somente planejada permanece NOT_RUN.

DOC1 é exclusivamente documental: nenhum build, rede, gate funcional, publicação ou commit executado. A única busca `rg` foi a leitura da spec registrada acima. Escrita restrita a `tasks.md`, `evidence.md` e `.gitignore`, com inclusão de `/frontend/e2e/.artifacts/`; nenhum segredo, caminho de credencial ou endereço de correio foi acrescentado.

<a id="doc1-observadas"></a>

## Retificação das evidências observadas DOC1 — 2026-10-12

Contrato **DOC1, tentativa 1**, executor **codex-sol-17**, sandbox workspace-write. Fonte: evidências OBSERVADAS pelo coordenador no ledger Astra, transcritas no pedido DOC1. Estes resultados foram recebidos, não reexecutados pelo executor documental. Commits locais do sample na branch `feature/003-firebase-multi-tenant`, ainda sem push; HEAD de referência dos gates: `98db709`. Preservam-se todos os registros anteriores, inclusive falhas, estados superados e trechos com caracteres corrompidos; esta retificação registra os fatos em UTF-8, sem reconstruir evidências históricas.

| Prova / tarefa | Resultado observado pelo coordenador | Limite |
| :--- | :--- | :--- |
| FE1D | `9135b24`: G18, base da API e `parseApiBase` recusando base relativa; review independente PASS | Não demonstra todas as lacunas R005 |
| FE2A2 / T025 | `c61b091`: G06, G09 e G11 em DemoRegistro; review independente PASS | Fechamento frontend nomeado para o demo |
| FE2B3 | `a2d7eff`: G12 e G14, login, retorno seguro e rotas canônicas; 419 testes e 8/8 mutações detectadas; review independente PASS | G13/G15/G17 fora do escopo aceito |
| MIG2 / T029 | `da59455`: E2E com Playwright, PostgreSQL descartável e Firebase real de testes; revogação real com tratamento do desvio de relógio e recuperação da outbox | Suíte automatizada real comprovada; homologação humana separada |
| MIG3B / T026 | `8154aa3`: CI com E2E real; secret materializado somente no runtime; PR de fork pula com aviso | Implementação do CI não equivale a execução remota neste HEAD |
| BE1 / T018, T025 | `54a9712` e checkpoint: T018-G1/G2, T025-G1/G2 e GEMS-S1/S2 | Fonte recebida: ledger Astra |
| SDK Java 3.4.1 | Cinco defeitos reais corrigidos: primeiro administrador duplicado, bootstrap não idempotente, READY antes do unlock, sessão com lock herdado e log com mensagem da exceção. PR #47 com merge, tag `v3.4.1`, workflow Release verde e pacote publicado no GitHub Packages; clean verify com 18 módulos SUCCESS; mutações detectadas no SDK | Release SDK, não gate do sample |
| BE2E / T017, T019 | `bfb8a50`: adoção SDK 3.4.1; T017-G1 com versão pelo JAR carregado e metadados Maven, consumidor com repositório Maven local isolado baixando do registro. T019-G1/G2/G3: Administration 41, Identity 13, Lifecycle 4, Adoption 1+7. Mutação `gems.version=3.4.0` detectada | Cronologia B08/F05 não provada |
| BE3C / T020, T021 | `c8e8d53` e `80c88f4`: T020-G1 e T020-G2, incluindo falha de unlock contra SDK 3.4.1; T021-G1 com nomes explícitos de constraints e validação de tipo, colunas e referência no forward e rollback; 3 mutações detectadas | Fonte recebida: ledger Astra |
| MUT1 / D001-B | 10/10 mutações detectadas com controles verdes. T014, Angular-GEMS-SDK HEAD `49b5eb9`: export renomeado, versão divergente, keycloak no core e every→some. B08: rota do first-administrator, bootstrap sem ações, grant MEMBER→PROFILE e validação de e-mail. F05: PASSWORD pela porta Google e modo identity-worker | CAS, ACK e eleição vivem no SDK Java e foram provados por mutação no próprio SDK. Cronologia histórica “teste antes” NÃO provada e não forjada |
| T028 | `6c49201`: initializer com Spotless pós-rename e clean-demo com Prettier e transformações corrigidas; regressão G16 cobre referências sobreviventes, fundamentos enfraquecidos, helpers e ações órfãos, contagens fixas e retornos rejeitados. Harness correction8 em duas cópias descartáveis (default `br.com.gems`, custom `com.acme`): init, replay idempotente, clean-demo, backend clean install com smoke E2E real, frontend test/build/prettier e invariantes, todos PASS; unittest 26+24 | Prova observada nas cópias descartáveis |
| MEM1 | `b58169b`: `spring.test.context.cache.maxSize=2`; Surefire do app-deploy com `-Xmx1536m`, SerialGC e um fork | Não limita a JVM extra do E2E |
| T030 | `7080bc7` e `98db709`, somente N=100: provisionamento p50 1,43 s, p95 1,56 s, máximo 1,69 s por tenant, total 143,5 s; inventário 10,4 ms; heap em torno de 60 MB; 12.203 statements SQL; PostgreSQL 16.15; SDK 3.4.1 | N=1000 NOT_RUN; sem SLA |
| T031 | `98db709`: backend `mvnw.cmd clean install`, 290 testes, 0 falhas e E2E T029 SUITE PASS no Firebase real; frontend `npm ci`, test, build e `prettier --check`; `gems-ai-gate` strict mais conformance; `verificar-contexto-agentes`; checker do CI. Todos com exit 0; review independente PASS. Commits de T031: `7080bc7` e `98db709` | Gates locais observados; não comprovam CI remoto nem o critério de termos operacionais |
| T013 / T016 | Publicação e consumidores isolados dos SDKs comprovados nas tarefas próprias e na release Java 3.4.1; evidências históricas Angular de publicação core/adapter e consumidores preservadas acima | Não inferidos do gate do sample; não reexecutados em DOC1 |

<a id="doc1-d001"></a>

### Decisão D001 — transcrição da resposta humana

- **Data:** 2026-10-10.
- **Fonte:** resposta real do usuário no **AskUserQuestion** de 2026-10-10, fornecida pelo coordenador no contrato DOC1 (ledger Astra).
- **Transcrição literal:** **"B — mutação atual"**.
- **Efeito:** mutação atual como controle compensatório; a cronologia histórica de T014, B08 e F05 segue **NÃO provada**, sem alegação de teste antes retroativo.
- **Assinatura pendente:** a seção “Decisão” de [proposta-regularizacao-tdd-historico.md](proposta-regularizacao-tdd-historico.md#decisão) é exclusiva do humano e não foi preenchida, assinada nem editada por este executor. A transcrição da resposta não é assinatura por proxy nem criação de waiver.

<a id="doc1-limitacoes"></a>

### Limitações aceitas pelo usuário (2026-10-10) — registro vigente

1. **R005 — G13, G15 e G17:** ficam fora do escopo enxuto. FE1D fecha G18; FE2A2 fecha G06/G09/G11; FE2B3 fecha G12/G14. O recorte fornecido não demonstra a correspondência integral das demais lacunas R005 por tarefa; T022/T023/T024/T029/T033/T034 permanecem `[ ]`, sem inferir fechamento por suíte verde. T025 tem provas nomeadas de backend e frontend.
2. **T030:** somente 100 schemas executados. **1000 = NOT_RUN**. Medidas registradas sem SLA; tarefa permanece `[ ]`.
3. **D001-B — T014, B08 e F05:** cronologia histórica **NÃO provada**. MUT1 é o controle compensatório atual, 10/10 mutações detectadas com controles verdes. T014/T019 permanecem `[ ]` pela limitação aceita. CAS, ACK e eleição foram provados no SDK Java, onde vivem os algoritmos.
4. **T032:** homologação manual do usuário **NOT_RUN** até a execução dele. Inclui Google OAuth real e entrega de reset; não é inferida do E2E automatizado. Tarefa permanece `[ ]`.
5. **Publicação e consumidores SDK:** provas pertencem a **T013/T016 e à release Java 3.4.1**, não ao gate do sample. Não se alega nova publicação ou execução de consumidores por T031.

<a id="doc1-t031"></a>

### Verificação documental T031 — termos e execuções pendentes

T031 exige “termos operacionais zero”. A leitura integral de `spec.md` não fornece lista ou definição dos termos a buscar; referências de domínio e projetos não foram transformadas em lista proibida pelo executor. A única busca `rg` de leitura deste contrato foi executada na raiz do repositório:

```powershell
rg -n -i -- 'termos operacionais|termo operacional' specs/003-firebase-multi-tenant/spec.md
```

**Resultado observado nesta execução DOC1:** exit **1**, nenhuma correspondência, saída vazia. O resultado atesta somente a ausência dessas expressões na spec. A verificação da ausência de termos operacionais no código do repositório é **NOT_RUN**, causa: a spec não define quais termos compõem o critério. T031 permanece `[ ]`; não foi atribuído aceite humano para dispensar esse requisito. Os gates locais recebidos continuam PASS, sem extrapolar seu alcance.

**CI remoto no HEAD `98db709`: NOT_RUN**, até push e execução observada. MIG3B e o checker local não são execução remota. **T032: NOT_RUN** até homologação manual pelo usuário. **T030 N=1000: NOT_RUN**. Toda execução planejada e não observada permanece NOT_RUN.

DOC1 executou apenas leitura e edição documental, sem builds, rede ou commit. O gate GEMS não foi reexecutado neste contrato, que proíbe builds e restringe a verificação a uma busca de leitura; seu PASS acima é a evidência observada pelo coordenador em T031. A regra `/frontend/e2e/.artifacts/` já existia em `.gitignore` e foi preservada com comentário documental. Nenhum segredo, caminho de credencial ou endereço de correio foi acrescentado. Estado da lista principal: **23 tarefas `[x]`, 11 tarefas `[ ]`**; não equivale a fechamento integral da feature.
