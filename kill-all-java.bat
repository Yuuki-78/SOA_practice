@echo off
echo ========================================
echo Killing ALL Java processes
echo ========================================
echo.

taskkill /F /IM java.exe 2>nul
if %ERRORLEVEL% EQU 0 (
    echo ✓ All Java processes killed successfully
) else (
    echo ! No Java processes found running
)

echo.
echo ========================================
echo Done! You can now start services again.
echo ========================================
echo.
timeout /t 2 /nobreak >nul
