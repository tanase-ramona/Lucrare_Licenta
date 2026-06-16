$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendDir = Join-Path $root "backend"
$frontendDir = Join-Path $root "frontend"
$backendLog = Join-Path $backendDir "backend-run.log"
$backendErrLog = Join-Path $backendDir "backend-run.err.log"

function Stop-PortListener {
    param([int] $Port)

    $listeners = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    $pids = $listeners | Select-Object -ExpandProperty OwningProcess -Unique

    foreach ($processId in $pids) {
        if ($processId -and $processId -ne $PID) {
            $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
            if ($process) {
                Write-Host "Stopping process $($process.ProcessName) ($processId) from port $Port..."
                Stop-Process -Id $processId -Force
            }
        }
    }
}

function Wait-Backend {
    $deadline = (Get-Date).AddSeconds(60)

    while ((Get-Date) -lt $deadline) {
        try {
            Invoke-WebRequest -Uri "http://localhost:8080/api/interview/filters" -UseBasicParsing -TimeoutSec 3 | Out-Null
            return
        }
        catch {
            Start-Sleep -Seconds 2
        }
    }

    Write-Host "Backend did not become ready. Last backend log lines:"
    if (Test-Path $backendLog) {
        Get-Content $backendLog -Tail 40
    }
    if (Test-Path $backendErrLog) {
        Get-Content $backendErrLog -Tail 40
    }
    exit 1
}

Write-Host "Freeing backend port 8080..."
Stop-PortListener -Port 8080

Remove-Item $backendLog, $backendErrLog -Force -ErrorAction SilentlyContinue

Write-Host "Starting backend on http://localhost:8080..."
Start-Process `
    -FilePath (Join-Path $backendDir "mvnw.cmd") `
    -ArgumentList "spring-boot:run" `
    -WorkingDirectory $backendDir `
    -RedirectStandardOutput $backendLog `
    -RedirectStandardError $backendErrLog `
    -WindowStyle Hidden

Wait-Backend
Write-Host "Backend is ready."

Write-Host "Starting frontend on http://localhost:5173..."
Set-Location $frontendDir
& npm.cmd run dev
