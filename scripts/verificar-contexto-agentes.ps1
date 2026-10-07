[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $PSScriptRoot
$falhas = [System.Collections.Generic.List[string]]::new()

function Falhar([string]$mensagem) {
    $falhas.Add($mensagem)
}

Write-Output 'Orcamento dos arquivos raiz'
$arquivosRaiz = @(
    'AGENTS.md',
    'CLAUDE.md',
    'CODEX.md',
    'GEMINI.md',
    '.github/copilot-instructions.md'
)

foreach ($relativo in $arquivosRaiz) {
    $caminho = Join-Path $raiz $relativo
    if (-not (Test-Path -LiteralPath $caminho -PathType Leaf)) {
        Falhar("arquivo ausente: $relativo")
        continue
    }
    $item = Get-Item -LiteralPath $caminho
    $linhas = @(Get-Content -LiteralPath $caminho).Count
    Write-Output ("  {0}: {1} bytes, {2} linhas" -f $relativo, $item.Length, $linhas)
    if ($item.Length -gt 16384) { Falhar("$relativo excede 16384 bytes") }
    if ($linhas -gt 200) { Falhar("$relativo excede 200 linhas") }
}

Write-Output 'Paridade das camadas'
$camadas = @('backend', 'frontend', 'environments', 'specs')
foreach ($camada in $camadas) {
    $esperados = @(
        "$camada/AGENTS.md",
        "$camada/GEMINI.md",
        ".claude/rules/$camada.md",
        ".cursor/rules/$camada.mdc",
        ".github/instructions/$camada.instructions.md"
    )
    foreach ($relativo in $esperados) {
        if (-not (Test-Path -LiteralPath (Join-Path $raiz $relativo) -PathType Leaf)) {
            Falhar("regra de camada ausente: $relativo")
        }
    }
}

Write-Output 'Sintaxe e contrato'
foreach ($relativo in @('.claude/settings.json', '.gemini/settings.json', '.specify/feature.json', 'kilo.json', '.vscode/settings.json')) {
    try {
        Get-Content -Raw -LiteralPath (Join-Path $raiz $relativo) | ConvertFrom-Json | Out-Null
    } catch {
        Falhar("JSON invalido: $relativo - $($_.Exception.Message)")
    }
}

$template = Get-Content -Raw -LiteralPath (Join-Path $raiz '.specify/templates/pacote-de-execucao-template.md')
foreach ($marcador in @('### READ', '### EDIT / CREATE', '## 5.', '## 6.')) {
    if (-not $template.Contains($marcador)) { Falhar("template de lote sem marcador: $marcador") }
}

$ignores = @('.cursorignore', '.geminiignore', '.aiexclude', '.kilocodeignore')
$padroes = @('.kilo/', '**/node_modules/', '**/target/', 'business/**/*.png', '.env')
foreach ($ignore in $ignores) {
    $conteudo = Get-Content -Raw -LiteralPath (Join-Path $raiz $ignore)
    foreach ($padrao in $padroes) {
        if (-not $conteudo.Contains($padrao)) { Falhar("$ignore nao cobre $padrao") }
    }
}

Write-Output 'Politica economica do Kilo'
$modeloKilo = 'openrouter/deepseek/deepseek-v4-flash-0731'
$kilo = Get-Content -Raw -LiteralPath (Join-Path $raiz 'kilo.json') | ConvertFrom-Json

if ($kilo.model -ne $modeloKilo) { Falhar('modelo principal do Kilo nao e o modelo economico permitido') }
if ($kilo.small_model -ne $modeloKilo) { Falhar('small_model do Kilo nao e o modelo economico permitido') }
if ($kilo.default_agent -ne 'ask') { Falhar('agente padrao do Kilo deve ser ask') }
if (@($kilo.enabled_providers).Count -ne 1 -or $kilo.enabled_providers[0] -ne 'openrouter') {
    Falhar('Kilo deve habilitar somente o provedor openrouter')
}
if (@($kilo.provider.openrouter.whitelist).Count -ne 1 -or $kilo.provider.openrouter.whitelist[0] -ne 'deepseek/deepseek-v4-flash-0731') {
    Falhar('allowlist do OpenRouter deve conter somente DeepSeek V4 Flash')
}

foreach ($agente in $kilo.agent.PSObject.Properties) {
    if ($agente.Value.model -ne $modeloKilo) { Falhar("agente Kilo usa modelo nao permitido: $($agente.Name)") }
    if ($agente.Value.steps -lt 1 -or $agente.Value.steps -gt 15) {
        Falhar("agente Kilo excede 15 passos: $($agente.Name)")
    }
}

$kiloSerializado = $kilo | ConvertTo-Json -Depth 20 -Compress
foreach ($premium in @('anthropic', 'claude', 'openai/gpt', 'deepseek-v4-pro', 'sonnet', 'opus')) {
    if ($kiloSerializado -match [regex]::Escape($premium)) { Falhar("Kilo referencia modelo premium: $premium") }
}

if ($kilo.subagent_depth -ne 0 -or $kilo.permission.task -ne 'deny') {
    Falhar('subagentes do Kilo devem permanecer bloqueados')
}
if ($kilo.permission.doom_loop -ne 'deny' -or $kilo.experimental.continue_loop_on_deny -ne $false) {
    Falhar('loops automaticos do Kilo devem permanecer bloqueados')
}
if ($kilo.permission.edit -ne 'ask' -or $kilo.permission.bash.'*' -ne 'ask') {
    Falhar('edicao e shell do Kilo devem exigir confirmacao')
}
if ($kilo.experimental.batch_tool -ne $true -or $kilo.experimental.openTelemetry -ne $false) {
    Falhar('batch deve estar ativo e OpenTelemetry desativada no Kilo')
}
if ($kilo.compaction.auto -ne $true -or $kilo.compaction.prune -ne $true) {
    Falhar('compactacao e poda do Kilo devem permanecer ativas')
}
if (@($kilo.skills.paths).Count -gt 0 -or @($kilo.skills.urls).Count -gt 0) {
    Falhar('Kilo nao deve carregar skills adicionais ou remotas')
}
if ($kilo.indexing.enabled -ne $false -or $kilo.tools.semantic_search -ne $false) {
    Falhar('indexacao e busca semantica pagas devem permanecer desativadas')
}
if ($kilo.tools.memory_recall -ne $false -or $kilo.tools.memory_save -ne $false) {
    Falhar('ferramentas automaticas de memoria devem permanecer desativadas')
}

$watcher = @($kilo.watcher.ignore)
foreach ($padrao in @('.kilo/**', '**/node_modules/**', '**/target/**', '**/*-lock.json')) {
    if ($watcher -notcontains $padrao) { Falhar("watcher do Kilo nao cobre $padrao") }
}

$vscode = Get-Content -Raw -LiteralPath (Join-Path $raiz '.vscode/settings.json') | ConvertFrom-Json
foreach ($chave in @(
    'kilo-code.new.autocomplete.enableAutoTrigger',
    'kilo-code.new.autocomplete.enableSmartInlineTaskKeybinding',
    'kilo-code.new.autocomplete.enableChatAutocomplete'
)) {
    if ($vscode.$chave -ne $false) { Falhar("autocomplete do Kilo deve estar desligado: $chave") }
}

if ($falhas.Count -gt 0) {
    Write-Output ''
    foreach ($falha in $falhas) { Write-Error $falha }
    exit 1
}

Write-Output 'Contexto de agentes aprovado.'
