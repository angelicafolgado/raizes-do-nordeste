param([switch]$SomentePreparar)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot

# Usa Node instalado ou o runtime local já disponível neste computador.
$nodeCommand = Get-Command node -ErrorAction SilentlyContinue
$runtimeRoot = Join-Path $env:USERPROFILE '.cache\codex-runtimes\codex-primary-runtime\dependencies\node'
if (-not $nodeCommand) {
    $runtimeBin = Join-Path $runtimeRoot 'bin'
    if (-not (Test-Path -LiteralPath (Join-Path $runtimeBin 'node.exe'))) {
        throw 'Instale Node.js 20 ou superior e execute este script novamente.'
    }
    $env:PATH = $runtimeBin + ';' + $env:PATH
}
$npmCommand = Get-Command npm.cmd -ErrorAction SilentlyContinue
$pnpmPath = Join-Path $runtimeRoot 'node_modules\pnpm\bin\pnpm.cjs'
if (-not $npmCommand -and -not (Test-Path -LiteralPath $pnpmPath)) {
    throw 'npm nao encontrado. Instale Node.js com npm.'
}
function Invoke-Package {
    if ($npmCommand) { & $npmCommand.Source @args }
    else { & node $pnpmPath @args }
    if ($LASTEXITCODE -ne 0) { throw ('Comando falhou: ' + ($args -join ' ')) }
}
if (-not (Test-Path -LiteralPath 'node_modules\.bin\prisma.cmd')) {
    Invoke-Package install
}
Invoke-Package run setup
Invoke-Package run build
if (-not $SomentePreparar) {
    Write-Host 'Swagger: http://localhost:3000/api-docs (porta padrao). Ctrl+C para encerrar.'
    Invoke-Package start
}
