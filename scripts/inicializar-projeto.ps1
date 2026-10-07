<#
.SYNOPSIS
    Renomeia o molde para um projeto novo.

.DESCRIPTION
    O molde nasce com o nome neutro `app`: módulos `app-model`..`app-deploy`, pacote
    `br.com.gems.app`, classe `AppApplication`, realm `app-realm`, banco `app-db`. Este script troca
    tudo isso pelo nome do seu projeto — diretórios, pacotes, classes, artifactIds, nomes de
    contêiner, rede, volume, banco, realm, imagens e as ocorrências textuais nos arquivos de
    governança.

    Rode-o UMA VEZ, logo depois de clonar, com a árvore limpa. Ele é destrutivo por natureza: se
    houver alteração não commitada, ele se recusa a rodar, para que o `git diff` continue sendo o
    caminho de volta.

.PARAMETER Nome
    O nome do projeto, em minúsculas, sem espaço. Vira o prefixo dos módulos (`<nome>-model`), o
    último segmento do pacote e a base dos nomes de infraestrutura.

    Precisa ser um identificador Java válido em minúsculas: letras e dígitos, começando por letra.
    Hífen NÃO serve — ele quebraria o nome do pacote.

.PARAMETER Grupo
    O groupId Maven e o prefixo do pacote. Padrão: `br.com.gems`.

.PARAMETER Descricao
    Uma linha para o README e para o `<description>` do POM agregador.

.PARAMETER DryRun
    Mostra o que seria alterado, sem alterar nada.

.EXAMPLE
    .\scripts\inicializar-projeto.ps1 -Nome vendas -Descricao "Gestão de vendas e pós-venda"

.EXAMPLE
    .\scripts\inicializar-projeto.ps1 -Nome vendas -Grupo com.acme -DryRun
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[a-z][a-z0-9]*$')]
    [string]$Nome,

    [string]$Grupo = 'br.com.gems',

    [string]$Descricao = '',

    [switch]$DryRun
)

$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $PSScriptRoot

# ---------------------------------------------------------------------------------------------
# Guardas. Nenhuma delas é excesso de zelo: sem elas, um rename parcial fica indistinguível de um
# rename completo, e é preciso ler o repositório inteiro para descobrir a diferença.
# ---------------------------------------------------------------------------------------------

if ($Nome -eq 'app') {
    throw "O nome 'app' é o do próprio molde. Escolha o nome do seu projeto."
}

Push-Location $raiz
try {
    $sujo = git status --porcelain --untracked-files=all
    if ($sujo -and -not $DryRun) {
        Write-Host 'Árvore de trabalho suja. Commite ou descarte antes de renomear:' -ForegroundColor Red
        $sujo | ForEach-Object { Write-Host "  $_" }
        throw 'Abortado. O `git diff` precisa ser o caminho de volta.'
    }
} finally {
    Pop-Location
}

$Pascal = $Nome.Substring(0, 1).ToUpper() + $Nome.Substring(1)

Write-Host ''
Write-Host "Renomeando o molde para '$Nome'" -ForegroundColor Cyan
Write-Host "  módulos ....... app-*            -> $Nome-*"
Write-Host "  pacote ........ br.com.gems.app  -> $Grupo.$Nome"
Write-Host "  classe main ... AppApplication   -> ${Pascal}Application"
Write-Host "  infra ......... app-db, app-realm, app-postgres  -> $Nome-*"
Write-Host "  volume/rede ... app_postgres_data, app_network   -> ${Nome}_*"
Write-Host "  env vars ...... APP_*                            -> $($Nome.ToUpper())_*"
if ($DryRun) { Write-Host '  MODO SECO: nada será alterado.' -ForegroundColor Yellow }
Write-Host ''

# ---------------------------------------------------------------------------------------------
# 1. Conteúdo dos arquivos.
#
# A ORDEM DAS SUBSTITUIÇÕES IMPORTA: as mais específicas primeiro. Trocar `app` isolado antes de
# `br.com.gems.app` produziria `br.com.gems.<nome>` corretamente por acidente, mas destruiria
# `application.yml`, `ApplicationContext` e toda palavra que contenha "app". Por isso não existe
# nenhuma regra que troque `app` solto.
# ---------------------------------------------------------------------------------------------

