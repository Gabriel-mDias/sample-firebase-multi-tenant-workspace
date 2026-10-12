# Configuração da homologação Firebase real

Este guia prepara o projeto **`sample-firebase-multi-tenant`** para a homologação T032. A conta de serviço **`firebase-backend` já foi entregue pelo usuário** em [`autenticação/dados-gerais.md`](../../../autenticação/dados-gerais.md), na raiz do workspace. O JSON apontado nesse arquivo existe fora do checkout e seus metadados foram validados. Use essa conta existente; não é necessário criar outra conta, gerar outra chave nem reenviar os dados já fornecidos.

Em **2026-10-09**, uma consulta Admin real `getUser` com UID sintético único inexistente, projeto explícito e ADC do JSON existente retornou **`USER_NOT_FOUND`**. Isso confirma autenticação e autorização para essa consulta (`firebaseauth.users.get`). Criação/alteração de usuários, Google/senha, operador, testers, banco e homologação T032 continuam **NOT_RUN**. O isolamento do projeto e a configuração do app Web/providers/domínios precisam de confirmação no console. Configurar o ambiente não equivale a homologar o produto.

As instruções foram conferidas contra o código atual e documentação oficial consultada em **2026-10-09**, preservando as entregas documentais de 2026-10-08 e a revisão 2. Não exigem publicar o site. Hosting, deploy cloud e alteração de CI ficam fora desta etapa.

### Comece aqui: conta de serviço do backend

Você pode usar o JSON existente, sem instalar `gcloud` e sem login da CLI. Em `autenticação/dados-gerais.md`, confira localmente o caminho atual do arquivo externo; o guia não copia esse caminho nem o conteúdo privado. No mesmo PowerShell que iniciará o backend, informe apenas esse caminho:

```powershell
$env:GOOGLE_APPLICATION_CREDENTIALS = Read-Host 'Caminho externo do JSON já indicado em autenticação/dados-gerais.md'
if (-not (Test-Path -LiteralPath $env:GOOGLE_APPLICATION_CREDENTIALS -PathType Leaf)) {
  throw 'Arquivo externo não encontrado; confira localmente o caminho informado.'
}
```

