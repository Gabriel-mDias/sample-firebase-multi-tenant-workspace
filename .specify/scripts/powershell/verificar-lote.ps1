#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Confronta o que o pacote PEDIU, o que o executor DECLAROU e o que a arvore MOSTRA.

.DESCRIPTION
    Le a secao 1 do pacote-de-execucao (o esperado), a secao 5 do estado-lote (o declarado)
    e o git status --porcelain --untracked-files=all (o real), e reporta as divergencias
    entre os tres.

    Ele NAO substitui rodar o portao: checa a extensao e a honestidade do relato, nunca a
    correcao do codigo. E NAO confia no estado — o valor dele esta justamente em comparar
    o que o executor declarou com o que a arvore mostra.

.EXAMPLE
    .\verificar-lote.ps1 -Pacote specs/017-x/pacote-de-execucao-lote-A.md -Estado specs/017-x/estado-lote-A.md

.NOTES
    Exit 0 = aprovado (pode ter avisos) | 1 = erro de uso ou de parse | 2 = lote reprovado
#>
[CmdletBinding()]
param(
    [string]$Pacote,
    [string]$Estado,
    [string]$Ancora,
    [string]$Worktree,
    [switch]$Help
)

$ErrorActionPreference = 'Stop'

# -Pacote e -Estado sao obrigatorios, mas validados aqui e nao por [Parameter(Mandatory)]:
# com Mandatory, o PowerShell recusa a chamada antes de executar o corpo, e o -Help fica
# inalcancavel — alem de abrir um prompt interativo, que em headless trava.
if ($Help -or -not $Pacote -or -not $Estado) {
    Write-Output 'Uso: verificar-lote.ps1 -Pacote <caminho> -Estado <caminho> [-Ancora <arquivo>] [-Worktree <caminho>]'
    Write-Output ''
    Write-Output '  -Pacote    o pacote-de-execucao do lote; a secao 1 dele e a lista fechada (o esperado).'
    Write-Output '  -Estado    o estado-lote do executor; a secao 5 dele e o que ele declara ter tocado.'
    Write-Output '  -Ancora    arquivo com a saida de git status tirada ANTES do disparo; suas entradas'
    Write-Output '             sao descontadas, para nao acusar trabalho anterior ao lote.'
    Write-Output '  -Worktree  onde rodar o git status, quando o lote foi isolado em worktree proprio.'
    Write-Output ''
    Write-Output '  Exit 0 = aprovado (pode ter avisos) | 1 = erro de uso ou de parse | 2 = lote reprovado'
    if ($Help) { exit 0 }
    exit 1
}

# ---------------------------------------------------------------- falha ruidosa
# Parse que nao consegue ler devolveria lista vazia, e lista vazia passa como "nada fora
# da lista" — exatamente a rede que passa com e sem o defeito. Aqui, nunca: aborta
# nomeando o arquivo e a secao.
function Stop-Ruidoso {
    param([string]$Mensagem, [string]$Arquivo, [string]$Secao)
    [Console]::Error.WriteLine('')
    [Console]::Error.WriteLine('ERRO DE LEITURA — nao foi possivel verificar o lote.')
    [Console]::Error.WriteLine("  arquivo: $Arquivo")
    if ($Secao) { [Console]::Error.WriteLine("  secao..: $Secao") }
    [Console]::Error.WriteLine("  causa..: $Mensagem")
    [Console]::Error.WriteLine('')
    [Console]::Error.WriteLine("Corrija o arquivo e rode de novo. NAO trate isto como 'nada fora da lista'.")
    exit 1
}

function ConvertTo-CaminhoNormal {
    param([string]$Caminho)
    $p = $Caminho.Trim()
    $p = $p.Trim([char]34)
    $p = $p.Trim([char]39)
    $p = $p -replace '\\', '/'
    $p = $p -replace '^\./', ''
    return $p.TrimEnd('/')
}

# Corpo de uma secao "## <n>." ate a proxima "## ".
function Get-CorpoDaSecao {
    param([string[]]$Linhas, [int]$Numero)
    $inicio = -1
    for ($i = 0; $i -lt $Linhas.Count; $i++) {
        if ($Linhas[$i] -match "^##\s+$Numero\.") { $inicio = $i + 1; break }
    }
    if ($inicio -lt 0) { return $null }
    $corpo = @()
    for ($i = $inicio; $i -lt $Linhas.Count; $i++) {
        if ($Linhas[$i] -match '^##\s') { break }
        $corpo += $Linhas[$i]
    }
    return , $corpo
}

