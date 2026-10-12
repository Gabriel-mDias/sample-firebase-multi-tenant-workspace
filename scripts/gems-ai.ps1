<#
.SYNOPSIS
  Wrapper portatil da CLI GEMS AI. Resolve a fonte, confere o pin de gems-ai.json e repassa argumentos/exit code.
.DESCRIPTION
  Ordem de resolucao: (1) $env:GEMS_AI_SDK_SRC (python -m gems_ai com PYTHONPATH=src; se definido e
  inexistente, falha sem cair para outra fonte); (2) comando `gems-ai` no PATH; (3) ../.tooling/gems-ai-sdk/src
  relativo a raiz do sample. Quando a fonte e um src dentro de repo git, `git rev-parse HEAD` do SDK deve ser
  igual ao campo commit de gems-ai.json; src fora de repo git falha (fail-closed). A CLI do PATH so tem a
  versao conferida (`gems-ai --version` deve ser igual ao campo version de gems-ai.json), nao o commit.
#>
$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $PSScriptRoot

function Falhar([string]$mensagem) {
    [Console]::Error.WriteLine("gems-ai.ps1: $mensagem")
    exit 2
}

function Confirmar-Pin([string]$src) {
    $ErrorActionPreference = 'Continue' # no PS 5.1, stderr do git vira excecao com 'Stop'
    $topo = (& git -C $src rev-parse --show-toplevel 2>$null)
    if ($LASTEXITCODE -ne 0 -or -not $topo) {
        Falhar "fonte '$src' nao esta em um repo git; impossivel conferir o pin de gems-ai.json (fail-closed)."
    }
    $head = ((& git -C $src rev-parse HEAD 2>$null) | Select-Object -First 1).Trim()
    $config = Get-Content -Raw -LiteralPath (Join-Path $raiz 'gems-ai.json') | ConvertFrom-Json
    if ($head -ne $config.commit) {
        Falhar "pin divergente: SDK em '$src' esta em $head, mas gems-ai.json exige $($config.commit). Faca checkout do commit pinado."
    }
}

$fonte = $null
if ($null -ne $env:GEMS_AI_SDK_SRC -and $env:GEMS_AI_SDK_SRC -ne '') {
    if (-not (Test-Path -LiteralPath $env:GEMS_AI_SDK_SRC -PathType Container)) {
        Falhar "GEMS_AI_SDK_SRC definido mas inexistente: $($env:GEMS_AI_SDK_SRC)"
    }
    $fonte = (Resolve-Path -LiteralPath $env:GEMS_AI_SDK_SRC).Path
} elseif (Get-Command gems-ai -ErrorAction SilentlyContinue) {
    $versaoEsperada = (Get-Content -Raw -LiteralPath (Join-Path $raiz 'gems-ai.json') | ConvertFrom-Json).version
    $versaoPath = (& gems-ai --version 2>&1 | Out-String)
    if ($LASTEXITCODE -ne 0 -or -not $versaoEsperada -or $versaoPath.Trim() -ne ([string]$versaoEsperada).Trim()) {
        Falhar "gems-ai do PATH nao reporta a versao $versaoEsperada de gems-ai.json (saiu: $($versaoPath.Trim()))."
    }
    & gems-ai @args
    exit $LASTEXITCODE
} else {
    $candidato = Join-Path $raiz '../.tooling/gems-ai-sdk/src'
    if (Test-Path -LiteralPath $candidato -PathType Container) {
        $fonte = (Resolve-Path -LiteralPath $candidato).Path
    }
}

if (-not $fonte) {
    Falhar 'nenhuma fonte da CLI encontrada (GEMS_AI_SDK_SRC, comando gems-ai no PATH ou ../.tooling/gems-ai-sdk/src).'
}

Confirmar-Pin $fonte

$pythonPathAnterior = $env:PYTHONPATH
try {
    $env:PYTHONPATH = $fonte
    & python -m gems_ai @args
    $codigo = $LASTEXITCODE
} finally {
    $env:PYTHONPATH = $pythonPathAnterior
}
exit $codigo