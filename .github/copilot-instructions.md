# Instruções universais Firebase multi-tenant

A constituição 3.1.1 é autoridade. A spec aprovada define comportamento; código/testes
mostram o estado atual. Regras GEMS prevalecem sob constituição, segurança e APIs publicadas.
Adoção de SDK exige publicação, alcance medido, garantia nomeada, equivalência/mutação no
consumidor e remoção da classe duplicada. Agente não cria waiver/exceção para passar gate.
Leia o AGENTS mais próximo e a feature ativa .specify/feature.json; somente contexto necessário.
Plano Firebase multi-tenant aprovado: "Implement the plan." após PLAN_REV3_20261007 PASS;
registrar autorização existente sem assinatura humana fabricada ou confirmação repetida.
GEMS AI 3.1.0 pin 909c69aa846ea97b3aea06a08fdfed8c6657d391; snapshot/blocos só CLI oficial.
Antes de alterar código rode `scripts/gems-ai.ps1 rules bundle --project . --task <tipo>`; antes de concluir
rode `scripts/gems-ai-gate.ps1` (Stop hook do Claude e job CI `gems-ai` executam o gate).
Builds, publicação, consumers, conformance do código, E2E e homologação permanecem NOT_RUN.
Preserve alterações existentes. Não leia segredos nem gerados node_modules/target/dist/.angular
ou imagens business. Nunca docker compose down -v, commit ou push delegado.
PostgreSQL/Keycloak Docker: procure enviroments primeiro (aqui environments); identifique
containers/portas/bancos/URLs/realms nos arquivos antes de MCP, sem credenciais em config.toml.

Angular22/TypeScript6/Vitest/Prettier, standalone/signals e core GEMS publicado. Fachada Firebase e guard locais com contratos/testes; não inventar exports SDK. Bearer só API/path autorizado, geração/UID/alias/sessão antes do await e rechecados antes do envio. Troca/logout cancela pendências e limpa stores/forms/UI; falha sem ações tenant, respostas antigas descartadas. Siga frontend/AGENTS.md e specs/003-firebase-multi-tenant/contracts/frontend.md.

<!-- GEMS-AI:BEGIN -->
GEMS AI SDK 3.1.0 is mandatory for this repository. Law, security and the local constitution govern
the consumer's domain; published SDK contracts govern API facts. Toolkit base rules govern safe
operation. Profiles, task overlays and local facts specialize them without weakening higher duties.
Material conflicts name IDs and paths and fail closed before mutation. Generated files
under `.gems-ai/` must not be edited manually.

Before changing code, run `gems-ai rules bundle --project . --task <type>` and follow every returned
rule and required gate. Run `gems-ai check --project . --strict` before completion. A waiver must be
explicitly approved, scoped, unexpired, and recorded in `gems-ai.lock.json`; it cannot hide a broken
tool, build, functional gate, or infrastructure failure.

For every authored Java file, including tests, follow universal `java.formatting`. Use the pinned
canonical executor `gems-ai check-java --project . --changed --base <revision>` on whole changed
files; apply only with `gems-ai format java --project . --changed --apply`. A separate full audit is
`gems-ai check-java --project . --all`. All agents (Codex, Claude, Gemini, Cursor, Copilot) share
this authority; controller-only style and a plain IDE JDT profile do not replace the executor.
Preserve tokens, comments, literal values and logical groups. Java formatter failures fail closed.
<!-- GEMS-AI:END -->