$grupoAntigoPacote = 'br.com.gems.app'
$grupoNovoPacote = "$Grupo.$Nome"

$substituicoes = [ordered]@{
    # pacote e groupId
    $grupoAntigoPacote                  = $grupoNovoPacote
    'br/com/gems/app'                   = ($grupoNovoPacote -replace '\.', '/')
    '<groupId>br.com.gems</groupId>'    = "<groupId>$Grupo</groupId>"
    # módulos Maven
    'app-model'                         = "$Nome-model"
    'app-security'                      = "$Nome-security"
    'app-events'                        = "$Nome-events"
    'app-core'                          = "$Nome-core"
    'app-deploy'                        = "$Nome-deploy"
    # classe principal
    'AppApplication'                    = "${Pascal}Application"
    # infraestrutura — nomes de projeto Compose, redes e volumes.
    #
    # Volume e rede NÃO são cosméticos. Eles vivem no daemon do Docker, fora de qualquer stack:
    # dois projetos derivados deste molde na MESMA máquina — produção de um e staging de outro,
    # por exemplo — compartilhariam `app_postgres_data` e escreveriam no mesmo banco. É o modo de
    # falha mais caro que um rename incompleto produz, e ele não dá erro: dá dado misturado.
    'app_containers'                    = "${Nome}_containers"
    'app_staging_postgres_data'         = "${Nome}_staging_postgres_data"
    'app_staging_network'               = "${Nome}_staging_network"
    'app_postgres_data'                 = "${Nome}_postgres_data"
    'app_caddy_config'                  = "${Nome}_caddy_config"
    'app_caddy_data'                    = "${Nome}_caddy_data"
    'app_network'                       = "${Nome}_network"
    'app-staging-network'               = "$Nome-staging-network"
    'app-network'                       = "$Nome-network"
    'app-postgres'                      = "$Nome-postgres"
    'app-keycloak'                      = "$Nome-keycloak"
    'app-caddy'                         = "$Nome-caddy"
    'app-localstack'                    = "$Nome-localstack"
    'app-backend'                       = "$Nome-backend"
    'app-frontend'                      = "$Nome-frontend"
    'app-staging'                       = "$Nome-staging"
    'app-db'                            = "$Nome-db"
    'app-access'                        = "$Nome-access"
    'app-realm'                         = "$Nome-realm"
    'app-bucket'                        = "$Nome-bucket"
    'app-test'                          = "$Nome-test"
    'app-login.css'                     = "$Nome-login.css"
    # Artefatos que vivem no HOST da VPS, fora do Compose: unidades systemd do backup, o
    # EnvironmentFile delas, a chave SSH de deploy e o diretório do clone. Nada disso quebra o
    # build se ficar para trás — quebra a operação de uma segunda máquina, meses depois.
    'app-backup'                        = "$Nome-backup"
    'app_deploy'                        = "${Nome}_deploy"
    'app-project'                       = "$Nome-project"
    # Nomes de variável psql dentro do init-postgres.sh. São internos ao script (os VALORES vêm
    # das env vars já renomeadas acima), mas deixá-los sujaria a conferência do passo 1.
    'app_user'                          = "${Nome}_user"
    'app_password'                      = "${Nome}_password"
    'app_db'                            = "${Nome}_db"
    # Variáveis de ambiente: o prefixo INTEIRO, e não uma regra por família.
    #
    # A forma antiga listava APP_DB_, APP_ADMIN_, APP_CORS_ e APP_CEP_ uma a uma, e por isso
    # APP_DOMAIN, APP_ORIGIN e APP_NAME sobreviviam ao rename — o `.env` do projeto derivado saía
    # com dois prefixos misturados. Nenhuma variável de terceiro no repositório começa com `APP_`.
    'APP_'                              = ($Nome.ToUpper() + '_')
    # `spring.application.name` traz o default `app` embutido no próprio placeholder. Sem esta
    # regra ele sobrevive — não há (nem pode haver) troca de `app` solto —, e o projeto renomeado
    # se identifica como "app" em log, métrica e trace.
    ':app}'                             = ":$Nome}"
    # namespace de configuração tipada
    'app.security.'                     = "$Nome.security."
    'app.cep.'                          = "$Nome.cep."
    'AppKeycloakProperties'             = "${Pascal}KeycloakProperties"
}

