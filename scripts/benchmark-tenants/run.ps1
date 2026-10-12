[CmdletBinding()]
param(
    [ValidateRange(1, 100)]
    [int]$N = 100,
    [string]$Output
)

$ErrorActionPreference = 'Stop'
$backend = Join-Path $PSScriptRoot '../../backend'
$arguments = @(
    '-B', '-pl', 'app-deploy', '-am', 'test',
    '-Dtest=TenantProvisioningBenchmark',
    "-Dbenchmark.tenants=$N",
    '-Dsurefire.failIfNoSpecifiedTests=false'
)
if ($Output) {
    $arguments += "-Dbenchmark.output=$([System.IO.Path]::GetFullPath($Output))"
}
Push-Location $backend
try {
    & .\mvnw.cmd @arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Benchmark falhou com código $LASTEXITCODE"
    }
} finally {
    Pop-Location
}
