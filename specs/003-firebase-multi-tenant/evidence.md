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

<a id="ci-remoto-20261012"></a>

## CI remoto — 2026-10-12

O PR #9 (`feature/003-firebase-multi-tenant` → `main`) entrou com o merge `c111d8e`. Execução observada: run **38295576662**, com os 4 jobs PASS.

| Job | Resultado observado |
| --- | --- |
| Backend, reator Maven com testes | PASS, 5m14s. `app-security` 1, `app-core` 87 e `app-deploy` 202 testes, todos com 0 falhas, 0 erros e 0 skips. `T029 SUITE PASS` contra o projeto Firebase real de testes, usando o secret `FIREBASE_TEST_SERVICE_ACCOUNT_JSON` |
| Frontend: testes, build e formatação | PASS. Vitest com 419 testes em 24 arquivos |
| GEMS AI, gate das rules | PASS. `check --strict` conformant e `verify-angular` passed |
| Contexto de agentes | PASS |

Duas correções de CI entraram no mesmo PR, cada uma com prova antes do push:

- **`7b5cb5b`.** O job `gems-ai` deixou de fazer o checkout cross-repo do `gems-ai-sdk` privado, que respondia "Repository not found" com o `GITHUB_TOKEN`. Agora ele usa a action compartilhada `Gabriel-mDias/gems-ai-sdk@main`, que veio do PR #15 do SDK (merge `baba0f5`), com Actions access `user` e sem token. O `scripts/gems-ai.ps1` passou a comparar a versão por igualdade exata, onde antes usava `Contains`.
- **`53adbaa`.** O `.gitattributes` passou a usar `eol=crlf` nos caminhos do `gems-ai.lock.json`. A CLI 3.0.0 grava o hash dos bytes CRLF, e o checkout limpo com `eol=lf` acusava drift em todos os arquivos gerenciados. Prova: um clone limpo sem a correção falhou com drift (exit 1); com a correção, retornou conformant/passed (exit 0), tanto com `autocrlf=false` quanto com `autocrlf=true`.

Com isso, a pendência "CI remoto NOT_RUN" da verificação T031 acima fica **satisfeita**.

<a id="homologacao-local-20261012"></a>

## Homologação em tela pelo product owner — 2026-10-12

**Ambiente.** Execução local na máquina do usuário, com o código de `main` `c111d8e` e o projeto Firebase real de testes `sample-firebase-multi-tenant`. Não foi usado emulador.

| Componente | Configuração |
| --- | --- |
| Backend | Jar do `app-deploy` no host, porta 8081. `FIREBASE_EMULATOR_ENABLED=false`, `check-revoked` ativo, ADC pela conta de serviço de testes e CORS restrito a `http://localhost:4280` |
| Frontend | Imagem do próprio `frontend/` em container, porta 4280. `APP_RUNTIME_PROFILE=production`, com a configuração Web pública lida do app registrado no projeto |
| Banco | PostgreSQL 16 dedicado (`gems-local-real-db`, porta 55432), separado do demo e criado vazio. As migrations foram aplicadas pela aplicação |

**Preparação.** Operador e testers são identificados por rótulo, sem e-mail.

1. Login Google do **Operador** sem cadastro SQL: a API recusou com `LOCAL_DENIED`, e o evento `ACCESS_REFUSED_LOCAL_DENIED` foi registrado. Isso confirma que o primeiro login não concede administração.
2. Job `bootstrap` com o UID Google do Operador e uma prova efêmera: concluído. O Operador ficou `BOUND`, com as 14 ações globais do catálogo.
3. Empresas **Alpha** e **Beta** criadas pela API central. Cada uma foi provisionada pelo job `provision` (schemas `tenant_alpha` e `tenant_beta`), e o Operador foi definido como primeiro administrador nas duas, com as 12 ações tenant. `/api/auth/context` retornou 200 para Alpha e para Beta.
4. **Tester Beta**, pré-cadastrado como Google (`GOOGLE_PENDING`) pela API central, recebeu vínculo **somente em Beta** pela API tenant. Ganhou `CONSULTAR_DEMO`, `INSERIR_DEMO`, `ALTERAR_DEMO` e `EXCLUIR_DEMO`, sem `VISUALIZAR_VALOR_RESTRITO`. Uma tentativa anterior com o token de uma conta sem cadastro foi recusada com `LOCAL_DENIED`, como esperado.

As provas efêmeras (ID tokens) ficaram apenas em um arquivo local, foram apagadas depois do uso e não aparecem em nenhum artefato.

**Percorrido e aprovado pelo usuário em tela**, com resposta literal: "Homologado as ações dos dois usuários, a visão restrita daquele que tem o tenant isolado, além da tabela de audit da base de dados, a flag sigilosa e a gestão dos usuários pelo backend! Aqui, está perfeito até o momento."

| Cenário (matriz da [seção 8 do guia](configuracao-homologacao-firebase.md)) | Resultado |
| --- | --- |
| Login Google real do Operador e do Tester Beta (vínculo `GOOGLE_PENDING` → `BOUND` no primeiro login) | PASS, observado pelo usuário |
| Isolamento: o Tester Beta vê somente Beta; registros de Alpha e Beta cadastrados pelo usuário permanecem separados | PASS, observado pelo usuário |
| Campo restrito: sem `VISUALIZAR_VALOR_RESTRITO`, o valor fica oculto para o Tester Beta e visível para o Operador | PASS, observado pelo usuário |
| Ações de DemoRegistro dos dois usuários (consultar, inserir, alterar, excluir) | PASS, observado pelo usuário |
| Auditoria por tenant (`tenant_*.audit_operation` e `audit_change`) no banco | PASS, observado pelo usuário no DBeaver |
| Gestão de usuários pelo backend: pré-cadastro, vínculo e concessões | PASS, observado pelo coordenador e conferido pelo usuário |
| Primeiro login sem cadastro SQL não concede acesso | PASS, `LOCAL_DENIED` observado |
| Senha: entrega real do e-mail de primeiro acesso e reset pelo Firebase, definição da senha e login por e-mail e senha em seguida | PASS, observado pelo usuário. Fala literal: "login e senha após o reset/primeiro acesso: OK!" |