$extensoes = @('.java', '.xml', '.yml', '.yaml', '.json', '.md', '.ts', '.html', '.css', '.sh',
    '.properties', '.ftl', '.mdc', '.toml', '.conf', '.example', '.gitignore', '.dockerignore')

$arquivos = Get-ChildItem -Recurse -File $raiz -Force |
    Where-Object {
        $_.FullName -notmatch '\\\.git\\|\\node_modules\\|\\target\\|\\dist\\|\\\.angular\\' -and
        ($extensoes -contains $_.Extension -or $_.Name -in @('Dockerfile', 'Caddyfile', 'mvnw', 'mvnw.cmd', '.env.example'))
    }

$alterados = 0
foreach ($arquivo in $arquivos) {
    $conteudo = [System.IO.File]::ReadAllText($arquivo.FullName)
    $original = $conteudo

    foreach ($de in $substituicoes.Keys) {
        $conteudo = $conteudo.Replace($de, $substituicoes[$de])
    }

    # O namespace de configuração tem DUAS formas, e a substituição por ponto só alcança uma.
    # No Java e no @Value ele é `app.security.keycloak`; no YAML é uma chave de topo aninhada:
    #
    #   app:
    #     security:
    #       keycloak:
    #
    # Sem esta regra, o @ConfigurationProperties passa a apontar para `<nome>.security.keycloak`
    # enquanto o YAML continua dizendo `app:` — e a aplicação só falha no boot, com um erro de
    # binding que não menciona rename nenhum.
    if ($arquivo.Extension -in '.yml', '.yaml') {
        $conteudo = $conteudo -creplace '(?m)^app:$', "${Nome}:"
    }

    # -cne, e não -ne: a comparação padrão do PowerShell é case-insensitive, e passaria batido
    # numa troca que só muda a caixa.
    if ($conteudo -cne $original) {
        if (-not $DryRun) {
            [System.IO.File]::WriteAllText($arquivo.FullName, $conteudo, (New-Object System.Text.UTF8Encoding($false)))
        }
        $alterados++
    }
}

Write-Host "Conteúdo: $alterados arquivos" -ForegroundColor Green

# ---------------------------------------------------------------------------------------------
# 2. Caminhos. Do mais profundo para o mais raso, ou renomear um diretório invalidaria os caminhos
#    dos filhos que ainda faltam.
# ---------------------------------------------------------------------------------------------

$renomeados = 0

# 2a. arquivos
#
# `App[A-Z]` cobre AppApplication.java, mas também AppKeycloakProperties.java e qualquer outra
# classe que o molde prefixe. Renomear só a primeira deixa o arquivo com um nome e a classe com
# outro — e o Java recusa a compilar, o que é sorte: o modo de falha é alto.
#
# ⚠️ NÃO renomeie qualquer arquivo `app-*`. No frontend, `app-footer.component.ts` e
# `app-field-error` usam o **prefixo de seletor do Angular** (`"prefix": "app"` no angular.json),
# que é convenção do framework e não tem relação com o nome do projeto. Renomeá-los quebra os
# imports sem nenhum ganho. Só os arquivos de tema, nomeados a partir do projeto, entram.
$arquivosARenomear = @("app-login.css", "app-realm.yaml")

Get-ChildItem -Recurse -File $raiz -Force |
    Where-Object { $_.FullName -notmatch '\\\.git\\|\\node_modules\\|\\target\\|\\dist\\|\\\.angular\\' } |
    Where-Object { $_.Name -cmatch '^App[A-Z]' -or $arquivosARenomear -ccontains $_.Name } |
    ForEach-Object {
        $novo = $_.Name -creplace '^App(?=[A-Z])', $Pascal
        if ($arquivosARenomear -ccontains $_.Name) { $novo = $_.Name.Replace('app-', "$Nome-") }
        if ($novo -cne $_.Name) {
            if (-not $DryRun) { Rename-Item -LiteralPath $_.FullName -NewName $novo }
            $renomeados++
        }
    }

