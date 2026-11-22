@echo off
echo ========================================
echo Killing all Java processes on ports 9000-9003
echo ========================================
echo.

echo Checking port 9000...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :9000') do (
    echo Killing process %%a on port 9000
    taskkill /F /PID %%a 2>nul
)

echo Checking port 9001...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :9001') do (
    echo Killing process %%a on port 9001
    taskkill /F /PID %%a 2>nul
)

echo Checking port 9002...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :9002') do (
    echo Killing process %%a on port 9002
    taskkill /F /PID %%a 2>nul
)

echo Checking port 9003...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :9003') do (
    echo Killing process %%a on port 9003
    taskkill /F /PID %%a 2>nul
)

echo.
echo ========================================
echo Done! All ports should be free now.
echo ========================================
echo.
pause
