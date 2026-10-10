# Hook Stop do Claude Code: bloqueia o fim do turno (exit 2) se o gate GEMS AI falhar.
$entrada = [Console]::In.ReadToEnd()
try {
    $dados = $entrada | ConvertFrom-Json
    if ($dados.stop_hook_active -is [bool] -and $dados.stop_hook_active) { exit 0 }
} catch {
    # entrada ausente ou invalida: segue para o gate
}
try {
    $gate = Join-Path (Split-Path -Parent $PSScriptRoot) 'gems-ai-gate.ps1'
    $saida = & $gate 2>&1 | Out-String
    if ($LASTEXITCODE -ne 0) {
        $resumo = ($saida.Trim() -split "`r?`n" | Select-Object -Last 5) -join ' | '
        [Console]::Error.WriteLine("Gate GEMS AI falhou; corrija antes de concluir. $resumo")
        exit 2
    }
} catch {
    [Console]::Error.WriteLine("Gate GEMS AI com erro: $($_.Exception.Message)")
    exit 2
}
exit 0