# Caminhos entre crases nas LINHAS DE TABELA da secao 1 — o texto em volta e ignorado.
function Get-CaminhosDaTabela {
    param([string[]]$Corpo)
    $achados = @()
    foreach ($linha in $Corpo) {
        if ($linha -notmatch '^\s*\|') { continue }
        foreach ($m in [regex]::Matches($linha, '`([^`]+)`')) {
            $valor = $m.Groups[1].Value
            if ($valor -match '[/.]') { $achados += (ConvertTo-CaminhoNormal $valor) }
        }
    }
    return , $achados
}

# Linhas de git status --porcelain -> caminhos.
function ConvertFrom-Porcelain {
    param([string[]]$Linhas)
    $achados = @()
    foreach ($linha in $Linhas) {
        if ([string]::IsNullOrWhiteSpace($linha)) { continue }
        $caminho = $null
        if ($linha -match '^(..)\s(.+)$') { $caminho = $Matches[2] }
        elseif ($linha -match '^\s*[MADRCU?!]+\s+(.+)$') { $caminho = $Matches[1] }
        else { continue }
        if ($caminho -match '\s->\s') { $caminho = ($caminho -split '\s->\s')[-1] }
        $achados += (ConvertTo-CaminhoNormal $caminho)
    }
    return , $achados
}

# Conteudo do primeiro bloco cercado de uma secao. $null = bloco AUSENTE (erro de parse);
# array vazio = bloco presente e vazio (que e um achado, nao um erro).
function Get-BlocoText {
    param([string[]]$Corpo)
    $dentro = $false
    $achou = $false
    $conteudo = @()
    foreach ($linha in $Corpo) {
        if (-not $dentro -and $linha -match '^\s*```') { $dentro = $true; $achou = $true; continue }
        if ($dentro -and $linha -match '^\s*```') { break }
        if ($dentro) { $conteudo += $linha }
    }
    if (-not $achou) { return $null }
    return , $conteudo
}

# Sobra texto de verdade, depois de tirar comentario, citacao, cerca, separador de
# tabela, placeholder e rotulo de campo sem valor?
function Test-SecaoPreenchida {
    param([string[]]$Corpo)
    if ($null -eq $Corpo -or $Corpo.Count -eq 0) { return $false }

    $texto = ($Corpo -join "`n")
    # Comentario e placeholder saem da STRING INTEIRA, nao linha a linha: os dois
    # atravessam quebras de linha no template, e removidos por linha eles sobrevivem
    # em pedacos — que e o que fazia um estado intocado passar como preenchido.
    $texto = [regex]::Replace($texto, '<!--.*?-->', '', 'Singleline')
    # De dentro para fora, ate estabilizar: um placeholder pode conter outro
    # (`<... <caminho> ...>`), e uma passada so deixaria o resto do externo de pe.
    for ($passada = 0; $passada -lt 10; $passada++) {
        $antes = $texto
        $texto = [regex]::Replace($texto, '<[^<>]*>', '')
        if ($texto -eq $antes) { break }
    }

    $linhas = @($texto -split "`n")

    # Cabecalho e separador de tabela sao estrutura, nao conteudo: uma tabela so com
    # eles esta VAZIA, por mais que o cabecalho tenha texto.
    $estrutura = New-Object 'System.Collections.Generic.HashSet[int]'
    for ($i = 0; $i -lt $linhas.Count - 1; $i++) {
        if ($linhas[$i].Trim() -match '^\|' -and $linhas[$i + 1].Trim() -match '^\|[\s:|\-]+\|?$') {
            [void]$estrutura.Add($i)
            [void]$estrutura.Add($i + 1)
        }
    }

    $restante = @()
    for ($i = 0; $i -lt $linhas.Count; $i++) {
        if ($estrutura.Contains($i)) { continue }
        $l = $linhas[$i].Trim()
        if ($l -eq '') { continue }
        if ($l -match '^>') { continue }
        if ($l -match '^```') { continue }
        if ($l -match '^([-*_]\s*){3,}$') { continue }   # regua horizontal entre secoes
        if ($l -match '^\|[\s:|\-]*\|?$') { continue }
        if ($l -match '^[-*]\s*\*\*[^*]+:\*\*\s*$') { continue }
        $l = $l.Trim('|').Trim()
        if ($l -ne '') { $restante += $l }
    }
    return ($restante.Count -gt 0)
}

# ---------------------------------------------------------------- entrada
foreach ($par in @(@{ n = 'Pacote'; v = $Pacote }, @{ n = 'Estado'; v = $Estado })) {
    if (-not (Test-Path -LiteralPath $par.v -PathType Leaf)) {
        Stop-Ruidoso -Arquivo $par.v -Mensagem "arquivo nao encontrado (parametro -$($par.n))."
    }
}

