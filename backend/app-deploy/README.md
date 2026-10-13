# Backend Firebase

Use Java 21 e execute `.\mvnw.cmd clean install` na raiz do backend. As dependências
GEMS 3.5.0 vêm do registro Maven publicado; configure o acesso externamente.

O servidor exige `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` e `FIREBASE_PROJECT_ID`
no ambiente. Configure ADC pelo mecanismo oficial, sem credenciais na linha
de comando. Identidade vem do Firebase; concessões vigentes vêm do SQL.
O header empresarial é `X-Tenant-Alias`.

Os perfis `local`, `ci` e `demo` permitem Auth Emulator quando
`FIREBASE_EMULATOR_ENABLED=true` e `FIREBASE_AUTH_EMULATOR_HOST` estão definidos.
Use projeto demo isolado. O perfil de produção não seleciona emulador
automaticamente. OpenAPI e Swagger UI permanecem desabilitados: os handlers
Springdoc publicados não têm a classificação exigida pelo scanner obrigatório.
A variável `SPRINGDOC_ENABLED` não é suportada e é ignorada. Uma configuração
direta que habilite esses handlers faz o scanner recusar o startup.

## Bootstrap central

Execute em processo separado com `--spring.main.web-application-type=none`,
`--app.jobs.mode=bootstrap` e `--app.jobs.operator-uid=<UID verificado>`.
Forneça a prova transitória em `GEMS_BOOTSTRAP_ID_TOKEN`, protegida pelo operador.
O job verifica projeto/UID do token e confirma a conta pelo Firebase Admin.
O bootstrap inicial exige conta Google com e-mail verificado. É idempotente,
auditado e explícito; a primeira autenticação não concede administração.
Remova a prova do ambiente após execução e nunca persista o token em arquivos.

## Provisionamento e workers

A API central cria empresa PENDING. Em outro processo, execute
`--spring.main.web-application-type=none --app.jobs.mode=provision`
e `--app.jobs.tenant-id=<UUID>`. `GEMS_JOB_ID_TOKEN` contém a prova transitória
de um operador central com `ALTERAR_TENANT`. A SDK prende o lock PostgreSQL
à sessão de migrations e publica READY após consultar a revisão aplicada.
Alias é estrito e imutável. Schemas suspensos permanecem no inventário.

Agende um processo separado com `--spring.main.web-application-type=none`
e `--app.jobs.mode=identity-worker`. Cada execução processa até 1000 comandos
Firebase e 1000 eventos duráveis. Falhas conservam retries SQL; execute
periodicamente. Suspensão bloqueia SQL antes da chamada remota. O listener
demonstrativo registra IDs sanitizados. Destinos externos devem deduplicar
por UUID, pois a entrega é pelo menos uma vez.

O servidor não provisiona empresas no startup. Apenas `modelo` é migrado
antes da validação do EntityManager. OSIV está desabilitado.

## API

`src/main/resources/authorization.actions.json` é produzido pelo gerador Java
oficial a partir de `AcaoSistema`; o build rejeita divergências.
`GET /api/auth/me` exige `CONSULTAR_PROPRIA_CONTA` global e retorna
`user`, `tenants`, `globalActions`. `GET /api/auth/context` exige o header
e `CONSULTAR_PROPRIO_CONTEXTO_TENANT`, retornando `tenant`, `profiles`,
`groups`, `actions`. Identidade inválida retorna 401, negação SQL 403 e
indisponibilidade do provider 503.

O exemplo removível `/api/demo-registros` fornece GET UUID, POST `/search`,
POST criação, PUT UUID e DELETE lógico UUID. `VISUALIZAR_VALOR_RESTRITO`
controla leitura, projeção de busca e escrita de `valorRestrito` no serviço.

Os testes PostgreSQL usam Testcontainers e portas Firebase determinísticas.
Navegador com Auth Emulator e homologação Firebase real são gates posteriores.
