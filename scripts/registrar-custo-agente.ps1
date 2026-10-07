[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$Agent,
    [Parameter(Mandatory)][string]$Model,
    [Parameter(Mandatory)][string]$Lote,
    [Parameter(Mandatory)][ValidateSet('new', 'resumed')][string]$Session,
    [Parameter(Mandatory)][long]$InputTokens,
    [Parameter(Mandatory)][long]$CachedTokens,
    [Parameter(Mandatory)][long]$OutputTokens,
    [Parameter(Mandatory)][decimal]$BilledCost,
    [Parameter(Mandatory)][ValidateSet('accepted', 'corrected', 'discarded')][string]$Outcome,
    [Parameter(Mandatory)][string]$Gate,
    [Parameter(Mandatory)][int]$ReviewMinutes,
    [string]$OutputPath
)

$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
if (-not $OutputPath) {
    $OutputPath = Join-Path $repo '.agents/metrics/custos.csv'
}

$parent = Split-Path -Parent $OutputPath
if (-not (Test-Path -LiteralPath $parent)) {
    New-Item -ItemType Directory -Path $parent | Out-Null
}

$registro = [pscustomobject]@{
    TimestampUtc  = [DateTime]::UtcNow.ToString('o')
    Agent         = $Agent
    Model         = $Model
    Lote          = $Lote
    Session       = $Session
    InputTokens   = $InputTokens
    CachedTokens  = $CachedTokens
    OutputTokens  = $OutputTokens
    BilledCost    = $BilledCost
    Outcome       = $Outcome
    Gate          = $Gate
    ReviewMinutes = $ReviewMinutes
}

if (Test-Path -LiteralPath $OutputPath) {
    $registro | Export-Csv -LiteralPath $OutputPath -NoTypeInformation -Append -Encoding UTF8
} else {
    $registro | Export-Csv -LiteralPath $OutputPath -NoTypeInformation -Encoding UTF8
}

Write-Output $OutputPath