Continue pelas seções 2, 3, 5 e 6 para app Web, métodos, banco separado e bootstrap. A chave sozinha não configura esses itens nem concede membership SQL. `GOOGLE_APPLICATION_CREDENTIALS` vale somente para este processo e seus filhos Java; para backend em Docker, veja o mount exclusivo e somente leitura da seção 4. Nunca forneça ADC ao frontend. [ADC no Admin SDK](https://firebase.google.com/docs/admin/setup).

Projeto, conta e referência local da credencial já foram entregues. Ao terminar as etapas restantes, informe somente o estado e as confirmações ainda pendentes, **sem anexar o JSON**:

```text
Projeto/conta existentes: sample-firebase-multi-tenant / firebase-backend
Consulta Admin sintética: USER_NOT_FOUND observado (somente users.get)
Isolamento do projeto / app Web / providers / domínios: confirmado / pendente
Papéis para criação/alteração: conferidos / pendentes (nome do papel, sem credencial)
Backend: host Windows / Docker
Banco separado / bootstrap / Alpha e Beta / testers: estado e impedimentos sanitizados
Homologação T032: NOT_RUN
```

## 1. O que você precisa

- Uma conta Google para administrar o projeto isolado de homologação e uma conta Google designada como operador central do sample. Podem ser a mesma pessoa, mas são duas responsabilidades diferentes.
- Acesso ao console Firebase/Google Cloud suficiente para registrar o app Web, configurar Authentication e solicitar os papéis IAM necessários. Se o projeto pertencer a uma organização, peça essas permissões ao administrador.
- Dois testers com caixas de e-mail que realmente recebam mensagens: **A**, autorizado somente em Alpha; **B**, autorizado em Alpha e Beta. Use contas exclusivas de teste, distintas do operador.
- PostgreSQL 16 com **banco e credencial exclusivos da homologação**, sem dados de produção ou do demo. Um responsável deve criar o banco vazio e permitir as migrações da aplicação.
- Windows/PowerShell, JDK 21, Node 24.16.0, Docker Desktop para a opção de frontend em container e registries privados Maven/npm já configurados externamente. A Google Cloud CLI é necessária apenas nas alternativas ADC de impersonação/usuário; a rota JSON pelo console não exige `gcloud`. Para as alternativas, siga a [instalação oficial](https://cloud.google.com/sdk/docs/install). Este documento não instala nem executa login.

**ADC** (Application Default Credentials) é o mecanismo pelo qual o Java encontra a identidade autorizada do backend. **UID** é o identificador estável de uma conta em Firebase Authentication; não é seu e-mail nem o UUID do usuário SQL. **Membership** é o vínculo vigente entre um usuário SQL e uma empresa. Autenticar no Google comprova identidade; entrar em Alpha/Beta exige também membership e ações no banco.

O modelo usa um projeto Firebase por produto/ambiente. Alpha e Beta são empresas SQL, não tenants do Google Identity Platform. Não é necessário ativar Firestore, Storage, Functions, Analytics, MFA ou Identity Platform para este roteiro.

## 2. Confirmar o projeto e registrar o app Web

1. Abra o console Firebase e selecione o projeto. Em **Configurações do projeto → Geral**, confira o **ID do projeto**, não apenas o nome exibido: `sample-firebase-multi-tenant`.
2. Confirme que esse projeto é dedicado à homologação e não contém usuários de outro produto/produção. Se já tiver uso compartilhado, resolva o isolamento antes de continuar.
3. Se o projeto ainda não existir, o ID pode já estar ocupado globalmente: crie um projeto isolado pelo console, registre o ID realmente atribuído e use esse mesmo ID no frontend, backend e bootstrap. Não suponha que a criação foi feita por este guia.
4. Em **Geral → Seus apps**, registre um app **Web (`</>`)**, com nome como `sample-homolog-web`. Não selecione configuração de Hosting.
5. Na configuração do SDK desse app, anote os quatro campos públicos usados pelo sample: `apiKey`, `authDomain`, `projectId`, `appId`. Copie os valores do console, sem inventar `appId` ou usar os valores `demo-*`.

Fluxo oficial: [registrar projeto e app Web](https://firebase.google.com/docs/web/setup). A SDK já pertence à aplicação; não execute `npm install firebase` para alterar os pins durante esta configuração.

A configuração Web identifica o app e é entregue ao navegador. Uma API key Firebase restrita aos serviços Firebase não é uma credencial de Admin. Confira suas restrições em **Google Cloud → APIs e serviços → Credenciais**, preservando as APIs necessárias à autenticação; não reutilize uma chave de APIs pagas ou de IA no browser. Consulte [chaves de API Firebase](https://firebase.google.com/docs/projects/api-keys). ADC, refresh tokens, chaves privadas, senhas e ID tokens são credenciais sensíveis e nunca entram em `env.js`, Git, imagem, `config.toml` ou respostas ao agente.

## 3. Habilitar os métodos de acesso

1. Em **Authentication → Método de login/Sign-in method**, habilite **Google**. Escolha um e-mail de suporte válido e salve. O frontend atual usa popup e solicita seleção de conta: [Google no Web](https://firebase.google.com/docs/auth/web/google-signin).
2. Habilite **E-mail/senha (Email/Password)**. Não habilite login por link de e-mail para cumprir este roteiro: [autenticação por senha](https://firebase.google.com/docs/auth/web/password-auth).
3. Em **Authentication → Configurações → Domínios autorizados**, acrescente `localhost` para esta homologação local. O campo recebe hostname, sem protocolo, caminho ou porta. Abra o sample sempre por `http://localhost:4280` na opção abaixo. Projetos criados depois de 28/04/2025 não incluem `localhost` automaticamente; essa autorização pertence ao projeto isolado, conforme [FAQ de Authentication](https://firebase.google.com/docs/auth/faq-and-troubleshooting).
4. Em **Authentication → Modelos/Templates**, confira o remetente e o modelo de redefinição de senha. Use a página de ação hospedada padrão do Firebase; não precisa criar um handler no sample nem configurar SMTP/Gmail API. O cliente chama `sendPasswordResetEmail`: [gerenciamento de usuários Web](https://firebase.google.com/docs/auth/web/manage-users).
5. Confira proteção contra enumeração de e-mails em **Authentication → Configurações → Ações do usuário**. Projetos novos normalmente já a habilitam. A UI continua usando confirmação genérica no reset e não deve revelar existência de conta: [proteção contra enumeração](https://firebase.google.com/docs/auth/web/password-auth).

**Plano e custos.** Spark é uma opção sem cobrança inicial para esses métodos, sujeita a quotas e mecanismos antiabuso. A tabela consultada limita e-mails de reset a **150/dia** no Spark e de verificação de endereço a **1.000/dia**; isso não promete entrega nem capacidade. Se houver upgrade para Authentication with Identity Platform, Spark passa a ter limite de **3.000 usuários ativos/dia** para a maioria dos provedores. Confira o estado do projeto e as páginas de [planos](https://firebase.google.com/docs/projects/billing/firebase-pricing-plans), [limites](https://firebase.google.com/docs/auth/limits) e [Identity Platform](https://firebase.google.com/docs/auth). Vincular uma conta de faturamento pode mudar o plano para Blaze; não faça esse vínculo para cumprir este guia.

## 4. Dar acesso administrativo ao backend por ADC

O código Java real usa `GoogleCredentials.getApplicationDefault()` e define o projeto explicitamente a partir de `FIREBASE_PROJECT_ID`. A conta autenticada no browser não substitui ADC. O backend precisa consultar usuários/revogação e criar, suspender, reativar e revogar contas durante o ciclo de vida.

Use a conta existente **`firebase-backend`**; confirme com o responsável que seu uso é exclusivo deste ambiente. Para as operações do código, um papel personalizado pode restringir-se às permissões `firebaseauth.users.get`, `firebaseauth.users.create` e `firebaseauth.users.update`, sujeito à conferência pelo administrador e aos testes. A consulta executada comprovou somente `users.get`; não identifica o papel atribuído nem comprova CREATE/update/disable/enable/revoke. O papel pronto `roles/firebaseauth.admin` funciona como alternativa limitada ao produto Authentication, mas é mais amplo: permite operações adicionais como exclusão e configuração. Não conceda Owner/Editor como atalho. Veja [papéis de Authentication](https://firebase.google.com/docs/projects/iam/roles-predefined-product).

### Referência condicional: criar conta e chave JSON se a existente não puder ser usada

Esta sequência é apenas referência para um ambiente novo ou substituição aprovada pelo responsável; não é pendência da conta já entregue. No Google Cloud, selecione o projeto correto → **IAM e administrador → Contas de serviço → Criar conta de serviço**; informe um nome/ID exclusivo (por exemplo `sample-backend-homolog`) e confira o e-mail gerado. O operador que cria a conta precisa de `iam.serviceAccounts.create` (por exemplo, **Create Service Accounts**, `roles/iam.serviceAccountCreator`, no projeto); conceder-lhe acesso ao projeto exige também administração do IAM do projeto, como `roles/resourcemanager.projectIamAdmin`. São permissões administrativas do operador, não papéis a entregar ao backend. Se você já tem acesso suficiente, use a mesma conta humana. [Permissões de criação](https://docs.cloud.google.com/iam/docs/service-accounts-create?hl=en).

Se o console solicitar habilitar a IAM API, o responsável precisa de `serviceusage.services.enable`; isso não exige conceder esse acesso ao backend. [Pré-requisito oficial](https://docs.cloud.google.com/iam/docs/service-accounts-create?hl=en).

Na etapa **Conceder acesso a esta conta de serviço ao projeto**, selecione **Firebase Authentication Admin** e conclua. Se a conta já foi criada sem papel, em **IAM → Conceder acesso**, informe o e-mail dela como principal e atribua o papel no projeto correto. O papel pronto permite também exclusão/configuração e acesso a configurações sensíveis de Authentication; não é o mínimo exato. Um administrador pode preparar um papel personalizado com `firebaseauth.users.get` para consultas/verificação de revogação, `firebaseauth.users.create` para CREATE e `firebaseauth.users.update` para disable/enable/revogação. Esse mapeamento das operações observadas precisa ser validado no runtime; não é prova de suficiência executada. [Permissões oficiais](https://cloud.google.com/iam/docs/roles-permissions/firebaseauth).

Para gerar uma chave nova, abra a conta → **Chaves/Keys → Adicionar chave → Criar nova chave → JSON → Criar**. O operador precisa de `iam.serviceAccountKeys.create`, disponível em **Service Account Key Admin (`roles/iam.serviceAccountKeyAdmin`)**, preferencialmente apenas nessa conta. Esse papel não é necessário ao Java. Se a criação estiver bloqueada pela política da organização, registre o impedimento e use a alternativa sem chave; não altere a política como atalho. [Permissões e restrições de chaves](https://cloud.google.com/iam/docs/keys-create-delete).

O JSON baixado é uma credencial privada de backend. Use uma pasta externa controlada, com acesso somente ao usuário que executa Java e aos administradores responsáveis; confira as permissões NTFS antes de usá-la. Mova/renomeie pelo Explorer para `C:/Users/gabri/.credentials/firebase/backend-homolog.json` e elimine cópias desnecessárias do download após conferir a guarda. A pasta fora do checkout evita versionamento acidental, mas não substitui controle de acesso. Não cole conteúdo, chave privada ou arquivo em conversa, Git, `config.toml`, imagem Docker, frontend ou CI. Trate rotação/exclusão da chave pelo responsável ao encerrar seu uso. [Guarda de chaves](https://cloud.google.com/iam/docs/best-practices-for-managing-service-account-keys).

No host, `GOOGLE_APPLICATION_CREDENTIALS` vale para o processo PowerShell e seus filhos, incluindo Maven/Java; repita-a nos terminais dos jobs. Não use `setx` para persistir esta receita. O Java atual encontra o arquivo por ADC, sem mudar código. A variável prevalece sobre ADC de login local; antes de trocar para impersonação, remova-a com `Remove-Item Env:GOOGLE_APPLICATION_CREDENTIALS -ErrorAction SilentlyContinue`. [Ordem de busca ADC](https://cloud.google.com/docs/authentication/application-default-credentials).

### Se o backend for executado em Docker

A receita entregue na seção 5 inicia o backend **no host**. Não existe Compose de homologação real entregue: não adicione esta chave ao Compose demo ou ao frontend. Um runtime dedicado do backend deve montar o arquivo externo somente para leitura e fornecer o caminho **interno** ao Java. O exemplo abaixo é apenas o fragmento de configuração a integrar à futura receita revisada, **NOT_RUN**, e não um arquivo Compose pronto:

```yaml
services:
  backend-homolog:
    environment:
      GOOGLE_APPLICATION_CREDENTIALS: /run/secrets/firebase/backend-homolog.json
      SPRING_PROFILES_ACTIVE: homolog
      FIREBASE_PROJECT_ID: sample-firebase-multi-tenant
      FIREBASE_EMULATOR_ENABLED: 'false'
      GEMS_FIREBASE_AUTH_CHECK_REVOKED: 'true'
    volumes:
      - type: bind
        source: ${HOMOLOG_FIREBASE_JSON_PATH}
        target: /run/secrets/firebase/backend-homolog.json
        read_only: true
        bind:
          create_host_path: false
```

No PowerShell que executará o futuro Compose, defina `$env:HOMOLOG_FIREBASE_JSON_PATH` com o mesmo caminho externo conferido em `autenticação/dados-gerais.md`; essa variável é apenas o caminho host para interpolação do mount. O arquivo precisa existir e ser legível pelo usuário do processo no container. Não copie o JSON com `COPY`/`ADD` nem envie-o como build context; o backend recebe a credencial só em runtime. O trecho ainda exige imagem/entrypoint revisados, banco/credencial separados, `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` externos, rede e portas próprios. Dentro do container, `localhost` aponta ao próprio container: use a topologia real do banco, sem reutilizar `gems_demo`. Preserve emulator ausente e revogação ativa. [Bind mount somente leitura](https://docs.docker.com/engine/storage/bind-mounts/), [sintaxe Compose e `create_host_path`](https://docs.docker.com/reference/compose-file/services/#volumes).

O frontend recebe só configuração Web pública; ela não concede acesso Admin e o JSON nunca vai para o browser. O login do operador/tester e a conta de serviço do backend são identidades distintas.

### Alternativa opcional: impersonação sem chave privada

O administrador concede ao seu usuário **Service Account Token Creator (`roles/iam.serviceAccountTokenCreator`) somente na service account escolhida** e as permissões de Authentication à service account no projeto de homologação. A documentação confirma ADC com impersonação em Java: [ADC local](https://cloud.google.com/docs/authentication/set-up-adc-local-dev-environment).

Em PowerShell, você executará pessoalmente, substituindo o e-mail:

```powershell
gcloud --version
$homologProject = 'sample-firebase-multi-tenant'
$homologServiceAccount = Read-Host 'E-mail da service account exclusiva de homologação'
gcloud auth application-default login --impersonate-service-account="$homologServiceAccount" --project="$homologProject"
```

Esse login cria/substitui o arquivo ADC local. Confira a conta no fluxo de consentimento. Ele não imprime ID token do tester nem faz bootstrap SQL. A autenticação da CLI e a ADC são configurações distintas; trocar apenas `gcloud config set project` não troca a identidade da ADC. Preserve a ADC fora do checkout e não envie seu arquivo ao agente.

### Alternativa: ADC de usuário com cliente OAuth próprio

**`gcloud auth application-default login` sozinho não é suficiente para Firebase Authentication**: o cliente OAuth padrão da CLI não é aceito nesse caso. Se não puder usar impersonação, o administrador deve preparar um cliente OAuth do tipo **Desktop app**, com consentimento e acesso apropriados. Mantenha o arquivo do cliente fora do repositório e use:

```powershell
$homologOAuthClientFile = Read-Host 'Caminho externo do arquivo do cliente OAuth Desktop'
gcloud auth application-default login --client-id-file="$homologOAuthClientFile" --project="$homologProject"
```

O usuário da ADC ainda precisa de IAM para Authentication. O sample já fornece o ID na inicialização. Requisitos e limitação: [Admin SDK, credenciais locais de usuário](https://firebase.google.com/docs/admin/setup); sintaxe: [application-default login](https://cloud.google.com/sdk/gcloud/reference/auth/application-default/login).

A rota JSON existente e as alternativas acima são escolhas de ADC: use uma identidade efetiva por execução. `GOOGLE_APPLICATION_CREDENTIALS` pode selecionar uma credencial antiga de outro ambiente; revise sua presença antes de usar ADC recém-preparada por login. Não coloque ADC nos containers do frontend nem no CI.

### Consulta real observada, com alcance limitado

Em 2026-10-09, o helper documental em `.tooling/astra-sample-development-20261008/firebase-real-admin-probe/` (raiz do workspace) usou **JDK 21.0.12.1**, **Firebase Admin Java 9.11.0 publicado**, `GoogleCredentials.getApplicationDefault()` e `setProjectId('sample-firebase-multi-tenant')`. A única operação Authentication foi `getUser` sobre um UID sintético único gerado em memória; não houve listagem, leitura de conta de operador/tester ou escrita cloud. O resultado sanitizado foi **`USER_NOT_FOUND` / `LOOKUP_AUTHORIZED`, exit 0**. Consulte [consulta Admin oficial](https://firebase.google.com/docs/auth/admin/manage-users), [erros Admin](https://firebase.google.com/docs/auth/admin/errors) e [permissões IAM](https://cloud.google.com/iam/docs/roles-permissions/firebaseauth).

Essa resposta confirma o acesso remoto para lookup com a credencial fornecida; não comprova permissões `users.create`/`users.update`, verificação de token/revogação real, configuração Google/senha, app Web/domínios, isolamento, banco, UID operador ou testers. As evidências locais guardam argv/cwd/timeout e hashes do helper/configuração pública, apenas código/categoria do resultado; não guardam conteúdo JSON, chave, token, mensagem ou stack. A prova está em `.tooling/astra-sample-development-20261008/firebase-real-setup-guide-existing3-proof.json`. A consulta já foi executada uma vez nesta tentativa; ela não é uma etapa a repetir para concluir o guia.

## 5. Separar o runtime real do demo

A descoberta foi feita em `environments/` (a grafia `enviroments/` não existe aqui). O Compose atual é exclusivo do **demo**: banco `gems_demo`, projeto `demo-gems`, Auth Emulator 9099/UI 4000, API 8080 e SPA 4200. **Não transforme esse Compose em homologação apenas trocando o project ID**: ele mantém emulator, dependências e volumes de demo.

Use uma cópia de trabalho isolada do sample e um banco novo, por exemplo `sample_homolog`, criado pelo responsável por PostgreSQL. O banco pode estar em outro container dedicado/porta, como 55432, mas o host, a porta e o nome reais devem vir dessa configuração; o exemplo não cria o banco nem reutiliza volumes. As migrações do sample criam seus schemas; Hibernate apenas valida. Não copie linhas de usuários/grants do demo e não escreva memberships diretamente por SQL.

No terminal do backend, estando na raiz da cópia do sample, configure variáveis **de processo**. Não execute sob transcrição que capture credenciais. O helper abaixo lê um segredo sem eco; a aplicação ainda precisa recebê-lo em memória como texto:

```powershell
function Read-HomologSecret([string] $Prompt) {
  $secretValue = Read-Host $Prompt -AsSecureString
  $secretPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secretValue)
  try { [Runtime.InteropServices.Marshal]::PtrToStringBSTR($secretPointer) }
  finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($secretPointer) }
}

$env:SPRING_PROFILES_ACTIVE = 'homolog'
$env:FIREBASE_PROJECT_ID = 'sample-firebase-multi-tenant'
$env:FIREBASE_EMULATOR_ENABLED = 'false'
$env:GEMS_FIREBASE_AUTH_CHECK_REVOKED = 'true'
Remove-Item Env:FIREBASE_AUTH_EMULATOR_HOST -ErrorAction SilentlyContinue
Remove-Item Env:GEMS_FIREBASE_AUTH_EMULATOR_HOST -ErrorAction SilentlyContinue
$env:SERVER_PORT = '8081'
$env:DB_URL = Read-Host 'JDBC do banco exclusivo (ex.: jdbc:postgresql://localhost:55432/sample_homolog)'
$env:DB_USERNAME = Read-Host 'Usuário exclusivo do banco de homologação'
$env:DB_PASSWORD = Read-HomologSecret 'Senha desse banco'
Set-Location backend
.\mvnw.cmd clean install
.\mvnw.cmd -pl app-deploy spring-boot:run
```

`homolog` é um nome de perfil para separar o processo; **não existe um `application-homolog.yml` pronto**. Ele usa `application.yml` base e as variáveis acima. Evite os profiles `local/test/ci/demo`, cujo bloco adicional exige host do emulator. Retire também overrides externos que configurem `gems.firebase.auth.emulator.host` ou desativem `check-revoked`. O código recusa mistura entre projeto real e emulator. A porta 8081 evita colisão com a API demo em 8080.

Confirme saúde sem credenciais:

```powershell
Invoke-RestMethod 'http://localhost:8081/actuator/health'
```

Saúde positiva prova startup e migração, não prova ADC/Admin nem T032. A primeira chamada autenticada/primeiro job é que exercita Authentication real. Em terminais novos de jobs, repita a configuração de processo, incluindo banco, projeto, perfil e acesso externo à mesma ADC. Não copie o conteúdo da ADC.

### Frontend real com runtime público em container

O entrypoint existente aceita `APP_RUNTIME_PROFILE=production`, os quatro campos Firebase Web e `API_BASE_URL`. `production` aqui seleciona **modo real sem emulator**, mesmo usando um projeto de homologação. A API absoluta aceita HTTP loopback. Esta opção conecta diretamente ao backend no host; requer CORS para a origem da SPA, que deve ser validado antes dos testes. Não presuma que o proxy `/api` de demo resolve a API real em 8081.

Em outro PowerShell, na raiz da cópia isolada, gere os assets pelo build real e use apenas configuração pública:

```powershell
Set-Location frontend
npm ci
npm run build
Set-Location ..
docker build --tag gems-homolog-frontend:local ./frontend
$env:APP_RUNTIME_PROFILE = 'production'
$env:API_BASE_URL = 'http://localhost:8081/api'
$env:FIREBASE_PROJECT_ID = 'sample-firebase-multi-tenant'
$env:FIREBASE_API_KEY = Read-Host 'apiKey pública do app Web'
$env:FIREBASE_AUTH_DOMAIN = Read-Host 'authDomain público do app Web'
$env:FIREBASE_APP_ID = Read-Host 'appId público do app Web'
Remove-Item Env:AUTH_EMULATOR_URL -ErrorAction SilentlyContinue
docker run --detach --name gems-homolog-frontend --publish 127.0.0.1:4280:80 --env APP_RUNTIME_PROFILE --env API_BASE_URL --env FIREBASE_PROJECT_ID --env FIREBASE_API_KEY --env FIREBASE_AUTH_DOMAIN --env FIREBASE_APP_ID gems-homolog-frontend:local
```

Abra `http://localhost:4280/login`. O browser chama a API em `localhost:8081`; o container não recebe ADC, senha SQL ou token. O arquivo público `/env.js` deve indicar `production:true`, o projeto real e ausência de `authEmulatorUrl`. Não use `npm start` sem override: o environment de desenvolvimento tem `demo-gems` e emulator como defaults.

**Pendência operacional:** o Compose real/proxy de homologação não está entregue. Se o navegador bloquear CORS, prepare uma topologia local de mesma origem com proxy para a API real, ou configure uma origem permitida pelo mecanismo efetivamente suportado após revisão. Não invente uma variável `CORS_*`, não desative a segurança do navegador e não marque login como testado enquanto esse bloqueio existir. O snippet acima é receita de preparação **NOT_RUN**, não prova de rede aprovada.

## 6. Criar o operador central por bootstrap explícito

O primeiro login **não** cria administrador. O job atual exige simultaneamente projeto, UID Google, e-mail verificado, ID token efêmero desse UID e confirmação da conta pelo Firebase Admin.

1. Com frontend/backend reais preparados, tente entrar com Google usando a conta designada de operador. A conta poderá existir no Firebase e receber **403 local** enquanto não houver cadastro SQL; isso é esperado antes do bootstrap.
2. Em **Authentication → Usuários**, confira o UID dessa conta no projeto correto. Não use o UID de outra conta ou projeto.
3. O sample não oferece CLI de exportação de ID token. Para o operador técnico, a prova efêmera pode ser obtida **somente localmente** no DevTools → Network, no header `Authorization: Bearer ...` de uma chamada da SPA à própria `/api/auth/me`. Copie apenas o token, sem `Bearer`, para a entrada oculta abaixo. Não exporte HAR, não copie como cURL, não fotografe headers e não envie o token ao agente. Se não houver essa requisição, falta uma forma revisada de obter a prova pela SDK Web; não substitua por access token da CLI, custom token ou JWT fabricado.
4. Em terminal separado, dentro de `backend/`, com a mesma configuração real de banco/ADC/projeto, execute:

```powershell
$homologOperatorUid = Read-Host 'UID Google confirmado no console de homologação'
$env:GEMS_BOOTSTRAP_ID_TOKEN = Read-HomologSecret 'ID token Firebase efêmero do operador Google'
try {
  .\mvnw.cmd -pl app-deploy spring-boot:run "-Dspring-boot.run.arguments=--spring.main.web-application-type=none --app.jobs.mode=bootstrap --app.jobs.operator-uid=$homologOperatorUid"
  if ($LASTEXITCODE -ne 0) { throw 'Bootstrap não concluído; revise a falha sanitizada.' }
} finally {
  Remove-Item Env:GEMS_BOOTSTRAP_ID_TOKEN -ErrorAction SilentlyContinue
}
```

O job não HTTP fecha o contexto ao terminar, cria/vincula o operador e concede ações **GLOBAL** do catálogo de forma transacional/idempotente. Não concede membership ou ações TENANT implicitamente. Faça novo login/recarregue o contexto e confira `/api/auth/me`. Se falhar, pare o bootstrap e verifique UID, projeto, Google, e-mail verificado, token recente e IAM; não conceda permissões por SQL para contornar o erro.

## 7. Preparar Alpha, Beta e os dois testers

Use a administração autorizada do sample/API protegida. As APIs abaixo já existem, mas este guia não pressupõe uma tela administrativa completa nem um importador de fixtures. Chamadas precisam do ID token atual do ator autorizado; nunca persistir headers nas provas.

| Operação existente | Rota e dados essenciais |
| --- | --- |
| Cadastrar tester Google ou senha | `POST /api/admin/central/users`, `name`, `email`, `method` (`GOOGLE` ou `PASSWORD`) |
| Criar empresa | `POST /api/admin/central/tenants`, `alias` (`alpha`/`beta`), `name` |
| Primeiro administrador da empresa | `POST /api/admin/central/tenants/{id}/first-administrator`, `userId`, `actions` |
| Acrescentar vínculo | `POST /api/admin/tenants/{tenant}/memberships`, `userId` |
| Conceder ação a membro | `POST /api/admin/tenants/{tenant}/grants`, `ownerId` (ID do membership), `action` |
| Encerrar vínculo | `DELETE /api/admin/tenants/{tenant}/memberships/{id}` |
| Revogar ação | `DELETE /api/admin/tenants/{tenant}/grants/{id}` |

Memberships/grants são operações TENANT: exigem contexto da empresa, `X-Tenant-Alias` correspondente e ações efetivas do ator. O operador central precisa ser inicializado como primeiro administrador de Alpha/Beta com as ações tenant necessárias antes de operar essas rotas; o catálogo GLOBAL sozinho não dá bypass. Use o catálogo atual da aplicação, incluindo `INSERIR_MEMBERSHIP`, `CONCEDER_GRANT` e as ações de DemoRegistro que o teste exigir, sem criar strings de permissão novas.

Após criar cada tenant, anote seu UUID SQL e execute o job fora de HTTP. Obtenha a prova efêmera atual do operador por fluxo local equivalente ao bootstrap, agora já autorizado; ela deve possuir a ação global `ALTERAR_TENANT`:

```powershell
$homologTenantId = Read-Host 'UUID SQL do tenant que será provisionado'
$env:GEMS_JOB_ID_TOKEN = Read-HomologSecret 'ID token Firebase atual do operador central'
try {
  .\mvnw.cmd -pl app-deploy spring-boot:run "-Dspring-boot.run.arguments=--spring.main.web-application-type=none --app.jobs.mode=provision --app.jobs.tenant-id=$homologTenantId"
  if ($LASTEXITCODE -ne 0) { throw 'Provisionamento não concluído.' }
} finally {
  Remove-Item Env:GEMS_JOB_ID_TOKEN -ErrorAction SilentlyContinue
}
```

Repita para Alpha e Beta. Confirme **READY** e revisão aplicada mínima **1** antes de acessar negócio. A API de criação não aplica migrations empresariais por conta própria.

Para **Google**, cadastre o e-mail exato por `method:GOOGLE` antes do primeiro login do tester. O vínculo automático exige exatamente um cadastro `GOOGLE_PENDING` elegível, UID nunca vinculado, provider `google.com` e e-mail verificado. Login no Firebase sem elegibilidade SQL permanece negado; alterar e-mail não transfere identidade histórica.

Para **senha**, use `method:PASSWORD` no sample; ele planeja o UID e enfileira CREATE. **Não crie manualmente essa mesma conta no console Firebase**, pois ela poderia ganhar outro UID. Execute o worker real com o mesmo banco/projeto/ADC:

```powershell
.\mvnw.cmd -pl app-deploy spring-boot:run '-Dspring-boot.run.arguments=--spring.main.web-application-type=none --app.jobs.mode=identity-worker'
```

O worker é uma execução limitada, não um serviço contínuo; confira os estados dos comandos antes de repetir. Exit 0 não prova ACK: falhas remotas podem ficar em RETRY. Após CREATE reconhecido, peça ao tester para usar **Esqueci minha senha** em `/login`, receber o e-mail nativo e definir a senha na página Firebase. Depois valide login por senha e novo reset. O backend não recebe nem guarda senha ou action link.

Prepare A com membership apenas Alpha e B com Alpha+Beta, com ações suficientes para consultar/criar os registros do cenário. Cadastros recebem `CONSULTAR_PROPRIA_CONTA` e memberships recebem `CONSULTAR_PROPRIO_CONTEXTO_TENANT` atomicamente; ações de negócio são concessões separadas. Para cobrir Google **e** senha sem presumir account linking, use A como Google e B como senha; se quiser repetir ambos os métodos para ambos os escopos, prepare identidades adicionais exclusivas de teste. Não troque o método de uma identidade já ligada para fabricar a cobertura.

## 8. Executar e registrar a homologação manual

Preencha a matriz apenas após a execução. Use contas/UIDs rotulados como Operador/A/B nas evidências, timestamps e status HTTP, sem e-mails completos. Separe a revogação de grant SQL da revogação de sessão Firebase: são mecanismos diferentes.

| Cenário | Resultado esperado | Estado inicial |
| --- | --- | --- |
| Google A elegível e conta Google sem pending elegível | A tem contexto Alpha; a outra conta é negada localmente, sem acesso a negócio | NOT_RUN |
| Senha B existente e senha incorreta | B acessa vínculos autorizados; erro genérico para falha | NOT_RUN |
| Definição inicial/reset nativo | E-mail realmente recebido, senha definida/resetada, login posterior válido; confirmação genérica inclusive para endereço não cadastrado | NOT_RUN |
| Token ausente/inválido | API protegida retorna 401 | NOT_RUN |
| Revogação Firebase com token anterior | Depois de revogação remota confirmada, repetir requisição com token anterior resulta em 401; não trocar por token recém-autenticado | NOT_RUN |
| Disable Firebase | Conta desabilitada no projeto de teste falha com 401 na API, mesmo com token anterior | NOT_RUN |
| Suspensão pelo sample + worker | SQL bloqueia após commit (403 local enquanto Firebase ainda aceita token); após DISABLE/REVOKE remotos reconhecidos, token anterior dá 401 | NOT_RUN |
| Encerrar membership/revogar grant SQL | Próxima requisição autorizada anteriormente é negada conforme vínculo/ação; não depende de logout ou token novo | NOT_RUN |
| Spoof Beta por A | Alterar `X-Tenant-Alias` para beta não concede membership: 403 e nenhum dado de Beta | NOT_RUN |
| B troca Alpha ↔ Beta | Registros isolados, inclusive mesma PK; nenhum payload/contexto/resposta tardia de Alpha reaparece em Beta | NOT_RUN |
| Campo restrito | Quem não tem a ação concreta não lê nem altera o campo via UI/API | NOT_RUN |
| Retorno pós-login | Path interno permitido só após contexto válido; URL externa, `//host`, loop e resposta antiga não navegam | NOT_RUN |
| Loading/logout/teclado/UI | Submit duplicado impedido; logout limpa contexto; foco/labels/erros anunciados; sem overflow em 320/768/1440 px e contraste contratado | NOT_RUN |

Para a revogação Firebase isolada, a SDK Admin dispõe de `revokeRefreshTokens(uid)`; a aplicação não expõe um endpoint de revogação remota isolada. O teste precisa de uma ferramenta administrativa confiável já revisada ou do fluxo de suspensão+worker, registrando claramente qual cenário foi feito. Não invente comando Firebase CLI para revogar sessão. Disable pode ser exercitado no console para a conta de teste; restaure-a de forma controlada. Não suspenda o único operador central.

O backend mantém `check-revoked=true`, que consulta a situação remota. Reset também revoga sessões segundo o Firebase; confira o token anterior separadamente. Veja [sessões e revogação reais](https://firebase.google.com/docs/auth/admin/manage-sessions). Reactivação pelo sample depende do ACK de ENABLE da versão atual e não reabre grants/memberships encerrados; novas concessões precisam ser explícitas.

Guarde evidências manuais em local externo controlado: cenário, horário, rótulo da conta, empresa, status HTTP, resultado esperado/observado e captura sanitizada da UI. Registre somente que o e-mail chegou e que a ação funcionou; não guarde corpo do e-mail, URL de ação, `oobCode`, senha, token, chave ou HAR. Um caso bloqueado continua **NOT_RUN/BLOCKED** com a causa. T032 só pode receber PASS após execução real e revisão das evidências.

## 9. Resolver problemas comuns

| Sintoma | Conferir |
| --- | --- |
| `auth/unauthorized-domain` | Domínio `localhost` autorizado no mesmo projeto do app Web e URL realmente aberta com esse hostname |
| Popup Google bloqueado/cancelado | Permissão de popup, conta de teste selecionada e Google habilitado; cancelamento não é login concluído |
| `auth/operation-not-allowed` | Método Google ou Email/Password habilitado; configuração pública pertence ao projeto selecionado |
| `auth/invalid-api-key`, app ausente ou frontend não inicia | Quatro campos reais do app Web; `APP_RUNTIME_PROFILE=production`; ausência de `AUTH_EMULATOR_URL`; não usar `demo-*` |
| Browser mostra CORS/API inacessível | API real em 8081, origem 4280 e topologia/proxy permitidos; resolver a pendência de rede, sem desativar controles |
| ADC não encontrada | Na rota JSON, caminho externo correto, arquivo presente e legível pelo Java (path interno/mount no Docker); nas alternativas, login ADC preparado no usuário Windows que executa Java; variável antiga pode prevalecer |
| ADC existe mas Admin retorna falta de permissão | Identidade efetiva, papéis na service account/projeto correto, configuração de impersonação; não ampliar para Owner/Editor |
| Login ADC de usuário não funciona com Auth | Cliente OAuth Desktop próprio necessário; preferir impersonação suportada em Java |
| Erro de quota project/Service Usage | Administrador deve verificar quota project e permissões pertinentes, como `serviceusage.services.use`, quando exigidas; não habilitar faturamento às cegas |
| API 401 | ID token Firebase correto/recentemente autenticado, `aud`/issuer do projeto, revogação e disabled; access token gcloud não serve |
| API 403 após login Firebase | Elegibilidade SQL, vínculo permanente, vigência, ações me/context, membership, alias, READY/revisão; primeiro login não é admin |
| Worker encerra mas senha não funciona | CREATE/ACK/RETRY, IAM, UID planejado, conta remota e e-mail; executar reset nativo depois de CREATE reconhecido |
| Reset não chega | Quota, spam, endereço/conta correta e template; a mensagem genérica da UI não prova envio ou existência |
| Migração falha | Banco realmente vazio/separado, usuário com permissões dos schemas e URL correta; não limpar volumes para tentar novamente |

Ao encerrar os testes, remova tokens/senha do ambiente dos terminais (`Remove-Item Env:DB_PASSWORD`, e as variáveis efêmeras de jobs). Para parar apenas o frontend desta receita: `docker stop gems-homolog-frontend`. Preserve dados e evidências; não use `docker compose down -v`. Se não precisar mais da ADC local, avalie revogá-la com `gcloud auth application-default revoke`, considerando outros usos dessa identidade na máquina.

## 10. O que informar quando a configuração estiver pronta

Projeto, conta `firebase-backend` e referência da credencial já foram fornecidos; não é preciso reenviá-los. Informe somente as confirmações ainda pendentes e mudanças posteriores: finalidade isolada do projeto; app Web registrado; métodos Google/senha habilitados; domínios autorizados; plano Spark/Blaze e existência ou não de upgrade Identity Platform; papel IAM para criação/alteração conferido; execução host/Docker; banco separado confirmado; URLs locais; bootstrap executado ou impedimento; Alpha/Beta READY; dois testers confirmados por rótulos A/B e método; cenários executados/pendentes com evidências sanitizadas. Se uma credencial diferente for necessária, informe apenas a referência local e o estado da preparação, nunca o arquivo/conteúdo.

Não envie senha, ID token, refresh token, JSON privado, client secret, link de reset ou arquivo ADC. UID pode ser conferido localmente pelo operador; este guia não pede sua publicação em conversa. A revisão de segurança deste documento e a homologação T032 permanecem gates separados.

## Referências locais conferidas

Paths relativos à raiz do sample, exceto a última linha. As linhas identificam a implementação observada e podem mudar com alterações posteriores.

| Fonte | Fato conferido |
| --- | --- |
| `environments/local/docker-compose.yml:1` | Recursos demo, perfis explícitos, portas e campos de runtime públicos |
| `backend/app-deploy/src/main/resources/application.yml:4` | DB_URL/DB_USERNAME/DB_PASSWORD, Liquibase, configuração real e check-revoked true |
| `backend/app-deploy/src/main/java/br/com/gems/app/config/JobsConfiguration.java:32` | Jobs somente não HTTP; modos bootstrap/provision/identity-worker e variáveis de prova efêmera |
| `backend/app-core/src/main/java/br/com/gems/app/administracao/BootstrapOperator.java:16` | Google/e-mail verificado, UID/projeto/token e confirmação Admin |
| `backend/app-core/src/main/java/br/com/gems/app/administracao/CentralBootstrapWorkflow.java:18` | Bootstrap transacional/idempotente com catálogo global |
| `backend/app-core/src/main/java/br/com/gems/app/administracao/controller/CentralAdministrationController.java:32` | Rotas de usuário/tenant/primeiro administrador protegidas |
| `backend/app-core/src/main/java/br/com/gems/app/administracao/controller/TenantAdministrationController.java:32` | Memberships e grants dentro do contexto tenant |
| `backend/app-core/src/main/java/br/com/gems/app/administracao/service/AdministracaoService.java:73` | Google pending/password provision e grants mínimos atômicos |
| `frontend/docker-entrypoint.sh:33` | Campos públicos permitidos, validação de modo real e serialização segura |
| `frontend/src/environments/environment.ts:8` | Defaults de desenvolvimento apontam ao emulator |
| `frontend/src/app/core/auth/firebase-web.adapter.ts:47` | Reset nativo; Google popup e autenticação Web encapsulados |
| `../Java-GEMS-SDK/gems-firebase-auth/src/main/java/br/com/gems/firebase/auth/internal/OwnedFirebaseApp.java:16` | ADC real e projectId explícito; adapter Admin verifica revogação e implementa operações de usuário |

Os comandos de configuração/runtime deste guia são instruções futuras **NOT_RUN**. A única execução remota desta revisão foi a consulta Admin sintética descrita na seção 4, consumindo a credencial externa por ADC. Não houve configuração cloud, leitura de conta real, bootstrap, alteração de código/dependências/ambientes ou homologação T032. Histórico: guia inicial, revisão documental 2 e presente tentativa **3/3**, com **0 escaladas**; a revisão independente de segurança permanece um gate separado.
