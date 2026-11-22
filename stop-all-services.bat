@echo off
echo ========================================
echo Stopping All SOA Services
echo ========================================
echo.

echo Killing all Java processes (including SBT and services)...
taskkill /F /IM java.exe 2>nul

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ✓ All services stopped successfully.
) else (
    echo.
    echo ! No services were running.
)

echo.
echo All ports (9000-9003) should now be free.
echo You can start services again with start-all-services.bat
echo.
pause