# 2b. diretórios, do mais profundo para o mais raso
Get-ChildItem -Recurse -Directory $raiz -Force |
    Where-Object { $_.FullName -notmatch '\\\.git\\|\\node_modules\\|\\target\\|\\dist\\|\\\.angular\\' } |
    Sort-Object { $_.FullName.Length } -Descending |
    ForEach-Object {
        $novo = $null
        if ($_.Name -ceq 'app' -and $_.Parent.Name -ceq 'gems') { $novo = $Nome }       # pacote Java
        elseif ($_.Name -ceq 'app' -and $_.Parent.Name -ceq 'keycloak-theme') { $novo = $Nome }
        elseif ($_.Name -clike 'app-*') { $novo = $_.Name.Replace('app-', "$Nome-") }

        if ($novo) {
            if (-not $DryRun -and (Test-Path -LiteralPath $_.FullName)) {
                Rename-Item -LiteralPath $_.FullName -NewName $novo
            }
            $renomeados++
        }
    }

Write-Host "Caminhos: $renomeados itens" -ForegroundColor Green

# ---------------------------------------------------------------------------------------------
# 3. A hierarquia do pacote, quando o grupo muda.
# ---------------------------------------------------------------------------------------------

if ($Grupo -cne 'br.com.gems' -and -not $DryRun) {
    Write-Host ''
    Write-Host "AVISO: o groupId mudou para '$Grupo'." -ForegroundColor Yellow
    Write-Host '  Os diretórios de pacote em src/main/java e src/test/java ainda estão sob' -ForegroundColor Yellow
    Write-Host "  br/com/gems/. Mova-os para $($Grupo -replace '\.', '/')/ à mão e rode o gate." -ForegroundColor Yellow
}

# ---------------------------------------------------------------------------------------------
# 4. Relatório e próximos passos.
# ---------------------------------------------------------------------------------------------

Write-Host ''
if ($DryRun) {
    Write-Host 'MODO SECO — nada foi alterado.' -ForegroundColor Yellow
    exit 0
}

Write-Host 'Pronto. Agora, nesta ordem:' -ForegroundColor Cyan
Write-Host ''
Write-Host '  1. Confira que não sobrou nada do molde:'
Write-Host '       git grep -inE "\bapp-|\bapp_|br\.com\.gems\.app|AppApplication|APP_"'
Write-Host '     Só devem sobrar os seletores Angular (app-root, app-footer, ...) e o'
Write-Host '     "prefix": "app" de angular.json — são convenção do framework, não nome de projeto.'
Write-Host ''
Write-Host '  2. Rode o gate completo:'
Write-Host '       cd backend  ; .\mvnw.cmd clean install'
Write-Host '       cd ..\frontend ; npm install --legacy-peer-deps ; npm test ; npm run build'
Write-Host ''
Write-Host '  3. Suba o ambiente:'
Write-Host '       cd environments\local ; docker compose up -d'
Write-Host ''
Write-Host '  4. Ajuste o que é decisão sua, não renomeação:'
Write-Host '     Identidade visível ao usuário — o script NÃO toca, porque nome de exibição não é'
Write-Host '     nome técnico:'
Write-Host '       - a constante de nome em frontend/src/app/app.ts (o rótulo da casca autenticada)'
Write-Host '       - <title> em frontend/src/index.html'
Write-Host '       - "name" em frontend/package.json e o projeto em frontend/angular.json'
Write-Host '       - a paleta em frontend/src/app/app.config.ts (provideGemsTheme)'
Write-Host '       - a identidade do tema de login em environments/keycloak-theme/ (logo, cores e'
Write-Host '         o conceito visual — o molde traz um layout de documento, não uma marca)'
Write-Host '     Metadados e governança:'
Write-Host '       - <developer> e <description> em backend/pom.xml'
Write-Host '       - o preâmbulo de .specify/memory/constitution.md'
Write-Host '       - o README.md'
Write-Host '     Publicação, se e quando houver VPS:'
Write-Host '       - o caminho do clone em .github/workflows/deploy.yml (o script assume'
Write-Host "         $Nome-project na home do usuário SSH)"
Write-Host ''
Write-Host '  5. Quando o primeiro domínio real existir, remova `parceiro` e `solicitacao`' -ForegroundColor Yellow
Write-Host '     POR INTEIRO — backend, frontend, testes, changeset e specs/001.' -ForegroundColor Yellow
Write-Host '     É a última cláusula do Princípio VIII, e ela não é opcional.' -ForegroundColor Yellow
Write-Host ''
