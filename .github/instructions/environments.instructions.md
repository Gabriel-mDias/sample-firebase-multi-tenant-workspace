---
applyTo: "environments/**/*"
---

A constituição 3.1.1 é autoridade. A spec aprovada define comportamento; código/testes
mostram o estado atual. Regras GEMS prevalecem sob constituição, segurança e APIs publicadas.
Adoção de SDK exige publicação, alcance medido, garantia nomeada, equivalência/mutação no
consumidor e remoção da classe duplicada. Agente não cria waiver/exceção para passar gate.
Leia o AGENTS mais próximo e a feature ativa .specify/feature.json; somente contexto necessário.
Plano Firebase multi-tenant aprovado: "Implement the plan." após PLAN_REV3_20261007 PASS;
registrar autorização existente sem assinatura humana fabricada ou confirmação repetida.
GEMS AI 3.1.0 pin 909c69aa846ea97b3aea06a08fdfed8c6657d391; snapshot/blocos só CLI oficial.
Builds, publicação, consumers, conformance do código, E2E e homologação permanecem NOT_RUN.
Preserve alterações existentes. Não leia segredos nem gerados node_modules/target/dist/.angular
ou imagens business. Nunca docker compose down -v, commit ou push delegado.
PostgreSQL/Keycloak Docker: procure enviroments primeiro (aqui environments); identifique
containers/portas/bancos/URLs/realms nos arquivos antes de MCP, sem credenciais em config.toml.

PostgreSQL16 e Auth Emulator demo-gems só demo/offline; suíte/CI usam Firebase real de testes via ADC/FIREBASE_TEST_SERVICE_ACCOUNT_JSON (fork pula E2E real com aviso); E2E real ainda NOT_RUN. Nunca destruir volumes nem copiar segredos. Configurações/runtime não implementados nesta tarefa. Siga environments/AGENTS.md e specs/003-firebase-multi-tenant/contracts/operations.md.
