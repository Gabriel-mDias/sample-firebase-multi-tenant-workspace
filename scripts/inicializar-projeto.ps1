<#
.SYNOPSIS
    Inicializa um produto Firebase a partir de um checkout limpo do molde.
.DESCRIPTION
    Preserva SDKs publicadas, snapshot GEMS AI, seletores Angular e APP_RUNTIME_PROFILE/FIREBASE_*.
    Formata Java e os payloads Java do clean-demo com o Spotless do backend renomeado (Java/Maven exigidos).
    -SkipFormat e fallback offline de inicializacao; nunca usar nos gates.
    Nao conecta Docker ou nuvem. Revise/registre o rename na copia antes de executar clean-demo.
.EXAMPLE
    .\scripts\inicializar-projeto.ps1 -Nome vendas -Grupo com.acme -DryRun
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$Nome,
    [string]$Grupo = 'br.com.gems',
    [string]$Descricao = '',
    [string]$Root = '',
    [switch]$DryRun,
    [switch]$Apply,
    [switch]$SkipFormat
)
$ErrorActionPreference = 'Stop'
if (-not $Root) { $Root = Split-Path -Parent $PSScriptRoot }
if ($DryRun -and $Apply) { throw 'DryRun e Apply sao mutuamente exclusivos.' }
$arguments = @('-B', (Join-Path $PSScriptRoot 'sample.py'), 'initialize', '--root', $Root,
    '--name', $Nome, '--group', $Grupo)
if ($Descricao) { $arguments += @('--description', $Descricao) }
if ($SkipFormat) { $arguments += '--skip-format' }
if ($DryRun) { $arguments += '--dry-run' } else { $arguments += '--apply' }
& python @arguments
$initializerExit = $LASTEXITCODE
if ($initializerExit -ne 0) { exit $initializerExit }
if (-not $DryRun) {
    Write-Host 'Revise o diff na copia e registre o rename antes do clean-demo (arvore limpa).'
    Write-Host 'Prepare o classpath Maven mostrado pelo dry-run do clean-demo; use Java 21 e npm ci.'
    Write-Host 'Execute clean-demo --dry-run/--apply com --root absoluto; o catalogo Java oficial e verificado.'
    Write-Host 'Gates: backend mvnw.cmd clean install; frontend npm test -- --watch=false, npm run build e npx prettier --check .'
    Write-Host 'Firebase real exige projeto isolado/configuracao manual. Hosting continua decisao posterior.'
}
exit 0
