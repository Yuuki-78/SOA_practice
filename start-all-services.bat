@echo off
echo ========================================
echo Starting All SOA Services
echo ========================================
echo.

REM Kiểm tra xem sbt có được cài đặt không
where sbt >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: SBT not found! Please install SBT first.
    echo Download from: https://www.scala-sbt.org/download.html
    pause
    exit /b 1
)

echo [1/3] Starting AuthService on port 9000...
start "AuthService (Port 9000)" cmd /k "cd /d %~dp0authservice && echo Starting AuthService... && sbt run"
timeout /t 5 /nobreak >nul

echo [2/3] Starting ProductService on port 9001...
start "ProductService (Port 9001)" cmd /k "cd /d %~dp0productservice && echo Starting ProductService... && sbt run"
timeout /t 5 /nobreak >nul

echo [3/4] Starting OrderService on port 9002...
start "OrderService (Port 9002)" cmd /k "cd /d %~dp0orderservice && echo Starting OrderService... && sbt run"
timeout /t 5 /nobreak >nul

echo [4/4] Starting ReportingService on port 9003...
start "ReportingService (Port 9003)" cmd /k "cd /d %~dp0reportingservice && echo Starting ReportingService... && sbt run"

echo.
echo ========================================
echo All services are starting...
echo ========================================
echo.
echo AuthService:      http://localhost:9000
echo ProductService:   http://localhost:9001
echo OrderService:     http://localhost:9002
echo ReportingService: http://localhost:9003
echo.
echo Please wait 30-60 seconds for all services to fully start.
echo You will see "Server started" message in each window.
echo.
echo Press any key to exit (services will keep running)...
pause >nul