| Disable e revogação Firebase com o token anterior do Tester Beta (login Google), via Identity Toolkit `accounts:update` com a conta de serviço de testes, chamando `GET /api/auth/me` a cada passo | PASS, observado pelo coordenador: 1) baseline 200; 2) conta desabilitada → 401 `INVALID_TOKEN`; 3) reabilitada, token não revogado → 200; 4) `validSince` atual (revogação) → 401 `INVALID_TOKEN`. A conta foi reabilitada no passo 3 |
| Spoof de `X-Tenant-Alias`: `GET /api/auth/context`, `POST /api/demo-registros/search` e `GET /api/demo/tenant/read` com token real | PASS, observado pelo coordenador. Tester Beta: `beta` → 200 nos três; `alpha` → 403 `LOCAL_DENIED` nos três; alias inexistente → 403 `LOCAL_DENIED` nos três. Operador (vinculado a Alpha e Beta): alias inexistente → 403 `LOCAL_DENIED` nos três |
| Revogar grant SQL sem logout: `DELETE /grants/{id}` de `CONSULTAR_DEMO` do Tester Beta | PASS. Mesmo token: `/auth/me` 200, contexto Beta 200 e listar DemoRegistro 403. Em tela, depois do F5, o usuário observou o menu vazio e o aviso do espaço de trabalho com a sugestão de trocar de empresa |
| Encerrar membership sem logout: `DELETE /memberships/{id}` em Beta | PASS. Mesmo token: `/auth/me` 200; contexto Beta e DemoRegistro 403 `LOCAL_DENIED`. Em tela: "Nenhuma empresa disponível" |
| Suspensão pelo sample com o worker | PASS. `POST /users/{id}/suspend` 200: bloqueio local imediato (403 `LOCAL_DENIED` nas três chamadas com o token ainda válido no Firebase), com `DISABLE` e `REVOKE` v1 em `PENDING`. Rodada do `identity-worker`, exit 0: os dois comandos `ACKED`/`APPLIED` na 1ª tentativa, e o mesmo token passou a 401 `INVALID_TOKEN`. Em tela: sessão derrubada para o login e novo login recusado |
| Reativação com o worker | PASS no ciclo remoto. `POST /users/{id}/reactivate` 200 → `ENABLE` v2 `PENDING`; rodada do worker → `ACKED`/`APPLIED`; usuário `BOUND`, versão 2, vigente; memberships 0, como manda o [data-model](data-model.md). Ver o achado 4 abaixo |
| Restauração explícita depois da reativação | PASS. Global `CONSULTAR_PROPRIA_CONTA`, novo vínculo em Beta e as 4 ações do DemoRegistro com 201. O usuário confirmou o acesso em tela: "Teste realizado e respondeu como esperado" |

**Não percorrido nesta sessão (NOT_RUN):**

- retorno pós-login com URL externa;
- responsividade 320/768/1440, teclado e contraste da tela de login.

T032 permanece `[ ]` por esses itens.

**Achados da preparação** (código vence; divergências reportadas):

1. O guia aponta uma "pendência de CORS". Ela se resolve com a propriedade que o código já suporta, `app.security.cors.allowed-origins` (`SecurityConfig.java:50`), definida por `APP_SECURITY_CORS_ALLOWED_ORIGINS`. O guia ainda não menciona essa propriedade.
2. O `nginx.conf` do frontend resolve o host `backend` no boot, mesmo quando `API_BASE_URL` é absoluta. Fora do Compose, o container só sobe com `--add-host backend:<ip>`.
3. O `README.md` da raiz ainda descreve o E2E, o cleanup e o benchmark como pendentes, o que diverge do estado atual (seção [CI remoto](#ci-remoto-20261012) e T030 acima).
4. **Reativação não devolve a ação básica de conta.** `JdbcUserService.suspend` (Java-GEMS-SDK `gems-firebase-auth`) encerra também `usuario_role_global`, incluindo `CONSULTAR_PROPRIA_CONTA`, que é concedida automaticamente no cadastro (`defaultSelfAction`). `requestReactivation` e o ACK do `ENABLE` não a devolvem, porque o [data-model](data-model.md) proíbe reabrir grants encerrados. Efeito observado: a conta fica vigente no Firebase e no SQL, mas `/auth/me` responde 403 e o login em tela não se completa (Google e senha), sem uma mensagem que explique o motivo. **Decisão do usuário (2026-10-12), literal:** "Sobre a reativação, a claim precisa automaticamente ser devolvida em uma reativação". Isso altera o contrato do data-model (somente a ação padrão de conta; memberships e demais grants continuam sem reabertura) e o SDK Java. Fica como mudança futura, com spec, release do SDK e adoção no sample; não foi implementada nesta homologação.
5. No log do `identity-worker`, cada evento `IDENTITY_COMMAND_ACKED` aparece entregue duas vezes com o mesmo id, o que é compatível com entrega "pelo menos uma vez". Observação, sem falha funcional observada.
