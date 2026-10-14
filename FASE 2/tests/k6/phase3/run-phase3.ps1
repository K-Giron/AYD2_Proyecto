[CmdletBinding()]
param(
    [ValidateSet(
        'dataset', 'load', 'load-01', 'load-02', 'load-03', 'load-04', 'load-05',
        'stress', 'stress-01', 'stress-02', 'stress-03', 'stress-04', 'stress-05',
        'all', 'smoke'
    )]
    [string]$Suite = 'all',
    [string]$EnvFile = '',
    [string]$BaseUrl = '',
    [string]$RunId = ''
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
if ([string]::IsNullOrWhiteSpace($RunId)) {
    $RunId = Get-Date -Format 'yyyyMMdd-HHmmss'
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
    'dataset' { @('prepare-dataset.js') }
    'load' { $loadScripts }
    'load-01' { @($loadScripts[0]) }
    'load-02' { @($loadScripts[1]) }
    'load-03' { @($loadScripts[2]) }
    'load-04' { @($loadScripts[3]) }
    'load-05' { @($loadScripts[4]) }
    'stress' { $stressScripts }
    'stress-01' { @($stressScripts[0]) }
    'stress-02' { @($stressScripts[1]) }
    'stress-03' { @($stressScripts[2]) }
    'stress-04' { @($stressScripts[3]) }
    'stress-05' { @($stressScripts[4]) }
    'smoke' { @('smoke-phase3-cloud.js') }
    default { $loadScripts + $stressScripts }
}

$resultsDir = Join-Path $scriptRoot 'results'
New-Item -ItemType Directory -Force -Path $resultsDir | Out-Null
$resolvedEnv = (Resolve-Path -LiteralPath $EnvFile).Path
$failed = @()

foreach ($script in $scripts) {
    $name = [IO.Path]::GetFileNameWithoutExtension($script)
    $scriptRunId = "$RunId-$name"
    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $summaryName = "$stamp-$name-summary.json"
    $logPath = Join-Path $resultsDir "$stamp-$name.log"
    $arguments = @(
        'run', '--rm',
        '--add-host=host.docker.internal:host-gateway',
        '--env-file', $resolvedEnv,
        '-e', "RUN_ID=$scriptRunId",
        '-v', "${scriptRoot}:/scripts:ro",
        '-v', "${resultsDir}:/results",
        'grafana/k6', 'run',
        '--summary-trend-stats=avg,min,med,p(50),p(90),p(95),p(99),max',
        "--summary-export=/results/$summaryName",
        "/scripts/$script"
    )
    if ($script -eq 'stress-04-notification-backlog.js') {
        $sourceId = [guid]::NewGuid().ToString()
        $insertAt = [Array]::IndexOf($arguments, '-v')
        $before = $arguments[0..($insertAt - 1)]
        $after = $arguments[$insertAt..($arguments.Length - 1)]
        $arguments = $before + @('-e', "FIXED_SOURCE_ID=$sourceId") + $after
        Write-Host "Fuente aislada de notificaciones: $sourceId" -ForegroundColor DarkGray
    }
    if (-not [string]::IsNullOrWhiteSpace($BaseUrl)) {
        $insertAt = [Array]::IndexOf($arguments, '-v')
        $before = $arguments[0..($insertAt - 1)]
        $after = $arguments[$insertAt..($arguments.Length - 1)]
        $arguments = $before + @('-e', "BASE_URL=$BaseUrl") + $after
    }

    Write-Host "Ejecutando $script" -ForegroundColor Cyan
    # Windows PowerShell convierte la salida stderr de un proceso nativo en
    # NativeCommandError cuando forma parte de un pipeline. k6 usa stderr para
    # informar umbrales cruzados; no debemos abortar la suite antes de guardar
    # los cinco resultados. Conservamos el exit code y lo evaluamos al final.
    $previousErrorActionPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        & docker @arguments 2>&1 | Tee-Object -FilePath $logPath
        $dockerExitCode = $LASTEXITCODE
    }
    finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    if ($dockerExitCode -ne 0) {
        $failed += $script
    }
}

if ($failed.Count -gt 0) {
    throw "Fallaron: $($failed -join ', '). Revise results/."
}
Write-Host "Suite $Suite finalizada. Evidencia cruda: $resultsDir" -ForegroundColor Green