$linhasPacote = @(Get-Content -LiteralPath $Pacote -Encoding UTF8)
$linhasEstado = @(Get-Content -LiteralPath $Estado -Encoding UTF8)

# --- esperado: secao 1 do pacote
$corpoS1 = Get-CorpoDaSecao -Linhas $linhasPacote -Numero 1
if ($null -eq $corpoS1) {
    Stop-Ruidoso -Arquivo $Pacote -Secao '## 1. Fronteira fechada do lote' -Mensagem 'secao nao encontrada. A numeracao do pacote e fixa — veja .specify/templates/pacote-de-execucao-template.md.'
}
# Variavel intermediaria de proposito: as funcoes acima devolvem o array embrulhado
# (return , $x) para preservar o vazio, e mandar isso direto para o pipeline entregaria
# o array inteiro como UM item.
$achadosPacote = Get-CaminhosDaTabela -Corpo $corpoS1
$esperado = @($achadosPacote | Select-Object -Unique)
if ($esperado.Count -eq 0) {
    Stop-Ruidoso -Arquivo $Pacote -Secao '## 1. Fronteira fechada do lote' -Mensagem 'nenhum caminho EDIT/CREATE encontrado na tabela. Todo caminho vai entre crases, em linha que comeca com barra vertical.'
}

# --- declarado: secao 5 do estado
$corpoS5 = Get-CorpoDaSecao -Linhas $linhasEstado -Numero 5
if ($null -eq $corpoS5) {
    Stop-Ruidoso -Arquivo $Estado -Secao '## 5. Arquivos tocados' -Mensagem 'secao nao encontrada. Nao apague secao do estado — veja .specify/templates/estado-lote-template.md.'
}
$blocoDeclarado = Get-BlocoText -Corpo $corpoS5
if ($null -eq $blocoDeclarado) {
    Stop-Ruidoso -Arquivo $Estado -Secao '## 5. Arquivos tocados' -Mensagem 'bloco de codigo ausente. Cole ali a saida literal de: git status --porcelain --untracked-files=all'
}
$achadosEstado = ConvertFrom-Porcelain -Linhas $blocoDeclarado
$declarado = @($achadosEstado | Select-Object -Unique)

# --- real: a arvore
$gitArgs = @()
if ($Worktree) {
    if (-not (Test-Path -LiteralPath $Worktree -PathType Container)) {
        Stop-Ruidoso -Arquivo $Worktree -Mensagem 'worktree nao encontrado (parametro -Worktree).'
    }
    $gitArgs += @('-C', $Worktree)
}
$gitArgs += @('status', '--porcelain', '--untracked-files=all')
$saidaGit = @(& git @gitArgs)
if ($LASTEXITCODE -ne 0) {
    Stop-Ruidoso -Arquivo '(git)' -Mensagem "git status falhou com exit $LASTEXITCODE."
}
$achadosArvore = ConvertFrom-Porcelain -Linhas $saidaGit
$real = @($achadosArvore | Select-Object -Unique)

