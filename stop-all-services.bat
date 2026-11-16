@echo off
echo ========================================
echo Stopping All SOA Services
echo ========================================
echo.

echo Killing all SBT processes...
taskkill /F /IM java.exe /FI "WINDOWTITLE eq AuthService*" 2>nul
taskkill /F /IM java.exe /FI "WINDOWTITLE eq ProductService*" 2>nul
taskkill /F /IM java.exe /FI "WINDOWTITLE eq OrderService*" 2>nul

echo.
echo All services stopped.
echo.
pause
