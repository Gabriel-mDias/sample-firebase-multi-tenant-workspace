<#
.SYNOPSIS
  Gate unico GEMS AI: check --strict e verify-angular. Nao cria waiver nem baseline.
#>
$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $PSScriptRoot
$wrapper = Join-Path $PSScriptRoot 'gems-ai.ps1'
$falhou = $false
Push-Location $raiz
try {
    foreach ($comando in @(@('check', '--project', '.', '--strict'), @('verify-angular', '--project', '.'))) {
        & $wrapper @comando
        if ($LASTEXITCODE -ne 0) {
            [Console]::Error.WriteLine("gems-ai-gate.ps1: falhou: gems-ai $($comando -join ' ') (exit $LASTEXITCODE)")
            $falhou = $true
        }
    }
} finally {
    Pop-Location
}
if ($falhou) { exit 1 }
exit 0