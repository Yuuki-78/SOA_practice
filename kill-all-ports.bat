@echo off
echo ========================================
echo Killing processes on ports 9000-9003
echo ========================================
echo.

powershell -ExecutionPolicy Bypass -File "%~dp0kill-ports.ps1"

