# sample-firebase-multi-tenant-workspace

Molde GEMS para SaaS com várias empresas, Firebase Google/e-mail/senha e autorização
SQL vigente. Java 21/Spring Boot 4.1, Angular 22 e PostgreSQL 16. Cada empresa tem schema
de negócio e auditoria isolados; identidade e catálogo de ações são globais. Um projeto
Firebase corresponde a um produto/ambiente. A referência de 100–1000 empresas não promete SLA.

O código de autenticação, tenancy, administração central e DemoRegistro está implementado.
A rodada [003](specs/003-firebase-multi-tenant/roteiro.md) registra a autorização e os
[gates](specs/003-firebase-multi-tenant/tasks.md) executados: E2E integral (T029),
cleanup/rename (T028), benchmark (T030) e homologação local estão registrados nas
[evidências da 003](specs/003-firebase-multi-tenant/evidence.md#homologacao-local-20261012).
O clean-demo e o inicializador foram provados em A10 e A11 nas
[evidências da 006](specs/006-formatter-gems-ai/evidence.md).

## Ambiente local isolado

Primeiro procure `enviroments`; neste projeto a pasta é `environments`. O Compose local
usa somente recursos novos `gems-demo`, volumes `gems-demo-postgres` e `gems-demo-auth`,
PostgreSQL 16 e Firebase CLI **15.32.1**, fixada na imagem do Auth Emulator. Nenhum recurso
preexistente é reutilizado. Não remova volumes ao parar o ambiente.

Configure os registries privados de Maven/npm externamente. No Windows, use JDK 21
apenas no processo e Node **24.16.0** (compatível com Angular 22), sem alterar os pins SDK:

```powershell
$env:JAVA_HOME = 'D:/Program Files/Java/jdk21'
$env:PATH = "$env:JAVA_HOME/bin;$env:PATH"
Set-Location backend
.\mvnw.cmd clean install
Set-Location ../frontend
npm ci
npm test -- --watch=false
npm run build
npx prettier --check .
Set-Location ..
```

As imagens de aplicação empacotam artefatos já construídos no host. Contextos Docker
fechados contêm somente o jar executável ou assets públicos e arquivos de runtime;
settings, `.npmrc`, ADC e chaves não entram nas imagens. O único install na imagem do
emulator usa pacote público no registry npm oficial.

Gere um arquivo externo exclusivo de senha do banco demo uma vez e preserve-o junto
ao volume. Não imprima o conteúdo. Não use uma credencial real ou de outro ambiente:

```powershell
$demoPasswordFile = Join-Path $env:LOCALAPPDATA 'gems-demo-db-password'
if (-not (Test-Path -LiteralPath $demoPasswordFile)) {
  [IO.File]::WriteAllText($demoPasswordFile, [guid]::NewGuid().ToString('N'))
}
$env:GEMS_DEMO_PASSWORD_FILE = $demoPasswordFile
docker compose --project-name gems-demo --file environments/local/docker-compose.yml --profile demo config --quiet
docker compose --project-name gems-demo --file environments/local/docker-compose.yml --profile demo up -d --build
docker compose --project-name gems-demo --file environments/local/docker-compose.yml --profile demo ps
```

Todos os serviços são opt-in pelos profiles `demo`, `local`, `test` ou `ci`. As portas
publicadas se vinculam exclusivamente ao loopback: SPA `http://localhost:4200`, API/health
`http://localhost:8080`, PostgreSQL `localhost:5432`, Auth Emulator `http://localhost:9099`
e UI `http://localhost:4000`. O frontend usa `/api` via nginx; somente o backend recebe
`auth-emulator:9099`. O browser sempre usa localhost, project `demo-gems` e configuração
Web pública de demonstração. Não são credenciais de Admin.

Para parar mantendo banco e export do emulator:

```powershell
docker compose --project-name gems-demo --file environments/local/docker-compose.yml --profile demo stop
```

Para desenvolvimento no host, inicie somente `sample-postgres auth-emulator`, siga
[backend/README.md](backend/README.md) para variáveis do processo e use `npm start` no
frontend. Não execute os dois servidores simultaneamente nas mesmas portas.

## Runtime e verificações

O entrypoint serializa Firebase Web (`FIREBASE_API_KEY`, `FIREBASE_AUTH_DOMAIN`,
`FIREBASE_PROJECT_ID`, `FIREBASE_APP_ID`) e `API_BASE_URL` como JSON seguro em `env.js`,
servido sem cache. Campos obrigatórios ausentes falham antes de iniciar nginx.
`APP_RUNTIME_PROFILE` é `production` por padrão. `AUTH_EMULATOR_URL` exige profile local
explícito, project `demo-*` e hostname loopback com porta válida; nesse caso o override
tipado `production=false` permite o adapter local já existente. Projeto real nunca
usa emulator. Para configuração Web real, forneça todos os campos públicos externamente
e deixe o emulator ausente. Isso não configura nem publica hosting.

```powershell
python -m unittest discover -s scripts -p test_runtime_config.py
python scripts/verify-operational-legacy.py --stage runtime --strict
python scripts/verify-operational-legacy.py --strict
```

O último comando é o gate integral; a remoção das referências antigas e as provas do
clean-demo/inicializador constam nas [evidências da 006](specs/006-formatter-gems-ai/evidence.md)
(A10 e A11). O gate runtime reporta os caminhos pendentes quando houver regressão.
GEMS strict valida o snapshot e é separado da conformance efetiva do código:
use a CLI oficial por `../.tooling/run-gems-ai.ps1`, sem editar `.gems-ai/` manualmente.

Ausência de token em `/api/auth/me` e `/api/auth/context` retorna 401. Bootstrap central
exige operador UID Google verificado e job explícito; login não concede administração.
Tenant admin concede somente ações delegáveis que possui. Lifecycle usa bloqueio SQL,
outbox durável e CAS antes de reconhecer a operação remota. DemoRegistro demonstra CRUD,
campo restrito e isolamento; cleanup e rename foram comprovados em cópia descartável
([003, T028](specs/003-firebase-multi-tenant/evidence.md)).

Homologação real exige projeto Firebase isolado, ADC externo, Google/password habilitados
e testers confirmados. A homologação local foi executada e registrada nas
[evidências da 003](specs/003-firebase-multi-tenant/evidence.md#homologacao-local-20261012). Hosting permanece fora
desta fase. Constituição [.specify/memory/constitution.md](.specify/memory/constitution.md)
3.1.0, specs aprovadas e AGENTS continuam a autoridade do projeto.