# --- descontos: a ancora e o proprio par do protocolo
$descontar = New-Object 'System.Collections.Generic.HashSet[string]'
function ConvertTo-CaminhoDaArvore {
    param([string]$Caminho)
    $absoluto = (Resolve-Path -LiteralPath $Caminho).Path
    if (-not $Worktree) { return (ConvertTo-CaminhoNormal $absoluto) }

    $raiz = (Resolve-Path -LiteralPath $Worktree).Path.TrimEnd([char]'\', [char]'/')
    if (-not $absoluto.StartsWith($raiz, [System.StringComparison]::OrdinalIgnoreCase)) {
        Stop-Ruidoso -Arquivo $Caminho -Mensagem 'pacote/estado fora do worktree informado; nao e seguro descontar um caminho de outro repositorio.'
    }
    return (ConvertTo-CaminhoNormal $absoluto.Substring($raiz.Length).TrimStart([char]'\', [char]'/'))
}
[void]$descontar.Add((ConvertTo-CaminhoDaArvore $Pacote))
[void]$descontar.Add((ConvertTo-CaminhoDaArvore $Estado))
if ($Ancora) {
    if (-not (Test-Path -LiteralPath $Ancora -PathType Leaf)) {
        Stop-Ruidoso -Arquivo $Ancora -Mensagem 'arquivo de ancora nao encontrado (parametro -Ancora).'
    }
    $achadosAncora = ConvertFrom-Porcelain -Linhas @(Get-Content -LiteralPath $Ancora)
    foreach ($c in $achadosAncora) { [void]$descontar.Add($c) }
}
$real = @($real | Where-Object { -not $descontar.Contains($_) })
$declarado = @($declarado | Where-Object { -not $descontar.Contains($_) })

# ---------------------------------------------------------------- confronto
function Get-Diferenca {
    param([string[]]$De, [string[]]$Menos)
    $set = New-Object 'System.Collections.Generic.HashSet[string]'
    foreach ($x in $Menos) { [void]$set.Add($x) }
    return @($De | Where-Object { -not $set.Contains($_) })
}

$foraDaLista      = Get-Diferenca -De $real      -Menos $esperado
$naoDeclarado     = Get-Diferenca -De $real      -Menos $declarado
$declaradoAusente = Get-Diferenca -De $declarado -Menos $real
$naoEntregue      = Get-Diferenca -De $esperado  -Menos $real

$rotulos = @{ 1 = 'Rollback'; 2 = 'Concluido'; 3 = 'Em curso'; 4 = 'Nao iniciado';
              5 = 'Arquivos tocados'; 6 = 'Decisoes fora do pacote'; 7 = 'O que esta quebrado agora' }
$camposVazios = @()
foreach ($n in 1..7) {
    $corpo = Get-CorpoDaSecao -Linhas $linhasEstado -Numero $n
    if ($null -eq $corpo) { $camposVazios += "secao $n ($($rotulos[$n])) — AUSENTE"; continue }
    if ($n -eq 5) { $preenchida = ($blocoDeclarado.Count -gt 0) }
    else { $preenchida = Test-SecaoPreenchida -Corpo $corpo }
    if (-not $preenchida) { $camposVazios += "secao $n ($($rotulos[$n]))" }
}

# ---------------------------------------------------------------- relatorio
function Write-Bloco {
    param([string]$Titulo, [string[]]$Itens, [string]$Porque, [string]$Cor)
    if ($Itens.Count -eq 0) { return }
    Write-Host ''
    Write-Host "  $Titulo ($($Itens.Count))" -ForegroundColor $Cor
    Write-Host "    $Porque" -ForegroundColor DarkGray
    foreach ($i in $Itens) { Write-Host "      $i" }
}

if ($Worktree) { $ondeReal = $Worktree } else { $ondeReal = 'diretorio atual' }

Write-Host ''
Write-Host 'Verificacao do lote' -ForegroundColor Cyan
Write-Host "  pacote (esperado) : $Pacote -> $($esperado.Count) arquivo(s)"
Write-Host "  estado (declarado): $Estado -> $($declarado.Count) arquivo(s)"
Write-Host "  arvore (real)     : $ondeReal -> $($real.Count) arquivo(s)"

Write-Bloco -Titulo 'FORA DA LISTA' -Itens $foraDaLista -Cor Red -Porque 'a arvore tem o que a secao 1 do pacote nao pediu -> LOTE REPROVADO, execute o rollback da secao 1 do estado.'
Write-Bloco -Titulo 'NAO DECLARADO' -Itens $naoDeclarado -Cor Red -Porque 'o executor tocou e nao registrou -> o estado nao e confiavel; desfaca e refaca.'
Write-Bloco -Titulo 'DECLARADO MAS AUSENTE' -Itens $declaradoAusente -Cor Red -Porque 'ele afirma ter escrito o que nao esta aqui — sintoma do scratch dir. Confira a invocacao antes de culpar o lote.'
Write-Bloco -Titulo 'NAO ENTREGUE' -Itens $naoEntregue -Cor Yellow -Porque 'previsto e nao tocado -> lote INCOMPLETO, que nao e lote falho. Leia a secao 3 do estado e decida retomar ou assumir.'
Write-Bloco -Titulo 'CAMPOS VAZIOS NO ESTADO' -Itens $camposVazios -Cor Yellow -Porque 'sem eles um estranho nao assume o lote. A secao 3 e a que salva ou perde o handoff.'

$reprovado = (($foraDaLista.Count + $naoDeclarado.Count + $declaradoAusente.Count) -gt 0)

Write-Host ''
if ($reprovado) {
    Write-Host '  LOTE REPROVADO.' -ForegroundColor Red
    Write-Host "  Rollback na secao 1 do estado. Nao comite, e nao 'corrija de passagem'." -ForegroundColor Red
} elseif (($naoEntregue.Count -gt 0) -or ($camposVazios.Count -gt 0)) {
    Write-Host '  Extensao do lote confere, com ressalvas acima.' -ForegroundColor Yellow
} else {
    Write-Host '  Extensao do lote confere.' -ForegroundColor Green
}
Write-Host '  Isto nao e o portao. Rode o portao do pacote voce mesmo e leia a saida.' -ForegroundColor DarkGray
Write-Host ''

if ($reprovado) { exit 2 }
exit 0
