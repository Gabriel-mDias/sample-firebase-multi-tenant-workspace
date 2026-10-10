# CLAUDE.md — entrada local Firebase multi-tenant

A constituição 3.1.0 é autoridade. A spec aprovada é contrato de comportamento; código/testes
mostram o estado real. Regras GEMS vencem código consolidado sob constituição, segurança e API
publicada. Nenhum waiver/exceção ou baseline é criado pelo agente para passar gate. Referências
anteriores servem à forma de código. Adoção SDK exige publicação, alcance medido, garantia nomeada,
equivalência/mutação no consumidor e remoção da classe duplicada na mesma alteração.

## Leitura e autorização

Leia AGENTS mais próximo e apenas o contrato/READ da tarefa. Arquitetura/governança exige
constituição inteira. Feature usa spec-driven e roteiro; rodada 003 materializa autorização já
recebida: usuário "Implement the plan." após PLAN_REV3_20261007 PASS. Não assinar por proxy nem
pedir aprovação repetida para o mesmo plano. Mudança material de escopo precisa decisão real.
Feature ativa em .specify/feature.json; não carregar specs históricas sem referência necessária.

## Estado e transição

Molde SaaS Firebase multi-tenant com assinatura GEMS AI; um projeto por produto/ambiente, referência
100–1000 empresas sem SLA. Constituição 3.1.0/documentação são alvo aprovado; código atual ainda
aguarda implementação e os gates em specs/003-firebase-multi-tenant/tasks.md.
Java 21/Spring Boot 4.1 em app-model/app-security/app-events/app-core/app-deploy; config/Liquibase só
app-deploy. Angular 22/TypeScript 6/Vitest/Prettier, standalone/signals e UI GEMS. Exemplo mínimo
DemoRegistro removível por CLI; parceiro/solicitacao anteriores são forma, não domínio do produto.

Firebase Google+e-mail/senha; SQL security vigente/catalog/scope, sem cache/claims de roles.
Tenant admin só membership/grants próprios com subset do ator∩catálogo delegável; central cria
identidade/tenants/primeiro admin. UID+issuer+provider e ownership imutáveis; Google somente pending elegível
com e-mail verificado e rowlock. Lifecycle outbox durável/CAS de versão/bloqueio local antes da chamada remota.

Gates de fechamento: Java SDK clean verify/gate oficial/publicação/consumer M2 isolado;
Angular core/adapter publicados/consumer npm isolado; sample GEMS strict+conformance efetiva de código
novo E preservado baseline zero, backend mvnw.cmd clean install, frontend npm ci/test/build/prettier check,
E2E PostgreSQL descartável+Firebase real de testes (ADC/FIREBASE_TEST_SERVICE_ACCOUNT_JSON; PR de fork pula com aviso; Emu só demo/offline)/constraints/races/outbox/thread/mesma PK. Planejado é
NOT_RUN até execução observada; installlocal não comprova publicação.

## Regras de execução

- Preserve alterações existentes; não execute comandos destrutivos amplos, down-v ou commit delegado.
- Não pesquise node_modules/target/dist/.angular/imagens de business nem leia segredos.
- PostgreSQL/Docker: procurar enviroments primeiro; aqui a pasta real é environments. Identificar
  containers/portas/bancos pelos arquivos antes de MCP; nunca copiar credenciais para config.toml.
- Backend Windows usa mvnw.cmd; gate completo uma vez no fechamento após iteração focada.
- Sem filhos neste contrato documental. Delegação futura precisa ownership/READ/EDIT/CREATE/TEST.
- Snapshot gerenciado e blocos GEMS-AI somente CLI oficial. Configuração 3.0.0 é gems-ai.json;
  snapshot oficial aplicado e comprovado em evidence.md, sem handedit de snapshot.

Instruções por camada: backend/AGENTS.md, frontend/AGENTS.md, environments/AGENTS.md e specs/AGENTS.md.
GEMS AI snapshot oficial 3.0.0 aplicado pela CLI autorizada; versão 3.0.0
pin 51b94e3a5479da74e06444f4133e07db85ece3cc. Antes de alterar código rode `scripts/gems-ai.ps1 rules bundle --project . --task <tipo>`; antes de concluir rode
`scripts/gems-ai-gate.ps1` (o Stop hook do Claude e o job CI `gems-ai` executam o gate).


<!-- GEMS-AI:BEGIN -->
GEMS AI SDK 3.0.0 is mandatory for this repository. Law, security and the local constitution govern
the consumer's domain; published SDK contracts govern API facts. Toolkit base rules govern safe
operation. Profiles, task overlays and local facts specialize them without weakening higher duties.
Material conflicts name IDs and paths and fail closed before mutation. Generated files
under `.gems-ai/` must not be edited manually.

Before changing code, run `gems-ai rules bundle --project . --task <type>` and follow every returned
rule and required gate. Run `gems-ai check --project . --strict` before completion. A waiver must be
explicitly approved, scoped, unexpired, and recorded in `gems-ai.lock.json`; it cannot hide a broken
tool, build, functional gate, or infrastructure failure.
<!-- GEMS-AI:END -->
