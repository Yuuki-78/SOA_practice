# Kill processes using ports 9000-9003
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Finding and killing processes on ports 9000-9003" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$ports = 9000..9003
$killed = 0

foreach ($port in $ports) {
    $connections = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
    
    if ($connections) {
        foreach ($conn in $connections) {
            $pid = $conn.OwningProcess
            $processName = (Get-Process -Id $pid -ErrorAction SilentlyContinue).Name
            
            Write-Host "Port $port is used by PID $pid ($processName)" -ForegroundColor Yellow
            
            try {
                Stop-Process -Id $pid -Force
                Write-Host "  ✓ Killed process $pid" -ForegroundColor Green
                $killed++
            }
            catch {
                Write-Host "  ✗ Failed to kill process $pid" -ForegroundColor Red
            }
        }
    }
    else {
        Write-Host "Port $port is free" -ForegroundColor Gray
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
if ($killed -gt 0) {
    Write-Host "✓ Killed $killed process(es)" -ForegroundColor Green
}
else {
    Write-Host "! No processes found on ports 9000-9003" -ForegroundColor Yellow
}
Write-Host "All ports should now be free" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Press any key to exit..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
