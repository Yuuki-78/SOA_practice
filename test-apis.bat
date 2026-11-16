@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ========================================
echo Testing SOA Services APIs
echo ========================================
echo.

REM Kiểm tra curl
where curl >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: curl not found! Please install curl first.
    pause
    exit /b 1
)

echo [Step 1] Testing if services are running...
echo.

curl -s http://localhost:9000 >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ❌ AuthService (port 9000) is NOT running!
    echo    Please run start-all-services.bat first
    pause
    exit /b 1
) else (
    echo ✓ AuthService (port 9000) is running
)

curl -s http://localhost:9001 >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ❌ ProductService (port 9001) is NOT running!
    pause
    exit /b 1
) else (
    echo ✓ ProductService (port 9001) is running
)

curl -s http://localhost:9002 >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ❌ OrderService (port 9002) is NOT running!
    pause
    exit /b 1
) else (
    echo ✓ OrderService (port 9002) is running
)

echo.
echo ========================================
echo [Step 2] Login to get JWT Token
echo ========================================
echo.

echo Request: POST http://localhost:9000/auth/login
echo Body: {"username":"admin","password":"admin123"}
echo.

for /f "delims=" %%i in ('curl -s -X POST http://localhost:9000/auth/login -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"admin123\"}"') do set RESPONSE=%%i

echo Response: !RESPONSE!
echo.

REM Parse token từ JSON response (simple parsing)
for /f "tokens=2 delims=:}" %%a in ("!RESPONSE!") do set TOKEN_RAW=%%a
set TOKEN=!TOKEN_RAW:"=!
set TOKEN=!TOKEN: =!

if "!TOKEN!"=="" (
    echo ❌ Failed to get token! Please check AuthService.
    pause
    exit /b 1
)

echo ✓ Token received: !TOKEN:~0,30!...
echo.
timeout /t 2 /nobreak >nul

echo ========================================
echo [Step 3] Test ProductService
echo ========================================
echo.

echo GET http://localhost:9001/products
curl -s -X GET http://localhost:9001/products -H "Authorization: Bearer !TOKEN!" -H "Content-Type: application/json"
echo.
echo.
timeout /t 2 /nobreak >nul

echo ========================================
echo [Step 4] Test OrderService - Get Orders
echo ========================================
echo.

echo GET http://localhost:9002/orders
curl -s -X GET http://localhost:9002/orders -H "Authorization: Bearer !TOKEN!" -H "Content-Type: application/json"
echo.
echo.
timeout /t 2 /nobreak >nul

echo ========================================
echo [Step 5] Test Create Order
echo ========================================
echo.

echo POST http://localhost:9002/orders
echo Body: {"customer_name":"Test Customer","customer_email":"test@example.com","status":"pending"}
for /f "delims=" %%i in ('curl -s -X POST http://localhost:9002/orders -H "Authorization: Bearer !TOKEN!" -H "Content-Type: application/json" -d "{\"customer_name\":\"Test Customer\",\"customer_email\":\"test@example.com\",\"status\":\"pending\"}"') do set CREATE_RESPONSE=%%i
echo Response: !CREATE_RESPONSE!
echo.
timeout /t 2 /nobreak >nul

echo ========================================
echo [Step 6] Test Add Product to Order (with stock check)
echo ========================================
echo.

echo POST http://localhost:9002/order_items
echo Body: {"order_id":1,"product_id":1,"quantity":2}
echo Note: This will check stock from ProductService automatically
curl -s -X POST http://localhost:9002/order_items -H "Authorization: Bearer !TOKEN!" -H "Content-Type: application/json" -d "{\"order_id\":1,\"product_id\":1,\"quantity\":2}"
echo.
echo.
timeout /t 2 /nobreak >nul

echo ========================================
echo [Step 7] Test Get Order Items
echo ========================================
echo.

echo GET http://localhost:9002/order_items
curl -s -X GET http://localhost:9002/order_items -H "Authorization: Bearer !TOKEN!" -H "Content-Type: application/json"
echo.
echo.

echo ========================================
echo ✓ All tests completed!
echo ========================================
echo.
echo You can now use Postman or any REST client to test further.
echo Token for manual testing: !TOKEN!
echo.
pause
