$ErrorActionPreference = "Stop"

$listeners = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
$pids = $listeners | Select-Object -ExpandProperty OwningProcess -Unique

if (-not $pids) {
    Write-Host "No process is listening on port 8080."
    exit 0
}

foreach ($processId in $pids) {
    $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
    if ($process) {
        Write-Host "Stopping $($process.ProcessName) ($processId) on port 8080..."
        Stop-Process -Id $processId -Force
    }
}

Write-Host "Port 8080 is free."
