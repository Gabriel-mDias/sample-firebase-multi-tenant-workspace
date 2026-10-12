# Evidências da preparação documental — 2026-10-07

**Resultado desta tentativa: PARTIAL.** Os documentos do mesmo plano autorizado foram materializados.
A configuração real `gems-ai.json` ficou fora do contrato READ/EDIT recebido; o root determinou
encerrar esta tentativa e preparar sucessor corrigido. Nenhum snapshot foi editado à mão.
Modelo solicitado GPT6.1Sol High; modelo/esforço observado desconhecido. Executor governance_impl,
task GOVERNANCE_1, work_key GOVERNANCE_FIREBASE, tentativa1. Sem commits/publicações/filhos.

## Matriz de provas

| Verificação | Resultado | Evidência/limite |
| :--- | :--- | :--- |
| Clone privado oficial e pin GEMS AI3.0.0 | PASS | gh clone em .tooling/gems-ai-sdk; HEAD51b94e3a5479da74e06444f4133e07db85ece3cc; CLI --version3.0.0; checkout limpo |
| Parser/inspect e cinco bundles oficiais | PASS | 7 comandos com exit0; união por ID tem20regras; saídas literais em archive/cli-3.0.0-readonly |
| Constituição3.0.0/SIR/nove princípios | PASS documental | observations.json confere versão e nove princípios; review independente atual pendente |
| Artefatos spec/research/plan/data-model/contracts/quickstart/tasks/roteiro/checklists | PASS presença | 15 artefatos aplicáveis não vazios; análise/rastreabilidade; não prova código implementado |
| Autorização humana prévia | PASS registro | literal "Implement the plan." após PLAN_REV3_20261007 PASS; roteiro registra fonte da conversa sem assinatura por proxy |
| Planejamento não marcado como implementado | PASS | 32tarefas abertas; publicação/build/tests/homologação NOT_RUN |
| Blocos gerenciados/lock recebidos preservados | PASS | hashes atuais/baseline em observations.json; lock ainda2.0.0; nenhuma escrita .gems-ai/ |
| Fatos operacionais antigos em instruções locais manuais ativas | PASS escopo local | nove entradas locais + constituição; busca zero; não inclui varredura de código runtime |
| Nenhum código de produção/POM/package.json/compose/runtime alterado | PASS escopo | git diff só arquivos documentais; Java SDK só docs/plans novos; observations.json |
| Setup-plan/setup-tasks | PASS | scripts oficiais da .specify resolveram rodada003; plan existente preservado; template tasks e docs encontrados; nenhuma branch criada |
| sample_diff/sdk_diff | execução terminal no ledger | git diff --check autorizado; IDs/exit/hashes ficam no result de GOVERNANCE_1 |
| Config3.0.0/preview/apply/strict/verify-angular | NOT_RUN | nome correto gems-ai.json não autorizado; gems-ai.config.json não existe; sucessor deve corrigir READ/EDIT/allow_dirty_paths |
| Propagação .cursor/.github/kilo e skills gerenciadas | NOT_RUN | sincronizar fatos locais sob READ corrigido; managed skills/blocos somente CLI oficial |
| Review independente desta materialização | NOT_RUN | PLAN_REV3 é review do plano recebido; não substitui review atual dos bytes |
| SDK builds/publicações/consumers Maven/npm isolados | NOT_RUN | futura implementação; installlocal não é publicação |
| Sample conformance real baselinezero/backend/frontend/E2E/bench/homolog | NOT_RUN | tarefa atual é somente governança, sem serviços conectados |

## Comandos observados

- `gh repo clone Gabriel-mDias/gems-ai-sdk .tooling/gems-ai-sdk` e checkout detach do pin51b94e3;
  `git rev-parse HEAD`, `git status --short`, wrapper oficial `--help` e Python `-m gems_ai --version`.
- CLI oficial3.0.0 `inspect --project sample`; bundles `multi-tenant/java`, `persistence/java`,
  `security/workspace`, `angular-auth/angular`, `angular-crud/angular`, todos `--format json`.
  `commands.json` arquiva argv/cwd/exit e hashes de stdout/stderr. União por ID, sem flag inventada.
- `.specify/scripts/powershell/setup-plan.ps1 -Json` e `setup-tasks.ps1 -Json` com
  SPECIFY_FEATURE_DIRECTORY=specs/003-firebase-multi-tenant. Setup-plan preservou plan existente.
- Verificações mecânicas documentais em .tooling/observe-governance.py, observações versionadas
  em observations.json. Estas provas não validam segurança/runtime/conformance integral do código.
- Diff checks finais são executados por `state.py verify --validation sample_diff|sdk_diff`.

## Ocorrências exploratórias sem ocultação

Start GOVERNANCE/version2 foi rejeitado por proteção de arquivos existentes na raiz semGit/repos
aninhados. Root encerrou esse contrato sem tentativa e preparou GOVERNANCE_1 com allow_dirty_paths
limitado à escrita já aprovada; tentativa1 iniciou emversion6. Não houve alteração do gate pelo executor.

Get-Content do nome contratado gems-ai.config.json retornou arquivo inexistente; parser oficial
confirmou CONFIG=gems-ai.json. Uma busca exploratória incluiu src/gems_ai/toolkit.py, inexistente
no checkout oficial, e retornou exit1; os arquivos reais cli.py/project.py foram consultados em
seguida. São falhas de leitura exploratória, não gates funcionais omitidos. Nenhuma configuração,
credencial ou snapshot foi alterado para contornar essas ocorrências.

## Pendências para contrato sucessor

Autorizar READ/EDIT/allow_dirty_paths do gems-ai.json real e leituras dos adapters locais existentes.
Atualizar configuração3.0.0/pin, preview oficial por destino/ação/hash, aplicar/updateCLI oficialmente,
strict e verify-angular com limites explícitos. Propagar fatos locais nos adapters e managed skills
via CLI. Reavaliar a extensão estreita de consumidor Firebase prevista no auth3.0.0: não inventar API
Firebase publicada. Fazer revisão independente dos contratos atuais e registrar resultado.

Este registro não solicita confirmação humana repetida: o root reconheceu erro administrativo do
contrato e preservou a autorização do usuário para o mesmo plano. Gates futuros permanecem abertos.
