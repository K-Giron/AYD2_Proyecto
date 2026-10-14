[CmdletBinding()]
param(
    [ValidateSet('load', 'stress', 'all')]
    [string]$Suite = 'all',
    [string]$EnvFile = '',
    [string]$BaseUrl = ''
)

$ErrorActionPreference = 'Stop'
$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
if ([string]::IsNullOrWhiteSpace($EnvFile)) {
    $EnvFile = Join-Path $scriptRoot '.env.performance'
}
if (-not (Test-Path -LiteralPath $EnvFile)) {
    throw "No existe $EnvFile. Copie .env.performance.example y ajuste el ambiente."
}
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw 'Docker no está disponible en PATH.'
}

$loadScripts = @(
    'load-01-eligibility.js',
    'load-02-mixed-checkout.js',
    'load-03-erp-history.js',
    'load-04-notifications.js',
    'load-05-state-audit.js'
)
$stressScripts = @(
    'stress-01-progressive-limit.js',
    'stress-02-checkout-spike.js',
    'stress-03-credit-vs-payklik.js',
    'stress-04-notification-backlog.js',
    'stress-05-recovery.js'
)
$scripts = switch ($Suite) {
    'load' { $loadScripts }
    'stress' { $stressScripts }
    default { $loadScripts + $stressScripts }
}

$resultsDir = Join-Path $scriptRoot 'results'
New-Item -ItemType Directory -Force -Path $resultsDir | Out-Null
$resolvedEnv = (Resolve-Path -LiteralPath $EnvFile).Path
$failed = @()

foreach ($script in $scripts) {
    $name = [IO.Path]::GetFileNameWithoutExtension($script)
    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $summaryName = "$stamp-$name-summary.json"
    $logPath = Join-Path $resultsDir "$stamp-$name.log"
    $arguments = @(
        'run', '--rm',
        '--add-host=host.docker.internal:host-gateway',
        '--env-file', $resolvedEnv,
        '-v', "${scriptRoot}:/scripts:ro",
        '-v', "${resultsDir}:/results",
        'grafana/k6', 'run',
        '--summary-trend-stats=avg,min,med,p(50),p(90),p(95),p(99),max',
        "--summary-export=/results/$summaryName",
        "/scripts/$script"
    )
    if (-not [string]::IsNullOrWhiteSpace($BaseUrl)) {
        $insertAt = [Array]::IndexOf($arguments, '-v')
        $before = $arguments[0..($insertAt - 1)]
        $after = $arguments[$insertAt..($arguments.Length - 1)]
        $arguments = $before + @('-e', "BASE_URL=$BaseUrl") + $after
    }

    Write-Host "Ejecutando $script" -ForegroundColor Cyan
    & docker @arguments 2>&1 | Tee-Object -FilePath $logPath
    if ($LASTEXITCODE -ne 0) {
        $failed += $script
    }
}

if ($failed.Count -gt 0) {
    throw "Fallaron: $($failed -join ', '). Revise results/."
}
Write-Host "Suite $Suite finalizada. Evidencia cruda: $resultsDir" -ForegroundColor Green

