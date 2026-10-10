# Governança Firebase multi-tenant

A constituição 3.0.0 é autoridade. A spec aprovada define comportamento; código/testes
mostram o estado atual. Regras GEMS prevalecem sob constituição, segurança e APIs publicadas.
Adoção de SDK exige publicação, alcance medido, garantia nomeada, equivalência/mutação no
consumidor e remoção da classe duplicada. Agente não cria waiver/exceção para passar gate.
Leia o AGENTS mais próximo e a feature ativa .specify/feature.json; somente contexto necessário.
Plano Firebase multi-tenant aprovado: "Implement the plan." após PLAN_REV3_20261007 PASS;
registrar autorização existente sem assinatura humana fabricada ou confirmação repetida.
GEMS AI 3.0.0 pin 51b94e3a5479da74e06444f4133e07db85ece3cc; snapshot/blocos só CLI oficial.
Builds, publicação, consumers, conformance do código, E2E e homologação permanecem NOT_RUN.
Preserve alterações existentes. Não leia segredos nem gerados node_modules/target/dist/.angular
ou imagens business. Nunca docker compose down -v, commit ou push delegado.
PostgreSQL/Keycloak Docker: procure enviroments primeiro (aqui environments); identifique
containers/portas/bancos/URLs/realms nos arquivos antes de MCP, sem credenciais em config.toml.

Backend Java21/SpringBoot4.1, cinco módulos; config/Liquibase só app-deploy. Firebase Google/e-mail+senha e SQL security sem claims de roles/cache. UID/issuer/provider e ownership imutáveis; grants limitados ao ator e catálogo. Authentication AuthorizationContextAware, scanner e interceptor obrigatórios; isolamento TenantScope/READY, outbox durável/CAS e limpeza begin/finally. Siga backend/AGENTS.md e specs/003-firebase-multi-tenant/contracts/backend.md.

Angular22/TypeScript6/Vitest/Prettier, standalone/signals e core GEMS publicado. Fachada Firebase e guard locais com contratos/testes; não inventar exports SDK. Bearer só API/path autorizado, geração/UID/alias/sessão antes do await e rechecados antes do envio. Troca/logout cancela pendências e limpa stores/forms/UI; falha sem ações tenant, respostas antigas descartadas. Siga frontend/AGENTS.md e specs/003-firebase-multi-tenant/contracts/frontend.md.
